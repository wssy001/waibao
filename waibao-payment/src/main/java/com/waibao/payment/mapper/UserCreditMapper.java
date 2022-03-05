package com.waibao.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.entity.UserCreditExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCreditMapper extends BaseMapper<UserCredit> {
    long countByExample(UserCreditExample example);

    int deleteByExample(UserCreditExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserCredit record);

    int insertSelective(UserCredit record);

    List<UserCredit> selectByExample(UserCreditExample example);

    UserCredit selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") UserCredit record, @Param("example") UserCreditExample example);

    int updateByExample(@Param("record") UserCredit record, @Param("example") UserCreditExample example);

    int updateByPrimaryKeySelective(UserCredit record);

    int updateByPrimaryKey(UserCredit record);
}