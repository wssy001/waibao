package com.waibao.payment.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.payment.entity.LogPayment;
import com.waibao.payment.entity.LogUserCredit;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.payment.service.cache.LogPaymentCacheService;
import com.waibao.payment.service.cache.UserCreditCacheService;
import com.waibao.payment.service.db.LogPaymentService;
import com.waibao.payment.service.db.LogUserCreditService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.function.Function;
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
public class PaymentRequestPayConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final AsyncMQMessage asyncMQMessage;
    private final UserCreditMapper userCreditMapper;
    private final LogPaymentService logPaymentService;
    private final LogUserCreditService logUserCreditService;
    private final UserCreditCacheService userCreditCacheService;
    private final LogPaymentCacheService logPaymentCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    private DefaultMQProducer paymentRequestPayMQProducer;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        log.info("******PaymentRequestPayConsumer：本轮消息数量：{}", msgs.size());
        Map<String, MessageExt> messageExtMap = msgs.parallelStream()
                .collect(Collectors.toMap(Message::getKeys, Function.identity(), (prev, next) -> next));
        log.info("******PaymentRequestPayConsumer：处理后消息数量：{}", messageExtMap.size());
        List<PaymentVO> paymentVOList = logPaymentCacheService.batchCheckNotConsumeTags(convert(messageExtMap.values(), PaymentVO.class), "paid");
        log.info("******PaymentRequestPayConsumer：过滤后消息数量：{}", paymentVOList.size());
        if (paymentVOList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        CopyOnWriteArrayList<JSONObject> paidList = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<JSONObject> unpaidList = new CopyOnWriteArrayList<>();
        try {
            //FIXME 重复扣费
            List<JSONObject> jsonObjectList = userCreditCacheService.batchDecreaseUserCredit(paymentVOList);
            jsonObjectList.forEach(jsonObject -> {
                if (jsonObject.getString("operation").equals("paid")) {
                    paidList.add(jsonObject);
                    log.info("******executeLocalTransaction：userId：{},orderId：{} 付款成功", jsonObject.getString("userId"), jsonObject.getString("orderId"));
                } else {
                    unpaidList.add(jsonObject);
                    log.info("******executeLocalTransaction：userId：{},orderId：{} 付款失败，原因：{}", jsonObject.getString("userId"), jsonObject.getString("orderId"), jsonObject.getString("status"));
                }
            });
        } catch (Exception e) {
            log.error("******PaymentRequestPayConsumer.consumeMessage：{}", e.getMessage());
        }
        log.info("******PaymentRequestPayConsumer：本轮支付成功的数量：{}", paidList.size());
        log.info("******PaymentRequestPayConsumer：本轮支付失败的数量：{}", unpaidList.size());

        Future<List<LogUserCredit>> paidLogUserCreditFuture = asyncService.basicTask(paidList.stream()
                .map(jsonObject -> jsonObject.toJavaObject(LogUserCredit.class))
                .collect(Collectors.toList()));
        Future<List<Message>> paidMessageFuture = asyncService.basicTask(paidList.stream()
                .map(jsonObject -> jsonObject.toJavaObject(PaymentVO.class))
                .map(paymentVO -> new Message("payment", "update", paymentVO.getOrderId(), JSON.toJSONBytes(paymentVO)))
                .collect(Collectors.toList()));
        Future<List<Message>> storageMessageListFuture = asyncService.basicTask(paidList.stream()
                .map(jsonObject -> jsonObject.toJavaObject(PaymentVO.class))
                .map(paymentVO -> new Message("storage", "decrease", paymentVO.getOrderId(), JSON.toJSONBytes(paymentVO)))
                .collect(Collectors.toList()));
        Future<List<Message>> unpaidMessageFuture = asyncService.basicTask(unpaidList.stream()
                .map(jsonObject -> jsonObject.toJavaObject(PaymentVO.class))
                .map(paymentVO -> new Message("payment", "cancel", paymentVO.getOrderId(), JSON.toJSONBytes(paymentVO)))
                .collect(Collectors.toList()));
        while (true) {
            if (paidLogUserCreditFuture.isDone() && paidMessageFuture.isDone() && unpaidMessageFuture.isDone() && storageMessageListFuture.isDone())
                break;
        }

        List<LogUserCredit> logUserCreditList = paidLogUserCreditFuture.get();
        try {
            logUserCreditService.saveBatch(logUserCreditList);
            userCreditMapper.batchUpdateByIdAndOldMoney(logUserCreditList);
        } catch (Exception e) {
            log.error("******PaymentRequestPayConsumer.consumeMessage：{}", e.getMessage());
        }

        asyncService.basicTask(() -> logPaymentService.saveBatch(convert(paymentVOList, LogPayment.class)));
        asyncMQMessage.sendMessage(paymentRequestPayMQProducer, storageMessageListFuture.get());
        log.info("已发送至storageDecrease");
        List<Message> paid = paidMessageFuture.get();
        if (!paid.isEmpty()) asyncMQMessage.sendMessage(paymentRequestPayMQProducer, paid);
        List<Message> unpaid = unpaidMessageFuture.get();
        if (!unpaid.isEmpty()) asyncMQMessage.sendMessage(paymentRequestPayMQProducer, unpaid);
        mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, messageExtMap.keySet())
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费"));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.stream()
                .map(messageExt -> (JSONObject) JSON.parse(messageExt.getBody()))
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

    private <T> List<T> convert(List<PaymentVO> paymentVOList, Class<T> clazz) {
        return paymentVOList.stream()
                .map(paymentVO -> (JSONObject) JSON.toJSON(paymentVO))
                .peek(jsonObject -> jsonObject.put("topic", "payment"))
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

    @Lazy
    @Autowired
    public void setPaymentRequestPayMQProducer(DefaultMQProducer paymentRequestPayMQProducer) {
        this.paymentRequestPayMQProducer = paymentRequestPayMQProducer;
    }
}
