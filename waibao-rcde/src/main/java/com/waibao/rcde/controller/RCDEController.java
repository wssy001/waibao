package com.waibao.rcde.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.mapper.RuleMapper;
import com.waibao.rcde.service.cache.RiskUserCacheService;
import com.waibao.rcde.service.mq.AsyncMQMessage;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.tools.SMUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.rcde.RiskUserVO;
import com.waibao.util.vo.rcde.RuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * RCDEController
 *
 * @author alexpetertyler
 * @since 2022/3/9
 */
@Slf4j
@RestController
@RequestMapping("/rcde")
@RequiredArgsConstructor
public class RCDEController {
    private final RuleMapper ruleMapper;
    private final AsyncMQMessage asyncMQMessage;
    private final Executor dbThreadPoolExecutor;
    private final RiskUserCacheService riskUserCacheService;

    @PostMapping("/add/rule")
    public Mono<GlobalResult<RuleVO>> storeRCDERule(
            @RequestBody JSONObject jsonObject
    ) {
        String sign = jsonObject.getString("sign");
        String ruleVOJson = jsonObject.getString("data");
        if (!SMUtil.sm2VerifySign("", ruleVOJson, sign))
            return Mono.just(GlobalResult.error("签名验证失败"));

        return Mono.just(JSON.parseObject(ruleVOJson, RuleVO.class))
                .map(ruleVO -> BeanUtil.copyProperties(ruleVO, Rule.class))
                .flatMap(rule ->
                        Mono.fromFuture(CompletableFuture.supplyAsync(() -> ruleMapper.insert(rule), dbThreadPoolExecutor))
                                .map(count -> {
                                    if (count == 0) {
                                        return GlobalResult.error(ResultEnum.SYSTEM_SAVE_FAIL);
                                    } else {
                                        return GlobalResult.success(ResultEnum.SUCCESS, BeanUtil.copyProperties(rule, RuleVO.class));
                                    }
                                }));
    }

    @PostMapping("/request/check")
    public Mono<GlobalResult<String>> requestCheckUser(
            @RequestParam("userId") Long userId,
            @RequestParam("goodsId") Long goodsId
    ) {
        return Mono.just(new RiskUserVO().setGoodsId(goodsId).setUserId(userId))
                .map(riskUserVO -> new Message("riskUser", "check", JSON.toJSONBytes(riskUserVO)))
                //TODO 完成riskProducer
                .doOnNext(message -> asyncMQMessage.sendMessage(null, message))
                .thenReturn(GlobalResult.success("请求发送成功"));
    }

    @GetMapping("/check")
    public Mono<GlobalResult<String>> checkUser(
            @RequestParam("userId") Long userId,
            @RequestParam("goodsId") Long goodsId
    ) {
        return riskUserCacheService.checkRiskUser(userId, goodsId)
                .map(contains -> contains ? GlobalResult.success("存在") : GlobalResult.success("不存在"));
    }
}
