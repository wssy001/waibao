local key = KEYS[1] .. ARGV[1]
local payment = {}
for _ , value in pairs(redis.call('HKEYS' , key)) do
    payment[value] = redis.call('HGET' , key , value)
end

return cjson.encode(payment)