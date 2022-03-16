---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 11:03
---
-- canalSyncDepositScript DepositCacheService
local key = KEYS[1]
local redisCommand
local deposit
local userId
local id
for _ , redisCommandData in pairs(ARGV) do
    redisCommand = cjson.decode(redisCommandData)
    deposit = redisCommand['value']
    userId = deposit['userId']
    id = deposit['id']
    if (redisCommand['command'] == 'INSERT' or redisCommand['command'] == 'UPDATE') then
        redis.call('SADD' , 'index-' .. key .. userId , id)
        local oldDeposit = redisCommand['oldValue']
        local oldId = oldDeposit['id']
        for index , value in pairs(oldDeposit) do
            redis.call('HSET' , key .. oldId , index , value)
        end
    else
        redis.call('SMOVE' , 'index-' .. key .. userId , id)
        redis.call('DEL' , key .. id)
    end
end