package com.waibao.seckill.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.waibao.seckill.entity.Goods;
import com.waibao.seckill.mapper.GoodsMapper;
import com.waibao.seckill.service.db.GoodsService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-03-30
 */
@Service
public class GoodsServiceImp extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

}
