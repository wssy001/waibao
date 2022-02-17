import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Arrays;

/**
 * FunctionTest
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Slf4j
public class FunctionTest {
    @Test
    void test1() {
        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        byte[] privateKey = pair.getPrivate().getEncoded();
        byte[] publicKey = pair.getPublic().getEncoded();
//        log.info("******FunctionTest：{}", Arrays.toString(privateKey));
//        log.info("******FunctionTest：{}", Arrays.toString(publicKey));

        SM4 sm4 = SmUtil.sm4();
        byte[] encoded = sm4.getSecretKey().getEncoded();
        log.info("******FunctionTest：{}", Arrays.toString(encoded));
//        extracted(publicKey);

//        SM2 sm2 = SmUtil.sm2(privateKey, publicKey);
//// 公钥加密，私钥解密
//        String encryptStr = sm2.encryptBcd(text, KeyType.PublicKey);
//        String decryptStr = StrUtil.utf8Str(sm2.decryptFromBcd(encryptStr, KeyType.PrivateKey));
    }


}
