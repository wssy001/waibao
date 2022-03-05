package com.waibao.user.service.db.impl;

import com.waibao.user.entity.Admin;
import com.waibao.user.mapper.AdminMapper;
import com.waibao.user.service.db.AdminService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 管理员表 服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Service
public class AdminServiceImp extends ServiceImpl<AdminMapper, Admin> implements AdminService {

}
