local admin = {}
local key = KEYS[1]
local adminId = ARGV[1]
local adminKeys = redis.call('HKEYS' , key .. adminId)
for _ , value in pairs(adminKeys) do
    admin[value] = redis.call('HGET' , key .. adminId , tostring(value))
end

return cjson.encode(admin)