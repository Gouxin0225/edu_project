package com.example.edubackend.scheduler;

import com.example.edubackend.service.IExamTakingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamAutoSubmitScheduler {

    private final IExamTakingService examTakingService;

    @Scheduled(fixedDelayString = "${edu.exam.auto-submit-scan-interval-ms:30000}")
    public void autoSubmitExpiredExams() {
        try {
            examTakingService.autoSubmitExpiredExams();
        } catch (Exception e) {
            log.error("Failed to scan expired exam submissions", e);
        }
    }
}
