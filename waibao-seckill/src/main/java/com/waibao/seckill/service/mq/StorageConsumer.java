package com.waibao.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * StorageConsumer
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(consumerGroup = "storageConsumer", topic = "storage", selectorExpression = "increase", consumeMode = ConsumeMode.CONCURRENTLY)
public class StorageConsumer implements RocketMQListener<OrderVO> {
    //todo

    @Resource
    private RedisTemplate<String, String> storageRedisTemplate;

    @Override
    public void onMessage(OrderVO orderVO) {
        log.info("******TestConsumer.onMessage：{}", JSON.toJSONString(orderVO));
    }
}
