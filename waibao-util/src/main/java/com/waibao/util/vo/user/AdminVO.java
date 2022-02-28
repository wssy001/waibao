package com.waibao.util.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AdminVO
 *
 * @author alexpetertyler
 * @since 2022-01-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String password;
    private Integer level = 1;

}
