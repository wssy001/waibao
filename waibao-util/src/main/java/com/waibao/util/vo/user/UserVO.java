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
    private String mobile;
    private String email;
    private String password;
    private Integer sex;
    private Integer age;
    private String nickname;
    private Boolean defaulter;
    private String workStatus;
    private Long expireTime;

    public void hideMobile() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.delete(0, stringBuffer.length());
        stringBuffer.append(this.mobile);
        stringBuffer.replace(3, 7, "*");
        this.mobile = stringBuffer.toString();
    }
}
