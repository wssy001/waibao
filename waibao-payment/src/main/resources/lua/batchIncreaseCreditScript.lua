--batchIncreaseCreditScript UserCreditCacheService
local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
local orderVOList = {}
local userId
local oldMoney
local orderPrice
local money
for _ , orderVO in pairs(cjson.decode(ARGV[1])) do
    userId = orderVO['userId']
    orderPrice = tonumber(orderVO['orderPrice'])
    oldMoney = string.format("%.2f" , redis.call('HGET' , key .. userId , 'money'))
    orderVO['paid'] = false
    orderVO['operation'] = 'cancel'
    if oldMoney then
        orderVO['oldMoney'] = oldMoney
        money = string.format("%.2f" , redis.call('HINCRBYFLOAT' , key .. userId , 'money' , orderPrice))
        redis.call('HSET' , key .. userId , 'money' , money)
        orderVO['paid'] = true
        orderVO['money'] = money
        orderVO['status'] = '用户退钱成功'
        orderVO['operation'] = 'money back'
    else
        orderVO['status'] = '用户账户不存在'
        orderVO['oldMoney'] = 0
        orderVO['money'] = 0
    end
    table.insert(orderVOList , orderVO)
end

return cjson.encode(orderVOList)