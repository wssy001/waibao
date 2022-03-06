package com.waibao.order.service.db.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.order.entity.MqMsgCompensation;
import com.waibao.order.mapper.MqMsgCompensationMapper;
import com.waibao.order.service.db.MqMsgCompensationService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-03-03
 */
@Service
public class MqMsgCompensationServiceImp extends ServiceImpl<MqMsgCompensationMapper, MqMsgCompensation> implements MqMsgCompensationService {

}
