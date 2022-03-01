package com.waibao.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * TransactionListener
 *
 * @author alexpetertyler
 * @since 2022/2/22
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQTransactionListener
public class SeckillTransactionListener implements RocketMQLocalTransactionListener {
    public static final String REDIS_TRANSACTION_ORDER_KEY = "transaction-order";

    @Resource
    private RedisTemplate<String, String> transactionRedisTemplate;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            String s = new String((byte[]) msg.getPayload());
            OrderVO orderVO = JSON.parseObject(s, OrderVO.class);
            return add(orderVO);
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        try {
            OrderVO orderVO = (OrderVO) msg.getPayload();
            String orderId = orderVO.getOrderId();
            Boolean member = transactionRedisTemplate.opsForSet()
                    .isMember(REDIS_TRANSACTION_ORDER_KEY, orderId);
            if (Boolean.FALSE.equals(member)) return add(orderVO);
        } catch (Exception e) {
            return RocketMQLocalTransactionState.UNKNOWN;
        }
        return RocketMQLocalTransactionState.COMMIT;
    }

    private RocketMQLocalTransactionState add(OrderVO orderVO) {
        String orderId = orderVO.getOrderId();
        Long count = transactionRedisTemplate.opsForSet()
                .add(REDIS_TRANSACTION_ORDER_KEY, orderId);
        if (Objects.equals(count, 0L)) {
            log.error("******SeckillTransactionListener.executeLocalTransaction：{} 重复消费", orderId);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
        return RocketMQLocalTransactionState.COMMIT;
    }
}
