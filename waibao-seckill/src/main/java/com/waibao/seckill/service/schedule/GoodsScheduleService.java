package com.waibao.seckill.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.seckill.service.cache.GoodsCacheService;
import com.waibao.seckill.service.cache.GoodsRetailerCacheService;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 * AdminScheduleService
 *
 * @author alexpetertyler
 * @since 2022/3/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsScheduleService {
    private final AsyncService asyncService;
    private final GoodsCacheService goodsCacheService;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final GoodsRetailerCacheService goodsRetailerCacheService;

    private LongAdder longAdder;

    @PostConstruct
    public void init() {
        Long count = seckillGoodsMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 1000 + 1);
    }

    @Scheduled(fixedDelay = 2000L)
    public void storeAdmin() {
        log.info("******GoodsScheduleService：开始读取数据库放入缓存");
        long l = longAdder.longValue();
        if (l > 0) {
            IPage<SeckillGoods> seckillGoodsPage = new Page<>(l, 1000);
            seckillGoodsPage = seckillGoodsMapper.selectPage(seckillGoodsPage, null);
            List<SeckillGoods> seckillGoodsList = seckillGoodsPage.getRecords();
            asyncService.basicTask(() -> goodsCacheService.insertBatch(seckillGoodsList));
            asyncService.basicTask(() -> goodsRetailerCacheService.insertBatch(seckillGoodsList));
            longAdder.decrement();
        }
        log.info("******GoodsScheduleService：读取数据库放入缓存结束");
    }
}
