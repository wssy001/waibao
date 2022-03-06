-- canalSyncScript PaymentCacheService
for index, value in ipairs(ARGV) do
    local redisCommand = cjson.decode(value)
    local payment = redisCommand['value']
    key = '"' .. string.gsub(key, '"', '') .. payment['payId'] .. '"'
    if redisCommand['command'] == 'SET' then
        payment['@type'] = 'com.waibao.payment.entity.Payment'
        redis.call('SET', key, cjson.encode(payment))
    else
        redis.call('DEL', key)
    end
end