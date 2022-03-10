-- batchInsertScript UserCacheService
local key = KEYS[1]
local user
for index, value in ipairs(ARGV) do
    user = cjson.decode(value)
    user['@type'] = 'com.waibao.user.entity.User'
    redis.call('SET', key .. user["id"], cjson.encode(user))
end

-- batchInsertScript AdminCacheService
local key = KEYS[1]
local admin
for index, value in ipairs(ARGV) do
    admin = cjson.decode(value)
    admin['@type'] = 'com.waibao.user.entity.Admin'
    redis.call('SET', key .. admin["id"], cjson.encode(admin))
end