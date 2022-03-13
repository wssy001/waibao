package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.payment.service.db.LogPaymentService;
import com.waibao.payment.service.db.PaymentService;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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
    private final AsyncMQMessage asyncMQMessage;
    private final LogPaymentCacheService logPaymentCacheService;
    private final PaymentService paymentService;
    private final LogPaymentService logPaymentService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    @SneakyThrows
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        //TODO 完成 订单取消
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));
        convert(messageExtMap.values(), Payment.class).parallelStream()
                .filter(payment -> logPaymentCacheService.hasConsumeTags(payment.getUserId(),payment.getPayId(),"cancel"))
                .map(Payment::getPayId)
                .forEach(messageExtMap::remove);
        Future<List<Payment>> paymentFuture = asyncService.basicTask(convert(messageExtMap.values(), Payment.class));
        Future<List<LogPayment>> logPaymentFuture = asyncService.basicTask(convert(messageExtMap.values(), LogPayment.class));

        while (true) {
            if (paymentFuture.isDone() && logPaymentFuture.isDone()) break;
        }

        List<Payment> payments = paymentFuture.get();
        List<LogPayment> logPayments = logPaymentFuture.get();
        asyncService.basicTask(() -> paymentService.updateBatchById(payments));
        asyncService.basicTask(()->logPaymentService.saveBatch(logPayments));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, messageExtMap.keySet())
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody())))
                .peek(jsonObject -> jsonObject.put("status", "支付取消"))
                .peek(jsonObject -> {
                    if (clazz == LogPayment.class) jsonObject.put("topic", "cancel");
                })
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }
}
