package com.waibao.util.vo.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

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

    private String payId;

    private Long goodsId;

    private Long userId;

    private Long retailerId;

    private BigDecimal goodsPrice;

    private Integer count;

    private BigDecimal orderPrice;

    private Date purchaseTime;

    private Boolean paid = false;

    private String status;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderVO)) return false;
        OrderVO that = (OrderVO) o;
        return getOrderId().equals(that.getOrderId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrderId());
    }
}
