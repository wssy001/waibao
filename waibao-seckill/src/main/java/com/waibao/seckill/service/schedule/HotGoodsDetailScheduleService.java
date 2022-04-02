package com.waibao.seckill.service.schedule;

import com.waibao.seckill.entity.Goods;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.GoodsMapper;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.seckill.service.cache.HotGoodsDetailCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GoodsDetailScheduleService
 *
 * @author alexpetertyler
 * @since 2022/4/02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotGoodsDetailScheduleService {
    private final GoodsMapper goodsMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final HotGoodsDetailCacheService hotGoodsDetailCacheService;

    @PostConstruct
    public void init() {
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(null);
        if (seckillGoodsList.isEmpty()) return;

        log.info("******GoodsDetailScheduleService：开始读取数据库放入缓存");
        Set<Long> collect = seckillGoodsList.parallelStream()
                .map(SeckillGoods::getGoodsId)
                .collect(Collectors.toSet());
        List<Goods> goodsList = goodsMapper.selectBatchIds(collect);
        hotGoodsDetailCacheService.insertBatch(goodsList);
        log.info("******GoodsDetailScheduleService：读取数据库放入缓存结束");
    }

//    @Scheduled(fixedDelay = 2000L)
//    public void storeAdmin() {
//        long l = longAdder.longValue();
//        if (l > 0) {
//            log.info("******GoodsDetailScheduleService：开始读取数据库放入缓存");
//            IPage<SeckillGoods> seckillGoodsPage = new Page<>(l, 1000);
//            seckillGoodsPage = seckillGoodsMapper.selectPage(seckillGoodsPage, null);
//            List<SeckillGoods> seckillGoodsList = seckillGoodsPage.getRecords();
//            asyncService.basicTask(() -> goodsCacheService.insertBatch(seckillGoodsList));
//            asyncService.basicTask(() -> goodsRetailerCacheService.insertBatch(seckillGoodsList));
//            longAdder.decrement();
//            log.info("******GoodsDetailScheduleService：读取数据库放入缓存结束");
//        }
//    }
}
