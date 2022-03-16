---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 10:23
---
-- canalSyncLogGoodsScript LogSeckillGoodsCacheService
local key = KEYS[1]
local redisCommand
local logSeckillGoods
local goodsId
local oldLogSeckillGoods
for _, value in pairs(ARGV) do
    redisCommand = cjson.decode(value)
    logSeckillGoods = redisCommand['value']
    goodsId = logSeckillGoods['goodsId']
    key = '"' .. string.gsub(key, '"', '') .. goodsId .. '"'
    if command == 'INSERT' then
        redis.call('LPUSH', key, logSeckillGoods['orderId'] .. '-' .. logSeckillGoods['operation'])
    elseif command == 'UPDATE' then
        oldLogSeckillGoods = redisCommand['oldValue']
        if oldLogSeckillGoods['operation'] ~= nil then
            redis.call('LPUSH', key, logSeckillGoods['orderId'] .. '-' .. logSeckillGoods['operation'])
            redis.call('LREM', key, 0, logSeckillGoods['orderId'] .. '-' .. oldLogSeckillGoods['operation'])
        end
    else
        redis.call('LREM', key, 0, logSeckillGoods['orderId'] .. '-' .. logSeckillGoods['operation'])
    end
end