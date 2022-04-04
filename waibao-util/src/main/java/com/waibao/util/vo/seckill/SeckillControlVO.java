package com.waibao.util.vo.seckill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SeckillControlVO
 *
 * @author alexpetertyler
 * @since 2022/4/3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillControlVO {
    private Long goodsId;
    private Boolean finished;
}
