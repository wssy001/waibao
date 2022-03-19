package com.waibao.order.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.order.OrderVO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OrderGoodsCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/1
 */
@Service
public class OrderGoodsCacheService {
    public static final String REDIS_ORDER_GOODS_KEY_PREFIX = "order-goods-";

    @Resource
    private RedisTemplate<String, OrderVO> orderGoodsRedisTemplate;

    private RedisScript<String> canalSync;
    private RedisScript<String> batchInsertOrderGoods;
    private RedisScript<String> batchUpdateOrderGoods;
    private RedisScript<String> batchDeleteOrderGoods;
    private ValueOperations<String, OrderVO> valueOperations;

    @PostConstruct
    void init() {
        valueOperations = orderGoodsRedisTemplate.opsForValue();
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncOrderGoodsScript.lua"), String.class);
        batchInsertOrderGoods = RedisScript.of(new ClassPathResource("lua/batchInsertOrderGoodsScript.lua"), String.class);
        batchUpdateOrderGoods = RedisScript.of(new ClassPathResource("lua/batchUpdateOrderGoodsScript.lua"), String.class);
        batchDeleteOrderGoods = RedisScript.of(new ClassPathResource("lua/batchDeleteOrderGoodsScript.lua"), String.class);
    }

    public OrderVO get(String orderId) {
        return valueOperations.get(orderId);
    }

    //    返回未添加成功的订单列表
    public List<OrderVO> insertBatch(List<OrderVO> orderVOList) {
        String arrayString = orderGoodsRedisTemplate.execute(batchInsertOrderGoods, Collections.singletonList(REDIS_ORDER_GOODS_KEY_PREFIX),
                orderVOList.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderVO.class);
    }

    //    返回不存在的订单列表
    public List<OrderVO> updateBatch(List<OrderVO> orderVOList) {
        String arrayString = orderGoodsRedisTemplate.execute(batchUpdateOrderGoods, Collections.singletonList(REDIS_ORDER_GOODS_KEY_PREFIX),
                orderVOList.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderVO.class);
    }

    //    返回不存在的订单列表
    public List<OrderVO> deleteBatch(List<OrderVO> orderVOList) {
        String arrayString = orderGoodsRedisTemplate.execute(batchDeleteOrderGoods, Collections.singletonList(REDIS_ORDER_GOODS_KEY_PREFIX),
                orderVOList.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        orderGoodsRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_ORDER_GOODS_KEY_PREFIX), redisCommandList.toArray());
    }
}
