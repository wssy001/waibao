---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 17:31
---
-- canalSyncOrderUserScript OrderUserCacheService
local key = KEYS[1]
for _ , value in pairs(ARGV) do
    local redisCommand = cjson.decode(value)
    local orderUser = redisCommand['value']
    key = '"' .. string.gsub(key, '"', '') .. orderUser['userId'] .. '"'
    if redisCommand['command'] == 'SET' then
        orderUser['@type'] = 'com.waibao.order.entity.OrderUser'
        redis.call('SET', key, cjson.encode(orderUser))
    else
        redis.call('DEL', key)
    end
end