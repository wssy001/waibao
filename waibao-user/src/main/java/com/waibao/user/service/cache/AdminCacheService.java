package com.waibao.user.service.cache;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.Admin;
import com.waibao.user.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.cache.LRUCacheMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
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
    private final RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, Admin> adminRedisTemplate;

    private LongAdder longAdder;
    private RBloomFilter<Long> bloomFilter;
    private DefaultRedisScript<Admin> getAdmin;
    private LRUCacheMap<Long, Admin> lruCacheMap;
    private DefaultRedisScript<String> insertAdmin;
    private DefaultRedisScript<String> batchInsertAdmin;

    @PostConstruct
    public void init() {
        String insertAdminScript = "local key = KEYS[1]\n" +
                "local admin = cjson.decode(ARGV[1])\n" +
                "redis.call('HSET', key .. admin['id'], 'id', admin['id'], 'updateTime', admin['updateTime'], 'password',\n" +
                "        admin['password'], 'level', admin['level'], 'createTime', admin['createTime'],\n" +
                "        '@type', 'com.waibao.user.entity.Admin')";
        String batchInsertAdminScript = "local admin\n" +
                "local key = KEYS[1]\n" +
                "for _, value in ipairs(ARGV) do\n" +
                "    admin = cjson.decode(value)\n" +
                "    redis.call('HSET', key .. admin['id'], 'id', admin['id'], 'updateTime', admin['updateTime'], 'password',\n" +
                "            admin['password'], 'level', admin['level'], 'createTime', admin['createTime'],\n" +
                "            '@type', 'com.waibao.user.entity.Admin')\n" +
                "end";
        String getAdminScript = "local admin={}\n" +
                "local key = KEYS[1]\n" +
                "local adminId = ARGV[1]\n" +
                "local adminKeys = redis.call('HVALS', key .. adminId)\n" +
                "for _, value in ipairs(adminKeys) do\n" +
                "    admin[value] = redis.call('HGET', key .. adminId, value)\n" +
                "end\n" +
                "\n" +
                "return cjson.encode(admin)";
        bloomFilter = redissonClient.getBloomFilter("adminList");
        bloomFilter.tryInit(1000000L, 0.03);
        Long count = adminMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 1000 + 1);
        batchInsertAdmin = new DefaultRedisScript<>(batchInsertAdminScript);
        getAdmin = new DefaultRedisScript<>(getAdminScript);
        insertAdmin = new DefaultRedisScript<>(insertAdminScript);
        lruCacheMap = new LRUCacheMap<>(300, 0, 0);
    }

    public Admin get(Long adminId) {
        Admin admin = lruCacheMap.get(adminId);
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
        lruCacheMap.put(userId, user);
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

    @Scheduled(fixedDelay = 60 * 1000L)
    public void storeAdmin() {
        log.info("******AdminCacheService：开始读取数据库放入缓存");
        long l = longAdder.longValue();
        if (l > 0) {
            IPage<Admin> adminPage = new Page<>(l, 1000);
            adminPage = adminMapper.selectPage(adminPage, null);
            insertBatch(adminPage.getRecords());
            longAdder.decrement();
        }
        log.info("******AdminCacheService：读取数据库放入缓存结束");
    }
}
