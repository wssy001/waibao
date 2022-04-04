package com.waibao.rcde.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.rcde.entity.Deposit;
import com.waibao.rcde.mapper.DepositMapper;
import com.waibao.util.async.AsyncService;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * DepositCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/14
 */
@Service
@RequiredArgsConstructor
public class DepositCacheService {
    public static final String REDIS_DEPOSIT_KEY_PREFIX = "deposit-";

    private final AsyncService asyncService;
    private final DepositMapper depositMapper;

    @Resource
    private ReactiveRedisTemplate<String, Deposit> depositReactiveRedisTemplate;

    private BloomFilter<Long> bloomFilter;
    private RedisScript<String> getDeposit;
    private RedisScript<String> insertDeposit;
    private RedisScript<String> batchGetDeposit;
    private RedisScript<String> canalSyncDeposit;
    private RedisScript<String> batchInsertDeposit;

    @PostConstruct
    public void init() {
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 100000L, 0.001);
        getDeposit = RedisScript.of(new ClassPathResource("lua/getDepositScript.lua"), String.class);
        insertDeposit = RedisScript.of(new ClassPathResource("lua/insertDepositScript.lua"), String.class);
        batchGetDeposit = RedisScript.of(new ClassPathResource("lua/batchGetDepositScript.lua"), String.class);
        canalSyncDeposit = RedisScript.of(new ClassPathResource("lua/canalSyncDepositScript.lua"), String.class);
        batchInsertDeposit = RedisScript.of(new ClassPathResource("lua/batchInsertDepositScript.lua"), String.class);
    }

    public Flux<Deposit> get(Long userId) {
        return depositReactiveRedisTemplate.execute(getDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), Collections.singletonList(userId + ""))
                .filter(StrUtil::isNotBlank)
                .map(str -> JSONArray.parseArray(str, Deposit.class))
                .collectList()
                .map(list -> list.get(0))
                .switchIfEmpty(Mono.defer(() -> {
                    if (!bloomFilter.mightContain(userId)) return Mono.empty();
                    return Mono.justOrEmpty(depositMapper.selectList(Wrappers.<Deposit>lambdaQuery().eq(Deposit::getUserId, userId)));
                }))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<Deposit> get(List<Long> userIdList) {
        return depositReactiveRedisTemplate.execute(batchGetDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), userIdList)
                .filter(StrUtil::isNotBlank)
                .map(jsonArray -> JSONArray.parseArray(jsonArray, Deposit.class))
                .flatMap(Flux::fromIterable);
    }

    public void set(Deposit deposit) {
        asyncService.basicTask(() -> depositReactiveRedisTemplate.execute(insertDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), Collections.singletonList(JSON.toJSONString(deposit)))
                .subscribe());
    }

    public void insertBatch(List<Deposit> depositList) {
        asyncService.basicTask(() -> depositList.stream().map(Deposit::getUserId).forEach(bloomFilter::put));
        asyncService.basicTask(() -> depositReactiveRedisTemplate.execute(batchInsertDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), depositList)
                .subscribe());
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        asyncService.basicTask(() -> depositReactiveRedisTemplate.execute(canalSyncDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), redisCommandList)
                .subscribe());
    }
}
