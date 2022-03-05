package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
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
    private final AsyncService asyncService;
    private final AsyncMQMessage asyncMQMessage;

    /**
     * PaymentCreateConsumer监听订单创建的topic，接收到的信息是OrderVO
     *
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
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));

        List<OrderVO> convert = convert(messageExtMap.values());
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private List<OrderVO> convert(Collection<MessageExt> msgs) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), OrderVO.class))
                .collect(Collectors.toList());
    }

}
