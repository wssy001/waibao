package com.waibao.payment.controller;

import com.waibao.payment.service.PaymentCacheService;
import com.waibao.payment.service.UserCreditCacheService;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.payment.PaymentVO;
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
@RequestMapping("/payment")
public class PaymentController {
    @Resource
    private PaymentCacheService paymentCacheService;

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PaymentVO> addUserCreditInfo(@RequestBody PaymentVO paymentVO){
        return paymentCacheService.add(paymentVO);
    }

    @GetMapping(value = "/{payId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PaymentVO> getUserCreditInfo(@PathVariable("payId") Long payId){
        return paymentCacheService.get(payId);
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<PaymentVO>> getPage(@RequestBody PageVO<PaymentVO> pageVO) {
        return  paymentCacheService.list(pageVO);
    }
}
