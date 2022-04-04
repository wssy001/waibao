local key = KEYS[1]
local now = tonumber(redis.call('TIME')[1])
local seckillEndTime
for _ , goods in pairs(cjson.decode(ARGV[1])) do
    seckillEndTime = tonumber(goods['seckillEndTime'])
    redis.call('HSET' , key , tostring(goods['goodsId']) , tostring(now < seckillEndTime))
end