package com.waibao.payment.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.PaymentMapper;
import com.waibao.payment.service.db.PaymentService;
import org.springframework.stereotype.Service;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {
}
