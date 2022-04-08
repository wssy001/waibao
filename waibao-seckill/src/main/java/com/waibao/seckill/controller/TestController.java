package com.waibao.seckill.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.service.cache.PurchasedUserCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.seckill.service.mq.AsyncMQMessage;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.seckill.KillVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.Future;

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
    private final AsyncService asyncService;
    private final AsyncMQMessage asyncMQMessage;
    private final DefaultMQProducer orderCreateMQProducer;
    private final SeckillGoodsCacheService seckillGoodsCacheService;
    private final PurchasedUserCacheService purchasedUserCacheService;

    @PostMapping("/kill")
    public GlobalResult<OrderVO> seckill(
            @RequestBody KillVO killVO,
            @RequestParam("userId") Long userId
    ) {
        Long goodsId = killVO.getGoodsId();
        if (seckillGoodsCacheService.finished(goodsId))
            return GlobalResult.error("秒杀已结束");

        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
        Integer purchaseLimit = seckillGoods.getPurchaseLimit();
        Integer count = killVO.getCount();
        if (count > purchaseLimit) return GlobalResult.error("秒杀失败，超过最大购买限制");

        try {
            Future<Boolean> decreaseStorage = asyncService.basicTask(seckillGoodsCacheService.decreaseStorage(goodsId, count));
            Future<Boolean> increase = asyncService.basicTask(purchasedUserCacheService.increase(goodsId, userId, count, purchaseLimit));
            while (true) {
                if (decreaseStorage.isDone() && increase.isDone()) break;
            }
            Boolean decreaseStorageResult = decreaseStorage.get();
            log.info("******减库存操作执行完毕");
            Boolean increaseResult = increase.get();
            log.info("******增加用户购买量操作执行完毕");
            if (!decreaseStorageResult) return GlobalResult.error("秒杀失败，库存不足");
            if (!increaseResult) return GlobalResult.error("秒杀失败，已超过个人最大购买量");
        } catch (Exception e) {
            return GlobalResult.error("秒杀失败，服务器暂时无法执行操作");
        }

        OrderVO orderVO = new OrderVO();
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

        String jsonString = JSON.toJSONString(orderVO);
        Message message = new Message("order", "create", orderId, jsonString.getBytes());
        asyncMQMessage.sendMessage(orderCreateMQProducer, message);

        return GlobalResult.success("秒杀成功", orderVO);
    }
}
