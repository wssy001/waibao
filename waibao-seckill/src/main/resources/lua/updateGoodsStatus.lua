local key = KEYS[1]
local goodsId = tostring(ARGV[1])

redis.call('HSET' , key , goodsId , ARGV[2])