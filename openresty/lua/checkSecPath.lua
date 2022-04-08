--TODO 完成从URL中提取seckillPath并判断请求
local arg = ngx.req.get_uri_args()

local result = table.new(0 , 4)
result['code'] = -200
result['time'] = ngx.localtime()
result['timestamp'] = ngx.time()