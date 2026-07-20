package com.tsy.oa.user.auth.config;

import com.tsy.oa.common.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class JwtConfiguration {

    @Bean
    public JwtTokenService jwtTokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.validity}") Duration validity
    ) {
        return new JwtTokenService(secret, validity);
    }
}
