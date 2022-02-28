package com.waibao.order.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.order.entity.OrderUser;
import com.waibao.order.mapper.OrderUserMapper;
import com.waibao.order.service.db.OrderUserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-02-28
 */
@Service
public class OrderUserServiceImp extends ServiceImpl<OrderUserMapper, OrderUser> implements OrderUserService {

}
