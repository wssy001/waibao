---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 09:16
---
-- getUserExtraScript UserExtraCacheService
local key = KEYS[1]
local userId = ARGV[1]
local userExtra = {}
local userExtraKeys = redis.call('HVALS' , key .. userId)
for _ , value in pairs(userExtraKeys) do
    userExtra[value] = redis.call('HGET' , key .. userId , value)
end

return cjson.encode(userExtra)