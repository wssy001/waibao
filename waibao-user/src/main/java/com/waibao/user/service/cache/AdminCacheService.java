package com.waibao.user.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waibao.user.entity.Admin;
import com.waibao.user.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
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
    public static final String REDIS_ADMIN_KEY_PREFIX = "admin-";

    private final AdminMapper adminMapper;
    private final RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, Admin> adminRedisTemplate;

    private Cache<Long, Admin> adminCache;
    private RBloomFilter<Long> bloomFilter;
    private RedisScript<Admin> getAdmin;
    private RedisScript<String> insertAdmin;
    private RedisScript<String> batchInsertAdmin;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("adminList");
        bloomFilter.tryInit(1000000L, 0.03);
        getAdmin = RedisScript.of(new ClassPathResource("lua/getAdminScript.lua"), Admin.class);
        insertAdmin = RedisScript.of(new ClassPathResource("lua/insertAdminScript.lua"), String.class);
        batchInsertAdmin = RedisScript.of(new ClassPathResource("lua/batchInsertAdminScript.lua"), String.class);

        adminCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public Admin get(Long adminId) {
        Admin admin = adminCache.getIfPresent(adminId);
        if (admin != null) return admin;

        if (!bloomFilter.contains(adminId)) return null;

        admin = adminRedisTemplate.execute(getAdmin, Collections.singletonList(REDIS_ADMIN_KEY_PREFIX), adminId);
        if (admin != null) {
            set(admin, false);
            return admin;
        }

        admin = adminMapper.selectById(adminId);
        if (admin != null) set(admin);

        return admin;
    }

    public void set(Admin user) {
        set(user, true);
    }

    public void set(Admin user, boolean updateRedis) {
        Long userId = user.getId();
        adminCache.put(userId, user);
        bloomFilter.add(userId);
        if (updateRedis)
            adminRedisTemplate.execute(insertAdmin, Collections.singletonList(REDIS_ADMIN_KEY_PREFIX), user);
    }

    public boolean checkAdmin(Long userId) {
        return get(userId) != null;
    }

    public void insertBatch(List<Admin> userList) {
        adminRedisTemplate.execute(batchInsertAdmin, Collections.singletonList(REDIS_ADMIN_KEY_PREFIX),
                userList.toArray());
    }

}
