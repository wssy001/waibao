---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/18 23:29
---
-- canalSyncLogUserCreditScript LogUserCreditCacheService
local key = KEYS[1]
local logUserCredit
local payId
local operation
for _ , redisCommand in pairs(cjson.decode(ARGV[1])) do
    logUserCredit = redisCommand['value']
    payId = logUserCredit['payId']
    operation = logUserCredit['operation']
    redis.call('SADD' , key , payId .. '-' .. operation)
end