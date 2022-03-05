package com.waibao.payment.service.cache;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.PaymentMapper;
import com.waibao.payment.service.db.PaymentService;
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
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
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

    private ValueOperations<String, Payment> valueOperations;

    @PostConstruct
    public void init() {
        valueOperations = paymentRedisTemplate.opsForValue();
    }

    public GlobalResult<PaymentVO> add(PaymentVO paymentVO) {
        //判断用户是否存在
        GlobalResult<String> result = userService.checkUser(paymentVO.getUserId());
        if (result.getCode() != 200) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        //TODO 判断商品是否存在、判断订单是否存在、判断账户信息是否存在
        //TODO 从账户信息表扣除客户对应金额，银行账户（账户信息表事先添加银行账户）增加金额
        Payment record = BeanUtil.copyProperties(paymentVO, Payment.class);
        record.setPayId(IdWorker.getId());
        int insert = paymentMapper.insert(record);
        if (insert == 0) return GlobalResult.error(ResultEnum.SYSTEM_SAVE_FAIL);
        this.set(record);
        return GlobalResult.success(paymentVO);
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

    public GlobalResult<PageVO<PaymentVO>> list(PageVO<PaymentVO> pageVO) {
        IPage<Payment> paymentPage = new Page<>(pageVO.getIndex(), pageVO.getCount());
        paymentPage = paymentMapper.selectPage(paymentPage, Wrappers.<Payment>lambdaQuery().orderByDesc(Payment::getUpdateTime));
        List<Payment> records = paymentPage.getRecords();
        if (records == null) {
            records = new ArrayList<>();
        }
        List<PaymentVO> paymentVOList = records.parallelStream()
                .map(payment -> cn.hutool.core.bean.BeanUtil.copyProperties(payment, PaymentVO.class))
                .collect(Collectors.toList());
        pageVO.setMaxIndex(paymentPage.getPages());
        pageVO.setList(paymentVOList);
        pageVO.setMaxSize(paymentPage.getTotal());

        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }

    private void set(Payment payment) {
        valueOperations.set(REDIS_USER_CREDIT_KEY_PREFIX + payment.getPayId(), payment);
    }
}
