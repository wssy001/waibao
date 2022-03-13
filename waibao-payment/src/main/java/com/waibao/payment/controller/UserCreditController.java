package com.waibao.payment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.payment.service.db.UserCreditService;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.feign.UserService;
import com.waibao.util.tools.BeanUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.PageVO;
import com.waibao.util.vo.payment.UserCreditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/credit")
public class UserCreditController {
    private final UserService userService;
    private final UserCreditService userCreditService;
    private final UserCreditMapper userCreditMapper;

    private final UserCreditCacheService userCreditCacheService;



    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserCreditVO> addUserCreditInfo(@RequestBody UserCreditVO userCreditVO){
        GlobalResult<String> result = userService.checkUser(userCreditVO.getUserId());
        if (result.getCode() != 200) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        UserCredit record = BeanUtil.copyProperties(userCreditVO, UserCredit.class);
        int insert = userCreditMapper.insert(record);
        if (insert == 0) return GlobalResult.error(ResultEnum.SYSTEM_SAVE_FAIL);
        userCreditCacheService.set(record);
        return GlobalResult.success(userCreditVO);
    }

    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserCreditVO> getUserCreditInfo(@PathVariable("userId") Long userId){
        return userCreditCacheService.get(userId);
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<UserCreditVO>> getPage(@RequestBody PageVO<UserCreditVO> pageVO) {
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

    @PostMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserCreditVO> update(@RequestBody UserCreditVO userCreditVO) {
        GlobalResult<String> result = userService.checkUser(userCreditVO.getUserId());
        if (result.getCode() != 200) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        UserCredit record = BeanUtil.copyProperties(userCreditVO, UserCredit.class);
        int insert = userCreditMapper.updateByPrimaryKeySelective(record);
        if (insert == 0) return GlobalResult.error(ResultEnum.SYSTEM_UPDATE_FAIL);
        return GlobalResult.success(userCreditVO);
    }
}
