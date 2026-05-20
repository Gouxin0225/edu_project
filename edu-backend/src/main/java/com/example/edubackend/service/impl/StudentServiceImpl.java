package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.dto.*;
import com.example.edubackend.entity.*;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.*;
import com.example.edubackend.service.IStudentService;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements IStudentService {

    private final AssessmentTaskMapper assessmentTaskMapper;
    private final StudentSubmissionMapper submissionMapper;
    private final StudentMistakeBookMapper mistakeBookMapper;
    private final QuestionBankMapper questionBankMapper;
    private final SysUserMapper sysUserMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final ObjectMapper objectMapper;
    private final ExamSettingHelper examSettingHelper;

    @Override
    public StudentDashboardVO getDashboard(Long studentId) {
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        
        StudentDashboardVO dashboard = new StudentDashboardVO();
        dashboard.setUpcomingTasks(new ArrayList<>());
        dashboard.setPendingTasks(new ArrayList<>());
        dashboard.setCompletedTasks(new ArrayList<>());
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysLater = now.plusDays(3);
        
        LambdaQueryWrapper<AssessmentTask> taskQuery = new LambdaQueryWrapper<AssessmentTask>()
                .in(AssessmentTask::getType, List.of("EXAM", "HOMEWORK"));
        
        List<AssessmentTask> allTasks = assessmentTaskMapper.selectList(taskQuery);
        
        for (AssessmentTask task : allTasks) {
            if (task.getIsDeleted() != null && task.getIsDeleted() == 1) continue;
            if (!isTaskTargetedToStudent(task, student)) continue;
            if ("EXAM".equals(task.getType()) && !examSettingHelper.isPublished(task)) continue;
            
            StudentSubmission submission = submissionMapper.selectOne(
                    new LambdaQueryWrapper<StudentSubmission>()
                            .eq(StudentSubmission::getTaskId, task.getId())
                            .eq(StudentSubmission::getStudentId, studentId)
                            .orderByDesc(StudentSubmission::getVersion)
                            .last("LIMIT 1")
            );
            
            StudentDashboardVO.TaskVO taskVO = new StudentDashboardVO.TaskVO();
            taskVO.setTaskId(task.getId());
            taskVO.setTaskType(task.getType());
            taskVO.setTitle(task.getTitle());
            taskVO.setDeadline(task.getEndTime());
            
            if (submission == null) {
                if (task.getEndTime().isBefore(now)) {
                    continue;
                } else if (task.getEndTime().isBefore(threeDaysLater)) {
                    taskVO.setStatus("UPCOMING");
                    dashboard.getUpcomingTasks().add(taskVO);
                } else {
                    taskVO.setStatus("PENDING");
                    dashboard.getPendingTasks().add(taskVO);
                }
            } else if ("GRADED".equals(submission.getStatus())) {
                taskVO.setStatus("COMPLETED");
                taskVO.setScoreGained(submission.getTotalScoreGained() != null ? 
                        submission.getTotalScoreGained().intValue() : null);
                dashboard.getCompletedTasks().add(taskVO);
            } else {
                taskVO.setStatus("SUBMITTED");
                dashboard.getPendingTasks().add(taskVO);
            }
        }
        
        dashboard.getUpcomingTasks().sort(Comparator.comparing(StudentDashboardVO.TaskVO::getDeadline));
        dashboard.getPendingTasks().sort(Comparator.comparing(StudentDashboardVO.TaskVO::getDeadline));
        dashboard.getCompletedTasks().sort(Comparator.comparing(StudentDashboardVO.TaskVO::getDeadline).reversed());
        
        return dashboard;
    }

    private boolean isTaskTargetedToStudent(AssessmentTask task, SysUser student) {
        Set<Long> classIds = getStudentClassIds(student);
        if (classIds.isEmpty()) {
            return false;
        }
        List<Long> targetClassIds = parseTargetClassIds(task.getTargetClassIds());
        return targetClassIds.isEmpty() || targetClassIds.stream().anyMatch(classIds::contains);
    }

    private Set<Long> getStudentClassIds(SysUser student) {
        Set<Long> classIds = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .eq(ClassStudentRel::getStudentId, student.getId())
                        .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .stream()
                .map(ClassStudentRel::getClassId)
                .collect(Collectors.toSet());
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

    @Override
    public List<MistakeVO> getMistakeList(Long studentId, String knowledgePoint, String questionType) {
        LambdaQueryWrapper<StudentMistakeBook> query = new LambdaQueryWrapper<StudentMistakeBook>()
                .eq(StudentMistakeBook::getStudentId, studentId)
                .eq(StudentMistakeBook::getIsMastered, 0);

        List<StudentMistakeBook> mistakes = mistakeBookMapper.selectList(query);

        if (mistakes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> questionIds = mistakes.stream()
                .map(StudentMistakeBook::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (questionIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<QuestionBank> questionQuery = new LambdaQueryWrapper<QuestionBank>()
                .in(QuestionBank::getId, questionIds);

        if (knowledgePoint != null && !knowledgePoint.isEmpty()) {
            questionQuery.like(QuestionBank::getKnowledgePoint, knowledgePoint);
        }
        if (questionType != null && !questionType.isEmpty()) {
            questionQuery.eq(QuestionBank::getType, questionType);
        }
        
        List<QuestionBank> questions = questionBankMapper.selectList(questionQuery);
        Map<Long, QuestionBank> questionMap = questions.stream()
                .collect(Collectors.toMap(QuestionBank::getId, q -> q));
        
        List<MistakeVO> result = new ArrayList<>();
        for (StudentMistakeBook mistake : mistakes) {
            QuestionBank question = questionMap.get(mistake.getQuestionId());
            if (question == null) continue;
            
            MistakeVO vo = new MistakeVO();
            vo.setMistakeId(mistake.getId());
            vo.setQuestionId(question.getId());
            vo.setCourseCategory(question.getCourseCategory());
            vo.setKnowledgePoint(question.getKnowledgePoint());
            vo.setQuestionType(question.getType());
            vo.setDifficulty(question.getDifficulty());
            vo.setContent(question.getContent());
            vo.setOptionsJson(question.getOptionsJson());
            vo.setStandardAnswer(question.getStandardAnswer());
            vo.setAnalysis(question.getAnalysis());
            vo.setWrongCount(mistake.getWrongCount());
            vo.setLastWrongTime(mistake.getLastWrongTime());
            vo.setIsMastered(mistake.getIsMastered());
            result.add(vo);
        }
        
        return result;
    }

    @Override
    @Transactional
    public void markMistakeMastered(Long studentId, Long questionId) {
        StudentMistakeBook mistake = mistakeBookMapper.selectOne(
                new LambdaQueryWrapper<StudentMistakeBook>()
                        .eq(StudentMistakeBook::getStudentId, studentId)
                        .eq(StudentMistakeBook::getQuestionId, questionId)
        );
        
        if (mistake == null) {
            throw new BusinessException(404, "错题记录不存在");
        }
        
        mistake.setIsMastered((byte) 1);
        mistakeBookMapper.updateById(mistake);
    }

    @Override
    public ChallengeVO challenge(Long studentId, ChallengeDTO dto) {
        int questionCount = dto.getQuestionCount() != null ? dto.getQuestionCount() : 10;
        if (questionCount <= 0) {
            throw new BusinessException(400, "题目数量必须大于0");
        }
        
        List<StudentMistakeBook> unmasteredMistakes = mistakeBookMapper.selectList(
                new LambdaQueryWrapper<StudentMistakeBook>()
                        .eq(StudentMistakeBook::getStudentId, studentId)
                        .eq(StudentMistakeBook::getIsMastered, 0)
        );
        
        if (unmasteredMistakes.isEmpty()) {
            throw new BusinessException(400, "暂无待攻克的错题");
        }
        
        List<Long> availableQuestionIds = unmasteredMistakes.stream()
                .map(StudentMistakeBook::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (availableQuestionIds.isEmpty()) {
            throw new BusinessException(400, "暂无待攻克的错题");
        }
        
        Collections.shuffle(availableQuestionIds);
        
        int actualCount = Math.min(questionCount, availableQuestionIds.size());
        List<Long> selectedIds = availableQuestionIds.subList(0, actualCount);
        
        List<QuestionBank> questions = questionBankMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                        .in(QuestionBank::getId, selectedIds)
        );
        
        String challengeId = "CHALLENGE-" + studentId + "-" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        ChallengeVO result = new ChallengeVO();
        result.setChallengeId(challengeId);
        
        List<ChallengeVO.ChallengeQuestionVO> challengeQuestions = questions.stream()
                .map(q -> {
                    ChallengeVO.ChallengeQuestionVO cq = new ChallengeVO.ChallengeQuestionVO();
                    cq.setQuestionId(q.getId());
                    cq.setCourseCategory(q.getCourseCategory());
                    cq.setKnowledgePoint(q.getKnowledgePoint());
                    cq.setType(q.getType());
                    cq.setDifficulty(q.getDifficulty());
                    cq.setContent(q.getContent());
                    cq.setOptionsJson(q.getOptionsJson());
                    cq.setStandardAnswer(q.getStandardAnswer());
                    cq.setAnalysis(q.getAnalysis());
                    return cq;
                })
                .collect(Collectors.toList());
        
        result.setQuestions(challengeQuestions);
        return result;
    }
}
