package com.waibao.payment.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.payment.service.UserCreditCacheService;
import com.waibao.util.tools.JWTUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.PageVO;
import com.waibao.util.vo.user.UserCreditVO;
import com.waibao.util.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;

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

}
