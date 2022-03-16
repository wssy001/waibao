---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 18:12
---
-- checkLogOrderGoodsOperation LogOrderGoodsCacheService
local key = KEYS[1]
local count = tonumber(redis.call('LREM' , key , 0 , ARGV[1] .. '-' .. ARGV[2]))
if count > 0 then
    redis.call('LPUSH' , key , ARGV[1] .. '-' .. ARGV[2])
    return true
else
    return false
end