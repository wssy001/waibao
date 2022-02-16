package com.waibao.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.user.entity.Admin;
import com.waibao.user.entity.User;
import com.waibao.user.service.db.AdminService;
import com.waibao.user.service.db.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * AdminService
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Service
@RequiredArgsConstructor
public class AdminCacheService {
    private final AdminService adminService;

    @Resource
    private ValueOperations<Long, Admin> valueOperations;

    public Admin get(Long id) {
        Admin admin = valueOperations.get(id);
        if (admin == null) {
            admin = adminService.getById(id);
            if (admin == null) admin = new Admin();
            valueOperations.set(id, admin, 15 * 60, TimeUnit.SECONDS);
        }

        return admin;
    }

    public void set(Admin admin) {
        valueOperations.set(admin.getId(), admin);
    }
}
