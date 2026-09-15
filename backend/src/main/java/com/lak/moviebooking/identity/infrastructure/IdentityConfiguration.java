package com.lak.moviebooking.identity.infrastructure;

import com.lak.moviebooking.common.config.AuthProperties;
import com.lak.moviebooking.identity.application.IdentityAuthenticationPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class IdentityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    IdentityAuthenticationPolicy identityAuthenticationPolicy(AuthProperties properties) {
        return new IdentityAuthenticationPolicy(
                properties.refreshTokenTtl(), properties.passwordResetTokenTtl(), properties.passwordResetUrl());
    }
}
