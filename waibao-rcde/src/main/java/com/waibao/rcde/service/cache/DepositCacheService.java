package com.waibao.rcde.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.rcde.entity.Deposit;
import com.waibao.rcde.mapper.DepositMapper;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    private final RedissonClient redissonClient;
    private final DepositMapper depositMapper;

    @Resource
    private ReactiveRedisTemplate<String, Deposit> depositReactiveRedisTemplate;

    private RBloomFilter<Long> bloomFilter;
    private RedisScript<String> getDeposit;
    private RedisScript<String> insertDeposit;
    private RedisScript<String> batchGetDeposit;
    private RedisScript<String> canalSyncDeposit;
    private RedisScript<String> batchInsertDeposit;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("depositList");
        bloomFilter.tryInit(100000L, 0.01);
        batchGetDeposit = RedisScript.of(new ClassPathResource("lua/batchGetDepositScript.lua"), String.class);
        batchInsertDeposit = RedisScript.of(new ClassPathResource("lua/batchInsertDepositScript.lua"), String.class);
        getDeposit = RedisScript.of(new ClassPathResource("lua/getDepositScript.lua"), String.class);
        insertDeposit = RedisScript.of(new ClassPathResource("lua/insertDepositScript.lua"), String.class);
        canalSyncDeposit = RedisScript.of(new ClassPathResource("lua/canalSyncDepositScript.lua"), String.class);
    }

    public Flux<Deposit> get(Long userId) {

        if (!bloomFilter.contains(userId)) return Flux.empty();

        return depositReactiveRedisTemplate.execute(getDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), Collections.singletonList(userId))
                .map(jsonArray -> JSONArray.parseArray(jsonArray, Deposit.class))
                .filter(list -> !list.isEmpty())
                .defaultIfEmpty(depositMapper.selectList(Wrappers.<Deposit>lambdaQuery().eq(Deposit::getUserId, userId)))
                .filter(list -> !list.isEmpty())
                .doOnNext(this::set)
                .flatMap(Flux::fromIterable);
    }

    public Flux<Deposit> get(List<Long> userIdList) {
        return depositReactiveRedisTemplate.execute(batchGetDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), userIdList)
                .map(jsonArray -> JSONArray.parseArray(jsonArray, Deposit.class))
                .flatMap(Flux::fromIterable);
    }

    public void set(List<Deposit> depositList) {
        set(depositList, true);
    }

    public void set(List<Deposit> depositList, boolean updateRedis) {
        depositList.parallelStream()
                .forEach(deposit -> bloomFilter.add(deposit.getUserId()));

        if (updateRedis)
            depositReactiveRedisTemplate.execute(insertDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), depositList)
                    .subscribe();
    }

    public void insertBatch(List<Deposit> depositList) {
        depositReactiveRedisTemplate.execute(batchInsertDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), depositList)
                .subscribe();
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        depositReactiveRedisTemplate.execute(canalSyncDeposit, Collections.singletonList(REDIS_DEPOSIT_KEY_PREFIX), redisCommandList)
                .subscribe();
    }
}
