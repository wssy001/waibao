package com.waibao.order.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OrderUserCacheService
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogOrderGoodsCacheService {
    public static final String REDIS_LOG_ORDER_GOODS_KEY_PREFIX = "log-order-goods-";

    @Resource
    private RedisTemplate<String, String> logOrderUserRedisTemplate;

    private RedisScript<String> canalSync;
    private BloomFilter<String> bloomFilter;
    private RedisScript<Boolean> checkLogOrderGoodsOperation;
    private RedisScript<String> batchCheckLogOrderGoodsOperation;

    @PostConstruct
    public void init() {
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncLogOrderGoodsScript.lua"), String.class);
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 60000, 0.001);
        checkLogOrderGoodsOperation = RedisScript.of(new ClassPathResource("lua/checkLogOrderGoodsOperation.lua"), Boolean.class);
        batchCheckLogOrderGoodsOperation = RedisScript.of(new ClassPathResource("lua/batchCheckLogOrderGoodsOperation.lua"), String.class);
    }

    public boolean checkWhetherConsumedTags(Long goodsId, String orderId, String operation) {
        if (!bloomFilter.mightContain(orderId + operation)) return false;
        return Boolean.TRUE.equals(logOrderUserRedisTemplate.execute(checkLogOrderGoodsOperation, Collections.singletonList(REDIS_LOG_ORDER_GOODS_KEY_PREFIX + goodsId), orderId, operation));
    }

    public List<OrderVO> batchCheckNotConsumedTags(List<OrderVO> orderVOList, String operation) {
        List<OrderVO> mightContain = new ArrayList<>();
        List<OrderVO> result = new ArrayList<>();
        orderVOList.forEach(orderVO -> {
            if (bloomFilter.mightContain(orderVO.getOrderId() + operation)) {
                mightContain.add(orderVO);
            } else {
                result.add(orderVO);
            }
        });
        if (mightContain.isEmpty()) return result;

        String execute = logOrderUserRedisTemplate.execute(batchCheckLogOrderGoodsOperation, Collections.singletonList(REDIS_LOG_ORDER_GOODS_KEY_PREFIX), JSONArray.toJSONString(mightContain), operation);
        if (!"{}".equals(execute)) result.addAll(JSONArray.parseArray(execute, OrderVO.class));
        return result;
    }

    public void putToBloomFilter(String orderId, String operation) {
        bloomFilter.put(orderId + operation);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        logOrderUserRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_LOG_ORDER_GOODS_KEY_PREFIX), JSONArray.toJSONString(redisCommandList));
    }
}
