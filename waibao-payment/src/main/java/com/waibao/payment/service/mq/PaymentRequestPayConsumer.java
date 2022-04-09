package com.waibao.payment.service.mq;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.payment.service.cache.OrderUserCacheService;
import com.waibao.payment.service.db.LogPaymentService;
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
    private final LogPaymentService logPaymentService;
    private final OrderUserCacheService orderUserCacheService;
    private final LogPaymentCacheService logPaymentCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    private DefaultMQProducer paymentPayMQProducer;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        List<PaymentVO> paymentVOList = logPaymentCacheService.batchCheckNotConsumeTags(convert(msgs, PaymentVO.class), "requestPay");
        List<OrderVO> orderVOList = orderUserCacheService.batchGetOrderVO(paymentVOList);
        if (orderVOList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        Message message = new Message("storage", "update", JSON.toJSONBytes(orderVOList));
        message.setTransactionId(IdUtil.objectId());
        asyncMQMessage.sendMessageInTransaction(paymentPayMQProducer, message);
        asyncService.basicTask(() -> logPaymentService.saveBatch(convert(paymentVOList, LogPayment.class)));
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

    private <T> List<T> convert(List<PaymentVO> paymentVOList, Class<T> clazz) {
        return paymentVOList.parallelStream()
                .map(paymentVO -> (JSONObject) JSON.toJSON(paymentVO))
                .peek(jsonObject -> jsonObject.put("status", "请求支付"))
                .peek(jsonObject -> {
                    if (clazz == LogPayment.class) {
                        jsonObject.put("topic", "payment");
                        jsonObject.put("operation", "requestPay");
                    }
                })
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

//    private List<OrderVO> batchGetOrderVo(List<PaymentVO> paymentVOList) {
//        List<String> keyList = paymentVOList.parallelStream()
//                .map(paymentVO -> "order-user-" + paymentVO.getOrderId())
//                .collect(Collectors.toList());
//        return orderUserRedisTemplate.opsForValue()
//                .multiGet(keyList);
//    }


    @Lazy
    @Autowired
    public void setPaymentPayMQProducer(DefaultMQProducer paymentPayMQProducer) {
        this.paymentPayMQProducer = paymentPayMQProducer;
    }
}
