/**
 * Copyright 2015-现在 广州市领课网络科技有限公司
 */
package com.waibao.util.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultEnum {
    // 成功
    SUCCESS(200, "成功"),

    // token异常
    TOKEN_PAST(301, "token过期"), TOKEN_ERROR(302, "token异常"),
    // 登录异常
    LOGIN_ERROR(303, "登录异常"), REMOTE_ERROR(304, "异地登录"),

    MENU_PAST(305, "菜单过期"), MENU_NO(306, "没此权限，请联系管理员！"),

    // 用户异常，5开头
    USER_IS_NOT_EXIST(501, "用户不存在"),
    //
    USER_SAVE_FAIL(504, "添加失败"), USER_UPDATE_FAIL(505, "更新失败"), USER_SEND_FAIL(508, "发送失败"),
    USER_DELETE_FAIL(509, "删除失败"),

    // 系統异常，6开头
    SYSTEM_SAVE_FAIL(601, "添加失败"), SYSTEM_UPDATE_FAIL(602, "更新失败"), SYSTEM_DELETE_FAIL(603, "删除失败"),

    // 错误
    ERROR(999, "错误");

    private Integer code;

    private String desc;

}
