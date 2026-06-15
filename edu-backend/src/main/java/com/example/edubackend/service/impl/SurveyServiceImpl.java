package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.*;
import com.example.edubackend.entity.*;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.*;
import com.example.edubackend.service.ISurveyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyServiceImpl implements ISurveyService {

    private static final String FIVE_POINT_OPTIONS_JSON = "[1,2,3,4,5]";
    private static final List<FixedSurveyQuestion> FIXED_SURVEY_QUESTIONS = List.of(
            new FixedSurveyQuestion("STAR", "您对这次授课讲师打几分？满分5分"),
            new FixedSurveyQuestion("STAR", "您对讲师授课的逻辑性打几分？满分5分"),
            new FixedSurveyQuestion("STAR", "您对讲师上课的趣味性打几分？满分5分"),
            new FixedSurveyQuestion("STAR", "讲师对学员关系度打几分？满分5分"),
            new FixedSurveyQuestion("STAR", "讲师理论与工程结合能力您打几分？满分5分"),
            new FixedSurveyQuestion("TEXT", "您对授课讲师的建议")
    );

    private final SurveyTaskMapper surveyTaskMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;
    private final SurveyRecordMapper surveyRecordMapper;
    private final SurveyAnswerDetailMapper surveyAnswerDetailMapper;
    private final SysUserMapper sysUserMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long createSurvey(CreateSurveyDTO dto, Long creatorId) {
        if (dto.getEndTime() == null) {
            throw new BusinessException(400, "截止时间不能为空");
        }
        validateFixedQuestionConfig(dto.getQuestions());
        assertCanTargetClasses(dto.getTargetClassIds(), creatorId);
        
        SurveyTask survey = new SurveyTask();
        survey.setTitle(dto.getTitle());
        survey.setEndTime(dto.getEndTime());
        survey.setCreatorId(creatorId);
        survey.setIsAnonymousRequired(dto.getIsAnonymousRequired() != null ? dto.getIsAnonymousRequired() : 0);
        survey.setStatus((byte) 0);
        
        SysUser creator = sysUserMapper.selectById(creatorId);
        if (creator != null && "TEACHER".equals(creator.getRole())) {
            survey.setTargetTeacherId(creatorId);
        } else if (dto.getTargetTeacherId() != null && dto.getTargetTeacherId() > 0) {
            survey.setTargetTeacherId(dto.getTargetTeacherId());
        } else {
            survey.setTargetTeacherId(null);
        }
        
        if (dto.getTargetClassIds() != null && !dto.getTargetClassIds().isEmpty()) {
            try {
                survey.setTargetClassIds(objectMapper.writeValueAsString(dto.getTargetClassIds()));
            } catch (Exception e) {
                throw new BusinessException(500, "Failed to serialize target class ids");
            }
        } else {
            survey.setTargetClassIds("[]");
        }
        
        surveyTaskMapper.insert(survey);
        
        for (int i = 0; i < FIXED_SURVEY_QUESTIONS.size(); i++) {
            FixedSurveyQuestion fixed = FIXED_SURVEY_QUESTIONS.get(i);
            SurveyQuestion question = new SurveyQuestion();
            question.setSurveyId(survey.getId());
            question.setType(fixed.type());
            question.setTitle(fixed.title());
            question.setOptionsJson(defaultFixedQuestionOptions(fixed.type()));
            question.setIsRequired((byte) 1);
            question.setSortOrder(i + 1);
            surveyQuestionMapper.insert(question);
        }
        
        return survey.getId();
    }

    private void validateFixedQuestionConfig(List<CreateSurveyDTO.QuestionConfig> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        if (questions.size() != FIXED_SURVEY_QUESTIONS.size()) {
            throw new BusinessException(400, "当前仅支持固定问卷题目");
        }
        for (int i = 0; i < FIXED_SURVEY_QUESTIONS.size(); i++) {
            FixedSurveyQuestion fixed = FIXED_SURVEY_QUESTIONS.get(i);
            CreateSurveyDTO.QuestionConfig question = questions.get(i);
            if (question == null
                    || !fixed.type().equalsIgnoreCase(question.getType())
                    || !fixed.title().equals(question.getTitle())) {
                throw new BusinessException(400, "当前仅支持固定问卷题目");
            }
        }
    }

    private String defaultFixedQuestionOptions(String type) {
        if ("STAR".equalsIgnoreCase(type) || "SCALE".equalsIgnoreCase(type)) {
            return FIVE_POINT_OPTIONS_JSON;
        }
        return "[]";
    }

    private String normalizeQuestionOptions(String type, String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            try {
                if ("SCALE".equalsIgnoreCase(type)) {
                    return objectMapper.writeValueAsString(List.of("非常不同意", "不同意", "一般", "同意", "非常同意"));
                }
                return "[]";
            } catch (Exception e) {
                throw new BusinessException(500, "Failed to serialize survey question options");
            }
        }

        String trimmed = optionsJson.trim();
        try {
            objectMapper.readTree(trimmed);
            return trimmed;
        } catch (Exception e) {
            throw new BusinessException(400, "题目选项配置必须是合法JSON");
        }
    }

    @Override
    @Transactional
    public void publishSurvey(Long surveyId) {
        SurveyTask survey = surveyTaskMapper.selectById(surveyId);
        if (survey == null) {
            throw new BusinessException(404, "问卷不存在");
        }
        assertCanManageSurvey(survey);
        Long questionCount = surveyQuestionMapper.selectCount(
                new LambdaQueryWrapper<SurveyQuestion>()
                        .eq(SurveyQuestion::getSurveyId, surveyId)
        );
        if (questionCount == null || questionCount == 0) {
            throw new BusinessException(400, "问卷没有题目，无法发布");
        }
        survey.setStatus((byte) 1);
        surveyTaskMapper.updateById(survey);
    }

    @Override
    @Transactional
    public void deleteSurvey(Long surveyId) {
        SurveyTask survey = surveyTaskMapper.selectById(surveyId);
        if (survey == null) {
            throw new BusinessException(404, "问卷不存在");
        }
        assertCanManageSurvey(survey);
        surveyTaskMapper.deleteById(surveyId);
    }

    @Override
    public List<SurveyListVO> getSurveyList(Long creatorId, String role) {
        LambdaQueryWrapper<SurveyTask> queryWrapper = new LambdaQueryWrapper<SurveyTask>()
                .orderByDesc(SurveyTask::getCreateTime);
        
        if (!"ADMIN".equals(role)) {
            queryWrapper.eq(SurveyTask::getCreatorId, creatorId);
        }
        
        List<SurveyTask> surveys = surveyTaskMapper.selectList(queryWrapper);

        List<Long> surveyIds = surveys.stream().map(SurveyTask::getId).toList();
        Map<Long, String> teacherNames = loadTeacherNames(surveys);
        Map<Long, Integer> questionCounts = countQuestionsBySurveyIds(surveyIds);
        Map<Long, Integer> submissionCounts = countRecordsBySurveyIds(surveyIds);

        List<SurveyListVO> result = new ArrayList<>();
        for (SurveyTask s : surveys) {
            SurveyListVO vo = new SurveyListVO();
            vo.setSurveyId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setEndTime(s.getEndTime());
            vo.setIsAnonymousRequired(s.getIsAnonymousRequired());
            vo.setTargetTeacherName(teacherNames.get(s.getTargetTeacherId()));
            vo.setStatus(s.getStatus() != null ? s.getStatus() : (byte) 0);
            vo.setTotalQuestions(questionCounts.getOrDefault(s.getId(), 0));
            vo.setTotalSubmissions(submissionCounts.getOrDefault(s.getId(), 0));
            
            result.add(vo);
        }
        
        return result;
    }

    @Override
    public List<StudentSurveyVO> getStudentSurveyList(Long studentId) {
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        Set<Long> studentClassIds = getStudentClassIds(student);
        if (studentClassIds.isEmpty()) {
            return List.of();
        }
        
        List<SurveyTask> surveys = surveyTaskMapper.selectList(
                new LambdaQueryWrapper<SurveyTask>()
                        .orderByDesc(SurveyTask::getCreateTime)
        );

        List<SurveyTask> availableSurveys = new ArrayList<>();
        for (SurveyTask s : surveys) {
            if (s.getIsDeleted() != null && s.getIsDeleted() == 1) {
                continue;
            }
            if (s.getStatus() == null || s.getStatus() != 1) {
                continue;
            }
            if (!isSurveyTargetedToClasses(s, studentClassIds)) {
                continue;
            }
            availableSurveys.add(s);
        }

        List<Long> surveyIds = availableSurveys.stream().map(SurveyTask::getId).toList();
        Map<Long, String> teacherNames = loadTeacherNames(availableSurveys);
        Map<Long, Integer> questionCounts = countQuestionsBySurveyIds(surveyIds);
        Set<Long> submittedSurveyIds = loadSubmittedSurveyIds(studentId, surveyIds);

        List<StudentSurveyVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (SurveyTask s : availableSurveys) {
            StudentSurveyVO vo = new StudentSurveyVO();
            vo.setSurveyId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setEndTime(s.getEndTime());
            vo.setIsAnonymousRequired(s.getIsAnonymousRequired());
            vo.setStatus(now.isBefore(s.getEndTime()) ? "ACTIVE" : "EXPIRED");
            vo.setTargetTeacherName(teacherNames.get(s.getTargetTeacherId()));
            vo.setAlreadySubmitted(submittedSurveyIds.contains(s.getId()));
            vo.setTotalQuestions(questionCounts.getOrDefault(s.getId(), 0));
            
            result.add(vo);
        }
        
        return result;
    }

    @Override
    public SurveyDetailVO getSurveyDetail(Long surveyId) {
        SurveyTask survey = surveyTaskMapper.selectById(surveyId);
        if (survey == null) {
            throw new BusinessException(404, "问卷不存在");
        }
        assertCanViewSurvey(survey);
        
        SurveyDetailVO vo = new SurveyDetailVO();
        vo.setSurveyId(survey.getId());
        vo.setTitle(survey.getTitle());
        vo.setIsAnonymousRequired(survey.getIsAnonymousRequired());
        
        if (survey.getTargetTeacherId() != null) {
            SysUser teacher = sysUserMapper.selectById(survey.getTargetTeacherId());
            if (teacher != null) {
                vo.setTargetTeacherName(teacher.getRealName());
            }
        }
        
        List<SurveyQuestion> questions = surveyQuestionMapper.selectList(
                new LambdaQueryWrapper<SurveyQuestion>()
                        .eq(SurveyQuestion::getSurveyId, surveyId)
                        .orderByAsc(SurveyQuestion::getSortOrder)
        );
        
        List<SurveyDetailVO.QuestionVO> questionVOs = questions.stream()
                .map(q -> {
                    SurveyDetailVO.QuestionVO qvo = new SurveyDetailVO.QuestionVO();
                    qvo.setQuestionId(q.getId());
                    qvo.setType(q.getType());
                    qvo.setTitle(q.getTitle());
                    qvo.setOptionsJson(q.getOptionsJson());
                    qvo.setIsRequired(q.getIsRequired());
                    qvo.setSortOrder(q.getSortOrder());
                    return qvo;
                })
                .toList();
        
        vo.setQuestions(questionVOs);
        return vo;
    }

    @Override
    @Transactional
    public void submitSurvey(Long studentId, SubmitSurveyDTO dto, String clientIp) {
        SurveyTask survey = surveyTaskMapper.selectById(dto.getSurveyId());
        if (survey == null) {
            throw new BusinessException(404, "问卷不存在");
        }
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null || !isSurveyTargetedToStudent(survey, student)) {
            throw new BusinessException(403, "无权提交该问卷");
        }
        if (survey.getStatus() == null || survey.getStatus() != 1) {
            throw new BusinessException(400, "问卷尚未发布");
        }
        
        if (LocalDateTime.now().isAfter(survey.getEndTime())) {
            throw new BusinessException(400, "问卷已截止");
        }
        
        SurveyRecord existingRecord = surveyRecordMapper.selectOne(
                new LambdaQueryWrapper<SurveyRecord>()
                        .eq(SurveyRecord::getSurveyId, dto.getSurveyId())
                        .eq(SurveyRecord::getStudentId, studentId)
        );
        if (existingRecord != null) {
            throw new BusinessException(400, "您已提交过此问卷");
        }
        
        List<SurveyQuestion> questions = surveyQuestionMapper.selectList(
                new LambdaQueryWrapper<SurveyQuestion>()
                        .eq(SurveyQuestion::getSurveyId, dto.getSurveyId())
        );
        
        Map<Long, SurveyQuestion> questionMap = new HashMap<>();
        for (SurveyQuestion q : questions) {
            questionMap.put(q.getId(), q);
        }
        
        Map<Long, String> answerMap = new HashMap<>();
        Set<Long> answeredQuestionIds = new HashSet<>();
        for (SubmitSurveyDTO.AnswerItem answer : dto.getAnswers()) {
            if (answer.getQuestionId() == null) {
                throw new BusinessException(400, "题目ID不能为空");
            }
            if (!answeredQuestionIds.add(answer.getQuestionId())) {
                throw new BusinessException(400, "同一题目不能重复提交答案: " + answer.getQuestionId());
            }
            SurveyQuestion question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                throw new BusinessException(400, "题目不存在: " + answer.getQuestionId());
            }
            answerMap.put(answer.getQuestionId(), answer.getAnswerValue());
        }

        for (SurveyQuestion question : questions) {
            String answerValue = answerMap.get(question.getId());
            if (question.getIsRequired() == 1 && (answerValue == null || answerValue.trim().isEmpty())) {
                throw new BusinessException(400, "必填题目未作答: " + question.getTitle());
            }
        }
        
        SurveyRecord record = new SurveyRecord();
        record.setSurveyId(dto.getSurveyId());
        record.setStudentId(studentId);
        record.setClientIp(clientIp);
        record.setSubmitTime(LocalDateTime.now());
        surveyRecordMapper.insert(record);
        
        for (SubmitSurveyDTO.AnswerItem answer : dto.getAnswers()) {
            SurveyAnswerDetail detail = new SurveyAnswerDetail();
            detail.setRecordId(record.getId());
            detail.setSurveyQuestionId(answer.getQuestionId());
            detail.setAnswerValue(answer.getAnswerValue());
            surveyAnswerDetailMapper.insert(detail);
        }
    }

    @Override
    public SurveyStatisticsVO getSurveyStatistics(Long surveyId) {
        SurveyTask survey = surveyTaskMapper.selectById(surveyId);
        if (survey == null) {
            throw new BusinessException(404, "问卷不存在");
        }
        assertCanManageSurvey(survey);
        
        SurveyStatisticsVO result = new SurveyStatisticsVO();
        result.setSurveyId(surveyId);
        result.setTitle(survey.getTitle());
        
        List<SurveyRecord> records = surveyRecordMapper.selectList(
                new LambdaQueryWrapper<SurveyRecord>()
                        .eq(SurveyRecord::getSurveyId, surveyId)
        );
        result.setTotalCount(records.size());
        
        Map<Long, SurveyStatisticsVO.QuestionStatisticsVO> questionStatsMap = new LinkedHashMap<>();
        
        List<SurveyQuestion> questions = surveyQuestionMapper.selectList(
                new LambdaQueryWrapper<SurveyQuestion>()
                        .eq(SurveyQuestion::getSurveyId, surveyId)
                        .orderByAsc(SurveyQuestion::getSortOrder)
        );
        
        for (SurveyQuestion q : questions) {
            SurveyStatisticsVO.QuestionStatisticsVO qStat = new SurveyStatisticsVO.QuestionStatisticsVO();
            qStat.setQuestionId(q.getId());
            qStat.setTitle(q.getTitle());
            qStat.setType(q.getType());
            qStat.setDistribution(new ArrayList<>());
            qStat.setTextAnswers(new ArrayList<>());
            questionStatsMap.put(q.getId(), qStat);
        }
        
        List<Long> recordIds = records.stream().map(SurveyRecord::getId).toList();
        if (recordIds.isEmpty()) {
            result.setQuestions(new ArrayList<>(questionStatsMap.values()));
            result.setNps(null);
            result.setRadarData(new ArrayList<>());
            return result;
        }
        
        List<SurveyAnswerDetail> allAnswers = surveyAnswerDetailMapper.selectList(
                new LambdaQueryWrapper<SurveyAnswerDetail>()
                        .in(SurveyAnswerDetail::getRecordId, recordIds)
        );
        
        Map<Long, Integer> answerCountByQuestion = new HashMap<>();
        Map<Long, Map<Integer, Integer>> distributionByQuestion = new HashMap<>();
        SurveyQuestion npsQuestion = questions.stream()
                .filter(q -> "NPS".equals(q.getType()))
                .findFirst()
                .orElse(null);
        int promoterCount = 0;
        int detractorCount = 0;
        int totalNpsAnswers = 0;

        for (SurveyAnswerDetail answer : allAnswers) {
            SurveyStatisticsVO.QuestionStatisticsVO qStat = questionStatsMap.get(answer.getSurveyQuestionId());
            if (qStat == null) continue;
            
            String value = answer.getAnswerValue();
            if (value == null || value.isEmpty()) continue;
            
            switch (qStat.getType()) {
                case "STAR":
                case "SCALE":
                    try {
                        java.math.BigDecimal score = new java.math.BigDecimal(value);
                        if (qStat.getAvgScore() == null) {
                            qStat.setAvgScore(new java.math.BigDecimal("0"));
                        }
                        qStat.setAvgScore(qStat.getAvgScore().add(score));
                        answerCountByQuestion.merge(qStat.getQuestionId(), 1, Integer::sum);
                        try {
                            int distributionValue = Integer.parseInt(value);
                            distributionByQuestion
                                    .computeIfAbsent(qStat.getQuestionId(), id -> new TreeMap<>())
                                    .merge(distributionValue, 1, Integer::sum);
                        } catch (NumberFormatException ignored) {}
                    } catch (NumberFormatException ignored) {}
                    break;
                case "TEXT":
                    qStat.getTextAnswers().add(value);
                    break;
                case "NPS":
                    try {
                        int npsScore = Integer.parseInt(value);
                        if (qStat.getAvgScore() == null) {
                            qStat.setAvgScore(new java.math.BigDecimal("0"));
                        }
                        qStat.setAvgScore(qStat.getAvgScore().add(new java.math.BigDecimal(npsScore)));
                        answerCountByQuestion.merge(qStat.getQuestionId(), 1, Integer::sum);
                        if (npsQuestion != null && qStat.getQuestionId().equals(npsQuestion.getId())) {
                            totalNpsAnswers++;
                            if (npsScore >= 9) {
                                promoterCount++;
                            } else if (npsScore <= 6) {
                                detractorCount++;
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                    break;
            }
        }
        
        for (SurveyStatisticsVO.QuestionStatisticsVO qStat : questionStatsMap.values()) {
            String type = qStat.getType();
            
            if ("STAR".equals(type) || "NPS".equals(type) || "SCALE".equals(type)) {
                int totalAnswers = answerCountByQuestion.getOrDefault(qStat.getQuestionId(), 0);
                
                if (totalAnswers > 0 && qStat.getAvgScore() != null) {
                    java.math.BigDecimal avgScore = qStat.getAvgScore()
                            .divide(new java.math.BigDecimal(totalAnswers), 2, java.math.RoundingMode.HALF_UP);
                    qStat.setAvgScore(avgScore);
                }
            }
            
            if ("STAR".equals(type) || "SCALE".equals(type)) {
                Map<Integer, Integer> distMap = distributionByQuestion.getOrDefault(qStat.getQuestionId(), Map.of());
                
                int totalForDist = distMap.values().stream().mapToInt(Integer::intValue).sum();
                List<SurveyStatisticsVO.DistributionVO> distList = new ArrayList<>();
                for (Map.Entry<Integer, Integer> entry : distMap.entrySet()) {
                    int val = entry.getKey();
                    int count = entry.getValue();
                    SurveyStatisticsVO.DistributionVO dist = new SurveyStatisticsVO.DistributionVO();
                    dist.setValue(val);
                    dist.setCount(count);
                    dist.setRate(totalForDist > 0 
                            ? new java.math.BigDecimal(count)
                                .divide(new java.math.BigDecimal(totalForDist), 4, java.math.RoundingMode.HALF_UP)
                            : java.math.BigDecimal.ZERO);
                    distList.add(dist);
                }
                qStat.setDistribution(distList);
            }
        }
        
        if (npsQuestion != null) {
            if (totalNpsAnswers > 0) {
                SurveyStatisticsVO.NpsVO nps = new SurveyStatisticsVO.NpsVO();
                java.math.BigDecimal promoterRate = new java.math.BigDecimal(promoterCount)
                        .divide(new java.math.BigDecimal(totalNpsAnswers), 4, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal detractorRate = new java.math.BigDecimal(detractorCount)
                        .divide(new java.math.BigDecimal(totalNpsAnswers), 4, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal npsScore = promoterRate.subtract(detractorRate)
                        .multiply(new java.math.BigDecimal("100"))
                        .setScale(0, java.math.RoundingMode.HALF_UP);
                
                nps.setPromoterRate(promoterRate);
                nps.setDetractorRate(detractorRate);
                nps.setScore(npsScore);
                result.setNps(nps);
            }
        }
        
        if (survey.getTargetTeacherId() != null) {
            List<SurveyStatisticsVO.RadarDataVO> radarData = new ArrayList<>();
            
            for (SurveyQuestion q : questions) {
                if ("STAR".equals(q.getType()) || "SCALE".equals(q.getType())) {
                    SurveyStatisticsVO.QuestionStatisticsVO qs = questionStatsMap.get(q.getId());
                    if (qs != null && qs.getAvgScore() != null) {
                        SurveyStatisticsVO.RadarDataVO radar = new SurveyStatisticsVO.RadarDataVO();
                        radar.setQuestionTitle(q.getTitle());
                        radar.setAvgScore(qs.getAvgScore());
                        radarData.add(radar);
                    }
                }
            }
            
            result.setRadarData(radarData);
        } else {
            result.setRadarData(new ArrayList<>());
        }
        
        result.setQuestions(new ArrayList<>(questionStatsMap.values()));
        return result;
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (targetClassIdsJson == null || targetClassIdsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("Failed to parse target class ids", e);
            return new ArrayList<>();
        }
    }

    private void assertCanViewSurvey(SurveyTask survey) {
        SysUser user = UserContext.getUser();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        if ("STUDENT".equals(user.getRole())) {
            SysUser student = sysUserMapper.selectById(user.getId());
            if (survey.getStatus() == null || survey.getStatus() != 1) {
                throw new BusinessException(403, "问卷尚未发布");
            }
            if (student == null || !isSurveyTargetedToStudent(survey, student)) {
                throw new BusinessException(403, "无权查看该问卷");
            }
            return;
        }
        assertCanManageSurvey(survey);
    }

    private void assertCanManageSurvey(SurveyTask survey) {
        SysUser user = UserContext.getUser();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        if (!"ADMIN".equals(user.getRole()) && !survey.getCreatorId().equals(user.getId())) {
            throw new BusinessException(403, "只能管理自己创建的问卷");
        }
    }

    private boolean isSurveyTargetedToStudent(SurveyTask survey, SysUser student) {
        Set<Long> studentClassIds = getStudentClassIds(student);
        return isSurveyTargetedToClasses(survey, studentClassIds);
    }

    private boolean isSurveyTargetedToClasses(SurveyTask survey, Set<Long> studentClassIds) {
        if (studentClassIds == null || studentClassIds.isEmpty()) {
            return false;
        }
        List<Long> targetClassIds = parseTargetClassIds(survey.getTargetClassIds());
        return targetClassIds.isEmpty() || targetClassIds.stream().anyMatch(studentClassIds::contains);
    }

    private Map<Long, String> loadTeacherNames(Collection<SurveyTask> surveys) {
        Set<Long> teacherIds = surveys.stream()
                .map(SurveyTask::getTargetTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, teacherIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (left, right) -> left));
    }

    private Map<Long, Integer> countQuestionsBySurveyIds(Collection<Long> surveyIds) {
        if (surveyIds == null || surveyIds.isEmpty()) {
            return Map.of();
        }
        return countBySurveyId(surveyQuestionMapper.selectMaps(new QueryWrapper<SurveyQuestion>()
                .select("survey_id", "COUNT(*) AS total_count")
                .in("survey_id", surveyIds)
                .groupBy("survey_id")));
    }

    private Map<Long, Integer> countRecordsBySurveyIds(Collection<Long> surveyIds) {
        if (surveyIds == null || surveyIds.isEmpty()) {
            return Map.of();
        }
        return countBySurveyId(surveyRecordMapper.selectMaps(new QueryWrapper<SurveyRecord>()
                .select("survey_id", "COUNT(*) AS total_count")
                .in("survey_id", surveyIds)
                .groupBy("survey_id")));
    }

    private Map<Long, Integer> countBySurveyId(List<Map<String, Object>> rows) {
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long surveyId = readLong(row, "survey_id", "surveyId", "SURVEY_ID");
            Long totalCount = readLong(row, "total_count", "totalCount", "TOTAL_COUNT");
            if (surveyId != null && totalCount != null) {
                result.put(surveyId, totalCount.intValue());
            }
        }
        return result;
    }

    private Set<Long> loadSubmittedSurveyIds(Long studentId, Collection<Long> surveyIds) {
        if (surveyIds == null || surveyIds.isEmpty()) {
            return Set.of();
        }
        return surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                        .select(SurveyRecord::getSurveyId)
                        .eq(SurveyRecord::getStudentId, studentId)
                        .in(SurveyRecord::getSurveyId, surveyIds))
                .stream()
                .map(SurveyRecord::getSurveyId)
                .collect(Collectors.toSet());
    }

    private Long readLong(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value != null) {
                try {
                    return Long.parseLong(value.toString());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Set<Long> getStudentClassIds(SysUser student) {
        if (student == null) {
            return Set.of();
        }
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

    private record FixedSurveyQuestion(String type, String title) {}
}
