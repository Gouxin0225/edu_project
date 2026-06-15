package com.example.edubackend.config;

import com.example.edubackend.dto.CreateExamFromTemplateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {

    @Test
    void objectMapperAcceptsIsoAndSpaceSeparatedLocalDateTime() throws Exception {
        ObjectMapper mapper = new AppConfig().objectMapper();

        CreateExamFromTemplateDTO dto = mapper.readValue("""
                {
                  "templateId": 1,
                  "title": "模板考试",
                  "startTime": "2026-06-22T08:30:00",
                  "endTime": "2026-06-23 00:00:00"
                }
                """, CreateExamFromTemplateDTO.class);

        assertEquals(LocalDateTime.of(2026, 6, 22, 8, 30), dto.getStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 23, 0, 0), dto.getEndTime());
    }
}
