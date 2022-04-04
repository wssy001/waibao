cjson = require("cjson")

local redis_util = require "resty.redis-util"
redis = redis_util:new({
    host = '10.61.20.211',
    port = 6379,
    db_index = 0,
    password = 'wssy001',
    timeout = 1000,
    keepalive = 60000,
    pool_size = 100
});

local mlcache = require "resty.mlcache"
goodsCache , err = mlcache.new("mycache" , "goodsDict" , {
    lru_size = 3000,
    ttl = 3,
    neg_ttl = 3,
    ipc_shm = "ipc_shared_dict1"
})

if not goodsCache then
    ngx.log(ngx.ERR , "goodsCache初始化失败:" , err)
end

riskUserCache , err = mlcache.new("mycache2" , "riskUserDict" , {
    lru_size = 3000,
    ttl = 3600,
    neg_ttl = 10,
    ipc_shm = "ipc_shared_dict2"
})

if not riskUserCache then
    ngx.log(ngx.ERR , "riskUserCache初始化失败:" , err)
end
