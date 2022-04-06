--canalSyncOrderRetailerScript OrderRetailerCacheService
local key = KEYS[1]
local orderRetailer
local retailerId
local orderId
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
for _ , redisCommand in pairs(cjson.decode(ARGV[1])) do
    orderRetailer = redisCommand['value']
    retailerId = orderRetailer['retailer_id']
    orderId = orderRetailer['order_id']
    if (redisCommand['command'] == 'INSERT' or redisCommand['command'] == 'UPDATE') then
        for index , value in pairs(orderRetailer) do
            redis.call('HSET' , key .. retailerId .. orderId , index , tostring(value))
        end
    else
        redis.call('DEL' , key .. retailerId .. orderId)
    end
end