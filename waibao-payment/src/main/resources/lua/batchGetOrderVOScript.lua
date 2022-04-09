local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1], '("userId":)(%s*)(%d+)', '%1"%3"')
local userId
local orderId
local payId
local orderVOList = {}
for _, paymentVO in pairs(cjson.decode(ARGV[1])) do
    userId = paymentVO['userId']
    orderId = paymentVO['orderId']
    payId = paymentVO['payId']
    local orderVO = {}
    for _, index in pairs(redis.call('HKEYS', key .. userId .. orderId)) do
        orderVO[index] = redis.call('HGET', key .. userId .. orderId, index)
    end
    if next(orderVO) then
        table.insert(orderVOList, orderVO)
    end
end

return cjson.encode(orderVOList)