package com.waibao.seckill.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.seckill.entity.LogSeckillGoods;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    private DefaultRedisScript<String> canalSync;
    private DefaultRedisScript<String> batchCheckCancel;

    @PostConstruct
    void init() {
        String canalSyncLogGoodsScript = "local key = KEYS[1]\n" +
                "local redisCommand\n" +
                "local logSeckillGoods\n" +
                "local goodsId\n" +
                "local oldLogSeckillGoods\n" +
                "for _, value in ipairs(ARGV) do\n" +
                "    redisCommand = cjson.decode(value)\n" +
                "    logSeckillGoods = redisCommand['value']\n" +
                "    goodsId = logSeckillGoods['goodsId']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. goodsId .. '\"'\n" +
                "    if command == 'INSERT' then\n" +
                "        redis.call('LPUSH', key, logSeckillGoods['orderId'] .. '-' .. logSeckillGoods['operation'])\n" +
                "    elseif command == 'UPDATE' then\n" +
                "        oldLogSeckillGoods = redisCommand['oldValue']\n" +
                "        if oldLogSeckillGoods['operation'] ~= nil then\n" +
                "            redis.call('LPUSH', key, logSeckillGoods['orderId'] .. '-' .. logSeckillGoods['operation'])\n" +
                "            redis.call('LREM', key, 0, logSeckillGoods['orderId'] .. '-' .. oldLogSeckillGoods['operation'])\n" +
                "        end\n" +
                "    else\n" +
                "        redis.call('LREM', key, 0, logSeckillGoods['orderId'] .. '-' .. logSeckillGoods['operation'])\n" +
                "    end\n" +
                "end";
        String batchCheckCancelScript = "local key = KEYS[1]\n" +
                "local orderVO\n" +
                "local orderVOList = {}\n" +
                "local goodsId\n" +
                "for _, value in ipairs(ARGV) do\n" +
                "    orderVO = cjson.decode(value)\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. goodsId .. '\"'\n" +
                "    if tonumber(redis.call('LREM', key, 0, orderVO['orderId'] .. '-' .. 'CANCEL')) > 0 then\n" +
                "        redis.call('LPUSH', key, 0, orderVO['orderId'] .. '-' .. 'CANCEL')\n"+
                "        table.insert(orderVOList, orderVO)\n" +
                "    end\n" +
                "end\n" +
                "\n" +
                "if not next(orderVOList) then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderVOList)\n" +
                "end";
        canalSync = new DefaultRedisScript<>(canalSyncLogGoodsScript);
        bloomFilter.tryInit(100000L, 0.01);
        bloomFilter = redissonClient.getBloomFilter("logGoodsList");
        batchCheckCancel = new DefaultRedisScript<>(batchCheckCancelScript, String.class);
    }

    public List<OrderVO> batchCheckCancel(List<OrderVO> orderVOList) {
        String arrayString = goodsRedisTemplate.execute(batchCheckCancel, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX),
                orderVOList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        goodsRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX),
                redisCommandList.toArray());
    }
}
