package com.waibao.order.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.order.entity.LogOrderRetailer;
import com.waibao.order.entity.LogOrderUser;
import com.waibao.order.service.db.LogOrderRetailerService;
import com.waibao.order.service.db.LogOrderUserService;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * LogOrderConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogOrderConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final LogOrderUserService logOrderUserService;
    private final LogOrderRetailerService logOrderRetailerService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));

        Future<List<LogOrderUser>> logOrderUsersFuture = asyncService.basicTask(convert(messageExtMap.values(), LogOrderUser.class));
        Future<List<LogOrderRetailer>> logOrderRetailersFuture = asyncService.basicTask(convert(messageExtMap.values(), LogOrderRetailer.class));
        while (true) {
            if (logOrderRetailersFuture.isDone() && logOrderUsersFuture.isDone()) break;
        }
        asyncService.basicTask(() -> {
            List<LogOrderUser> logOrderUserList = logOrderUsersFuture.get();
            return logOrderUserService.saveBatch(logOrderUserList);
        });
        asyncService.basicTask(() -> {
            List<LogOrderRetailer> logOrderRetailerList = logOrderRetailersFuture.get();
            return logOrderRetailerService.saveBatch(logOrderRetailerList);
        });

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> {
                    JSONObject jsonObject = (JSONObject) JSON.toJSON(new String(messageExt.getBody()));
                    jsonObject.put("operation", messageExt.getTags());
                    return jsonObject.toJavaObject(clazz);
                })
                .collect(Collectors.toList());
    }
}
