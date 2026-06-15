package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.*;
import com.example.edubackend.entity.*;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.service.*;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamTakingServiceImpl implements IExamTakingService {

    private final IAssessmentTaskService assessmentTaskService;
    private final IStudentSubmissionService submissionService;
    private final IStudentAnswerDetailService answerDetailService;
    private final IStudentMistakeBookService mistakeBookService;
    private final ITaskQuestionRelService taskQuestionRelService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionBankMapper questionBankMapper;
    private final SysUserMapper sysUserMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final SurveyTaskMapper surveyTaskMapper;
    private final SurveyRecordMapper surveyRecordMapper;
    private final ExamSettingHelper examSettingHelper;
    private final TransactionTemplate transactionTemplate;

    private static final String REDIS_KEY_PREFIX = "exam:progress:";
    private static final long REDIS_EXPIRE_HOURS = 24;

    @Override
    @Transactional
    public ExamStartVO startExam(Long examId, Long studentId) {
        AssessmentTask exam = assessmentTaskService.getById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanTakeExam(exam, studentId);

        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BusinessException(400, "考试尚未开始");
        }
        if (now.isAfter(exam.getEndTime())) {
            throw new BusinessException(400, "考试已结束");
        }

        StudentSubmission existing = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getStudentId, studentId)
        );
        if (existing != null) {
            if ("SUBMITTED".equals(existing.getStatus()) || "GRADED".equals(existing.getStatus())) {
                throw new BusinessException(400, "您已参加过本次考试");
            }
            ExamProgressVO progress = restoreFromDatabase(exam, existing);
            return toStartVO(exam, existing, progress.getQuestions());
        }

        StudentSubmission submission = new StudentSubmission();
        submission.setTaskId(examId);
        submission.setStudentId(studentId);
        submission.setStatus("UN");
        submission.setStartTime(now);
        submission.setVersion(1);
        submission.setSwitchScreenCount(0);
        submissionService.save(submission);

        List<ExamQuestionVO> examQuestions = buildExamQuestions(examId, Map.of());

        String redisKey = getRedisKey(examId, studentId);
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("submissionId", submission.getId());
        progressData.put("questions", examQuestions);
        progressData.put("switchScreenCount", 0);
        try {
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(progressData),
                    REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to write exam progress to Redis", e);
        }

        return toStartVO(exam, submission, examQuestions);
    }

    @Override
    public ExamProgressVO resumeExam(Long examId, Long studentId) {
        AssessmentTask exam = assessmentTaskService.getById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanTakeExam(exam, studentId);

        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getStudentId, studentId)
        );
        if (submission == null) {
            throw new BusinessException(404, "未找到考试记录");
        }

        String redisKey = getRedisKey(examId, studentId);
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            try {
                Map<String, Object> progressData = objectMapper.readValue(cached,
                        new TypeReference<Map<String, Object>>() {});
                ExamProgressVO vo = new ExamProgressVO();
                vo.setSubmissionId(((Number) progressData.get("submissionId")).longValue());
                Object switchScreenCount = progressData.get("switchScreenCount");
                vo.setSwitchScreenCount(switchScreenCount instanceof Number number ? number.intValue() : 0);
                vo.setStatus(submission.getStatus());
                applyProgressMetadata(vo, exam, submission);

                List<ExamQuestionVO> questions = objectMapper.convertValue(progressData.get("questions"),
                        new TypeReference<List<ExamQuestionVO>>() {});
                vo.setQuestions(questions);

                return vo;
            } catch (Exception e) {
                log.error("Failed to parse Redis cached exam progress", e);
            }
        }

        return restoreFromDatabase(exam, submission);
    }

    private ExamProgressVO restoreFromDatabase(AssessmentTask exam, StudentSubmission submission) {
        List<StudentAnswerDetail> answers = answerDetailService.list(
                new LambdaQueryWrapper<StudentAnswerDetail>()
                        .eq(StudentAnswerDetail::getSubmissionId, submission.getId())
        );
        Map<Long, String> answerMap = new HashMap<>();
        for (StudentAnswerDetail a : answers) {
            answerMap.put(a.getQuestionId(), a.getStudentAnswer());
        }

        List<ExamQuestionVO> examQuestions = buildExamQuestions(exam.getId(), answerMap);

        String redisKey = getRedisKey(exam.getId(), submission.getStudentId());
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("submissionId", submission.getId());
        progressData.put("questions", examQuestions);
        progressData.put("switchScreenCount", submission.getSwitchScreenCount());
        try {
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(progressData),
                    REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to write exam progress to Redis after restore", e);
        }

        ExamProgressVO vo = new ExamProgressVO();
        vo.setSubmissionId(submission.getId());
        vo.setQuestions(examQuestions);
        vo.setSwitchScreenCount(submission.getSwitchScreenCount());
        vo.setStatus(submission.getStatus());
        applyProgressMetadata(vo, exam, submission);
        return vo;
    }

    @Override
    @Transactional
    public void saveAnswers(Long examId, Long studentId, SaveAnswerDTO dto) {
        AssessmentTask exam = assessmentTaskService.getById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanTakeExam(exam, studentId);

        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getStudentId, studentId)
        );

        if (submission == null) {
            throw new BusinessException(404, "未找到考试记录");
        }
        if ("SUBMITTED".equals(submission.getStatus()) || "GRADED".equals(submission.getStatus())) {
            throw new BusinessException(400, "考试已提交，无法继续作答");
        }

        for (SaveAnswerDTO.AnswerItem item : dto.getAnswers()) {
            StudentAnswerDetail answer = answerDetailService.getOne(
                    new LambdaQueryWrapper<StudentAnswerDetail>()
                            .eq(StudentAnswerDetail::getSubmissionId, submission.getId())
                            .eq(StudentAnswerDetail::getQuestionId, item.getQuestionId())
            );

            if (answer == null) {
                answer = new StudentAnswerDetail();
                answer.setSubmissionId(submission.getId());
                answer.setQuestionId(item.getQuestionId());
                answer.setStudentAnswer(item.getAnswerValue());
                answerDetailService.save(answer);
            } else {
                answer.setStudentAnswer(item.getAnswerValue());
                answerDetailService.updateById(answer);
            }
        }

        String redisKey = getRedisKey(examId, studentId);
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                Map<String, Object> progressData = objectMapper.readValue(cached,
                        new TypeReference<Map<String, Object>>() {});
                List<ExamQuestionVO> questions = objectMapper.convertValue(progressData.get("questions"),
                        new TypeReference<List<ExamQuestionVO>>() {});

                Map<Long, String> answerMap = new HashMap<>();
                for (SaveAnswerDTO.AnswerItem item : dto.getAnswers()) {
                    answerMap.put(item.getQuestionId(), item.getAnswerValue());
                }

                for (ExamQuestionVO q : questions) {
                    if (answerMap.containsKey(q.getQuestionId())) {
                        q.setStudentAnswer(answerMap.get(q.getQuestionId()));
                    }
                }

                progressData.put("questions", questions);
                redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(progressData),
                        REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.error("Failed to update Redis cache", e);
            }
        }
    }

    @Override
    public ExamSubmitRequirementVO getSubmitRequirement(Long examId, Long studentId) {
        AssessmentTask exam = assessmentTaskService.getById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanTakeExam(exam, studentId);
        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getStudentId, studentId)
        );
        if (submission != null && isSubmissionExpired(exam, submission, LocalDateTime.now())) {
            ExamSubmitRequirementVO vo = new ExamSubmitRequirementVO();
            vo.setRequireSurvey(false);
            vo.setSurveySubmitted(true);
            vo.setSurveyExpired(false);
            vo.setMessage("考试时间已到，将直接交卷");
            return vo;
        }
        return buildSubmitRequirement(exam, studentId);
    }

    @Override
    @Transactional
    public void submitExam(Long examId, Long studentId) {
        AssessmentTask exam = assessmentTaskService.getById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanTakeExam(exam, studentId);

        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getStudentId, studentId)
        );

        if (submission == null) {
            throw new BusinessException(404, "未找到考试记录");
        }

        if ("SUBMITTED".equals(submission.getStatus()) || "GRADED".equals(submission.getStatus())) {
            throw new BusinessException(400, "考试已提交，无法重复提交");
        }
        if (!isSubmissionExpired(exam, submission, LocalDateTime.now())) {
            assertRequiredSurveySubmitted(exam, studentId);
        }
        submitSubmission(exam, submission);
    }

    @Override
    public int autoSubmitExpiredExams() {
        LocalDateTime now = LocalDateTime.now();
        List<StudentSubmission> runningSubmissions = submissionService.list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getStatus, "UN")
                        .isNotNull(StudentSubmission::getStartTime)
        );

        int submittedCount = 0;
        for (StudentSubmission runningSubmission : runningSubmissions) {
            try {
                AssessmentTask exam = assessmentTaskService.getById(runningSubmission.getTaskId());
                if (!shouldAutoSubmit(exam, runningSubmission, now)) {
                    continue;
                }
                StudentSubmission latestSubmission = submissionService.getById(runningSubmission.getId());
                if (latestSubmission == null || !"UN".equals(latestSubmission.getStatus())) {
                    continue;
                }
                transactionTemplate.executeWithoutResult(status -> submitSubmission(exam, latestSubmission));
                submittedCount++;
            } catch (Exception e) {
                log.error("Failed to auto submit expired exam submission {}", runningSubmission.getId(), e);
            }
        }
        if (submittedCount > 0) {
            log.info("Auto submitted {} expired exam submissions", submittedCount);
        }
        return submittedCount;
    }

    private boolean shouldAutoSubmit(AssessmentTask exam, StudentSubmission submission, LocalDateTime now) {
        if (exam == null || submission == null) {
            return false;
        }
        if (!"EXAM".equals(exam.getType()) || !examSettingHelper.isPublished(exam)) {
            return false;
        }
        LocalDateTime deadline = examSettingHelper.resolveExamDeadline(exam, submission.getStartTime());
        return deadline != null && !deadline.isAfter(now);
    }

    private boolean isSubmissionExpired(AssessmentTask exam, StudentSubmission submission, LocalDateTime now) {
        LocalDateTime deadline = examSettingHelper.resolveExamDeadline(exam, submission.getStartTime());
        return deadline != null && !deadline.isAfter(now);
    }

    private void submitSubmission(AssessmentTask exam, StudentSubmission submission) {
        Long examId = exam.getId();
        Long studentId = submission.getStudentId();
        List<TaskQuestionRel> rels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
                        .orderByAsc(TaskQuestionRel::getSortOrder)
        );
        if (rels.isEmpty()) {
            throw new BusinessException(400, "试卷没有题目");
        }

        Map<Long, QuestionBank> questionMap = loadQuestionMap(rels);
        if (questionMap.isEmpty()) {
            throw new BusinessException(400, "试卷题目不存在或已删除");
        }
        Map<Long, Integer> scoreMap = new HashMap<>();
        for (TaskQuestionRel rel : rels) {
            scoreMap.put(rel.getQuestionId(), rel.getScore());
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        Set<Long> mistakeQuestionIds = new LinkedHashSet<>();
        boolean hasSubjective = false;

        List<StudentAnswerDetail> answers = answerDetailService.list(
                new LambdaQueryWrapper<StudentAnswerDetail>()
                        .eq(StudentAnswerDetail::getSubmissionId, submission.getId())
        );
        Map<Long, StudentAnswerDetail> answerMap = new HashMap<>();
        for (StudentAnswerDetail a : answers) {
            answerMap.put(a.getQuestionId(), a);
        }

        for (TaskQuestionRel rel : rels) {
            QuestionBank question = questionMap.get(rel.getQuestionId());
            if (question == null) continue;

            StudentAnswerDetail answer = answerMap.get(rel.getQuestionId());
            String studentAnswer = answer != null ? answer.getStudentAnswer() : null;
            boolean unanswered = studentAnswer == null || studentAnswer.isBlank();

            if (isObjectiveQuestion(question)) {
                boolean isCorrect = isObjectiveCorrect(question, studentAnswer);
                BigDecimal scoreGained = isCorrect ? BigDecimal.valueOf(scoreMap.get(rel.getQuestionId())) : BigDecimal.ZERO;

                if (answer == null) {
                    answer = new StudentAnswerDetail();
                    answer.setSubmissionId(submission.getId());
                    answer.setQuestionId(rel.getQuestionId());
                    answer.setStudentAnswer(studentAnswer);
                }
                answer.setIsCorrect(isCorrect ? (byte) 1 : (byte) 0);
                answer.setScoreGained(scoreGained);
                answerDetailService.saveOrUpdate(answer);

                totalScore = totalScore.add(scoreGained);
                if (unanswered || !isCorrect) {
                    mistakeQuestionIds.add(rel.getQuestionId());
                }
            } else {
                hasSubjective = true;
                if (answer == null) {
                    answer = new StudentAnswerDetail();
                    answer.setSubmissionId(submission.getId());
                    answer.setQuestionId(rel.getQuestionId());
                    answer.setStudentAnswer(studentAnswer);
                    answerDetailService.save(answer);
                }
                if (unanswered || isSubjectiveQuestion(question)) {
                    mistakeQuestionIds.add(rel.getQuestionId());
                }
            }
        }

        for (Long questionId : mistakeQuestionIds) {
            upsertMistake(studentId, questionId);
        }

        submission.setStatus(hasSubjective ? "SUBMITTED" : "GRADED");
        submission.setTotalScoreGained(totalScore);
        submission.setSubmitTime(LocalDateTime.now());
        submissionService.updateById(submission);

        String redisKey = getRedisKey(examId, studentId);
        redisTemplate.delete(redisKey);
    }

    @Override
    @Transactional
    public int reportScreenSwitch(Long examId, Long studentId) {
        AssessmentTask exam = assessmentTaskService.getById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertCanTakeExam(exam, studentId);

        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getStudentId, studentId)
        );

        if (submission == null) {
            throw new BusinessException(404, "未找到考试记录");
        }

        int newCount = (submission.getSwitchScreenCount() != null ? submission.getSwitchScreenCount() : 0) + 1;
        submission.setSwitchScreenCount(newCount);
        submissionService.updateById(submission);

        String redisKey = getRedisKey(examId, studentId);
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                Map<String, Object> progressData = objectMapper.readValue(cached,
                        new TypeReference<Map<String, Object>>() {});
                progressData.put("switchScreenCount", newCount);
                redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(progressData),
                        REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.error("Failed to update screen switch count in Redis", e);
            }
        }

        int limit = examSettingHelper.getSwitchScreenLimit(exam);
        if (newCount >= limit) {
            log.info("Screen switch limit exceeded for exam {} student {}, forcing submit", examId, studentId);
            if (isRequiredSurveyPending(exam, studentId)) {
                log.info("Exam {} student {} is blocked by required survey, defer forced submit to client", examId, studentId);
            } else {
                submitExam(examId, studentId);
            }
        }

        return newCount;
    }

    private ExamSubmitRequirementVO buildSubmitRequirement(AssessmentTask exam, Long studentId) {
        ExamSubmitRequirementVO vo = new ExamSubmitRequirementVO();
        vo.setRequireSurvey(false);
        vo.setSurveySubmitted(true);
        vo.setSurveyExpired(false);

        if (!examSettingHelper.isSurveyRequiredBeforeSubmit(exam)) {
            return vo;
        }

        Long surveyId = examSettingHelper.getRequiredSurveyId(exam);
        SurveyTask survey = getRequiredSurvey(surveyId);
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null || !isSurveyTargetedToStudent(survey, student)) {
            throw new BusinessException(403, "无权提交考试绑定的问卷");
        }

        vo.setRequireSurvey(true);
        vo.setSurveyId(survey.getId());
        vo.setSurveyTitle(survey.getTitle());
        boolean submitted = hasSubmittedSurvey(survey.getId(), studentId);
        boolean expired = isRequiredSurveyExpired(survey);
        vo.setSurveyExpired(expired);
        vo.setSurveySubmitted(submitted);
        if (submitted) {
            vo.setMessage("已完成交卷前问卷");
        } else if (expired) {
            vo.setMessage("绑定的调查问卷已截止且未填写，无法提交试卷");
        } else {
            vo.setMessage("请先完成调查问卷后再提交试卷");
        }
        return vo;
    }

    private void assertRequiredSurveySubmitted(AssessmentTask exam, Long studentId) {
        if (!isRequiredSurveyPending(exam, studentId)) {
            return;
        }
        throw new BusinessException(400, "请先完成调查问卷后再提交试卷");
    }

    private boolean isRequiredSurveyPending(AssessmentTask exam, Long studentId) {
        if (!examSettingHelper.isSurveyRequiredBeforeSubmit(exam)) {
            return false;
        }
        Long surveyId = examSettingHelper.getRequiredSurveyId(exam);
        SurveyTask survey = getRequiredSurvey(surveyId);
        if (survey.getStatus() == null || survey.getStatus() != 1) {
            throw new BusinessException(400, "考试绑定的问卷尚未发布");
        }
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null || !isSurveyTargetedToStudent(survey, student)) {
            throw new BusinessException(403, "无权提交考试绑定的问卷");
        }
        return !hasSubmittedSurvey(survey.getId(), studentId);
    }

    private boolean isRequiredSurveyExpired(SurveyTask survey) {
        return survey.getEndTime() != null && LocalDateTime.now().isAfter(survey.getEndTime());
    }

    private SurveyTask getRequiredSurvey(Long surveyId) {
        if (surveyId == null || surveyId <= 0) {
            throw new BusinessException(400, "考试未配置交卷前问卷");
        }
        SurveyTask survey = surveyTaskMapper.selectById(surveyId);
        if (survey == null) {
            throw new BusinessException(404, "考试绑定的问卷不存在");
        }
        return survey;
    }

    private boolean hasSubmittedSurvey(Long surveyId, Long studentId) {
        Long count = surveyRecordMapper.selectCount(new LambdaQueryWrapper<SurveyRecord>()
                .eq(SurveyRecord::getSurveyId, surveyId)
                .eq(SurveyRecord::getStudentId, studentId));
        return count != null && count > 0;
    }

    private boolean isSurveyTargetedToStudent(SurveyTask survey, SysUser student) {
        Set<Long> studentClassIds = getStudentClassIds(student);
        if (studentClassIds.isEmpty()) {
            return false;
        }
        List<Long> surveyClassIds = parseTargetClassIds(survey.getTargetClassIds());
        return surveyClassIds.isEmpty() || surveyClassIds.stream().anyMatch(studentClassIds::contains);
    }

    private boolean isObjectiveCorrect(QuestionBank question, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return false;
        }

        String standardAnswer = question.getStandardAnswer();
        if (standardAnswer == null) {
            return false;
        }

        standardAnswer = standardAnswer.trim().toUpperCase();
        studentAnswer = studentAnswer.trim().toUpperCase();

        if ("JUDGE".equals(question.getType())) {
            Boolean standardJudge = parseJudgeAnswer(standardAnswer);
            Boolean studentJudge = parseJudgeAnswer(studentAnswer);
            return standardJudge != null && standardJudge.equals(studentJudge);
        }

        if ("SINGLE".equals(question.getType())) {
            return standardAnswer.equals(studentAnswer);
        }

        if ("MULTIPLE".equals(question.getType())) {
            return normalizeMultipleAnswers(standardAnswer).equals(normalizeMultipleAnswers(studentAnswer));
        }

        return false;
    }

    private boolean isObjectiveQuestion(QuestionBank question) {
        return "SINGLE".equals(question.getType())
                || "MULTIPLE".equals(question.getType())
                || "JUDGE".equals(question.getType());
    }

    private boolean isSubjectiveQuestion(QuestionBank question) {
        return "SHORT".equals(question.getType()) || "CODE".equals(question.getType());
    }

    private void upsertMistake(Long studentId, Long questionId) {
        StudentMistakeBook existing = mistakeBookService.getOne(
                new LambdaQueryWrapper<StudentMistakeBook>()
                        .eq(StudentMistakeBook::getStudentId, studentId)
                        .eq(StudentMistakeBook::getQuestionId, questionId)
        );

        if (existing != null) {
            existing.setWrongCount((existing.getWrongCount() == null ? 0 : existing.getWrongCount()) + 1);
            existing.setLastWrongTime(LocalDateTime.now());
            existing.setIsMastered((byte) 0);
            mistakeBookService.updateById(existing);
            return;
        }

        StudentMistakeBook mistake = new StudentMistakeBook();
        mistake.setStudentId(studentId);
        mistake.setQuestionId(questionId);
        mistake.setWrongCount(1);
        mistake.setLastWrongTime(LocalDateTime.now());
        mistake.setIsMastered((byte) 0);
        mistakeBookService.save(mistake);
    }

    private String getRedisKey(Long examId, Long studentId) {
        return REDIS_KEY_PREFIX + examId + ":" + studentId;
    }

    private void assertCanTakeExam(AssessmentTask exam, Long studentId) {
        if (!"EXAM".equals(exam.getType())) {
            throw new BusinessException(400, "任务不是考试");
        }
        if (!examSettingHelper.isPublished(exam)) {
            throw new BusinessException(403, "考试尚未发布");
        }
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(403, "无权参加该考试");
        }
        Set<Long> classIds = getStudentClassIds(student);
        if (classIds.isEmpty()) {
            throw new BusinessException(403, "无权参加该考试");
        }
        List<Long> targetClassIds = parseTargetClassIds(exam.getTargetClassIds());
        if (!targetClassIds.isEmpty() && targetClassIds.stream().noneMatch(classIds::contains)) {
            throw new BusinessException(403, "无权参加该考试");
        }
    }

    private Set<Long> getStudentClassIds(SysUser student) {
        Set<Long> classIds = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .eq(ClassStudentRel::getStudentId, student.getId())
                        .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .stream()
                .map(ClassStudentRel::getClassId)
                .collect(java.util.stream.Collectors.toSet());
        if (student.getClassId() != null) {
            classIds.add(student.getClassId());
        }
        return classIds;
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (targetClassIdsJson == null || targetClassIdsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("Failed to parse target class ids", e);
            return List.of();
        }
    }

    private List<ExamQuestionVO> buildExamQuestions(Long examId, Map<Long, String> answerMap) {
        List<TaskQuestionRel> rels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
                        .orderByAsc(TaskQuestionRel::getSortOrder)
        );

        if (rels.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, QuestionBank> questionMap = loadQuestionMap(rels);

        List<ExamQuestionVO> examQuestions = new ArrayList<>();
        for (TaskQuestionRel rel : rels) {
            QuestionBank question = questionMap.get(rel.getQuestionId());
            if (question == null) {
                continue;
            }
            ExamQuestionVO vo = new ExamQuestionVO();
            vo.setQuestionId(question.getId());
            vo.setType(question.getType());
            vo.setDifficulty(question.getDifficulty());
            vo.setContent(question.getContent());
            vo.setOptionsJson(question.getOptionsJson());
            vo.setScore(rel.getScore());
            vo.setStudentAnswer(answerMap.get(question.getId()));
            examQuestions.add(vo);
        }
        return examQuestions;
    }

    private Map<Long, QuestionBank> loadQuestionMap(List<TaskQuestionRel> rels) {
        if (rels == null || rels.isEmpty()) {
            return Map.of();
        }
        List<Long> questionIds = rels.stream()
                .map(TaskQuestionRel::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        List<QuestionBank> questions = questionBankMapper.selectBatchIdsIncludingDeleted(questionIds);
        Map<Long, QuestionBank> questionMap = new HashMap<>();
        for (QuestionBank question : questions) {
            if (question.getId() != null) {
                questionMap.put(question.getId(), question);
            }
        }
        return questionMap;
    }

    private ExamStartVO toStartVO(AssessmentTask exam, StudentSubmission submission, List<ExamQuestionVO> questions) {
        ExamStartVO vo = new ExamStartVO();
        vo.setSubmissionId(submission.getId());
        vo.setQuestions(questions);
        vo.setTotalScore(exam.getTotalScore());
        vo.setDuration(examSettingHelper.getDuration(exam));
        vo.setRemainingSeconds(examSettingHelper.getRemainingSeconds(exam, submission.getStartTime(), LocalDateTime.now()));
        vo.setSwitchScreenLimit(examSettingHelper.getSwitchScreenLimit(exam));
        vo.setDeadline(examSettingHelper.resolveExamDeadline(exam, submission.getStartTime()));
        return vo;
    }

    private void applyProgressMetadata(ExamProgressVO vo, AssessmentTask exam, StudentSubmission submission) {
        vo.setDuration(examSettingHelper.getDuration(exam));
        vo.setRemainingSeconds(examSettingHelper.getRemainingSeconds(exam, submission.getStartTime(), LocalDateTime.now()));
        vo.setSwitchScreenLimit(examSettingHelper.getSwitchScreenLimit(exam));
        vo.setDeadline(examSettingHelper.resolveExamDeadline(exam, submission.getStartTime()));
    }

    private Boolean parseJudgeAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return null;
        }
        String value = answer.trim().toUpperCase();
        Set<String> trueValues = Set.of("正确", "对", "T", "TRUE", "YES", "1");
        Set<String> falseValues = Set.of("错误", "错", "F", "FALSE", "NO", "0", "不对");
        if (trueValues.contains(value)) {
            return true;
        }
        if (falseValues.contains(value)) {
            return false;
        }
        return null;
    }

    private Set<String> normalizeMultipleAnswers(String rawAnswer) {
        if (rawAnswer == null || rawAnswer.isBlank()) {
            return Set.of();
        }
        String normalized = rawAnswer.trim().toUpperCase().replaceAll("\\s+", "");
        String[] tokens;
        if (normalized.matches("[A-Z]+") && normalized.length() > 1) {
            tokens = normalized.split("");
        } else {
            tokens = normalized.split("[,，/|;]+");
        }
        Set<String> result = new TreeSet<>();
        for (String token : tokens) {
            if (!token.isBlank()) {
                result.add(token);
            }
        }
        return result;
    }
}
