package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.dto.*;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.StudentAnswerDetail;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.TaskQuestionRel;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.ClassStudentRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.service.*;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentSubmissionServiceImpl extends ServiceImpl<StudentSubmissionMapper, StudentSubmission> implements IStudentSubmissionService {

    private final IStudentAnswerDetailService answerDetailService;
    private final ITaskQuestionRelService taskQuestionRelService;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final QuestionBankMapper questionBankMapper;
    private final SysUserMapper sysUserMapper;
    private final SysClassMapper sysClassMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final ObjectMapper objectMapper;
    private final ExamSettingHelper examSettingHelper;

    @Override
    public List<SubmissionListVO> getSubmissionList(Long examId) {
        List<StudentSubmission> allSubmissions = list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
        );
        List<StudentSubmission> submissions = new ArrayList<>(latestSubmissionByStudent(allSubmissions).values());
        submissions.sort(Comparator
                .comparing(this::submissionActivityTime, Comparator.nullsLast(LocalDateTime::compareTo))
                .reversed()
                .thenComparing(StudentSubmission::getId, Comparator.nullsLast(Long::compareTo)));

        List<SubmissionListVO> result = new ArrayList<>();
        for (StudentSubmission s : submissions) {
            SubmissionListVO vo = new SubmissionListVO();
            vo.setSubmissionId(s.getId());
            vo.setStudentId(s.getStudentId());
            vo.setStatus(s.getStatus());
            vo.setTotalScoreGained(s.getTotalScoreGained());
            vo.setScoreGained(s.getTotalScoreGained());
            vo.setSubmitTime(s.getSubmitTime());
            vo.setSwitchScreenCount(s.getSwitchScreenCount());
            
            SysUser student = sysUserMapper.selectById(s.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName());
            }
            
            result.add(vo);
        }
        return result;
    }

    @Override
    public SubmissionDetailVO getSubmissionDetail(Long examId, Long submissionId) {
        StudentSubmission submission = getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getId, submissionId)
        );

        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }

        SubmissionDetailVO vo = new SubmissionDetailVO();
        vo.setSubmissionId(submission.getId());
        vo.setStudentId(submission.getStudentId());
        vo.setStatus(submission.getStatus());
        vo.setTotalScoreGained(submission.getTotalScoreGained());
        vo.setSwitchScreenCount(submission.getSwitchScreenCount());
        
        SysUser student = sysUserMapper.selectById(submission.getStudentId());
        if (student != null) {
            vo.setStudentName(student.getRealName());
        }
        
        if (submission.getSubmitTime() != null) {
            vo.setSubmitTime(formatTime(submission.getSubmitTime()));
        }
        vo.setGradeTime(null);

        List<TaskQuestionRel> rels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
                        .orderByAsc(TaskQuestionRel::getSortOrder)
        );

        Map<Long, QuestionBank> questionMap = loadQuestionMap(rels);
        Map<Long, Integer> scoreMap = new HashMap<>();
        for (TaskQuestionRel rel : rels) {
            scoreMap.put(rel.getQuestionId(), rel.getScore());
        }

        List<StudentAnswerDetail> answerDetails = answerDetailService.list(
                new LambdaQueryWrapper<StudentAnswerDetail>()
                        .eq(StudentAnswerDetail::getSubmissionId, submissionId)
        );
        Map<Long, StudentAnswerDetail> answerMap = new HashMap<>();
        for (StudentAnswerDetail a : answerDetails) {
            answerMap.put(a.getQuestionId(), a);
        }

        List<SubmissionDetailVO.QuestionAnswerVO> answerVOList = new ArrayList<>();
        for (TaskQuestionRel rel : rels) {
            QuestionBank q = questionMap.get(rel.getQuestionId());
            if (q == null) continue;

            SubmissionDetailVO.QuestionAnswerVO qvo = new SubmissionDetailVO.QuestionAnswerVO();
            qvo.setQuestionId(q.getId());
            qvo.setType(q.getType());
            qvo.setContent(q.getContent());
            qvo.setOptionsJson(q.getOptionsJson());
            qvo.setQuestionScore(scoreMap.get(q.getId()));
            qvo.setStandardAnswer(q.getStandardAnswer());

            StudentAnswerDetail answer = answerMap.get(q.getId());
            if (answer != null) {
                qvo.setStudentAnswer(answer.getStudentAnswer());
                if (answer.getIsCorrect() != null) {
                    qvo.setIsCorrect(answer.getIsCorrect() == 1);
                }
                qvo.setScoreGained(answer.getScoreGained());
                qvo.setAiSuggestScore(answer.getAiSuggestScore());
                qvo.setAiSuggestDetail(answer.getAiSuggestDetail());
            }

            answerVOList.add(qvo);
        }
        vo.setAnswers(answerVOList);

        return vo;
    }

    @Override
    @Transactional
    public void gradeSubmission(Long examId, Long submissionId, List<GradeItemDTO> gradeItems) {
        StudentSubmission submission = getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getId, submissionId)
        );

        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }

        if (!"SUBMITTED".equals(submission.getStatus())
                && !"GRADED".equals(submission.getStatus())
                && !"RETURNED".equals(submission.getStatus())) {
            throw new BusinessException(400, "只能批改已提交或已批改的答卷");
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        Map<Long, Integer> questionScoreMap = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
        ).stream().collect(java.util.stream.Collectors.toMap(
                TaskQuestionRel::getQuestionId, TaskQuestionRel::getScore, (left, right) -> left));

        for (GradeItemDTO item : gradeItems) {
            StudentAnswerDetail answer = answerDetailService.getOne(
                    new LambdaQueryWrapper<StudentAnswerDetail>()
                            .eq(StudentAnswerDetail::getSubmissionId, submissionId)
                            .eq(StudentAnswerDetail::getQuestionId, item.getQuestionId())
            );

            Integer fullScore = questionScoreMap.get(item.getQuestionId());
            if (fullScore == null) {
                throw new BusinessException(400, "题目不属于当前试卷: " + item.getQuestionId());
            }
            if (item.getScoreGained() == null) {
                throw new BusinessException(400, "题目分数不能为空: " + item.getQuestionId());
            }
            if (item.getScoreGained().compareTo(BigDecimal.ZERO) < 0
                    || item.getScoreGained().compareTo(BigDecimal.valueOf(fullScore)) > 0) {
                throw new BusinessException(400, "题目分数超出范围: " + item.getQuestionId());
            }
            if (answer == null) {
                answer = new StudentAnswerDetail();
                answer.setSubmissionId(submissionId);
                answer.setQuestionId(item.getQuestionId());
            }

            answer.setScoreGained(item.getScoreGained());
            answer.setAiSuggestScore(item.getAiSuggestScore());
            answer.setAiSuggestDetail(item.getAiSuggestDetail());
            if (item.getScoreGained() != null && item.getScoreGained().compareTo(BigDecimal.ZERO) > 0) {
                if (fullScore != null && item.getScoreGained().compareTo(BigDecimal.valueOf(fullScore)) < 0) {
                    answer.setIsCorrect((byte) 2);
                } else {
                    answer.setIsCorrect((byte) 1);
                }
            } else {
                answer.setIsCorrect((byte) 0);
            }
            answerDetailService.saveOrUpdate(answer);

            totalScore = totalScore.add(item.getScoreGained() != null ? item.getScoreGained() : BigDecimal.ZERO);
        }

        submission.setTotalScoreGained(totalScore);
        submission.setStatus("GRADED");
        updateById(submission);
    }

    @Override
    @Transactional
    public void publishScore(Long examId) {
        List<StudentSubmission> submissions = list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
        );

        boolean hasPendingGrade = submissions.stream().anyMatch(s -> "SUBMITTED".equals(s.getStatus()));
        if (hasPendingGrade) {
            throw new BusinessException(400, "仍有待批改答卷，不能发布成绩");
        }

        for (StudentSubmission s : submissions) {
            if (!"GRADED".equals(s.getStatus())) {
                continue;
            }
            s.setStatus("GRADED");
            updateById(s);
        }
    }

    @Override
    public List<StudentExamRecordVO> getStudentExamRecords(Long studentId) {
        List<StudentSubmission> submissions = list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getStudentId, studentId)
                        .orderByDesc(StudentSubmission::getSubmitTime)
        );

        List<StudentExamRecordVO> result = new ArrayList<>();
        for (StudentSubmission s : submissions) {
            StudentExamRecordVO vo = new StudentExamRecordVO();
            vo.setExamId(s.getTaskId());
            vo.setStatus(s.getStatus());
            vo.setScoreGained(s.getTotalScoreGained());
            vo.setSubmitTime(s.getSubmitTime());

            AssessmentTask exam = assessmentTaskMapper.selectById(s.getTaskId());
            if (exam == null || !"EXAM".equals(exam.getType())) {
                continue;
            }
            vo.setTitle(exam.getTitle());
            vo.setDeadline(exam.getEndTime());

            result.add(vo);
        }
        return result;
    }

    @Override
    public StudentExamResultVO getStudentExamResult(Long examId, Long studentId) {
        StudentSubmission submission = getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .eq(StudentSubmission::getStudentId, studentId)
        );

        if (submission == null) {
            throw new BusinessException(404, "未找到考试记录");
        }

        AssessmentTask exam = assessmentTaskMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }

        StudentExamResultVO vo = new StudentExamResultVO();
        vo.setExamId(examId);
        vo.setExamTitle(exam.getTitle());
        vo.setTotalScore(exam.getTotalScore());
        vo.setScoreGained(submission.getTotalScoreGained());
        vo.setSubmitTime(submission.getSubmitTime());

        Integer examPassScore = examSettingHelper.getPassScore(exam);
        boolean passed = submission.getTotalScoreGained() != null
                && submission.getTotalScoreGained().compareTo(BigDecimal.valueOf(examPassScore)) >= 0;
        vo.setPassed(passed);
        
        int totalStudents = (int) count(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
        );
        vo.setTotalStudents(totalStudents);
        
        int rank = (int) count(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .isNotNull(StudentSubmission::getTotalScoreGained)
                        .gt(StudentSubmission::getTotalScoreGained, submission.getTotalScoreGained())
        ) + 1;
        vo.setRank(rank);

        boolean showAnalysis = examSettingHelper.isShowAnalysis(exam);
        vo.setShowAnalysis(showAnalysis);

        List<TaskQuestionRel> rels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
                        .orderByAsc(TaskQuestionRel::getSortOrder)
        );

        Map<Long, QuestionBank> questionMap = loadQuestionMap(rels);
        Map<Long, Integer> scoreMap = new HashMap<>();
        for (TaskQuestionRel rel : rels) {
            scoreMap.put(rel.getQuestionId(), rel.getScore());
        }

        List<StudentAnswerDetail> answers = answerDetailService.list(
                new LambdaQueryWrapper<StudentAnswerDetail>()
                        .eq(StudentAnswerDetail::getSubmissionId, submission.getId())
        );
        Map<Long, StudentAnswerDetail> answerMap = new HashMap<>();
        for (StudentAnswerDetail a : answers) {
            answerMap.put(a.getQuestionId(), a);
        }

        List<StudentExamResultVO.QuestionResultVO> answerVOList = new ArrayList<>();
        for (TaskQuestionRel rel : rels) {
            QuestionBank q = questionMap.get(rel.getQuestionId());
            if (q == null) continue;

            StudentExamResultVO.QuestionResultVO qvo = new StudentExamResultVO.QuestionResultVO();
            qvo.setQuestionId(q.getId());
            qvo.setType(q.getType());
            qvo.setDifficulty(q.getDifficulty());
            qvo.setContent(q.getContent());
            qvo.setOptionsJson(q.getOptionsJson());
            qvo.setScore(scoreMap.get(q.getId()));
            qvo.setStandardAnswer(showAnalysis ? q.getStandardAnswer() : null);
            qvo.setAnalysis(showAnalysis ? q.getAnalysis() : null);

            StudentAnswerDetail answer = answerMap.get(q.getId());
            if (answer != null) {
                qvo.setStudentAnswer(answer.getStudentAnswer());
                qvo.setIsCorrect(answer.getIsCorrect() != null ? answer.getIsCorrect() == 1 : null);
                qvo.setScoreGained(answer.getScoreGained());
                qvo.setAiSuggestScore(answer.getAiSuggestScore());
                qvo.setAiSuggestDetail(answer.getAiSuggestDetail());
            }

            answerVOList.add(qvo);
        }
        vo.setAnswers(answerVOList);

        return vo;
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

    @Override
    public List<ScoreExportDTO> getExportData(Long examId) {
        AssessmentTask exam = getExamForExport(examId);
        BigDecimal passScore = BigDecimal.valueOf(examSettingHelper.getPassScore(exam));

        List<SysUser> students = getExamTargetStudents(exam);
        Map<Long, StudentSubmission> submissionMap = latestSubmissionByStudent(examId);
        List<ScoreExportDTO> result = new ArrayList<>();
        for (SysUser student : students) {
            StudentSubmission submission = submissionMap.get(student.getId());
            ScoreExportDTO dto = new ScoreExportDTO();
            dto.setStudentNo(student.getUsername());
            dto.setStudentName(student.getRealName());
            dto.setClassName(getStudentClassName(student));
            dto.setStatus(statusLabel(submission == null ? "NOT_SUBMITTED" : submission.getStatus()));
            dto.setExamTotalScore(exam.getTotalScore());
            if (submission != null) {
                dto.setTotalScore(submission.getTotalScoreGained());
                dto.setIsPass(passLabel(submission.getTotalScoreGained(), passScore));
                dto.setSubmitTime(formatTime(submission.getSubmitTime()));
                dto.setSwitchScreenCount(submission.getSwitchScreenCount());
            } else {
                dto.setIsPass("-");
                dto.setSubmitTime("");
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<ExamAnswerDetailExportRow> getAnswerExportData(Long examId) {
        AssessmentTask exam = getExamForExport(examId);
        List<SysUser> students = getExamTargetStudents(exam);
        Map<Long, StudentSubmission> submissionMap = latestSubmissionByStudent(examId);
        Map<Long, Map<Long, StudentAnswerDetail>> answerMap = answerDetailsBySubmission(submissionMap.values());

        List<TaskQuestionRel> rels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
                        .orderByAsc(TaskQuestionRel::getSortOrder)
        );
        Map<Long, QuestionBank> questionMap = loadQuestionMap(rels);

        List<ExamAnswerDetailExportRow> rows = new ArrayList<>();
        for (SysUser student : students) {
            StudentSubmission submission = submissionMap.get(student.getId());
            if (rels.isEmpty()) {
                rows.add(buildAnswerExportRow(student, submission, null, null, null, null));
                continue;
            }

            int questionNo = 1;
            for (TaskQuestionRel rel : rels) {
                StudentAnswerDetail answer = null;
                if (submission != null) {
                    answer = answerMap.getOrDefault(submission.getId(), Map.of()).get(rel.getQuestionId());
                }
                rows.add(buildAnswerExportRow(
                        student,
                        submission,
                        questionNo++,
                        rel,
                        questionMap.get(rel.getQuestionId()),
                        answer));
            }
        }
        return rows;
    }

    @Override
    public List<StudentExamListVO> getStudentExamList(Long studentId) {
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            return new ArrayList<>();
        }

        Set<Long> studentClassIds = getStudentClassIds(student);
        if (studentClassIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<AssessmentTask> allExams = assessmentTaskMapper.selectList(
                new LambdaQueryWrapper<AssessmentTask>()
                        .eq(AssessmentTask::getType, "EXAM")
                        .orderByDesc(AssessmentTask::getStartTime)
        );

        List<StudentSubmission> submissions = list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getStudentId, studentId)
        );
        Map<Long, StudentSubmission> submissionMap = new HashMap<>();
        for (StudentSubmission s : submissions) {
            submissionMap.put(s.getTaskId(), s);
        }

        List<StudentExamListVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AssessmentTask exam : allExams) {
            if (exam.getTargetClassIds() == null) continue;
            if (!examSettingHelper.isPublished(exam)) continue;

            List<Long> targetClassIds;
            try {
                targetClassIds = objectMapper.readValue(exam.getTargetClassIds(),
                        new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                continue;
            }

            if (!targetClassIds.isEmpty() && targetClassIds.stream().noneMatch(studentClassIds::contains)) continue;

            StudentExamListVO vo = new StudentExamListVO();
            vo.setExamId(exam.getId());
            vo.setExamTitle(exam.getTitle());
            vo.setStartTime(exam.getStartTime());
            vo.setEndTime(exam.getEndTime());
            vo.setTotalScore(exam.getTotalScore());

            StudentSubmission submission = submissionMap.get(exam.getId());

            if (submission != null) {
                vo.setStatus(submission.getStatus());
                vo.setSubmissionId(submission.getId());
                if ("GRADED".equals(submission.getStatus()) && submission.getTotalScoreGained() != null) {
                    vo.setScoreGained(submission.getTotalScoreGained().intValue());
                }
            } else if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
                vo.setStatus("NOT_STARTED");
            } else if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
                vo.setStatus("EXPIRED");
            } else {
                vo.setStatus("ONGOING");
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    public ExamParticipationVO getExamParticipation(Long examId) {
        AssessmentTask exam = assessmentTaskMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }

        ExamParticipationVO vo = new ExamParticipationVO();
        vo.setExamId(examId);
        vo.setExamTitle(exam.getTitle());

        List<Long> targetClassIds = new ArrayList<>();
        if (exam.getTargetClassIds() != null && !exam.getTargetClassIds().isEmpty()) {
            try {
                targetClassIds = objectMapper.readValue(exam.getTargetClassIds(), new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                targetClassIds = new ArrayList<>();
            }
        }

        List<SysUser> allStudents = targetClassIds.isEmpty()
                ? sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getIsDeleted, (byte) 0))
                : getStudentsInClasses(targetClassIds);

        List<StudentSubmission> submissions = list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
        );

        Map<Long, StudentSubmission> submissionMap = latestSubmissionByStudent(submissions);

        List<ExamParticipationVO.StudentInfo> submitted = new ArrayList<>();
        List<ExamParticipationVO.StudentInfo> inProgress = new ArrayList<>();
        List<ExamParticipationVO.StudentInfo> notSubmitted = new ArrayList<>();

        for (SysUser student : allStudents) {
            ExamParticipationVO.StudentInfo info = new ExamParticipationVO.StudentInfo();
            info.setStudentId(student.getId());
            info.setStudentName(student.getRealName());

            StudentSubmission submission = submissionMap.get(student.getId());
            if (submission != null) {
                info.setStartTime(formatTimeOrNull(submission.getStartTime()));
                info.setSubmitTime(formatTimeOrNull(submission.getSubmitTime()));
                info.setStatus(submission.getStatus());
                if ("UN".equals(submission.getStatus())) {
                    inProgress.add(info);
                } else {
                    submitted.add(info);
                }
            } else {
                info.setStatus("NOT_SUBMITTED");
                notSubmitted.add(info);
            }
        }

        vo.setSubmittedStudents(submitted);
        vo.setInProgressStudents(inProgress);
        vo.setNotSubmittedStudents(notSubmitted);

        return vo;
    }

    private AssessmentTask getExamForExport(Long examId) {
        AssessmentTask exam = assessmentTaskMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        if (!"EXAM".equals(exam.getType())) {
            throw new BusinessException(400, "任务不是考试");
        }
        return exam;
    }

    private List<SysUser> getExamTargetStudents(AssessmentTask exam) {
        List<Long> targetClassIds = parseTargetClassIds(exam.getTargetClassIds());
        if (targetClassIds.isEmpty()) {
            return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, "STUDENT")
                    .eq(SysUser::getIsDeleted, (byte) 0)
                    .orderByAsc(SysUser::getClassId)
                    .orderByAsc(SysUser::getUsername));
        }
        return getStudentsInClasses(targetClassIds).stream()
                .sorted(Comparator
                        .comparing(SysUser::getClassId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(SysUser::getUsername, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (targetClassIdsJson == null || targetClassIdsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {})
                    .stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<Long, StudentSubmission> latestSubmissionByStudent(Long examId) {
        List<StudentSubmission> submissions = list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
        );
        return latestSubmissionByStudent(submissions);
    }

    private Map<Long, StudentSubmission> latestSubmissionByStudent(Collection<StudentSubmission> submissions) {
        Map<Long, StudentSubmission> result = new HashMap<>();
        for (StudentSubmission submission : submissions) {
            if (submission.getStudentId() == null) {
                continue;
            }
            result.merge(submission.getStudentId(), submission, this::latestSubmission);
        }
        return result;
    }

    private LocalDateTime submissionActivityTime(StudentSubmission submission) {
        if (submission == null) {
            return null;
        }
        return submission.getSubmitTime() != null ? submission.getSubmitTime() : submission.getStartTime();
    }

    private StudentSubmission latestSubmission(StudentSubmission left, StudentSubmission right) {
        int leftVersion = left.getVersion() == null ? 0 : left.getVersion();
        int rightVersion = right.getVersion() == null ? 0 : right.getVersion();
        if (leftVersion != rightVersion) {
            return leftVersion > rightVersion ? left : right;
        }
        LocalDateTime leftTime = left.getSubmitTime() == null ? left.getStartTime() : left.getSubmitTime();
        LocalDateTime rightTime = right.getSubmitTime() == null ? right.getStartTime() : right.getSubmitTime();
        if (leftTime != null && rightTime != null && !leftTime.equals(rightTime)) {
            return leftTime.isAfter(rightTime) ? left : right;
        }
        if (leftTime == null && rightTime != null) {
            return right;
        }
        if (leftTime != null) {
            return left;
        }
        long leftId = left.getId() == null ? 0L : left.getId();
        long rightId = right.getId() == null ? 0L : right.getId();
        return leftId >= rightId ? left : right;
    }

    private Map<Long, Map<Long, StudentAnswerDetail>> answerDetailsBySubmission(Collection<StudentSubmission> submissions) {
        List<Long> submissionIds = submissions.stream()
                .map(StudentSubmission::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        List<StudentAnswerDetail> details = answerDetailService.list(
                new LambdaQueryWrapper<StudentAnswerDetail>()
                        .in(StudentAnswerDetail::getSubmissionId, submissionIds)
        );
        Map<Long, Map<Long, StudentAnswerDetail>> result = new HashMap<>();
        for (StudentAnswerDetail detail : details) {
            result.computeIfAbsent(detail.getSubmissionId(), ignored -> new HashMap<>())
                    .put(detail.getQuestionId(), detail);
        }
        return result;
    }

    private ExamAnswerDetailExportRow buildAnswerExportRow(
            SysUser student,
            StudentSubmission submission,
            Integer questionNo,
            TaskQuestionRel rel,
            QuestionBank question,
            StudentAnswerDetail answer) {
        ExamAnswerDetailExportRow row = new ExamAnswerDetailExportRow();
        row.setStudentNo(student.getUsername());
        row.setStudentName(student.getRealName());
        row.setClassName(getStudentClassName(student));
        row.setStatus(statusLabel(submission == null ? "NOT_SUBMITTED" : submission.getStatus()));
        row.setTotalScore(submission == null ? null : submission.getTotalScoreGained());
        row.setSubmitTime(submission == null ? "" : formatTime(submission.getSubmitTime()));
        row.setQuestionNo(questionNo);
        if (rel != null) {
            row.setQuestionScore(rel.getScore());
        }
        if (question != null) {
            row.setQuestionType(typeLabel(question.getType()));
            row.setQuestionContent(question.getContent());
            row.setStandardAnswer(question.getStandardAnswer());
        }
        if (answer != null) {
            row.setStudentAnswer(answer.getStudentAnswer());
            row.setScoreGained(answer.getScoreGained());
            row.setResult(answerResultLabel(answer.getIsCorrect()));
        } else if (submission == null || "NOT_SUBMITTED".equals(submission.getStatus())) {
            row.setResult("未参加");
        } else if ("UN".equals(submission.getStatus())) {
            row.setResult("未交卷");
        } else {
            row.setResult("未作答");
        }
        return row;
    }

    private String getStudentClassName(SysUser student) {
        LinkedHashSet<String> classNames = new LinkedHashSet<>();
        if (student.getClassId() != null) {
            SysClass sysClass = sysClassMapper.selectById(student.getClassId());
            if (sysClass != null && sysClass.getClassName() != null) {
                classNames.add(sysClass.getClassName());
            }
        }
        classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .eq(ClassStudentRel::getStudentId, student.getId())
                        .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .forEach(rel -> {
                    SysClass sysClass = sysClassMapper.selectById(rel.getClassId());
                    if (sysClass != null && sysClass.getClassName() != null) {
                        classNames.add(sysClass.getClassName());
                    }
                });
        return String.join("、", classNames);
    }

    private String passLabel(BigDecimal score, BigDecimal passScore) {
        if (score == null) {
            return "-";
        }
        return score.compareTo(passScore) >= 0 ? "是" : "否";
    }

    private String statusLabel(String status) {
        if ("SUBMITTED".equals(status)) {
            return "已交卷";
        }
        if ("GRADED".equals(status)) {
            return "已批改";
        }
        if ("RETURNED".equals(status)) {
            return "已退回";
        }
        if ("UN".equals(status)) {
            return "答题中/未交卷";
        }
        if ("NOT_SUBMITTED".equals(status)) {
            return "未参加";
        }
        return status == null ? "" : status;
    }

    private String typeLabel(String type) {
        if ("SINGLE".equals(type)) {
            return "单选";
        }
        if ("MULTIPLE".equals(type)) {
            return "多选";
        }
        if ("JUDGE".equals(type)) {
            return "判断";
        }
        if ("SHORT".equals(type)) {
            return "简答";
        }
        if ("CODE".equals(type)) {
            return "编程";
        }
        return type == null ? "" : type;
    }

    private String answerResultLabel(Byte isCorrect) {
        if (isCorrect == null) {
            return "待批改";
        }
        if (isCorrect == 1) {
            return "正确";
        }
        if (isCorrect == 2) {
            return "部分正确";
        }
        return "错误";
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        String value = time.toString().replace('T', ' ');
        return value.length() > 19 ? value.substring(0, 19) : value;
    }

    private String formatTimeOrNull(LocalDateTime time) {
        return time == null ? null : formatTime(time);
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

    private List<SysUser> getStudentsInClasses(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        classIds = classIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        Set<Long> studentIds = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .in(ClassStudentRel::getClassId, classIds)
                        .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .stream()
                .map(ClassStudentRel::getStudentId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getIsDeleted, (byte) 0)
                        .in(SysUser::getClassId, classIds))
                .forEach(student -> {
                    if (student.getId() != null) {
                        studentIds.add(student.getId());
                    }
                });

        if (studentIds.isEmpty()) {
            return List.of();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getIsDeleted, (byte) 0)
                .in(SysUser::getId, studentIds));
    }
}
