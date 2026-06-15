package com.example.edubackend.service;

import com.example.edubackend.dto.AiChatMessageDTO;
import com.example.edubackend.dto.AiChatMessageVO;
import com.example.edubackend.dto.AiChatHistoryMessageVO;
import com.example.edubackend.dto.AiChatSessionVO;
import com.example.edubackend.dto.CreateAiChatSessionDTO;
import com.example.edubackend.dto.UpdateAiChatSessionDTO;
import com.example.edubackend.entity.SysUser;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface IAiChatService {
    AiChatMessageVO sendMessage(AiChatMessageDTO dto, SysUser user);

    AiChatSessionVO createSession(CreateAiChatSessionDTO dto, SysUser user);

    List<AiChatSessionVO> getSessionList(SysUser user);

    List<AiChatHistoryMessageVO> getSessionMessages(Long sessionId, SysUser user);

    AiChatMessageVO sendSessionMessage(Long sessionId, AiChatMessageDTO dto, SysUser user);

    SseEmitter streamSessionMessage(Long sessionId, AiChatMessageDTO dto, SysUser user);

    SseEmitter regenerateLastAnswer(Long sessionId, SysUser user);

    void cancelStreamMessage(Long sessionId, SysUser user);

    AiChatSessionVO updateSession(Long sessionId, UpdateAiChatSessionDTO dto, SysUser user);

    void deleteSession(Long sessionId, SysUser user);
}
