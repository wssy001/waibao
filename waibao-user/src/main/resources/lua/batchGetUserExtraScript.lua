---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 09:20
---
-- batchGetUserExtraScript UserExtraCacheService
local key = KEYS[1]
local userExtraList = {}
local userExtraKeys
for _ , userId in pairs(cjson.decode(ARGV[1])) do
    userExtraKeys = redis.call('HKEYS' , key .. userId)
    local userExtra = {}
    for _ , value2 in pairs(userExtraKeys) do
        userExtra[value2] = redis.call('HGET' , key .. userId , '"' .. value2)
    end

    if userExtra['id'] ~= nil then
        table.insert(userExtraList , userExtra)
    end
end

return cjson.encode(userExtraList)