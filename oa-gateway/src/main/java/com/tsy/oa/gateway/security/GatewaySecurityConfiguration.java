package com.tsy.oa.gateway.security;

import com.tsy.oa.common.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewaySecurityConfiguration {

    @Bean
    public JwtTokenService jwtTokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.validity}") Duration validity
    ) {
        return new JwtTokenService(secret, validity);
    }
}
