package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.util.async.AsyncService;
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
 * RedisLogPaymentCanalConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLogPaymentCanalConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final LogPaymentCacheService logPaymentCacheService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));

        List<RedisCommand> redisCommandList = messageExtMap.values()
                .parallelStream()
                .map(messageExt -> (JSONObject) JSON.parse(messageExt.getBody()))
                .flatMap(jsonObject -> convert(jsonObject).stream())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(RedisCommand::getTimestamp))
                .collect(Collectors.toList());
        if (redisCommandList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        asyncService.basicTask(() -> logPaymentCacheService.canalSync(redisCommandList));
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
                            redisCommand.setCommand("INSERT");
                            logPaymentCacheService.putToBloomFilter(((JSONObject) v).getString("payId"), ((JSONObject) v).getString("operation"));
                            break;
                        case "UPDATE":
                            redisCommand.setCommand("UPDATE");
                            redisCommand.setOldValue(jsonObject.getJSONArray("old").getJSONObject(0).toJavaObject(LogPayment.class));
                            logPaymentCacheService.putToBloomFilter(((JSONObject) v).getString("payId"), ((JSONObject) v).getString("operation"));
                            break;
                        case "DELETE":
                            redisCommand.setCommand("DELETE");
                            break;
                        default:
                            return;
                    }
                    redisCommand.setValue(((JSONObject) v).toJavaObject(LogPayment.class));
                    redisCommand.setTimestamp(timestamp);
                    list.add(redisCommand);
                });

        return list;
    }
}
