package com.waibao.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.waibao.util.vo.order.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * TestConsumer
 *
 * @author alexpetertyler
 * @since 2022/2/22
 */
@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "orderConsumer", topic = "order", selectorExpression = "create", consumeMode = ConsumeMode.ORDERLY)
public class TestConsumer implements RocketMQListener<OrderVO> {
    @Override
    public void onMessage(OrderVO orderVO) {
        log.info("******TestConsumer.onMessage：{}", JSON.toJSONString(orderVO));
    }
}
