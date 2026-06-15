package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.AiChatContextResult;
import com.example.edubackend.dto.AiChatHistoryMessageVO;
import com.example.edubackend.dto.AiChatMessageDTO;
import com.example.edubackend.dto.AiChatMessageVO;
import com.example.edubackend.dto.AiChatSessionVO;
import com.example.edubackend.dto.CreateAiChatSessionDTO;
import com.example.edubackend.dto.UpdateAiChatSessionDTO;
import com.example.edubackend.entity.AiChatMessage;
import com.example.edubackend.entity.AiChatSession;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AiChatMessageMapper;
import com.example.edubackend.mapper.AiChatSessionMapper;
import com.example.edubackend.service.AiChatBusinessContextAssembler;
import com.example.edubackend.service.AiChatPromptBuilder;
import com.example.edubackend.service.IAiChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements IAiChatService {

    private static final String DEFAULT_TITLE = "新会话";
    private static final String STATUS_GENERATING = "generating";
    private static final String STATUS_FINISHED = "finished";
    private static final String STATUS_INTERRUPTED = "interrupted";
    private static final String STATUS_FAILED = "failed";
    private static final long SSE_TIMEOUT_MS = 180000L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiChatPromptBuilder promptBuilder;
    private final AiChatBusinessContextAssembler businessContextAssembler;
    private final ExecutorService streamExecutor = new ThreadPoolExecutor(
            4,
            16,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    private final ConcurrentHashMap<String, StreamGeneration> activeGenerations = new ConcurrentHashMap<>();

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.endpoint:https://api.deepseek.com/v1/chat/completions}")
    private String apiEndpoint;

    @Value("${ai.api.model:deepseek-chat}")
    private String model;

    @Value("${ai.chat.context-size:10}")
    private Integer contextSize;

    @Override
    public AiChatMessageVO sendMessage(AiChatMessageDTO dto, SysUser user) {
        checkUser(user);

        String role = user.getRole();
        String mode = normalizeMode(dto.getMode());
        AiChatContextResult contextResult = businessContextAssembler.buildContextResult(dto, user);
        if (!isAiConfigured()) {
            return buildOneShotVO(dto, user, mode, contextResult, buildLocalContextAnswer(contextResult.getContext()));
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", promptBuilder.buildSystemPrompt(role, mode, contextResult.getIntent())));
        messages.add(Map.of("role", "user", "content", promptBuilder.buildUserPrompt(
                dto,
                contextResult.getContext(),
                contextResult.getIntent(),
                contextResult.getResponseSource(),
                contextResult.getSourceSummary())));

        String answer = callAiApi(messages);
        return buildOneShotVO(dto, user, mode, contextResult, answer);
    }

    @Override
    @Transactional
    public AiChatSessionVO createSession(CreateAiChatSessionDTO dto, SysUser user) {
        checkUser(user);

        AiChatSession session = new AiChatSession();
        session.setUserId(user.getId());
        session.setUserRole(user.getRole());
        session.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : DEFAULT_TITLE);
        session.setCourseCategory(trimToNull(dto.getCourseCategory()));
        session.setKnowledgePoint(trimToNull(dto.getKnowledgePoint()));
        session.setLastMessageTime(LocalDateTime.now());
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setIsDeleted((byte) 0);
        sessionMapper.insert(session);
        return toSessionVO(session);
    }

    @Override
    public List<AiChatSessionVO> getSessionList(SysUser user) {
        checkUser(user);
        List<AiChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, user.getId())
                        .eq(AiChatSession::getUserRole, user.getRole())
                        .orderByDesc(AiChatSession::getIsTop)
                        .orderByDesc(AiChatSession::getLastMessageTime)
                        .orderByDesc(AiChatSession::getCreateTime)
        );
        return sessions.stream().map(this::toSessionVO).collect(Collectors.toList());
    }

    @Override
    public List<AiChatHistoryMessageVO> getSessionMessages(Long sessionId, SysUser user) {
        checkUser(user);
        assertSessionOwner(sessionId, user);
        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .eq(AiChatMessage::getUserId, user.getId())
                        .eq(AiChatMessage::getUserRole, user.getRole())
                        .orderByAsc(AiChatMessage::getCreateTime)
                        .orderByAsc(AiChatMessage::getId)
        );
        return messages.stream().map(this::toMessageVO).toList();
    }

    @Override
    public AiChatMessageVO sendSessionMessage(Long sessionId, AiChatMessageDTO dto, SysUser user) {
        checkUser(user);

        AiChatSession session = assertSessionOwner(sessionId, user);
        String mode = normalizeMode(dto.getMode());
        LocalDateTime now = LocalDateTime.now();

        applySessionDefaults(dto, session);

        AiChatContextResult contextResult = businessContextAssembler.buildContextResult(dto, session, user);

        AiChatMessage userMessage = buildMessage(session, user, "USER", dto.getQuestion(), dto, mode, null, now);
        applyContextMetadata(userMessage, contextResult);
        messageMapper.insert(userMessage);

        AiChatMessage assistantMessage = buildMessage(
                session, user, "ASSISTANT", "", dto, mode, model, LocalDateTime.now(), STATUS_GENERATING);
        applyContextMetadata(assistantMessage, contextResult);
        messageMapper.insert(assistantMessage);

        try {
            if (!isAiConfigured()) {
                String answer = buildLocalContextAnswer(contextResult.getContext());
                LocalDateTime finishedAt = LocalDateTime.now();
                assistantMessage.setContent(answer);
                assistantMessage.setStatus(STATUS_FINISHED);
                assistantMessage.setCreateTime(finishedAt);
                messageMapper.updateById(assistantMessage);
                refreshSessionAfterMessage(session, dto.getQuestion(), finishedAt);
                return buildSessionMessageVO(session, user, userMessage, assistantMessage, dto, mode, contextResult, answer, finishedAt);
            }

            List<Map<String, String>> contextMessages = buildContextMessages(
                    session, user, mode, contextResult, userMessage.getId(), userMessage.getId());
            String answer = callAiApi(contextMessages);

            LocalDateTime finishedAt = LocalDateTime.now();
            assistantMessage.setContent(answer);
            assistantMessage.setStatus(STATUS_FINISHED);
            assistantMessage.setCreateTime(finishedAt);
            messageMapper.updateById(assistantMessage);

            refreshSessionAfterMessage(session, dto.getQuestion(), finishedAt);

            return buildSessionMessageVO(session, user, userMessage, assistantMessage, dto, mode, contextResult, answer, finishedAt);
        } catch (BusinessException e) {
            markAssistantFailed(assistantMessage);
            refreshSessionAfterMessage(session, dto.getQuestion(), LocalDateTime.now());
            throw e;
        } catch (RuntimeException e) {
            markAssistantFailed(assistantMessage);
            refreshSessionAfterMessage(session, dto.getQuestion(), LocalDateTime.now());
            throw e;
        }
    }

    @Override
    public SseEmitter streamSessionMessage(Long sessionId, AiChatMessageDTO dto, SysUser user) {
        checkUser(user);

        AiChatSession session = assertSessionOwner(sessionId, user);
        String mode = normalizeMode(dto.getMode());
        LocalDateTime now = LocalDateTime.now();

        applySessionDefaults(dto, session);
        AiChatContextResult contextResult = businessContextAssembler.buildContextResult(dto, session, user);

        AiChatMessage userMessage = buildMessage(session, user, "USER", dto.getQuestion(), dto, mode, null, now);
        applyContextMetadata(userMessage, contextResult);
        messageMapper.insert(userMessage);

        AiChatMessage assistantMessage = buildMessage(
                session, user, "ASSISTANT", "", dto, mode, model, LocalDateTime.now(), STATUS_GENERATING);
        applyContextMetadata(assistantMessage, contextResult);
        messageMapper.insert(assistantMessage);

        if (!isAiConfigured()) {
            return startLocalFallbackGeneration(session, userMessage, assistantMessage, contextResult, buildLocalContextAnswer(contextResult.getContext()));
        }

        List<Map<String, String>> contextMessages = buildContextMessages(
                session, user, mode, contextResult, userMessage.getId(), userMessage.getId());

        return startStreamGeneration(session, user, userMessage, assistantMessage, contextResult, contextMessages);
    }

    @Override
    public SseEmitter regenerateLastAnswer(Long sessionId, SysUser user) {
        checkUser(user);

        AiChatSession session = assertSessionOwner(sessionId, user);
        AiChatMessage lastUserMessage = messageMapper.selectOne(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, session.getId())
                        .eq(AiChatMessage::getUserId, user.getId())
                        .eq(AiChatMessage::getUserRole, user.getRole())
                        .eq(AiChatMessage::getMessageRole, "USER")
                        .orderByDesc(AiChatMessage::getCreateTime)
                        .orderByDesc(AiChatMessage::getId)
                        .last("LIMIT 1")
        );
        if (lastUserMessage == null) {
            throw new BusinessException(400, "当前会话没有可重新生成的问题");
        }

        String mode = normalizeMode(lastUserMessage.getMode());
        AiChatMessageDTO dto = new AiChatMessageDTO();
        dto.setCourseCategory(StringUtils.hasText(lastUserMessage.getCourseCategory())
                ? lastUserMessage.getCourseCategory()
                : session.getCourseCategory());
        dto.setKnowledgePoint(StringUtils.hasText(lastUserMessage.getKnowledgePoint())
                ? lastUserMessage.getKnowledgePoint()
                : session.getKnowledgePoint());
        dto.setQuestion(lastUserMessage.getContent());
        dto.setMode(mode);
        dto.setIntent(lastUserMessage.getIntent());
        dto.setResponseSource(lastUserMessage.getResponseSource());

        AiChatContextResult contextResult = businessContextAssembler.buildContextResult(dto, session, user);

        AiChatMessage assistantMessage = buildMessage(
                session, user, "ASSISTANT", "", dto, mode, model, LocalDateTime.now(), STATUS_GENERATING);
        applyContextMetadata(assistantMessage, contextResult);
        messageMapper.insert(assistantMessage);

        if (!isAiConfigured()) {
            return startLocalFallbackGeneration(session, lastUserMessage, assistantMessage, contextResult, buildLocalContextAnswer(contextResult.getContext()));
        }

        List<Map<String, String>> contextMessages = buildContextMessages(
                session, user, mode, contextResult, lastUserMessage.getId(), lastUserMessage.getId());

        return startStreamGeneration(session, user, lastUserMessage, assistantMessage, contextResult, contextMessages);
    }

    @Override
    public void cancelStreamMessage(Long sessionId, SysUser user) {
        checkUser(user);
        assertSessionOwner(sessionId, user);
        StreamGeneration generation = activeGenerations.get(streamKey(user, sessionId));
        if (generation != null) {
            generation.cancelled().set(true);
        }
    }

    @Override
    @Transactional
    public AiChatSessionVO updateSession(Long sessionId, UpdateAiChatSessionDTO dto, SysUser user) {
        AiChatSession session = assertSessionOwner(sessionId, user);
        if (dto.getTitle() != null) {
            session.setTitle(dto.getTitle());
        }
        if (dto.getIsTop() != null) {
            session.setIsTop((byte) (dto.getIsTop() ? 1 : 0));
        }
        sessionMapper.updateById(session);
        return toSessionVO(session);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, SysUser user) {
        checkUser(user);
        cancelStreamMessage(sessionId, user);
        AiChatSession session = assertSessionOwner(sessionId, user);
        sessionMapper.deleteById(session.getId());

        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, session.getId())
                        .eq(AiChatMessage::getUserId, user.getId())
                        .eq(AiChatMessage::getUserRole, user.getRole())
        );
        for (AiChatMessage message : messages) {
            messageMapper.deleteById(message.getId());
        }
    }

    private void checkUser(SysUser user) {
        if (user == null || user.getId() == null || !StringUtils.hasText(user.getRole())) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
    }

    private void checkAiConfig() {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(500, "AI服务未配置API Key");
        }
    }

    private boolean isAiConfigured() {
        return StringUtils.hasText(apiKey);
    }

    private String normalizeMode(String mode) {
        return StringUtils.hasText(mode) ? mode : "DETAILED";
    }

    private AiChatSession assertSessionOwner(Long sessionId, SysUser user) {
        if (sessionId == null) {
            throw new BusinessException(400, "会话ID不能为空");
        }
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !user.getId().equals(session.getUserId())) {
            throw new BusinessException(404, "会话不存在");
        }
        if (!user.getRole().equals(session.getUserRole())) {
            throw new BusinessException(403, "无权访问该会话");
        }
        return session;
    }

    private void applySessionDefaults(AiChatMessageDTO dto, AiChatSession session) {
        if (!StringUtils.hasText(dto.getCourseCategory()) && StringUtils.hasText(session.getCourseCategory())) {
            dto.setCourseCategory(session.getCourseCategory());
        }
        if (!StringUtils.hasText(dto.getKnowledgePoint()) && StringUtils.hasText(session.getKnowledgePoint())) {
            dto.setKnowledgePoint(session.getKnowledgePoint());
        }
    }

    private AiChatMessage buildMessage(
            AiChatSession session,
            SysUser user,
            String messageRole,
            String content,
            AiChatMessageDTO dto,
            String mode,
            String messageModel,
            LocalDateTime createTime) {
        return buildMessage(session, user, messageRole, content, dto, mode, messageModel, createTime, STATUS_FINISHED);
    }

    private AiChatMessage buildMessage(
            AiChatSession session,
            SysUser user,
            String messageRole,
            String content,
            AiChatMessageDTO dto,
            String mode,
            String messageModel,
            LocalDateTime createTime,
            String status) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(session.getId());
        message.setUserId(user.getId());
        message.setUserRole(user.getRole());
        message.setMessageRole(messageRole);
        message.setContent(content);
        message.setCourseCategory(trimToNull(dto == null ? null : dto.getCourseCategory()));
        message.setKnowledgePoint(trimToNull(dto == null ? null : dto.getKnowledgePoint()));
        message.setMode(mode);
        message.setIntent(dto == null ? null : trimToNull(dto.getIntent()));
        message.setResponseSource(dto == null ? null : trimToNull(dto.getResponseSource()));
        message.setModel(messageModel);
        message.setStatus(status);
        message.setCreateTime(createTime);
        message.setIsDeleted((byte) 0);
        return message;
    }

    private void refreshSessionAfterMessage(AiChatSession session, String firstQuestion, LocalDateTime lastMessageTime) {
        if (!StringUtils.hasText(session.getTitle()) || DEFAULT_TITLE.equals(session.getTitle())) {
            session.setTitle(buildTitle(firstQuestion));
        }
        session.setLastMessageTime(lastMessageTime);
        session.setUpdateTime(lastMessageTime);
        sessionMapper.updateById(session);
    }

    private void markAssistantFailed(AiChatMessage assistantMessage) {
        assistantMessage.setStatus(STATUS_FAILED);
        assistantMessage.setContent("");
        messageMapper.updateById(assistantMessage);
    }

    private void applyContextMetadata(AiChatMessage message, AiChatContextResult contextResult) {
        if (message == null || contextResult == null) {
            return;
        }
        message.setIntent(contextResult.getIntent());
        message.setResponseSource(contextResult.getResponseSource());
        message.setSourceSummary(contextResult.getSourceSummary());
    }

    private String buildTitle(String question) {
        if (!StringUtils.hasText(question)) {
            return DEFAULT_TITLE;
        }
        String title = question.replaceAll("\\s+", " ").trim();
        if (title.length() > 30) {
            title = title.substring(0, 30) + "...";
        }
        return title;
    }

    private List<Map<String, String>> buildContextMessages(
            AiChatSession session,
            SysUser user,
            String mode,
            Long currentUserMessageId,
            AiChatContextResult contextResult) {
        return buildContextMessages(session, user, mode, contextResult, currentUserMessageId, null);
    }

    private List<Map<String, String>> buildContextMessages(
            AiChatSession session,
            SysUser user,
            String mode,
            AiChatContextResult contextResult,
            Long currentUserMessageId,
            Long maxMessageId) {
        List<AiChatMessage> latestMessages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, session.getId())
                        .eq(AiChatMessage::getUserId, user.getId())
                        .eq(AiChatMessage::getUserRole, user.getRole())
                        .le(maxMessageId != null, AiChatMessage::getId, maxMessageId)
                        .orderByDesc(AiChatMessage::getCreateTime)
                        .orderByDesc(AiChatMessage::getId)
                        .last("LIMIT " + getContextSize())
        );
        Collections.reverse(latestMessages);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", promptBuilder.buildSystemPrompt(user.getRole(), mode, contextResult.getIntent())));
        for (AiChatMessage message : latestMessages) {
            String role = "ASSISTANT".equals(message.getMessageRole()) ? "assistant" : "user";
            String content = "USER".equals(message.getMessageRole())
                    ? promptBuilder.buildUserPrompt(
                            session,
                            message.getContent(),
                            message.getId().equals(currentUserMessageId) ? contextResult.getContext() : null,
                            message.getId().equals(currentUserMessageId) ? contextResult.getIntent() : message.getIntent(),
                            message.getId().equals(currentUserMessageId) ? contextResult.getResponseSource() : message.getResponseSource(),
                            message.getId().equals(currentUserMessageId) ? contextResult.getSourceSummary() : message.getSourceSummary())
                    : message.getContent();
            messages.add(Map.of("role", role, "content", content));
        }
        return messages;
    }

    private int getContextSize() {
        if (contextSize == null || contextSize < 2) {
            return 10;
        }
        return Math.min(contextSize, 20);
    }

    private SseEmitter startStreamGeneration(
            AiChatSession session,
            SysUser user,
            AiChatMessage userMessage,
            AiChatMessage assistantMessage,
            AiChatContextResult contextResult,
            List<Map<String, String>> contextMessages) {
        String key = streamKey(user, session.getId());
        StreamGeneration previous = activeGenerations.remove(key);
        if (previous != null) {
            previous.cancelled().set(true);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        StreamGeneration generation = new StreamGeneration(new AtomicBoolean(false), emitter);
        activeGenerations.put(key, generation);
        emitter.onCompletion(() -> activeGenerations.remove(key, generation));
        emitter.onTimeout(() -> {
            generation.cancelled().set(true);
            activeGenerations.remove(key, generation);
        });
        emitter.onError(error -> {
            generation.cancelled().set(true);
            activeGenerations.remove(key, generation);
        });

        streamExecutor.submit(() -> runStreamGeneration(
                key, generation, session, userMessage, assistantMessage, contextResult, contextMessages));
        return emitter;
    }

    private SseEmitter startLocalFallbackGeneration(
            AiChatSession session,
            AiChatMessage userMessage,
            AiChatMessage assistantMessage,
            AiChatContextResult contextResult,
            String answer) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        streamExecutor.submit(() -> {
            try {
                sendSse(emitter, "start", Map.of(
                        "sessionId", session.getId(),
                        "userMessageId", userMessage.getId(),
                        "assistantMessageId", assistantMessage.getId(),
                        "intent", safeText(contextResult.getIntent()),
                        "responseSource", safeText(contextResult.getResponseSource()),
                        "sourceSummary", safeText(contextResult.getSourceSummary()),
                        "status", STATUS_GENERATING
                ));
                sendSse(emitter, "delta", Map.of("content", answer));

                LocalDateTime finishedAt = LocalDateTime.now();
                if (isGenerationTargetAvailable(session, assistantMessage)) {
                    assistantMessage.setContent(answer);
                    assistantMessage.setStatus(STATUS_FINISHED);
                    assistantMessage.setCreateTime(finishedAt);
                    messageMapper.updateById(assistantMessage);
                    refreshSessionAfterMessage(session, userMessage.getContent(), finishedAt);
                }

                Map<String, Object> doneData = new HashMap<>();
                doneData.put("sessionId", session.getId());
                doneData.put("userMessageId", userMessage.getId());
                doneData.put("assistantMessageId", assistantMessage.getId());
                doneData.put("sessionTitle", session.getTitle());
                doneData.put("courseCategory", safeText(userMessage.getCourseCategory()));
                doneData.put("knowledgePoint", safeText(userMessage.getKnowledgePoint()));
                doneData.put("answer", answer);
                doneData.put("mode", safeText(assistantMessage.getMode()));
                doneData.put("intent", safeText(contextResult.getIntent()));
                doneData.put("responseSource", safeText(contextResult.getResponseSource()));
                doneData.put("sourceSummary", safeText(contextResult.getSourceSummary()));
                doneData.put("model", "LOCAL_CONTEXT");
                doneData.put("status", STATUS_FINISHED);
                doneData.put("createTime", finishedAt.toString());
                sendSse(emitter, "done", doneData);
                emitter.complete();
            } catch (Exception e) {
                log.error("AI问答本地上下文降级失败", e);
                try {
                    assistantMessage.setStatus(STATUS_FAILED);
                    messageMapper.updateById(assistantMessage);
                    sendSse(emitter, "error", Map.of(
                            "assistantMessageId", assistantMessage.getId(),
                            "status", STATUS_FAILED,
                            "message", "AI服务未配置API Key，且本地平台数据查询失败"
                    ));
                } catch (Exception ignored) {
                    // 客户端断开时不再重复记录发送失败。
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private void runStreamGeneration(
            String key,
            StreamGeneration generation,
            AiChatSession session,
            AiChatMessage userMessage,
            AiChatMessage assistantMessage,
            AiChatContextResult contextResult,
            List<Map<String, String>> contextMessages) {
        StringBuilder answer = new StringBuilder();
        try {
            sendSse(generation.emitter(), "start", Map.of(
                    "sessionId", session.getId(),
                    "userMessageId", userMessage.getId(),
                    "assistantMessageId", assistantMessage.getId(),
                    "intent", safeText(contextResult.getIntent()),
                    "responseSource", safeText(contextResult.getResponseSource()),
                    "sourceSummary", safeText(contextResult.getSourceSummary()),
                    "status", STATUS_GENERATING
            ));

            streamAiApi(contextMessages, generation.cancelled(), delta -> {
                answer.append(delta);
                sendSse(generation.emitter(), "delta", Map.of("content", delta));
            });

            if (generation.cancelled().get()) {
                if (!isGenerationTargetAvailable(session, assistantMessage)) {
                    generation.emitter().complete();
                    return;
                }
                assistantMessage.setContent(answer.toString());
                assistantMessage.setStatus(STATUS_INTERRUPTED);
                messageMapper.updateById(assistantMessage);
                refreshSessionAfterMessage(session, userMessage.getContent(), LocalDateTime.now());
                sendSse(generation.emitter(), "interrupted", Map.of(
                        "assistantMessageId", assistantMessage.getId(),
                        "answer", answer.toString(),
                        "status", STATUS_INTERRUPTED
                ));
                generation.emitter().complete();
                return;
            }

            LocalDateTime finishedAt = LocalDateTime.now();
            if (!isGenerationTargetAvailable(session, assistantMessage)) {
                generation.emitter().complete();
                return;
            }
            assistantMessage.setContent(answer.toString());
            assistantMessage.setStatus(STATUS_FINISHED);
            assistantMessage.setCreateTime(finishedAt);
            messageMapper.updateById(assistantMessage);
            refreshSessionAfterMessage(session, userMessage.getContent(), finishedAt);
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("sessionId", session.getId());
            doneData.put("userMessageId", userMessage.getId());
            doneData.put("assistantMessageId", assistantMessage.getId());
            doneData.put("sessionTitle", session.getTitle());
            doneData.put("courseCategory", safeText(userMessage.getCourseCategory()));
            doneData.put("knowledgePoint", safeText(userMessage.getKnowledgePoint()));
            doneData.put("answer", answer.toString());
            doneData.put("mode", safeText(assistantMessage.getMode()));
            doneData.put("intent", safeText(contextResult.getIntent()));
            doneData.put("responseSource", safeText(contextResult.getResponseSource()));
            doneData.put("sourceSummary", safeText(contextResult.getSourceSummary()));
            doneData.put("model", safeText(assistantMessage.getModel()));
            doneData.put("status", STATUS_FINISHED);
            doneData.put("createTime", finishedAt.toString());
            sendSse(generation.emitter(), "done", doneData);
            generation.emitter().complete();
        } catch (Exception e) {
            log.error("AI问答流式调用失败", e);
            String errorMessage = e instanceof BusinessException businessException
                    ? businessException.getMessage()
                    : "AI问答服务暂时不可用，请稍后重试";
            if (isGenerationTargetAvailable(session, assistantMessage)) {
                assistantMessage.setContent(answer.toString());
                assistantMessage.setStatus(generation.cancelled().get() ? STATUS_INTERRUPTED : STATUS_FAILED);
                messageMapper.updateById(assistantMessage);
            }
            try {
                sendSse(generation.emitter(), "error", Map.of(
                        "assistantMessageId", assistantMessage.getId(),
                        "status", assistantMessage.getStatus(),
                        "message", generation.cancelled().get() ? "生成已中断" : errorMessage
                ));
            } catch (Exception ignored) {
                // 客户端断开时不再重复记录发送失败。
            }
            generation.emitter().complete();
        } finally {
            activeGenerations.remove(key, generation);
        }
    }

    private boolean isGenerationTargetAvailable(AiChatSession session, AiChatMessage assistantMessage) {
        return sessionMapper.selectById(session.getId()) != null
                && messageMapper.selectById(assistantMessage.getId()) != null;
    }

    private void streamAiApi(List<Map<String, String>> messages, AtomicBoolean cancelled, StreamTokenHandler handler) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.5);
        body.put("max_tokens", 8192);
        body.put("stream", true);
        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint))
                .timeout(java.time.Duration.ofSeconds(180))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<Stream<String>> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(500, "AI API调用失败: " + response.statusCode());
        }

        try (Stream<String> lines = response.body()) {
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                if (cancelled.get()) {
                    return;
                }
                String line = iterator.next();
                if (!StringUtils.hasText(line) || !line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    return;
                }
                String delta = parseStreamDelta(data);
                if (StringUtils.hasText(delta)) {
                    handler.onToken(delta);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String parseStreamDelta(String data) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
            if (delta == null) {
                return "";
            }
            Object content = delta.get("content");
            return content == null ? "" : String.valueOf(content);
        } catch (Exception e) {
            log.warn("解析AI流式片段失败: {}", data, e);
            return "";
        }
    }

    private void sendSse(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
    }

    private String streamKey(SysUser user, Long sessionId) {
        return user.getId() + ":" + user.getRole() + ":" + sessionId;
    }

    private String callAiApi(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.5);
        body.put("max_tokens", 8192);
        body.put("messages", messages);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiEndpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new BusinessException(500, "AI API调用失败: " + response.getStatusCode());
            }
            return parseAiResponse(response.getBody());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI问答调用失败", e);
            throw new BusinessException(500, "AI问答服务暂时不可用，请稍后重试");
        }
    }

    @SuppressWarnings("unchecked")
    private String parseAiResponse(String responseBody) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

            if (responseMap.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
                String errorMessage = String.valueOf(error.getOrDefault("message", "Unknown error"));
                throw new BusinessException(500, "AI API错误: " + errorMessage);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(500, "AI API返回为空");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || !StringUtils.hasText((String) message.get("content"))) {
                throw new BusinessException(500, "AI API返回内容为空");
            }
            return ((String) message.get("content")).trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析AI问答响应失败", e);
            throw new BusinessException(500, "解析AI响应失败");
        }
    }

    private String buildLocalContextAnswer(String businessContext) {
        if (!StringUtils.hasText(businessContext)) {
            throw new BusinessException(500, "AI服务未配置API Key，且当前问题没有可直接查询的平台数据");
        }
        return """
                当前大模型 API Key 未配置，暂时无法进行开放式知识讲解或生成式分析。

                先根据平台已有数据返回可确认的信息：

                %s

                需要继续使用完整 AI 问答能力时，请在后端运行环境配置 AI_API_KEY 后重启 edu-backend。
                """.formatted(cleanLocalContext(businessContext)).trim();
    }

    private String cleanLocalContext(String businessContext) {
        return businessContext
                .replace("以下是平台业务数据检索结果，仅作为回答参考；若数据不足，请明确说明不能从平台记录中确认。", "")
                .trim();
    }

    private AiChatMessageVO buildOneShotVO(AiChatMessageDTO dto, SysUser user, String mode, AiChatContextResult contextResult, String answer) {
        AiChatMessageVO vo = new AiChatMessageVO();
        vo.setCourseCategory(dto.getCourseCategory());
        vo.setKnowledgePoint(dto.getKnowledgePoint());
        vo.setQuestion(dto.getQuestion());
        vo.setAnswer(answer);
        vo.setMode(mode);
        vo.setIntent(contextResult.getIntent());
        vo.setResponseSource(contextResult.getResponseSource());
        vo.setSourceSummary(contextResult.getSourceSummary());
        vo.setRole(user.getRole());
        vo.setModel(isAiConfigured() ? model : "LOCAL_CONTEXT");
        vo.setStatus(STATUS_FINISHED);
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    private AiChatMessageVO buildSessionMessageVO(
            AiChatSession session,
            SysUser user,
            AiChatMessage userMessage,
            AiChatMessage assistantMessage,
            AiChatMessageDTO dto,
            String mode,
            AiChatContextResult contextResult,
            String answer,
            LocalDateTime finishedAt) {
        AiChatMessageVO vo = new AiChatMessageVO();
        vo.setSessionId(session.getId());
        vo.setUserMessageId(userMessage.getId());
        vo.setAssistantMessageId(assistantMessage.getId());
        vo.setSessionTitle(session.getTitle());
        vo.setCourseCategory(userMessage.getCourseCategory());
        vo.setKnowledgePoint(userMessage.getKnowledgePoint());
        vo.setQuestion(dto.getQuestion());
        vo.setAnswer(answer);
        vo.setMode(mode);
        vo.setIntent(contextResult.getIntent());
        vo.setResponseSource(contextResult.getResponseSource());
        vo.setSourceSummary(contextResult.getSourceSummary());
        vo.setRole(user.getRole());
        vo.setModel(isAiConfigured() ? model : "LOCAL_CONTEXT");
        vo.setStatus(STATUS_FINISHED);
        vo.setCreateTime(finishedAt);
        return vo;
    }

    private AiChatSessionVO toSessionVO(AiChatSession session) {
        AiChatSessionVO vo = new AiChatSessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setCourseCategory(session.getCourseCategory());
        vo.setKnowledgePoint(session.getKnowledgePoint());
        vo.setUserRole(session.getUserRole());
        vo.setIsTop(session.getIsTop() != null && session.getIsTop() == 1);
        vo.setLastMessageTime(session.getLastMessageTime());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    private AiChatHistoryMessageVO toMessageVO(AiChatMessage message) {
        AiChatHistoryMessageVO vo = new AiChatHistoryMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setMessageRole(message.getMessageRole());
        vo.setContent(message.getContent());
        vo.setCourseCategory(message.getCourseCategory());
        vo.setKnowledgePoint(message.getKnowledgePoint());
        vo.setMode(message.getMode());
        vo.setIntent(message.getIntent());
        vo.setResponseSource(message.getResponseSource());
        vo.setSourceSummary(message.getSourceSummary());
        vo.setModel(message.getModel());
        vo.setStatus(StringUtils.hasText(message.getStatus()) ? message.getStatus() : STATUS_FINISHED);
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    @FunctionalInterface
    private interface StreamTokenHandler {
        void onToken(String delta) throws IOException;
    }

    private record StreamGeneration(AtomicBoolean cancelled, SseEmitter emitter) {
    }

}
