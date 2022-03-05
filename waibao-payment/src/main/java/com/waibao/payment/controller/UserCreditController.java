package com.waibao.payment.controller;

import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.PageVO;
import com.waibao.util.vo.user.UserCreditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/credit")
public class UserCreditController {

    @Resource
    private UserCreditCacheService userCreditCacheService;

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserCreditVO> addUserCreditInfo(@RequestBody UserCreditVO userCreditVO){
        return userCreditCacheService.add(userCreditVO);
    }

    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserCreditVO> getUserCreditInfo(@PathVariable("userId") Long userId){
        return userCreditCacheService.get(userId);
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<UserCreditVO>> getPage(@RequestBody PageVO<UserCreditVO> pageVO) {
        return  userCreditCacheService.list(pageVO);
    }

    @PostMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserCreditVO> update(@RequestBody UserCreditVO userCreditVO) {
        return  userCreditCacheService.update(userCreditVO);
    }
}
