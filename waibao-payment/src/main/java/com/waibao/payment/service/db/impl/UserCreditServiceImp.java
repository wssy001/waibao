package com.waibao.payment.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.db.UserCreditService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 账户信息表 服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-04-05
 */
@Service
public class UserCreditServiceImp extends ServiceImpl<UserCreditMapper, UserCredit> implements UserCreditService {

}
