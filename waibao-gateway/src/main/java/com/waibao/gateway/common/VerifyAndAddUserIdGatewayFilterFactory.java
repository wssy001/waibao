package com.waibao.gateway.common;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.waibao.util.base.BaseException;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.tools.JWTUtil;
import com.waibao.util.vo.user.UserVO;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * VerifyAndAddUserIdGatewayFilterFactory
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Component
public class VerifyAndAddUserIdGatewayFilterFactory extends AbstractNameValueGatewayFilterFactory {
    @Override
    public GatewayFilter apply(NameValueConfig config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StrUtil.isBlank(token)) throw new BaseException("token不存在，请重新登录");

            String[] split = token.split("\\.");
            String payloadBase64 = split[1];
            if (!JWTUtil.verify(split[0], payloadBase64, split[2])) throw new BaseException(ResultEnum.TOKEN_ERROR);

            UserVO userVO = JSON.parseObject(Base64.encode(payloadBase64), UserVO.class);

            URI uri = request.getURI();
            String rawQuery = uri.getQuery();
            Map<String, String> paramMap = HttpUtil.decodeParamMap(rawQuery, StandardCharsets.UTF_8);
            paramMap.put("userId", userVO.getId() + "");

            URI newUri = UriComponentsBuilder.fromUri(uri).replaceQuery(HttpUtil.toParams(paramMap)).build(true).toUri();
            request = exchange.getRequest().mutate().uri(newUri).build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }
}
