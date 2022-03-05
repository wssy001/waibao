package com.waibao.payment.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.mapper.MqMsgCompensationMapper;
import com.waibao.payment.service.db.MqMsgCompensationService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-03-03
 */
@Service
public class MqMsgCompensationServiceImp extends ServiceImpl<MqMsgCompensationMapper, MqMsgCompensation> implements MqMsgCompensationService {

}