package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.TeacherStatisticsVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.AiChatMessage;
import com.example.edubackend.entity.AiKnowledgeDocument;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.StudentAnswerDetail;
import com.example.edubackend.entity.StudentReviewNote;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.SurveyRecord;
import com.example.edubackend.entity.SurveyTask;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TaskQuestionRel;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AiChatMessageMapper;
import com.example.edubackend.mapper.AiKnowledgeDocumentMapper;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.StudentAnswerDetailMapper;
import com.example.edubackend.mapper.StudentReviewNoteMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TaskQuestionRelMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/statistics")
@RequiredArgsConstructor
public class TeacherStatisticsController {

    private static final List<String> SCORE_RANGES = List.of("0-59", "60-69", "70-79", "80-89", "90-100");

    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SysClassMapper sysClassMapper;
    private final SysUserMapper sysUserMapper;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final StudentSubmissionMapper studentSubmissionMapper;
    private final SurveyTaskMapper surveyTaskMapper;
    private final SurveyRecordMapper surveyRecordMapper;
    private final StudentAnswerDetailMapper studentAnswerDetailMapper;
    private final TaskQuestionRelMapper taskQuestionRelMapper;
    private final QuestionBankMapper questionBankMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;
    private final StudentReviewNoteMapper studentReviewNoteMapper;
    private final ObjectMapper objectMapper;
    private final ExamSettingHelper examSettingHelper;

    @GetMapping
    @RequireRole({"TEACHER", "ASSISTANT"})
    public Result<TeacherStatisticsVO> getStatistics(@RequestParam(required = false) Long classId) {
        Long teacherId = UserContext.getUserId();
        String role = UserContext.getUser().getRole();
        List<SysClass> managedClasses = getManagedClasses(teacherId);
        Map<Long, SysClass> classMap = managedClasses.stream().collect(Collectors.toMap(SysClass::getId, Function.identity()));

        if (classId != null && !classMap.containsKey(classId)) {
            throw new BusinessException(403, "无权查看该班级统计");
        }

        Set<Long> scopedClassIds = classId == null
                ? new HashSet<>(classMap.keySet())
                : new HashSet<>(List.of(classId));

        List<SysUser> scopedStudents = getStudents(scopedClassIds);
        Map<Long, SysUser> studentMap = scopedStudents.stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<AssessmentTask> tasks = getScopedTasks(teacherId, scopedClassIds, role);
        List<StudentSubmission> submissions = getScopedSubmissions(tasks, studentMap.keySet());
        Map<Long, List<StudentSubmission>> submissionsByTask = submissions.stream()
                .collect(Collectors.groupingBy(StudentSubmission::getTaskId));
        List<SurveyTask> surveys = getScopedSurveys(teacherId, scopedClassIds, role);

        TeacherStatisticsVO vo = new TeacherStatisticsVO();
        vo.setSelectedClassId(classId);
        vo.setSelectedClassName(classId == null ? "全部班级" : classMap.get(classId).getClassName());
        vo.setClasses(buildClassOptions(managedClasses));
        vo.setRecentTasks(buildTaskProgress(tasks, submissionsByTask, scopedStudents, classMap));
        vo.setClassComparisons(buildClassComparisons(managedClasses, classId, tasks, submissions, classMap));
        vo.setRiskStudents(buildRiskStudents(scopedStudents, tasks, submissions, classMap));
        vo.setScoreDistribution(buildScoreDistribution(submissions));
        vo.setScoreTrends(buildScoreTrends(tasks, submissionsByTask, scopedStudents, classMap));
        vo.setWeakKnowledgePoints(buildWeakKnowledgePoints(tasks, submissions));
        vo.setAiUsageSummary(buildAiUsageSummary(teacherId, scopedStudents));
        vo.setSurveySummary(buildSurveySummary(surveys, scopedStudents));
        vo.setOverview(buildOverview(scopedClassIds, scopedStudents, tasks, submissions, vo.getRecentTasks()));
        return Result.success(vo);
    }

    private List<SysClass> getManagedClasses(Long teacherId) {
        List<Long> classIds = teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>().eq(TeacherClassRel::getTeacherId, teacherId)
        ).stream().map(TeacherClassRel::getClassId).filter(Objects::nonNull).distinct().toList();

        if (classIds.isEmpty()) {
            return List.of();
        }

        return sysClassMapper.selectList(
                new LambdaQueryWrapper<SysClass>()
                        .in(SysClass::getId, classIds)
                        .eq(SysClass::getStatus, (byte) 1)
                        .orderByDesc(SysClass::getCreateTime)
        );
    }

    private List<SysUser> getStudents(Set<Long> classIds) {
        List<Long> safeClassIds = nonNullIds(classIds);
        if (safeClassIds.isEmpty()) {
            return List.of();
        }
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getIsDeleted, (byte) 0)
                        .in(SysUser::getClassId, safeClassIds)
        );
    }

    private List<AssessmentTask> getScopedTasks(Long teacherId, Set<Long> scopedClassIds, String role) {
        if (scopedClassIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<AssessmentTask> query = new LambdaQueryWrapper<AssessmentTask>()
                .orderByDesc(AssessmentTask::getCreateTime);
        if (!"ASSISTANT".equals(role)) {
            query.eq(AssessmentTask::getCreatorId, teacherId);
        }
        return assessmentTaskMapper.selectList(query).stream()
                .filter(task -> intersectsScope(parseTargetClassIds(task.getTargetClassIds()), scopedClassIds))
                .toList();
    }

    private List<StudentSubmission> getScopedSubmissions(List<AssessmentTask> tasks, Set<Long> studentIds) {
        List<Long> taskIds = nonNullIds(tasks.stream().map(AssessmentTask::getId).toList());
        List<Long> safeStudentIds = nonNullIds(studentIds);
        if (taskIds.isEmpty() || safeStudentIds.isEmpty()) {
            return List.of();
        }
        return studentSubmissionMapper.selectList(
                new LambdaQueryWrapper<StudentSubmission>()
                        .in(StudentSubmission::getTaskId, taskIds)
                        .in(StudentSubmission::getStudentId, safeStudentIds)
        );
    }

    private List<SurveyTask> getScopedSurveys(Long teacherId, Set<Long> scopedClassIds, String role) {
        if (scopedClassIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SurveyTask> query = new LambdaQueryWrapper<SurveyTask>()
                .orderByDesc(SurveyTask::getCreateTime);
        if (!"ASSISTANT".equals(role)) {
            query.eq(SurveyTask::getCreatorId, teacherId);
        }
        return surveyTaskMapper.selectList(query).stream()
                .filter(survey -> intersectsScope(parseTargetClassIds(survey.getTargetClassIds()), scopedClassIds))
                .toList();
    }

    private List<TeacherStatisticsVO.ClassOption> buildClassOptions(List<SysClass> classes) {
        if (classes.isEmpty()) {
            return List.of();
        }
        List<Long> classIds = nonNullIds(classes.stream().map(SysClass::getId).toList());
        if (classIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> countByClass = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getIsDeleted, (byte) 0)
                        .in(SysUser::getClassId, classIds)
        ).stream().collect(Collectors.groupingBy(SysUser::getClassId, Collectors.counting()));

        return classes.stream().map(clazz -> {
            TeacherStatisticsVO.ClassOption option = new TeacherStatisticsVO.ClassOption();
            option.setClassId(clazz.getId());
            option.setClassName(clazz.getClassName());
            option.setStudentCount(countByClass.getOrDefault(clazz.getId(), 0L).intValue());
            return option;
        }).toList();
    }

    private List<TeacherStatisticsVO.TaskProgress> buildTaskProgress(
            List<AssessmentTask> tasks,
            Map<Long, List<StudentSubmission>> submissionsByTask,
            List<SysUser> scopedStudents,
            Map<Long, SysClass> classMap) {
        return tasks.stream()
                .sorted(Comparator.comparing(AssessmentTask::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .map(task -> buildTaskProgressItem(task, submissionsByTask.getOrDefault(task.getId(), List.of()), scopedStudents, classMap))
                .toList();
    }

    private TeacherStatisticsVO.TaskProgress buildTaskProgressItem(
            AssessmentTask task,
            List<StudentSubmission> submissions,
            List<SysUser> scopedStudents,
            Map<Long, SysClass> classMap) {
        List<Long> targetClassIds = parseTargetClassIds(task.getTargetClassIds());
        Set<Long> relevantClassIds = targetClassIds.isEmpty()
                ? scopedStudents.stream().map(SysUser::getClassId).filter(Objects::nonNull).collect(Collectors.toSet())
                : targetClassIds.stream().filter(classMap::containsKey).collect(Collectors.toSet());
        List<SysUser> relevantStudents = scopedStudents.stream()
                .filter(student -> relevantClassIds.contains(student.getClassId()))
                .toList();
        Map<Long, StudentSubmission> latestSubmissionByStudent = latestSubmissionByStudent(submissions);
        List<StudentSubmission> latestSubmissions = new ArrayList<>(latestSubmissionByStudent.values());

        int totalStudents = relevantStudents.size();
        int submittedCount = latestSubmissionByStudent.size();
        int gradedCount = (int) latestSubmissions.stream().filter(this::isGraded).count();
        int pendingGradeCount = (int) latestSubmissions.stream().filter(this::isPendingGrade).count();

        TeacherStatisticsVO.TaskProgress item = new TeacherStatisticsVO.TaskProgress();
        item.setTaskId(task.getId());
        item.setTitle(task.getTitle());
        item.setType(task.getType());
        item.setDeadline(task.getEndTime());
        item.setTotalStudents(totalStudents);
        item.setSubmittedCount(submittedCount);
        item.setGradedCount(gradedCount);
        item.setPendingCount(Math.max(totalStudents - submittedCount, 0));
        item.setPendingGradeCount(pendingGradeCount);
        item.setCompletionRate(rate(submittedCount, totalStudents));
        item.setAverageScore(averageScore(latestSubmissions));
        item.setTargetClassNames(relevantClassIds.stream()
                .map(classMap::get)
                .filter(Objects::nonNull)
                .map(SysClass::getClassName)
                .toList());
        return item;
    }

    private List<TeacherStatisticsVO.ClassComparison> buildClassComparisons(
            List<SysClass> managedClasses,
            Long selectedClassId,
            List<AssessmentTask> tasks,
            List<StudentSubmission> submissions,
            Map<Long, SysClass> classMap) {
        List<SysClass> classes = selectedClassId == null
                ? managedClasses
                : managedClasses.stream().filter(clazz -> selectedClassId.equals(clazz.getId())).toList();
        Map<Long, List<SysUser>> studentsByClass = getStudents(classes.stream().map(SysClass::getId).collect(Collectors.toSet()))
                .stream().collect(Collectors.groupingBy(SysUser::getClassId));
        Map<Long, List<StudentSubmission>> submissionsByTask = submissions.stream().collect(Collectors.groupingBy(StudentSubmission::getTaskId));

        return classes.stream().map(clazz -> {
            List<SysUser> students = studentsByClass.getOrDefault(clazz.getId(), List.of());
            Set<Long> studentIds = students.stream().map(SysUser::getId).collect(Collectors.toSet());
            List<AssessmentTask> classTasks = tasks.stream()
                    .filter(task -> intersectsScope(parseTargetClassIds(task.getTargetClassIds()), Set.of(clazz.getId())))
                    .toList();
            List<StudentSubmission> classSubmissions = classTasks.stream()
                    .flatMap(task -> submissionsByTask.getOrDefault(task.getId(), List.of()).stream())
                    .filter(submission -> studentIds.contains(submission.getStudentId()))
                    .toList();
            int expectedSubmissions = students.size() * classTasks.size();

            TeacherStatisticsVO.ClassComparison item = new TeacherStatisticsVO.ClassComparison();
            item.setClassId(clazz.getId());
            item.setClassName(classMap.getOrDefault(clazz.getId(), clazz).getClassName());
            item.setStudentCount(students.size());
            item.setTaskCount(classTasks.size());
            item.setSubmissionRate(rate(latestSubmissionCount(classSubmissions), expectedSubmissions));
            item.setAverageScore(averageScore(new ArrayList<>(latestSubmissionByStudentAndTask(classSubmissions).values())));
            item.setRiskCount((int) buildRiskStudents(students, classTasks, classSubmissions, classMap).stream()
                    .filter(risk -> risk.getRiskScore() > 0)
                    .count());
            return item;
        }).toList();
    }

    private List<TeacherStatisticsVO.RiskStudent> buildRiskStudents(
            List<SysUser> students,
            List<AssessmentTask> tasks,
            List<StudentSubmission> submissions,
            Map<Long, SysClass> classMap) {
        Map<Long, Map<Long, StudentSubmission>> submissionByTaskAndStudent = new HashMap<>();
        for (StudentSubmission submission : submissions) {
            submissionByTaskAndStudent
                    .computeIfAbsent(submission.getTaskId(), ignored -> new HashMap<>())
                    .merge(submission.getStudentId(), submission, this::latestSubmission);
        }

        List<TeacherStatisticsVO.RiskStudent> risks = new ArrayList<>();
        for (SysUser student : students) {
            List<AssessmentTask> studentTasks = tasks.stream()
                    .filter(task -> intersectsScope(parseTargetClassIds(task.getTargetClassIds()), Set.of(student.getClassId())))
                    .toList();
            List<StudentSubmission> studentSubmissions = new ArrayList<>();
            int missingCount = 0;
            int lowScoreCount = 0;
            int switchScreenCount = 0;

            for (AssessmentTask task : studentTasks) {
                StudentSubmission submission = submissionByTaskAndStudent
                        .getOrDefault(task.getId(), Map.of())
                        .get(student.getId());
                if (submission == null) {
                    if (task.getEndTime() != null && task.getEndTime().isBefore(LocalDateTime.now())) {
                        missingCount++;
                    }
                    continue;
                }
                studentSubmissions.add(submission);
                if ("EXAM".equals(task.getType()) && submission.getTotalScoreGained() != null
                        && submission.getTotalScoreGained().compareTo(BigDecimal.valueOf(examSettingHelper.getPassScore(task))) < 0) {
                    lowScoreCount++;
                }
                switchScreenCount += submission.getSwitchScreenCount() == null ? 0 : submission.getSwitchScreenCount();
            }

            int riskScore = missingCount * 3 + lowScoreCount * 2 + switchScreenCount;
            if (riskScore <= 0) {
                continue;
            }

            TeacherStatisticsVO.RiskStudent item = new TeacherStatisticsVO.RiskStudent();
            item.setStudentId(student.getId());
            item.setStudentName(student.getRealName());
            item.setClassName(classMap.get(student.getClassId()) == null ? "-" : classMap.get(student.getClassId()).getClassName());
            item.setMissingCount(missingCount);
            item.setLowScoreCount(lowScoreCount);
            item.setSwitchScreenCount(switchScreenCount);
            item.setAverageScore(averageScore(studentSubmissions));
            item.setRiskScore(riskScore);
            risks.add(item);
        }

        return risks.stream()
                .sorted(Comparator.comparing(TeacherStatisticsVO.RiskStudent::getRiskScore).reversed())
                .limit(10)
                .toList();
    }

    private List<TeacherStatisticsVO.ScoreDistribution> buildScoreDistribution(List<StudentSubmission> submissions) {
        List<StudentSubmission> scored = submissions.stream()
                .filter(submission -> submission.getTotalScoreGained() != null)
                .toList();
        Map<String, Integer> counts = new LinkedHashMap<>();
        SCORE_RANGES.forEach(range -> counts.put(range, 0));

        for (StudentSubmission submission : scored) {
            int score = submission.getTotalScoreGained().setScale(0, RoundingMode.DOWN).intValue();
            String range = score < 60 ? "0-59"
                    : score < 70 ? "60-69"
                    : score < 80 ? "70-79"
                    : score < 90 ? "80-89"
                    : "90-100";
            counts.put(range, counts.get(range) + 1);
        }

        return counts.entrySet().stream().map(entry -> {
            TeacherStatisticsVO.ScoreDistribution item = new TeacherStatisticsVO.ScoreDistribution();
            item.setRange(entry.getKey());
            item.setCount(entry.getValue());
            item.setRate(rate(entry.getValue(), scored.size()));
            return item;
        }).toList();
    }

    private List<TeacherStatisticsVO.ScoreTrend> buildScoreTrends(
            List<AssessmentTask> tasks,
            Map<Long, List<StudentSubmission>> submissionsByTask,
            List<SysUser> scopedStudents,
            Map<Long, SysClass> classMap) {
        return tasks.stream()
                .filter(task -> "EXAM".equals(task.getType()))
                .sorted(Comparator.comparing(AssessmentTask::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(6)
                .map(task -> {
                    TeacherStatisticsVO.TaskProgress progress = buildTaskProgressItem(
                            task,
                            submissionsByTask.getOrDefault(task.getId(), List.of()),
                            scopedStudents,
                            classMap
                    );
                    TeacherStatisticsVO.ScoreTrend trend = new TeacherStatisticsVO.ScoreTrend();
                    trend.setTaskId(task.getId());
                    trend.setTitle(task.getTitle());
                    trend.setDeadline(task.getEndTime());
                    trend.setAverageScore(progress.getAverageScore());
                    trend.setCompletionRate(progress.getCompletionRate());
                    trend.setSubmittedCount(progress.getSubmittedCount());
                    trend.setTotalStudents(progress.getTotalStudents());
                    return trend;
                })
                .toList();
    }

    private List<TeacherStatisticsVO.WeakKnowledgePoint> buildWeakKnowledgePoints(
            List<AssessmentTask> tasks,
            List<StudentSubmission> submissions) {
        List<StudentSubmission> latestSubmissions = new ArrayList<>(latestSubmissionByStudentAndTask(submissions).values());
        if (tasks.isEmpty() || latestSubmissions.isEmpty()) {
            return List.of();
        }

        List<Long> submissionIds = latestSubmissions.stream().map(StudentSubmission::getId).filter(Objects::nonNull).toList();
        if (submissionIds.isEmpty()) {
            return List.of();
        }

        List<StudentAnswerDetail> details = studentAnswerDetailMapper.selectList(
                new LambdaQueryWrapper<StudentAnswerDetail>().in(StudentAnswerDetail::getSubmissionId, submissionIds)
        );
        if (details.isEmpty()) {
            return List.of();
        }

        Map<Long, StudentSubmission> submissionMap = latestSubmissions.stream()
                .collect(Collectors.toMap(StudentSubmission::getId, Function.identity()));
        List<Long> questionIds = details.stream().map(StudentAnswerDetail::getQuestionId).filter(Objects::nonNull).distinct().toList();
        Map<Long, QuestionBank> questionMap = questionIds.isEmpty()
                ? Map.of()
                : questionBankMapper.selectList(new LambdaQueryWrapper<QuestionBank>().in(QuestionBank::getId, questionIds))
                        .stream().collect(Collectors.toMap(QuestionBank::getId, Function.identity()));
        Map<String, Integer> scoreMap = buildQuestionScoreMap(tasks);
        Map<String, WeakKnowledgeAccumulator> accumulators = new HashMap<>();

        for (StudentAnswerDetail detail : details) {
            QuestionBank question = questionMap.get(detail.getQuestionId());
            StudentSubmission submission = submissionMap.get(detail.getSubmissionId());
            if (question == null || submission == null || question.getKnowledgePoint() == null || question.getKnowledgePoint().isBlank()) {
                continue;
            }

            String key = (question.getCourseCategory() == null ? "" : question.getCourseCategory()) + "::" + question.getKnowledgePoint();
            WeakKnowledgeAccumulator accumulator = accumulators.computeIfAbsent(key, ignored ->
                    new WeakKnowledgeAccumulator(question.getCourseCategory(), question.getKnowledgePoint()));
            accumulator.attempts++;
            if (detail.getIsCorrect() == null || detail.getIsCorrect() != 1) {
                accumulator.wrongCount++;
            }

            Integer fullScore = scoreMap.get(submission.getTaskId() + ":" + detail.getQuestionId());
            if (fullScore != null && fullScore > 0 && detail.getScoreGained() != null) {
                accumulator.scoreRateTotal = accumulator.scoreRateTotal.add(
                        detail.getScoreGained()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(fullScore), 1, RoundingMode.HALF_UP)
                );
                accumulator.scoredAttempts++;
            }
        }

        return accumulators.values().stream()
                .map(WeakKnowledgeAccumulator::toVO)
                .sorted(Comparator
                        .comparing(TeacherStatisticsVO.WeakKnowledgePoint::getWrongRate).reversed()
                        .thenComparing(TeacherStatisticsVO.WeakKnowledgePoint::getAttempts, Comparator.reverseOrder()))
                .limit(8)
                .toList();
    }

    private Map<String, Integer> buildQuestionScoreMap(List<AssessmentTask> tasks) {
        List<Long> taskIds = nonNullIds(tasks.stream().map(AssessmentTask::getId).toList());
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<TaskQuestionRel> relations = taskQuestionRelMapper.selectList(
                new LambdaQueryWrapper<TaskQuestionRel>().in(TaskQuestionRel::getTaskId, taskIds)
        );
        Map<String, Integer> scoreMap = new HashMap<>();
        for (TaskQuestionRel relation : relations) {
            scoreMap.put(relation.getTaskId() + ":" + relation.getQuestionId(), relation.getScore());
        }
        return scoreMap;
    }

    private TeacherStatisticsVO.AiUsageSummary buildAiUsageSummary(Long teacherId, List<SysUser> scopedStudents) {
        TeacherStatisticsVO.AiUsageSummary summary = new TeacherStatisticsVO.AiUsageSummary();
        List<Long> studentIds = nonNullIds(scopedStudents.stream().map(SysUser::getId).toList());

        List<AiChatMessage> studentMessages = studentIds.isEmpty() ? List.of() : aiChatMessageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .in(AiChatMessage::getUserId, studentIds)
                        .eq(AiChatMessage::getMessageRole, "USER")
        );
        List<AiChatMessage> teacherMessages = aiChatMessageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getUserId, teacherId)
                        .eq(AiChatMessage::getMessageRole, "USER")
        );
        List<AiKnowledgeDocument> documents = aiKnowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocument>().eq(AiKnowledgeDocument::getOwnerId, teacherId)
        );
        List<StudentReviewNote> notes = studentIds.isEmpty() ? List.of() : studentReviewNoteMapper.selectList(
                new LambdaQueryWrapper<StudentReviewNote>().in(StudentReviewNote::getStudentId, studentIds)
        );

        summary.setStudentQuestionCount(studentMessages.size());
        summary.setTeacherQuestionCount(teacherMessages.size());
        summary.setActiveStudentCount((int) studentMessages.stream().map(AiChatMessage::getUserId).distinct().count());
        summary.setKnowledgeDocumentCount(documents.size());
        summary.setKnowledgeChunkCount(documents.stream().map(AiKnowledgeDocument::getChunkCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum());
        summary.setReviewNoteCount(notes.size());
        summary.setTopTopics(buildAiTopics(studentMessages));
        return summary;
    }

    private List<TeacherStatisticsVO.AiTopic> buildAiTopics(List<AiChatMessage> messages) {
        Map<String, Long> counts = messages.stream()
                .map(message -> {
                    if (message.getKnowledgePoint() != null && !message.getKnowledgePoint().isBlank()) {
                        return message.getKnowledgePoint();
                    }
                    if (message.getCourseCategory() != null && !message.getCourseCategory().isBlank()) {
                        return message.getCourseCategory();
                    }
                    return "未分类";
                })
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    TeacherStatisticsVO.AiTopic topic = new TeacherStatisticsVO.AiTopic();
                    topic.setLabel(entry.getKey());
                    topic.setCount(entry.getValue().intValue());
                    return topic;
                })
                .toList();
    }

    private TeacherStatisticsVO.SurveySummary buildSurveySummary(List<SurveyTask> surveys, List<SysUser> scopedStudents) {
        TeacherStatisticsVO.SurveySummary summary = new TeacherStatisticsVO.SurveySummary();
        summary.setSurveyCount(surveys.size());
        summary.setPublishedCount((int) surveys.stream().filter(survey -> survey.getStatus() != null && survey.getStatus() == 1).count());

        if (surveys.isEmpty()) {
            return summary;
        }

        List<Long> surveyIds = nonNullIds(surveys.stream().map(SurveyTask::getId).toList());
        List<Long> studentIds = nonNullIds(scopedStudents.stream().map(SysUser::getId).toList());
        List<SurveyRecord> records = surveyIds.isEmpty() || studentIds.isEmpty() ? List.of() : surveyRecordMapper.selectList(
                new LambdaQueryWrapper<SurveyRecord>()
                        .in(SurveyRecord::getSurveyId, surveyIds)
                        .in(SurveyRecord::getStudentId, studentIds)
        );
        Map<Long, Long> recordsBySurvey = records.stream().collect(Collectors.groupingBy(SurveyRecord::getSurveyId, Collectors.counting()));
        summary.setTotalResponses(records.size());
        summary.setRecentSurveys(surveys.stream().limit(5).map(survey -> {
            TeacherStatisticsVO.SurveyItem item = new TeacherStatisticsVO.SurveyItem();
            item.setSurveyId(survey.getId());
            item.setTitle(survey.getTitle());
            item.setDeadline(survey.getEndTime());
            item.setStatus(survey.getStatus() == null ? 0 : survey.getStatus().intValue());
            item.setTotalResponses(recordsBySurvey.getOrDefault(survey.getId(), 0L).intValue());
            return item;
        }).toList());
        return summary;
    }

    private TeacherStatisticsVO.Overview buildOverview(
            Set<Long> scopedClassIds,
            List<SysUser> students,
            List<AssessmentTask> tasks,
            List<StudentSubmission> submissions,
            List<TeacherStatisticsVO.TaskProgress> taskProgress) {
        List<StudentSubmission> latestSubmissions = new ArrayList<>(latestSubmissionByStudentAndTask(submissions).values());
        int passedCount = 0;
        int examScoreCount = 0;
        Map<Long, AssessmentTask> taskMap = tasks.stream().collect(Collectors.toMap(AssessmentTask::getId, Function.identity()));
        for (StudentSubmission submission : latestSubmissions) {
            AssessmentTask task = taskMap.get(submission.getTaskId());
            if (task == null || !"EXAM".equals(task.getType()) || submission.getTotalScoreGained() == null) {
                continue;
            }
            examScoreCount++;
            if (submission.getTotalScoreGained().compareTo(BigDecimal.valueOf(examSettingHelper.getPassScore(task))) >= 0) {
                passedCount++;
            }
        }

        TeacherStatisticsVO.Overview overview = new TeacherStatisticsVO.Overview();
        overview.setClassCount(scopedClassIds.size());
        overview.setStudentCount(students.size());
        overview.setExamCount((int) tasks.stream().filter(task -> "EXAM".equals(task.getType())).count());
        overview.setHomeworkCount((int) tasks.stream().filter(task -> "HOMEWORK".equals(task.getType())).count());
        overview.setActiveTaskCount((int) tasks.stream().filter(task -> task.getEndTime() != null && task.getEndTime().isAfter(LocalDateTime.now())).count());
        overview.setPendingGradeCount(taskProgress.stream().mapToInt(TeacherStatisticsVO.TaskProgress::getPendingGradeCount).sum());
        overview.setAverageScore(averageScore(latestSubmissions));
        overview.setPassRate(rate(passedCount, examScoreCount));
        return overview;
    }

    private Map<Long, StudentSubmission> latestSubmissionByStudent(List<StudentSubmission> submissions) {
        Map<Long, StudentSubmission> latest = new HashMap<>();
        for (StudentSubmission submission : submissions) {
            latest.merge(submission.getStudentId(), submission, this::latestSubmission);
        }
        return latest;
    }

    private Map<String, StudentSubmission> latestSubmissionByStudentAndTask(List<StudentSubmission> submissions) {
        Map<String, StudentSubmission> latest = new HashMap<>();
        for (StudentSubmission submission : submissions) {
            latest.merge(submission.getTaskId() + ":" + submission.getStudentId(), submission, this::latestSubmission);
        }
        return latest;
    }

    private StudentSubmission latestSubmission(StudentSubmission left, StudentSubmission right) {
        LocalDateTime leftTime = left.getSubmitTime() == null ? LocalDateTime.MIN : left.getSubmitTime();
        LocalDateTime rightTime = right.getSubmitTime() == null ? LocalDateTime.MIN : right.getSubmitTime();
        return rightTime.isAfter(leftTime) ? right : left;
    }

    private int latestSubmissionCount(List<StudentSubmission> submissions) {
        return latestSubmissionByStudentAndTask(submissions).size();
    }

    private boolean isGraded(StudentSubmission submission) {
        return "GRADED".equals(submission.getStatus()) || submission.getTotalScoreGained() != null;
    }

    private boolean isPendingGrade(StudentSubmission submission) {
        return submission != null && submission.getTotalScoreGained() == null && "SUBMITTED".equals(submission.getStatus());
    }

    private BigDecimal averageScore(List<StudentSubmission> submissions) {
        List<BigDecimal> scores = submissions.stream()
                .map(StudentSubmission::getTotalScoreGained)
                .filter(Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private boolean intersectsScope(List<Long> targetClassIds, Set<Long> scopedClassIds) {
        if (targetClassIds.isEmpty()) {
            return true;
        }
        return targetClassIds.stream().anyMatch(scopedClassIds::contains);
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (targetClassIdsJson == null || targetClassIdsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Long> nonNullIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static class WeakKnowledgeAccumulator {
        private final String courseCategory;
        private final String knowledgePoint;
        private int attempts;
        private int wrongCount;
        private int scoredAttempts;
        private BigDecimal scoreRateTotal = BigDecimal.ZERO;

        private WeakKnowledgeAccumulator(String courseCategory, String knowledgePoint) {
            this.courseCategory = courseCategory;
            this.knowledgePoint = knowledgePoint;
        }

        private TeacherStatisticsVO.WeakKnowledgePoint toVO() {
            TeacherStatisticsVO.WeakKnowledgePoint vo = new TeacherStatisticsVO.WeakKnowledgePoint();
            vo.setCourseCategory(courseCategory);
            vo.setKnowledgePoint(knowledgePoint);
            vo.setAttempts(attempts);
            vo.setWrongCount(wrongCount);
            vo.setWrongRate(attempts <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(wrongCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(attempts), 1, RoundingMode.HALF_UP));
            vo.setAverageScoreRate(scoredAttempts <= 0 ? BigDecimal.ZERO : scoreRateTotal
                    .divide(BigDecimal.valueOf(scoredAttempts), 1, RoundingMode.HALF_UP));
            return vo;
        }
    }
}
