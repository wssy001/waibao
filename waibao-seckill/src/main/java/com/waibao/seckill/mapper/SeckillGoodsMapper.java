package com.waibao.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waibao.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Update("BEGIN;" + "SELECT goods_id FROM seckill_goods WHERE goods_id = #{goodsId} FOR UPDATE;" +
            "UPDATE seckill_goods SET storage = storage - #{totalStorage} WHERE goods_id = #{goodsId} AND storage >= #{totalStorage};" +
            "COMMIT;")
    int decreaseStorage(@Param("goodsId") Long goodsId, @Param("totalStorage") Integer totalStorage);

    @Update("BEGIN;" + "SELECT goods_id FROM seckill_goods WHERE goods_id = #{goodsId} FOR UPDATE;" +
            "UPDATE seckill_goods SET storage = storage + #{totalStorage} WHERE goods_id = #{goodsId};" +
            "COMMIT;")
    void increaseStorage(@Param("goodsId") Long goodsId, @Param("totalStorage") Integer totalStorage);

    @Select("SELECT storage FROM seckill_goods WHERE goods_id = #{goodsId}")
    int selectTrueStorage(@Param("goodsId") Long goodsId);
}
