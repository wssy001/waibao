package com.waibao.rcde.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.mapper.RuleMapper;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
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

    private final RedissonClient redissonClient;
    private final RuleMapper ruleMapper;

    @Resource
    private RedisTemplate<String, Rule> ruleRedisTemplate;

    private RedisScript<Rule> getRule;
    private RBloomFilter<Long> bloomFilter;
    private RedisScript<String> insertRule;
    private RedisScript<String> batchGetRule;
    private Cache<Long, Rule> ruleCache;
    private RedisScript<String> canalSyncRule;
    private RedisScript<String> batchInsertRule;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("ruleList");
        bloomFilter.tryInit(100000L, 0.01);
        batchGetRule = RedisScript.of(new ClassPathResource("lua/batchGetRuleScript.lua"), String.class);
        batchInsertRule = RedisScript.of(new ClassPathResource("lua/batchInsertRuleScript.lua"), String.class);
        getRule = RedisScript.of(new ClassPathResource("lua/getRuleScript.lua"), Rule.class);
        insertRule = RedisScript.of(new ClassPathResource("lua/insertRuleScript.lua"), String.class);
        canalSyncRule = RedisScript.of(new ClassPathResource("lua/canalSyncRuleScript.lua"), String.class);

        ruleCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public Rule get(Long goodsId) {
        Rule rule = ruleCache.getIfPresent(goodsId);
        if (rule != null) return rule;

        if (!bloomFilter.contains(goodsId)) return null;

        rule = ruleRedisTemplate.execute(getRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), goodsId);
        if (rule != null) {
            set(rule, false);
            return rule;
        }

        rule = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getGoodsId, goodsId));
        if (rule != null) set(rule);

        return rule;
    }

    public List<Rule> get(Collection<Long> goodsIdList) {
        Map<Long, Rule> allPresent = ruleCache.getAllPresent(goodsIdList);
        goodsIdList.removeAll(allPresent.keySet());
        if (goodsIdList.isEmpty()) return new ArrayList<>(allPresent.values());

        String jsonArray = ruleRedisTemplate.execute(batchGetRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), goodsIdList);
        return jsonArray.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(jsonArray, Rule.class);
    }

    public void set(Rule rule) {
        set(rule, true);
    }

    public void set(Rule rule, boolean updateRedis) {
        ruleCache.put(rule.getGoodsId(), rule);
        bloomFilter.add(rule.getGoodsId());
        if (updateRedis)
            ruleRedisTemplate.execute(insertRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), rule);
    }

    public void insertBatch(List<Rule> ruleList) {
        ruleRedisTemplate.execute(batchInsertRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), ruleList.toArray());
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        ruleRedisTemplate.execute(canalSyncRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), redisCommandList.toArray());
    }
}
