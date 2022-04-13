package com.waibao.seckill.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.service.cache.PurchasedUserCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.seckill.service.mq.AsyncMQMessage;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.seckill.KillVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * TestController
 *
 * @author alexpetertyler
 * @since 2022/4/8
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/seckill/goods")
public class TestController {
    private final AsyncMQMessage asyncMQMessage;
    private final DefaultMQProducer orderCreateMQProducer;
    private final SeckillGoodsCacheService seckillGoodsCacheService;
    private final PurchasedUserCacheService purchasedUserCacheService;

    private final OrderVO orderVO = new OrderVO();

    private final Message message = new Message("order", "test", "", null);


    @PostMapping("/kill")
    public GlobalResult<OrderVO> seckill(
            @RequestBody KillVO killVO,
            @RequestParam("userId") Long userId
    ) {
        Long goodsId = killVO.getGoodsId();
        if (seckillGoodsCacheService.finished(goodsId)) {
            log.error("******TestController：userId：{}，秒杀已结束", userId);
            return GlobalResult.error("秒杀已结束");
        }

        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
        Integer purchaseLimit = seckillGoods.getPurchaseLimit();
        Integer count = killVO.getCount();

        boolean decreaseStorageResult = seckillGoodsCacheService.decreaseStorage(goodsId, count);
        boolean increaseResult = purchasedUserCacheService.increase(goodsId, userId, count, purchaseLimit);
        if (!decreaseStorageResult) {
            log.error("******TestController：userId：{}，秒杀失败，库存不足", userId);
            seckillGoodsCacheService.updateGoodsStatus(goodsId, false);
            return GlobalResult.error("秒杀失败，库存不足");
        }
        if (!increaseResult) {
            log.error("******TestController：userId：{}，秒杀失败，已超过个人最大购买量", userId);
            return GlobalResult.error("秒杀失败，已超过个人最大购买量");
        }

        String orderId = goodsId + IdUtil.getSnowflakeNextIdStr();
        orderVO.setOrderId(orderId);
        orderVO.setRetailerId(seckillGoods.getRetailerId());
        orderVO.setCount(count);
        orderVO.setGoodsId(goodsId);
        orderVO.setRetailerId(seckillGoods.getRetailerId());
        orderVO.setUserId(userId);
        orderVO.setGoodsPrice(seckillGoods.getPrice());
        orderVO.setOrderPrice(seckillGoods.getSeckillPrice().multiply(new BigDecimal(count)));
        orderVO.setPayId(IdUtil.getSnowflakeNextIdStr());

        message.setKeys(orderId);
        message.setBody(JSON.toJSONBytes(orderVO));
        asyncMQMessage.sendMessage(orderCreateMQProducer, message);
        log.info("******TestController：userId：{}，orderId：{} 预减库存成功", userId, orderId);
        return GlobalResult.success("秒杀成功", orderVO);
    }
}
