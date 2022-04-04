package com.waibao.rcde.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.rcde.entity.Deposit;
import com.waibao.rcde.mapper.DepositMapper;
import com.waibao.rcde.service.cache.DepositCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.LongAdder;

/**
 * DepositScheduleService
 *
 * @author alexpetertyler
 * @since 2022/3/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepositScheduleService {
    private final DepositMapper depositMapper;
    private final DepositCacheService depositCacheService;

    private LongAdder longAdder;

    @PostConstruct
    public void init() {
        Long count = depositMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 3000 + 1);
    }

//    @Scheduled(fixedDelay = 60 * 1000L)
    public void storeDeposit() {
        long l = longAdder.longValue();
        if (l > 0) {
            log.info("******DepositScheduleService：开始读取数据库放入缓存");
            IPage<Deposit> depositPage = new Page<>(l, 3000);
            depositPage = depositMapper.selectPage(depositPage, null);
            depositCacheService.insertBatch(depositPage.getRecords());
            longAdder.decrement();
            log.info("******DepositScheduleService：读取数据库放入缓存结束");
        }
    }
}
