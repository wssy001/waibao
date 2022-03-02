package com.waibao.seckill.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TransactionListener
 *
 * @author alexpetertyler
 * @since 2022/3/2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillTransactionListener implements TransactionListener {
    public static final String REDIS_TRANSACTION_ORDER_KEY = "transaction-order";
    private final ConcurrentHashMap<String, Integer> tryTimes = new ConcurrentHashMap<>();

    @Resource
    private RedisTemplate<String, String> transactionRedisTemplate;

    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        LocalTransactionState result;
        String orderId = msg.getKeys();
        try {
            Boolean absent = transactionRedisTemplate.opsForValue()
                    .setIfAbsent(REDIS_TRANSACTION_ORDER_KEY + msg.getTransactionId(), orderId);
            result = Boolean.TRUE.equals(absent) ? LocalTransactionState.COMMIT_MESSAGE :
                    LocalTransactionState.ROLLBACK_MESSAGE;
        } catch (Exception e) {
            log.error("******SeckillTransactionListener.executeLocalTransaction：事务id：{} 原因：{} 处理方式：{}", orderId, e.getMessage(), "返回UNKNOWN");
            return LocalTransactionState.UNKNOW;
        }
        return result;
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        Boolean hasKey;
        String transactionId = msg.getTransactionId();
        try {
            hasKey = transactionRedisTemplate.hasKey(REDIS_TRANSACTION_ORDER_KEY + transactionId);
        } catch (Exception e) {
            Integer times = tryTimes.compute(transactionId, (k, v) -> v == null ? 1 : v + 1);
            if (times < 4) {
                log.error("******SeckillTransactionListener.executeLocalTransaction：事务id：{} 原因：{} 处理方式：{}", transactionId, "redis操作失败", "返回UNKNOWN");
                return LocalTransactionState.UNKNOW;
            }
            log.error("******SeckillTransactionListener.executeLocalTransaction：事务id：{} 原因：{} 处理方式：{}", transactionId, "已达到重试上限", "ROLLBACK");
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
        if (Boolean.FALSE.equals(hasKey)) return LocalTransactionState.ROLLBACK_MESSAGE;
        return LocalTransactionState.COMMIT_MESSAGE;
    }
}
