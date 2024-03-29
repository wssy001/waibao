---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 10:20
---
-- batchRollBackGoodsRetailerStorage GoodsRetailerCacheService
local key = KEYS[1]
local orderVOList = {}
local goodsId
local retailerId
for _ , orderVO in pairs(cjson.decode(ARGV[1])) do
    goodsId = tostring(orderVO['goodsId'])
    retailerId = tostring(orderVO['retailerId'])
    if not redis.call('HGET' , key .. retailerId .. goodsId , 'storage') then
        orderVO['status'] = '库存回滚失败'
        table.insert(orderVOList , orderVO)
    else
        redis.call('HINCRBY' , key .. retailerId .. goodsId , 'storage' , tonumber(orderVO['count']))
    end
end

return cjson.encode(orderVOList)