--canalSyncOrderUserScript OrderUserCacheService
local key = KEYS[1]
local orderUser
local userId
local orderId
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
for _ , redisCommand in pairs(cjson.decode(ARGV[1])) do
    orderUser = redisCommand['value']
    userId = orderUser['user_id']
    orderId = orderUser['order_id']
    if (redisCommand['command'] == 'INSERT' or redisCommand['command'] == 'UPDATE') then
        for index , value in pairs(orderUser) do
            redis.call('HSET' , key .. userId .. orderId , index , tostring(value))
        end
    else
        redis.call('DEL' , key .. userId .. orderId)
    end
end