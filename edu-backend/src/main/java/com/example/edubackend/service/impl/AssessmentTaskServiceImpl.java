package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.CreateExamDTO;
import com.example.edubackend.dto.ExamStatisticsVO;
import com.example.edubackend.dto.ExamVO;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.TaskQuestionRel;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.TaskQuestionRelMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.service.IAssessmentTaskService;
import com.example.edubackend.service.IQuestionBankService;
import com.example.edubackend.service.IStudentSubmissionService;
import com.example.edubackend.service.ITaskQuestionRelService;
import com.example.edubackend.util.ExamSettingHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentTaskServiceImpl extends ServiceImpl<AssessmentTaskMapper, AssessmentTask> implements IAssessmentTaskService {

    private final ITaskQuestionRelService taskQuestionRelService;
    private final IQuestionBankService questionBankService;
    private final IStudentSubmissionService submissionService;
    private final ObjectMapper objectMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final ExamSettingHelper examSettingHelper;

    private static final Map<String, Double> DIFFICULTY_WEIGHT = Map.of(
            "EASY", 1.0,
            "MEDIUM", 1.5,
            "HARD", 2.0
    );

    private static final Set<String> OBJECTIVE_TYPES = Set.of("SINGLE", "MULTIPLE", "JUDGE");
    private static final double OBJECTIVE_WEIGHT = 1.0;
    private static final double SUBJECTIVE_WEIGHT = 1.5;

    @Override
    @Transactional
    public ExamVO createExam(CreateExamDTO dto, Long creatorId) {
        assertCanTargetClasses(dto.getTargetClassIds(), creatorId);
        AssessmentTask exam = new AssessmentTask();
        exam.setTitle(dto.getTitle());
        exam.setType("EXAM");
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setTotalScore(dto.getTotalScore() != null && dto.getTotalScore() > 0 ? dto.getTotalScore() : 100);
        exam.setCreatorId(creatorId);
        
        if (dto.getTargetClassIds() != null) {
            try {
                exam.setTargetClassIds(objectMapper.writeValueAsString(dto.getTargetClassIds()));
            } catch (Exception e) {
                exam.setTargetClassIds("[]");
            }
        } else {
            exam.setTargetClassIds("[]");
        }
        
        exam.setSettingJson(examSettingHelper.buildSettingJson(
                dto.getSettingJson(), dto.getDuration(), dto.getPassScore(), false));

        baseMapper.insert(exam);

        return toExamVO(exam);
    }

    @Override
    @Transactional
    public void addQuestions(Long examId, List<Long> questionIds) {
        AssessmentTask exam = baseMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }

        List<TaskQuestionRel> existingRels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>().eq(TaskQuestionRel::getTaskId, examId)
        );
        int maxSortOrder = existingRels.stream()
                .mapToInt(TaskQuestionRel::getSortOrder)
                .max()
                .orElse(0);

        for (Long questionId : questionIds) {
            QuestionBank question = questionBankService.getById(questionId);
            if (question == null) {
                throw new BusinessException(404, "题目不存在: " + questionId);
            }

            boolean exists = existingRels.stream()
                    .anyMatch(r -> r.getQuestionId().equals(questionId));
            if (exists) {
                continue;
            }

            TaskQuestionRel rel = new TaskQuestionRel();
            rel.setTaskId(examId);
            rel.setQuestionId(questionId);
            rel.setScore(0);
            rel.setSortOrder(++maxSortOrder);
            taskQuestionRelService.save(rel);
        }
    }

    @Override
    @Transactional
    public void removeQuestion(Long examId, Long questionId) {
        taskQuestionRelService.remove(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
                        .eq(TaskQuestionRel::getQuestionId, questionId)
        );
    }

    @Override
    @Transactional
    public void autoScore(Long examId) {
        AssessmentTask exam = baseMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }

        List<TaskQuestionRel> rels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
                        .orderByAsc(TaskQuestionRel::getSortOrder)
        );

        if (rels.isEmpty()) {
            throw new BusinessException(400, "试卷没有题目");
        }

        List<Long> questionIds = rels.stream()
                .map(TaskQuestionRel::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            throw new BusinessException(400, "试卷题目不存在或已删除");
        }
        List<QuestionBank> questions = questionBankService.listByIds(questionIds);
        Map<Long, QuestionBank> questionMap = new HashMap<>();
        for (QuestionBank q : questions) {
            if (q.getId() != null) {
                questionMap.put(q.getId(), q);
            }
        }

        double totalWeight = 0;
        List<TaskQuestionWithWeight> scoredRels = new ArrayList<>();

        for (TaskQuestionRel rel : rels) {
            QuestionBank question = questionMap.get(rel.getQuestionId());
            if (question == null) continue;

            double difficultyWeight = DIFFICULTY_WEIGHT.getOrDefault(question.getDifficulty(), 1.0);
            double typeWeight = OBJECTIVE_TYPES.contains(question.getType())
                    ? OBJECTIVE_WEIGHT : SUBJECTIVE_WEIGHT;
            double weight = difficultyWeight * typeWeight;

            totalWeight += weight;
            scoredRels.add(new TaskQuestionWithWeight(rel, weight, question.getDifficulty()));
        }

        if (scoredRels.isEmpty()) {
            throw new BusinessException(400, "试卷题目不存在或已删除");
        }

        int totalScore = 100;
        int allocatedScore = 0;
        int hardestIndex = -1;
        double hardestWeight = 0;

        for (int i = 0; i < scoredRels.size(); i++) {
            TaskQuestionWithWeight item = scoredRels.get(i);
            if (item.difficulty.equals("HARD") && item.weight > hardestWeight) {
                hardestWeight = item.weight;
                hardestIndex = i;
            }
        }

        if (hardestIndex == -1) {
            hardestIndex = scoredRels.size() - 1;
        }

        for (int i = 0; i < scoredRels.size(); i++) {
            TaskQuestionWithWeight item = scoredRels.get(i);
            int score;

            if (i == hardestIndex) {
                score = totalScore - allocatedScore;
            } else {
                score = (int) ((item.weight / totalWeight) * totalScore);
                allocatedScore += score;
            }

            item.rel.setScore(score);
            taskQuestionRelService.updateById(item.rel);
        }

        exam.setTotalScore(totalScore);
        baseMapper.updateById(exam);
    }

    @Override
    @Transactional
    public void updateQuestionScore(Long examId, Long questionId, Integer score) {
        TaskQuestionRel rel = taskQuestionRelService.getOne(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
                        .eq(TaskQuestionRel::getQuestionId, questionId)
        );

        if (rel == null) {
            throw new BusinessException(404, "题目不在试卷中");
        }

        rel.setScore(score);
        taskQuestionRelService.updateById(rel);
    }

    @Override
    @Transactional
    public void publishExam(Long examId) {
        AssessmentTask exam = baseMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }

        List<TaskQuestionRel> rels = taskQuestionRelService.list(
                new LambdaQueryWrapper<TaskQuestionRel>()
                        .eq(TaskQuestionRel::getTaskId, examId)
        );

        if (rels.isEmpty()) {
            throw new BusinessException(400, "试卷没有题目，无法发布");
        }

        int totalScore = rels.stream().mapToInt(TaskQuestionRel::getScore).sum();
        if (totalScore != 100) {
            throw new BusinessException(400, "总分必须等于100才能发布，当前总分: " + totalScore);
        }

        exam.setTotalScore(totalScore);
        exam.setSettingJson(examSettingHelper.markPublished(exam.getSettingJson()));
        baseMapper.updateById(exam);
    }

    @Override
    public ExamStatisticsVO getStatistics(Long examId) {
        AssessmentTask exam = baseMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }

        List<StudentSubmission> submissions = submissionService.list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
                        .isNotNull(StudentSubmission::getTotalScoreGained)
        );

        if (submissions.isEmpty()) {
            ExamStatisticsVO vo = new ExamStatisticsVO();
            vo.setAverageScore(java.math.BigDecimal.ZERO);
            vo.setHighestScore(java.math.BigDecimal.ZERO);
            vo.setLowestScore(java.math.BigDecimal.ZERO);
            vo.setPassRate(java.math.BigDecimal.ZERO);
            vo.setTotalStudents(0);
            vo.setPassedStudents(0);
            vo.setScoreDistribution(new java.util.HashMap<>());
            return vo;
        }

        int totalCount = submissions.size();
        int passedCount = 0;
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        java.math.BigDecimal max = null;
        java.math.BigDecimal min = null;

        java.util.Map<String, Integer> distribution = new java.util.HashMap<>();
        distribution.put("0-60", 0);
        distribution.put("60-70", 0);
        distribution.put("70-80", 0);
        distribution.put("80-90", 0);
        distribution.put("90-100", 0);

        for (StudentSubmission s : submissions) {
            java.math.BigDecimal score = s.getTotalScoreGained();
            if (score == null) continue;

            sum = sum.add(score);

            if (max == null || score.compareTo(max) > 0) {
                max = score;
            }
            if (min == null || score.compareTo(min) < 0) {
                min = score;
            }

            if (score.compareTo(new java.math.BigDecimal(examSettingHelper.getPassScore(exam))) >= 0) {
                passedCount++;
            }

            int intScore = score.intValue();
            if (intScore < 60) {
                distribution.put("0-60", distribution.get("0-60") + 1);
            } else if (intScore < 70) {
                distribution.put("60-70", distribution.get("60-70") + 1);
            } else if (intScore < 80) {
                distribution.put("70-80", distribution.get("70-80") + 1);
            } else if (intScore < 90) {
                distribution.put("80-90", distribution.get("80-90") + 1);
            } else {
                distribution.put("90-100", distribution.get("90-100") + 1);
            }
        }

        ExamStatisticsVO vo = new ExamStatisticsVO();
        vo.setTotalStudents(totalCount);
        vo.setPassedStudents(passedCount);
        vo.setHighestScore(max != null ? max : java.math.BigDecimal.ZERO);
        vo.setLowestScore(min != null ? min : java.math.BigDecimal.ZERO);
        vo.setAverageScore(sum.divide(new java.math.BigDecimal(totalCount), 1, java.math.RoundingMode.HALF_UP));
        vo.setPassRate(new java.math.BigDecimal(passedCount)
                .multiply(new java.math.BigDecimal("100"))
                .divide(new java.math.BigDecimal(totalCount), 1, java.math.RoundingMode.HALF_UP));
        vo.setScoreDistribution(distribution);

        return vo;
    }

    private ExamVO toExamVO(AssessmentTask exam) {
        ExamVO vo = new ExamVO();
        vo.setId(exam.getId());
        vo.setTitle(exam.getTitle());
        vo.setType(exam.getType());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setTotalScore(exam.getTotalScore());
        vo.setDuration(examSettingHelper.getDuration(exam));
        vo.setPassScore(examSettingHelper.getPassScore(exam));
        vo.setCreatorId(exam.getCreatorId());
        vo.setStatus(examSettingHelper.calculateStatus(exam));
        vo.setCreateTime(exam.getCreateTime());

        if (exam.getTargetClassIds() != null) {
            try {
                vo.setTargetClassIds(objectMapper.readValue(exam.getTargetClassIds(),
                        new TypeReference<List<Long>>() {}));
            } catch (Exception e) {
                vo.setTargetClassIds(new ArrayList<>());
            }
        }

        return vo;
    }

    private void assertCanTargetClasses(List<Long> targetClassIds, Long creatorId) {
        if ("ADMIN".equals(UserContext.getUser().getRole())) {
            return;
        }
        if (targetClassIds == null || targetClassIds.isEmpty()) {
            throw new BusinessException(400, "目标班级不能为空");
        }
        List<Long> allowedClassIds = teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>().eq(TeacherClassRel::getTeacherId, creatorId)
        ).stream().map(TeacherClassRel::getClassId).toList();
        if (!allowedClassIds.containsAll(targetClassIds)) {
            throw new BusinessException(403, "只能发布到自己负责的班级");
        }
    }

    private static class TaskQuestionWithWeight {
        TaskQuestionRel rel;
        double weight;
        String difficulty;

        TaskQuestionWithWeight(TaskQuestionRel rel, double weight, String difficulty) {
            this.rel = rel;
            this.weight = weight;
            this.difficulty = difficulty;
        }
    }
}
