---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by alexpetertyler.
--- DateTime: 2022/3/16 10:53
---
-- batchCheckUserScript RiskUserCacheService
local key = KEYS[1]
local riskUserVOList = {}
local riskUserVO
for _, value in pairs(ARGV) do
    riskUserVO = cjson.decode(value)
    local count = tonumber(redis.call('SISMEMBER', key .. riskUserVO["goodsId"], riskUserVO["userId"]))
    if count == 1 then
        table.insert(riskUserVOList, riskUserVO)
    end
end
if not next(riskUserVOList) then
    return nil
else
    return cjson.encode(riskUserVOList)
end