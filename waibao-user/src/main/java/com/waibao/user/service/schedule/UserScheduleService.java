package com.waibao.user.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.User;
import com.waibao.user.mapper.UserMapper;
import com.waibao.user.service.cache.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.LongAdder;

/**
 * UserScheduleService
 *
 * @author alexpetertyler
 * @since 2022/3/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserScheduleService {
    private final UserCacheService userCacheService;
    private final UserMapper userMapper;

    private LongAdder longAdder;

    @PostConstruct
    public void init() {
        Long count = userMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 2000 + 1);
    }

    @Scheduled(fixedDelay = 2000L)
    public void storeUser() {
        log.info("******UserScheduleService：开始读取数据库放入缓存");
        longAdder.longValue();
        long l = longAdder.longValue();
        if (l > 0) {
            IPage<User> userPage = new Page<>(l, 2000);
            userPage = userMapper.selectPage(userPage, null);
            userCacheService.insertBatch(userPage.getRecords());
            longAdder.decrement();
        }
        log.info("******UserScheduleService：读取数据库放入缓存结束");
    }
}
