package com.waibao.user.service.db.impl;

import com.waibao.user.entity.User;
import com.waibao.user.mapper.UserMapper;
import com.waibao.user.service.db.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户基本信息表 服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Service
public class UserServiceImp extends ServiceImpl<UserMapper, User> implements UserService {

}
