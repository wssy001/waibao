package com.waibao.payment.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.payment.entity.LogUserCredit;
import com.waibao.payment.mapper.LogUserCreditMapper;
import com.waibao.payment.service.db.LogUserCreditService;
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
public class LogUserCreditServiceImp extends ServiceImpl<LogUserCreditMapper, LogUserCredit> implements LogUserCreditService {

}
