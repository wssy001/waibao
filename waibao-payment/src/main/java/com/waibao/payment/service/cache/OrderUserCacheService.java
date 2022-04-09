package com.waibao.payment.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.payment.PaymentVO;
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
 * OrderUserCacheService
 *
 * @author alexpetertyler
 * @since 2022/4/9
 */
@Service
public class OrderUserCacheService {
    public static final String REDIS_ORDER_USER_KEY_PREFIX = "order-user-";

    @Resource
    private RedisTemplate<String, OrderVO> orderUserRedisTemplate;

    private RedisScript<String> batchGetOrderVO;

    @PostConstruct
    void init() {
        batchGetOrderVO = RedisScript.of(new ClassPathResource("lua/batchGetOrderVOScript.lua"), String.class);
    }

    public List<OrderVO> batchGetOrderVO(List<PaymentVO> paymentVOList) {
        String execute = orderUserRedisTemplate.execute(batchGetOrderVO, Collections.singletonList(REDIS_ORDER_USER_KEY_PREFIX), JSONArray.toJSONString(paymentVOList));
        if ("{}".equals(execute)) return new ArrayList<>();
        return JSONArray.parseArray(execute, OrderVO.class);
    }
}
