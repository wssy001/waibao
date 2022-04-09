---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/18 23:29
---
-- canalSyncPaymentScript PaymentCacheService
local key = KEYS[1]
local redisCommand
local payment
ARGV[1] = string.gsub(ARGV[1] , '("id":)(%s*)(%d+)' , '%1"%3"')
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
for _ , value in pairs(cjson.decode(ARGV[1])) do
    redisCommand = cjson.decode(value)
    payment = redisCommand['value']
    key = '"' .. string.gsub(key , '"' , '') .. payment['payId'] .. '"'
    if (redisCommand['command'] == 'INSERT' or redisCommand['command'] == 'UPDATE') then
        redis.call('SET' , key , cjson.encode(payment))
    else
        redis.call('DEL' , key)
    end
end