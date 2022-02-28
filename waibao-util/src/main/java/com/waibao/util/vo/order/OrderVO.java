package com.waibao.util.vo.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * OrderVO
 *
 * @author alexpetertyler
 * @since 2022/2/19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {
    private String orderId;

    private Long goodsId;

    private Long userId;

    private Long retailerId;

    private BigDecimal goodsPrice;

    private BigDecimal orderPrice;

    private Date purchaseTime;

    private Boolean paid = false;

    private String status;
}
