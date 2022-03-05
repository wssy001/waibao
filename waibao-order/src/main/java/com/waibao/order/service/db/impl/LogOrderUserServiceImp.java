package com.waibao.order.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.order.entity.LogOrderUser;
import com.waibao.order.mapper.LogOrderUserMapper;
import com.waibao.order.service.db.LogOrderUserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-03-05
 */
@Service
public class LogOrderUserServiceImp extends ServiceImpl<LogOrderUserMapper, LogOrderUser> implements LogOrderUserService {

}
