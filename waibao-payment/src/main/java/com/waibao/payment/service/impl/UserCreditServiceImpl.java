package com.waibao.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.payment.entitiy.UserCredit;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.db.UserCreditService;
import org.springframework.stereotype.Service;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Service
public class UserCreditServiceImpl extends ServiceImpl<UserCreditMapper, UserCredit> implements UserCreditService {
}
