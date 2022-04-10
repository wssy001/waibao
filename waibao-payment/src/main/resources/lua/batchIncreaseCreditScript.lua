--batchIncreaseCreditScript UserCreditCacheService
local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
local paymentVOList = {}
local userId
local oldMoney
local orderPrice
local money
for _ , paymentVO in pairs(cjson.decode(ARGV[1])) do
    userId = paymentVO['userId']
    orderPrice = paymentVO['money'] * 100
    oldMoney = redis.call('HGET' , key .. userId , 'money') * 100
    if oldMoney then
        paymentVO['oldMoney'] = math.floor(oldMoney) / 100
        money = redis.call('HGET' , key .. userId , 'money') * 100
        money = math.floor(money + orderPrice) / 100
        redis.call('HSET' , key .. userId , 'money' , money)
        paymentVO['paid'] = false
        paymentVO['money'] = math.floor(money) / 100
        paymentVO['status'] = '用户退款成功'
        paymentVO['operation'] = 'money back'
    else
        paymentVO['status'] = '用户账户不存在'
        paymentVO['oldMoney'] = 0
        paymentVO['money'] = 0
        paymentVO['paid'] = true
        paymentVO['operation'] = 'non exist'
    end
    table.insert(paymentVOList , paymentVO)
end

return cjson.encode(paymentVOList)