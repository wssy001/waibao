--getOrderUserScript OrderUserCacheService
local retailerId = ARGV[1]
local orderRetailerList = {}
for _ , key in ipairs(redis.call('KEYS' , KEYS[1] .. retailerId .. '*')) do
    local orderRetailer = {}
    for _ , index in pairs(redis.call('HKEYS' , key)) do
        orderRetailer[index] = redis.call('HGET' , key , index)
    end
    if next(orderRetailer) then
        table.insert(orderRetailerList , orderRetailer)
    end
end

return cjson.encode(orderRetailerList)