---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 10:44
---
-- reachLimitScript PurchasedUserCacheService
local key = KEYS[1]
local userId = tostring(ARGV[1])
local limit = tonumber(ARGV[2])
if tonumber(redis.call('HEXISTS' , key , userId)) == 0 then
    return false
else
    return tonumber(redis.call('HGET' , key , userId)) >= limit
end