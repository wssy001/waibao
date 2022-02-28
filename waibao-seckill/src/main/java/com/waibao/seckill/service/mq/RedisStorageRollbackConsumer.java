package com.waibao.seckill.service.mq;

import com.waibao.seckill.service.cache.SeckillGoodsStorageCacheService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * StorageConsumer
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(consumerGroup = "redisStorageConsumer", topic = "storage", selectorExpression = "rollback||redisRollback", consumeMode = ConsumeMode.CONCURRENTLY)
public class RedisStorageRollbackConsumer implements RocketMQListener<OrderVO> {
    public static final String REDIS_TRANSACTION_STORAGE_KEY = "transaction-storage-rollback-";

    private final SeckillGoodsStorageCacheService seckillGoodsStorageCacheService;

    @Resource
    private RedisTemplate<String, String> transactionRedisTemplate;

    @Override
    public void onMessage(OrderVO orderVO) {
        String orderId = orderVO.getOrderId();
        Long count = transactionRedisTemplate.opsForSet()
                .add(REDIS_TRANSACTION_STORAGE_KEY + orderId, orderId);

        if (Objects.equals(count, 0L)) {
            log.error("******StorageConsumer.onMessage：{} 重复消费,回滚失败", orderId);
            return;
        }

        Boolean increaseStorage = seckillGoodsStorageCacheService.increaseStorage(orderVO.getGoodsId(), 1);
        if (Boolean.FALSE.equals(increaseStorage)) {
            log.error("******StorageConsumer.onMessage：{} 库存已达最大限制，回滚失败", orderId);
            return;
        }

        log.info("******StorageConsumer.onMessage：{} 库存回滚成功", orderId);
    }
}
