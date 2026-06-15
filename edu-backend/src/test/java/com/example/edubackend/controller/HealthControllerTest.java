package com.example.edubackend.controller;

import com.example.edubackend.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void reportsDatabaseAndRedisStatus() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection sqlConnection = mock(Connection.class);
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        HealthController controller = new HealthController(dataSource, redisConnectionFactory);
        ReflectionTestUtils.setField(controller, "activeProfile", "test");

        when(dataSource.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.isValid(1)).thenReturn(true);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        Result<Map<String, Object>> result = controller.health();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData())
                .containsEntry("status", "UP")
                .containsEntry("profile", "test")
                .containsEntry("database", "UP")
                .containsEntry("redis", "UP");
    }
}
