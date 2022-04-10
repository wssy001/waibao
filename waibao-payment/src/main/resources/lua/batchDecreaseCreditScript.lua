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
    orderPrice = orderVO['orderPrice'] * 100
    oldMoney = redis.call('HGET' , key .. userId , 'money') * 100
    orderVO['paid'] = false
    orderVO['operation'] = 'cancel'
    if oldMoney then
        orderVO['oldMoney'] = math.floor(oldMoney) / 100
        money = redis.call('HGET' , key .. userId , 'money') * 100
        if money >= orderPrice then
            money = math.floor(money - orderPrice) / 100
            redis.call('HSET' , key .. userId , 'money' , money)
            orderVO['paid'] = true
            orderVO['money'] = math.floor(money) / 100
            orderVO['status'] = '用户付款成功'
            orderVO['operation'] = 'paid'
        else
            redis.call('HSET' , key .. userId , 'money' , math.floor(money + orderPrice) / 100)
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