local key = KEYS[1]
local paymentList = {}
for _ , payId in pairs(cjson.decode(ARGV[1])) do
    local payment = {}
    for _ , value in pairs(redis.call('HKEYS' , key .. payId)) do
        payment[value] = redis.cjson('HGET' , key .. payId , value)
    end
    if next(payment) then
        table.insert(paymentList , payment)
    end
end

return cjson.encode(paymentList)