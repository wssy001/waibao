package com.waibao.rcde.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.mapper.RuleMapper;
import com.waibao.rcde.service.cache.RiskUserCacheService;
import com.waibao.rcde.service.mq.AsyncMQMessage;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.rcde.RiskUserVO;
import com.waibao.util.vo.rcde.RuleVO;
import com.waibao.util.vo.user.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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
    private final DefaultMQProducer riskUserCheckMQProducer;

    @PostMapping(value = "/add/rule", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<GlobalResult<RuleVO>> storeRCDERule(
            @RequestBody RuleVO ruleVO
    ) {
        return Mono.just(ruleVO)
                .map(vo -> BeanUtil.copyProperties(vo, Rule.class))
                .flatMap(rule ->
                        Mono.fromFuture(CompletableFuture.supplyAsync(() -> ruleMapper.insert(rule), dbThreadPoolExecutor))
                                .map(count -> {
                                    if (count == 0) {
                                        return GlobalResult.error(ResultEnum.SYSTEM_SAVE_FAIL);
                                    } else {
                                        return GlobalResult.success(ResultEnum.SUCCESS, BeanUtil.copyProperties(rule, RuleVO.class));
                                    }
                                })
                );
    }

    @PostMapping(value = "/request/check/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<GlobalResult<String>> requestCheckUser(
            @RequestBody RiskUserVO riskUserVO
    ) {
        return Mono.just(new RiskUserVO().setGoodsId(riskUserVO.getGoodsId()).setUserId(riskUserVO.getUserId()))
                .map(vo -> new Message("riskUser", "check", JSON.toJSONBytes(vo)))
                .doOnNext(message -> asyncMQMessage.sendMessage(riskUserCheckMQProducer, message))
                .thenReturn(GlobalResult.success("请求发送成功"));
    }

    @PostMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<GlobalResult<String>> checkUser(
            @RequestBody RiskUserVO riskUserVO
    ) {
        return riskUserCacheService.checkRiskUser(riskUserVO.getUserId(), riskUserVO.getGoodsId())
                .map(contains -> contains ? GlobalResult.success("存在") : GlobalResult.success("不存在"));
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<RuleVO>> getAdminPage(
            @RequestBody PageVO<RuleVO> pageVO
    ) {
        IPage<Rule> rulePage = new Page<>(pageVO.getIndex(), pageVO.getCount());
        rulePage = ruleMapper.selectPage(rulePage, Wrappers.<Rule>lambdaQuery().orderByDesc(Rule::getGoodsId));

        List<Rule> records = rulePage.getRecords();
        if (records == null) records = new ArrayList<>();

        List<RuleVO> ruleVOList = records.parallelStream()
                .map(rule -> BeanUtil.copyProperties(rule, RuleVO.class))
                .collect(Collectors.toList());
        pageVO.setMaxIndex(rulePage.getPages());
        pageVO.setList(ruleVOList);
        pageVO.setMaxSize(rulePage.getTotal());

        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }
}
