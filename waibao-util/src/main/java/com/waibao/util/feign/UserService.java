package com.waibao.util.feign;

import com.alibaba.fastjson.JSONObject;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.PageVO;
import com.waibao.util.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * UserService
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Component
@FeignClient(name = "user-provider")
public interface UserService {

    @GetMapping(value = "/check/{userNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<String> checkUser(@PathVariable("userNo") Long userNo);

    @GetMapping(value = "/{userNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<UserVO> getUserInfo(@PathVariable("userNo") Long userNo);

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<PageVO<UserVO>> getUserPage(@RequestBody PageVO<UserVO> pageVO);

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<UserVO> addUserInfo(@RequestBody UserVO userVO);

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<JSONObject> login(@RequestParam String principal, @RequestParam String password);

    @PostMapping(value = "/renew", produces = MediaType.APPLICATION_JSON_VALUE)
    GlobalResult<JSONObject> renew(@RequestHeader String token);
}
