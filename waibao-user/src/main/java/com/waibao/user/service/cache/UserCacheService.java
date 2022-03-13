package com.waibao.user.service.cache;

import com.waibao.user.entity.User;
import com.waibao.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.cache.LRUCacheMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * UserService
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Service
@RequiredArgsConstructor
public class UserCacheService {
    public static final String REDIS_USER_KEY_PREFIX = "user-";

    private final UserMapper userMapper;
    private final RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, User> userRedisTemplate;

    private RBloomFilter<Long> bloomFilter;
    private DefaultRedisScript<User> getUser;
    private LRUCacheMap<Long, User> lruCacheMap;
    private DefaultRedisScript<String> insertUser;
    private DefaultRedisScript<String> batchInsertUser;

    @PostConstruct
    public void init() {
        String insertUserScript = "local key = KEYS[1]\n" +
                "local user = cjson.decode(ARGV[1])\n" +
                "redis.call('HSET', key .. user['id'], 'id', user['id'], 'userNo', user['userNo'], 'updateTime',\n" +
                "        user['updateTime'], 'mobile', user['mobile'], 'eamil', user['eamil'], 'password',\n" +
                "        user['password'], 'sex', user['sex'], 'age', user['age'], 'nickname', user['nickname'],\n" +
                "        'createTime', user['createTime'], '@type', 'com.waibao.user.entity.User')";
        String batchInsertUserScript = "local key = KEYS[1]\n" +
                "local user\n" +
                "for _, value in ipairs(ARGV) do\n" +
                "    user = cjson.decode(value)\n" +
                "    redis.call('HSET', key .. user['id'], 'id', user['id'], 'userId', user['userId'], 'updateTime',\n" +
                "            user['updateTime'], 'mobile', user['mobile'], 'eamil', user['eamil'], 'password',\n" +
                "            user['password'], 'sex', user['sex'], 'age', user['age'], 'nickname', user['nickname'],\n" +
                "            'createTime', user['createTime'])\n" +
                "end";
        String getUserScript = "local user = {}\n" +
                "local key = KEYS[1]\n" +
                "local userId = ARGV[1]\n" +
                "local userKeys = redis.call('HVALS', key .. userId)\n" +
                "for _, value in ipairs(userKeys) do\n" +
                "    user[value] = redis.call('HGET', key .. userId, value)\n" +
                "end\n" +
                "\n" +
                "return cjson.encode(user)";
        bloomFilter = redissonClient.getBloomFilter("userList");
        bloomFilter.tryInit(1000000L, 0.03);
        batchInsertUser = new DefaultRedisScript<>(batchInsertUserScript);
        getUser = new DefaultRedisScript<>(getUserScript);
        insertUser = new DefaultRedisScript<>(insertUserScript);
        lruCacheMap = new LRUCacheMap<>(300, 0, 0);
    }

    public User get(Long userId) {
        User user = lruCacheMap.get(userId);
        if (user != null) return user;

        if (!bloomFilter.contains(userId)) return null;

        user = userRedisTemplate.execute(getUser, Collections.singletonList(REDIS_USER_KEY_PREFIX), userId);
        if (user != null) {
            set(user, false);
            return user;
        }

        user = userMapper.selectById(userId);
        if (user != null) set(user);

        return user;
    }

    public void set(User user) {
        set(user, true);
    }

    public void set(User user, boolean updateRedis) {
        Long userId = user.getId();
        lruCacheMap.put(userId, user);
        bloomFilter.add(userId);
        if (updateRedis)
            userRedisTemplate.execute(insertUser, Collections.singletonList(REDIS_USER_KEY_PREFIX), user);
    }

    public boolean checkUser(Long userId) {
        return get(userId) != null;
    }

    public void insertBatch(List<User> userList) {
        userRedisTemplate.execute(batchInsertUser, Collections.singletonList(REDIS_USER_KEY_PREFIX),
                userList.toArray());
    }

}
