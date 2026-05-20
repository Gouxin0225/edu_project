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
    private final IQuestionBankService questionBankService;
    private final ITaskQuestionRelService taskQuestionRelService;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final SysUserMapper sysUserMapper;
    private final SysClassMapper sysClassMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final ObjectMapper objectMapper;
    private final ExamSettingHelper examSettingHelper;

    @Override
    public List<SubmissionListVO> getSubmissionList(Long examId) {
        List<StudentSubmission> submissions = list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .orderByDesc(StudentSubmission::getSubmitTime)
        );

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
            vo.setSubmitTime(submission.getSubmitTime().toString().replace('T', ' ').substring(0, 19));
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

        if (!"SUBMITTED".equals(submission.getStatus())) {
            throw new BusinessException(400, "只能批改已提交的答卷");
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

            if (answer == null) {
                throw new BusinessException(404, "答题记录不存在: " + item.getQuestionId());
            }

            answer.setScoreGained(item.getScoreGained());
            Integer fullScore = questionScoreMap.get(item.getQuestionId());
            if (item.getScoreGained() != null && item.getScoreGained().compareTo(BigDecimal.ZERO) > 0) {
                if (fullScore != null && item.getScoreGained().compareTo(BigDecimal.valueOf(fullScore)) < 0) {
                    answer.setIsCorrect((byte) 2);
                } else {
                    answer.setIsCorrect((byte) 1);
                }
            } else {
                answer.setIsCorrect((byte) 0);
            }
            answerDetailService.updateById(answer);

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

        for (StudentSubmission s : submissions) {
            if (!"GRADED".equals(s.getStatus()) && !"SUBMITTED".equals(s.getStatus())) {
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
        List<QuestionBank> questions = questionBankService.listByIds(questionIds);
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
        AssessmentTask exam = assessmentTaskMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        if (!"EXAM".equals(exam.getType())) {
            throw new BusinessException(400, "任务不是考试");
        }
        BigDecimal passScore = BigDecimal.valueOf(examSettingHelper.getPassScore(exam));

        List<StudentSubmission> submissions = list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .isNotNull(StudentSubmission::getTotalScoreGained)
        );

        List<ScoreExportDTO> result = new ArrayList<>();
        for (StudentSubmission s : submissions) {
            ScoreExportDTO dto = new ScoreExportDTO();
            SysUser student = sysUserMapper.selectById(s.getStudentId());
            if (student != null) {
                dto.setStudentName(student.getRealName());
                if (student.getClassId() != null) {
                    SysClass sysClass = sysClassMapper.selectById(student.getClassId());
                    if (sysClass != null) {
                        dto.setClassName(sysClass.getClassName());
                    }
                }
            }
            dto.setTotalScore(s.getTotalScoreGained());
            dto.setIsPass(s.getTotalScoreGained() != null && 
                         s.getTotalScoreGained().compareTo(passScore) >= 0 ? "是" : "否");
            dto.setSubmitTime(s.getSubmitTime() != null ? s.getSubmitTime().toString() : "");
            result.add(dto);
        }
        return result;
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
                if (submission.getTotalScoreGained() != null) {
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

        Map<Long, StudentSubmission> submissionMap = new HashMap<>();
        for (StudentSubmission s : submissions) {
            submissionMap.put(s.getStudentId(), s);
        }

        List<ExamParticipationVO.StudentInfo> submitted = new ArrayList<>();
        List<ExamParticipationVO.StudentInfo> notSubmitted = new ArrayList<>();

        for (SysUser student : allStudents) {
            ExamParticipationVO.StudentInfo info = new ExamParticipationVO.StudentInfo();
            info.setStudentId(student.getId());
            info.setStudentName(student.getRealName());

            StudentSubmission submission = submissionMap.get(student.getId());
            if (submission != null) {
                info.setSubmitTime(submission.getSubmitTime() != null ? 
                        submission.getSubmitTime().toString().replace('T', ' ').substring(0, 19) : null);
                info.setStatus(submission.getStatus());
                submitted.add(info);
            } else {
                info.setStatus("NOT_SUBMITTED");
                notSubmitted.add(info);
            }
        }

        vo.setSubmittedStudents(submitted);
        vo.setNotSubmittedStudents(notSubmitted);

        return vo;
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
