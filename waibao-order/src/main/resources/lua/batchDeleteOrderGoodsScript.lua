---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 18:10
---
-- batchDeleteOrderGoodsScript OrderGoodsCacheService
local key = KEYS[1]
local orderGoodsList = {}
local orderGoods
for _ , value in pairs(ARGV) do
    orderGoods = cjson.decode(value)
    local count = tonumber(redis.call('DEL' , key .. orderGoods["orderId"]))
    if count == 0 then
        table.insert(orderGoodsList , orderGoods)
    end
end

return cjson.encode(orderGoodsList)