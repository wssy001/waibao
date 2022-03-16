package com.waibao.util.vo.rcde;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * RuleVO
 *
 * @author alexpetertyler
 * @since 2022/3/11
 */
@Getter
@Setter
@Accessors(chain = true)
public class RuleVO {
    private Long id;

    private Integer allowOverdueDelayedDays;

    private Long goodsId;

    private Long checkUserId;

    private boolean denyDefaulter = true;

    private Integer collectYears;

    private Integer denyOverdueTimes;

    private Integer ignoreOverdueAmount;

    private Integer denyAgeBelow;

    private Integer userType = 0;

    private Integer ruleCode;
}
