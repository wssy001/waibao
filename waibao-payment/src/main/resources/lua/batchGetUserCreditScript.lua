local key = KEYS[1]
local userCreditList = {}
for _ , userId in pairs(cjson.decode(ARGV[1])) do
    local userCredit = {}
    for _ , value in pairs(redis.call('HKEYS' , key .. userId)) do
        userCredit[value] = redis.cjson('HGET' , key .. userId , value)
    end
    if next(userCredit) then
        table.insert(userCreditList , userCredit)
    end
end

return cjson.encode(userCreditList)