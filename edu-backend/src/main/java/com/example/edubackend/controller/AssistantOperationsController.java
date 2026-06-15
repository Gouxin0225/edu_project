package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.AssistantOperationsVO;
import com.example.edubackend.entity.AiChatMessage;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.ClassStudentRel;
import com.example.edubackend.entity.StudentMistakeBook;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.StudentVisitRecord;
import com.example.edubackend.entity.SurveyRecord;
import com.example.edubackend.entity.SurveyTask;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AiChatMessageMapper;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.StudentMistakeBookMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.StudentVisitRecordMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
@RequestMapping("/api/assistant/operations")
@RequireRole({"ASSISTANT"})
@RequiredArgsConstructor
public class AssistantOperationsController {

    private static final int LOW_SCORE_LINE = 60;

    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SysClassMapper sysClassMapper;
    private final SysUserMapper sysUserMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final StudentSubmissionMapper studentSubmissionMapper;
    private final SurveyTaskMapper surveyTaskMapper;
    private final SurveyRecordMapper surveyRecordMapper;
    private final StudentVisitRecordMapper studentVisitRecordMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final StudentMistakeBookMapper studentMistakeBookMapper;
    private final ObjectMapper objectMapper;

    @GetMapping("/classes")
    public Result<List<AssistantOperationsVO.ClassBoard>> getClassBoards() {
        DataScope scope = loadScope(null);
        List<AssistantOperationsVO.RiskItem> risks = buildRisks(scope);
        Map<Long, Long> riskCountByClass = risks.stream()
                .collect(Collectors.groupingBy(AssistantOperationsVO.RiskItem::getClassId, Collectors.counting()));

        List<AssistantOperationsVO.ClassBoard> boards = new ArrayList<>();
        for (SysClass clazz : scope.classes()) {
            List<SysUser> students = scope.students().stream()
                    .filter(student -> Objects.equals(student.getClassId(), clazz.getId()))
                    .toList();
            AssistantOperationsVO.ClassBoard board = new AssistantOperationsVO.ClassBoard();
            board.setClassId(clazz.getId());
            board.setClassName(clazz.getClassName());
            board.setSchoolName(clazz.getSchoolName());
            board.setStudentCount(students.size());
            board.setActiveCount((int) students.stream().filter(student -> isActive(student.getId(), scope)).count());
            board.setRiskCount(riskCountByClass.getOrDefault(clazz.getId(), 0L).intValue());
            board.setHomeworkCompletionRate(completionRate(clazz.getId(), "HOMEWORK", scope));
            board.setExamCompletionRate(completionRate(clazz.getId(), "EXAM", scope));
            board.setSurveyCompletionRate(surveyCompletionRate(clazz.getId(), scope));
            board.setStudents(students.stream()
                    .map(student -> buildStudentBrief(student, scope, risks))
                    .sorted(Comparator.comparing(AssistantOperationsVO.StudentBrief::getRiskScore).reversed())
                    .toList());
            boards.add(board);
        }
        return Result.success(boards);
    }

    @GetMapping("/students/{studentId}/profile")
    public Result<AssistantOperationsVO.StudentProfile> getStudentProfile(@PathVariable Long studentId) {
        DataScope scope = loadScope(null);
        SysUser student = scope.studentMap().get(studentId);
        if (student == null) {
            throw new BusinessException(403, "无权查看该学员");
        }

        AssistantOperationsVO.StudentProfile profile = new AssistantOperationsVO.StudentProfile();
        profile.setRisks(buildRisks(scope).stream()
                .filter(item -> Objects.equals(item.getStudentId(), studentId))
                .toList());
        profile.setVisits(scope.visitsByStudent().getOrDefault(studentId, List.of()).stream()
                .sorted(Comparator.comparing(StudentVisitRecord::getVisitTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toVisitItem)
                .toList());
        profile.setExams(buildTaskRecords(student, "EXAM", scope));
        profile.setHomework(buildTaskRecords(student, "HOMEWORK", scope));
        profile.setSurveys(buildSurveyRecords(student, scope));
        profile.setClassHistory(buildClassHistory(studentId, scope));
        profile.setWeakPoints(buildWeakPoints(studentId, scope));

        AssistantOperationsVO.BasicInfo basic = profile.getBasicInfo();
        basic.setStudentId(student.getId());
        basic.setStudentName(student.getRealName());
        basic.setUsername(student.getUsername());
        basic.setPhone(student.getPhone());
        basic.setSchoolName(student.getSchoolName());
        basic.setCurrentClassId(student.getClassId());
        basic.setCurrentClassName(className(student.getClassId(), scope.classMap()));
        basic.setCreateTime(student.getCreateTime());
        basic.setJoinTime(profile.getClassHistory().stream()
                .filter(item -> Objects.equals(item.getClassId(), student.getClassId()))
                .map(AssistantOperationsVO.ClassHistoryItem::getJoinTime)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(student.getCreateTime()));

        AssistantOperationsVO.LearningState learning = profile.getLearningState();
        learning.setExamCount(profile.getExams().size());
        learning.setHomeworkCount(profile.getHomework().size());
        learning.setSubmittedExamCount((int) profile.getExams().stream().filter(item -> item.getSubmitTime() != null).count());
        learning.setSubmittedHomeworkCount((int) profile.getHomework().stream().filter(item -> item.getSubmitTime() != null).count());
        learning.setMissingTaskCount((int) profile.getRisks().stream().filter(item -> "MISSING_TASK".equals(item.getType())).count());
        learning.setLowScoreCount((int) profile.getRisks().stream().filter(item -> "LOW_SCORE".equals(item.getType())).count());
        learning.setSwitchScreenCount(profile.getExams().stream().mapToInt(item -> item.getSwitchScreenCount() == null ? 0 : item.getSwitchScreenCount()).sum());
        learning.setSurveySubmittedCount((int) profile.getSurveys().stream().filter(AssistantOperationsVO.SurveyRecordItem::getSubmitted).count());
        learning.setSurveyMissingCount((int) profile.getSurveys().stream().filter(item -> !item.getSubmitted()).count());
        learning.setAiQuestionCount(scope.aiByStudent().getOrDefault(studentId, List.of()).size());
        learning.setMistakeCount(scope.mistakesByStudent().getOrDefault(studentId, List.of()).stream()
                .mapToInt(item -> item.getWrongCount() == null ? 1 : item.getWrongCount()).sum());
        learning.setAverageScore(average(profile.getExams().stream().map(AssistantOperationsVO.TaskRecord::getScore).filter(Objects::nonNull).toList()));
        learning.setLastActivityTime(lastActivity(studentId, scope));
        return Result.success(profile);
    }

    @GetMapping("/risks")
    public Result<List<AssistantOperationsVO.RiskItem>> getRisks(@RequestParam(required = false) Long classId) {
        return Result.success(buildRisks(loadScope(classId)));
    }

    @GetMapping("/todos")
    public Result<List<AssistantOperationsVO.TodoItem>> getTodos(@RequestParam(required = false) Long classId) {
        DataScope scope = loadScope(classId);
        List<AssistantOperationsVO.TodoItem> todos = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (AssistantOperationsVO.RiskItem risk : buildRisks(scope)) {
            if ("HIGH".equals(risk.getLevel())) {
                todos.add(todo("RISK-" + risk.getId(), "HIGH_RISK", "高风险学员待处理", risk, risk.getSourceTime(), risk.getReason(), "HIGH"));
            }
        }
        for (AssessmentTask task : scope.tasks()) {
            Set<Long> targetClassIds = scopedTaskClassIds(task.getTargetClassIds(), scope.classIds());
            List<SysUser> targetStudents = studentsInClasses(scope.students(), targetClassIds);
            Map<Long, StudentSubmission> latestByStudent = latestByStudent(scope.submissionsByTask().getOrDefault(task.getId(), List.of()));
            for (SysUser student : targetStudents) {
                StudentSubmission submission = latestByStudent.get(student.getId());
                if (submission == null || ("EXAM".equals(task.getType()) && "UN".equals(submission.getStatus()))) {
                    String type = "EXAM".equals(task.getType()) ? "EXAM_MISSING" : "HOMEWORK_MISSING";
                    String title = "EXAM".equals(task.getType()) ? "考试未参加名单" : "今日待催交";
                    todos.add(todo(type + "-" + task.getId() + "-" + student.getId(), type, title, student, task, scope, "EXAM".equals(task.getType()) ? "HIGH" : "MEDIUM"));
                }
            }
        }
        for (SurveyTask survey : scope.surveys()) {
            Set<Long> targetClassIds = scopedTaskClassIds(survey.getTargetClassIds(), scope.classIds());
            Set<Long> submittedIds = scope.surveyRecordsBySurvey().getOrDefault(survey.getId(), List.of()).stream()
                    .map(SurveyRecord::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (SysUser student : studentsInClasses(scope.students(), targetClassIds)) {
                if (!submittedIds.contains(student.getId())) {
                    AssistantOperationsVO.TodoItem item = new AssistantOperationsVO.TodoItem();
                    item.setId("SURVEY-" + survey.getId() + "-" + student.getId());
                    item.setType("SURVEY_MISSING");
                    item.setTitle("问卷未填写名单");
                    fillStudent(item, student, scope);
                    item.setSourceType("SURVEY");
                    item.setSourceId(survey.getId());
                    item.setDeadline(survey.getEndTime());
                    item.setReason("未填写问卷：" + survey.getTitle());
                    item.setPriority("MEDIUM");
                    todos.add(item);
                }
            }
        }
        for (StudentVisitRecord visit : scope.visits()) {
            if (visit.getNextFollowTime() == null || "RESOLVED".equals(visit.getVisitResult())) {
                continue;
            }
            boolean dueToday = visit.getNextFollowTime().toLocalDate().equals(today);
            boolean overdue = visit.getNextFollowTime().isBefore(LocalDateTime.now()) && !dueToday;
            if (dueToday || overdue) {
                SysUser student = scope.studentMap().get(visit.getStudentId());
                if (student == null) {
                    continue;
                }
                AssistantOperationsVO.TodoItem item = new AssistantOperationsVO.TodoItem();
                item.setId("VISIT-" + visit.getId());
                item.setType(overdue ? "VISIT_OVERDUE" : "VISIT_TODAY");
                item.setTitle(overdue ? "回访逾期提醒" : "今日待回访");
                fillStudent(item, student, scope);
                item.setSourceType("VISIT");
                item.setSourceId(visit.getId());
                item.setDeadline(visit.getNextFollowTime());
                item.setReason(overdue ? "回访计划已逾期" : "今日需要跟进回访");
                item.setPriority(overdue ? "HIGH" : "MEDIUM");
                todos.add(item);
            }
        }
        todos.sort(Comparator
                .comparing((AssistantOperationsVO.TodoItem item) -> priorityWeight(item.getPriority())).reversed()
                .thenComparing(AssistantOperationsVO.TodoItem::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())));
        return Result.success(todos);
    }

    private DataScope loadScope(Long selectedClassId) {
        List<SysClass> classes = loadManagedClasses();
        Map<Long, SysClass> classMap = classes.stream().collect(Collectors.toMap(SysClass::getId, Function.identity()));
        if (selectedClassId != null && !classMap.containsKey(selectedClassId)) {
            throw new BusinessException(403, "无权查看该班级");
        }
        Set<Long> classIds = selectedClassId == null
                ? new HashSet<>(classMap.keySet())
                : new HashSet<>(List.of(selectedClassId));
        List<SysUser> students = loadStudents(classIds);
        Map<Long, SysUser> studentMap = students.stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<AssessmentTask> tasks = loadTasks(classIds);
        List<StudentSubmission> submissions = loadSubmissions(tasks, studentMap.keySet());
        List<SurveyTask> surveys = loadSurveys(classIds);
        List<SurveyRecord> surveyRecords = loadSurveyRecords(surveys, studentMap.keySet());
        List<StudentVisitRecord> visits = loadVisits(classIds, studentMap.keySet());
        List<AiChatMessage> aiMessages = loadAiMessages(studentMap.keySet());
        List<StudentMistakeBook> mistakes = loadMistakes(studentMap.keySet());
        return new DataScope(
                classes.stream().filter(item -> classIds.contains(item.getId())).toList(),
                classMap, classIds, students, studentMap, tasks, submissions, surveys, surveyRecords, visits,
                aiMessages, mistakes,
                submissions.stream().collect(Collectors.groupingBy(StudentSubmission::getTaskId)),
                submissions.stream().collect(Collectors.groupingBy(StudentSubmission::getStudentId)),
                surveyRecords.stream().collect(Collectors.groupingBy(SurveyRecord::getSurveyId)),
                surveyRecords.stream().collect(Collectors.groupingBy(SurveyRecord::getStudentId)),
                visits.stream().collect(Collectors.groupingBy(StudentVisitRecord::getStudentId)),
                aiMessages.stream().collect(Collectors.groupingBy(AiChatMessage::getUserId)),
                mistakes.stream().collect(Collectors.groupingBy(StudentMistakeBook::getStudentId))
        );
    }

    private List<AssistantOperationsVO.RiskItem> buildRisks(DataScope scope) {
        List<AssistantOperationsVO.RiskItem> risks = new ArrayList<>();
        for (AssessmentTask task : scope.tasks()) {
            Set<Long> targetClassIds = scopedTaskClassIds(task.getTargetClassIds(), scope.classIds());
            List<SysUser> targetStudents = studentsInClasses(scope.students(), targetClassIds);
            Map<Long, StudentSubmission> latestByStudent = latestByStudent(scope.submissionsByTask().getOrDefault(task.getId(), List.of()));
            for (SysUser student : targetStudents) {
                StudentSubmission submission = latestByStudent.get(student.getId());
                if (submission == null || ("EXAM".equals(task.getType()) && "UN".equals(submission.getStatus()))) {
                    risks.add(risk("MISS-" + task.getId() + "-" + student.getId(), "MISSING_TASK", "任务缺交", "MEDIUM",
                            student, task, scope, "未完成：" + task.getTitle()));
                    continue;
                }
                if ("EXAM".equals(task.getType()) && isLowScore(submission, task)) {
                    risks.add(risk("LOW-" + submission.getId(), "LOW_SCORE", "考试低分", "HIGH",
                            student, task, scope, "得分 " + submission.getTotalScoreGained() + "，低于及格线"));
                }
                if ("EXAM".equals(task.getType()) && submission.getSwitchScreenCount() != null && submission.getSwitchScreenCount() > 0) {
                    risks.add(risk("SWITCH-" + submission.getId(), "SWITCH_SCREEN", "考试切屏异常", "HIGH",
                            student, task, scope, "考试中切屏 " + submission.getSwitchScreenCount() + " 次"));
                }
            }
        }
        for (SurveyTask survey : scope.surveys()) {
            Set<Long> targetClassIds = scopedTaskClassIds(survey.getTargetClassIds(), scope.classIds());
            Set<Long> submittedIds = scope.surveyRecordsBySurvey().getOrDefault(survey.getId(), List.of()).stream()
                    .map(SurveyRecord::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (SysUser student : studentsInClasses(scope.students(), targetClassIds)) {
                if (!submittedIds.contains(student.getId())) {
                    risks.add(risk("SURVEY-" + survey.getId() + "-" + student.getId(), "SURVEY_MISSING", "问卷未填", "MEDIUM",
                            student, survey, scope, "未填写问卷：" + survey.getTitle()));
                }
            }
        }
        for (SysUser student : scope.students()) {
            LocalDateTime last = lastActivity(student.getId(), scope);
            if (last == null || last.isBefore(LocalDateTime.now().minusDays(14))) {
                AssistantOperationsVO.RiskItem item = new AssistantOperationsVO.RiskItem();
                item.setId("INACTIVE-" + student.getId());
                item.setType("INACTIVE");
                item.setTitle("长时间无学习活动");
                item.setLevel("HIGH");
                fillStudent(item, student, scope);
                item.setSourceType("ACTIVITY");
                item.setSourceId(student.getId());
                item.setSourceTime(last);
                item.setReason(last == null ? "暂无学习活动记录" : "最近学习活动：" + last);
                risks.add(item);
            }
        }
        for (StudentVisitRecord visit : scope.visits()) {
            if (visit.getNextFollowTime() != null
                    && !"RESOLVED".equals(visit.getVisitResult())
                    && visit.getNextFollowTime().isBefore(LocalDateTime.now())) {
                SysUser student = scope.studentMap().get(visit.getStudentId());
                if (student == null) {
                    continue;
                }
                AssistantOperationsVO.RiskItem item = new AssistantOperationsVO.RiskItem();
                item.setId("VISIT-" + visit.getId());
                item.setType("VISIT_OVERDUE");
                item.setTitle("回访逾期");
                item.setLevel("HIGH");
                fillStudent(item, student, scope);
                item.setSourceType("VISIT");
                item.setSourceId(visit.getId());
                item.setSourceTime(visit.getNextFollowTime());
                item.setReason("下次跟进时间已逾期");
                risks.add(item);
            }
        }
        return risks.stream()
                .sorted(Comparator
                        .comparing((AssistantOperationsVO.RiskItem item) -> priorityWeight(item.getLevel())).reversed()
                        .thenComparing(AssistantOperationsVO.RiskItem::getSourceTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private AssistantOperationsVO.StudentBrief buildStudentBrief(SysUser student, DataScope scope, List<AssistantOperationsVO.RiskItem> risks) {
        AssistantOperationsVO.StudentBrief brief = new AssistantOperationsVO.StudentBrief();
        brief.setStudentId(student.getId());
        brief.setStudentName(student.getRealName());
        brief.setUsername(student.getUsername());
        brief.setPhone(student.getPhone());
        brief.setSchoolName(student.getSchoolName());
        brief.setClassId(student.getClassId());
        brief.setClassName(className(student.getClassId(), scope.classMap()));
        brief.setLastActivityTime(lastActivity(student.getId(), scope));
        brief.setRiskScore((int) risks.stream().filter(item -> Objects.equals(item.getStudentId(), student.getId())).count());
        scope.visitsByStudent().getOrDefault(student.getId(), List.of()).stream()
                .max(Comparator.comparing(StudentVisitRecord::getVisitTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .ifPresent(visit -> {
                    brief.setLastVisitTime(visit.getVisitTime());
                    brief.setNextFollowTime(visit.getNextFollowTime());
                });
        brief.setJoinTime(loadJoinTime(student.getId(), student.getClassId()));
        return brief;
    }

    private List<AssistantOperationsVO.TaskRecord> buildTaskRecords(SysUser student, String type, DataScope scope) {
        return scope.tasks().stream()
                .filter(task -> type.equals(task.getType()))
                .filter(task -> scopedTaskClassIds(task.getTargetClassIds(), scope.classIds()).contains(student.getClassId()))
                .map(task -> {
                    StudentSubmission submission = latestByStudent(scope.submissionsByTask().getOrDefault(task.getId(), List.of())).get(student.getId());
                    AssistantOperationsVO.TaskRecord record = new AssistantOperationsVO.TaskRecord();
                    record.setTaskId(task.getId());
                    record.setTitle(task.getTitle());
                    record.setType(task.getType());
                    record.setDeadline(task.getEndTime());
                    record.setTotalScore(task.getTotalScore());
                    if (submission == null) {
                        record.setStatus("MISSING");
                    } else {
                        record.setStatus(submission.getStatus());
                        record.setScore(submission.getTotalScoreGained());
                        record.setSwitchScreenCount(submission.getSwitchScreenCount());
                        record.setStartTime(submission.getStartTime());
                        record.setSubmitTime(submission.getSubmitTime());
                        record.setAssistantComment(submission.getAssistantComment());
                    }
                    return record;
                })
                .sorted(Comparator.comparing(AssistantOperationsVO.TaskRecord::getDeadline, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<AssistantOperationsVO.SurveyRecordItem> buildSurveyRecords(SysUser student, DataScope scope) {
        Map<Long, SurveyRecord> recordMap = scope.surveyRecordsByStudent().getOrDefault(student.getId(), List.of()).stream()
                .collect(Collectors.toMap(SurveyRecord::getSurveyId, Function.identity(), (left, right) -> {
                    LocalDateTime leftTime = left.getSubmitTime();
                    LocalDateTime rightTime = right.getSubmitTime();
                    if (leftTime == null) return right;
                    if (rightTime == null) return left;
                    return rightTime.isAfter(leftTime) ? right : left;
                }));
        return scope.surveys().stream()
                .filter(survey -> scopedTaskClassIds(survey.getTargetClassIds(), scope.classIds()).contains(student.getClassId()))
                .map(survey -> {
                    SurveyRecord record = recordMap.get(survey.getId());
                    AssistantOperationsVO.SurveyRecordItem item = new AssistantOperationsVO.SurveyRecordItem();
                    item.setSurveyId(survey.getId());
                    item.setTitle(survey.getTitle());
                    item.setDeadline(survey.getEndTime());
                    item.setSubmitted(record != null);
                    item.setSubmitTime(record == null ? null : record.getSubmitTime());
                    return item;
                })
                .sorted(Comparator.comparing(AssistantOperationsVO.SurveyRecordItem::getDeadline, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<AssistantOperationsVO.ClassHistoryItem> buildClassHistory(Long studentId, DataScope scope) {
        return classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .eq(ClassStudentRel::getStudentId, studentId)
                        .orderByDesc(ClassStudentRel::getJoinTime))
                .stream()
                .filter(rel -> scope.classMap().containsKey(rel.getClassId()))
                .map(rel -> {
                    SysClass clazz = scope.classMap().get(rel.getClassId());
                    AssistantOperationsVO.ClassHistoryItem item = new AssistantOperationsVO.ClassHistoryItem();
                    item.setClassId(rel.getClassId());
                    item.setClassName(clazz.getClassName());
                    item.setStatus(clazz.getStatus() == null ? "-" : String.valueOf(clazz.getStatus()));
                    item.setJoinStatus(rel.getStatus());
                    item.setJoinSource(rel.getJoinSource());
                    item.setJoinTime(rel.getJoinTime());
                    item.setLeaveTime(rel.getLeaveTime());
                    return item;
                })
                .toList();
    }

    private List<String> buildWeakPoints(Long studentId, DataScope scope) {
        return scope.mistakesByStudent().getOrDefault(studentId, List.of()).stream()
                .sorted(Comparator.comparing(StudentMistakeBook::getWrongCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(item -> "错题 " + item.getQuestionId() + "（累计 " + (item.getWrongCount() == null ? 1 : item.getWrongCount()) + " 次）")
                .toList();
    }

    private AssistantOperationsVO.VisitItem toVisitItem(StudentVisitRecord visit) {
        AssistantOperationsVO.VisitItem item = new AssistantOperationsVO.VisitItem();
        item.setId(visit.getId());
        item.setVisitMethod(visit.getVisitMethod());
        item.setVisitResult(visit.getVisitResult());
        item.setContent(visit.getContent());
        item.setProblemCategory(visit.getProblemCategory());
        item.setConclusion(visit.getConclusion());
        item.setAttachmentUrl(visit.getAttachmentUrl());
        item.setResolved(visit.getIsResolved() != null && visit.getIsResolved() == 1);
        item.setVisitTime(visit.getVisitTime());
        item.setNextFollowTime(visit.getNextFollowTime());
        return item;
    }

    private AssistantOperationsVO.RiskItem risk(String id, String type, String title, String level, SysUser student,
                                               AssessmentTask task, DataScope scope, String reason) {
        AssistantOperationsVO.RiskItem item = new AssistantOperationsVO.RiskItem();
        item.setId(id);
        item.setType(type);
        item.setTitle(title);
        item.setLevel(level);
        fillStudent(item, student, scope);
        item.setSourceType(task.getType());
        item.setSourceId(task.getId());
        item.setSourceTitle(task.getTitle());
        item.setSourceTime(task.getEndTime());
        item.setReason(reason);
        return item;
    }

    private AssistantOperationsVO.RiskItem risk(String id, String type, String title, String level, SysUser student,
                                               SurveyTask survey, DataScope scope, String reason) {
        AssistantOperationsVO.RiskItem item = new AssistantOperationsVO.RiskItem();
        item.setId(id);
        item.setType(type);
        item.setTitle(title);
        item.setLevel(level);
        fillStudent(item, student, scope);
        item.setSourceType("SURVEY");
        item.setSourceId(survey.getId());
        item.setSourceTitle(survey.getTitle());
        item.setSourceTime(survey.getEndTime());
        item.setReason(reason);
        return item;
    }

    private AssistantOperationsVO.TodoItem todo(String id, String type, String title, AssistantOperationsVO.RiskItem risk,
                                               LocalDateTime deadline, String reason, String priority) {
        AssistantOperationsVO.TodoItem item = new AssistantOperationsVO.TodoItem();
        item.setId(id);
        item.setType(type);
        item.setTitle(title);
        item.setStudentId(risk.getStudentId());
        item.setStudentName(risk.getStudentName());
        item.setClassId(risk.getClassId());
        item.setClassName(risk.getClassName());
        item.setSourceType(risk.getSourceType());
        item.setSourceId(risk.getSourceId());
        item.setDeadline(deadline);
        item.setReason(reason);
        item.setPriority(priority);
        return item;
    }

    private AssistantOperationsVO.TodoItem todo(String id, String type, String title, SysUser student,
                                               AssessmentTask task, DataScope scope, String priority) {
        AssistantOperationsVO.TodoItem item = new AssistantOperationsVO.TodoItem();
        item.setId(id);
        item.setType(type);
        item.setTitle(title);
        fillStudent(item, student, scope);
        item.setSourceType(task.getType());
        item.setSourceId(task.getId());
        item.setDeadline(task.getEndTime());
        item.setReason("未完成：" + task.getTitle());
        item.setPriority(priority);
        return item;
    }

    private void fillStudent(AssistantOperationsVO.RiskItem item, SysUser student, DataScope scope) {
        item.setStudentId(student.getId());
        item.setStudentName(student.getRealName());
        item.setClassId(student.getClassId());
        item.setClassName(className(student.getClassId(), scope.classMap()));
    }

    private void fillStudent(AssistantOperationsVO.TodoItem item, SysUser student, DataScope scope) {
        item.setStudentId(student.getId());
        item.setStudentName(student.getRealName());
        item.setClassId(student.getClassId());
        item.setClassName(className(student.getClassId(), scope.classMap()));
    }

    private List<SysClass> loadManagedClasses() {
        List<Long> classIds = teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>().eq(TeacherClassRel::getTeacherId, UserContext.getUserId())
        ).stream().map(TeacherClassRel::getClassId).filter(Objects::nonNull).distinct().toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        return sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>()
                .eq(SysClass::getIsDeleted, (byte) 0)
                .in(SysClass::getId, classIds)
                .orderByDesc(SysClass::getCreateTime));
    }

    private List<SysUser> loadStudents(Set<Long> classIds) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getIsDeleted, (byte) 0)
                .in(SysUser::getClassId, classIds));
    }

    private List<AssessmentTask> loadTasks(Set<Long> classIds) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        return assessmentTaskMapper.selectList(new LambdaQueryWrapper<AssessmentTask>()
                        .eq(AssessmentTask::getIsDeleted, (byte) 0)
                        .orderByDesc(AssessmentTask::getCreateTime))
                .stream()
                .filter(task -> intersects(parseTargetClassIds(task.getTargetClassIds()), classIds))
                .toList();
    }

    private List<StudentSubmission> loadSubmissions(List<AssessmentTask> tasks, Set<Long> studentIds) {
        List<Long> taskIds = tasks.stream().map(AssessmentTask::getId).filter(Objects::nonNull).toList();
        if (taskIds.isEmpty() || studentIds.isEmpty()) {
            return List.of();
        }
        return studentSubmissionMapper.selectList(new LambdaQueryWrapper<StudentSubmission>()
                .in(StudentSubmission::getTaskId, taskIds)
                .in(StudentSubmission::getStudentId, studentIds));
    }

    private List<SurveyTask> loadSurveys(Set<Long> classIds) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        return surveyTaskMapper.selectList(new LambdaQueryWrapper<SurveyTask>()
                        .eq(SurveyTask::getIsDeleted, (byte) 0)
                        .eq(SurveyTask::getStatus, (byte) 1)
                        .orderByDesc(SurveyTask::getCreateTime))
                .stream()
                .filter(survey -> intersects(parseTargetClassIds(survey.getTargetClassIds()), classIds))
                .toList();
    }

    private List<SurveyRecord> loadSurveyRecords(List<SurveyTask> surveys, Set<Long> studentIds) {
        List<Long> surveyIds = surveys.stream().map(SurveyTask::getId).filter(Objects::nonNull).toList();
        if (surveyIds.isEmpty() || studentIds.isEmpty()) {
            return List.of();
        }
        return surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                .in(SurveyRecord::getSurveyId, surveyIds)
                .in(SurveyRecord::getStudentId, studentIds));
    }

    private List<StudentVisitRecord> loadVisits(Set<Long> classIds, Set<Long> studentIds) {
        if (classIds.isEmpty() || studentIds.isEmpty()) {
            return List.of();
        }
        return studentVisitRecordMapper.selectList(new LambdaQueryWrapper<StudentVisitRecord>()
                .eq(StudentVisitRecord::getIsDeleted, (byte) 0)
                .in(StudentVisitRecord::getClassId, classIds)
                .in(StudentVisitRecord::getStudentId, studentIds));
    }

    private List<AiChatMessage> loadAiMessages(Set<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return aiChatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getIsDeleted, (byte) 0)
                .eq(AiChatMessage::getUserRole, "STUDENT")
                .eq(AiChatMessage::getMessageRole, "USER")
                .in(AiChatMessage::getUserId, studentIds));
    }

    private List<StudentMistakeBook> loadMistakes(Set<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return studentMistakeBookMapper.selectList(new LambdaQueryWrapper<StudentMistakeBook>()
                .in(StudentMistakeBook::getStudentId, studentIds));
    }

    private BigDecimal completionRate(Long classId, String type, DataScope scope) {
        List<AssessmentTask> tasks = scope.tasks().stream()
                .filter(task -> type.equals(task.getType()))
                .filter(task -> scopedTaskClassIds(task.getTargetClassIds(), scope.classIds()).contains(classId))
                .toList();
        List<SysUser> students = studentsInClasses(scope.students(), Set.of(classId));
        int total = tasks.size() * students.size();
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        int done = 0;
        for (AssessmentTask task : tasks) {
            Set<Long> submittedIds = latestByStudent(scope.submissionsByTask().getOrDefault(task.getId(), List.of())).entrySet().stream()
                    .filter(entry -> !"EXAM".equals(type) || !"UN".equals(entry.getValue().getStatus()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            done += (int) students.stream().filter(student -> submittedIds.contains(student.getId())).count();
        }
        return rate(done, total);
    }

    private BigDecimal surveyCompletionRate(Long classId, DataScope scope) {
        List<SurveyTask> surveys = scope.surveys().stream()
                .filter(survey -> scopedTaskClassIds(survey.getTargetClassIds(), scope.classIds()).contains(classId))
                .toList();
        List<SysUser> students = studentsInClasses(scope.students(), Set.of(classId));
        int total = surveys.size() * students.size();
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        int done = 0;
        for (SurveyTask survey : surveys) {
            Set<Long> submittedIds = scope.surveyRecordsBySurvey().getOrDefault(survey.getId(), List.of()).stream()
                    .map(SurveyRecord::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
            done += (int) students.stream().filter(student -> submittedIds.contains(student.getId())).count();
        }
        return rate(done, total);
    }

    private boolean isActive(Long studentId, DataScope scope) {
        LocalDateTime last = lastActivity(studentId, scope);
        return last != null && last.isAfter(LocalDateTime.now().minusDays(7));
    }

    private LocalDateTime lastActivity(Long studentId, DataScope scope) {
        List<LocalDateTime> times = new ArrayList<>();
        scope.submissionsByStudent().getOrDefault(studentId, List.of()).forEach(item -> {
            if (item.getSubmitTime() != null) times.add(item.getSubmitTime());
            if (item.getStartTime() != null) times.add(item.getStartTime());
        });
        scope.surveyRecordsByStudent().getOrDefault(studentId, List.of()).stream()
                .map(SurveyRecord::getSubmitTime).filter(Objects::nonNull).forEach(times::add);
        scope.aiByStudent().getOrDefault(studentId, List.of()).stream()
                .map(AiChatMessage::getCreateTime).filter(Objects::nonNull).forEach(times::add);
        return times.stream().max(LocalDateTime::compareTo).orElse(null);
    }

    private boolean isLowScore(StudentSubmission submission, AssessmentTask task) {
        if (submission.getTotalScoreGained() == null) {
            return false;
        }
        BigDecimal line = task.getTotalScore() == null || task.getTotalScore() <= 0
                ? BigDecimal.valueOf(LOW_SCORE_LINE)
                : BigDecimal.valueOf(task.getTotalScore()).multiply(BigDecimal.valueOf(0.6));
        return submission.getTotalScoreGained().compareTo(line) < 0;
    }

    private LocalDateTime loadJoinTime(Long studentId, Long classId) {
        ClassStudentRel rel = classStudentRelMapper.selectOne(new LambdaQueryWrapper<ClassStudentRel>()
                .eq(ClassStudentRel::getStudentId, studentId)
                .eq(ClassStudentRel::getClassId, classId)
                .orderByDesc(ClassStudentRel::getJoinTime)
                .last("LIMIT 1"));
        return rel == null ? null : rel.getJoinTime();
    }

    private Map<Long, StudentSubmission> latestByStudent(Collection<StudentSubmission> submissions) {
        Map<Long, StudentSubmission> result = new LinkedHashMap<>();
        for (StudentSubmission submission : submissions) {
            result.merge(submission.getStudentId(), submission, this::latest);
        }
        return result;
    }

    private StudentSubmission latest(StudentSubmission left, StudentSubmission right) {
        LocalDateTime leftTime = left.getSubmitTime() == null ? left.getStartTime() : left.getSubmitTime();
        LocalDateTime rightTime = right.getSubmitTime() == null ? right.getStartTime() : right.getSubmitTime();
        if (leftTime == null) return right;
        if (rightTime == null) return left;
        return rightTime.isAfter(leftTime) ? right : left;
    }

    private BigDecimal rate(int value, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    private List<SysUser> studentsInClasses(List<SysUser> students, Set<Long> classIds) {
        return students.stream().filter(student -> classIds.contains(student.getClassId())).toList();
    }

    private Set<Long> scopedTaskClassIds(String targetClassIdsJson, Set<Long> managedClassIds) {
        List<Long> targetClassIds = parseTargetClassIds(targetClassIdsJson);
        if (targetClassIds.isEmpty()) {
            return managedClassIds;
        }
        return targetClassIds.stream().filter(managedClassIds::contains).collect(Collectors.toSet());
    }

    private boolean intersects(List<Long> targetClassIds, Set<Long> classIds) {
        if (classIds.isEmpty()) {
            return false;
        }
        if (targetClassIds.isEmpty()) {
            return true;
        }
        return targetClassIds.stream().anyMatch(classIds::contains);
    }

    private List<Long> parseTargetClassIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {})
                    .stream().filter(Objects::nonNull).distinct().toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String className(Long classId, Map<Long, SysClass> classMap) {
        SysClass clazz = classMap.get(classId);
        return clazz == null ? "-" : clazz.getClassName();
    }

    private int priorityWeight(String priority) {
        if ("HIGH".equals(priority)) return 3;
        if ("MEDIUM".equals(priority)) return 2;
        return 1;
    }

    private record DataScope(
            List<SysClass> classes,
            Map<Long, SysClass> classMap,
            Set<Long> classIds,
            List<SysUser> students,
            Map<Long, SysUser> studentMap,
            List<AssessmentTask> tasks,
            List<StudentSubmission> submissions,
            List<SurveyTask> surveys,
            List<SurveyRecord> surveyRecords,
            List<StudentVisitRecord> visits,
            List<AiChatMessage> aiMessages,
            List<StudentMistakeBook> mistakes,
            Map<Long, List<StudentSubmission>> submissionsByTask,
            Map<Long, List<StudentSubmission>> submissionsByStudent,
            Map<Long, List<SurveyRecord>> surveyRecordsBySurvey,
            Map<Long, List<SurveyRecord>> surveyRecordsByStudent,
            Map<Long, List<StudentVisitRecord>> visitsByStudent,
            Map<Long, List<AiChatMessage>> aiByStudent,
            Map<Long, List<StudentMistakeBook>> mistakesByStudent
    ) {}
}
