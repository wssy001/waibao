---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 18:12
---
-- batchCheckLogOrderGoodsOperation LogOrderGoodsCacheService
local key = KEYS[1]
local operation = ARGV[2]
local goodsId
local orderId
local orderVOList = {}
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
for _ , orderVO in pairs(cjson.decode(ARGV[1])) do
    goodsId = orderVO['goodsId']
    orderId = orderVO['orderId']
    if tonumber(redis.call('LREM' , key .. goodsId , 0 , orderId .. '-' .. operation)) > 0 then
        redis.call('LPUSH' , key .. goodsId , 0 , orderId .. '-' .. operation)
    else
        table.insert(orderVOList , orderVO)
    end
end

return cjson.encode(orderVOList);