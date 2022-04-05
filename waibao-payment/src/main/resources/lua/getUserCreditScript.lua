local key = KEYS[1] .. ARGV[1]
local userCredit = {}
for _ , value in pairs(redis.call('HKEYS' , key)) do
    userCredit[value] = redis.call('HGET' , key , value)
end

return cjson.encode(userCredit)