package com.waibao.util.vo.payment;

import com.waibao.util.vo.order.OrderVO;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVO extends OrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private BigDecimal money;

    private String status;
}
