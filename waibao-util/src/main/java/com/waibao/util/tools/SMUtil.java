package com.waibao.util.tools;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;

/**
 * 国密工具类
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
public class SMUtil {
    private static final byte[] SM2_PRIVATE_KEY = {48, -127, -109, 2, 1, 0, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -127, 28, -49, 85, 1, -126, 45, 4, 121, 48, 119, 2, 1, 1, 4, 32, -24, 46, 108, 42, 106, 61, -16, -12, -79, 70, -84, 63, 82, 100, -13, -27, 34, -39, 57, 27, 119, 4, 109, 93, -24, -5, 125, 117, 20, 35, -85, 74, -96, 10, 6, 8, 42, -127, 28, -49, 85, 1, -126, 45, -95, 68, 3, 66, 0, 4, 78, -89, 44, 107, 81, -25, -66, -71, -99, -22, 63, -84, 50, 4, 113, 32, -94, -50, -85, -112, 74, -95, 57, 26, -9, 45, -120, 63, -32, -64, 122, 43, 76, -69, 73, 90, 91, 89, -52, 11, -124, 2, 9, 29, 89, 98, -86, 118, -19, 90, -59, 24, -51, 14, -7, 20, -26, -1, -35, 38, -79, -65, 32, 100};
    private static final byte[] SM2_PUBLIC_KEY = {48, 89, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -127, 28, -49, 85, 1, -126, 45, 3, 66, 0, 4, 78, -89, 44, 107, 81, -25, -66, -71, -99, -22, 63, -84, 50, 4, 113, 32, -94, -50, -85, -112, 74, -95, 57, 26, -9, 45, -120, 63, -32, -64, 122, 43, 76, -69, 73, 90, 91, 89, -52, 11, -124, 2, 9, 29, 89, 98, -86, 118, -19, 90, -59, 24, -51, 14, -7, 20, -26, -1, -35, 38, -79, -65, 32, 100};

    private static final byte[] SM4_KEY = {-84, -77, -82, 60, 123, -76, -53, 76, 127, -25, -78, 17, -53, -81, -12, 25};

    private static String sm2Sign(byte[] clientPublicKey, String content) {
        return SmUtil.sm2(SM2_PRIVATE_KEY, clientPublicKey).signHex(HexUtil.encodeHexStr(content));
    }

    private static boolean sm2VerifySign(byte[] clientPublicKey, String content, String sign) {
        return SmUtil.sm2(SM2_PRIVATE_KEY, clientPublicKey).verifyHex(HexUtil.encodeHexStr(content), sign);
    }

    public static String sm3Encode(String content) {
        return SmUtil.sm3(content);
    }

    public static String sm4Encrypt(String content) {
        return new SM4(SM4_KEY).encryptBase64(content);
    }

    public static String sm4Decrypt(String content) {
        return new SM4(SM4_KEY).decryptStr(content);
    }
}
