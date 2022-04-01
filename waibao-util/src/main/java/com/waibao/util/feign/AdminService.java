package com.waibao.util.feign;

import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.AdminLoginVO;
import com.waibao.util.vo.user.AdminVO;
import com.waibao.util.vo.user.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AdminService
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Component
@FeignClient(name = "waibao-user")
public interface AdminService {

    @GetMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<AdminVO> checkAdmin(@RequestParam("adminId") Long adminId);

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<AdminVO> getAdminInfo(@RequestParam("adminId") Long adminId);

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<PageVO<AdminVO>> getAdminPage(@RequestBody PageVO<AdminVO> pageVO);

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<AdminVO> addAdminInfo(@RequestBody AdminVO adminVO);

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<AdminLoginVO> login(@RequestBody AdminVO adminVO);
}
