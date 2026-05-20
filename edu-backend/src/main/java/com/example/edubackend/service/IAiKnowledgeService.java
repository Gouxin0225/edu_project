package com.example.edubackend.service;

import com.example.edubackend.dto.AiKnowledgeCreateDTO;
import com.example.edubackend.dto.AiKnowledgeDocumentVO;
import com.example.edubackend.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IAiKnowledgeService {

    AiKnowledgeDocumentVO createTextDocument(AiKnowledgeCreateDTO dto, SysUser user);

    AiKnowledgeDocumentVO uploadDocument(MultipartFile file, String title, String courseCategory,
                                         String knowledgePoint, String visibility, String accessScope, SysUser user);

    List<AiKnowledgeDocumentVO> listDocuments(String courseCategory, SysUser user);

    void deleteDocument(Long id, SysUser user);
}
