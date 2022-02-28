package com.waibao.seckill.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.service.cache.PurchasedUserCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsRetailerCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsStorageCacheService;
import com.waibao.seckill.service.cache.SeckillPathCacheService;
import com.waibao.util.base.BaseException;
import com.waibao.util.tools.BeanUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.seckill.KillVO;
import com.waibao.util.vo.seckill.SeckillGoodsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.concurrent.Future;

/**
 * SeckillController
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill")
public class SeckillController {
    private final SeckillGoodsRetailerCacheService seckillGoodsRetailerCacheService;
    private final SeckillPathCacheService seckillPathCacheService;
    private final SeckillGoodsStorageCacheService seckillGoodsStorageCacheService;
    private final PurchasedUserCacheService purchasedUserCacheService;
    private final CaptchaService captchaService;
    private final RocketMQTemplate rocketMQTemplate;

    @GetMapping("/goods/{goodsId}/seckillPath")
    public GlobalResult<JSONObject> getSeckillPath(
            @PathVariable("goodsId") Long goodsId,
            @RequestParam("userId") Long userId,
            @RequestBody CaptchaVO captchaVO
    ) {
        ResponseModel verification = captchaService.verification(captchaVO);
        if (!verification.isSuccess()) return GlobalResult.error(verification.getRepMsg());

        SeckillGoods seckillGoods = seckillGoodsRetailerCacheService.get(goodsId);
        if (seckillGoods.getGoodsId() == null) return GlobalResult.error("秒杀产品不存在");
        Date date = new Date();
        if (date.before(seckillGoods.getSeckillStartTime())) return GlobalResult.error("秒杀还未开始");
        if (date.after(seckillGoods.getSeckillEndTime())) return GlobalResult.error("秒杀已结束");

        if (purchasedUserCacheService.reachLimit(userId, seckillGoods.getPurchaseLimit()))
            GlobalResult.error("您已达到最大秒杀次数");

        JSONObject jsonObject = new JSONObject();
        String path = seckillPathCacheService.set(goodsId);
        jsonObject.put("seckillPath", path);
        return GlobalResult.success(jsonObject);
    }

    @GetMapping("/goods/{goodsId}")
    public GlobalResult<SeckillGoodsVO> getSeckillGoods(
            @PathVariable("goodsId") Long goodsId,
            @RequestParam("userId") Long userId
    ) {
        SeckillGoods seckillGoods = seckillGoodsRetailerCacheService.get(goodsId);
        if (seckillGoods.getGoodsId() == null) return GlobalResult.error("秒杀产品不存在");

        SeckillGoodsVO seckillGoodsVO = BeanUtil.copyProperties(seckillGoods, SeckillGoodsVO.class);
        int currentStorage = seckillGoodsStorageCacheService.get(goodsId);
        seckillGoodsVO.setCurrentStorage(currentStorage);
        return GlobalResult.success(seckillGoodsVO);
    }

    @PostMapping("/goods/kill")
    public GlobalResult<JSONObject> seckill(
            @RequestBody KillVO killVO,
            @RequestParam("userId") Long userId
    ) {
        Long goodsId = killVO.getGoodsId();
        if (!seckillPathCacheService.delete(killVO.getRandomStr(), goodsId)) return GlobalResult.error("秒杀地址无效！");
        SeckillGoods seckillGoods = seckillGoodsRetailerCacheService.get(goodsId);
        Integer purchaseLimit = seckillGoods.getPurchaseLimit();
        Integer count = killVO.getCount();
        if (count > purchaseLimit) return GlobalResult.error("秒杀失败，超过最大购买限制");

        try {
            Future<Boolean> decreaseStorage = seckillGoodsStorageCacheService.decreaseStorageAsync(goodsId, count);
            Future<Boolean> increase = purchasedUserCacheService.increaseAsync(userId, count, purchaseLimit);
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
        String orderId = IdUtil.objectId();
        orderVO.setOrderId(orderId);
        orderVO.setGoodsId(goodsId);
        orderVO.setUserId(userId);
        Message<OrderVO> message = MessageBuilder.withPayload(orderVO)
                .setHeader("KEYS", orderId)
                .build();

        try {
            TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction("order:create", message, null);
            log.info("******SeckillController.seckillTest：");
            if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK) || sendResult.getLocalTransactionState().equals(LocalTransactionState.ROLLBACK_MESSAGE))
                throw new BaseException();
        } catch (Exception e) {
            rocketMQTemplate.convertAndSend("storage:rollback", orderVO);
            log.error("******SeckillController.seckill：{}", e.getMessage());
            return GlobalResult.error("服务异常，无法创建订单");
        }

        return GlobalResult.success("秒杀成功");
    }

    @PostMapping("/goods/{goodsId}")
    public GlobalResult<JSONObject> seckillTest(
            @PathVariable("goodsId") Long goodsId,
            @RequestParam("userId") Long userId,
            @RequestParam("count") Integer count,
            @RequestParam("purchaseLimit") Integer purchaseLimit
    ) {
        long start = new Date().getTime();
        if (count > purchaseLimit) return GlobalResult.error("秒杀失败，超过最大购买限制");
        try {
            Future<Boolean> decreaseStorage = seckillGoodsStorageCacheService.decreaseStorageAsync(goodsId, count);
            Future<Boolean> increase = purchasedUserCacheService.increaseAsync(userId, count, purchaseLimit);
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
        String orderId = IdUtil.objectId();
        orderVO.setOrderId(orderId);
        orderVO.setGoodsId(goodsId);
        orderVO.setUserId(userId);
        Message<OrderVO> message = MessageBuilder.withPayload(orderVO)
                .setHeader("KEYS", orderId)
                .build();

        try {
            TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction("order:create", message, null);
            log.info("******SeckillController.seckillTest：");
            if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK) || sendResult.getLocalTransactionState().equals(LocalTransactionState.ROLLBACK_MESSAGE))
                throw new BaseException();
        } catch (Exception e) {
            rocketMQTemplate.convertAndSend("storage:rollback", orderVO);
            log.error("******SeckillController.seckill：{}", e.getMessage());
            return GlobalResult.error("服务异常，无法创建订单");
        }

        long end = new Date().getTime();
        return GlobalResult.success("秒杀成功，耗时：" + (end - start) + " ms");
    }
}
