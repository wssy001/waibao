package com.waibao.util.tools;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSON;
import com.waibao.util.vo.user.AdminVO;
import com.waibao.util.vo.user.UserVO;

public final class JWTUtil {
    private static final String HEADER = "{\"alg\":\"SM3\",\"typ\":\"JWT\"}";

    public static String create(UserVO userVO) {
        String header = Base64.encode(HEADER);
        String payload = Base64.encode(JSON.toJSONString(userVO));
        return header + "." + payload + "." + SMUtil.sm3Encode(header + "." + payload);
    }

    public static String create(AdminVO adminVO) {
        String header = Base64.encode(HEADER);
        String payload = Base64.encode(JSON.toJSONString(adminVO));
        return header + "." + payload + "." + SMUtil.sm3Encode(header + "." + payload);
    }

    public static boolean verify(String jwt) {
        String[] split = jwt.split("\\.");
        if (split.length != 3) return false;

        return verify(split[0], split[1], split[2]);
    }

    public static boolean verify(String headerBase64, String payloadBase64, String sign) {
        return SMUtil.sm3Encode(headerBase64 + "." + payloadBase64).equals(sign);
    }

    public static UserVO getUserVO(String jwt) {
        String[] split = jwt.split("\\.");
        return JSON.parseObject(Base64.decode(split[1]), UserVO.class);
    }

    public static AdminVO getAdminVO(String jwt) {
        String[] split = jwt.split("\\.");
        return JSON.parseObject(Base64.decode(split[1]), AdminVO.class);
    }
}
