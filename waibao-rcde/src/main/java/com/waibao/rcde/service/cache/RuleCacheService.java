package com.waibao.rcde.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.mapper.RuleMapper;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RuleCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/14
 */
@Service
@RequiredArgsConstructor
public class RuleCacheService {
    public static final String REDIS_RULE_KEY_PREFIX = "rule-";

    private final RuleMapper ruleMapper;

    @Resource
    private ReactiveRedisTemplate<String, Rule> ruleReactiveRedisTemplate;

    private Cache<Long, Rule> ruleCache;
    private RedisScript<String> getRule;
    private BloomFilter<Long> bloomFilter;
    private RedisScript<String> insertRule;
    private RedisScript<String> batchGetRule;
    private RedisScript<String> canalSyncRule;
    private RedisScript<String> batchInsertRule;

    @PostConstruct
    public void init() {
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 100000L, 0.01);
        getRule = RedisScript.of(new ClassPathResource("lua/getRuleScript.lua"), String.class);
        insertRule = RedisScript.of(new ClassPathResource("lua/insertRuleScript.lua"), String.class);
        batchGetRule = RedisScript.of(new ClassPathResource("lua/batchGetRuleScript.lua"), String.class);
        canalSyncRule = RedisScript.of(new ClassPathResource("lua/canalSyncRuleScript.lua"), String.class);
        batchInsertRule = RedisScript.of(new ClassPathResource("lua/batchInsertRuleScript.lua"), String.class);

        ruleCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public Rule get(Long goodsId) {
        Rule rule = ruleCache.getIfPresent(goodsId);
        if (rule != null) return rule;

        List<Rule> list = ruleReactiveRedisTemplate.execute(getRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), Collections.singletonList(goodsId + ""))
                .filter(StrUtil::isNotBlank)
                .collectList()
                .map(ruleList -> ruleList.get(0))
                .map(str -> JSONArray.parseArray(str, Rule.class))
                .share()
                .block();

        if (list != null && !list.isEmpty()) {
            rule = list.get(0);
            set(rule, false);
            return rule;
        }

        if (!bloomFilter.mightContain(goodsId)) return null;

        rule = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getGoodsId, goodsId));
        if (rule != null) set(rule);
        return rule;
    }

    public List<Rule> get(List<Long> goodsIdList) {
        Map<Long, Rule> allPresent = ruleCache.getAllPresent(goodsIdList);
        goodsIdList.removeAll(allPresent.keySet());
        if (goodsIdList.isEmpty()) return new ArrayList<>(allPresent.values());

        List<Rule> list = ruleReactiveRedisTemplate.execute(batchGetRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), goodsIdList)
                .filter(StrUtil::isNotBlank)
                .collectList()
                .map(ruleList -> ruleList.get(0))
                .map(str -> JSONArray.parseArray(str, Rule.class))
                .share()
                .block();

        if (list == null) return new ArrayList<>();
        return list;
    }

    public void set(Rule rule) {
        set(rule, true);
    }

    public void set(Rule rule, boolean updateRedis) {
        ruleCache.put(rule.getGoodsId(), rule);
        bloomFilter.put(rule.getGoodsId());
        if (updateRedis)
            ruleReactiveRedisTemplate.execute(insertRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), Collections.singletonList(rule))
                    .subscribe();
    }

    public void insertBatch(List<Rule> ruleList) {
        ruleReactiveRedisTemplate.execute(batchInsertRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), ruleList)
                .subscribe();
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        ruleReactiveRedisTemplate.execute(canalSyncRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), redisCommandList)
                .subscribe();
    }
}
