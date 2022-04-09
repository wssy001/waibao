package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.LogUserCredit;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.PaymentCacheService;
import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.payment.service.db.LogUserCreditService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
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
    private final PaymentCacheService paymentCacheService;
    private final LogUserCreditService logUserCreditService;
    private final UserCreditCacheService userCreditCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    @Resource
    private RedisTemplate<String, String> transactionRedisTemplate;

    private DefaultMQProducer paymentCancelMQProducer;
    private DefaultMQProducer paymentUpdateMQProducer;

    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String transactionId = msg.getTransactionId();
        String keys = msg.getKeys();
        Boolean exist = transactionRedisTemplate.opsForSet()
                .isMember("payment-transaction", transactionId + "-" + keys);

        if (Boolean.TRUE.equals(exist)) {
            log.warn("******PaymentTransactionListener.executeLocalTransaction：事务Id：{}，key：{} 重复消费", transactionId, keys);
            return LocalTransactionState.COMMIT_MESSAGE;
        }

        try {
            List<JSONObject> jsonObjectList = userCreditCacheService.batchDecreaseUserCredit(convert(msg, OrderVO.class));

            Future<List<OrderVO>> orderVoFuture = asyncService.basicTask(jsonObjectList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(OrderVO.class))
                    .collect(Collectors.toList()));
            Future<List<UserCredit>> userCreditFuture = asyncService.basicTask(jsonObjectList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(UserCredit.class))
                    .collect(Collectors.toList()));
            Future<List<LogUserCredit>> logUserCreditFuture = asyncService.basicTask(jsonObjectList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(LogUserCredit.class))
                    .collect(Collectors.toList()));
            Future<List<Message>> messageListFuture = asyncService.basicTask(jsonObjectList.stream()
                    .map(jsonObject -> new Message("order", "update", jsonObject.getString("orderId"), jsonObject.toJSONString().getBytes()))
                    .collect(Collectors.toList()));
            while (true) {
                if (orderVoFuture.isDone() && logUserCreditFuture.isDone() && messageListFuture.isDone() && userCreditFuture.isDone())
                    break;
            }

            List<UserCredit> userCredits = userCreditFuture.get();
            List<LogUserCredit> logUserCredits = logUserCreditFuture.get();
            asyncService.basicTask(() -> userCreditCacheService.batchSet(userCredits));
            asyncService.basicTask(() -> logUserCreditService.saveBatch(logUserCredits));
            asyncMQMessage.sendMessage(paymentUpdateMQProducer, messageListFuture.get());
            asyncService.basicTask(() -> mqMsgCompensationMapper.update(null, Wrappers.<MqMsgCompensation>lambdaUpdate()
                    .eq(MqMsgCompensation::getMsgId, keys)
                    .set(MqMsgCompensation::getStatus, "补偿消息已消费")));

            CopyOnWriteArrayList<LogUserCredit> paidLogUserCreditList = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<Message> unpaidPaymentList = new CopyOnWriteArrayList<>();

            logUserCredits.parallelStream()
                    .forEach(logUserCredit -> {
                        if (logUserCredit.getOperation().equals("paid")) {
                            paidLogUserCreditList.add(logUserCredit);
                        } else {
                            String payId = logUserCredit.getPayId();
                            unpaidPaymentList.add(new Message("payment", "cancel", payId, JSON.toJSONBytes(paymentCacheService.get(payId))));
                        }
                    });

            if (!paidLogUserCreditList.isEmpty())
                asyncService.basicTask(() -> userCreditMapper.batchUpdateByIdAndOldMoney(paidLogUserCreditList));
            if (!unpaidPaymentList.isEmpty()) asyncMQMessage.sendMessage(paymentCancelMQProducer, unpaidPaymentList);

            transactionRedisTemplate.opsForSet()
                    .add("payment-transaction", transactionId + "-" + keys);
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            log.error("******PaymentTransactionListener.executeLocalTransaction：消息id：{} 事务id：{} 出错 原因：{} 处理：回查", keys, transactionId, e.getMessage());
            return LocalTransactionState.UNKNOW;
        }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        String transactionId = msg.getTransactionId();
        String keys = msg.getKeys();
        Boolean exist = transactionRedisTemplate.opsForSet()
                .isMember("payment-transaction", transactionId + "-" + keys);

        if (Boolean.TRUE.equals(exist)) {
            log.warn("******PaymentTransactionListener.executeLocalTransaction：事务Id：{}，key：{} 重复消费", transactionId, keys);
            return LocalTransactionState.COMMIT_MESSAGE;
        }

        try {
            List<JSONObject> jsonObjectList = userCreditCacheService.batchDecreaseUserCredit(convert(msg, OrderVO.class));

            Future<List<OrderVO>> orderVoFuture = asyncService.basicTask(jsonObjectList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(OrderVO.class))
                    .collect(Collectors.toList()));
            Future<List<UserCredit>> userCreditFuture = asyncService.basicTask(jsonObjectList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(UserCredit.class))
                    .collect(Collectors.toList()));
            Future<List<LogUserCredit>> logUserCreditFuture = asyncService.basicTask(jsonObjectList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(LogUserCredit.class))
                    .collect(Collectors.toList()));
            Future<List<Message>> messageListFuture = asyncService.basicTask(jsonObjectList.stream()
                    .map(jsonObject -> new Message("order", "update", jsonObject.getString("orderId"), jsonObject.toJSONString().getBytes()))
                    .collect(Collectors.toList()));
            while (true) {
                if (orderVoFuture.isDone() && logUserCreditFuture.isDone() && messageListFuture.isDone() && userCreditFuture.isDone())
                    break;
            }

            List<UserCredit> userCredits = userCreditFuture.get();
            List<LogUserCredit> logUserCredits = logUserCreditFuture.get();
            asyncService.basicTask(() -> userCreditCacheService.batchSet(userCredits));
            asyncService.basicTask(() -> logUserCreditService.saveBatch(logUserCredits));
            asyncMQMessage.sendMessage(paymentUpdateMQProducer, messageListFuture.get());
            asyncService.basicTask(() -> mqMsgCompensationMapper.update(null, Wrappers.<MqMsgCompensation>lambdaUpdate()
                    .eq(MqMsgCompensation::getMsgId, keys)
                    .set(MqMsgCompensation::getStatus, "补偿消息已消费")));

            CopyOnWriteArrayList<LogUserCredit> paidLogUserCreditList = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<Message> unpaidPaymentList = new CopyOnWriteArrayList<>();

            logUserCredits.parallelStream()
                    .forEach(logUserCredit -> {
                        if (logUserCredit.getOperation().equals("paid")) {
                            paidLogUserCreditList.add(logUserCredit);
                        } else {
                            String payId = logUserCredit.getPayId();
                            unpaidPaymentList.add(new Message("payment", "cancel", payId, JSON.toJSONBytes(paymentCacheService.get(payId))));
                        }
                    });

            if (!paidLogUserCreditList.isEmpty())
                asyncService.basicTask(() -> userCreditMapper.batchUpdateByIdAndOldMoney(paidLogUserCreditList));
            if (!unpaidPaymentList.isEmpty()) asyncMQMessage.sendMessage(paymentCancelMQProducer, unpaidPaymentList);

            transactionRedisTemplate.opsForSet()
                    .add("payment-transaction", transactionId + "-" + keys);
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            log.error("******PaymentTransactionListener.checkLocalTransaction：消息id：{} 事务id：{} 出错 原因：{} 处理：回滚", keys, transactionId, e.getMessage());
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
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

    @Lazy
    @Autowired
    public void setPaymentCancelMQProducer(DefaultMQProducer paymentCancelMQProducer) {
        this.paymentCancelMQProducer = paymentCancelMQProducer;
    }

    @Lazy
    @Autowired
    public void setPaymentUpdateMQProducer(DefaultMQProducer paymentUpdateMQProducer) {
        this.paymentUpdateMQProducer = paymentUpdateMQProducer;
    }
}
