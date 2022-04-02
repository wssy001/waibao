---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 09:10
---
-- batchInsertAdminScript AdminCacheService
local key = KEYS[1]
local adminId
for _ , admin in pairs(cjson.decode(ARGV[1])) do
    adminId = admin['id']
    for index , value in pairs(admin) do
        redis.call('HSET' , key .. adminId , index , tostring(value))
    end
end