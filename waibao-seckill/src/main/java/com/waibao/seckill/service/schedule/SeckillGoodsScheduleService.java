package com.waibao.seckill.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.seckill.service.cache.GoodsRetailerCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
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
//@Service
@RequiredArgsConstructor
public class SeckillGoodsScheduleService {
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillGoodsCacheService seckillGoodsCacheService;
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
        long l = longAdder.longValue();
        if (l > 0) {
            log.info("******SeckillGoodsScheduleService：开始读取数据库放入缓存");
            IPage<SeckillGoods> seckillGoodsPage = new Page<>(l, 1000);
            seckillGoodsPage = seckillGoodsMapper.selectPage(seckillGoodsPage, null);
            List<SeckillGoods> seckillGoodsList = seckillGoodsPage.getRecords();
            seckillGoodsCacheService.insertBatch(seckillGoodsList);
            goodsRetailerCacheService.insertBatch(seckillGoodsList);
            longAdder.decrement();
            log.info("******SeckillGoodsScheduleService：读取数据库放入缓存结束");
        }
    }
}
