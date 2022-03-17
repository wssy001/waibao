package com.waibao.seckill.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.waibao.seckill.entity.LogSeckillGoods;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GoodsRetailerCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogSeckillGoodsCacheService {
    public static final String REDIS_SECKILL_GOODS_KEY_PREFIX = "log-goods-";

    private final RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, LogSeckillGoods> goodsRedisTemplate;

    private RBloomFilter<Long> bloomFilter;
    private RedisScript<String> canalSync;
    private RedisScript<String> batchCheckCancel;

    @PostConstruct
    void init() {
        bloomFilter = redissonClient.getBloomFilter("logGoodsList");
        bloomFilter.tryInit(100000L, 0.01);
        canalSync = RedisScript.of(new ClassPathResource(""), String.class);
        batchCheckCancel = RedisScript.of(new ClassPathResource(""), String.class);
    }

    public List<OrderVO> batchCheckCancel(List<OrderVO> orderVOList) {
        String arrayString = goodsRedisTemplate.execute(batchCheckCancel, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX),
                orderVOList.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        goodsRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX),
                redisCommandList.toArray());
    }
}
