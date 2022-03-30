package com.waibao.payment.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.PaymentMapper;
import com.waibao.payment.service.cache.PaymentCacheService;
import com.waibao.payment.service.mq.AsyncMQMessage;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.feign.UserService;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.payment.PaymentVO;
import com.waibao.util.vo.user.PageVO;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
    private final UserService userService;
    private final PaymentMapper paymentMapper;
    private final AsyncMQMessage asyncMQMessage;
    private final PaymentCacheService paymentCacheService;
    private final DefaultMQProducer paymentRequestPayMQProducer;

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PaymentVO> addPayment(
            @RequestBody PaymentVO paymentVO
    ) {
        GlobalResult<String> result = userService.checkUser(paymentVO.getUserId());
        if (result.getCode() != 200) return GlobalResult.error(ResultEnum.USER_IS_NOT_EXIST);
        Payment record = BeanUtil.copyProperties(paymentVO, Payment.class);
        record.setPayId(IdUtil.getSnowflakeNextIdStr());
        int insert = paymentMapper.insert(record);
        if (insert == 0) return GlobalResult.error(ResultEnum.SYSTEM_SAVE_FAIL);
        paymentCacheService.set(record);
        return GlobalResult.success(paymentVO);
    }

    @GetMapping(value = "/{payId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PaymentVO> getPaymentInfo(
            @PathVariable("payId") String payId,
            @RequestParam("userId") Long userId
    ) {
        Payment payment = paymentCacheService.get(payId);
        if (payment == null || !payment.getUserId().equals(userId)) return GlobalResult.error("订单不存在");
        return GlobalResult.success(BeanUtil.copyProperties(payment, PaymentVO.class));
    }

    @PostMapping(value = "pay/{payId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<String> requestPay(
            @PathVariable("payId") String payId,
            @RequestParam("userId") Long userId
    ) {
        OrderVO orderVO = new OrderVO();
        orderVO.setPayId(payId);
        orderVO.setUserId(userId);
        Message message = new Message("payment", "pay", JSON.toJSONBytes(orderVO));
        asyncMQMessage.sendMessage(paymentRequestPayMQProducer, message);
        return GlobalResult.success("已提交支付请求");
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<PaymentVO>> getPage(
            @RequestBody PageVO<PaymentVO> pageVO
    ) {
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
