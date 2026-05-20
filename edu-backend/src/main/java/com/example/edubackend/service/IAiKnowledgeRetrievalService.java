package com.example.edubackend.service;

import com.example.edubackend.dto.AiKnowledgeChunkVO;
import com.example.edubackend.entity.SysUser;

import java.util.List;

public interface IAiKnowledgeRetrievalService {

    List<AiKnowledgeChunkVO> search(String question, String courseCategory, String knowledgePoint, SysUser user, int limit);

    String buildRagContext(String question, String courseCategory, String knowledgePoint, SysUser user, int limit);
}
