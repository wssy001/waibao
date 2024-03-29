package com.waibao.user.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.Admin;
import com.waibao.user.mapper.AdminMapper;
import com.waibao.user.service.cache.AdminCacheService;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.tools.JWTUtil;
import com.waibao.util.tools.SMUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.AdminLoginVO;
import com.waibao.util.vo.user.AdminVO;
import com.waibao.util.vo.user.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 管理员表 前端控制器
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-01-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController{
    private final AdminMapper adminMapper;
    private final AdminCacheService adminCacheService;
    
    @GetMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<AdminVO> checkAdmin(
            @RequestParam("adminId") Long adminId
    ) {
        if (!adminCacheService.checkAdmin(adminId))
            return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        Admin admin = adminCacheService.get(adminId);
        admin.setPassword(null);
        return GlobalResult.success(BeanUtil.copyProperties(admin, AdminVO.class));
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<AdminVO> getAdminInfo(
            @RequestParam("adminId") Long adminId
    ) {
        if (!adminCacheService.checkAdmin(adminId))
            return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        Admin admin = adminCacheService.get(adminId);
        admin.setPassword(null);
        return GlobalResult.success(BeanUtil.copyProperties(admin, AdminVO.class));
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<AdminVO>> getAdminPage(
            @RequestBody PageVO<AdminVO> pageVO
    ) {
        IPage<Admin> adminPage = new Page<>(pageVO.getIndex(), pageVO.getCount());
        adminPage = adminMapper.selectPage(adminPage, Wrappers.<Admin>lambdaQuery().orderByDesc(Admin::getUpdateTime));

        List<Admin> records = adminPage.getRecords();
        if (records == null) records = new ArrayList<>();

        List<AdminVO> adminVOList = records.parallelStream()
                .map(admin -> BeanUtil.copyProperties(admin, AdminVO.class))
                .collect(Collectors.toList());
        pageVO.setMaxIndex(adminPage.getPages());
        pageVO.setList(adminVOList);
        pageVO.setMaxSize(adminPage.getTotal());

        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<AdminVO> addAdminInfo(
            @RequestBody AdminVO adminVO
    ) {
        String password = adminVO.getPassword();
        adminVO.setPassword(SMUtil.sm3Encode(password));
        Admin admin = BeanUtil.copyProperties(adminVO, Admin.class);

        int count = adminMapper.insert(admin);
        if (count == 0) return GlobalResult.error(ResultEnum.USER_SAVE_FAIL);
        adminCacheService.set(admin);

        adminVO.setPassword(null);
        adminVO.setId(admin.getId());
        return GlobalResult.success(ResultEnum.SUCCESS, adminVO);
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<AdminLoginVO> login(
            @RequestBody AdminVO adminVO
    ) {
        String password = SMUtil.sm3Encode(adminVO.getPassword());
        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getPassword, password).eq(Admin::getName, adminVO.getName()));
        if (admin == null) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);

        admin.setPassword(null);
        AdminLoginVO loginVO = BeanUtil.copyProperties(admin, AdminLoginVO.class);
        adminVO = BeanUtil.copyProperties(admin, AdminVO.class);
        loginVO.setToken(JWTUtil.create(adminVO));
        return GlobalResult.success(loginVO);
    }
}
