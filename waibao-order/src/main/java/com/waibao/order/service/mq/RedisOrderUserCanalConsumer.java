package com.waibao.order.service.mq;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.order.entity.OrderUser;
import com.waibao.order.service.cache.OrderUserCacheService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * OrderCanalSyncConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Component
@RequiredArgsConstructor
public class RedisOrderUserCanalConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final OrderUserCacheService orderUserCacheService;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        List<RedisCommand> redisCommandList = msgs.parallelStream()
                .map(messageExt -> (JSONObject) JSON.parse(messageExt.getBody()))
                .flatMap(jsonObject -> convert(jsonObject).stream())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(RedisCommand::getTimestamp))
                .collect(Collectors.toList());
        if (redisCommandList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        asyncService.basicTask(() -> orderUserCacheService.canalSync(redisCommandList));

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<RedisCommand> convert(JSONObject jsonObject) {
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
                    }
                    if (StrUtil.isBlank(redisCommand.getCommand())) {
                        list.add(null);
                        return;
                    }
                    redisCommand.setValue(((JSONObject) v).toJavaObject(OrderUser.class));
                    redisCommand.setTimestamp(timestamp);
                    list.add(redisCommand);
                });

        return list;
    }
}
