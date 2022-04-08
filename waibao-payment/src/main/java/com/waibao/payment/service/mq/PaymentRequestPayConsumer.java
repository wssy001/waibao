package com.waibao.payment.service.mq;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.payment.service.db.LogPaymentService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
    private final DefaultMQProducer paymentPayMQProducer;
    private final LogPaymentCacheService logPaymentCacheService;

    @Resource
    private RedisTemplate<String, OrderVO> orderGoodsRedisTemplate;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        ConcurrentHashMap<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));
        List<OrderVO> orderVOList = batchGetOrderVo(convert(messageExtMap.values(), OrderVO.class));

        Message message = new Message("storage", "update", JSON.toJSONBytes(orderVOList));
        message.setTransactionId(IdUtil.objectId());
        asyncMQMessage.sendMessageInTransaction(paymentPayMQProducer, message);
        asyncService.basicTask(() -> logPaymentService.saveBatch(convert(msgs, LogPayment.class)));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> (JSONObject) JSON.toJSON(new String(messageExt.getBody())))
                .peek(jsonObject -> {
                    if (clazz == LogPayment.class) jsonObject.put("topic", "requestPay");
                })
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

    private List<OrderVO> batchGetOrderVo(List<OrderVO> orderVOList) {
        List<String> keyList = orderVOList.parallelStream()
                .filter(orderVO -> logPaymentCacheService.hasConsumeTags(orderVO.getUserId(), orderVO.getPayId(), "requestPay"))
                .map(orderVO -> "order-goods-" + orderVO.getOrderId())
                .collect(Collectors.toList());
        return orderGoodsRedisTemplate.opsForValue()
                .multiGet(keyList);
    }

}
