---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 10:25
---
-- batchDecreasePurchasedScript PurchasedUserCacheService
local key = KEYS[1]
local orderVOList = {}
local goodsId
local userId
local count
for _ , orderVO in pairs(cjson.decode(ARGV[1])) do
    goodsId = tostring(orderVO['goodsId'])
    userId = tostring(orderVO['userId'])
    count = tonumber(orderVO['count'])
    if tonumber(redis.call('HEXISTS' , key .. goodsId , userId)) == 0 then
        table.insert(orderVOList , orderVO)
    else
        local current = tonumber(redis.call('HINCRBY' , key .. goodsId , userId , -count))
        if current <= 0 then
            redis.call('HDEL' , key .. goodsId , userId)
        end
    end
end

return cjson.encode(orderVOList)