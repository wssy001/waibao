package com.waibao.util.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * UserVo
 *
 * @author alexpetertyler
 * @since 2022-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userNo;
    private String mobile;
    private String eamil;
    private String password;
    private Integer sex;
    private Integer age;
    private String nickname;
    private Long expireTime;

    public void hideMobile() {
        StringBuffer stringBuffer = new StringBuffer(this.mobile);
        stringBuffer.replace(3, 7, "*");
        this.mobile = stringBuffer.toString();
    }
}
