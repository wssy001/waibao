local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
local orderVOList = {}
local userId
local oldMoney
local orderPrice
for _ , orderVO in pairs(cjson.decode(ARGV[1])) do
    userId = orderVO['userId']
    orderPrice = orderVO['orderPrice']
    oldMoney = tonumber(redis.call('HGET' , key .. userId , 'money'))
    orderVO['oldMoney'] = oldMoney
    if not oldMoney then
        if tonumber(redis.call('HINCRBY' , key .. userId , 'money' , -tonumber(orderPrice))) >= 0 then
            orderVO['paid'] = true
            orderVO['status'] = '用户付款成功'
            orderVO['operation'] = 'paid'
        else
            redis.call('HINCRBY' , key .. userId , 'money' , tonumber(orderPrice))
            orderVO['paid'] = false
            orderVO['status'] = '用户余额不足'
            orderVO['operation'] = 'cancel'
        end
    else
        orderVO['paid'] = false
        orderVO['status'] = '用户账户不存在'
        orderVO['operation'] = 'cancel'
    end
    table.insert(orderVOList , orderVO)
end

return cjson.encode(orderVOList)