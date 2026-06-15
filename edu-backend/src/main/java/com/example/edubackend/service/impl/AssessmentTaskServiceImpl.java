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
import com.example.edubackend.entity.SurveyTask;
import com.example.edubackend.entity.TaskQuestionRel;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.QuestionBankMapper;
import com.example.edubackend.mapper.SurveyTaskMapper;
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

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentTaskServiceImpl extends ServiceImpl<AssessmentTaskMapper, AssessmentTask> implements IAssessmentTaskService {

    private final ITaskQuestionRelService taskQuestionRelService;
    private final IQuestionBankService questionBankService;
    private final IStudentSubmissionService submissionService;
    private final ObjectMapper objectMapper;
    private final QuestionBankMapper questionBankMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SurveyTaskMapper surveyTaskMapper;
    private final ExamSettingHelper examSettingHelper;

    private static final Map<String, Double> DIFFICULTY_WEIGHT = Map.of(
            "EASY", 0.9,
            "MEDIUM", 1.0,
            "HARD", 1.25
    );

    private static final Map<String, Double> QUESTION_TYPE_WEIGHT = Map.of(
            "SINGLE", 1.0,
            "JUDGE", 1.0,
            "MULTIPLE", 1.25,
            "SHORT", 1.8,
            "CODE", 2.2
    );

    private static final Map<String, Integer> QUESTION_TYPE_PRIORITY = Map.of(
            "SINGLE", 1,
            "JUDGE", 1,
            "MULTIPLE", 2,
            "SHORT", 3,
            "CODE", 4
    );

    private static final Map<String, Integer> DIFFICULTY_PRIORITY = Map.of(
            "EASY", 1,
            "MEDIUM", 2,
            "HARD", 3
    );

    @Override
    @Transactional
    public ExamVO createExam(CreateExamDTO dto, Long creatorId) {
        assertCanTargetClasses(dto.getTargetClassIds(), creatorId);
        validateRequiredSurveySelection(dto.getRequireSurveyBeforeSubmit(), dto.getRequiredSurveyId(), creatorId, dto.getEndTime());
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
                dto.getSettingJson(),
                dto.getDuration(),
                dto.getPassScore(),
                false,
                dto.getRequireSurveyBeforeSubmit(),
                dto.getRequiredSurveyId()));

        baseMapper.insert(exam);

        return toExamVO(exam);
    }

    @Override
    @Transactional
    public ExamVO updateExam(Long examId, CreateExamDTO dto, Long userId) {
        AssessmentTask exam = baseMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        if (!"EXAM".equals(exam.getType())) {
            throw new BusinessException(400, "任务不是考试");
        }
        boolean wasPublished = examSettingHelper.isPublished(exam);
        assertExamEditable(examId);

        assertCanTargetClasses(dto.getTargetClassIds(), userId);
        validateRequiredSurveySelection(dto.getRequireSurveyBeforeSubmit(), dto.getRequiredSurveyId(), userId, dto.getEndTime());
        exam.setTitle(dto.getTitle());
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setTotalScore(dto.getTotalScore() != null && dto.getTotalScore() > 0 ? dto.getTotalScore() : 100);
        try {
            exam.setTargetClassIds(objectMapper.writeValueAsString(
                    dto.getTargetClassIds() == null ? List.of() : dto.getTargetClassIds()));
        } catch (Exception e) {
            exam.setTargetClassIds("[]");
        }
        exam.setSettingJson(examSettingHelper.buildSettingJson(
                dto.getSettingJson(),
                dto.getDuration(),
                dto.getPassScore(),
                wasPublished,
                dto.getRequireSurveyBeforeSubmit(),
                dto.getRequiredSurveyId()));
        baseMapper.updateById(exam);
        return toExamVO(exam);
    }

    @Override
    @Transactional
    public void addQuestions(Long examId, List<Long> questionIds) {
        AssessmentTask exam = baseMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        assertExamEditable(examId);

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
        assertExamEditable(examId);
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
        assertExamEditable(examId);

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
        List<QuestionBank> questions = questionBankMapper.selectBatchIdsIncludingDeleted(questionIds);
        Map<Long, QuestionBank> questionMap = new HashMap<>();
        for (QuestionBank q : questions) {
            if (q.getId() != null) {
                questionMap.put(q.getId(), q);
            }
        }

        List<TaskQuestionWithWeight> scoredRels = new ArrayList<>();

        for (TaskQuestionRel rel : rels) {
            QuestionBank question = questionMap.get(rel.getQuestionId());
            if (question == null) continue;

            double difficultyWeight = DIFFICULTY_WEIGHT.getOrDefault(question.getDifficulty(), 1.0);
            double typeWeight = QUESTION_TYPE_WEIGHT.getOrDefault(question.getType(), 1.0);
            double weight = difficultyWeight * typeWeight;

            scoredRels.add(new TaskQuestionWithWeight(
                    rel,
                    weight,
                    QUESTION_TYPE_PRIORITY.getOrDefault(question.getType(), 1),
                    DIFFICULTY_PRIORITY.getOrDefault(question.getDifficulty(), 1),
                    rel.getSortOrder() == null ? 0 : rel.getSortOrder()));
        }

        if (scoredRels.isEmpty()) {
            throw new BusinessException(400, "试卷题目不存在或已删除");
        }

        int totalScore = 100;
        List<Integer> scores = allocateScores(scoredRels, totalScore);
        for (int i = 0; i < scoredRels.size(); i++) {
            TaskQuestionWithWeight item = scoredRels.get(i);
            item.rel.setScore(scores.get(i));
            taskQuestionRelService.updateById(item.rel);
        }

        exam.setTotalScore(totalScore);
        baseMapper.updateById(exam);
    }

    @Override
    @Transactional
    public void updateQuestionScore(Long examId, Long questionId, Integer score) {
        assertExamEditable(examId);
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
        validateRequiredSurveyForPublish(exam);

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
        vo.setRequireSurveyBeforeSubmit(examSettingHelper.isSurveyRequiredBeforeSubmit(exam));
        vo.setRequiredSurveyId(examSettingHelper.getRequiredSurveyId(exam));
        vo.setCreatorId(exam.getCreatorId());
        vo.setStatus(examSettingHelper.calculateStatus(exam));
        vo.setEditable(!hasSubmissions(exam.getId()));
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

    private void assertExamEditable(Long examId) {
        if (hasSubmissions(examId)) {
            throw new BusinessException(400, "已有学生开始或提交考试，不能再编辑试卷");
        }
    }

    private boolean hasSubmissions(Long examId) {
        Long count = submissionService.count(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, examId)
        );
        return count != null && count > 0;
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

    private void validateRequiredSurveySelection(Boolean requireSurveyBeforeSubmit, Long requiredSurveyId, Long userId, LocalDateTime examEndTime) {
        if (!Boolean.TRUE.equals(requireSurveyBeforeSubmit)) {
            return;
        }
        SurveyTask survey = validateRequiredSurveyExists(requiredSurveyId);
        if (!"ADMIN".equals(UserContext.getUser().getRole()) && !Objects.equals(survey.getCreatorId(), userId)) {
            throw new BusinessException(403, "只能绑定自己创建的问卷");
        }
        validateRequiredSurveyEndTime(survey, examEndTime);
    }

    private void validateRequiredSurveyForPublish(AssessmentTask exam) {
        if (!examSettingHelper.isSurveyRequiredBeforeSubmit(exam)) {
            return;
        }
        SurveyTask survey = validateRequiredSurveyExists(examSettingHelper.getRequiredSurveyId(exam));
        if (survey.getStatus() == null || survey.getStatus() != 1) {
            throw new BusinessException(400, "必填问卷尚未发布，无法发布考试");
        }
        validateRequiredSurveyEndTime(survey, exam.getEndTime());
        List<Long> examClassIds = parseTargetClassIds(exam.getTargetClassIds());
        List<Long> surveyClassIds = parseTargetClassIds(survey.getTargetClassIds());
        if (!surveyClassIds.isEmpty() && (examClassIds.isEmpty() || !surveyClassIds.containsAll(examClassIds))) {
            throw new BusinessException(400, "必填问卷的目标班级必须覆盖考试目标班级");
        }
    }

    private SurveyTask validateRequiredSurveyExists(Long requiredSurveyId) {
        if (requiredSurveyId == null || requiredSurveyId <= 0) {
            throw new BusinessException(400, "启用交卷前问卷时必须选择问卷");
        }
        SurveyTask survey = surveyTaskMapper.selectById(requiredSurveyId);
        if (survey == null) {
            throw new BusinessException(404, "绑定的问卷不存在");
        }
        return survey;
    }

    private void validateRequiredSurveyEndTime(SurveyTask survey, LocalDateTime examEndTime) {
        if (examEndTime == null || survey.getEndTime() == null) {
            return;
        }
        if (survey.getEndTime().isBefore(examEndTime)) {
            throw new BusinessException(400, "必填问卷截止时间不能早于考试截止时间");
        }
    }

    private List<Long> parseTargetClassIds(String rawClassIds) {
        if (rawClassIds == null || rawClassIds.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawClassIds, new TypeReference<List<Long>>() {})
                    .stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Integer> allocateScores(List<TaskQuestionWithWeight> questions, int totalScore) {
        int questionCount = questions.size();
        int minimumScore = questionCount <= totalScore ? 1 : 0;
        int distributableScore = totalScore - minimumScore * questionCount;
        double totalWeight = questions.stream().mapToDouble(item -> item.weight).sum();
        if (totalWeight <= 0) {
            totalWeight = questionCount;
        }

        int allocatedScore = 0;
        List<ScoreAllocation> allocations = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            TaskQuestionWithWeight item = questions.get(i);
            double exactScore = minimumScore + distributableScore * (item.weight / totalWeight);
            int score = (int) Math.floor(exactScore);
            allocatedScore += score;
            allocations.add(new ScoreAllocation(
                    i,
                    score,
                    exactScore - score,
                    item.weight,
                    item.typePriority,
                    item.difficultyPriority,
                    item.sortOrder));
        }

        int remainingScore = totalScore - allocatedScore;
        allocations.sort(Comparator
                .comparingDouble((ScoreAllocation item) -> item.remainder).reversed()
                .thenComparing((ScoreAllocation item) -> item.difficultyPriority, Comparator.reverseOrder())
                .thenComparing((ScoreAllocation item) -> item.typePriority, Comparator.reverseOrder())
                .thenComparing((left, right) -> Double.compare(right.weight, left.weight))
                .thenComparing(item -> item.sortOrder));
        for (int i = 0; i < remainingScore; i++) {
            allocations.get(i % allocations.size()).score += 1;
        }

        allocations.sort(Comparator.comparingInt(item -> item.index));
        return allocations.stream().map(item -> item.score).toList();
    }

    private static class TaskQuestionWithWeight {
        TaskQuestionRel rel;
        double weight;
        int typePriority;
        int difficultyPriority;
        int sortOrder;

        TaskQuestionWithWeight(TaskQuestionRel rel, double weight, int typePriority, int difficultyPriority, int sortOrder) {
            this.rel = rel;
            this.weight = weight;
            this.typePriority = typePriority;
            this.difficultyPriority = difficultyPriority;
            this.sortOrder = sortOrder;
        }
    }

    private static class ScoreAllocation {
        int index;
        int score;
        double remainder;
        double weight;
        int typePriority;
        int difficultyPriority;
        int sortOrder;

        ScoreAllocation(int index, int score, double remainder, double weight, int typePriority, int difficultyPriority, int sortOrder) {
            this.index = index;
            this.score = score;
            this.remainder = remainder;
            this.weight = weight;
            this.typePriority = typePriority;
            this.difficultyPriority = difficultyPriority;
            this.sortOrder = sortOrder;
        }
    }
}
