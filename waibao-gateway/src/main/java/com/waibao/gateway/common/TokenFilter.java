package com.waibao.gateway.common;

import com.waibao.util.base.BaseException;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.tools.JWTUtil;
import com.waibao.util.vo.user.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * @Author: wwj
 * @Date: 2022/2/17 14:59
 */
@Component
public class TokenFilter implements GlobalFilter, Ordered {
    private static final String TOKEN = "token";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("经过第0个过滤器TokenFilter");
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        String url = request.getURI().getPath();
        if(url.equals("/admin/login") || url.equals("/user/login")){
            return chain.filter(exchange);
        }
        String token = headers.getFirst(TOKEN);
        if (StringUtils.isEmpty(token)) {
            throw new BaseException("token不存在，请重新登录");
        }
        // 校验token
        if (!JWTUtil.verify(token)) {
            throw new BaseException(ResultEnum.TOKEN_ERROR);
        }

//        UserVO userVo = JWTUtil.getUserVo(token);
//        if (userVo == null || userVo.getUserNo() < 0) {
//            throw new BaseException(ResultEnum.TOKEN_ERROR);
//        }
//        Long userNo = userVo.getUserNo();
//        // 更新时间，使token不过期
//        stringRedisTemplate.opsForValue().set(userNo.toString(), token, 1, TimeUnit.HOURS);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
