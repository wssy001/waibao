package com.waibao.util.vo.seckill;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * GoodsVO
 *
 * @author alexpetertyler
 * @since 2022/3/28
 */
@Data
@Accessors(chain = true)
public class GoodsVO {
    private Long id;

    private String name;

    private String description;

    private Date startTime;

    private Date endTime;

    private BigDecimal price;
}
