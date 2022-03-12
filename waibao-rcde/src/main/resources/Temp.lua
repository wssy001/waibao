-- batchCheckUserScript RiskUserCacheService
local key = KEYS[1]
local riskUserVOList = {}
local riskUserVO
for index, value in ipairs(ARGV) do
    riskUserVO = cjson.decode(value)
    local count = tonumber(redis.call('SISMEMBER', key .. riskUserVO["goodsId"], riskUserVO["userId"]))
    if count == 1 then
        table.insert(riskUserVOList, riskUserVO)
    end
end
if table.maxn(riskUserVOList) == 0 then
    return nil
else
    return cjson.encode(riskUserVOList)
end

-- batchInsertScript RiskUserCacheService
local key = KEYS[1]
local riskUserVOList = {}
local riskUserVO
for index, value in ipairs(ARGV) do
    riskUserVO = cjson.decode(value)
    local count = tonumber(redis.call('SADD', key .. riskUserVO["goodsId"], riskUserVO["userId"]))
    if count == 0 then
        table.insert(riskUserVOList, riskUserVO)
    end
end
if table.maxn(riskUserVOList) == 0 then
    return nil
else
    return cjson.encode(riskUserVOList)
end

-- batchInsertScript RedisRuleCacheService
local key = KEYS[1]
local ruleList = {}
local rule
for index, value in ipairs(ARGV) do
    rule = cjson.decode(value)
    rule['@type'] = 'com.waibao.rcde.entity.Rule'
    local count = tonumber(redis.call('SET', key .. rule["id"], cjson.encode(rule)))
    if count == 0 then
        table.insert(ruleList, rule)
    else
        redis.call('SET', key .. rule["goodsId"], cjson.encode(rule))
    end
end
if table.maxn(ruleList) == 0 then
    return nil
else
    return cjson.encode(ruleList)
end

-- batchGetScript RedisRuleCacheService
local key = KEYS[1]
local ruleList = {}
local ruleStr
for index, value in ipairs(ARGV) do
    ruleStr = tostring(redis.call('GET', key .. value))
    if ruleStr then
        table.insert(ruleList, cjson.decode(ruleStr))
    end
end
if table.maxn(ruleList) == 0 then
    return nil
else
    return cjson.encode(ruleList)
end