---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 11:03
---
-- canalSyncRuleScript RuleCacheService
local key = KEYS[1]
local redisCommand
local rule
local goodsId
for _ , redisCommandData in pairs(ARGV) do
    redisCommand = cjson.decode(redisCommandData)
    rule = redisCommand['value']
    goodsId = rule['goodsId']
    if (redisCommand['command'] == 'INSERT' or redisCommand['command'] == 'UPDATE') then
        for index , value in pairs(rule) do
            redis.call('HSET' , key .. goodsId , index , tostring(value))
        end
    else
        redis.call('DEL' , key .. goodsId)
    end
end