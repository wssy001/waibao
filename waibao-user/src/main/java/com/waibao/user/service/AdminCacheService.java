package com.waibao.user.service;

import com.waibao.user.entity.Admin;
import com.waibao.user.service.db.AdminService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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
    private final RedissonClient redissonClient;

    private RBloomFilter<Long> bloomFilter;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("adminList");
        bloomFilter.tryInit(1000L, 0.03);
    }

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

    public boolean checkAdmin(Long id) {
        if (!bloomFilter.contains(id)) return false;
        return get(id).getId() != null;
    }
}
