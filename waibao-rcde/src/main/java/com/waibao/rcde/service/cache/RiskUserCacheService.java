package com.waibao.rcde.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.util.vo.rcde.RiskUserVO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * RiskUserCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/11
 */
@Service
@RequiredArgsConstructor
public class RiskUserCacheService {
    public static final String REDIS_RISK_USER_KEY_PREFIX = "risk-user-";

    private final RedissonClient redissonClient;

    @Resource
    private ReactiveRedisTemplate<String, Long> riskUserReactiveRedisTemplate;

    private RBloomFilter<String> bloomFilter;
    private RedisScript<String> batchCheckUser;
    private RedisScript<String> batchInsertUser;
    private ReactiveSetOperations<String, Long> setOperations;

    @PostConstruct
    public void init() {
        String batchCheckUserScript = "local key = KEYS[1]\n" +
                "local riskUserVOList = {}\n" +
                "local riskUserVO\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    riskUserVO = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SISMEMBER', key .. riskUserVO[\"goodsId\"], riskUserVO[\"userId\"]))\n" +
                "    if count == 1 then\n" +
                "        table.insert(riskUserVOList, riskUserVO)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(riskUserVOList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(riskUserVOList)\n" +
                "end";
        String batchInsertUserScript = "local key = KEYS[1]\n" +
                "local riskUserVOList = {}\n" +
                "local riskUserVO\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    riskUserVO = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SADD', key .. riskUserVO[\"goodsId\"], riskUserVO[\"userId\"]))\n" +
                "    if count == 0 then\n" +
                "        table.insert(riskUserVOList, riskUserVO)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(riskUserVOList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(riskUserVOList)\n" +
                "end";
        bloomFilter = redissonClient.getBloomFilter("riskUserList");
        bloomFilter.tryInit(100000L, 0.01);
        setOperations = riskUserReactiveRedisTemplate.opsForSet();
        batchCheckUser = new DefaultRedisScript<>(batchCheckUserScript, String.class);
        batchInsertUser = new DefaultRedisScript<>(batchInsertUserScript, String.class);
    }

    public Mono<Boolean> checkRiskUser(Long userId, Long goodsId) {
        boolean contains = bloomFilter.contains(userId + "-" + goodsId);
        if (!contains) return Mono.just(false);
        return setOperations.isMember(REDIS_RISK_USER_KEY_PREFIX + goodsId, userId);
    }

    public Flux<RiskUserVO> checkRiskUser(List<RiskUserVO> riskUserVOList) {
        riskUserVOList = riskUserVOList.parallelStream()
                .filter(riskUserVO -> !bloomFilter.contains(riskUserVO.getUserId() + "-" + riskUserVO.getGoodsId()))
                .collect(Collectors.toList());

        if (riskUserVOList.isEmpty()) return Flux.empty();
        return riskUserReactiveRedisTemplate.execute(batchCheckUser, Collections.singletonList(REDIS_RISK_USER_KEY_PREFIX), riskUserVOList)
                .filter(StrUtil::isNotBlank)
                .flatMap(jsonArray -> Flux.fromIterable(JSONArray.parseArray(jsonArray, RiskUserVO.class)))
                .distinct();
    }

    public Flux<RiskUserVO> batchInsert(List<RiskUserVO> riskUserVOList) {
        riskUserVOList.parallelStream()
                .forEach(riskUserVO -> bloomFilter.add((riskUserVO.getUserId() + "-" + riskUserVO.getGoodsId())));
        return riskUserReactiveRedisTemplate.execute(batchInsertUser, Collections.singletonList(REDIS_RISK_USER_KEY_PREFIX), riskUserVOList)
                .filter(StrUtil::isNotBlank)
                .flatMap(jsonArray -> Flux.fromIterable(JSONArray.parseArray(jsonArray, RiskUserVO.class)))
                .distinct();
    }

    public List<RiskUserVO> getNonRiskUser(List<RiskUserVO> riskUserVOList) {
        AtomicReference<List<RiskUserVO>> block = new AtomicReference<>();
        checkRiskUser(riskUserVOList)
                .collectList()
                .subscribe(block::set);

        riskUserVOList.removeAll(block.get());
        return riskUserVOList;
    }

}
