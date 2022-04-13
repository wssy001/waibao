package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.LogUserCredit;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.payment.service.cache.OrderUserCacheService;
import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.payment.service.db.LogPaymentService;
import com.waibao.payment.service.db.PaymentService;
import com.waibao.util.async.AsyncService;
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
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PaymentCancelConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final PaymentService paymentService;
    private final AsyncMQMessage asyncMQMessage;
    private final UserCreditMapper userCreditMapper;
    private final LogPaymentService logPaymentService;
    private final OrderUserCacheService orderUserCacheService;
    private final UserCreditCacheService userCreditCacheService;
    private final LogPaymentCacheService logPaymentCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    private DefaultMQProducer paymentRequestPayMQProducer;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = msgs.parallelStream()
                .collect(Collectors.toMap(Message::getKeys, Function.identity(), (prev, next) -> next));
        List<PaymentVO> paymentVOList = logPaymentCacheService.batchCheckNotConsumeTags(convert(messageExtMap.values(), PaymentVO.class), "cancel");

        Future<List<Payment>> paymentFuture = asyncService.basicTask(convert(paymentVOList, Payment.class));
        Future<List<LogPayment>> logPaymentFuture = asyncService.basicTask(convert(paymentVOList, LogPayment.class));
        Future<List<Message>> messageFuture = asyncService.basicTask(orderUserCacheService.batchGetOrderVO(paymentVOList)
                .stream()
                .peek(orderVO -> log.info("******PaymentCancelConsumer：userId：{},orderId：{} 订单取消中", orderVO.getUserId(), orderVO.getOrderId()))
                .map(orderVO -> new Message("order", "cancel", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList()));
        Map<Boolean, List<PaymentVO>> collect = paymentVOList.stream()
                .collect(Collectors.groupingBy(PaymentVO::getPaid));

        if (collect.containsKey(Boolean.TRUE)) {
            List<JSONObject> jsonObjectList = userCreditCacheService.batchIncreaseUserCredit(collect.get(Boolean.TRUE));
            List<LogUserCredit> succeed = jsonObjectList.parallelStream()
                    .filter(jsonObject -> jsonObject.getString("operation").equals("money back"))
                    .peek(jsonObject -> log.info("******PaymentCancelConsumer：userId：{},payId：{} 退钱中", jsonObject.getString("userId"), jsonObject.getString("payId")))
                    .map(jsonObject -> jsonObject.toJavaObject(LogUserCredit.class))
                    .collect(Collectors.toList());

            userCreditMapper.batchUpdateByIdAndOldMoney(succeed);
        }

        while (true) {
            if (paymentFuture.isDone() && logPaymentFuture.isDone() && messageFuture.isDone()) break;
        }

        List<Payment> payments = paymentFuture.get();
        List<LogPayment> logPayments = logPaymentFuture.get();
        asyncService.basicTask(() -> paymentService.saveOrUpdateBatch(payments));
        asyncService.basicTask(() -> logPaymentService.saveBatch(logPayments));
        asyncMQMessage.sendMessage(paymentRequestPayMQProducer, messageFuture.get());
        Future<Integer> task = asyncService.basicTask(mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, messageExtMap.keySet())
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        if (task.isDone()) messageExtMap.clear();
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), clazz))
                .collect(Collectors.toList());
    }

    private <T> List<T> convert(List<PaymentVO> paymentVOList, Class<T> clazz) {
        return paymentVOList.parallelStream()
                .map(paymentVO -> (JSONObject) JSON.toJSON(paymentVO))
                .peek(jsonObject -> jsonObject.put("status", "支付取消"))
                .peek(jsonObject -> {
                    if (clazz == LogPayment.class) {
                        jsonObject.put("topic", "payment");
                        jsonObject.put("operation", "cancel");
                    }
                })
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

    @Lazy
    @Autowired
    public void setPaymentRequestPayMQProducer(DefaultMQProducer paymentRequestPayMQProducer) {
        this.paymentRequestPayMQProducer = paymentRequestPayMQProducer;
    }
}
