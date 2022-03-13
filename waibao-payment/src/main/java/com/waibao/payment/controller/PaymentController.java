package com.waibao.payment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.PaymentMapper;
import com.waibao.payment.service.cache.PaymentCacheService;
import com.waibao.payment.service.db.PaymentService;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.feign.UserService;
import com.waibao.util.tools.BeanUtil;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.payment.PaymentVO;
import com.waibao.util.vo.user.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentCacheService paymentCacheService;

    private  final PaymentMapper paymentMapper;
    private final UserService userService;

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PaymentVO> addUserCreditInfo(@RequestBody PaymentVO paymentVO){
        GlobalResult<String> result = userService.checkUser(paymentVO.getUserId());
        if (result.getCode() != 200) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        Payment record = BeanUtil.copyProperties(paymentVO, Payment.class);
        record.setPayId(IdWorker.getId());
        int insert = paymentMapper.insert(record);
        if (insert == 0) return GlobalResult.error(ResultEnum.SYSTEM_SAVE_FAIL);
        paymentCacheService.set(record);
        return GlobalResult.success(paymentVO);
    }

    @GetMapping(value = "/{payId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PaymentVO> getUserCreditInfo(@PathVariable("payId") Long payId){
        return paymentCacheService.get(payId);
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<PaymentVO>> getPage(@RequestBody PageVO<PaymentVO> pageVO) {
        IPage<Payment> paymentPage = new Page<>(pageVO.getIndex(), pageVO.getCount());
        paymentPage = paymentMapper.selectPage(paymentPage, Wrappers.<Payment>lambdaQuery().orderByDesc(Payment::getUpdateTime));
        List<Payment> records = paymentPage.getRecords();
        if (records == null) {
            records = new ArrayList<>();
        }
        List<PaymentVO> paymentVOList = records.parallelStream()
                .map(payment -> cn.hutool.core.bean.BeanUtil.copyProperties(payment, PaymentVO.class))
                .collect(Collectors.toList());
        pageVO.setMaxIndex(paymentPage.getPages());
        pageVO.setList(paymentVOList);
        pageVO.setMaxSize(paymentPage.getTotal());

        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }
}
