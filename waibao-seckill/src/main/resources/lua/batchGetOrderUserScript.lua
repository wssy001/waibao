--batchGetOrderUserScript StorageDecreaseScheduleService
local paiIdList = cjson.decode(ARGV[1])
local orderUserList = {}
local payId
for _ , key in pairs(redis.call('KEYS' , KEYS[1] .. '*')) do
    payId = redis.call('HGET' , key , 'payId')
    if not paiIdList[payId] then
        local orderUser = {}
        for _ , index in pairs(redis.call('HKEYS' , key)) do
            orderUser[index] = redis.call('HGET' , key , index)
        end
        if not next(orderUserList) then
            table.insert(orderUserList , orderUser)
        end
    end
end

return cjson.encode(orderUserList)