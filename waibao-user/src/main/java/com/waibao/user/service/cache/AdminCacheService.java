package com.waibao.user.service.cache;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.Admin;
import com.waibao.user.mapper.AdminMapper;
import com.waibao.user.service.db.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * AdminService
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCacheService {
    public static final String REDIS_ADMIN_KEY_PREFIX = "admin-";

    private final AdminMapper adminMapper;
    private final AdminService adminService;
    private final RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, Admin> adminRedisTemplate;

    private RBloomFilter<Long> bloomFilter;
    private LongAdder longAdder;
    private ValueOperations<String, Admin> valueOperations;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("adminList");
        bloomFilter.tryInit(1000L, 0.03);
        Long count = adminMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 1000 + 1);
        valueOperations = adminRedisTemplate.opsForValue();
    }

    public Admin get(Long id) {
        Admin admin = valueOperations.get(REDIS_ADMIN_KEY_PREFIX + id);
        if (admin == null) {
            admin = adminService.getById(id);
            if (admin == null) admin = new Admin();
            valueOperations.set(REDIS_ADMIN_KEY_PREFIX + id, admin, 15 * 60, TimeUnit.SECONDS);
        }

        return admin;
    }

    public void set(Admin admin) {
        valueOperations.set(REDIS_ADMIN_KEY_PREFIX + admin.getId(), admin);
    }

    public boolean checkAdmin(Long id) {
        if (!bloomFilter.contains(id)) return false;
        return get(id).getId() != null;
    }

    @Scheduled(fixedDelay = 60 * 1000L)
    public void storeUser() {
        log.info("******AdminCacheService：开始读取数据库放入缓存");
        long l = longAdder.longValue();
        if (l > 0) {
            IPage<Admin> adminPage = new Page<>(l, 1000);
            adminPage = adminMapper.selectPage(adminPage, null);

            adminPage.getRecords()
                    .parallelStream()
                    .forEach(this::set);
            longAdder.decrement();
        }
        log.info("******AdminCacheService：读取数据库放入缓存结束");
    }
}
