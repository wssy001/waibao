package com.waibao.seckill.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.seckill.entity.Goods;
import com.waibao.seckill.mapper.GoodsMapper;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GoodsCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsCacheService {
    private final GoodsMapper goodsMapper;
    private final AsyncService asyncService;

    private Cache<Long, Goods> goodsCache;
    private BloomFilter<Long> bloomFilter;

    @PostConstruct
    public void init() {
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 100, 0.01);

        goodsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(300)
                .build();
    }

    public Goods get(Long goodsId) {
        if (!bloomFilter.mightContain(goodsId)) return null;
        return goodsCache.get(goodsId, goodsMapper::selectById);
    }

    public void set(Goods goods) {
        Long goodsId = goods.getId();
        goodsCache.put(goodsId, goods);
        bloomFilter.put(goodsId);
    }

    public boolean checkGoods(Long userId) {
        return get(userId) != null;
    }

    public void insertBatch(List<Goods> userList) {
        asyncService.basicTask(() -> userList.parallelStream().forEach(user -> goodsCache.put(user.getId(), user)));
        asyncService.basicTask(() -> userList.parallelStream().forEach(user -> bloomFilter.put(user.getId())));
    }
}
