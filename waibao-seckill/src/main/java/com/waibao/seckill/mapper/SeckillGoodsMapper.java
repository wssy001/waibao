package com.waibao.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waibao.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    @Update({"UPDATE seckill_goods SET storage = storage - #{totalStorage} WHERE goods_id = #{goodsId} AND storage > #{totalStorage};"})
    int decreaseStorage(@Param("goodsId") Long goodsId, @Param("totalStorage") Integer totalStorage);
}
