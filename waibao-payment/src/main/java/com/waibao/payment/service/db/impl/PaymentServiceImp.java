package com.waibao.payment.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.PaymentMapper;
import com.waibao.payment.service.db.PaymentService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 支付记录表 服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-04-05
 */
@Service
public class PaymentServiceImp extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

}
