package com.waibao.payment.service.cache;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.db.UserCreditService;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.feign.UserService;
import com.waibao.util.tools.BeanUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.PageVO;
import com.waibao.util.vo.user.UserCreditVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreditCacheService {
    private final String REDIS_USER_CREDIT_KEY_PREFIX = "user-credit-";

    private final UserCreditMapper userCreditMapper;
    private final UserService userService;
    private final UserCreditService userCreditService;

    @Resource
    private RedisTemplate<String, UserCredit> userCreditRedisTemplate;

    private ValueOperations<String, UserCredit> valueOperations;

    @PostConstruct
    public void init() {
        valueOperations = userCreditRedisTemplate.opsForValue();
    }

    public GlobalResult<UserCreditVO> add(UserCreditVO userCreditVO) {
        GlobalResult<String> result = userService.checkUser(userCreditVO.getUserId());
        if (result.getCode() != 200) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        UserCredit record = BeanUtil.copyProperties(userCreditVO, UserCredit.class);
        int insert = userCreditMapper.insert(record);
        if (insert == 0) return GlobalResult.error(ResultEnum.SYSTEM_SAVE_FAIL);
        this.set(record);
        return GlobalResult.success(userCreditVO);
    }

    public GlobalResult<UserCreditVO> get(Long userId) {
        UserCredit credit = valueOperations.get(REDIS_USER_CREDIT_KEY_PREFIX + userId);
        if (credit == null) {
            credit = userCreditService.getOne(Wrappers.<UserCredit>lambdaQuery().eq(UserCredit::getUserId, userId));
            if (credit == null) {
                return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
            }
        }
        UserCreditVO record = BeanUtil.copyProperties(credit, UserCreditVO.class);
        return GlobalResult.success(record);
    }

    public GlobalResult<PageVO<UserCreditVO>> list(PageVO<UserCreditVO> pageVO) {
        IPage<UserCredit> creditPage = new Page<>(pageVO.getIndex(), pageVO.getCount());
        creditPage = userCreditMapper.selectPage(creditPage, Wrappers.<UserCredit>lambdaQuery().orderByDesc(UserCredit::getUpdateTime));

        List<UserCredit> records = creditPage.getRecords();
        if (records == null) {
            records = new ArrayList<>();
        }
        List<UserCreditVO> creditVOList = records.parallelStream()
                .map(credit -> cn.hutool.core.bean.BeanUtil.copyProperties(credit, UserCreditVO.class))
                .collect(Collectors.toList());
        pageVO.setMaxIndex(creditPage.getPages());
        pageVO.setList(creditVOList);
        pageVO.setMaxSize(creditPage.getTotal());

        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }

    public GlobalResult<UserCreditVO> update(UserCreditVO userCreditVO) {
        GlobalResult<String> result = userService.checkUser(userCreditVO.getUserId());
        if (result.getCode() != 200) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        UserCredit record = BeanUtil.copyProperties(userCreditVO, UserCredit.class);
        int insert = userCreditMapper.updateByPrimaryKeySelective(record);
        if (insert == 0) return GlobalResult.error(ResultEnum.SYSTEM_UPDATE_FAIL);
        return GlobalResult.success(userCreditVO);
    }

    private void set(UserCredit userCredit) {
        valueOperations.set(REDIS_USER_CREDIT_KEY_PREFIX + userCredit.getUserId(), userCredit);
    }
}
