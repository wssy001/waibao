package com.waibao.rcde.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.mapper.RuleMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RedisRuleCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/12
 */
@Service
@RequiredArgsConstructor
public class RedisRuleCacheService {
    public static final String REDIS_RULE_KEY_PREFIX = "rule-";

    private final RuleMapper ruleMapper;
    private final RedissonClient redissonClient;

    @Resource
    private ReactiveRedisTemplate<String, Rule> reactiveRedisTemplate;

    private RBloomFilter<Long> bloomFilter;
    private RedisScript<String> batchInsertRule;
    private RedisScript<String> batchGetRule;
    private ReactiveValueOperations<String, Rule> valueOperations;

    @PostConstruct
    public void init() {
        String batchInsertRuleScript = "local key = KEYS[1]\n" +
                "local ruleList = {}\n" +
                "local rule\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    rule = cjson.decode(value)\n" +
                "    rule['@type'] = 'com.waibao.rcde.entity.Rule'\n" +
                "    local count = tonumber(redis.call('SET', key .. rule[\"id\"], cjson.encode(rule)))\n" +
                "    if count == 0 then\n" +
                "        table.insert(ruleList, rule)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(ruleList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(ruleList)\n" +
                "end";
        String batchGetScript = "local key = KEYS[1]\n" +
                "local ruleList = {}\n" +
                "local ruleStr\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    ruleStr = tostring(redis.call('GET', key .. value))\n" +
                "    if ruleStr then\n" +
                "        table.insert(ruleList, cjson.decode(ruleStr))\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(ruleList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(ruleList)\n" +
                "end";
        bloomFilter = redissonClient.getBloomFilter("ruleList");
        bloomFilter.tryInit(1000, 0.01);
        valueOperations = reactiveRedisTemplate.opsForValue();
        batchInsertRule = new DefaultRedisScript<>(batchInsertRuleScript, String.class);
        batchGetRule = new DefaultRedisScript<>(batchGetScript, String.class);

        batchInsert(ruleMapper.selectList(null));
    }

    public Flux<Rule> batchInsert(List<Rule> ruleList) {
        ruleList.parallelStream()
                .forEach(ruleVO -> bloomFilter.add(ruleVO.getId()));

        return reactiveRedisTemplate.execute(batchInsertRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), ruleList)
                .filter(StrUtil::isNotBlank)
                .flatMap(jsonArray -> Flux.fromIterable(JSONArray.parseArray(jsonArray, Rule.class)))
                .distinct();
    }

    public Mono<Rule> get(Long goodsId) {
        return valueOperations.get(REDIS_RULE_KEY_PREFIX + goodsId)
                .filter(Objects::nonNull);
    }

    public Flux<Rule> batchGet(List<Long> goodsId) {
        return reactiveRedisTemplate.execute(batchGetRule, Collections.singletonList(REDIS_RULE_KEY_PREFIX), goodsId)
                .filter(StrUtil::isNotBlank)
                .flatMap(jsonArray -> Flux.fromIterable(JSONArray.parseArray(jsonArray, Rule.class)))
                .distinct();
    }
}
