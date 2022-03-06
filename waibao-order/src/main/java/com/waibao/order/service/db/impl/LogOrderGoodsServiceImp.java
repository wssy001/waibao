package com.waibao.order.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.order.entity.LogOrderGoods;
import com.waibao.order.mapper.LogOrderGoodsMapper;
import com.waibao.order.service.db.LogOrderGoodsService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-03-05
 */
@Service
public class LogOrderGoodsServiceImp extends ServiceImpl<LogOrderGoodsMapper, LogOrderGoods> implements LogOrderGoodsService {

}
