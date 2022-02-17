package com.waibao.util.feign;

import com.waibao.util.vo.user.AdminLoginVO;
import com.waibao.util.vo.user.AdminVO;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.user.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * AdminService
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Component
@FeignClient(name = "waibao-user")
public interface AdminService {

    @GetMapping(value = "/check/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<String> checkAdmin(@PathVariable("id") Long adminId);

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<AdminVO> getAdminInfo(@PathVariable("id") Long adminId);

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<PageVO<AdminVO>> getAdminPage(@RequestBody PageVO<AdminVO> pageVO);

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<AdminVO> addAdminInfo(@RequestBody AdminVO adminVO);

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<AdminLoginVO> login(@RequestParam String name, @RequestParam String password);
}
