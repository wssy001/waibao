package com.waibao.util.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreditVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long money;
    private Date createTime;
    private Date updateTime;

}
