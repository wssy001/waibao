--getOrderGoodsScript OrderGoodsCacheService
local key = KEYS[1]
local retailerId = ARGV[1]
local orderId = ARGV[2]
local orderRetailer = {}
for _ , index in pairs(redis.call('HKEYS' , key .. retailerId .. orderId)) do
    orderRetailer[index] = redis.call('HGET' , key .. retailerId .. orderId , index)
end

return cjson.encode(orderRetailer)