import cn.hutool.http.HttpUtil;
import com.waibao.util.tools.JWTUtil;
import com.waibao.util.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * FunctionTest
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Slf4j
public class FunctionTest {

    @Test
    void test2() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/examples")
                .path("123")
                .build();
        Map<String, String> stringStringMap = HttpUtil.decodeParamMap(uri.getRawQuery(), StandardCharsets.UTF_8);
        log.info("******FunctionTest：");
    }

    @Test
    void test3() {
        UserVO userVO = new UserVO();
        userVO.setNickname("test");
        userVO.setPassword("saaskhhkaskjsjkahjha");
        userVO.setId(111L);
        log.info("******FunctionTest.test3：{}", JWTUtil.create(userVO));
    }

    @Test
    void test4(){
        log.info("******{}：开始读取数据库放入缓存", this.getClass().getSimpleName());
    }
}
