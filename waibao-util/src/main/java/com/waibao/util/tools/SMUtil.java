package com.waibao.util.tools;

import com.antherd.smcrypto.sm2.Sm2;
import com.antherd.smcrypto.sm3.Sm3;
import com.antherd.smcrypto.sm4.Sm4;

/**
 * 国密工具类
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
public class SMUtil {
    public static final String SM2_PRIVATE_KEY = "64c9d8b69b241e1e9a9e426ed14dd4fafa022489d18b90ef673a99728bacb24b";
    public static final String SM2_PUBLIC_KEY = "0469b15775203d4d2f86c2a93343902ca5aed24af648c4f6ecfdcce093ffdb5dd8d02934d332fd6fe5772ea3e74ddd7ed13e6686150625684e268fd6b757c033e8";
    public static final String SM4_KEY = "3481f0a9d68b200ba6feff1f1b84cb82";

    private static String sm2Sign(String content) {
        return Sm2.doSignature(content, SM2_PRIVATE_KEY);
    }

    private static boolean sm2VerifySign(String clientPublicKey, String content, String sign) {
        return Sm2.doVerifySignature(content, sign, clientPublicKey);
    }

    public static String sm3Encode(String content) {
        return Sm3.sm3(content);
    }

    public static String sm4Encrypt(String content) {
        return Sm4.encrypt(content, SM4_KEY);
    }

    public static String sm4Decrypt(String content) {
        return Sm4.decrypt(content, SM4_KEY);
    }
}
