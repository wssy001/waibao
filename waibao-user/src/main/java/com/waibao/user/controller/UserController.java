package com.waibao.user.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.User;
import com.waibao.user.mapper.UserMapper;
import com.waibao.user.service.cache.UserCacheService;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.feign.UserService;
import com.waibao.util.tools.JWTUtil;
import com.waibao.util.tools.SMUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.PageVO;
import com.waibao.util.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户基本信息表 前端控制器
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-01-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController implements UserService {
    private final UserMapper userMapper;
    private final UserCacheService userCacheService;

    @Override
    @GetMapping(value = "/check/{userNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<String> checkUser(
            @PathVariable("userNo") Long userNo
    ) {
        if (userCacheService.checkUser(userNo)) GlobalResult.success();
        return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
    }

    @Override
    @GetMapping(value = "/{userNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserVO> getUserInfo(
            @PathVariable("userNo") Long userNo
    ) {
        User user = userCacheService.get(userNo);
        if (user == null) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);

        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class, "password", "userSource");
        userVO.hideMobile();
        userVO.setPassword(null);
        return GlobalResult.success(ResultEnum.SUCCESS, userVO);
    }

    @Override
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<UserVO>> getUserPage(
            @RequestBody PageVO<UserVO> pageVO
    ) {
        IPage<User> userPage = new Page<>(pageVO.getIndex(), pageVO.getCount());
        userPage = userMapper.selectPage(userPage, Wrappers.<User>lambdaQuery().orderByDesc(User::getUpdateTime));

        List<User> records = userPage.getRecords();
        if (records == null) records = new ArrayList<>();

        List<UserVO> userVOList = records.parallelStream()
                .map(user -> BeanUtil.copyProperties(user, UserVO.class))
                .peek(UserVO::hideMobile)
                .peek(userVO -> userVO.setPassword(null))
                .collect(Collectors.toList());
        pageVO.setMaxIndex(userPage.getPages());
        pageVO.setList(userVOList);
        pageVO.setMaxSize(userPage.getTotal());

        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }

    @Override
    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserVO> addUserInfo(
            @RequestBody UserVO userVO
    ) {

        String password = userVO.getPassword();
        userVO.setPassword(SMUtil.sm3Encode(password));
        User user = BeanUtil.copyProperties(userVO, User.class);

        int count = userMapper.insert(user);
        if (count == 0) return GlobalResult.error(ResultEnum.USER_SAVE_FAIL);
        userCacheService.set(user);

        userVO.hideMobile();
        userVO.setPassword(null);
        return GlobalResult.success(ResultEnum.SUCCESS, userVO);
    }

    @Override
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<JSONObject> login(
            @RequestParam String principal,
            @RequestParam String password
    ) {
        password = SMUtil.sm3Encode(password);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPassword, password).and(wrapper -> wrapper.eq(User::getUserNo, principal)
                .or().eq(User::getEamil, principal).or().eq(User::getMobile, principal)));
        if (user == null) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);

        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.hideMobile();
        userVO.setPassword(null);
        userVO.setExpireTime(DateUtil.offsetHour(new Date(), 1).getTime());
        JSONObject jsonObject = (JSONObject) JSON.toJSON(userVO);
        jsonObject.put("token", JWTUtil.create(userVO));
        return GlobalResult.success(jsonObject);
    }

    @Override
    @PostMapping(value = "/renew", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<JSONObject> renew(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    ) {
        String data = token.split("\\.")[1];
        JSONObject jsonObject = JSONObject.parseObject(Base64.decodeStr(data));
        User user = userCacheService.get(jsonObject.getLong("userNo"));
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.hideMobile();
        userVO.setPassword(null);
        userVO.setExpireTime(DateUtil.offsetHour(new Date(), 1).getTime());
        jsonObject = (JSONObject) JSON.toJSON(userVO);
        jsonObject.put("token", JWTUtil.create(userVO));
        return GlobalResult.success(jsonObject);
    }

}
