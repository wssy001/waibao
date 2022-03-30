package com.waibao.user.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.Admin;
import com.waibao.user.mapper.AdminMapper;
import com.waibao.user.service.cache.AdminCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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
public class AdminScheduleService {
    private final AdminCacheService adminCacheService;
    private final AdminMapper adminMapper;

    private LongAdder longAdder;

    @PostConstruct
    public void init() {
        Long count = adminMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 1000 + 1);
    }

    @Scheduled(fixedDelay = 2000L)
    public void storeAdmin() {
        log.info("******AdminScheduleService：开始读取数据库放入缓存");
        long l = longAdder.longValue();
        if (l > 0) {
            IPage<Admin> adminPage = new Page<>(l, 1000);
            adminPage = adminMapper.selectPage(adminPage, null);
            adminCacheService.insertBatch(adminPage.getRecords());
            longAdder.decrement();
        }
        log.info("******AdminScheduleService：读取数据库放入缓存结束");
    }
}
