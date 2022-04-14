local key = KEYS[1]
ARGV[1] = string.gsub(ARGV[1] , '("id":)(%s*)(%d+)' , '%1"%3"')
ARGV[1] = string.gsub(ARGV[1] , '("userId":)(%s*)(%d+)' , '%1"%3"')
for _ , payment in pairs(cjson.decode(ARGV[1])) do
    for index , value in pairs(payment) do
        redis.call('HSET' , key .. payment['payId'] , index , value)
    end
end