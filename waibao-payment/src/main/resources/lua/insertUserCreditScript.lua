local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
local userCredit = cjson.decode(ARGV[1])
for index , value in pairs(userCredit) do
    if index == 'money' then
        value = string.format("%.2f" , value)
    end
    redis.call('HSET' , key .. userCredit['userId'] , index , value)
end