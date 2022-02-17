package com.waibao.user.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.User;
import com.waibao.user.mapper.UserMapper;
import com.waibao.user.service.UserCacheService;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.PageVO;
import com.waibao.util.vo.UserVO;
import com.waibao.util.enums.ResultEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
public class UserController {
    private final UserMapper userMapper;
    private final UserCacheService userCacheService;

    @GetMapping(value = "/check/{userNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<String> checkUser(
            @PathVariable("userNo") Long userNo
    ) {
        if (userCacheService.checkUser(userNo)) GlobalResult.success();
        return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
    }

    @GetMapping(value = "/{userNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserVO> getUserInfo(
            @PathVariable("userNo") Long userNo
    ) {
        User user = userCacheService.get(userNo);
        if (user == null) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);

        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class, "password", "userSource");
        userVO.hideMobile();
        return GlobalResult.success(ResultEnum.SUCCESS, userVO);
    }

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
                .collect(Collectors.toList());
        pageVO.setMaxIndex(userPage.getPages());
        pageVO.setList(userVOList);
        pageVO.setMaxSize(userPage.getTotal());

        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<UserVO> addUserInfo(
            @RequestBody UserVO userVO
    ) {

        String password = userVO.getPassword();
        userVO.setPassword(MD5.create().digestHex(password));
        User user = BeanUtil.copyProperties(userVO, User.class);

        int count = userMapper.insert(user);
        if (count == 0) return GlobalResult.error(ResultEnum.USER_SAVE_FAIL);
        userCacheService.set(user);

        userVO.hideMobile();
        return GlobalResult.success(ResultEnum.SUCCESS, userVO);
    }

}

