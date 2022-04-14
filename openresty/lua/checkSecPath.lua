local uri = ngx.var.request_uri;

local result = table.new(0 , 4)
result['code'] = -200
result['time'] = ngx.localtime()
result['timestamp'] = ngx.time()

function checkSeckillPath(seckillPath)
    return redis:eval([[
        redis.call('SELECT' , 5)
        return tonumber(redis.call('DEL' , KEYS[1])) == 1
    ]] , 1 , seckillPath)
end

--/seckill/goods/{seckillPath}/kill
local m , err = ngx.re.match(uri , '(/seckill/goods/)(.*)(/kill)')

if not m or err then
    if err then
        ngx.log(ngx.ERR , "正则匹配出错：" , err)
    end
    result['msg'] = '请传入seckillPath'
    return ngx.exit(200)
end

local seckillPath = m[2]

if not checkSeckillPath(seckillPath) then
    result['msg'] = 'seckillPath不存在'
    ngx.say(cjson.encode(result))
    return ngx.exit(200)
end