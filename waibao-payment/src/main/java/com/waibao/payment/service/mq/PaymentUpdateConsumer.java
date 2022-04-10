package com.waibao.payment.service.mq;

import cn.hutool.core.bean.BeanUtil;
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
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * PaymentUpdateConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentUpdateConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final PaymentService paymentService;
    private final AsyncMQMessage asyncMQMessage;
    private final LogPaymentService logPaymentService;
    private final LogPaymentCacheService logPaymentCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    private DefaultMQProducer paymentUpdateMQProducer;

    @SneakyThrows
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        //TODO 检查
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));
        List<PaymentVO> paymentVOList = logPaymentCacheService.batchCheckNotConsumeTags(convert(messageExtMap.values(), PaymentVO.class), "update");

        Future<List<Payment>> paymentFuture = asyncService.basicTask(convert(paymentVOList, Payment.class));
        Future<List<LogPayment>> logPaymentFuture = asyncService.basicTask(convert(paymentVOList, LogPayment.class));
        Future<List<Message>> messageFuture = asyncService.basicTask(convert(paymentVOList, OrderVO.class)
                .stream()
                .map(orderVO -> new Message("order", "update", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList()));
        while (true) {
            if (paymentFuture.isDone() && logPaymentFuture.isDone() && messageFuture.isDone()) break;
        }

        List<Payment> payments = paymentFuture.get();
        List<LogPayment> logPayments = logPaymentFuture.get();
        asyncService.basicTask(() -> paymentService.updateBatchById(payments));
        asyncMQMessage.sendMessage(paymentUpdateMQProducer, messageFuture.get());
        asyncService.basicTask(() -> logPaymentService.updateBatchById(logPayments));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, msgs.stream().map(MessageExt::getMsgId).collect(Collectors.toList()))
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), clazz))
                .collect(Collectors.toList());
    }

    private <T> List<T> convert(List<PaymentVO> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(paymentVO -> BeanUtil.copyProperties(paymentVO, clazz))
                .collect(Collectors.toList());
    }

    @Lazy
    @Autowired
    public void setPaymentUpdateMQProducer(DefaultMQProducer paymentUpdateMQProducer) {
        this.paymentUpdateMQProducer = paymentUpdateMQProducer;
    }
}
