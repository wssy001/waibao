-- batchInsertUserScript UserCacheService
--private Long id;
--private Long userNo;
--private String mobile;
--private String eamil;
--private String password;
--private Integer sex;
--private Integer age;
--private String nickname;
--private Date createTime;
--private Date updateTime;

local key = KEYS[1]
local user
for _, value in ipairs(ARGV) do
    user = cjson.decode(value)
    redis.call('HSET', key .. user['id'], 'id', user['id'], 'userNo', user['userNo'], 'updateTime',
            user['updateTime'], 'mobile', user['mobile'], 'eamil', user['eamil'], 'password',
            user['password'], 'sex', user['sex'], 'age', user['age'], 'nickname', user['nickname'],
            'createTime', user['createTime'], '@type', 'com.waibao.user.entity.User')
end


-- getUserScript UserCacheService
local user = {}
local key = KEYS[1]
local userId = ARGV[1]
local userKeys = redis.call('HVALS', key .. userId)
for _, value in ipairs(userKeys) do
    user[value] = redis.call('HGET', key .. userId, value)
end

return cjson.encode(user)


-- insertUserScript UserCacheService
local key = KEYS[1]
local user = cjson.decode(ARGV[1])
redis.call('HSET', key .. user['id'], 'id', user['id'], 'userNo', user['userNo'], 'updateTime',
        user['updateTime'], 'mobile', user['mobile'], 'eamil', user['eamil'], 'password',
        user['password'], 'sex', user['sex'], 'age', user['age'], 'nickname', user['nickname'],
        'createTime', user['createTime'], '@type', 'com.waibao.user.entity.User')


-- batchInsertAdminScript AdminCacheService
--private Long id;
--private String name;
--private String password;
--private Integer level;
--private Date createTime;
--private Date updateTime;
local admin
local key = KEYS[1]
for _, value in ipairs(ARGV) do
    admin = cjson.decode(value)
    redis.call('HSET', key .. admin['id'], 'id', admin['id'], 'updateTime', admin['updateTime'], 'password',
            admin['password'], 'level', admin['level'], 'createTime', admin['createTime'],
            '@type', 'com.waibao.user.entity.Admin')
end


-- getAdminScript AdminCacheService
local admin = {}
local key = KEYS[1]
local adminId = ARGV[1]
local adminKeys = redis.call('HVALS', key .. adminId)
for _, value in ipairs(adminKeys) do
    admin[value] = redis.call('HGET', key .. adminId, value)
end

return cjson.encode(admin)


-- insertAdminScript AdminCacheService
local key = KEYS[1]
local admin = cjson.decode(ARGV[1])
redis.call('HSET', key .. admin['id'], 'id', admin['id'], 'updateTime', admin['updateTime'], 'password',
        admin['password'], 'level', admin['level'], 'createTime', admin['createTime'],
        '@type', 'com.waibao.user.entity.Admin')