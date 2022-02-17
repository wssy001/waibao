/**
 * Copyright 2015-现在 广州市领课网络科技有限公司
 */
package com.waibao.util.tools;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSON;
import com.waibao.util.vo.user.UserVO;

/**
 * @author wujing
 */
public final class JWTUtil {
    private static final String HEADER = "{\"alg\":\"SM3\",\"typ\":\"JWT\"}";

    public static String create(UserVO userVO) {
        String header = Base64.encode(HEADER);
        String payload = Base64.encode(JSON.toJSONString(userVO));
        return header + "." + payload + "." + SMUtil.sm3Encode(header + "." + payload);
    }

    public static boolean verify(String jwt) {
        String[] split = jwt.split("\\.");
        if (split.length != 3) return false;

        return SMUtil.sm3Encode(split[0] + "." + split[1]).equals(split[2]);
    }

    public static UserVO getUserVo(String jwt) {
        String[] split = jwt.split("\\.");
        return JSON.parseObject(split[1], UserVO.class);
    }
}
