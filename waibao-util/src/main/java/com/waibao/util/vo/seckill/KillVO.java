package com.waibao.util.vo.seckill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KillVO
 *
 * @author alexpetertyler
 * @since 2022-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KillVO {
    private String seckillPath;
    private Long goodsId;
    private Long userId;
    private Integer count;
}
