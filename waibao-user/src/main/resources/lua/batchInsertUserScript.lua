---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 08:35
---
-- batchInsertUserScript UserCacheService
local key = KEYS[1]
local userId
if not (string.find(ARGV[1] , '"id"') == nil) then
    ARGV[1] = string.gsub(ARGV[1] , '("id":)(%d+)' , '%1"%2"')
end
for _ , user in pairs(cjson.decode(ARGV[1])) do
    userId = user['id']
    for index , value in pairs(user) do
        redis.call('HSET' , key .. userId , index , tostring(value))
    end
end