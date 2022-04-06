--getOrderUserScript OrderUserCacheService
local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
local retailerId
local orderId
local orderRetailerList = {}
for _ , orderVo in pairs(cjson.decode(ARGV[1])) do
    retailerId = orderVo['retailerId']
    orderId = orderVo['orderId']
    local orderRetailer = {}
    for _ , index in pairs(redis.call('HKEYS' , key .. retailerId .. orderId)) do
        orderRetailer[index] = redis.call('HGET' , key .. retailerId .. orderId , index)
    end
    table.insert(orderRetailerList , orderRetailer)
end

return cjson.encode(orderRetailerList)