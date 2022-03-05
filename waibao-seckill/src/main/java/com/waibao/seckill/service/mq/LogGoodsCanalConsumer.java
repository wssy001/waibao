package com.waibao.seckill.service.mq;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.seckill.entity.LogSeckillGoods;
import com.waibao.seckill.service.db.LogSeckillGoodsService;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.tools.BeanUtil;
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
 * GoodsCanalConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogGoodsCanalConsumer implements MessageListenerConcurrently {
    private final LogSeckillGoodsService logSeckillGoodsService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));
        List<LogSeckillGoods> logSeckillGoodsList = messageExtMap.values()
                .parallelStream()
                .map(messageExt -> (JSONObject) JSON.parse(messageExt.getBody()))
                .flatMap(jsonObject -> convert(jsonObject).stream())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(RedisCommand::getTimestamp))
                .map(redisCommand -> {
                    LogSeckillGoods logSeckillGoods = BeanUtil.copyProperties(redisCommand.getValue(), LogSeckillGoods.class);
                    logSeckillGoods.setSeckillGoodsId(logSeckillGoods.getId());
                    logSeckillGoods.setOperation(redisCommand.getCommand());
                    return logSeckillGoods;
                })
                .collect(Collectors.toList());

        logSeckillGoodsService.saveBatch(logSeckillGoodsList);
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
                            break;
                        case "DELETE":
                            redisCommand.setCommand("DELETE");
                    }
                    if (StrUtil.isBlank(redisCommand.getCommand())) {
                        list.add(null);
                        return;
                    }
                    redisCommand.setValue(((JSONObject) v).toJavaObject(LogSeckillGoods.class));
                    redisCommand.setTimestamp(timestamp);
                    list.add(redisCommand);
                });

        return list;
    }
}
