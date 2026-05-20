package com.example.edubackend.service;

import com.example.edubackend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final String TOKEN_KEY_PREFIX = "token:user:";
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public void replaceActiveToken(Long userId, String newToken) {
        String tokenKey = getTokenKey(userId);
        String currentToken = redisTemplate.opsForValue().get(tokenKey);
        if (StringUtils.hasText(currentToken) && !currentToken.equals(newToken)) {
            blacklistToken(currentToken);
        }

        long ttlMillis = getRemainingMillis(newToken);
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(tokenKey, newToken, ttlMillis, TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(tokenKey, newToken);
        }
    }

    public boolean isActiveToken(Long userId, String token) {
        if (userId == null || !StringUtils.hasText(token)) {
            return false;
        }
        String activeToken = redisTemplate.opsForValue().get(getTokenKey(userId));
        return StringUtils.hasText(activeToken) && activeToken.equals(token);
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(getBlacklistKey(token)));
    }

    public void invalidateUserToken(Long userId) {
        if (userId == null) {
            return;
        }
        String tokenKey = getTokenKey(userId);
        String activeToken = redisTemplate.opsForValue().get(tokenKey);
        if (StringUtils.hasText(activeToken)) {
            blacklistToken(activeToken);
        }
        redisTemplate.delete(tokenKey);
    }

    public void invalidateToken(String token) {
        blacklistToken(token);
    }

    private void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        long ttlMillis = getRemainingMillis(token);
        if (ttlMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(getBlacklistKey(token), "1", ttlMillis, TimeUnit.MILLISECONDS);
    }

    private long getRemainingMillis(String token) {
        try {
            Claims claims = jwtUtil.parseToken(token);
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return 0;
            }
            return Math.max(Duration.between(new Date().toInstant(), expiration.toInstant()).toMillis(), 0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String getTokenKey(Long userId) {
        return TOKEN_KEY_PREFIX + userId;
    }

    private String getBlacklistKey(String token) {
        return BLACKLIST_KEY_PREFIX + token;
    }
}
