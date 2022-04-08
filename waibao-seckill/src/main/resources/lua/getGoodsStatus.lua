local key = KEYS[1]
local goodsId = tostring(ARGV[1])

if not redis.call('HGET' , key , goodsId) then
    return false
end
return true