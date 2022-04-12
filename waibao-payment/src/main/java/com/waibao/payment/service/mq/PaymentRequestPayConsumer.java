package com.waibao.payment.service.mq;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.LogUserCredit;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.payment.service.db.LogPaymentService;
import com.waibao.payment.service.db.LogUserCreditService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * PaymentCreateConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestPayConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final AsyncMQMessage asyncMQMessage;
    private final UserCreditMapper userCreditMapper;
    private final LogPaymentService logPaymentService;
    private final LogUserCreditService logUserCreditService;
    private final UserCreditCacheService userCreditCacheService;
    private final LogPaymentCacheService logPaymentCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    private final List<JSONObject> jsonObjectList = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<JSONObject> paidList = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<JSONObject> unpaidList = new CopyOnWriteArrayList<>();
    private final Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
    private final Message message = new Message("storage", "decrease", "", null);

    private DefaultMQProducer paymentRequestPayMQProducer;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));
        List<PaymentVO> paymentVOList = logPaymentCacheService.batchCheckNotConsumeTags(convert(messageExtMap.values(), PaymentVO.class), "requestPay");

        if (paymentVOList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        jsonObjectList.addAll(userCreditCacheService.batchDecreaseUserCredit(convert(paymentVOList, OrderVO.class)));
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

        Future<List<LogUserCredit>> paidLogUserCreditFuture = asyncService.basicTask(paidList.stream()
                .map(jsonObject -> jsonObject.toJavaObject(LogUserCredit.class))
                .collect(Collectors.toList()));
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
        logUserCreditService.saveBatch(logUserCreditList);
        userCreditMapper.batchUpdateByIdAndOldMoney(logUserCreditList);
        message.setBody(JSON.toJSONBytes(paidList));
        message.setKeys(IdUtil.objectId());
        paidList.clear();
        unpaidList.clear();

        asyncMQMessage.sendMessage(paymentRequestPayMQProducer, paidMessageFuture.get());
        asyncMQMessage.sendMessage(paymentRequestPayMQProducer, unpaidMessageFuture.get());
        asyncMQMessage.sendMessage(paymentRequestPayMQProducer, message);
        asyncService.basicTask(() -> logPaymentService.saveBatch(convert(paymentVOList, LogPayment.class)));
        Future<Integer> task = asyncService.basicTask(mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, messageExtMap.keySet())
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        if (task.isDone()) messageExtMap.clear();
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.stream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), clazz))
                .collect(Collectors.toList());
    }

    private <T> List<T> convert(List<PaymentVO> paymentVOList, Class<T> clazz) {
        return paymentVOList.stream()
                .peek(paymentVO -> {
                    paymentVO.setOperation("request pay");
                    paymentVO.setStatus("请求支付");
                })
                .map(paymentVO -> (JSONObject) JSON.toJSON(paymentVO))
                .peek(jsonObject -> jsonObject.put("topic", "payment"))
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

    @Lazy
    @Autowired
    public void setPaymentRequestPayMQProducer(DefaultMQProducer paymentRequestPayMQProducer) {
        this.paymentRequestPayMQProducer = paymentRequestPayMQProducer;
    }
}
