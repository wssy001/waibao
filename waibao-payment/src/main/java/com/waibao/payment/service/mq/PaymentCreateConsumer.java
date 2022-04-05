package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.payment.service.db.LogPaymentService;
import com.waibao.payment.service.db.PaymentService;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * PaymentCreateConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCreateConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final PaymentService paymentService;
    private final UserCreditMapper userCreditMapper;
    private final LogPaymentService logPaymentService;
    private final LogPaymentCacheService logPaymentCacheService;
    private final UserCreditCacheService userCreditCacheService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        ConcurrentHashMap<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));

        List<Payment> paymentList = convert(messageExtMap.values(), Payment.class)
                .stream()
                .filter(payment -> logPaymentCacheService.hasConsumeTags(payment.getUserId(), payment.getPayId(), "create"))
                .collect(Collectors.toList());

        asyncService.basicTask(() -> paymentService.saveBatch(paymentList));
        asyncService.basicTask(() -> logPaymentService.saveBatch(convert(messageExtMap.values(), LogPayment.class)));
        asyncService.basicTask(() -> {
            List<Long> userIdList = paymentList.stream()
                    .map(Payment::getUserId)
                    .collect(Collectors.toList());
            batchStoreCache(userCreditMapper.selectBatchIds(userIdList));
        });
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> {
                    JSONObject jsonObject = (JSONObject) JSON.toJSON(new String(messageExt.getBody()));
                    jsonObject.put("money", jsonObject.getBigDecimal("orderPrice"));
                    return jsonObject;
                })
                .peek(jsonObject -> {
                    if (clazz == LogPayment.class) jsonObject.put("operation", "create");
                })
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

    private void batchStoreCache(List<UserCredit> userCredits) {
        userCredits.parallelStream()
                .forEach(userCreditCacheService::set);
    }

}
