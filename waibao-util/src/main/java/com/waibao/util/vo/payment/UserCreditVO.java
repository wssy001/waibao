package com.waibao.util.vo.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreditVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private BigDecimal money;

}
