local mysqldb = require "resty.mysql"
local times
mysql , err = mysqldb:new()
if not mysql then
    ngx.log(ngx.ERR , "mysql初始化失败:" , err)
end

mysql:set_timeout(1000)
times , err = mysql:get_reused_times()

if not times or times == 0 then
    ok , err , errcode , sqlstate = mysql:connect {
        host = "10.61.20.211",
        port = 33306,
        database = "waibao_v2",
        user = "root",
        password = "wssy001",
        charset = "utf8",
        max_packet_size = 1024 * 1024,
    }

    if err then
        ngx.log(ngx.ERR , "mysql连接失败:" , err)
    end

    if ok then
        ngx.say(cjson.encode(ok))
    end

end

local result = table.new(0 , 4)
result['time'] = ngx.localtime()
result['timestamp'] = ngx.time()

ngx.req.read_body()
local bodyJson = ngx.req.get_body_data()
bodyJson = string.gsub(bodyJson , '("userId":)(%s*)(%d+)' , '%1"%3"')
local riskUserVO = cjson.decode(bodyJson)
--goodsId
--userId

local redisKey = table.new(2 , 0)
redisKey[1] = 'risk-user-'
redisKey[2] = tostring(riskUserVO['goodsId'])
local temp = table.concat(redisKey , '')

function checkRiskUser(bodyJson)
    return redis:eval([[
        local riskUserVO = cjson.decode(ARGV[1])
        redis.call('SELECT' , 15)
        return tonumber(redis.call('SISMEMBER' , KEYS[1] .. riskUserVO["goodsId"] , riskUserVO["userId"])) == 1
    ]] , 1 , 'risk-user-' , bodyJson)
end

function addRiskUser()
    return redis:eval([[
        local riskUserVO = cjson.decode(ARGV[1])
        redis.call('SELECT' , 15)
        redis.call('SISMEMBER' , KEYS[1] .. riskUserVO["goodsId"] , riskUserVO["userId"])
    ]] , 1 , 'risk-user-' , bodyJson)
end

function checkRiskUserFromBloomFilter(goodsId , userId)
    local checkBloomFilterScript = [[
        local entries = ARGV[2]
        local precision = ARGV[3]
        local count = redis.call('GET' , ARGV[1] .. ':count')

        if not count then
            return 0
        end

        local factor = math.ceil((entries + count) / entries)
        local index = math.ceil(math.log(factor) / 0.69314718055995)
        local scale = math.pow(2 , index - 1) * entries

        local hash = redis.sha1hex(ARGV[4])

        local h = { }
        h[0] = tonumber(string.sub(hash , 1 , 8) , 16)
        h[1] = tonumber(string.sub(hash , 9 , 16) , 16)
        h[2] = tonumber(string.sub(hash , 17 , 24) , 16)
        h[3] = tonumber(string.sub(hash , 25 , 32) , 16)

        local maxbits = math.floor((scale * math.log(precision * math.pow(0.5 , index))) / -0.4804530139182)

        local maxk = math.floor(0.69314718055995 * maxbits / scale)
        local b = { }

        for i = 1 , maxk do
            table.insert(b , h[i % 2] + i * h[2 + (((i + (i % 2)) % 4) / 2)])
        end

        for n = 1 , index do
            local key = ARGV[1] .. ':' .. n
            local found = true
            local scalen = math.pow(2 , n - 1) * entries

            local bits = math.floor((scalen * math.log(precision * math.pow(0.5 , n))) / -0.4804530139182)

            local k = math.floor(0.69314718055995 * bits / scalen)

            for i = 1 , k do
                if redis.call('GETBIT' , key , b[i] % bits) == 0 then
                    found = false
                    break
                end
            end

            if found then
                return true
            end
        end

        return false
    ]]

    return redis:eval(checkBloomFilterScript , 0 , 'bloom-filter-risk-user' , 100000 , 0.001 , goodsId .. userId)
end

function addToBloomFilter(goodsId , userId)
    local addToBloomFilterScript = [[
        local entries = ARGV[2]
        local precision = ARGV[3]
        local hash = redis.sha1hex(ARGV[4])
        local countkey = ARGV[1] .. ':count'
        local count = redis.call('GET' , countkey)
        if not count then
            count = 1
        else
            count = count + 1
        end

        local factor = math.ceil((entries + count) / entries)
        local index = math.ceil(math.log(factor) / 0.69314718055995)
        local scale = math.pow(2 , index - 1) * entries
        local key = ARGV[1] .. ':' .. index

        local bits = math.floor(-(scale * math.log(precision * math.pow(0.5 , index))) / 0.4804530139182)

        local k = math.floor(0.69314718055995 * bits / scale)

        local h = { }
        h[0] = tonumber(string.sub(hash , 1 , 8) , 16)
        h[1] = tonumber(string.sub(hash , 9 , 16) , 16)
        h[2] = tonumber(string.sub(hash , 17 , 24) , 16)
        h[3] = tonumber(string.sub(hash , 25 , 32) , 16)

        local found = true
        for i = 1 , k do
            if redis.call('SETBIT' , key , (h[i % 2] + i * h[2 + (((i + (i % 2)) % 4) / 2)]) % bits , 1) == 0 then
                found = false
            end
        end

        if found == false then
            -- INCR is a little bit faster than SET.
            redis.call('INCR' , countkey)
        end
    ]]

    return redis:eval(addToBloomFilterScript , 0 , 'bloom-filter-risk-user' , 100000 , 0.001 , goodsId .. userId)
end

function byte2bin(n)
    local t = {}
    for i = 7 , 0 , -1 do
        t[#t + 1] = math.floor(n / 2 ^ i)
        n = n % 2 ^ i
    end
    return t
end

function checkRiskUserFromMysql()
    res , err , errcode , sqlstate = mysql:query([[
        select goods_id,
        rule_code
        from rule
        where goods_id =
    ]] .. redisKey[2])

    if err then
        ngx.log(ngx.ERR , "mysql查询rule失败:" , err)
        result['code'] = -200
        result['msg'] = '请求失败'
        ngx.say(cjson.encode(result))
        return ngx.exit(200)
    end

    if not res then
        ngx.log(ngx.NOTICE , "mysql查询rule失败，rule不存在")
        result['code'] = 200
        result['msg'] = '请求成功'
        ngx.say(cjson.encode(result))
        return ngx.exit(200)
    end

    local ruleCodeArray = byte2bin(tonumber(res[1]['rule_code']))
    local userExtra
    local rule
    local deposit
    if ruleCodeArray[8] == 1 or ruleCodeArray[7] == 1 or ruleCodeArray[6] == 1 then

        local tempSql = 'select user_id, defaulter, age, work_status from user_extra where user_id = '
        tempSql = tempSql .. riskUserVO['userId'] .. ';'
        res , err , errcode , sqlstate = mysql:query(tempSql)

        if err then
            ngx.log(ngx.ERR , "mysql查询user_extra失败:" , err)
            result['code'] = -200
            result['msg'] = '请求失败'
            ngx.say(cjson.encode(result))
            return ngx.exit(200)
        end

        userExtra = res[1]
    end

    --客户年龄
    if ruleCodeArray[8] == 1 then
        res , err , errcode , sqlstate = mysql:query([[
        select count(id)
        from rule]] .. ' where goods_id = ' .. riskUserVO['goodsId'] .. ' and deny_age_below <= ' .. tonumber(userExtra['age']))

        if err then
            ngx.log(ngx.ERR , "mysql查询客户年龄失败:" , err , 'userId：' , riskUserVO['userId'])
            result['code'] = -200
            result['msg'] = '请求失败'
            ngx.say(cjson.encode(result))
            return ngx.exit(200)
        end

        if tonumber(res[1]['count(id)']) == 0 then
            riskUserCache:set(temp , nil , true)
            addToBloomFilter(riskUserVO['goodsId'] , riskUserVO['userId'])
        end
        result['code'] = 200
        result['msg'] = '请求成功'
        ngx.say(cjson.encode(result))
        return ngx.exit(200)
    end

    --失信人名单
    if ruleCodeArray[7] == 1 then
        if tonumber(userExtra['defaulter']) == 1 then
            riskUserCache:set(temp , nil , true)
            addToBloomFilter(riskUserVO['goodsId'] , riskUserVO['userId'])
        end
        result['code'] = 200
        result['msg'] = '请求成功'
        ngx.say(cjson.encode(result))
        return ngx.exit(200)
    end

    --工作状态异常
    if ruleCodeArray[6] == 1 then
        res , err , errcode , sqlstate = mysql:query([[
        select count(id)
        from rule]] .. ' where goods_id = ' .. tonumber(riskUserVO['goodsId']) .. 'and deny_work_status =' .. userExtra['work_status'])

        if err then
            ngx.log(ngx.ERR , "mysql查询客户年龄失败:" , err , 'userId：' , riskUserVO['userId'])
            result['code'] = -200
            result['msg'] = '请求失败'
            ngx.say(cjson.encode(result))
            return ngx.exit(200)
        end

        if tonumber(res[1]['count(id)']) == 1 then
            riskUserCache:set(temp , nil , true)
            addToBloomFilter(riskUserVO['goodsId'] , riskUserVO['userId'])
        end

        result['code'] = 200
        result['msg'] = '请求成功'
        ngx.say(cjson.encode(result))
        return ngx.exit(200)
    end

    --逾期记录
    if ruleCodeArray[5] == 1 then
        res , err , errcode , sqlstate = mysql:query([[
        select allow_overdue_delayed_days,
        deny_overdue_times
        collect_years,
        ignore_overdue_amount
        from rule
        where goods_id =
        ]] .. tonumber(riskUserVO['goodsId']))

        rule = res[1]

        local ignoreAmount = tonumber(rule['ignore_overdue_amount'])
        local now = ngx.today()
        local year = tonumber(string.sub(now , 1 , 4)) - tonumber(rule['collect_years'])
        local month = string.sub(now , 5 , 7)
        local day = tonumber(string.sub(now , 8 , 10))

        res , err , errcode , sqlstate = mysql:query('select count(id) from deposit where user_id ='
                .. riskUserVO['userId'] .. 'and debt_amount > ' .. ignoreAmount ..
                'and due_date >= \'' .. year .. month .. day .. '\'')

        if err then
            ngx.log(ngx.ERR , "mysql查询deposit失败:" , err)
            result['code'] = -200
            result['msg'] = '请求失败'
            ngx.say(cjson.encode(result))
            return ngx.exit(200)
        end

        if tonumber(res[1]['count(id)']) >= tonumber(rule['deny_overdue_times']) then
            riskUserCache:set(temp , nil , true)
            addToBloomFilter(riskUserVO['goodsId'] , riskUserVO['userId'])
        end
        result['code'] = 200
        result['msg'] = '请求成功'
        ngx.say(cjson.encode(result))
        return ngx.exit(200)

    end
end

res , err = goodsCache:get(temp , nil , checkRiskUser , bodyJson)
if err then
    ngx.log(ngx.ERR , "mysql连接失败:" , err)
end

if res or not checkRiskUserFromBloomFilter(riskUserVO['goodsId'] , riskUserVO['userId']) then
    result['code'] = 200
    result['msg'] = '请求已提交'
    ngx.say(cjson.encode(result))
    return ngx.exit(200)
end

checkRiskUserFromMysql()

ok , err = mysql:set_keepalive(60000 , 100)
if not ok then
    ngx.log(ngx.ERR , "mysql keepalive失败: " , err)
end

result['code'] = 200
result['msg'] = '请求成功'
ngx.say(cjson.encode(result))
return ngx.exit(200)