package com.waibao.seckill.controller;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.service.cache.PurchasedUserCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsStorageCacheService;
import com.waibao.seckill.service.cache.SeckillPathCacheService;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * SeckillController
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill")
public class SeckillController {
    private final SeckillGoodsCacheService seckillGoodsCacheService;
    private final SeckillPathCacheService seckillPathCacheService;
    private final SeckillGoodsStorageCacheService seckillGoodsStorageCacheService;
    private final PurchasedUserCacheService purchasedUserCacheService;

    @GetMapping("/goods/{goodsId}/seckillPath")
    public GlobalResult<JSONObject> getSeckillPath(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable("goodsId") Long goodsId
    ) {
        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
        if (seckillGoods.getGoodsId() == null) return GlobalResult.error("秒杀产品不存在");
        Date date = new Date();
        if (date.before(seckillGoods.getSeckillStartTime())) return GlobalResult.error("秒杀还未开始");
        if (date.after(seckillGoods.getSeckillEndTime())) return GlobalResult.error("秒杀已结束");

        String base64 = token.split("\\.")[1];
        UserVO userVO = JSON.parseObject(Base64.decodeStr(base64), UserVO.class);

        if (purchasedUserCacheService.reachLimit(userVO.getId(), seckillGoods.getPurchaseLimit()))
            GlobalResult.error("您已达到最大秒杀次数");

        JSONObject jsonObject = new JSONObject();
        String path = seckillPathCacheService.set(goodsId);
        jsonObject.put("seckillPath", path);
        return GlobalResult.success(jsonObject);
    }

    //todo
    @PostMapping("/goods/{goodsId}/{randomStr}")
    public GlobalResult<JSONObject> seckill(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable("randomStr") String randomStr,
            @PathVariable("goodsId") Long goodsId,
            @RequestParam("")
    ) {
        if (!seckillPathCacheService.delete(randomStr, goodsId)) return GlobalResult.error("秒杀地址无效！");
        if (!seckillGoodsStorageCacheService.decreaseStorage(goodsId,1)) return gl

        String base64 = token.split("\\.")[1];
        UserVO userVO = JSON.parseObject(Base64.decodeStr(base64), UserVO.class);

    }

    //todo
    @PostMapping("/goods/{goodsId}")
    public void seckillTest(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable("goodsId") Long goodsId
    ) {

    }
}
