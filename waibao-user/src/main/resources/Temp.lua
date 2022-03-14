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


-- getUserExtraScript UserExtraCacheService
--private Long id;
--private Long userId;
--private Boolean defaulter;
--private Integer age;
--private String workStatus;
--private Date createTime;
--private Date updateTime;
local key = KEYS[1]
local userId = ARGV[1]
local userExtra = {}
local userExtraKeys = redis.call('HVALS', key .. userId)
for _, value in ipairs(userExtraKeys) do
    userExtra[value] = redis.call('HGET', key .. userId, value)
end

return cjson.encode(userExtra)


-- batchInsertUserExtraScript UserExtraCacheService
local key = KEYS[1]
local userExtra
for _, value in ipairs(ARGV) do
    userExtra = cjson.decode(value)
    redis.call('HSET', key .. userExtra['userId'], 'id', userExtra['id'], 'updateTime', userExtra['updateTime'],
            'userId', userExtra['userId'], 'defaulter', userExtra['defaulter'], 'age', userExtra['age'],
            'workStatus', userExtra['workStatus'], 'createTime', userExtra['createTime'],
            '@type', 'com.waibao.user.entity.UserExtra')
end


-- getBatchUserExtraScript UserExtraCacheService
local key = KEYS[1]
local userExtraList = {}
local userExtraKeys
for _, userId in ipairs(ARGV) do
    userExtraKeys = redis.call('HVALS', key .. userId)
    local userExtra = {}
    for _, value2 in ipairs(userExtraKeys) do
        userExtra[value2] = redis.call('HGET', key .. userId, value2)
    end

    if userExtra['id'] ~= nil then
        table.insert(userExtraList, userExtra)
    end
end

return cjson.encode(userExtraList)


--insertUserExtraScript UserExtraCacheService
local key = KEYS[1]
local userExtra = cjson.decode(value)
redis.call('HSET', key .. userExtra['userId'], 'id', userExtra['id'], 'updateTime', userExtra['updateTime'],
        'userId', userExtra['userId'], 'defaulter', userExtra['defaulter'], 'age', userExtra['age'],
        'workStatus', userExtra['workStatus'], 'createTime', userExtra['createTime'],
        '@type', 'com.waibao.user.entity.UserExtra')
