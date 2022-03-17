-- canalSyncScript PaymentCacheService
local key = KEYS[1]
local redisCommand
local payment
for index, value in pairs(ARGV) do
    redisCommand = cjson.decode(value)
    payment = redisCommand['value']
    key = '"' .. string.gsub(key, '"', '') .. payment['payId'] .. '"'
    if redisCommand['command'] == 'SET' then
        payment['@type'] = 'com.waibao.payment.entity.Payment'
        redis.call('SET', key, cjson.encode(payment))
    else
        redis.call('DEL', key)
    end
end