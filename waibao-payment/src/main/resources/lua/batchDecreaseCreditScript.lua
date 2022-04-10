--batchDecreaseCreditScript UserCreditCacheService
local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
local orderVOList = {}
local userId
local oldMoney
local orderPrice
local money
for _ , orderVO in pairs(cjson.decode(ARGV[1])) do
    userId = orderVO['userId']
    orderPrice = orderVO['orderPrice']
    oldMoney = redis.call('HGET' , key .. userId , 'money')
    orderVO['paid'] = false
    orderVO['operation'] = 'cancel'
    if oldMoney then
        orderVO['oldMoney'] = oldMoney
        money = redis.call('HINCRBYFLOAT' , key .. userId , 'money' , -orderPrice)
        if money >= 0 then
            money = string.format("%.2f" , redis.call('HGET' , key .. userId , 'money'))
            orderVO['paid'] = true
            orderVO['money'] = money
            orderVO['status'] = '用户付款成功'
            orderVO['operation'] = 'paid'
        else
            money = redis.call('HINCRBYFLOAT' , key .. userId , 'money' , orderPrice)
            orderVO['money'] = oldMoney
            orderVO['status'] = '用户余额不足'
        end
    else
        orderVO['status'] = '用户账户不存在'
        orderVO['oldMoney'] = 0
        orderVO['money'] = 0
    end
    table.insert(orderVOList , orderVO)
end

return cjson.encode(orderVOList)