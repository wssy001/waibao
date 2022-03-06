package com.waibao.payment.service.mq;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.util.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

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
public class PaymentCreateConsumer implements MessageListenerConcurrently {
    private final AsyncMQMessage asyncMQMessage;
    private final DefaultMQProducer paymentCreateMQProducer;

    /**
     * PaymentCreateConsumer监听订单创建的topic，接收到的信息是OrderVO
     * <p>
     * MessageExt messageExt = new MessageExt();
     * 消息ID
     * messageExt.getKeys();
     * 消息内容，byte[] ，new String(byte [])可以得到JSON字符串
     * messageExt.getBody();
     * 消息主题
     * messageExt.getTopic();
     * 消息标签
     * messageExt.getTags();
     **/
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        ConcurrentHashMap<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));

        Message message = new Message("storage", "update", JSON.toJSONBytes(convert(messageExtMap.values())));
        message.setTransactionId(IdUtil.objectId());
        asyncMQMessage.sendMessageInTransaction(paymentCreateMQProducer, message);
        asyncMQMessage.sendDelayedMessage(paymentCreateMQProducer, message, 2);
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private List<PaymentVO> convert(Collection<MessageExt> msgs) {
        return msgs.parallelStream()
                .map(messageExt -> {
                    JSONObject jsonObject = (JSONObject) JSON.toJSON(new String(messageExt.getBody()));
                    PaymentVO paymentVO = new PaymentVO();
                    paymentVO.setUserId(jsonObject.getLong("userId"));
                    paymentVO.setOrderId(jsonObject.getString("orderId"));
                    paymentVO.setGoodsId(jsonObject.getLong("goodsId"));
                    paymentVO.setMoney(jsonObject.getBigDecimal("orderPrice"));
                    return paymentVO;
                })
                .collect(Collectors.toList());
    }

}
