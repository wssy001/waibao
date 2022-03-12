package com.waibao.util.vo.rcde;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * RiskUserVO
 *
 * @author alexpetertyler
 * @since 2022/3/11
 */
@Getter
@Setter
@Accessors(chain = true)
public class RiskUserVO {

    private Long goodsId;

    private Long userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RiskUserVO)) return false;
        RiskUserVO that = (RiskUserVO) o;
        return getGoodsId().equals(that.getGoodsId()) &&
                getUserId().equals(that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGoodsId(), getUserId());
    }
}
