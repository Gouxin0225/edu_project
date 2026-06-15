package com.example.edubackend.service;

import com.example.edubackend.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthTokenServiceTest {

    @Test
    void restoresValidTokenWhenRedisActiveTokenIsMissing() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        JwtUtil jwtUtil = jwtUtil();
        String token = jwtUtil.generateToken(7L, "admin", "ADMIN");
        AuthTokenService service = new AuthTokenService(redisTemplate, jwtUtil);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:user:7")).thenReturn(null);

        assertThat(service.isActiveOrRestoreToken(7L, token)).isTrue();
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("token:user:7"), eq(token), ttlCaptor.capture(), eq(TimeUnit.MILLISECONDS));
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    void rejectsDifferentActiveToken() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        AuthTokenService service = new AuthTokenService(redisTemplate, jwtUtil());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:user:7")).thenReturn("other-token");

        assertThat(service.isActiveOrRestoreToken(7L, "current-token")).isFalse();
    }

    private JwtUtil jwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        ReflectionTestUtils.setField(jwtUtil, "prefix", "Bearer ");
        return jwtUtil;
    }
}
