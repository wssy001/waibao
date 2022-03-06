import cn.hutool.http.HttpUtil;
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
}
