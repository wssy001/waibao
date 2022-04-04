function checkSeckillFinished()
    redis.call('SELECT' , 5)
    local key = KEYS[1]
    local goodsId = tostring(ARGV[1])
    return redis.call('HGET' , key , goodsId)
end

function checkGoodsStorage()
    redis.call('SELECT' , 5)
    local key = KEYS[1]
    local field = tostring(ARGV[1])
    return redis.call('HGET' , key , field)
end

function checkRiskUser()
    local riskUserVO = cjson.decode(ARGV[1])
    redis.call('SELECT' , 15)
    return tonumber(redis.call('SISMEMBER' , KEYS[1] .. riskUserVO["goodsId"] , riskUserVO["userId"])) == 1
end

function addRiskUser()
    local riskUserVO = cjson.decode(ARGV[1])
    redis.call('SELECT' , 15)
    redis.call('SISMEMBER' , KEYS[1] .. riskUserVO["goodsId"] , riskUserVO["userId"])
end

function addToBloomFilter()
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
    -- 0.69314718055995 = ln(2)
    local index = math.ceil(math.log(factor) / 0.69314718055995)
    local scale = math.pow(2 , index - 1) * entries
    local key = ARGV[1] .. ':' .. index

    -- Based on the math from: http://en.wikipedia.org/wiki/Bloom_filter#Probability_of_false_positives
    -- Combined with: http://www.sciencedirect.com/science/article/pii/S0020019006003127
    -- 0.4804530139182 = ln(2)^2
    local bits = math.floor(-(scale * math.log(precision * math.pow(0.5 , index))) / 0.4804530139182)

    -- 0.69314718055995 = ln(2)
    local k = math.floor(0.69314718055995 * bits / scale)

    -- This uses a variation on:
    -- 'Less Hashing, Same Performance: Building a Better Bloom Filter'
    -- https://www.eecs.harvard.edu/~michaelm/postscripts/tr-02-05.pdf
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

    -- We only increment the count key when we actually added the item to the filter.
    -- This doesn't mean count is accurate. Since this is a scaling bloom filter
    -- it is possible the item was already present in one of the filters in a lower index.
    -- If you really want to make sure an items isn't added multile times you
    -- can use cas.lua (Check And Set).
    if found == false then
        -- INCR is a little bit faster than SET.
        redis.call('INCR' , countkey)
    end
end

function checkFromBloomFilter()
    local entries = ARGV[2]
    local precision = ARGV[3]
    local count = redis.call('GET' , ARGV[1] .. ':count')

    if not count then
        return 0
    end

    local factor = math.ceil((entries + count) / entries)
    -- 0.69314718055995 = ln(2)
    local index = math.ceil(math.log(factor) / 0.69314718055995)
    local scale = math.pow(2 , index - 1) * entries

    local hash = redis.sha1hex(ARGV[4])

    -- This uses a variation on:
    -- 'Less Hashing, Same Performance: Building a Better Bloom Filter'
    -- https://www.eecs.harvard.edu/~michaelm/postscripts/tr-02-05.pdf
    local h = { }
    h[0] = tonumber(string.sub(hash , 1 , 8) , 16)
    h[1] = tonumber(string.sub(hash , 9 , 16) , 16)
    h[2] = tonumber(string.sub(hash , 17 , 24) , 16)
    h[3] = tonumber(string.sub(hash , 25 , 32) , 16)

    -- Based on the math from: http://en.wikipedia.org/wiki/Bloom_filter#Probability_of_false_positives
    -- Combined with: http://www.sciencedirect.com/science/article/pii/S0020019006003127
    -- 0.4804530139182 = ln(2)^2
    local maxbits = math.floor((scale * math.log(precision * math.pow(0.5 , index))) / -0.4804530139182)

    -- 0.69314718055995 = ln(2)
    local maxk = math.floor(0.69314718055995 * maxbits / scale)
    local b = { }

    for i = 1 , maxk do
        table.insert(b , h[i % 2] + i * h[2 + (((i + (i % 2)) % 4) / 2)])
    end

    for n = 1 , index do
        local key = ARGV[1] .. ':' .. n
        local found = true
        local scalen = math.pow(2 , n - 1) * entries

        -- 0.4804530139182 = ln(2)^2
        local bits = math.floor((scalen * math.log(precision * math.pow(0.5 , n))) / -0.4804530139182)

        -- 0.69314718055995 = ln(2)
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
end