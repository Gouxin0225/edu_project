package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.dto.AdminClassStatisticsVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.SurveyAnswerDetail;
import com.example.edubackend.entity.SurveyQuestion;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.SurveyRecord;
import com.example.edubackend.entity.SurveyTask;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.SurveyAnswerDetailMapper;
import com.example.edubackend.mapper.SurveyQuestionMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final SysClassMapper sysClassMapper;
    private final SysUserMapper sysUserMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final StudentSubmissionMapper studentSubmissionMapper;
    private final SurveyTaskMapper surveyTaskMapper;
    private final SurveyRecordMapper surveyRecordMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;
    private final SurveyAnswerDetailMapper surveyAnswerDetailMapper;
    private final ObjectMapper objectMapper;

    @GetMapping("/classes")
    @RequireRole("ADMIN")
    public Result<AdminClassStatisticsVO> getClassStatistics() {
        List<SysClass> classes = sysClassMapper.selectList(
                new LambdaQueryWrapper<SysClass>().orderByDesc(SysClass::getCreateTime)
        );
        List<Long> classIds = classes.stream().map(SysClass::getId).toList();

        Map<Long, List<SysUser>> studentsByClass = loadStudentsByClass(classIds);
        Map<Long, List<SysUser>> teachersByClass = loadAssignedUsersByClass(classIds, "TEACHER");
        Map<Long, List<SysUser>> assistantsByClass = loadAssignedUsersByClass(classIds, "ASSISTANT");
        List<AssessmentTask> tasks = assessmentTaskMapper.selectList(
                new LambdaQueryWrapper<AssessmentTask>().orderByDesc(AssessmentTask::getCreateTime)
        );
        List<SurveyTask> surveys = surveyTaskMapper.selectList(
                new LambdaQueryWrapper<SurveyTask>().orderByDesc(SurveyTask::getCreateTime)
        );
        Map<Long, SysUser> userMap = loadUserMap();
        Map<Long, List<StudentSubmission>> submissionsByTask = loadSubmissionsByTask(tasks);
        Map<Long, List<SurveyRecord>> recordsBySurvey = loadSurveyRecordsBySurvey(surveys);
        Map<Long, List<SurveyQuestion>> questionsBySurvey = loadSurveyQuestionsBySurvey(surveys);
        Map<Long, List<SurveyAnswerDetail>> answersByRecord = loadSurveyAnswersByRecord(recordsBySurvey);

        AdminClassStatisticsVO vo = new AdminClassStatisticsVO();
        for (SysClass clazz : classes) {
            List<SysUser> students = studentsByClass.getOrDefault(clazz.getId(), List.of());
            List<AssessmentTask> classTasks = tasks.stream()
                    .filter(task -> appliesToClass(task.getTargetClassIds(), clazz.getId()))
                    .toList();
            List<SurveyTask> classSurveys = surveys.stream()
                    .filter(survey -> appliesToClass(survey.getTargetClassIds(), clazz.getId()))
                    .toList();

            AdminClassStatisticsVO.ClassModule module = new AdminClassStatisticsVO.ClassModule();
            module.setClassId(clazz.getId());
            module.setClassName(clazz.getClassName());
            module.setStatus(clazz.getStatus());
            module.setStatusName(clazz.getStatus() != null && clazz.getStatus() == 1 ? "进行中" : "已结课");
            module.setStudentCount(students.size());
            module.setTeachers(toPeople(teachersByClass.getOrDefault(clazz.getId(), List.of())));
            module.setAssistants(toPeople(assistantsByClass.getOrDefault(clazz.getId(), List.of())));
            module.setHomeworks(buildWorkItems(classTasks, "HOMEWORK", students, submissionsByTask));
            module.setHomeworkSummary(buildWorkSummary(module.getHomeworks()));
            module.setExams(buildWorkItems(classTasks, "EXAM", students, submissionsByTask));
            module.setExamSummary(buildWorkSummary(module.getExams()));
            module.setSurveys(buildSurveyItems(classSurveys, students, recordsBySurvey, questionsBySurvey, answersByRecord, userMap));
            module.setSurveySummary(buildSurveySummary(module.getSurveys()));
            vo.getClasses().add(module);
        }

        vo.setOverview(buildOverview(vo.getClasses()));
        return Result.success(vo);
    }

    private Map<Long, List<SysUser>> loadStudentsByClass(List<Long> classIds) {
        classIds = nonNullIds(classIds);
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getIsDeleted, (byte) 0)
                        .in(SysUser::getClassId, classIds)
        ).stream().collect(Collectors.groupingBy(SysUser::getClassId));
    }

    private Map<Long, List<SysUser>> loadAssignedUsersByClass(List<Long> classIds, String role) {
        classIds = nonNullIds(classIds);
        if (classIds.isEmpty()) {
            return Map.of();
        }
        List<TeacherClassRel> rels = teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>().in(TeacherClassRel::getClassId, classIds)
        );
        if (rels.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = rels.stream()
                .map(TeacherClassRel::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> userMap = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, userIds)
                        .eq(SysUser::getRole, role)
                        .eq(SysUser::getIsDeleted, (byte) 0)
        ).stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));

        Map<Long, List<SysUser>> result = new HashMap<>();
        for (TeacherClassRel rel : rels) {
            SysUser user = userMap.get(rel.getTeacherId());
            if (user != null) {
                result.computeIfAbsent(rel.getClassId(), ignored -> new ArrayList<>()).add(user);
            }
        }
        return result;
    }

    private Map<Long, SysUser> loadUserMap() {
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getIsDeleted, (byte) 0)
        ).stream().collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, List<StudentSubmission>> loadSubmissionsByTask(List<AssessmentTask> tasks) {
        List<Long> taskIds = nonNullIds(tasks.stream().map(AssessmentTask::getId).toList());
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return studentSubmissionMapper.selectList(
                new LambdaQueryWrapper<StudentSubmission>()
                        .in(StudentSubmission::getTaskId, taskIds)
        ).stream().collect(Collectors.groupingBy(StudentSubmission::getTaskId));
    }

    private Map<Long, List<SurveyRecord>> loadSurveyRecordsBySurvey(List<SurveyTask> surveys) {
        List<Long> surveyIds = nonNullIds(surveys.stream().map(SurveyTask::getId).toList());
        if (surveyIds.isEmpty()) {
            return Map.of();
        }
        return surveyRecordMapper.selectList(
                new LambdaQueryWrapper<SurveyRecord>()
                        .in(SurveyRecord::getSurveyId, surveyIds)
        ).stream().collect(Collectors.groupingBy(SurveyRecord::getSurveyId));
    }

    private Map<Long, List<SurveyQuestion>> loadSurveyQuestionsBySurvey(List<SurveyTask> surveys) {
        List<Long> surveyIds = nonNullIds(surveys.stream().map(SurveyTask::getId).toList());
        if (surveyIds.isEmpty()) {
            return Map.of();
        }
        return surveyQuestionMapper.selectList(
                new LambdaQueryWrapper<SurveyQuestion>()
                        .in(SurveyQuestion::getSurveyId, surveyIds)
        ).stream().collect(Collectors.groupingBy(SurveyQuestion::getSurveyId));
    }

    private Map<Long, List<SurveyAnswerDetail>> loadSurveyAnswersByRecord(Map<Long, List<SurveyRecord>> recordsBySurvey) {
        List<Long> recordIds = nonNullIds(recordsBySurvey.values().stream()
                .flatMap(List::stream)
                .map(SurveyRecord::getId)
                .toList());
        if (recordIds.isEmpty()) {
            return Map.of();
        }
        return surveyAnswerDetailMapper.selectList(
                new LambdaQueryWrapper<SurveyAnswerDetail>().in(SurveyAnswerDetail::getRecordId, recordIds)
        ).stream().collect(Collectors.groupingBy(SurveyAnswerDetail::getRecordId));
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

    private List<AdminClassStatisticsVO.Person> toPeople(List<SysUser> users) {
        return users.stream()
                .sorted(Comparator.comparing(SysUser::getRealName, Comparator.nullsLast(String::compareTo)))
                .map(user -> {
                    AdminClassStatisticsVO.Person person = new AdminClassStatisticsVO.Person();
                    person.setId(user.getId());
                    person.setName(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
                    return person;
                })
                .toList();
    }

    private List<AdminClassStatisticsVO.WorkItem> buildWorkItems(
            List<AssessmentTask> tasks,
            String type,
            List<SysUser> students,
            Map<Long, List<StudentSubmission>> submissionsByTask) {
        Set<Long> studentIds = students.stream().map(SysUser::getId).collect(Collectors.toSet());
        return tasks.stream()
                .filter(task -> type.equals(task.getType()))
                .sorted(Comparator.comparing(AssessmentTask::getEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(task -> buildWorkItem(task, students.size(), studentIds, submissionsByTask.getOrDefault(task.getId(), List.of())))
                .toList();
    }

    private AdminClassStatisticsVO.WorkItem buildWorkItem(
            AssessmentTask task,
            int totalStudents,
            Set<Long> studentIds,
            List<StudentSubmission> submissions) {
        List<StudentSubmission> latestSubmissions = latestSubmissions(submissions, studentIds);
        int submittedCount = latestSubmissions.size();
        int gradedCount = (int) latestSubmissions.stream().filter(this::isGraded).count();
        int returnedCount = (int) latestSubmissions.stream().filter(s -> "RETURNED".equals(s.getStatus())).count();

        AdminClassStatisticsVO.WorkItem item = new AdminClassStatisticsVO.WorkItem();
        item.setTaskId(task.getId());
        item.setTitle(task.getTitle());
        item.setDeadline(task.getEndTime());
        item.setTotalStudents(totalStudents);
        item.setSubmittedCount(submittedCount);
        item.setPendingCount(Math.max(totalStudents - submittedCount, 0));
        item.setGradedCount(gradedCount);
        item.setReturnedCount(returnedCount);
        item.setPendingGradeCount(Math.max(submittedCount - gradedCount - returnedCount, 0));
        item.setCompletionRate(rate(submittedCount, totalStudents));
        item.setScoreSampleCount(scoreCount(latestSubmissions));
        item.setAverageScore(averageScore(latestSubmissions));
        return item;
    }

    private List<StudentSubmission> latestSubmissions(List<StudentSubmission> submissions, Set<Long> studentIds) {
        Map<Long, StudentSubmission> latestByStudent = new HashMap<>();
        for (StudentSubmission submission : submissions) {
            if (!studentIds.contains(submission.getStudentId())) {
                continue;
            }
            StudentSubmission existing = latestByStudent.get(submission.getStudentId());
            if (existing == null || compareSubmission(submission, existing) > 0) {
                latestByStudent.put(submission.getStudentId(), submission);
            }
        }
        return new ArrayList<>(latestByStudent.values());
    }

    private int compareSubmission(StudentSubmission left, StudentSubmission right) {
        int versionCompare = Integer.compare(valueOrZero(left.getVersion()), valueOrZero(right.getVersion()));
        if (versionCompare != 0) {
            return versionCompare;
        }
        return Long.compare(valueOrZero(left.getId()), valueOrZero(right.getId()));
    }

    private List<AdminClassStatisticsVO.SurveyItem> buildSurveyItems(
            List<SurveyTask> surveys,
            List<SysUser> students,
            Map<Long, List<SurveyRecord>> recordsBySurvey,
            Map<Long, List<SurveyQuestion>> questionsBySurvey,
            Map<Long, List<SurveyAnswerDetail>> answersByRecord,
            Map<Long, SysUser> userMap) {
        Set<Long> studentIds = students.stream().map(SysUser::getId).collect(Collectors.toSet());
        return surveys.stream()
                .sorted(Comparator.comparing(SurveyTask::getEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(survey -> buildSurveyItem(
                        survey,
                        students.size(),
                        studentIds,
                        recordsBySurvey.getOrDefault(survey.getId(), List.of()),
                        questionsBySurvey.getOrDefault(survey.getId(), List.of()),
                        answersByRecord,
                        userMap
                ))
                .toList();
    }

    private AdminClassStatisticsVO.SurveyItem buildSurveyItem(
            SurveyTask survey,
            int totalStudents,
            Set<Long> studentIds,
            List<SurveyRecord> records,
            List<SurveyQuestion> questions,
            Map<Long, List<SurveyAnswerDetail>> answersByRecord,
            Map<Long, SysUser> userMap) {
        List<SurveyRecord> classRecords = records.stream()
                .filter(record -> record.getStudentId() != null && studentIds.contains(record.getStudentId()))
                .toList();
        Set<Long> respondedStudentIds = classRecords.stream()
                .map(SurveyRecord::getStudentId)
                .collect(Collectors.toSet());
        SysUser targetTeacher = survey.getTargetTeacherId() == null ? null : userMap.get(survey.getTargetTeacherId());

        AdminClassStatisticsVO.SurveyItem item = new AdminClassStatisticsVO.SurveyItem();
        item.setSurveyId(survey.getId());
        item.setTitle(survey.getTitle());
        item.setDeadline(survey.getEndTime());
        item.setStatus(survey.getStatus());
        item.setStatusName(survey.getStatus() != null && survey.getStatus() == 1 ? "已发布" : "草稿");
        item.setTargetTeacherId(survey.getTargetTeacherId());
        item.setTargetTeacherName(targetTeacher == null ? null : targetTeacher.getRealName());
        item.setTotalStudents(totalStudents);
        item.setResponseCount(respondedStudentIds.size());
        item.setPendingCount(Math.max(totalStudents - respondedStudentIds.size(), 0));
        item.setCompletionRate(rate(respondedStudentIds.size(), totalStudents));
        SurveyScoreStats surveyScoreStats = surveyScoreStats(classRecords, questions, answersByRecord);
        item.setScoreSampleCount(surveyScoreStats.count());
        item.setAverageScore(surveyScoreStats.average());
        return item;
    }

    private AdminClassStatisticsVO.WorkSummary buildWorkSummary(List<AdminClassStatisticsVO.WorkItem> items) {
        AdminClassStatisticsVO.WorkSummary summary = new AdminClassStatisticsVO.WorkSummary();
        summary.setTaskCount(items.size());
        summary.setSubmittedCount(items.stream().mapToInt(AdminClassStatisticsVO.WorkItem::getSubmittedCount).sum());
        summary.setTotalRequired(items.stream().mapToInt(AdminClassStatisticsVO.WorkItem::getTotalStudents).sum());
        summary.setPendingCount(items.stream().mapToInt(AdminClassStatisticsVO.WorkItem::getPendingCount).sum());
        summary.setGradedCount(items.stream().mapToInt(AdminClassStatisticsVO.WorkItem::getGradedCount).sum());
        summary.setPendingGradeCount(items.stream().mapToInt(AdminClassStatisticsVO.WorkItem::getPendingGradeCount).sum());
        summary.setScoreSampleCount(items.stream().mapToInt(AdminClassStatisticsVO.WorkItem::getScoreSampleCount).sum());
        summary.setCompletionRate(rate(summary.getSubmittedCount(), summary.getTotalRequired()));
        summary.setAverageScore(weightedAverageWorkScore(items));
        return summary;
    }

    private AdminClassStatisticsVO.SurveySummary buildSurveySummary(List<AdminClassStatisticsVO.SurveyItem> items) {
        AdminClassStatisticsVO.SurveySummary summary = new AdminClassStatisticsVO.SurveySummary();
        summary.setSurveyCount(items.size());
        summary.setResponseCount(items.stream().mapToInt(AdminClassStatisticsVO.SurveyItem::getResponseCount).sum());
        summary.setTotalRequired(items.stream().mapToInt(AdminClassStatisticsVO.SurveyItem::getTotalStudents).sum());
        summary.setScoreSampleCount(items.stream().mapToInt(AdminClassStatisticsVO.SurveyItem::getScoreSampleCount).sum());
        summary.setCompletionRate(rate(summary.getResponseCount(), summary.getTotalRequired()));
        summary.setAverageScore(weightedAverageSurveyScore(items));
        return summary;
    }

    private AdminClassStatisticsVO.Overview buildOverview(List<AdminClassStatisticsVO.ClassModule> classes) {
        AdminClassStatisticsVO.Overview overview = new AdminClassStatisticsVO.Overview();
        overview.setClassCount(classes.size());
        overview.setActiveClassCount((int) classes.stream().filter(c -> c.getStatus() != null && c.getStatus() == 1).count());
        overview.setFinishedClassCount(Math.max(classes.size() - overview.getActiveClassCount(), 0));
        overview.setStudentCount(classes.stream().mapToInt(AdminClassStatisticsVO.ClassModule::getStudentCount).sum());
        overview.setHomeworkCount(classes.stream().mapToInt(c -> c.getHomeworkSummary().getTaskCount()).sum());
        overview.setExamCount(classes.stream().mapToInt(c -> c.getExamSummary().getTaskCount()).sum());
        overview.setSurveyCount(classes.stream().mapToInt(c -> c.getSurveySummary().getSurveyCount()).sum());
        overview.setHomeworkAverageScore(weightedAverageSummaryScore(classes.stream().map(c -> c.getHomeworkSummary()).toList()));
        overview.setExamAverageScore(weightedAverageSummaryScore(classes.stream().map(c -> c.getExamSummary()).toList()));
        overview.setSurveyAverageScore(weightedAverageSurveySummaryScore(classes.stream().map(c -> c.getSurveySummary()).toList()));
        overview.setHomeworkScoreSampleCount(classes.stream().mapToInt(c -> c.getHomeworkSummary().getScoreSampleCount()).sum());
        overview.setExamScoreSampleCount(classes.stream().mapToInt(c -> c.getExamSummary().getScoreSampleCount()).sum());
        overview.setSurveyScoreSampleCount(classes.stream().mapToInt(c -> c.getSurveySummary().getScoreSampleCount()).sum());
        return overview;
    }

    private boolean appliesToClass(String targetClassIdsJson, Long classId) {
        List<Long> targetClassIds = parseTargetClassIds(targetClassIdsJson);
        return targetClassIds.isEmpty() || targetClassIds.contains(classId);
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (!StringUtils.hasText(targetClassIdsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isGraded(StudentSubmission submission) {
        return "GRADED".equals(submission.getStatus()) || submission.getTotalScoreGained() != null;
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

    private int scoreCount(List<StudentSubmission> submissions) {
        return (int) submissions.stream()
                .map(StudentSubmission::getTotalScoreGained)
                .filter(Objects::nonNull)
                .count();
    }

    private SurveyScoreStats surveyScoreStats(
            List<SurveyRecord> records,
            List<SurveyQuestion> questions,
            Map<Long, List<SurveyAnswerDetail>> answersByRecord) {
        if (records.isEmpty() || questions.isEmpty()) {
            return new SurveyScoreStats(BigDecimal.ZERO, 0);
        }
        Set<Long> numericQuestionIds = questions.stream()
                .filter(question -> isNumericSurveyQuestion(question.getType()))
                .map(SurveyQuestion::getId)
                .collect(Collectors.toSet());
        if (numericQuestionIds.isEmpty()) {
            return new SurveyScoreStats(BigDecimal.ZERO, 0);
        }
        List<BigDecimal> scores = records.stream()
                .flatMap(record -> answersByRecord.getOrDefault(record.getId(), List.of()).stream())
                .filter(answer -> numericQuestionIds.contains(answer.getSurveyQuestionId()))
                .map(answer -> parseDecimal(answer.getAnswerValue()))
                .filter(Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return new SurveyScoreStats(BigDecimal.ZERO, 0);
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SurveyScoreStats(total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP), scores.size());
    }

    private boolean isNumericSurveyQuestion(String type) {
        return "STAR".equals(type) || "SCALE".equals(type) || "NPS".equals(type);
    }

    private BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal weightedAverageWorkScore(List<AdminClassStatisticsVO.WorkItem> items) {
        int totalWeight = items.stream().mapToInt(AdminClassStatisticsVO.WorkItem::getScoreSampleCount).sum();
        if (totalWeight <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = items.stream()
                .map(item -> item.getAverageScore().multiply(BigDecimal.valueOf(item.getScoreSampleCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(totalWeight), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedAverageSurveyScore(List<AdminClassStatisticsVO.SurveyItem> items) {
        int totalWeight = items.stream().mapToInt(AdminClassStatisticsVO.SurveyItem::getScoreSampleCount).sum();
        if (totalWeight <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = items.stream()
                .map(item -> item.getAverageScore().multiply(BigDecimal.valueOf(item.getScoreSampleCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedAverageSummaryScore(List<AdminClassStatisticsVO.WorkSummary> summaries) {
        int totalWeight = summaries.stream().mapToInt(AdminClassStatisticsVO.WorkSummary::getScoreSampleCount).sum();
        if (totalWeight <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = summaries.stream()
                .map(summary -> summary.getAverageScore().multiply(BigDecimal.valueOf(summary.getScoreSampleCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(totalWeight), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedAverageSurveySummaryScore(List<AdminClassStatisticsVO.SurveySummary> summaries) {
        int totalWeight = summaries.stream().mapToInt(AdminClassStatisticsVO.SurveySummary::getScoreSampleCount).sum();
        if (totalWeight <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = summaries.stream()
                .map(summary -> summary.getAverageScore().multiply(BigDecimal.valueOf(summary.getScoreSampleCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
    }

    private record SurveyScoreStats(BigDecimal average, int count) {}

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
