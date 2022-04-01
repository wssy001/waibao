package com.waibao.gateway.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.waibao.util.base.BaseException;
import com.waibao.util.tools.JWTUtil;
import com.waibao.util.vo.user.AdminVO;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * VerifyAndAddAdminIdGatewayFilterFactory
 *
 * @author alexpetertyler
 * @since 2022-04-01
 */
@Component
public class VerifyAndAddAdminIdGatewayFilterFactory extends AbstractNameValueGatewayFilterFactory {
    @Override
    public GatewayFilter apply(NameValueConfig config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StrUtil.isBlank(token)) throw new BaseException("token不存在，请重新登录");
            if (!JWTUtil.verify(token)) throw new BaseException("token非法");

            AdminVO adminVO = JWTUtil.getAdminVO(token);
            URI uri = request.getURI();
            String rawQuery = uri.getQuery();
            Map<String, String> paramMap = new HashMap<>();
            if (StrUtil.isBlank(rawQuery)) {
                paramMap.put("userId", adminVO.getId() + "");
            } else {
                paramMap.putAll(HttpUtil.decodeParamMap(rawQuery, StandardCharsets.UTF_8));
            }

            URI newUri = UriComponentsBuilder.fromUri(uri).replaceQuery(HttpUtil.toParams(paramMap)).build(true).toUri();
            request = exchange.getRequest().mutate().uri(newUri).build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }
}
