package com.waibao.user.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.User;
import com.waibao.user.entity.UserExtra;
import com.waibao.user.mapper.UserExtraMapper;
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
import java.util.Map;
import java.util.function.Function;
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
    private final UserExtraMapper userExtraMapper;
    private final UserCacheService userCacheService;

    @Override
    @GetMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserVO> checkUser(
            @RequestParam("userId") Long userId
    ) {
        if (!userCacheService.checkUser(userId)) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        User user = userCacheService.get(userId);
        user.setPassword(null);
        UserExtra userExtra = userExtraMapper.selectById(userId);
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.hideMobile();
        BeanUtil.copyProperties(userExtra, userVO);
        return GlobalResult.success(userVO);
    }

    @Override
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserVO> getUserInfo(
            @RequestParam("userId") Long userId
    ) {
        if (!userCacheService.checkUser(userId)) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        User user = userCacheService.get(userId);
        user.setPassword(null);
        UserExtra userExtra = userExtraMapper.selectById(userId);
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.hideMobile();
        BeanUtil.copyProperties(userExtra, userVO);
        return GlobalResult.success(userVO);
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

        List<Long> userIds = records.parallelStream()
                .map(User::getId)
                .collect(Collectors.toList());
        Map<Long, UserExtra> collect = userExtraMapper.selectBatchIds(userIds).parallelStream()
                .collect(Collectors.toMap(UserExtra::getUserId, Function.identity()));

        List<UserVO> userVOList = records.parallelStream()
                .map(user -> BeanUtil.copyProperties(user, UserVO.class))
                .peek(UserVO::hideMobile)
                .peek(userVO -> BeanUtil.copyProperties(collect.get(userVO.getId()), userVO))
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
        UserExtra userExtra = BeanUtil.copyProperties(userVO, UserExtra.class);

        int count = userMapper.insert(user);
        if (count == 0) return GlobalResult.error(ResultEnum.USER_SAVE_FAIL);
        userCacheService.set(user);
        userExtra.setUserId(user.getId());
        userExtraMapper.insert(userExtra);

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
        User user;
        if (principal.contains("@")) {
            user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPassword, password).eq(User::getEmail, principal));
        } else {
            user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPassword, password).eq(User::getMobile, principal));
        }
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
        UserVO userVO = JWTUtil.getUserVO(token);
        User user = userCacheService.get(userVO.getId());
        userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.hideMobile();
        userVO.setPassword(null);
        userVO.setExpireTime(DateUtil.offsetHour(new Date(), 1).getTime());
        JSONObject jsonObject = (JSONObject) JSON.toJSON(userVO);
        jsonObject.put("token", JWTUtil.create(userVO));
        return GlobalResult.success(jsonObject);
    }

}
