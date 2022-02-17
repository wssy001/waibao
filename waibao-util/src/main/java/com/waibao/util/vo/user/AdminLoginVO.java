package com.waibao.util.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: wwj
 * @Date: 2022/2/17 17:06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String password;
    private Integer level = 1;
    private String token;
}
