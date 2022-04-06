package com.waibao.user.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.waibao.user.entity.UserExtra;
import com.waibao.user.mapper.UserExtraMapper;
import com.waibao.user.service.cache.UserExtraCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.LongAdder;

/**
 * UserExtraScheduleService
 *
 * @author alexpetertyler
 * @since 2022/3/14
 */
@Slf4j
//@Service
@RequiredArgsConstructor
public class UserExtraScheduleService {
    private final UserExtraMapper userExtraMapper;
    private final UserExtraCacheService userExtraCacheService;

    private LongAdder longAdder;

    @PostConstruct
    public void init() {
        Long count = userExtraMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 10000 + 1);
    }

    @Scheduled(fixedDelay = 2000L)
    public void storeAdmin() {
        long l = longAdder.longValue();
        if (l > 0) {
            log.info("******UserExtraScheduleService：开始读取数据库放入缓存");
            IPage<UserExtra> userExtraPage = new Page<>(l, 10000);
            userExtraPage = userExtraMapper.selectPage(userExtraPage, null);
            Lists.partition(userExtraPage.getRecords(), 2000)
                    .parallelStream()
                    .forEach(userExtraCacheService::insertBatch);
            longAdder.decrement();
            log.info("******UserExtraScheduleService：读取数据库放入缓存结束");
        }
    }
}
