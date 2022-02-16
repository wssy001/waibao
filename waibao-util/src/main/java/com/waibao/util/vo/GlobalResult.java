package com.waibao.util.vo;

import com.waibao.util.enums.ResultEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 全局结果返回
 *
 * @author alexpetertyler
 * @since 2022-01-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private int code;
    private String msg;
    private T data;
    private Date time;
    private Long timestamp;


    public static <T> GlobalResult<T> success() {
        return globalResult(200, "成功", null);
    }

    public static <T> GlobalResult<T> success(String msg) {
        return globalResult(200, msg, null);
    }

    public static <T> GlobalResult<T> success(T data) {
        return globalResult(200, "成功", data);
    }

    public static <T> GlobalResult<T> success(String msg, T data) {
        return globalResult(200, msg, data);
    }

    public static <T> GlobalResult<T> success(ResultEnum resultEnum) {
        return globalResult(resultEnum.getCode(), resultEnum.getDesc(), null);
    }

    public static <T> GlobalResult<T> success(ResultEnum resultEnum, T data) {
        return globalResult(resultEnum.getCode(), resultEnum.getDesc(), data);
    }

    public static <T> GlobalResult<T> success(int code, String msg, T data) {
        return globalResult(code, msg, data);
    }

    public static <T> GlobalResult<T> error() {
        return globalResult(-200, "失败", null);
    }

    public static <T> GlobalResult<T> error(String msg) {
        return globalResult(-200, msg, null);
    }

    public static <T> GlobalResult<T> error(T data) {
        return globalResult(-200, "失败", data);
    }

    public static <T> GlobalResult<T> error(String msg, T data) {
        return globalResult(-200, msg, data);
    }

    public static <T> GlobalResult<T> error(ResultEnum resultEnum) {
        return globalResult(resultEnum.getCode(), resultEnum.getDesc(), null);
    }

    public static <T> GlobalResult<T> error(int code, String msg, T data) {
        return globalResult(code, msg, data);
    }

    public static <T> GlobalResult<T> globalResult(int code, String msg, T data) {
        Date now = new Date();
        return new GlobalResult<>(code, msg, data, now, now.getTime());
    }

}
