package com.waibao.seckill.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.waibao.seckill.entity.MqMsgCompensation;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.service.cache.PurchasedUserCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsStorageCacheService;
import com.waibao.seckill.service.cache.SeckillPathCacheService;
import com.waibao.seckill.service.db.MqMsgCompensationService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.tools.BeanUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.seckill.KillVO;
import com.waibao.util.vo.seckill.SeckillGoodsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
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
    private final SeckillGoodsCacheService seckillGoodsCacheService;
    private final SeckillPathCacheService seckillPathCacheService;
    private final SeckillGoodsStorageCacheService seckillGoodsStorageCacheService;
    private final PurchasedUserCacheService purchasedUserCacheService;
    private final CaptchaService captchaService;
    private final TransactionMQProducer seckillTransactionMQProducer;
    private final MqMsgCompensationService mqMsgCompensationService;
    private final AsyncService asyncService;

    @GetMapping("/goods/{goodsId}/seckillPath")
    public GlobalResult<JSONObject> getSeckillPath(
            @PathVariable("goodsId") Long goodsId,
            @RequestParam("userId") Long userId,
            @RequestBody CaptchaVO captchaVO
    ) {
        ResponseModel verification = captchaService.verification(captchaVO);
        if (!verification.isSuccess()) return GlobalResult.error(verification.getRepMsg());

        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
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
        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
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
        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
        Integer purchaseLimit = seckillGoods.getPurchaseLimit();
        Integer count = killVO.getCount();
        if (count > purchaseLimit) return GlobalResult.error("秒杀失败，超过最大购买限制");

        try {
            Future<Boolean> decreaseStorage = asyncService.basicTask(seckillGoodsStorageCacheService.decreaseStorage(goodsId, count));
            Future<Boolean> increase = asyncService.basicTask(purchasedUserCacheService.increase(userId, count, purchaseLimit));
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

        String jsonString = JSON.toJSONString(orderVO);
        Message message = new Message("order", "create", orderId, jsonString.getBytes());
        String transactionId = IdUtil.objectId();
        message.setTransactionId(transactionId);
        TransactionSendResult sendResult;
        try {
            sendResult = seckillTransactionMQProducer.sendMessageInTransaction(message, null);
        } catch (Exception e) {
            asyncService.basicTask(() -> log.error("******SeckillController.seckillTest：订单id：{} 原因：{} 处理:{}", orderId, "producer发送失败", "等待延迟补偿结果"));
            asyncService.basicTask(() -> sendMqMsgCompensation(orderId, jsonString, transactionId, e.getMessage()));
            return GlobalResult.error("服务异常，请等待，切勿重复下单");
        }
        if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
            String name = sendResult.getSendStatus().name();
            asyncService.basicTask(() -> log.error("******SeckillController.seckillTest：订单id：{} 原因：{} 处理:{}", orderId, name, "等待延迟补偿结果"));
            asyncService.basicTask(() -> sendMqMsgCompensation(orderId, jsonString, transactionId, name));
            return GlobalResult.error("系统忙，请等待，切勿重复下单");
        }
        if (sendResult.getLocalTransactionState().equals(LocalTransactionState.ROLLBACK_MESSAGE)) {
            asyncService.basicTask(() -> seckillGoodsStorageCacheService.increaseStorage(goodsId, count));
            asyncService.basicTask(() -> purchasedUserCacheService.decrease(userId, count));
            asyncService.basicTask(() -> log.error("******SeckillController.seckillTest：订单id：{} 原因：{} 处理:{}", orderId, "MQ事务消息发送失败", "人工介入"));
            return GlobalResult.error("服务异常，秒杀失败，请联系工作人员");
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("orderId", orderId);
        return GlobalResult.success("秒杀成功", jsonObject);
    }

//    正常情况：第一次请求>=200ms，后续10次请求平均47ms
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
            Future<Boolean> decreaseStorage = asyncService.basicTask(seckillGoodsStorageCacheService.decreaseStorage(goodsId, count));
            Future<Boolean> increase = asyncService.basicTask(purchasedUserCacheService.increase(userId, count, purchaseLimit));
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

        String jsonString = JSON.toJSONString(orderVO);
        Message message = new Message("order", "create", orderId, jsonString.getBytes());
        String transactionId = IdUtil.objectId();
        message.setTransactionId(transactionId);
        TransactionSendResult sendResult;
        try {
            sendResult = seckillTransactionMQProducer.sendMessageInTransaction(message, null);
        } catch (Exception e) {
            asyncService.basicTask(() -> log.error("******SeckillController.seckillTest：订单id：{} 原因：{} 处理:{}", orderId, "producer发送失败", "等待延迟补偿结果"));
            asyncService.basicTask(() -> sendMqMsgCompensation(orderId, jsonString, transactionId, e.getMessage()));
            return GlobalResult.error("服务异常，请等待，切勿重复下单");
        }
        if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
            String name = sendResult.getSendStatus().name();
            asyncService.basicTask(() -> log.error("******SeckillController.seckillTest：订单id：{} 原因：{} 处理:{}", orderId, name, "等待延迟补偿结果"));
            asyncService.basicTask(() -> sendMqMsgCompensation(orderId, jsonString, transactionId, name));
            return GlobalResult.error("系统忙，请等待，切勿重复下单");
        }
        if (sendResult.getLocalTransactionState().equals(LocalTransactionState.ROLLBACK_MESSAGE)) {
            asyncService.basicTask(() -> seckillGoodsStorageCacheService.increaseStorage(goodsId, count));
            asyncService.basicTask(() -> purchasedUserCacheService.decrease(userId, count));
            asyncService.basicTask(() -> log.error("******SeckillController.seckillTest：订单id：{} 原因：{} 处理:{}", orderId, "MQ事务消息发送失败", "人工介入"));
            return GlobalResult.error("服务异常，秒杀失败，请联系工作人员");
        }

        long end = new Date().getTime();
        return GlobalResult.success("秒杀成功，耗时：" + (end - start) + " ms");
    }

    private void sendMqMsgCompensation(String orderId, String jsonString, String transactionId, String exceptionMsg) {
        MqMsgCompensation mqMsgCompensation = new MqMsgCompensation();
        mqMsgCompensation.setBusinessKey(transactionId);
        mqMsgCompensation.setTags("create");
        mqMsgCompensation.setTopic("order");
        mqMsgCompensation.setContent(jsonString);
        mqMsgCompensation.setMsgId(orderId);
        mqMsgCompensation.setExceptionMsg(exceptionMsg);
        try {
            mqMsgCompensationService.saveOrUpdate(mqMsgCompensation);
            log.info("******SeckillController.sendMqMsgCompensation：补偿消息发送成功，消息id：{}", orderId);
        } catch (Exception e) {
            log.info("******SeckillController.sendMqMsgCompensation：补偿消息发送失败，消息id：{}", orderId);
        }
    }
}
