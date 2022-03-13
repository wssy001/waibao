package com.waibao.payment.service.cache;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.PaymentMapper;
import com.waibao.payment.service.db.PaymentService;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.feign.UserService;
import com.waibao.util.tools.BeanUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.payment.PaymentVO;
import com.waibao.util.vo.user.PageVO;
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
import java.util.stream.Collectors;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCacheService {
    private final String REDIS_USER_CREDIT_KEY_PREFIX = "payment-";

    private final PaymentMapper paymentMapper;
    private final UserService userService;
    private final PaymentService paymentService;

    @Resource
    private RedisTemplate<String, Payment> paymentRedisTemplate;

    private DefaultRedisScript<String> canalSync;
    private ValueOperations<String, Payment> valueOperations;

    @PostConstruct
    public void init() {
        String canalSyncScript = "local key = KEYS[1]\n" +
                "local redisCommand\n" +
                "local payment\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    redisCommand = cjson.decode(value)\n" +
                "    payment = redisCommand['value']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. payment['payId'] .. '\"'\n" +
                "    if redisCommand['command'] == 'SET' then\n" +
                "        payment['@type'] = 'com.waibao.payment.entity.Payment'\n" +
                "        redis.call('SET', key, cjson.encode(payment))\n" +
                "    else\n" +
                "        redis.call('DEL', key)\n" +
                "    end\n" +
                "end";
        valueOperations = paymentRedisTemplate.opsForValue();
        canalSync = new DefaultRedisScript<>(canalSyncScript);
    }


    public GlobalResult<PaymentVO> get(Long payId) {
        Payment payment = valueOperations.get(REDIS_USER_CREDIT_KEY_PREFIX + payId);
        if (payment == null) {
            payment = paymentService.getOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getPayId, payId));
            if (payment == null) {
                return GlobalResult.error("系统错误，请联系管理员");
            }
        }
        PaymentVO record = BeanUtil.copyProperties(payment, PaymentVO.class);
        return GlobalResult.success(record);
    }

    public void set(Payment payment) {
        valueOperations.set(REDIS_USER_CREDIT_KEY_PREFIX + payment.getPayId(), payment);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        paymentRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), redisCommandList.toArray());
    }
}
