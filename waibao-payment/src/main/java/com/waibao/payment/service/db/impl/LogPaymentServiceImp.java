package com.waibao.payment.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.mapper.LogPaymentMapper;
import com.waibao.payment.service.db.LogPaymentService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-04-05
 */
@Service
public class LogPaymentServiceImp extends ServiceImpl<LogPaymentMapper, LogPayment> implements LogPaymentService {

}
