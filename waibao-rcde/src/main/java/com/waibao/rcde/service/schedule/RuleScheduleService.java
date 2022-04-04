package com.waibao.rcde.service.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.rcde.entity.Rule;
import com.waibao.rcde.mapper.RuleMapper;
import com.waibao.rcde.service.cache.RuleCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.LongAdder;

/**
 * RuleScheduleService
 *
 * @author alexpetertyler
 * @since 2022/3/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleScheduleService {
    private final RuleMapper ruleMapper;
    private final RuleCacheService ruleCacheService;

    private LongAdder longAdder;

    @PostConstruct
    public void init() {
        Long count = ruleMapper.selectCount(null);
        longAdder = new LongAdder();
        longAdder.add(count / 3000 + 1);
    }

//    @Scheduled(fixedDelay = 60 * 1000L)
    public void storeRule() {
        long l = longAdder.longValue();
        if (l > 0) {
            log.info("******RuleScheduleService：开始读取数据库放入缓存");
            IPage<Rule> rulePage = new Page<>(l, 3000);
            rulePage = ruleMapper.selectPage(rulePage, null);
            ruleCacheService.insertBatch(rulePage.getRecords());
            longAdder.decrement();
            log.info("******RuleScheduleService：读取数据库放入缓存结束");
        }
    }
}
