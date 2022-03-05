package com.waibao.order.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.order.entity.OrderRetailer;
import com.waibao.order.service.cache.OrderRetailerCacheService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * OrderCanalSyncConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Component
@RequiredArgsConstructor
public class RedisOrderRetailerCanalConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final OrderRetailerCacheService orderRetailerCacheService;

    @Override
    @SneakyThrows
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
        if (redisCommandList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        asyncService.basicTask(() -> orderRetailerCacheService.canalSync(redisCommandList));

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
                    redisCommand.setValue(((JSONObject) v).toJavaObject(OrderRetailer.class));
                    redisCommand.setTimestamp(timestamp);
                    list.add(redisCommand);
                });

        return list;
    }
}
