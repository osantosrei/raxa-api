package com.raxa.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.raxa.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    @Test
    void corsConfigurationFailsFastWhenAllowedOriginsIsBlank() {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtAuthFilter.class), " , ");

        assertThatThrownBy(securityConfig::corsConfigurationSource)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FRONTEND_ORIGINS must not be blank");
    }
}
