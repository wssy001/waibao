package com.waibao.payment.service.mq;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.payment.service.cache.PaymentCacheService;
import com.waibao.payment.service.db.LogPaymentService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
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
 * PaymentCreateConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTestConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final AsyncMQMessage asyncMQMessage;
    private final LogPaymentService logPaymentService;
    private final PaymentCacheService paymentCacheService;
    private final LogPaymentCacheService logPaymentCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    private final Message message = new Message("payment", "requestPay", "", null);
    private final Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();

    private DefaultMQProducer paymentRequestPayMQProducer;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));
        List<PaymentVO> paymentVOList = logPaymentCacheService.batchCheckNotConsumeTags(convert(messageExtMap.values(), PaymentVO.class), "request pay")
                .stream()
                .peek(paymentVO -> log.info("******PaymentRequestPayConsumer：userId：{},orderId：{} 请求支付", paymentVO.getUserId(), paymentVO.getOrderId()))
                .peek(paymentVO -> paymentVO.setStatus("request pay"))
                .collect(Collectors.toList());

        asyncService.basicTask(() -> paymentCacheService.batchSet(convert(paymentVOList, Payment.class)));
        asyncService.basicTask(() -> logPaymentService.saveBatch(convert(paymentVOList, LogPayment.class)));
        Future<Integer> task = asyncService.basicTask(mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, messageExtMap.keySet())
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));

        List<JSONObject> collect = paymentVOList.parallelStream()
                .map(paymentVO -> (JSONObject) JSON.toJSON(paymentVO))
                .collect(Collectors.toList());

        message.setKeys(IdUtil.objectId());
        message.setBody(JSON.toJSONBytes(collect));
        asyncMQMessage.sendMessage(paymentRequestPayMQProducer, message);

        if (task.isDone()) messageExtMap.clear();
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody())))
                .peek(jsonObject -> jsonObject.put("money", jsonObject.getBigDecimal("orderPrice")))
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

    private <T> List<T> convert(List<PaymentVO> paymentVOList, Class<T> clazz) {
        return paymentVOList.parallelStream()
                .map(paymentVO -> (JSONObject) JSON.toJSON(paymentVO))
                .peek(jsonObject -> jsonObject.put("status", "请求支付"))
                .peek(jsonObject -> {
                    if (clazz == LogPayment.class) {
                        jsonObject.put("topic", "payment");
                        jsonObject.put("operation", "request pay");
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
