package com.waibao.seckill.service.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.seckill.service.cache.GoodsRetailerCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.seckill.service.mq.AsyncMQMessage;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * StorageDecreaseScheduleService
 *
 * @author alexpetertyler
 * @since 2022/4/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageDecreaseScheduleService {
    private final AsyncMQMessage asyncMQMessage;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final DefaultMQProducer orderUpdateMQProducer;
    private final DefaultMQProducer orderCancelMQProducer;
    private final SeckillGoodsCacheService seckillGoodsCacheService;
    private final GoodsRetailerCacheService goodsRetailerCacheService;

    @Resource
    private RedisTemplate<String, String> orderUserRedisTemplate;
    @Resource
    private RedisTemplate<String, String> logUserCreditRedisTemplate;

    private RedisScript<String> batchGetOrderUser;
    private RedisScript<String> batchGetLogUserCredit;

    @PostConstruct
    public void init() {
        batchGetOrderUser = RedisScript.of(new ClassPathResource("lua/batchGetOrderUserScript.lua"), String.class);
        batchGetLogUserCredit = RedisScript.of(new ClassPathResource("lua/batchGetLogUserCreditScript.lua"), String.class);
    }

    @Scheduled(fixedDelay = 5000L)
    public void collectPaidLogUserCredit() {
        String execute = logUserCreditRedisTemplate.execute(batchGetLogUserCredit, Collections.singletonList("log-user-credit"), "paid");
        log.info("******StorageDecreaseScheduleService.collectPaidLogUserCredit：{}", execute);
        if ("{}".equals(execute)) return;
        //FIXME
        execute = orderUserRedisTemplate.execute(batchGetOrderUser, Collections.singletonList("order-user-"), execute);
        if ("{}".equals(execute)) return;
        decreaseStorage(JSONArray.parseArray(execute, OrderVO.class));
    }

    public void decreaseStorage(List<OrderVO> orderVOList) {
        log.info("******StorageDecreaseConsumer：本轮获取消息：{}", orderVOList.size());
        Map<String, OrderVO> orderVOMap = orderVOList.parallelStream()
                .collect(Collectors.toMap(OrderVO::getOrderId, Function.identity()));
        log.info("******StorageDecreaseConsumer：处理后消息数量：{}", orderVOMap.size());
        ConcurrentMap<Long, List<OrderVO>> collect = orderVOMap.values()
                .parallelStream()
                .collect(Collectors.groupingByConcurrent(OrderVO::getGoodsId));

        List<OrderVO> cancel = new ArrayList<>();
        List<OrderVO> complete = new ArrayList<>();

        collect.forEach((k, v) -> {
            log.info("******StorageDecreaseConsumer：尝试批量扣减的商品Id：{}，数量：{}", k, v.size());
            SeckillGoods seckillGoods = goodsRetailerCacheService.get(v.get(0).getRetailerId(), k);
            if (seckillGoods.getStorage() == 0) {
                log.error("******StorageDecreaseConsumer：goodsId：{} 商品售罄", k);
                v = v.stream()
                        .peek(orderVO -> orderVO.setStatus("商品售罄"))
                        .collect(Collectors.toList());
                cancel.addAll(v);
                seckillGoodsCacheService.updateGoodsStatus(k, false);
                return;
            }

            int totalCount = v.parallelStream()
                    .mapToInt(OrderVO::getCount)
                    .sum();
            int update = seckillGoodsMapper.decreaseStorage(k, totalCount);
            if (update == 1) {
                complete.addAll(v);
                log.info("******StorageDecreaseConsumer：goodsId：{} 已批量扣减库存 {}个", k, totalCount);
            } else {
                log.info("******StorageDecreaseConsumer：goodsId：{} 库存告急，单个扣减", k);
                seckillGoodsCacheService.updateGoodsStatus(k, false);
                v.sort(Comparator.comparingLong(orderVO -> orderVO.getPurchaseTime().getTime()));
                for (OrderVO orderVO : v) {
                    update = seckillGoodsMapper.decreaseStorage(k, orderVO.getCount());
                    if (update == 0) {
                        orderVO.setStatus("库存不足");
                        cancel.add(orderVO);
                    } else {
                        complete.add(orderVO);
                    }
                }
            }
        });

        if (!complete.isEmpty()) asyncMQMessage.sendMessage(orderUpdateMQProducer, complete.stream()
                .peek(orderVO -> log.info("******StorageDecreaseConsumer：userId：{},orderId：{} 购买成功", orderVO.getUserId(), orderVO.getOrderId()))
                .map(orderVO -> new Message("order", "update", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList())
        );

        if (!cancel.isEmpty()) asyncMQMessage.sendMessage(orderCancelMQProducer, cancel.stream()
                .peek(orderVO -> log.info("******StorageDecreaseConsumer：userId：{},orderId：{} 购买失败 原因：{}", orderVO.getUserId(), orderVO.getOrderId(), orderVO.getStatus()))
                .map(orderVO -> new Message("order", "cancel", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList())
        );
    }

}
