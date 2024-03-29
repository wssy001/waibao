package com.waibao.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waibao.payment.entity.LogUserCredit;
import com.waibao.payment.entity.UserCredit;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * <p>
 * 账户信息表 Mapper 接口
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-04-05
 */
public interface UserCreditMapper extends BaseMapper<UserCredit> {

    void batchUpdateByIdAndOldMoney(@Param("logUserCreditList") List<LogUserCredit> logUserCreditList);
}
