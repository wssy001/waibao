package com.waibao.seckill.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SeckillGoodsCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsRetailerCacheService {
    public static final String REDIS_GOODS_RETAILER_KEY_PREFIX = "seckill-goods-retailer-";

    @Resource
    private RedisTemplate<String, SeckillGoods> goodsRetailerRedisTemplate;

    private ValueOperations<String, SeckillGoods> valueOperations;
    private DefaultRedisScript<String> batchInsertOrderGoods;
    private DefaultRedisScript<String> canalSync;

    @PostConstruct
    void init() {
        String batchInsertScript = "local key = KEYS[1]\n" +
                "local seckillGoodsList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local seckillGoods = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SETNX', key .. seckillGoods['retailerId'], value))\n" +
                "    if count == 0 then\n" +
                "        table.insert(seckillGoodsList, seckillGoods)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(seckillGoodsList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(seckillGoodsList)\n" +
                "end";
        String canalSyncScript = "local key = KEYS[1]\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local redisCommand = cjson.decode(value)\n" +
                "    local seckillGoods = redisCommand['value']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. seckillGoods['retailerId'] .. '\"'\n" +
                "    if redisCommand['command'] == 'SET' then\n" +
                "        seckillGoods['@type'] = 'com.waibao.seckill.entity.SeckillGoods'\n" +
                "        redis.call('SET', key, cjson.encode(seckillGoods))\n" +
                "    else\n" +
                "        redis.call('DEL', key)\n" +
                "    end\n" +
                "end";
        valueOperations = goodsRetailerRedisTemplate.opsForValue();
        batchInsertOrderGoods = new DefaultRedisScript<>(batchInsertScript, String.class);
        canalSync = new DefaultRedisScript<>(canalSyncScript);
    }

    public SeckillGoods get(Long retailerId) {
        SeckillGoods seckillGoods = valueOperations.get(REDIS_GOODS_RETAILER_KEY_PREFIX + retailerId);
        if (seckillGoods == null) return new SeckillGoods();
        return seckillGoods;
    }

    public void insert(SeckillGoods seckillGoods) {
        valueOperations.set(REDIS_GOODS_RETAILER_KEY_PREFIX + seckillGoods.getRetailerId(), seckillGoods);
    }

    public List<SeckillGoods> insertBatch(List<SeckillGoods> seckillGoods) {
        String arrayString = goodsRetailerRedisTemplate.execute(batchInsertOrderGoods, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX),
                seckillGoods.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, SeckillGoods.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        goodsRetailerRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX),
                redisCommandList.toArray());
    }
}
