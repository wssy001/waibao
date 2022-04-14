--batchGetOrderUserScript StorageDecreaseScheduleService
local keyList = cjson.decode(ARGV[1])
local orderUserList = {}
for key , _ in pairs(keyList) do
    local orderUser = {}
    for index , _ in pairs(redis.call('HKEYS' , KEYS[1] .. key)) do
        orderUser[index] = redis.call('HGET' , KEYS[1] .. key , index)
    end
    if next(orderUser) then
        table.insert(orderUserList , orderUser)
    end
end

return cjson.encode(orderUserList)