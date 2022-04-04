local key = KEYS[1]
local goodsId = tostring(ARGV[1])

if not redis.call('HEGT' , key , goodsId) then
    return false
end
return true