package com.example.edubackend.service;

import com.example.edubackend.dto.AiChatMessageDTO;
import com.example.edubackend.entity.AiChatSession;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiChatPromptBuilder {

    public String buildSystemPrompt(String userRole, String mode) {
        return buildSystemPrompt(userRole, mode, "GENERAL");
    }

    public String buildSystemPrompt(String userRole, String mode, String intent) {
        String normalizedRole = StringUtils.hasText(userRole) ? userRole.trim().toUpperCase() : "STUDENT";
        String lengthRule = buildModeRule(mode);
        String intentRule = buildIntentRule(intent);

        if (isTeacherLike(normalizedRole)) {
            return buildTeacherSystemPrompt(lengthRule, intentRule);
        }
        return buildStudentSystemPrompt(lengthRule, intentRule);
    }

    public String buildUserPrompt(AiChatMessageDTO dto) {
        return buildUserPrompt(dto, null);
    }

    public String buildUserPrompt(AiChatMessageDTO dto, String businessContext) {
        return buildUserPrompt(dto, businessContext, null, null, null);
    }

    public String buildUserPrompt(
            AiChatMessageDTO dto,
            String businessContext,
            String intent,
            String responseSource,
            String sourceSummary) {
        return String.format("""
                本次任务意图：%s
                回答来源策略：%s
                课程方向：%s
                知识点：%s
                可用参考上下文：
                %s

                问题：%s

                来源说明：%s
                请只在参考上下文能支持时回答平台数据、知识库文档、学生成绩、作业、题库等事实；上下文没有给出的内容要明确说明无法确认。
                如果参考上下文为空或与问题无关，请转为通用教学助理回答，不要编造平台记录。
                如果平台参考上下文包含【知识库召回】片段，回答中引用这些片段时必须使用对应的 [K编号] 标注来源。
                """, safeText(intent), safeText(responseSource), safeText(dto.getCourseCategory()), safeText(dto.getKnowledgePoint()),
                safeContext(businessContext), dto.getQuestion(), safeText(sourceSummary));
    }

    public String buildUserPrompt(AiChatSession session, String question) {
        return buildUserPrompt(session, question, null);
    }

    public String buildUserPrompt(AiChatSession session, String question, String businessContext) {
        return buildUserPrompt(session, question, businessContext, null, null, null);
    }

    public String buildUserPrompt(
            AiChatSession session,
            String question,
            String businessContext,
            String intent,
            String responseSource,
            String sourceSummary) {
        return String.format("""
                本次任务意图：%s
                回答来源策略：%s
                会话默认课程方向：%s
                会话默认知识点：%s
                可用参考上下文：
                %s

                问题：%s
                来源说明：%s
                请只在参考上下文能支持时回答平台数据、知识库文档、学生成绩、作业、题库等事实；上下文没有给出的内容要明确说明无法确认。
                如果使用【知识库召回】片段，请用对应的 [K编号] 标注来源。
                """, safeText(intent), safeText(responseSource), safeText(session.getCourseCategory()), safeText(session.getKnowledgePoint()),
                safeContext(businessContext), question, safeText(sourceSummary));
    }

    private String buildStudentSystemPrompt(String lengthRule, String intentRule) {
        // 学生角色以学习引导为主：遇到考试、测验、作业求答案时，只给思路和步骤，不直接泄露最终答案。
        return """
                你是教学平台中的AI问答助手，当前用户是学生。
                回答目标是帮助学生理解知识、形成解题能力，而不是替学生完成考试或作业。
                角色规则：
                1. 偏讲解、提示、举例和复习引导，避免只给结论
                2. 先解释相关概念，再拆解思路和关键步骤
                3. 可以提示常错点、检查方法和进一步复习方向
                4. 可以根据平台参考上下文回答学生自己的班级、任务、成绩、错题、作业反馈等个人信息查询
                5. 如果用户询问考试、测验、作业、题库的最终答案，不能直接给最终答案或完整可抄答案；应改为给解题思路、必要公式、分步提示、相关知识点解释，并鼓励学生先尝试作答
                6. 不编造不存在的教材页码、平台数据或学生个人信息；平台上下文没有给出的数据要明确说明无法确认
                输出建议：
                - 使用清晰的小标题或序号
                - 对计算题优先展示方法和中间推理，不直接跳到最终答案
                - 对概念题给出类比或例子帮助理解
                """ + "\n" + lengthRule + "\n" + intentRule;
    }

    private String buildTeacherSystemPrompt(String lengthRule, String intentRule) {
        // 教师角色允许输出教学设计类内容，包括出题、改题、解析、评分参考和练习设计。
        return """
                你是教学平台中的AI问答助手，当前用户是教师。
                回答目标是辅助教师完成教学设计、课堂讲解、命题和评价工作。
                角色规则：
                1. 可以生成教学建议、课堂导入、讲解顺序、板书思路和提问设计
                2. 可以辅助写题、改题、写解析，说明题目考查点、难度和区分度
                3. 可以输出评分参考、采分点、常见错误扣分建议和反馈话术
                4. 可以根据知识点设计分层练习、拓展练习和复习任务
                5. 可以根据平台参考上下文回答所管理班级、学生名单、任务提交、成绩概览等班级管理查询
                6. 避免编造不存在的教材页码、平台数据或学生个人信息；平台上下文没有给出的数据要明确说明无法确认
                输出建议：
                - 优先给结构化结果，便于教师直接调整后用于课堂或题库
                - 涉及评分时给出可执行的评分维度
                - 涉及练习设计时兼顾基础、提高和易错点
                """ + "\n" + lengthRule + "\n" + intentRule;
    }

    private String buildModeRule(String mode) {
        return switch (StringUtils.hasText(mode) ? mode.trim().toUpperCase() : "DETAILED") {
            case "SIMPLE" -> "回答控制在300字以内，优先给出核心思路和一个简短示例。";
            case "LESSON_PLAN" -> "按教案式输出，包含教学目标、重点难点、讲解流程、互动提问和课后练习建议。";
            case "QUESTION" -> "按出题式输出，优先给出题型、难度、题干、选项、答案、解析和考查点。";
            default -> "回答要相对完整，包含概念解释、关键步骤、示例、复习建议和常见误区。";
        };
    }

    private String buildIntentRule(String intent) {
        return switch (StringUtils.hasText(intent) ? intent.trim().toUpperCase() : "GENERAL") {
            case "PLATFORM_QUERY" -> "本次意图是查询平台数据。只能基于参考上下文回答平台事实，缺失数据要明确说无法从平台确认。";
            case "KNOWLEDGE_QA" -> "本次意图是基于知识库回答。优先引用知识库片段，并使用 [K编号] 标注来源。";
            case "QUESTION_GEN" -> "本次意图是生成题目。输出应便于后续保存到题库，题目、答案和解析要分区清晰。";
            case "QUESTION_REVIEW" -> "本次意图是改题或优化题目。请指出问题、给出修改版和修改理由。";
            case "LEARN" -> "本次意图是学习讲解或答疑。优先帮助用户理解概念、过程、例子和常见误区。";
            default -> "本次意图是普通教学助理对话。保持回答直接、准确，不主动编造平台数据。";
        };
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未指定";
    }

    private String safeContext(String value) {
        return StringUtils.hasText(value) ? value.trim() : "无相关平台数据";
    }

    private boolean isTeacherLike(String role) {
        return "ADMIN".equals(role) || "TEACHER".equals(role) || "ASSISTANT".equals(role);
    }
}
