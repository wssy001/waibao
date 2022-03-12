package com.waibao.rcde.service.mq;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.rcde.entity.Deposit;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.entity.UserExtra;
import com.waibao.rcde.mapper.DepositMapper;
import com.waibao.rcde.mapper.UserExtraMapper;
import com.waibao.rcde.service.cache.RedisRuleCacheService;
import com.waibao.rcde.service.cache.RiskUserCacheService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.rcde.RiskUserVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private final DepositMapper depositMapper;
    private final UserExtraMapper userExtraMapper;
    private final RiskUserCacheService riskUserCacheService;
    private final RedisRuleCacheService redisRuleCacheService;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        //TODO 优化
        List<RiskUserVO> riskUserVOList = msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), RiskUserVO.class))
                .collect(Collectors.toList());
        riskUserVOList = riskUserCacheService.getNonRiskUser(riskUserVOList);

        if (riskUserVOList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        Map<Long, List<Long>> collect = riskUserVOList.parallelStream()
                .collect(Collectors.groupingBy(RiskUserVO::getGoodsId, Collectors.mapping(RiskUserVO::getUserId, Collectors.toList())));

        collect.forEach((k, v) -> {
            Rule rule = redisRuleCacheService.get(k).block();
            if (rule == null) return;
            List<RiskUserVO> riskUserVOS = v.stream()
                    .filter(userId -> isRiskUser(rule, userId))
                    .map(userId -> new RiskUserVO().setGoodsId(k).setUserId(userId))
                    .collect(Collectors.toList());
            if (riskUserVOS.isEmpty()) return;
            riskUserCacheService.batchInsert(riskUserVOS);
        });

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private boolean isRiskUser(Rule rule, Long userId) {
        Integer ruleCode = rule.getRuleCode();
        UserExtra userExtra = userExtraMapper.selectById(userId);

        if ((ruleCode & 1) == 1) {
            if (userExtra.getAge() <= rule.getDenyAgeBelow()) return true;
            ruleCode = ruleCode >> 1;
        }

        if ((ruleCode & 1) == 1) {
            if (userExtra.getDefaulter() && rule.getDenyDefaulter()) return true;
            ruleCode = ruleCode >> 1;
        }

        if ((ruleCode & 1) == 1) {
            if (userExtra.getWorkStatus().equals(rule.getDenyWorkStatus())) return true;
            ruleCode = ruleCode >> 1;
        }

        if ((ruleCode & 1) == 1) {
            DateTime dueDate = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR, -rule.getCollectYears());
            DateTime depositDate = DateUtil.offsetDay(dueDate, rule.getAllowOverdueDelayedDays());

            long count = depositMapper.selectCount(Wrappers.<Deposit>lambdaQuery()
                    .eq(Deposit::getUserId, userId)
                    .ge(Deposit::getDueDate, dueDate)
                    .ge(Deposit::getDepositDate, depositDate)
                    .ge(Deposit::getDebtAmount, rule.getIgnoreOverdueAmount())
            );

            return count >= rule.getDenyOverdueTimes();
        }

        return false;
    }
}
