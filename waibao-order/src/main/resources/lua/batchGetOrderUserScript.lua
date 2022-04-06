--getOrderUserScript OrderUserCacheService
local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
local userId
local orderId
local orderUserList = {}
for _ , orderVo in pairs(cjson.decode(ARGV[1])) do
    userId = orderVo['userId']
    orderId = orderVo['orderId']
    local orderUser = {}
    for _ , index in pairs(redis.call('HKEYS' , key .. userId .. orderId)) do
        orderUser[index] = redis.call('HGET' , key .. userId .. orderId , index)
    end
    table.insert(orderUserList , orderUser)
end

return cjson.encode(orderUserList)