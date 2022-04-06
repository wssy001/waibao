--getOrderGoodsScript OrderGoodsCacheService
local key = KEYS[1]
local userId = ARGV[1]
local orderId = ARGV[2]
local orderUser = {}
for _ , index in pairs(redis.call('HKEYS' , key .. userId .. orderId)) do
    orderUser[index] = redis.call('HGET' , key .. userId .. orderId , index)
end

return cjson.encode(orderUser)