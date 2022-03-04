package com.waibao.seckill.service.mq;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.service.cache.GoodsCacheService;
import com.waibao.seckill.service.cache.GoodsRetailerCacheService;
import com.waibao.seckill.service.cache.GoodsStorageCacheService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * GoodsCanalConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisGoodsCanalConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final GoodsCacheService goodsCacheService;
    private final GoodsStorageCacheService goodsStorageCacheService;
    private final GoodsRetailerCacheService goodsRetailerCacheService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        List<RedisCommand> redisCommandList = msgs.parallelStream()
                .map(messageExt -> (JSONObject) JSON.parse(messageExt.getBody()))
                .flatMap(jsonObject -> convert(jsonObject).stream())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(RedisCommand::getTimestamp))
                .collect(Collectors.toList());

        asyncService.basicTask(() -> goodsCacheService.canalSync(redisCommandList));
        asyncService.basicTask(() -> goodsStorageCacheService.canalSync(redisCommandList));
        asyncService.basicTask(() -> goodsRetailerCacheService.canalSync(redisCommandList));
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
                    }
                    if (StrUtil.isBlank(redisCommand.getCommand())) {
                        list.add(null);
                        return;
                    }
                    redisCommand.setValue(((JSONObject) v).toJavaObject(SeckillGoods.class));
                    redisCommand.setTimestamp(timestamp);
                    list.add(redisCommand);
                });

        if (list.isEmpty()) return null;
        return list;
    }
}
