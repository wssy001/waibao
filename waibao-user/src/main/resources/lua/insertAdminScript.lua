local key = KEYS[1]
local admin = cjson.decode(ARGV[1])
local adminId = admin['id']
for index , value in pairs(admin) do
    redis.call('HSET' , key .. adminId , index , tostring(value))
end