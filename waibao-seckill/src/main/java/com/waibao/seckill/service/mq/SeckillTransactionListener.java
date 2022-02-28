package com.waibao.seckill.service.mq;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * TransactionListener
 *
 * @author alexpetertyler
 * @since 2022/2/22
 */
@Component
@RequiredArgsConstructor
@RocketMQTransactionListener
public class SeckillTransactionListener implements RocketMQLocalTransactionListener {
//todo 完成事务消息

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        Object payload = msg.getPayload();
        return RocketMQLocalTransactionState.COMMIT;
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        Object payload = msg.getPayload();
        return RocketMQLocalTransactionState.COMMIT;
    }
}
