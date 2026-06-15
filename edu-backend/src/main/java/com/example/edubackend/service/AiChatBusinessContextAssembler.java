package com.example.edubackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.AiChatContextResult;
import com.example.edubackend.dto.AiChatMessageDTO;
import com.example.edubackend.dto.MistakeVO;
import com.example.edubackend.entity.AiChatSession;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.HomeworkSubmissionContent;
import com.example.edubackend.entity.QuestionBank;
import com.example.edubackend.entity.QuestionPaperTemplate;
import com.example.edubackend.entity.StudentAnswerDetail;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.SysClass;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.mapper.HomeworkSubmissionContentMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiChatBusinessContextAssembler {

    private static final int MAX_CONTEXT_CHARS = 12000;
    private static final int SECTION_LIMIT = 3;
    private static final int TASK_LIMIT = 5;

    private final IQuestionBankService questionBankService;
    private final IStudentService studentService;
    private final IStudentSubmissionService submissionService;
    private final IStudentAnswerDetailService answerDetailService;
    private final IAssessmentTaskService assessmentTaskService;
    private final IQuestionPaperTemplateService templateService;
    private final IAiKnowledgeRetrievalService knowledgeRetrievalService;
    private final HomeworkSubmissionContentMapper homeworkContentMapper;
    private final SysUserMapper sysUserMapper;
    private final SysClassMapper sysClassMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final ObjectMapper objectMapper;

    public String buildContext(AiChatMessageDTO dto, SysUser user) {
        return buildContextResult(dto, null, user).getContext();
    }

    public String buildContext(AiChatMessageDTO dto, AiChatSession session, SysUser user) {
        return buildContextResult(dto, session, user).getContext();
    }

    public AiChatContextResult buildContextResult(AiChatMessageDTO dto, SysUser user) {
        return buildContextResult(dto, null, user);
    }

    public AiChatContextResult buildContextResult(AiChatMessageDTO dto, AiChatSession session, SysUser user) {
        if (user == null || !StringUtils.hasText(user.getRole())) {
            return new AiChatContextResult("", inferIntent(dto), normalizeResponseSource(dto), "未识别到登录用户，未使用平台数据或知识库。");
        }

        SearchScope scope = SearchScope.from(dto, session);
        String intent = inferIntent(dto);
        String source = normalizeResponseSource(dto);
        StringBuilder context = new StringBuilder();
        boolean usedPlatform = false;
        boolean usedKnowledge = false;
        appendLine(context, "以下是平台业务数据检索结果，仅作为回答参考；若数据不足，请明确说明不能从平台记录中确认。");

        if ("GENERAL_AI".equals(source) || ("AUTO".equals(source) && "GENERAL".equals(intent) && !hasLearningScope(scope))) {
            return new AiChatContextResult("", intent, source, "回答来源：通用 AI；未检索平台数据或知识库，平台记录相关内容无法确认。");
        }

        if ("KNOWLEDGE_ONLY".equals(source) || ("AUTO".equals(source) && shouldUseKnowledgeOnly(intent))) {
            usedKnowledge = appendAndCheck(context, () -> appendKnowledgeContext(context, scope, user));
            return buildResult(context, intent, source, usedPlatform, usedKnowledge);
        }

        if ("PLATFORM_ONLY".equals(source) || ("AUTO".equals(source) && "PLATFORM_QUERY".equals(intent))) {
            usedPlatform = appendPlatformSections(context, scope, user);
            return buildResult(context, intent, source, usedPlatform, usedKnowledge);
        }

        usedKnowledge = appendAndCheck(context, () -> appendKnowledgeContext(context, scope, user));
        if ("QUESTION_GEN".equals(intent) || "QUESTION_REVIEW".equals(intent)) {
            if (isTeacherLike(user.getRole())) {
                usedPlatform = appendAndCheck(context, () -> appendQuestionReferences(context, scope, true, user.getId())) || usedPlatform;
            }
        } else if ("LEARN".equals(intent) && "STUDENT".equalsIgnoreCase(user.getRole())) {
            usedPlatform = appendAndCheck(context, () -> appendStudentMistakes(context, scope, user.getId())) || usedPlatform;
        }

        return buildResult(context, intent, source, usedPlatform, usedKnowledge);
    }

    private boolean appendPlatformSections(StringBuilder context, SearchScope scope, SysUser user) {
        return appendAndCheck(context, () -> {
            appendPlatformAccessContext(context, scope, user);
            if (isTeacherLike(user.getRole())) {
                appendTeacherContext(context, scope, user);
            } else if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                appendStudentContext(context, scope, user);
            }
        });
    }

    private boolean appendAndCheck(StringBuilder context, Runnable appender) {
        int before = context.length();
        appender.run();
        return context.length() > before;
    }

    private AiChatContextResult buildResult(StringBuilder context, String intent, String source, boolean usedPlatform, boolean usedKnowledge) {
        String contextText = context.length() <= 90 ? "" : truncate(context.toString(), MAX_CONTEXT_CHARS);
        return new AiChatContextResult(contextText, intent, source, buildSourceSummary(usedPlatform, usedKnowledge, true));
    }

    private String buildSourceSummary(boolean usedPlatform, boolean usedKnowledge, boolean usedGeneralModel) {
        List<String> sources = new ArrayList<>();
        if (usedPlatform) {
            sources.add("平台数据");
        }
        if (usedKnowledge) {
            sources.add("知识库");
        }
        if (usedGeneralModel) {
            sources.add("通用模型推理");
        }
        if (sources.isEmpty()) {
            sources.add("通用 AI");
        }
        String suffix = usedPlatform || usedKnowledge
                ? "；未出现在上述来源中的内容需要人工核对。"
                : "；平台记录相关内容无法确认。";
        return "回答来源：" + String.join("、", sources) + suffix;
    }

    private boolean shouldUseKnowledgeOnly(String intent) {
        return "KNOWLEDGE_QA".equals(intent) || "LEARN".equals(intent);
    }

    private boolean hasLearningScope(SearchScope scope) {
        return scope != null
                && (StringUtils.hasText(scope.courseCategory())
                || StringUtils.hasText(scope.knowledgePoint())
                || containsAny(scope.question(), "是什么", "怎么", "如何", "为什么", "区别", "原理", "例子", "示例"));
    }

    private void appendPlatformAccessContext(StringBuilder context, SearchScope scope, SysUser user) {
        if (isTeacherLike(user.getRole())) {
            appendManagedClassContext(context, scope, user);
        } else if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            appendStudentProfileContext(context, scope, user);
        }
    }

    private void appendStudentContext(StringBuilder context, SearchScope scope, SysUser user) {
        appendStudentMistakes(context, scope, user.getId());
        appendStudentExamRecords(context, scope, user.getId());
        appendStudentHomeworkFeedback(context, scope, user.getId());
        appendQuestionReferences(context, scope, false, user.getId());
    }

    private void appendTeacherContext(StringBuilder context, SearchScope scope, SysUser user) {
        appendQuestionReferences(context, scope, true, user.getId());
        appendTeacherTemplates(context, scope, user.getId());
        appendTeacherTaskSummary(context, scope, user);
    }

    private void appendManagedClassContext(StringBuilder context, SearchScope scope, SysUser user) {
        List<SysClass> classes = getVisibleClasses(user).stream()
                .filter(sysClass -> matchesClass(sysClass, scope))
                .limit(8)
                .toList();
        if (classes.isEmpty()) {
            appendLine(context, "\n【可管理班级】暂无可管理班级或未匹配到相关班级。");
            return;
        }

        appendLine(context, "\n【可管理班级概览】");
        for (SysClass sysClass : classes) {
            Long studentCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, "STUDENT")
                    .eq(SysUser::getClassId, sysClass.getId()));
            List<SysUser> students = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, "STUDENT")
                    .eq(SysUser::getClassId, sysClass.getId())
                    .orderByAsc(SysUser::getUsername)
                    .last("LIMIT 12"));
            appendLine(context, String.format(
                    "- 班级ID:%s；班级:%s；状态:%s；学生数:%d；学生示例:%s",
                    sysClass.getId(),
                    safeText(sysClass.getClassName()),
                    sysClass.getStatus() != null && sysClass.getStatus() == 1 ? "进行中" : "已结课",
                    studentCount == null ? 0 : studentCount,
                    formatStudentBrief(students)
            ));

            appendClassRecentTasks(context, sysClass.getId(), user);
        }
    }

    private void appendClassRecentTasks(StringBuilder context, Long classId, SysUser user) {
        List<AssessmentTask> tasks = assessmentTaskService.list(new LambdaQueryWrapper<AssessmentTask>()
                .orderByDesc(AssessmentTask::getCreateTime)
                .last("LIMIT 80")).stream()
                .filter(task -> task.getIsDeleted() == null || task.getIsDeleted() == 0)
                .filter(task -> "ADMIN".equalsIgnoreCase(user.getRole()) || Objects.equals(task.getCreatorId(), user.getId())
                        || taskTargetsClass(task, classId))
                .filter(task -> taskTargetsClass(task, classId))
                .limit(3)
                .toList();
        if (tasks.isEmpty()) {
            return;
        }
        for (AssessmentTask task : tasks) {
            List<StudentSubmission> submissions = submissionService.list(new LambdaQueryWrapper<StudentSubmission>()
                    .eq(StudentSubmission::getTaskId, task.getId()));
            long submitted = submissions.stream().filter(item -> !"UN".equals(item.getStatus())).count();
            long graded = submissions.stream().filter(item -> "GRADED".equals(item.getStatus())).count();
            appendLine(context, String.format(
                    "  - 近期%s:%s；提交:%d；已评分:%d；截止:%s",
                    "EXAM".equals(task.getType()) ? "考试" : "作业",
                    safeText(task.getTitle()),
                    submitted,
                    graded,
                    task.getEndTime()
            ));
        }
    }

    private void appendStudentProfileContext(StringBuilder context, SearchScope scope, SysUser user) {
        SysUser student = sysUserMapper.selectById(user.getId());
        if (student == null) {
            return;
        }
        SysClass sysClass = student.getClassId() == null ? null : sysClassMapper.selectById(student.getClassId());
        appendLine(context, "\n【我的基础信息】");
        appendLine(context, String.format(
                "- 学生ID:%s；用户名:%s；姓名:%s；班级ID:%s；班级:%s",
                student.getId(),
                safeText(student.getUsername()),
                safeText(student.getRealName()),
                student.getClassId() == null ? "未分配" : student.getClassId(),
                sysClass == null ? "未分配" : safeText(sysClass.getClassName())
        ));

        appendStudentTaskOverview(context, scope, student);
        appendStudentRecentSubmissions(context, student.getId());
        appendStudentMistakeOverview(context, scope, student.getId());
    }

    private void appendStudentTaskOverview(StringBuilder context, SearchScope scope, SysUser student) {
        LocalDateTime now = LocalDateTime.now();
        List<AssessmentTask> visibleTasks = assessmentTaskService.list(new LambdaQueryWrapper<AssessmentTask>()
                .orderByDesc(AssessmentTask::getCreateTime)
                .last("LIMIT 100")).stream()
                .filter(task -> task.getIsDeleted() == null || task.getIsDeleted() == 0)
                .filter(task -> "EXAM".equals(task.getType()) || "HOMEWORK".equals(task.getType()))
                .filter(task -> student.getClassId() != null && taskTargetsClass(task, student.getClassId()))
                .toList();
        if (visibleTasks.isEmpty()) {
            return;
        }

        long pending = 0;
        long submitted = 0;
        long graded = 0;
        List<String> nextTasks = new ArrayList<>();
        for (AssessmentTask task : visibleTasks) {
            StudentSubmission submission = submissionService.getOne(new LambdaQueryWrapper<StudentSubmission>()
                    .eq(StudentSubmission::getTaskId, task.getId())
                    .eq(StudentSubmission::getStudentId, student.getId())
                    .orderByDesc(StudentSubmission::getVersion)
                    .last("LIMIT 1"), false);
            if (submission == null) {
                if (task.getEndTime() == null || task.getEndTime().isAfter(now)) {
                    pending++;
                    if (nextTasks.size() < 3) {
                        nextTasks.add(safeText(task.getTitle()) + "(截止:" + task.getEndTime() + ")");
                    }
                }
            } else if ("GRADED".equals(submission.getStatus())) {
                graded++;
            } else {
                submitted++;
            }
        }
        appendLine(context, "\n【我的任务概览】");
        appendLine(context, String.format(
                "- 待完成:%d；已提交待处理:%d；已评分:%d；最近待办:%s",
                pending,
                submitted,
                graded,
                nextTasks.isEmpty() ? "无" : String.join("；", nextTasks)
        ));
    }

    private void appendStudentRecentSubmissions(StringBuilder context, Long studentId) {
        List<StudentSubmission> submissions = submissionService.list(new LambdaQueryWrapper<StudentSubmission>()
                .eq(StudentSubmission::getStudentId, studentId)
                .orderByDesc(StudentSubmission::getSubmitTime)
                .last("LIMIT 5"));
        if (submissions.isEmpty()) {
            return;
        }

        appendLine(context, "\n【我的最近提交】");
        for (StudentSubmission submission : submissions) {
            AssessmentTask task = assessmentTaskService.getById(submission.getTaskId());
            appendLine(context, String.format(
                    "- %s:%s；状态:%s；得分:%s；提交时间:%s；教师评语:%s；班主任批注:%s",
                    task == null || !"EXAM".equals(task.getType()) ? "作业" : "考试",
                    task == null ? "未知任务" : safeText(task.getTitle()),
                    safeText(submission.getStatus()),
                    submission.getTotalScoreGained() == null ? "暂无" : submission.getTotalScoreGained(),
                    submission.getSubmitTime(),
                    clip(submission.getTeacherComment(), 100),
                    clip(submission.getAssistantComment(), 100)
            ));
        }
    }

    private void appendStudentMistakeOverview(StringBuilder context, SearchScope scope, Long studentId) {
        List<MistakeVO> mistakes = studentService.getMistakeList(
                        studentId,
                        StringUtils.hasText(scope.knowledgePoint()) ? scope.knowledgePoint() : null,
                        null
                ).stream()
                .filter(mistake -> matchesCourse(mistake.getCourseCategory(), scope.courseCategory()))
                .limit(3)
                .toList();
        appendLine(context, "\n【我的错题概览】");
        appendLine(context, "- 当前匹配错题数:" + mistakes.size());
        for (MistakeVO mistake : mistakes) {
            appendLine(context, String.format(
                    "  - 题目ID:%s；知识点:%s；累计错%d次；题干:%s",
                    mistake.getQuestionId(),
                    safeText(mistake.getKnowledgePoint()),
                    mistake.getWrongCount() == null ? 0 : mistake.getWrongCount(),
                    clip(mistake.getContent(), 120)
            ));
        }
    }

    private void appendStudentMistakes(StringBuilder context, SearchScope scope, Long studentId) {
        List<MistakeVO> mistakes = studentService.getMistakeList(
                        studentId,
                        StringUtils.hasText(scope.knowledgePoint()) ? scope.knowledgePoint() : null,
                        null
                ).stream()
                .filter(mistake -> matchesCourse(mistake.getCourseCategory(), scope.courseCategory()))
                .sorted(Comparator.comparing(MistakeVO::getWrongCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(SECTION_LIMIT)
                .toList();

        if (mistakes.isEmpty()) {
            return;
        }

        appendLine(context, "\n【学生错题本】");
        for (MistakeVO mistake : mistakes) {
            appendLine(context, String.format(
                    "- 题目ID:%s；知识点:%s；题型:%s；难度:%s；累计错%d次；最近错题时间:%s；题干:%s",
                    mistake.getQuestionId(),
                    safeText(mistake.getKnowledgePoint()),
                    safeText(mistake.getQuestionType()),
                    safeText(mistake.getDifficulty()),
                    mistake.getWrongCount() == null ? 0 : mistake.getWrongCount(),
                    mistake.getLastWrongTime(),
                    clip(mistake.getContent(), 160)
            ));
        }
    }

    private void appendKnowledgeContext(StringBuilder context, SearchScope scope, SysUser user) {
        String ragContext = knowledgeRetrievalService.buildRagContext(
                scope.question(),
                scope.courseCategory(),
                scope.knowledgePoint(),
                user,
                5
        );
        if (StringUtils.hasText(ragContext)) {
            for (String line : ("\n" + ragContext).split("\\n")) {
                appendLine(context, line);
            }
        }
    }

    private void appendStudentExamRecords(StringBuilder context, SearchScope scope, Long studentId) {
        List<StudentSubmission> submissions = submissionService.list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getStudentId, studentId)
                        .orderByDesc(StudentSubmission::getSubmitTime)
                        .last("LIMIT 8")
        );

        int appended = 0;
        for (StudentSubmission submission : submissions) {
            if (appended >= SECTION_LIMIT) {
                break;
            }
            AssessmentTask task = assessmentTaskService.getById(submission.getTaskId());
            if (task == null || !"EXAM".equals(task.getType())) {
                continue;
            }

            List<StudentAnswerDetail> answers = answerDetailService.list(
                    new LambdaQueryWrapper<StudentAnswerDetail>()
                            .eq(StudentAnswerDetail::getSubmissionId, submission.getId())
            );

            List<StudentAnswerDetail> relevantWrongAnswers = answers.stream()
                    .filter(answer -> answer.getIsCorrect() == null || answer.getIsCorrect() != 1)
                    .filter(answer -> {
                        QuestionBank question = questionBankService.getById(answer.getQuestionId());
                        return question != null && matchesQuestion(question, scope);
                    })
                    .limit(2)
                    .toList();

            if (relevantWrongAnswers.isEmpty() && !matchesText(task.getTitle(), scope)) {
                continue;
            }

            if (appended == 0) {
                appendLine(context, "\n【学生考试记录】");
            }
            appendLine(context, String.format(
                    "- 考试:%s；状态:%s；得分:%s/%s；提交时间:%s",
                    safeText(task.getTitle()),
                    safeText(submission.getStatus()),
                    submission.getTotalScoreGained(),
                    task.getTotalScore(),
                    submission.getSubmitTime()
            ));
            for (StudentAnswerDetail answer : relevantWrongAnswers) {
                QuestionBank question = questionBankService.getById(answer.getQuestionId());
                appendLine(context, String.format(
                        "  - 相关失分题: 题目ID:%s；知识点:%s；题型:%s；得分:%s；题干:%s",
                        answer.getQuestionId(),
                        safeText(question.getKnowledgePoint()),
                        safeText(question.getType()),
                        answer.getScoreGained(),
                        clip(question.getContent(), 140)
                ));
            }
            appended++;
        }
    }

    private void appendStudentHomeworkFeedback(StringBuilder context, SearchScope scope, Long studentId) {
        List<StudentSubmission> submissions = submissionService.list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getStudentId, studentId)
                        .orderByDesc(StudentSubmission::getSubmitTime)
                        .last("LIMIT 8")
        );

        int appended = 0;
        for (StudentSubmission submission : submissions) {
            if (appended >= SECTION_LIMIT) {
                break;
            }
            AssessmentTask task = assessmentTaskService.getById(submission.getTaskId());
            if (task == null || !"HOMEWORK".equals(task.getType())) {
                continue;
            }
            HomeworkSubmissionContent content = homeworkContentMapper.selectOne(
                    new LambdaQueryWrapper<HomeworkSubmissionContent>()
                            .eq(HomeworkSubmissionContent::getSubmissionId, submission.getId())
                            .last("LIMIT 1")
            );
            String combined = safeText(task.getTitle()) + " " + safeText(submission.getTeacherComment())
                    + " " + safeText(submission.getAssistantComment())
                    + " " + (content == null ? "" : safeText(content.getContent()));
            if (!matchesText(combined, scope)) {
                continue;
            }

            if (appended == 0) {
                appendLine(context, "\n【学生作业反馈】");
            }
            appendLine(context, String.format(
                    "- 作业:%s；状态:%s；得分:%s；教师评语:%s；班主任批注:%s；提交摘要:%s",
                    safeText(task.getTitle()),
                    safeText(submission.getStatus()),
                    submission.getTotalScoreGained(),
                    clip(submission.getTeacherComment(), 140),
                    clip(submission.getAssistantComment(), 140),
                    content == null ? "无" : clip(content.getContent(), 160)
            ));
            appended++;
        }
    }

    private void appendQuestionReferences(StringBuilder context, SearchScope scope, boolean teacher, Long userId) {
        LambdaQueryWrapper<QuestionBank> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(scope.courseCategory())) {
            query.like(QuestionBank::getCourseCategory, scope.courseCategory());
        }
        if (StringUtils.hasText(scope.knowledgePoint())) {
            query.like(QuestionBank::getKnowledgePoint, scope.knowledgePoint());
        }
        if (teacher) {
            query.and(wrapper -> wrapper.eq(QuestionBank::getCreatorId, userId).or().eq(QuestionBank::getIsPublic, (byte) 1));
        } else {
            query.eq(QuestionBank::getIsPublic, (byte) 1);
        }
        query.orderByDesc(QuestionBank::getCreateTime).last("LIMIT " + SECTION_LIMIT);

        List<QuestionBank> questions = questionBankService.list(query);
        if (questions.isEmpty()) {
            return;
        }

        appendLine(context, teacher ? "\n【题库参考】" : "\n【公开题库参考】");
        for (QuestionBank question : questions) {
            if (teacher) {
                appendLine(context, String.format(
                        "- 题目ID:%s；知识点:%s；题型:%s；难度:%s；题干:%s；标准答案:%s；解析:%s",
                        question.getId(),
                        safeText(question.getKnowledgePoint()),
                        safeText(question.getType()),
                        safeText(question.getDifficulty()),
                        clip(question.getContent(), 180),
                        clip(question.getStandardAnswer(), 120),
                        clip(question.getAnalysis(), 180)
                ));
            } else {
                appendLine(context, String.format(
                        "- 题目ID:%s；知识点:%s；题型:%s；难度:%s；题干:%s",
                        question.getId(),
                        safeText(question.getKnowledgePoint()),
                        safeText(question.getType()),
                        safeText(question.getDifficulty()),
                        clip(question.getContent(), 180)
                ));
            }
        }
    }

    private void appendTeacherTemplates(StringBuilder context, SearchScope scope, Long teacherId) {
        LambdaQueryWrapper<QuestionPaperTemplate> query = new LambdaQueryWrapper<QuestionPaperTemplate>()
                .eq(QuestionPaperTemplate::getCreatorId, teacherId)
                .orderByDesc(QuestionPaperTemplate::getUpdatedAt)
                .orderByDesc(QuestionPaperTemplate::getCreatedAt)
                .last("LIMIT " + SECTION_LIMIT);
        if (StringUtils.hasText(scope.courseCategory())) {
            query.like(QuestionPaperTemplate::getCourseName, scope.courseCategory());
        }

        List<QuestionPaperTemplate> templates = templateService.list(query);
        if (templates.isEmpty()) {
            return;
        }

        appendLine(context, "\n【课程大纲/试卷模板参考】");
        for (QuestionPaperTemplate template : templates) {
            appendLine(context, String.format(
                    "- 模板:%s；课程:%s；题量:%s；总分:%s；说明:%s",
                    safeText(template.getName()),
                    safeText(template.getCourseName()),
                    template.getQuestionCount(),
                    template.getTotalScore(),
                    clip(template.getDescription(), 180)
            ));
        }
    }

    private void appendTeacherTaskSummary(StringBuilder context, SearchScope scope, SysUser user) {
        Set<Long> visibleClassIds = getVisibleClasses(user).stream()
                .map(SysClass::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<AssessmentTask> tasks = assessmentTaskService.list(
                new LambdaQueryWrapper<AssessmentTask>()
                        .orderByDesc(AssessmentTask::getCreateTime)
                        .last("LIMIT 100")
        ).stream()
                .filter(task -> task.getIsDeleted() == null || task.getIsDeleted() == 0)
                .filter(task -> canViewTask(task, user, visibleClassIds))
                .limit(TASK_LIMIT)
                .toList();

        int appended = 0;
        for (AssessmentTask task : tasks) {
            if (appended >= SECTION_LIMIT) {
                break;
            }
            if (!matchesText(task.getTitle(), scope) && !matchesTaskQuestions(task.getId(), scope)) {
                continue;
            }

            List<StudentSubmission> submissions = submissionService.list(
                    new LambdaQueryWrapper<StudentSubmission>()
                            .eq(StudentSubmission::getTaskId, task.getId())
            );
            long submitted = submissions.stream()
                    .filter(item -> !"UN".equals(item.getStatus()))
                    .count();
            List<BigDecimal> scores = submissions.stream()
                    .map(StudentSubmission::getTotalScoreGained)
                    .filter(Objects::nonNull)
                    .toList();
            BigDecimal average = scores.isEmpty()
                    ? null
                    : scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);

            if (appended == 0) {
                appendLine(context, "\n【教师任务反馈汇总】");
            }
            appendLine(context, String.format(
                    "- %s:%s；提交数:%d；已评分:%d；平均分:%s；截止:%s",
                    "EXAM".equals(task.getType()) ? "考试" : "作业",
                    safeText(task.getTitle()),
                    submitted,
                    scores.size(),
                    average == null ? "暂无" : average,
                    task.getEndTime()
            ));
            appendTaskParticipantDetails(context, task, submissions, visibleClassIds, user);
            appended++;
        }
    }

    private boolean canViewTask(AssessmentTask task, SysUser user, Set<Long> visibleClassIds) {
        if (task == null || user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole()) || Objects.equals(task.getCreatorId(), user.getId())) {
            return true;
        }
        List<Long> targetClassIds = parseTargetClassIds(task.getTargetClassIds());
        return !visibleClassIds.isEmpty()
                && (targetClassIds.isEmpty() || targetClassIds.stream().anyMatch(visibleClassIds::contains));
    }

    private void appendTaskParticipantDetails(
            StringBuilder context,
            AssessmentTask task,
            List<StudentSubmission> submissions,
            Set<Long> visibleClassIds,
            SysUser user) {
        if (task == null || submissions == null || submissions.isEmpty()) {
            return;
        }

        Map<Long, StudentSubmission> latestByStudent = latestSubmissionByStudent(submissions);
        List<SysUser> targetStudents = loadTargetStudents(task, visibleClassIds, user);
        Map<Long, SysUser> studentsById = new HashMap<>();
        for (SysUser student : targetStudents) {
            if (student.getId() != null) {
                studentsById.put(student.getId(), student);
            }
        }

        List<StudentSubmission> activeSubmissions = latestByStudent.values().stream()
                .filter(this::isActiveSubmission)
                .sorted(Comparator
                        .comparing(StudentSubmission::getTotalScoreGained, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StudentSubmission::getSubmitTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StudentSubmission::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<SysUser> notParticipatedStudents = targetStudents.stream()
                .filter(student -> student.getId() != null)
                .filter(student -> !isActiveSubmission(latestByStudent.get(student.getId())))
                .toList();

        if (activeSubmissions.isEmpty() && notParticipatedStudents.isEmpty()) {
            return;
        }

        appendLine(context, String.format(
                "  - 人员明细: 目标学生:%d；已参加/已提交:%d；未参加:%d；以下为已参加学生最新记录",
                targetStudents.size(),
                activeSubmissions.size(),
                notParticipatedStudents.size()
        ));
        int detailLimit = "EXAM".equals(task.getType()) ? 30 : 20;
        for (StudentSubmission submission : activeSubmissions.stream().limit(detailLimit).toList()) {
            SysUser student = studentsById.get(submission.getStudentId());
            if (student == null && submission.getStudentId() != null) {
                student = sysUserMapper.selectById(submission.getStudentId());
            }
            appendLine(context, String.format(
                    "    - 学生ID:%s；姓名:%s；账号/学号:%s；班级ID:%s；状态:%s；得分:%s；提交时间:%s；开始时间:%s",
                    submission.getStudentId(),
                    student == null ? "未知学生" : safeText(student.getRealName()),
                    student == null ? "未知" : safeText(student.getUsername()),
                    student == null || student.getClassId() == null ? "未知" : student.getClassId(),
                    safeText(submission.getStatus()),
                    submission.getTotalScoreGained() == null ? "暂无" : submission.getTotalScoreGained(),
                    submission.getSubmitTime(),
                    submission.getStartTime()
            ));
        }
        if (activeSubmissions.size() > detailLimit) {
            appendLine(context, "    - 其余已参加学生未展开，数量:" + (activeSubmissions.size() - detailLimit));
        }

        if (!notParticipatedStudents.isEmpty()) {
            appendLine(context, "  - 未参加学生完整名单:");
            for (SysUser student : notParticipatedStudents) {
                appendLine(context, String.format(
                        "    - 学生ID:%s；姓名:%s；账号/学号:%s；班级ID:%s；状态:未参加",
                        student.getId(),
                        safeText(student.getRealName()),
                        safeText(student.getUsername()),
                        student.getClassId() == null ? "未知" : student.getClassId()
                ));
            }
        }
    }

    private List<SysUser> loadTargetStudents(AssessmentTask task, Set<Long> visibleClassIds, SysUser user) {
        List<Long> targetClassIds = parseTargetClassIds(task.getTargetClassIds());
        Set<Long> scopedClassIds = targetClassIds.isEmpty()
                ? visibleClassIds
                : targetClassIds.stream()
                .filter(classId -> "ADMIN".equalsIgnoreCase(user.getRole()) || visibleClassIds.contains(classId)
                        || Objects.equals(task.getCreatorId(), user.getId()))
                .collect(Collectors.toSet());

        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .orderByAsc(SysUser::getClassId)
                .orderByAsc(SysUser::getUsername);
        if (!scopedClassIds.isEmpty()) {
            query.in(SysUser::getClassId, scopedClassIds);
        } else if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !Objects.equals(task.getCreatorId(), user.getId())) {
            return Collections.emptyList();
        }
        query.last("LIMIT 500");
        return sysUserMapper.selectList(query);
    }

    private Map<Long, StudentSubmission> latestSubmissionByStudent(List<StudentSubmission> submissions) {
        Map<Long, StudentSubmission> result = new LinkedHashMap<>();
        for (StudentSubmission submission : submissions) {
            if (submission == null || submission.getStudentId() == null) {
                continue;
            }
            result.merge(submission.getStudentId(), submission, this::latestSubmission);
        }
        return result;
    }

    private StudentSubmission latestSubmission(StudentSubmission left, StudentSubmission right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        int leftVersion = left.getVersion() == null ? 0 : left.getVersion();
        int rightVersion = right.getVersion() == null ? 0 : right.getVersion();
        if (leftVersion != rightVersion) {
            return leftVersion > rightVersion ? left : right;
        }
        LocalDateTime leftTime = left.getSubmitTime() != null ? left.getSubmitTime() : left.getStartTime();
        LocalDateTime rightTime = right.getSubmitTime() != null ? right.getSubmitTime() : right.getStartTime();
        if (leftTime != null && rightTime != null && !leftTime.equals(rightTime)) {
            return leftTime.isAfter(rightTime) ? left : right;
        }
        if (left.getId() == null) {
            return right;
        }
        if (right.getId() == null) {
            return left;
        }
        return left.getId() > right.getId() ? left : right;
    }

    private boolean isActiveSubmission(StudentSubmission submission) {
        return submission != null
                && StringUtils.hasText(submission.getStatus())
                && !"UN".equalsIgnoreCase(submission.getStatus())
                && !"NOT_SUBMITTED".equalsIgnoreCase(submission.getStatus());
    }

    private boolean matchesTaskQuestions(Long taskId, SearchScope scope) {
        List<StudentSubmission> submissions = submissionService.list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, taskId)
                        .last("LIMIT 1")
        );
        if (submissions.isEmpty()) {
            return false;
        }
        List<StudentAnswerDetail> answers = answerDetailService.list(
                new LambdaQueryWrapper<StudentAnswerDetail>()
                        .eq(StudentAnswerDetail::getSubmissionId, submissions.get(0).getId())
                        .last("LIMIT 10")
        );
        return answers.stream()
                .map(answer -> questionBankService.getById(answer.getQuestionId()))
                .filter(Objects::nonNull)
                .anyMatch(question -> matchesQuestion(question, scope));
    }

    private List<SysClass> getVisibleClasses(SysUser user) {
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>()
                    .eq(SysClass::getStatus, (byte) 1)
                    .orderByDesc(SysClass::getCreateTime)
                    .last("LIMIT 20"));
        }

        List<Long> classIds = teacherClassRelMapper.selectList(new LambdaQueryWrapper<TeacherClassRel>()
                        .eq(TeacherClassRel::getTeacherId, user.getId()))
                .stream()
                .map(TeacherClassRel::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (classIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysClassMapper.selectList(new LambdaQueryWrapper<SysClass>()
                .in(SysClass::getId, classIds)
                .orderByDesc(SysClass::getCreateTime));
    }

    private boolean matchesClass(SysClass sysClass, SearchScope scope) {
        if (sysClass == null) {
            return false;
        }
        if (!StringUtils.hasText(scope.question()) && !StringUtils.hasText(scope.knowledgePoint())) {
            return true;
        }
        String combined = safeText(sysClass.getClassName()) + " " + sysClass.getId();
        if (contains(combined, scope.question()) || contains(combined, scope.knowledgePoint())) {
            return true;
        }
        return looksLikeClassQuery(scope.question());
    }

    private boolean looksLikeClassQuery(String question) {
        return contains(question, "班级")
                || contains(question, "学生")
                || contains(question, "学员")
                || contains(question, "名单")
                || contains(question, "提交")
                || contains(question, "成绩");
    }

    private boolean isPlatformInfoQuery(String question) {
        return looksLikeClassQuery(question)
                || contains(question, "个人信息")
                || contains(question, "我的信息")
                || contains(question, "我是谁")
                || contains(question, "我的班")
                || contains(question, "我的课")
                || contains(question, "最近")
                || contains(question, "今日")
                || contains(question, "今天")
                || contains(question, "明天")
                || contains(question, "截止")
                || contains(question, "未完成")
                || contains(question, "没完成")
                || contains(question, "待完成")
                || contains(question, "任务")
                || contains(question, "作业")
                || contains(question, "考试")
                || contains(question, "测验")
                || contains(question, "反馈")
                || contains(question, "错题");
    }

    private String formatStudentBrief(List<SysUser> students) {
        if (students == null || students.isEmpty()) {
            return "暂无";
        }
        return students.stream()
                .map(student -> safeText(student.getRealName()) + "(" + safeText(student.getUsername()) + ")")
                .collect(Collectors.joining("、"));
    }

    private boolean taskTargetsClass(AssessmentTask task, Long classId) {
        if (task == null || classId == null) {
            return false;
        }
        List<Long> targetClassIds = parseTargetClassIds(task.getTargetClassIds());
        return targetClassIds.isEmpty() || targetClassIds.contains(classId);
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (!StringUtils.hasText(targetClassIdsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private boolean matchesQuestion(QuestionBank question, SearchScope scope) {
        return matchesCourse(question.getCourseCategory(), scope.courseCategory())
                && (!StringUtils.hasText(scope.knowledgePoint())
                || contains(question.getKnowledgePoint(), scope.knowledgePoint())
                || contains(question.getContent(), scope.knowledgePoint())
                || contains(question.getAnalysis(), scope.knowledgePoint()));
    }

    private boolean matchesText(String text, SearchScope scope) {
        if (!StringUtils.hasText(scope.knowledgePoint()) && !StringUtils.hasText(scope.courseCategory())) {
            return true;
        }
        return contains(text, scope.knowledgePoint()) || contains(text, scope.courseCategory());
    }

    private boolean matchesCourse(String value, String courseCategory) {
        return !StringUtils.hasText(courseCategory) || contains(value, courseCategory);
    }

    private boolean contains(String text, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return false;
        }
        return StringUtils.hasText(text) && text.toLowerCase().contains(keyword.toLowerCase());
    }

    private String normalizeResponseSource(AiChatMessageDTO dto) {
        String source = dto == null ? null : dto.getResponseSource();
        if (!StringUtils.hasText(source)) {
            return "AUTO";
        }
        source = source.trim().toUpperCase();
        return switch (source) {
            case "PLATFORM_ONLY", "KNOWLEDGE_ONLY", "GENERAL_AI" -> source;
            default -> "AUTO";
        };
    }

    private String inferIntent(AiChatMessageDTO dto) {
        if (dto != null && StringUtils.hasText(dto.getIntent())) {
            String intent = dto.getIntent().trim().toUpperCase();
            if (List.of("LEARN", "PLATFORM_QUERY", "KNOWLEDGE_QA", "QUESTION_GEN", "QUESTION_REVIEW", "GENERAL").contains(intent)) {
                return intent;
            }
        }
        String question = dto == null || dto.getQuestion() == null ? "" : dto.getQuestion().toLowerCase();
        if (isPlatformInfoQuery(question)
                || containsAny(question, "成绩", "考试", "测验", "作业", "提交", "班级", "学生名单", "错题", "待完成", "未完成", "截止", "得分", "排名", "反馈")) {
            return "PLATFORM_QUERY";
        }
        if (containsAny(question, "知识库", "上传文档", "文档", "资料", "讲义", "根据资料", "根据文档", "依据资料", "依据文档")) {
            return "KNOWLEDGE_QA";
        }
        if (containsAny(question, "生成", "出题", "组卷", "命题", "选择题", "判断题", "简答题", "编程题", "练习题", "试题", "题目")) {
            return "QUESTION_GEN";
        }
        if (containsAny(question, "改题", "优化题", "修改题", "完善解析", "评分点", "采分点", "题目质量", "区分度", "难度")) {
            return "QUESTION_REVIEW";
        }
        if (containsAny(question, "讲解", "解释", "是什么", "怎么理解", "怎么做", "如何", "为什么", "原理", "区别", "示例", "例子", "复习", "学习")) {
            return "LEARN";
        }
        return "GENERAL";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (contains(text, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTeacherLike(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "TEACHER".equalsIgnoreCase(role) || "ASSISTANT".equalsIgnoreCase(role);
    }

    private void appendLine(StringBuilder context, String line) {
        if (context.length() >= MAX_CONTEXT_CHARS) {
            return;
        }
        context.append(truncate(line, Math.min(500, MAX_CONTEXT_CHARS - context.length()))).append('\n');
    }

    private String clip(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "无";
        }
        return truncate(value.replaceAll("\\s+", " ").trim(), maxLength);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未指定";
    }

    private record SearchScope(String courseCategory, String knowledgePoint, String question) {
        private static SearchScope from(AiChatMessageDTO dto, AiChatSession session) {
            String courseCategory = StringUtils.hasText(dto.getCourseCategory())
                    ? dto.getCourseCategory().trim()
                    : session == null ? null : session.getCourseCategory();
            String knowledgePoint = StringUtils.hasText(dto.getKnowledgePoint())
                    ? dto.getKnowledgePoint().trim()
                    : session == null ? null : session.getKnowledgePoint();
            String question = StringUtils.hasText(dto.getQuestion()) ? dto.getQuestion().trim() : "";
            return new SearchScope(courseCategory, knowledgePoint, question);
        }
    }
}
