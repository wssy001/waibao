package com.waibao.rcde.service.mq;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.service.cache.RuleCacheService;
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
 * RuleCanalConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleCanalConsumer implements MessageListenerConcurrently {
    private final RuleCacheService ruleCacheService;

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

        ruleCacheService.canalSync(redisCommandList);
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
                            break;
                        case "UPDATE":
                            redisCommand.setCommand("UPDATE");
                            redisCommand.setOldValue(jsonObject.getJSONArray("old").getJSONObject(0).toJavaObject(Rule.class));
                            break;
                        case "DELETE":
                            redisCommand.setCommand("DELETE");
                    }
                    if (StrUtil.isBlank(redisCommand.getCommand())) {
                        list.add(null);
                        return;
                    }
                    redisCommand.setValue(((JSONObject) v).toJavaObject(Rule.class));
                    redisCommand.setTimestamp(timestamp);
                    list.add(redisCommand);
                });

        return list;
    }
}
