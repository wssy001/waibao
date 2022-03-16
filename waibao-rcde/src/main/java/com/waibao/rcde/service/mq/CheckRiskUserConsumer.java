package com.waibao.rcde.service.mq;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.waibao.rcde.entity.Deposit;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.service.cache.DepositCacheService;
import com.waibao.rcde.service.cache.RiskUserCacheService;
import com.waibao.rcde.service.cache.RuleCacheService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.rcde.RiskUserVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * CheckRiskUserConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckRiskUserConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final RuleCacheService ruleCacheService;
    private final DepositCacheService depositCacheService;
    private final RiskUserCacheService riskUserCacheService;

    @Resource
    private ReactiveRedisTemplate<String, String> userExtraReactiveRedisTemplate;

    private ReactiveHashOperations<String, String, String> reactiveHashOperations;

    @PostConstruct
    public void init() {
        reactiveHashOperations = userExtraReactiveRedisTemplate.opsForHash();
    }

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        List<RiskUserVO> riskUserVOList = msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), RiskUserVO.class))
                .collect(Collectors.toList());
        riskUserVOList = riskUserCacheService.getNonRiskUser(riskUserVOList);

        if (riskUserVOList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        Future<List<Rule>> ruleFuture = asyncService.basicTask(ruleCacheService.get(
                riskUserVOList.stream()
                        .map(RiskUserVO::getGoodsId)
                        .collect(Collectors.toList()))
        );
        Future<List<Deposit>> depositFuture = asyncService.basicTask(depositCacheService.get(
                riskUserVOList.stream()
                        .map(RiskUserVO::getGoodsId)
                        .collect(Collectors.toList()))
                .collectList()
                .share()
                .block());
        while (true) {
            if (ruleFuture.isDone() && depositFuture.isDone()) break;
        }
        List<Rule> rules = ruleFuture.get();
        List<Deposit> depositList = depositFuture.get();

        riskUserVOList = riskUserVOList.parallelStream()
                .map(riskUserVO -> checkRiskUser(riskUserVO, rules, depositList))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (riskUserVOList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        riskUserCacheService.batchInsert(riskUserVOList);
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    @SneakyThrows
    private RiskUserVO checkRiskUser(RiskUserVO riskUserVO, List<Rule> ruleList, List<Deposit> depositList) {
        Future<Rule> ruleFuture = asyncService.basicTask(ruleList.stream()
                .filter(rule -> rule.getGoodsId().equals(riskUserVO.getGoodsId()))
                .findFirst()
                .get()
        );
        Long userId = riskUserVO.getUserId();
        Future<List<Deposit>> listFuture = asyncService.basicTask(depositList.stream()
                .filter(deposit -> deposit.getUserId().equals(userId))
                .collect(Collectors.toList())
        );
        while (true) {
            if (ruleFuture.isDone() && listFuture.isDone()) break;
        }

        Future<Boolean> ageFuture = asyncService.basicTask(checkAge(ruleFuture.get(), userId));
        Future<Boolean> defaulterFuture = asyncService.basicTask(checkDefaulter(ruleFuture.get(), userId));
        Future<Boolean> workStatusFuture = asyncService.basicTask(checkWorkStatus(ruleFuture.get(), userId));
        Future<Boolean> overdueFuture = asyncService.basicTask(checkOverdue(ruleFuture.get(), listFuture.get()));
        while (true) {
            if (ageFuture.isDone() && defaulterFuture.isDone() && workStatusFuture.isDone() && overdueFuture.isDone())
                break;
        }

        if (ageFuture.get().equals(true)) return riskUserVO;
        if (defaulterFuture.get().equals(true)) return riskUserVO;
        if (workStatusFuture.get().equals(true)) return riskUserVO;
        if (overdueFuture.get().equals(true)) return riskUserVO;

        return null;
    }

    private boolean checkAge(Rule rule, Long userId) {
        Integer ruleCode = rule.getRuleCode();
        if ((ruleCode & 1) == 0) return false;
        Integer age = reactiveHashOperations.get("user-extra-" + userId, "age")
                .map(Integer::parseInt)
                .share()
                .block();

        return rule.getDenyAgeBelow() < age;
    }

    private boolean checkDefaulter(Rule rule, Long userId) {
        Integer ruleCode = rule.getRuleCode();
        if (((ruleCode >> 1) & 1) == 0) return false;
        String defaulter = reactiveHashOperations.get("user-extra-" + userId, "defaulter")
                .share()
                .block();

        return "true".equals(defaulter);
    }

    private boolean checkWorkStatus(Rule rule, Long userId) {
        Integer ruleCode = rule.getRuleCode();
        if (((ruleCode >> 2) & 1) == 0) return false;
        String workStatus = reactiveHashOperations.get("user-extra-" + userId, "workStatus")
                .share()
                .block();

        return rule.getDenyWorkStatus().equals(workStatus);
    }

    private boolean checkOverdue(Rule rule, List<Deposit> depositList) {
        Integer ruleCode = rule.getRuleCode();
        if (((ruleCode >> 3) & 1) == 0) return false;
        DateTime time = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR, -rule.getCollectYears());

        depositList = depositList.stream()
                .filter(deposit -> deposit.getDueDate().after(time))
                .filter(deposit -> {
                    int flag = deposit.getDebtAmount().compareTo(rule.getIgnoreOverdueAmount());
                    return flag == 0 || flag > 0;
                })
                .filter(deposit -> {
                    Integer allowOverdueDelayedDays = rule.getAllowOverdueDelayedDays();
                    DateTime endDate = DateUtil.offsetDay(deposit.getDueDate(), allowOverdueDelayedDays);
                    return endDate.isBefore(deposit.getDepositDate());
                })
                .collect(Collectors.toList());

        if (depositList.isEmpty()) return false;
        return rule.getDenyOverdueTimes() <= depositList.size();
    }
}
