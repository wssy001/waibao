--batchGetOrderUserScript StorageDecreaseScheduleService
local orderUserList = {}
for key , _ in pairs(cjson.decode(ARGV[1])) do
    local orderUser = {}
    for _ , index in pairs(redis.call('HKEYS' , KEYS[1] .. key)) do
        orderUser[index] = redis.call('HGET' , KEYS[1] .. key , index)
    end
    if next(orderUser) then
        table.insert(orderUserList , orderUser)
    end
end

return cjson.encode(orderUserList)