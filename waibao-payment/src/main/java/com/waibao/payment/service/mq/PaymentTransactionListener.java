package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogUserCredit;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.payment.service.db.LogUserCreditService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.payment.PaymentVO;
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
    private final LogUserCreditService logUserCreditService;
    private final UserCreditCacheService userCreditCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    private final List<JSONObject> jsonObjectList = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<JSONObject> paidList = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<JSONObject> unpaidList = new CopyOnWriteArrayList<>();

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
            log.warn("******executeLocalTransaction：事务Id：{}，key：{} 重复消费", transactionId, keys);
            return LocalTransactionState.COMMIT_MESSAGE;
        }

        try {
            jsonObjectList.addAll(userCreditCacheService.batchDecreaseUserCredit(convert(msg, OrderVO.class)));
            jsonObjectList.forEach(jsonObject -> {
                        if (jsonObject.getString("operation").equals("paid")) {
                            paidList.add(jsonObject);
                            log.info("******executeLocalTransaction：userId：{},orderId：{} 付款成功", jsonObject.getString("userId"), jsonObject.getString("orderId"));
                        } else {
                            unpaidList.add(jsonObject);
                            log.info("******executeLocalTransaction：userId：{},orderId：{} 付款失败，原因：{}", jsonObject.getString("userId"), jsonObject.getString("orderId"), jsonObject.getString("status"));
                        }
                    });
            jsonObjectList.clear();

            Future<List<LogUserCredit>> paidLogUserCreditFuture = asyncService.basicTask(paidList.stream().map(jsonObject -> jsonObject.toJavaObject(LogUserCredit.class)).collect(Collectors.toList()));
            Future<List<Message>> paidMessageFuture = asyncService.basicTask(paidList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(PaymentVO.class))
                    .map(paymentVO -> new Message("payment", "update", paymentVO.getOrderId(), JSON.toJSONBytes(paymentVO)))
                    .collect(Collectors.toList()));
            Future<List<Message>> unpaidMessageFuture = asyncService.basicTask(unpaidList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(PaymentVO.class))
                    .map(paymentVO -> new Message("payment", "cancel", paymentVO.getOrderId(), JSON.toJSONBytes(paymentVO)))
                    .collect(Collectors.toList()));
            while (true) {
                if (paidLogUserCreditFuture.isDone() && paidMessageFuture.isDone() && unpaidMessageFuture.isDone())
                    break;
            }

            paidList.clear();
            unpaidList.clear();
            List<LogUserCredit> logUserCreditList = paidLogUserCreditFuture.get();
            List<Message> unpaidMessageList = unpaidMessageFuture.get();
            if (!logUserCreditList.isEmpty()) {
                asyncService.basicTask(() -> userCreditMapper.batchUpdateByIdAndOldMoney(logUserCreditList));
                asyncService.basicTask(() -> logUserCreditService.saveBatch(logUserCreditList));
                asyncMQMessage.sendMessage(paymentUpdateMQProducer, paidMessageFuture.get());
            }

            if (!unpaidMessageList.isEmpty()) {
                asyncMQMessage.sendMessage(paymentCancelMQProducer, unpaidMessageList);
            }
            asyncService.basicTask(() -> mqMsgCompensationMapper.update(null, Wrappers.<MqMsgCompensation>lambdaUpdate()
                    .eq(MqMsgCompensation::getMsgId, keys)
                    .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
            transactionRedisTemplate.opsForSet()
                    .add("payment-transaction", transactionId + "-" + keys);
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            log.error("******executeLocalTransaction：消息id：{} 事务id：{} 出错 原因：{} 处理：回查", keys, transactionId, e.getMessage());
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
            log.warn("******checkLocalTransaction：事务Id：{}，key：{} 重复消费", transactionId, keys);
            return LocalTransactionState.COMMIT_MESSAGE;
        }

        try {
            List<JSONObject> jsonObjectList = userCreditCacheService.batchDecreaseUserCredit(convert(msg, OrderVO.class));

            CopyOnWriteArrayList<JSONObject> paidList = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<JSONObject> unpaidList = new CopyOnWriteArrayList<>();
            jsonObjectList.parallelStream()
                    .forEach(jsonObject -> {
                        if (jsonObject.getString("operation").equals("paid")) {
                            paidList.add(jsonObject);
                            log.info("******checkLocalTransaction：userId：{},orderId：{} 付款成功", jsonObject.getString("userId"), jsonObject.getString("orderId"));
                        } else {
                            unpaidList.add(jsonObject);
                            log.info("******checkLocalTransaction：userId：{},orderId：{} 付款失败，原因：{}", jsonObject.getString("userId"), jsonObject.getString("orderId"), jsonObject.getString("status"));
                        }
                    });
            Future<List<LogUserCredit>> paidLogUserCreditFuture = asyncService.basicTask(paidList.stream().map(jsonObject -> jsonObject.toJavaObject(LogUserCredit.class)).collect(Collectors.toList()));
            Future<List<Message>> paidMessageFuture = asyncService.basicTask(paidList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(PaymentVO.class))
                    .map(paymentVO -> new Message("payment", "update", paymentVO.getOrderId(), JSON.toJSONBytes(paymentVO)))
                    .collect(Collectors.toList()));
            Future<List<Message>> unpaidMessageFuture = asyncService.basicTask(unpaidList.stream()
                    .map(jsonObject -> jsonObject.toJavaObject(PaymentVO.class))
                    .map(paymentVO -> new Message("payment", "cancel", paymentVO.getOrderId(), JSON.toJSONBytes(paymentVO)))
                    .collect(Collectors.toList()));
            while (true) {
                if (paidLogUserCreditFuture.isDone() && paidMessageFuture.isDone() && unpaidMessageFuture.isDone())
                    break;
            }

            List<LogUserCredit> logUserCreditList = paidLogUserCreditFuture.get();
            List<Message> unpaidMessageList = unpaidMessageFuture.get();
            if (!logUserCreditList.isEmpty()) {
                asyncService.basicTask(() -> userCreditMapper.batchUpdateByIdAndOldMoney(logUserCreditList));
                asyncService.basicTask(() -> logUserCreditService.saveBatch(logUserCreditList));
                asyncMQMessage.sendMessage(paymentUpdateMQProducer, paidMessageFuture.get());
            }

            if (!unpaidMessageList.isEmpty()) {
                asyncMQMessage.sendMessage(paymentCancelMQProducer, unpaidMessageList);
            }
            asyncService.basicTask(() -> mqMsgCompensationMapper.update(null, Wrappers.<MqMsgCompensation>lambdaUpdate()
                    .eq(MqMsgCompensation::getMsgId, keys)
                    .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
            transactionRedisTemplate.opsForSet()
                    .add("payment-transaction", transactionId + "-" + keys);
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            log.error("******checkLocalTransaction：消息id：{} 事务id：{} 出错 原因：{} 处理：回滚", keys, transactionId, e.getMessage());
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    private <T> List<T> convert(Message message, Class<T> clazz) {
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
