local arg = ngx.req.get_uri_args()

local result = table.new(0 , 4)
result['code'] = -200
result['time'] = ngx.localtime()
result['timestamp'] = ngx.time()

local redisKey = table.new(2 , 0)
redisKey[1] = 'seckill-goods'
redisKey[2] = tostring(arg['goodsId'])
local temp = table.concat(redisKey , '-')

function checkSeckillFinished()
    return redis:eval([[
        redis.call('SELECT' , 5)
        local key = KEYS[1]
        local goodsId = tostring(ARGV[1])
        return redis.call('HGET' , key , goodsId)
    ]] , 1 , 'seckill-goods-status' , redisKey[2])
end

function checkGoodsStorage()
    return redis:eval([[
        redis.call('SELECT' , 5)
        local key = KEYS[1]
        local field = tostring(ARGV[1])
        return redis.call('HGET' , key , field)
    ]] , 1 , temp , 'storage')
end

local res , err = goodsCache:get(redisKey[2] , nil , checkSeckillFinished , temp)

if err or res == nil then
    if err then
        ngx.log(ngx.ERR , "获取商品状态失败：" , err)
    else
        ngx.log(ngx.ERR , "获取商品状态失败，状态不存在")
    end
    return
end

if not res then
    result['msg'] = '秒杀已结束'
    ngx.say(cjson.encode(result))
    return ngx.exit(200)
else
    ngx.log(ngx.NOTICE , "获取商品状态成功，goodsId：" , redisKey[2] , '，状态：' , res)
end

res , err = checkGoodsStorage()
if err or not res then
    if err then
        ngx.log(ngx.ERR , "获取商品库存失败：" , err)
        return
    end
    ngx.log(ngx.ERR , "获取商品库存失败，库存不存在")
    return
else
    ngx.log(ngx.NOTICE , "获取商品库存成功，goodsId：" , redisKey[2] , '，库存：' , res)
    if tonumber(res) <= 0 then
        result['msg'] = '秒杀已结束'
        ngx.say(cjson.encode(result))
        return ngx.exit(200)
    end
end