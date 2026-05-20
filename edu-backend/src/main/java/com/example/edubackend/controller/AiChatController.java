package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.AiChatHistoryMessageVO;
import com.example.edubackend.dto.AiChatMessageDTO;
import com.example.edubackend.dto.AiChatMessageVO;
import com.example.edubackend.dto.AiChatSessionVO;
import com.example.edubackend.dto.CreateAiChatSessionDTO;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IAiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai-chat")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "TEACHER", "ASSISTANT", "STUDENT"})
public class AiChatController {

    private final IAiChatService aiChatService;

    @PostMapping("/message")
    public Result<AiChatMessageVO> sendMessage(@Valid @RequestBody AiChatMessageDTO dto) {
        SysUser user = UserContext.getUser();
        AiChatMessageVO vo = aiChatService.sendMessage(dto, user);
        return Result.success(vo);
    }

    @PostMapping("/sessions")
    public Result<AiChatSessionVO> createSession(@Valid @RequestBody CreateAiChatSessionDTO dto) {
        SysUser user = UserContext.getUser();
        return Result.success(aiChatService.createSession(dto, user));
    }

    @GetMapping("/sessions")
    public Result<List<AiChatSessionVO>> getSessionList() {
        SysUser user = UserContext.getUser();
        return Result.success(aiChatService.getSessionList(user));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<AiChatHistoryMessageVO>> getSessionMessages(@PathVariable Long id) {
        SysUser user = UserContext.getUser();
        return Result.success(aiChatService.getSessionMessages(id, user));
    }

    @PostMapping("/sessions/{id}/message")
    public Result<AiChatMessageVO> sendSessionMessage(
            @PathVariable Long id,
            @Valid @RequestBody AiChatMessageDTO dto) {
        SysUser user = UserContext.getUser();
        return Result.success(aiChatService.sendSessionMessage(id, dto, user));
    }

    @PostMapping(value = "/sessions/{id}/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSessionMessage(
            @PathVariable Long id,
            @Valid @RequestBody AiChatMessageDTO dto) {
        SysUser user = UserContext.getUser();
        return aiChatService.streamSessionMessage(id, dto, user);
    }

    @PostMapping(value = "/sessions/{id}/message/regenerate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter regenerateLastAnswer(@PathVariable Long id) {
        SysUser user = UserContext.getUser();
        return aiChatService.regenerateLastAnswer(id, user);
    }

    @PostMapping("/sessions/{id}/message/stream/cancel")
    public Result<Void> cancelStreamMessage(@PathVariable Long id) {
        SysUser user = UserContext.getUser();
        aiChatService.cancelStreamMessage(id, user);
        return Result.success("已请求中断生成");
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        SysUser user = UserContext.getUser();
        aiChatService.deleteSession(id, user);
        return Result.success("会话已删除");
    }
}
