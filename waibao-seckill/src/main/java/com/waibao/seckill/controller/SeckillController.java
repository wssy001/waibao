package com.waibao.seckill.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.util.async.AsyncService;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.seckill.service.cache.PurchasedUserCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.seckill.service.cache.SeckillPathCacheService;
import com.waibao.seckill.service.mq.AsyncMQMessage;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.seckill.KillVO;
import com.waibao.util.vo.seckill.SeckillControlVO;
import com.waibao.util.vo.seckill.SeckillGoodsVO;
import com.waibao.util.vo.user.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * SeckillController
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill/goods")
public class SeckillController {
    private final AsyncService asyncService;
    private final CaptchaService captchaService;
    private final AsyncMQMessage asyncMQMessage;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final DefaultMQProducer orderCreateMQProducer;
    private final SeckillPathCacheService seckillPathCacheService;
    private final SeckillGoodsCacheService seckillGoodsCacheService;
    private final PurchasedUserCacheService purchasedUserCacheService;


    @PostMapping(value = "/control", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<String> seckillControl(
            @RequestBody SeckillControlVO seckillControlVO
    ) {
        Long goodsId = seckillControlVO.getGoodsId();
        if (goodsId == null) return GlobalResult.error("请传入goodsId");
        asyncService.basicTask(() -> seckillGoodsCacheService.updateGoodsStatus(goodsId, seckillControlVO.getFinished()));
        asyncService.basicTask(() -> seckillGoodsMapper.update(null, Wrappers.<SeckillGoods>lambdaUpdate()
                .eq(SeckillGoods::getGoodsId, goodsId)
                .set(SeckillGoods::getStorage, 0)
                .set(SeckillGoods::getSeckillEndTime, new Date())
        ));
        return GlobalResult.success("请求成功");
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<SeckillGoodsVO>> getUserPage(
            @RequestBody PageVO<SeckillGoodsVO> pageVO
    ) {
        IPage<SeckillGoods> seckillGoodsPage = new Page<>(pageVO.getIndex(), pageVO.getCount());
        seckillGoodsPage = seckillGoodsMapper.selectPage(seckillGoodsPage, Wrappers.<SeckillGoods>lambdaQuery().orderByAsc(SeckillGoods::getSeckillStartTime));

        List<SeckillGoods> records = seckillGoodsPage.getRecords();
        if (records == null) records = new ArrayList<>();

        List<SeckillGoodsVO> goodsVOList = records.parallelStream()
                .map(seckillGoods -> BeanUtil.copyProperties(seckillGoods, SeckillGoodsVO.class))
                .collect(Collectors.toList());

        pageVO.setMaxIndex(seckillGoodsPage.getPages());
        pageVO.setList(goodsVOList);
        pageVO.setMaxSize(seckillGoodsPage.getTotal());
        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }

    @GetMapping("/info")
    public GlobalResult<SeckillGoodsVO> getSeckillGoods(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("userId") Long userId
    ) {
        if (seckillGoodsCacheService.finished(goodsId))
            return GlobalResult.error("秒杀已结束");

        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
        if (seckillGoods.getGoodsId() == null) return GlobalResult.error("秒杀产品不存在");

        SeckillGoodsVO seckillGoodsVO = BeanUtil.copyProperties(seckillGoods, SeckillGoodsVO.class);
        int currentStorage = seckillGoodsCacheService.get(goodsId).getStorage();
        seckillGoodsVO.setCurrentStorage(currentStorage);
        return GlobalResult.success(seckillGoodsVO);
    }

    @GetMapping("/seckillPath")
    public GlobalResult<KillVO> getSeckillPath(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("userId") Long userId,
            @RequestBody(required = false) CaptchaVO captchaVO
    ) {
        if (seckillGoodsCacheService.finished(goodsId))
            return GlobalResult.error("秒杀已结束");

//        ResponseModel verification = captchaService.verification(captchaVO);
//        if (!verification.isSuccess()) return GlobalResult.error(verification.getRepMsg());

        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
        if (seckillGoods.getGoodsId() == null) return GlobalResult.error("秒杀产品不存在");
        Date date = new Date();
        if (date.before(seckillGoods.getSeckillStartTime())) return GlobalResult.error("秒杀还未开始");
        if (date.after(seckillGoods.getSeckillEndTime())) {
            seckillGoodsCacheService.updateGoodsStatus(goodsId, true);
            return GlobalResult.error("秒杀已结束");
        }

        if (purchasedUserCacheService.reachLimit(goodsId, userId, seckillGoods.getPurchaseLimit()))
            GlobalResult.error("您已达到最大秒杀次数");

        String path = seckillPathCacheService.set(goodsId);
        KillVO killVO = new KillVO();
        killVO.setSeckillPath(path);
        killVO.setUserId(userId);
        killVO.setGoodsId(goodsId);
        return GlobalResult.success(killVO);
    }

    @PostMapping("/kill")
    public GlobalResult<OrderVO> seckill(
            @RequestBody KillVO killVO,
            @RequestParam("userId") Long userId
    ) {
        Long goodsId = killVO.getGoodsId();
        if (seckillGoodsCacheService.finished(goodsId))
            return GlobalResult.error("秒杀已结束");

//        if (!seckillPathCacheService.delete(killVO.getRandomStr(), goodsId)) return GlobalResult.error("秒杀地址无效！");
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
        orderVO.setUserId(userId);
        orderVO.setGoodsPrice(seckillGoods.getPrice());
        orderVO.setOrderPrice(seckillGoods.getSeckillPrice().multiply(new BigDecimal(count)));
        orderVO.setPayId(IdUtil.getSnowflakeNextIdStr());

        String jsonString = JSON.toJSONString(orderVO);
        Message message = new Message("order", "create", orderId, jsonString.getBytes());
        asyncMQMessage.sendMessage(orderCreateMQProducer, message);
        asyncMQMessage.sendDelayedMessage(orderCreateMQProducer, message, 2);

        return GlobalResult.success("秒杀成功", orderVO);
    }

    //    正常情况：第一次请求>150ms，后续10次请求平均20ms
    @PostMapping("/kill/test")
    public GlobalResult<JSONObject> seckillTest(
            @PathVariable("goodsId") Long goodsId,
            @RequestParam("userId") Long userId,
            @RequestParam("count") Integer count,
            @RequestParam("purchaseLimit") Integer purchaseLimit
    ) {
        long start = new Date().getTime();
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
        orderVO.setRetailerId(1L);
        orderVO.setCount(count);
        orderVO.setGoodsId(goodsId);
        orderVO.setUserId(userId);
        orderVO.setGoodsPrice(new BigDecimal("10000.00"));
        orderVO.setOrderPrice(new BigDecimal("10000.00").multiply(new BigDecimal(count)));
        orderVO.setPayId(IdUtil.getSnowflakeNextIdStr());

        String jsonString = JSON.toJSONString(orderVO);
        Message message = new Message("order", "create", orderId, jsonString.getBytes());
        asyncMQMessage.sendMessage(orderCreateMQProducer, message);
        asyncMQMessage.sendDelayedMessage(orderCreateMQProducer, message, 2);

        long end = new Date().getTime();
        return GlobalResult.success("秒杀成功，耗时：" + (end - start) + " ms");
    }

}
