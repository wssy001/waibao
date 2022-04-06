package com.waibao.user.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.user.entity.Admin;
import com.waibao.user.mapper.AdminMapper;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final AsyncService asyncService;

    @Resource
    private RedisTemplate<String, Admin> adminRedisTemplate;

    private Cache<Long, Admin> adminCache;
    private BloomFilter<Long> bloomFilter;
    private RedisScript<String> getAdmin;
    private RedisScript<String> insertAdmin;
    private RedisScript<String> batchInsertAdmin;

    @PostConstruct
    public void init() {
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 100, 0.01);
        getAdmin = RedisScript.of(new ClassPathResource("lua/getAdminScript.lua"), String.class);
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

        String execute = adminRedisTemplate.execute(getAdmin, Collections.singletonList(REDIS_ADMIN_KEY_PREFIX), adminId + "");
        if (!"{}".equals(execute)) admin = JSON.parseObject(execute, Admin.class);
        if (admin != null) {
            set(admin, false);
            return admin;
        }

        if (!bloomFilter.mightContain(adminId)) return null;

        admin = adminMapper.selectById(adminId);
        if (admin != null) set(admin);

        return admin;
    }

    public void set(Admin admin) {
        set(admin, true);
    }

    public void set(Admin admin, boolean updateRedis) {
        Long userId = admin.getId();
        adminCache.put(userId, admin);
        bloomFilter.put(userId);
        if (updateRedis)
            adminRedisTemplate.execute(insertAdmin, Collections.singletonList(REDIS_ADMIN_KEY_PREFIX), JSON.toJSONString(admin));
    }

    public boolean checkAdmin(Long userId) {
        return get(userId) != null;
    }

    public void insertBatch(List<Admin> adminList) {
        asyncService.basicTask(() -> {
            Map<Long, Admin> collect = adminList.parallelStream()
                    .peek(admin -> bloomFilter.put(admin.getId()))
                    .collect(Collectors.toMap(Admin::getId, Function.identity()));
            adminCache.asMap()
                    .putAll(collect);
        });
        asyncService.basicTask(() -> adminRedisTemplate.execute(batchInsertAdmin, Collections.singletonList(REDIS_ADMIN_KEY_PREFIX), JSONArray.toJSONString(adminList)));
    }

}
