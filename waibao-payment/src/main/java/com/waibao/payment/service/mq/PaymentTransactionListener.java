package com.waibao.payment.service.mq;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.*;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.service.db.LogPaymentService;
import com.waibao.payment.service.db.LogUserCreditService;
import com.waibao.payment.service.db.PaymentService;
import com.waibao.payment.service.db.UserCreditService;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SeckillTransactionListener
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTransactionListener implements TransactionListener {
    private final AsyncService asyncService;
    private final PaymentService paymentService;
    private final AsyncMQMessage asyncMQMessage;
    private final LogPaymentService logPaymentService;
    private final UserCreditService userCreditService;
    private final LogUserCreditService logUserCreditService;
    private final DefaultMQProducer paymentCancelMQProducer;
    private final DefaultMQProducer paymentUpdateMQProducer;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    @Resource
    private RedisTemplate<String, String> transactionRedisTemplate;

    /**
     * executeLocalTransaction：预处理事务
     * 返回值说明：
     * LocalTransactionState.UNKNOW：处理有异常，回查
     * LocalTransactionState.ROLLBACK_MESSAGE：检测到重复消息，回滚，同时取消原定于 处理成功后消息发送 的任务
     * LocalTransactionState.COMMIT_MESSAGE：处理正常
     **/
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            asyncService.basicTask(() -> mqMsgCompensationMapper.update(null, Wrappers.<MqMsgCompensation>lambdaUpdate()
                    .eq(MqMsgCompensation::getMsgId, msg.getKeys())
                    .set(MqMsgCompensation::getStatus, "补偿消息已消费")));

            Boolean absent = transactionRedisTemplate.opsForValue()
                    .setIfAbsent("payment-transaction-" + msg.getTransactionId(), msg.getKeys());

            //            当前消息已被消费过
            if (Boolean.FALSE.equals(absent)) {
                msg.setTopic("payment");
                msg.setTags("cancel");
                msg.setKeys(IdUtil.objectId());
                rollback(msg);
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }

            Future<List<Payment>> paymentsFuture = asyncService.basicTask(convert(msg, Payment.class));
            Future<List<LogPayment>> logPaymentsFuture = asyncService.basicTask(convert(msg, LogPayment.class));
            while (true) {
                if (paymentsFuture.isDone() && logPaymentsFuture.isDone()) break;
            }

            List<Payment> paymentList = paymentsFuture.get();
            asyncService.basicTask(() -> paymentService.saveBatch(paymentList));
            asyncService.basicTask(() -> logPaymentService.saveBatch(logPaymentsFuture.get()));
            Future<List<Long>> userIdMapFuture = asyncService.basicTask(paymentList.stream()
                    .map(Payment::getUserId)
                    .collect(Collectors.toList()));
            Future<Map<Long, Payment>> mapFuture = asyncService.basicTask(paymentList.stream()
                    .collect(Collectors.toMap(Payment::getId, Function.identity())));

            List<Message> messageList = paymentList.parallelStream()
                    .map(payment -> new Message("payment", "update", payment.getPayId() + "", JSON.toJSONBytes(payment)))
                    .collect(Collectors.toList());
            asyncMQMessage.sendMessage(paymentUpdateMQProducer, messageList);
            asyncMQMessage.sendDelayedMessage(paymentUpdateMQProducer, messageList, 2);

            while (true) {
                if (mapFuture.isDone() && userIdMapFuture.isDone()) break;
            }
            List<Long> userIdList = userIdMapFuture.get();

//            这里获取的都是用户金额
//            TODO 获取银行账户
            Map<Long, UserCredit> userCreditMap = new ConcurrentHashMap<>();
            userCreditService.list(Wrappers.<UserCredit>lambdaQuery().in(UserCredit::getUserId, userIdList))
                    .parallelStream()
                    .forEach(userCredit -> userCreditMap.put(userCredit.getUserId(), userCredit));

            List<LogUserCredit> logUserCreditList = paymentList.parallelStream()
                    .map(payment -> {
                        Long userId = payment.getUserId();
                        UserCredit userCredit = userCreditMap.get(userId);
                        if (userCredit == null) return null;
                        LogUserCredit logUserCredit = new LogUserCredit();
                        logUserCredit.setPayId(payment.getPayId());
//                        扣款前的钱
                        BigDecimal oldMoney = userCredit.getMoney();
                        logUserCredit.setOldMoney(oldMoney);
                        userCredit = userCreditMap.computeIfPresent(userId, (k, v) -> v.setMoney(oldMoney.min(payment.getMoney())));
//                        扣款后的钱
                        BigDecimal money = userCredit.getMoney();
//                        TODO 给银行内部账号加钱

                        logUserCredit.setMoney(money);
                        logUserCredit.setOperation("INSERT");
                        logUserCredit.setUserId(userId);
                        return logUserCredit;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            asyncService.basicTask(userCreditService.saveBatch(userCreditMap.values()));
            asyncService.basicTask(logUserCreditService.saveBatch(logUserCreditList));
        } catch (Exception e) {
            log.error("******PaymentTransactionListener.executeLocalTransaction：消息id：{} 事务id：{} 出错 原因：{} 处理：回查", msg.getKeys(), msg.getTransactionId(), e.getMessage());
            return LocalTransactionState.UNKNOW;
        }

        return LocalTransactionState.COMMIT_MESSAGE;
    }

    /**
     * checkLocalTransaction：回查事务
     * 返回值说明：
     * LocalTransactionState.UNKNOW：处理有异常，继续回查
     * LocalTransactionState.ROLLBACK_MESSAGE：检测到重复消息，回滚，同时取消原定于 处理成功后消息发送 的任务
     * LocalTransactionState.COMMIT_MESSAGE：处理正常
     **/
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        //TODO 完成回查

        return LocalTransactionState.COMMIT_MESSAGE;
    }

    private void rollback(Message msg) {
        asyncMQMessage.sendMessage(paymentCancelMQProducer, msg);
        asyncMQMessage.sendDelayedMessage(paymentCancelMQProducer, msg, 2);
    }

    private <T> List<T> convert(Message message, Class<T> clazz) {
        if (clazz == LogPayment.class) {
            return JSONArray.parseArray(new String(message.getBody()), clazz)
                    .parallelStream()
                    .peek(logPayment -> ((LogPayment) logPayment).setOperation("INSERT"))
                    .collect(Collectors.toList());
        }
        return JSONArray.parseArray(new String(message.getBody()), clazz);
    }
}
