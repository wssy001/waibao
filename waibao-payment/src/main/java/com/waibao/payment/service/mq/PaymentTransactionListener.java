package com.waibao.payment.service.mq;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.LogUserCredit;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.payment.service.db.LogUserCreditService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
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
    private final AsyncMQMessage asyncMQMessage;
    private final UserCreditMapper userCreditMapper;
    private final LogUserCreditService logUserCreditService;
    private final DefaultMQProducer paymentCancelMQProducer;
    private final DefaultMQProducer paymentUpdateMQProducer;
    private final UserCreditCacheService userCreditCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    @Resource
    private RedisTemplate<String, String> transactionRedisTemplate;

    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
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

            List<OrderVO> orderVOList = convert(msg, OrderVO.class);
            List<LogUserCredit> logUserCredits = userCreditCacheService.batchDecreaseUserCredit(orderVOList, LogUserCredit.class);
            asyncService.basicTask(() -> logUserCreditService.saveBatch(logUserCredits));

            List<Message> messageList = userCreditCacheService.batchDecreaseUserCredit(orderVOList, OrderVO.class)
                    .parallelStream()
                    .map(orderVO -> new Message("order", "update", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                    .collect(Collectors.toList());
            LambdaUpdateWrapper<UserCredit> userCreditLambdaUpdateWrapper = Wrappers.lambdaUpdate();
            asyncService.basicTask(() -> logUserCredits.forEach(logUserCredit -> {
                userCreditLambdaUpdateWrapper.clear();
                userCreditMapper.update(null, userCreditLambdaUpdateWrapper
                        .eq(UserCredit::getId, logUserCredit.getUserId())
                        .eq(UserCredit::getMoney, logUserCredit.getOldMoney())
                        .set(UserCredit::getMoney, logUserCredit.getMoney())
                );
            }));
            asyncMQMessage.sendMessage(paymentUpdateMQProducer, messageList);

            asyncService.basicTask(() -> mqMsgCompensationMapper.update(null, Wrappers.<MqMsgCompensation>lambdaUpdate()
                    .eq(MqMsgCompensation::getMsgId, msg.getKeys())
                    .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        } catch (Exception e) {
            log.error("******PaymentTransactionListener.executeLocalTransaction：消息id：{} 事务id：{} 出错 原因：{} 处理：回查", msg.getKeys(), msg.getTransactionId(), e.getMessage());
            return LocalTransactionState.UNKNOW;
        }

        return LocalTransactionState.COMMIT_MESSAGE;
    }

    @SneakyThrows
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        //TODO 完善回查
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

        try {

        } catch (Exception e) {
            log.error("******PaymentTransactionListener.checkLocalTransaction：消息id：{} 事务id：{} 出错 原因：{} 处理：回滚", msg.getKeys(), msg.getTransactionId(), e.getMessage());
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }

        return LocalTransactionState.COMMIT_MESSAGE;
    }

    private void rollback(Message msg) {
        asyncMQMessage.sendMessage(paymentCancelMQProducer, msg);
    }

    private <T> List<T> convert(Message message, Class<T> clazz) {
        if (clazz == LogPayment.class) {
            return JSONArray.parseArray(new String(message.getBody()), clazz)
                    .parallelStream()
                    .peek(logPayment -> ((LogPayment) logPayment).setOperation("paid"))
                    .collect(Collectors.toList());
        }
        return JSONArray.parseArray(new String(message.getBody()), clazz);
    }
}
