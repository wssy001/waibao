---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/18 23:37
---
-- batchCheckLogPaymentOperationScript LogPaymentCacheService
local key = KEYS[1]
local operation = ARGV[2]
local payId
local goodsId
local paymentVOList = {}
for _ , paymentVO in pairs(cjson.decode(ARGV[1])) do
    payId = paymentVO['payId']
    goodsId = paymentVO['goodsId']
    if tonumber(redis.call('LREM' , key .. goodsId , 0 , payId .. '-' .. operation)) > 0 then
        redis.call('LPUSH' , key .. goodsId , payId .. '-' .. operation)
    else
        table.insert(paymentVOList , paymentVO)
    end
end

return cjson.encode(paymentVOList)