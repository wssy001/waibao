package com.waibao.payment.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.UserCreditCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.LongAdder;

/**
 * UserCreditScheduleService
 *
 * @author alexpetertyler
 * @since 2022/4/6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreditScheduleService {
    private final UserCreditMapper userCreditMapper;
    private final UserCreditCacheService userCreditCacheService;

    private LongAdder longAdder;

    @PostConstruct
    public void init() {
        Long count = userCreditMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 10000 + 1);
    }

    @Scheduled(fixedDelay = 2000L)
    public void storeUser() {
        longAdder.longValue();
        long l = longAdder.longValue();
        if (l > 0) {
            log.info("******UserCreditScheduleService：开始读取数据库放入缓存");
            IPage<UserCredit> userPage = new Page<>(l, 10000);
            userPage = userCreditMapper.selectPage(userPage, null);
            Lists.partition(userPage.getRecords(), 2000)
                    .parallelStream()
                    .forEach(userCreditCacheService::batchSet);
            longAdder.decrement();
            log.info("******UserCreditScheduleService：读取数据库放入缓存结束");
        }
    }
}
