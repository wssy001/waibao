---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 10:13
---
-- insertGoodsRetailerScript GoodsRetailerCacheService
local key = KEYS[1]
local seckillGoods = cjson.decode(ARGV[1])
local retailerId = seckillGoods['retailerId']
local goodsId = seckillGoods['goodsId']
redis.call('HSET' , key .. retailerId .. goodsId , '@type' , 'com.waibao.seckill.entity.SeckillGoods')
for index , value in pairs(seckillGoods) do
    redis.call('HSET' , key .. retailerId .. goodsId , index , value)
end
