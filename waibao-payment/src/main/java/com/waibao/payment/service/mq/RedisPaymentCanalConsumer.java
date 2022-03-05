package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.payment.entity.Payment;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RedisPaymentCanalConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPaymentCanalConsumer implements MessageListenerConcurrently {

    /**
     * RedisPaymentCanalConsumer监听Canal，接收到的信息是payment（Binlog）
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

        List<RedisCommand> redisCommandList = messageExtMap.values()
                .parallelStream()
                .map(messageExt -> (JSONObject) JSON.parse(messageExt.getBody()))
                .flatMap(jsonObject -> convert(jsonObject).stream())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(RedisCommand::getTimestamp))
                .collect(Collectors.toList());

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private List<RedisCommand> convert(JSONObject jsonObject) {
        List<RedisCommand> list = new ArrayList<>();
        Long timestamp = jsonObject.getLong("ts");
        jsonObject.getJSONArray("data")
                .forEach(v -> {
                    RedisCommand redisCommand = new RedisCommand();
                    switch (jsonObject.getString("type")) {
                        case "INSERT":
                        case "UPDATE":
                            redisCommand.setCommand("SET");
                            break;
                        case "DELETE":
                            redisCommand.setCommand("DEL");
                            break;
                        default:
                            return;
                    }
                    redisCommand.setValue(((JSONObject) v).toJavaObject(Payment.class));
                    redisCommand.setTimestamp(timestamp);
                    list.add(redisCommand);
                });

        return list;
    }
}
