---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 09:55
---
-- insertGoodsScript SeckillGoodsCacheService
local key = KEYS[1]
local seckillGoods = cjson.decode(ARGV[1])
local goodsId = tostring(seckillGoods['goodsId'])
for index , value in pairs(seckillGoods) do
    redis.call('HSET' , key .. goodsId , index , tostring(value))
end