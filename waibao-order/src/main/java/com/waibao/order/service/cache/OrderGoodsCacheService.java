package com.waibao.order.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.util.vo.order.OrderVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * OrderGoodsCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/1
 */
@Service
public class OrderGoodsCacheService {
    public static final String REDIS_ORDER_GOODS_KEY = "order-goods-";

    @Resource
    private RedisTemplate<String, OrderVO> orderGoodsRedisTemplate;

    private ValueOperations<String, OrderVO> valueOperations;
    private DefaultRedisScript<String> batchInsertOrderGoods;
    private DefaultRedisScript<String> batchUpdateOrderGoods;
    private DefaultRedisScript<String> batchDeleteOrderGoods;

    @PostConstruct
    void init() {
        String batchInsertScript = "local key = KEYS[1]\n" +
                "local orderGoodsList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderGoods = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SETNX', key .. orderGoods[\"orderId\"], value))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderGoodsList, orderGoods)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderGoodsList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderGoodsList)\n" +
                "end";
        String batchUpdateScript = "local key = KEYS[1]\n" +
                "local orderGoodsList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderGoods = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('DEL', key .. orderGoods[\"orderId\"]))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderGoodsList, orderGoods)\n" +
                "    else\n" +
                "        redis.call('SET', key .. orderGoods[\"orderId\"], value)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderGoodsList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderGoodsList)\n" +
                "end";
        String batchDeleteScript = "local key = KEYS[1]\n" +
                "local orderGoodsList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderGoods = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('DEL', key .. orderGoods[\"orderId\"]))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderGoodsList, orderGoods)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderGoodsList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderGoodsList)\n" +
                "end";
        valueOperations = orderGoodsRedisTemplate.opsForValue();
        batchInsertOrderGoods = new DefaultRedisScript<>(batchInsertScript, String.class);
        batchUpdateOrderGoods = new DefaultRedisScript<>(batchUpdateScript, String.class);
        batchDeleteOrderGoods = new DefaultRedisScript<>(batchDeleteScript, String.class);
    }

    public OrderVO get(String orderId) {
        return valueOperations.get(orderId);
    }

    //    返回未添加成功的订单列表
    public List<OrderVO> insertBatch(List<OrderVO> orderVOList) {
        String arrayString = orderGoodsRedisTemplate.execute(batchInsertOrderGoods, Collections.singletonList(REDIS_ORDER_GOODS_KEY),
                orderVOList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderVO.class);
    }

    @Async
    public Future<List<OrderVO>> insertBatchAsync(List<OrderVO> orderVOList) {
        return new AsyncResult<>(insertBatch(orderVOList));
    }

    //    返回不存在的订单列表
    public List<OrderVO> updateBatch(List<OrderVO> orderVOList) {
        String arrayString = orderGoodsRedisTemplate.execute(batchUpdateOrderGoods, Collections.singletonList(REDIS_ORDER_GOODS_KEY),
                orderVOList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderVO.class);
    }

    @Async
    public Future<List<OrderVO>> updateBatchAsync(List<OrderVO> orderVOList) {
        return new AsyncResult<>(updateBatch(orderVOList));
    }

    //    返回不存在的订单列表
    public List<OrderVO> deleteBatch(List<OrderVO> orderVOList) {
        String arrayString = orderGoodsRedisTemplate.execute(batchDeleteOrderGoods, Collections.singletonList(REDIS_ORDER_GOODS_KEY),
                orderVOList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderVO.class);
    }

    @Async
    public Future<List<OrderVO>> deleteBatchAsync(List<OrderVO> orderVOList) {
        return new AsyncResult<>(updateBatch(orderVOList));
    }
}
