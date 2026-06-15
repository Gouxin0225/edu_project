package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.AssistantExamVO;
import com.example.edubackend.dto.AssistantSurveyResponseVO;
import com.example.edubackend.dto.ExamParticipationVO;
import com.example.edubackend.dto.RemindStudentsDTO;
import com.example.edubackend.dto.SubmissionDetailVO;
import com.example.edubackend.dto.SubmissionListVO;
import com.example.edubackend.dto.SurveyListVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.SurveyAnswerDetail;
import com.example.edubackend.entity.SurveyQuestion;
import com.example.edubackend.entity.SurveyRecord;
import com.example.edubackend.entity.SurveyTask;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SurveyAnswerDetailMapper;
import com.example.edubackend.mapper.SurveyQuestionMapper;
import com.example.edubackend.mapper.SurveyRecordMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IStudentSubmissionService;
import com.example.edubackend.service.ISurveyService;
import com.example.edubackend.service.OperationAuditLogService;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
@RequestMapping("/api/assistant")
@RequireRole({"ASSISTANT"})
@RequiredArgsConstructor
public class AssistantInsightController {

    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SysClassMapper sysClassMapper;
    private final SysUserMapper sysUserMapper;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final StudentSubmissionMapper studentSubmissionMapper;
    private final SurveyTaskMapper surveyTaskMapper;
    private final SurveyRecordMapper surveyRecordMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;
    private final SurveyAnswerDetailMapper surveyAnswerDetailMapper;
    private final IStudentSubmissionService submissionService;
    private final ISurveyService surveyService;
    private final ObjectMapper objectMapper;
    private final ExamSettingHelper examSettingHelper;
    private final OperationAuditLogService auditLogService;

    @GetMapping("/exams")
    public Result<List<AssistantExamVO>> getExamList() {
        Set<Long> classIds = getManagedClassIds();
        Map<Long, SysClass> classMap = loadClassMap(classIds);
        Map<Long, SysUser> studentMap = loadStudents(classIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<AssessmentTask> exams = assessmentTaskMapper.selectList(
                new LambdaQueryWrapper<AssessmentTask>()
                        .eq(AssessmentTask::getType, "EXAM")
                        .orderByDesc(AssessmentTask::getCreateTime)
        ).stream()
                .filter(exam -> intersects(parseTargetClassIds(exam.getTargetClassIds()), classIds))
                .toList();

        List<Long> examIds = exams.stream().map(AssessmentTask::getId).filter(Objects::nonNull).toList();
        Map<Long, List<StudentSubmission>> submissionsByTask = examIds.isEmpty() || studentMap.isEmpty()
                ? Map.of()
                : studentSubmissionMapper.selectList(new LambdaQueryWrapper<StudentSubmission>()
                        .in(StudentSubmission::getTaskId, examIds)
                        .in(StudentSubmission::getStudentId, studentMap.keySet()))
                .stream().collect(Collectors.groupingBy(StudentSubmission::getTaskId));

        List<AssistantExamVO> result = new ArrayList<>();
        for (AssessmentTask exam : exams) {
            Set<Long> scopedClassIds = scopedTargetClassIds(exam.getTargetClassIds(), classIds);
            List<SysUser> targetStudents = studentMap.values().stream()
                    .filter(student -> scopedClassIds.contains(student.getClassId()))
                    .toList();
            Map<Long, StudentSubmission> latestByStudent = latestSubmissionByStudent(
                    submissionsByTask.getOrDefault(exam.getId(), List.of()));
            List<StudentSubmission> latestSubmissions = new ArrayList<>(latestByStudent.values());

            AssistantExamVO vo = new AssistantExamVO();
            vo.setId(exam.getId());
            vo.setTitle(exam.getTitle());
            vo.setStartTime(exam.getStartTime());
            vo.setEndTime(exam.getEndTime());
            vo.setTotalScore(exam.getTotalScore());
            vo.setStatus(resolveTaskStatus(exam));
            vo.setTargetClassNames(scopedClassIds.stream()
                    .map(classMap::get)
                    .filter(Objects::nonNull)
                    .map(SysClass::getClassName)
                    .toList());
            vo.setTargetStudentCount(targetStudents.size());
            vo.setSubmittedCount((int) latestSubmissions.stream().filter(item -> !"UN".equals(item.getStatus())).count());
            vo.setInProgressCount((int) latestSubmissions.stream().filter(item -> "UN".equals(item.getStatus())).count());
            vo.setNotSubmittedCount(Math.max(targetStudents.size() - latestByStudent.size(), 0));
            vo.setAverageScore(averageScore(latestSubmissions));
            vo.setPassRate(passRate(exam, latestSubmissions));
            result.add(vo);
        }
        return Result.success(result);
    }

    @GetMapping("/exam/{id}/participation")
    public Result<ExamParticipationVO> getExamParticipation(@PathVariable Long id) {
        AssessmentTask exam = assertCanViewExam(id);
        ExamParticipationVO vo = submissionService.getExamParticipation(exam.getId());
        filterParticipation(vo);
        return Result.success(vo);
    }

    @GetMapping("/exam/{id}/submissions")
    public Result<List<SubmissionListVO>> getExamSubmissions(@PathVariable Long id) {
        AssessmentTask exam = assertCanViewExam(id);
        Set<Long> studentIds = getManagedStudentIdsForTask(exam.getTargetClassIds());
        List<SubmissionListVO> list = submissionService.getSubmissionList(exam.getId()).stream()
                .filter(item -> studentIds.contains(item.getStudentId()))
                .toList();
        return Result.success(list);
    }

    @GetMapping("/exam/{id}/submission/{submissionId}")
    public Result<SubmissionDetailVO> getExamSubmissionDetail(@PathVariable Long id, @PathVariable Long submissionId) {
        AssessmentTask exam = assertCanViewExam(id);
        StudentSubmission submission = studentSubmissionMapper.selectById(submissionId);
        if (submission == null || !exam.getId().equals(submission.getTaskId())) {
            throw new BusinessException(404, "提交记录不存在");
        }
        if (!getManagedStudentIdsForTask(exam.getTargetClassIds()).contains(submission.getStudentId())) {
            throw new BusinessException(403, "无权查看该学生答卷");
        }
        return Result.success(submissionService.getSubmissionDetail(exam.getId(), submissionId));
    }

    @PostMapping("/exam/{id}/remind")
    public Result<Void> remindExamStudents(@PathVariable Long id, @RequestBody RemindStudentsDTO dto) {
        AssessmentTask exam = assertCanViewExam(id);
        List<Long> studentIds = normalizeStudentIds(dto);
        if (studentIds.isEmpty()) {
            return Result.success();
        }
        ExamParticipationVO participation = submissionService.getExamParticipation(exam.getId());
        filterParticipation(participation);
        Set<Long> missingStudentIds = participation.getNotSubmittedStudents().stream()
                .map(ExamParticipationVO.StudentInfo::getStudentId)
                .collect(Collectors.toSet());
        List<Long> remindIds = studentIds.stream().filter(missingStudentIds::contains).toList();
        if (!remindIds.isEmpty()) {
            auditLogService.record(
                    "EXAM_REMIND",
                    "EXAM",
                    exam.getId(),
                    "提醒未参加考试学生，examTitle=" + exam.getTitle() + "，studentIds=" + remindIds + "，提醒人数=" + remindIds.size()
            );
        }
        return Result.success();
    }

    @GetMapping("/surveys")
    public Result<List<SurveyListVO>> getSurveyList() {
        return Result.success(surveyService.getSurveyList(UserContext.getUserId(), "ASSISTANT"));
    }

    @GetMapping("/survey/{id}/responses")
    public Result<AssistantSurveyResponseVO> getSurveyResponses(@PathVariable Long id) {
        SurveyTask survey = assertCanViewSurvey(id);
        Set<Long> managedClassIds = getManagedClassIds();
        Set<Long> scopedClassIds = scopedTargetClassIds(survey.getTargetClassIds(), managedClassIds);
        List<SysUser> students = loadStudents(scopedClassIds);
        Map<Long, SysUser> studentMap = students.stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, SysClass> classMap = loadClassMap(scopedClassIds);

        List<SurveyRecord> records = studentMap.isEmpty() ? List.of() : surveyRecordMapper.selectList(
                new LambdaQueryWrapper<SurveyRecord>()
                        .eq(SurveyRecord::getSurveyId, survey.getId())
                        .in(SurveyRecord::getStudentId, studentMap.keySet())
                        .orderByDesc(SurveyRecord::getSubmitTime)
        );
        List<SurveyQuestion> questions = surveyQuestionMapper.selectList(
                new LambdaQueryWrapper<SurveyQuestion>()
                        .eq(SurveyQuestion::getSurveyId, survey.getId())
                        .orderByAsc(SurveyQuestion::getSortOrder)
        );
        Map<Long, SurveyQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(SurveyQuestion::getId, Function.identity()));
        List<Long> recordIds = records.stream().map(SurveyRecord::getId).filter(Objects::nonNull).toList();
        Map<Long, List<SurveyAnswerDetail>> answersByRecord = recordIds.isEmpty()
                ? Map.of()
                : surveyAnswerDetailMapper.selectList(new LambdaQueryWrapper<SurveyAnswerDetail>().in(SurveyAnswerDetail::getRecordId, recordIds))
                .stream().collect(Collectors.groupingBy(SurveyAnswerDetail::getRecordId));

        boolean anonymous = survey.getIsAnonymousRequired() != null && survey.getIsAnonymousRequired() == 1;
        AssistantSurveyResponseVO vo = new AssistantSurveyResponseVO();
        vo.setSurveyId(survey.getId());
        vo.setTitle(survey.getTitle());
        vo.setIsAnonymousRequired(survey.getIsAnonymousRequired());
        vo.setTotalStudents(students.size());
        vo.setSubmittedCount(records.size());
        vo.setUnsubmittedCount(Math.max(students.size() - records.size(), 0));

        Set<Long> submittedStudentIds = new HashSet<>();
        int anonymousIndex = 1;
        for (SurveyRecord record : records) {
            submittedStudentIds.add(record.getStudentId());
            SysUser student = studentMap.get(record.getStudentId());
            AssistantSurveyResponseVO.ResponseItem item = new AssistantSurveyResponseVO.ResponseItem();
            item.setRecordId(record.getId());
            item.setSubmitTime(record.getSubmitTime());
            if (anonymous) {
                item.setStudentName("匿名反馈 " + anonymousIndex++);
                item.setClassName(student == null || classMap.get(student.getClassId()) == null ? "-" : classMap.get(student.getClassId()).getClassName());
            } else if (student != null) {
                item.setStudentId(student.getId());
                item.setStudentName(student.getRealName());
                item.setUsername(student.getUsername());
                item.setClassName(classMap.get(student.getClassId()) == null ? "-" : classMap.get(student.getClassId()).getClassName());
            }
            item.setAnswers(answersByRecord.getOrDefault(record.getId(), List.of()).stream()
                    .map(answer -> buildSurveyAnswer(answer, questionMap.get(answer.getSurveyQuestionId())))
                    .filter(Objects::nonNull)
                    .toList());
            vo.getResponses().add(item);
        }

        if (!anonymous) {
            students.stream()
                    .filter(student -> !submittedStudentIds.contains(student.getId()))
                    .sorted(Comparator.comparing(SysUser::getUsername, Comparator.nullsLast(String::compareTo)))
                    .map(student -> buildStudentItem(student, classMap))
                    .forEach(vo.getUnsubmittedStudents()::add);
        }
        return Result.success(vo);
    }

    @PostMapping("/survey/{id}/remind")
    public Result<Void> remindSurveyStudents(@PathVariable Long id, @RequestBody RemindStudentsDTO dto) {
        SurveyTask survey = assertCanViewSurvey(id);
        if (survey.getIsAnonymousRequired() != null && survey.getIsAnonymousRequired() == 1) {
            throw new BusinessException(400, "匿名问卷不支持按名单提醒");
        }
        List<Long> studentIds = normalizeStudentIds(dto);
        if (studentIds.isEmpty()) {
            return Result.success();
        }

        Set<Long> scopedClassIds = scopedTargetClassIds(survey.getTargetClassIds(), getManagedClassIds());
        Set<Long> managedStudentIds = loadStudents(scopedClassIds).stream()
                .map(SysUser::getId)
                .collect(Collectors.toSet());
        if (managedStudentIds.isEmpty()) {
            return Result.success();
        }
        Set<Long> submittedStudentIds = surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                        .eq(SurveyRecord::getSurveyId, survey.getId())
                        .in(SurveyRecord::getStudentId, managedStudentIds))
                .stream()
                .map(SurveyRecord::getStudentId)
                .collect(Collectors.toSet());
        List<Long> remindIds = studentIds.stream()
                .filter(managedStudentIds::contains)
                .filter(idValue -> !submittedStudentIds.contains(idValue))
                .toList();
        if (!remindIds.isEmpty()) {
            auditLogService.record(
                    "SURVEY_REMIND",
                    "SURVEY",
                    survey.getId(),
                    "提醒未填写问卷学生，surveyTitle=" + survey.getTitle() + "，studentIds=" + remindIds + "，提醒人数=" + remindIds.size()
            );
        }
        return Result.success();
    }

    private AssistantSurveyResponseVO.AnswerItem buildSurveyAnswer(SurveyAnswerDetail answer, SurveyQuestion question) {
        if (question == null) {
            return null;
        }
        AssistantSurveyResponseVO.AnswerItem item = new AssistantSurveyResponseVO.AnswerItem();
        item.setQuestionId(question.getId());
        item.setTitle(question.getTitle());
        item.setType(question.getType());
        item.setAnswerValue(answer.getAnswerValue());
        return item;
    }

    private List<Long> normalizeStudentIds(RemindStudentsDTO dto) {
        if (dto == null || dto.getStudentIds() == null) {
            return List.of();
        }
        return dto.getStudentIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private AssistantSurveyResponseVO.StudentItem buildStudentItem(SysUser student, Map<Long, SysClass> classMap) {
        AssistantSurveyResponseVO.StudentItem item = new AssistantSurveyResponseVO.StudentItem();
        item.setStudentId(student.getId());
        item.setStudentName(student.getRealName());
        item.setUsername(student.getUsername());
        item.setClassName(classMap.get(student.getClassId()) == null ? "-" : classMap.get(student.getClassId()).getClassName());
        return item;
    }

    private AssessmentTask assertCanViewExam(Long examId) {
        AssessmentTask exam = assessmentTaskMapper.selectById(examId);
        if (exam == null || !"EXAM".equals(exam.getType())) {
            throw new BusinessException(404, "考试不存在");
        }
        if (!intersects(parseTargetClassIds(exam.getTargetClassIds()), getManagedClassIds())) {
            throw new BusinessException(403, "无权查看该考试");
        }
        return exam;
    }

    private SurveyTask assertCanViewSurvey(Long surveyId) {
        SurveyTask survey = surveyTaskMapper.selectById(surveyId);
        if (survey == null) {
            throw new BusinessException(404, "问卷不存在");
        }
        if (!intersects(parseTargetClassIds(survey.getTargetClassIds()), getManagedClassIds())) {
            throw new BusinessException(403, "无权查看该问卷");
        }
        return survey;
    }

    private void filterParticipation(ExamParticipationVO vo) {
        Set<Long> allowedStudentIds = getManagedClassIds().isEmpty() ? Set.of() : loadStudents(getManagedClassIds()).stream()
                .map(SysUser::getId)
                .collect(Collectors.toSet());
        vo.setSubmittedStudents(filterStudentInfos(vo.getSubmittedStudents(), allowedStudentIds));
        vo.setInProgressStudents(filterStudentInfos(vo.getInProgressStudents(), allowedStudentIds));
        vo.setNotSubmittedStudents(filterStudentInfos(vo.getNotSubmittedStudents(), allowedStudentIds));
    }

    private List<ExamParticipationVO.StudentInfo> filterStudentInfos(List<ExamParticipationVO.StudentInfo> items, Set<Long> allowedStudentIds) {
        if (items == null) {
            return List.of();
        }
        return items.stream().filter(item -> allowedStudentIds.contains(item.getStudentId())).toList();
    }

    private Set<Long> getManagedStudentIdsForTask(String targetClassIdsJson) {
        return loadStudents(scopedTargetClassIds(targetClassIdsJson, getManagedClassIds())).stream()
                .map(SysUser::getId)
                .collect(Collectors.toSet());
    }

    private Set<Long> getManagedClassIds() {
        return teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>().eq(TeacherClassRel::getTeacherId, UserContext.getUserId())
        ).stream()
                .map(TeacherClassRel::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<SysUser> loadStudents(Collection<Long> classIds) {
        List<Long> ids = classIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getIsDeleted, (byte) 0)
                .in(SysUser::getClassId, ids));
    }

    private Map<Long, SysClass> loadClassMap(Collection<Long> classIds) {
        List<Long> ids = classIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>().in(SysClass::getId, ids))
                .stream().collect(Collectors.toMap(SysClass::getId, Function.identity()));
    }

    private Set<Long> scopedTargetClassIds(String targetClassIdsJson, Set<Long> managedClassIds) {
        List<Long> targetClassIds = parseTargetClassIds(targetClassIdsJson);
        if (targetClassIds.isEmpty()) {
            return managedClassIds;
        }
        return targetClassIds.stream().filter(managedClassIds::contains).collect(Collectors.toSet());
    }

    private boolean intersects(List<Long> targetClassIds, Set<Long> managedClassIds) {
        if (managedClassIds.isEmpty()) {
            return false;
        }
        if (targetClassIds.isEmpty()) {
            return true;
        }
        return targetClassIds.stream().anyMatch(managedClassIds::contains);
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (targetClassIdsJson == null || targetClassIdsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {})
                    .stream().filter(Objects::nonNull).distinct().toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<Long, StudentSubmission> latestSubmissionByStudent(Collection<StudentSubmission> submissions) {
        Map<Long, StudentSubmission> result = new HashMap<>();
        for (StudentSubmission submission : submissions) {
            result.merge(submission.getStudentId(), submission, this::latestSubmission);
        }
        return result;
    }

    private StudentSubmission latestSubmission(StudentSubmission left, StudentSubmission right) {
        LocalDateTime leftTime = left.getSubmitTime() != null ? left.getSubmitTime() : left.getStartTime();
        LocalDateTime rightTime = right.getSubmitTime() != null ? right.getSubmitTime() : right.getStartTime();
        if (leftTime != null && rightTime != null && !leftTime.equals(rightTime)) {
            return leftTime.isAfter(rightTime) ? left : right;
        }
        long leftId = left.getId() == null ? 0L : left.getId();
        long rightId = right.getId() == null ? 0L : right.getId();
        return leftId >= rightId ? left : right;
    }

    private BigDecimal averageScore(List<StudentSubmission> submissions) {
        List<BigDecimal> scores = submissions.stream()
                .map(StudentSubmission::getTotalScoreGained)
                .filter(Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal passRate(AssessmentTask exam, List<StudentSubmission> submissions) {
        List<StudentSubmission> scored = submissions.stream()
                .filter(item -> item.getTotalScoreGained() != null)
                .toList();
        if (scored.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int passScore = examSettingHelper.getPassScore(exam);
        long passed = scored.stream()
                .filter(item -> item.getTotalScoreGained().compareTo(BigDecimal.valueOf(passScore)) >= 0)
                .count();
        return BigDecimal.valueOf(passed * 100)
                .divide(BigDecimal.valueOf(scored.size()), 1, RoundingMode.HALF_UP);
    }

    private String resolveTaskStatus(AssessmentTask task) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getStartTime() != null && task.getStartTime().isAfter(now)) {
            return "NOT_STARTED";
        }
        if (task.getEndTime() != null && task.getEndTime().isBefore(now)) {
            return "ENDED";
        }
        return "PUBLISHED";
    }
}
