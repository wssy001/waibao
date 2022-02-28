package com.waibao.util.vo.seckill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * KillVO
 *
 * @author alexpetertyler
 * @since 2022-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGoodsVO {
    private Long goodsId;
    private Integer storage;
    private Integer currentStorage;
    private Integer purchaseLimit;
    private Date seckillStartTime;
    private Date seckillEndTime;
}
