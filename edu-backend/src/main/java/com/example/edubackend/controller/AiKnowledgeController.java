package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.AiKnowledgeChunkVO;
import com.example.edubackend.dto.AiKnowledgeCreateDTO;
import com.example.edubackend.dto.AiKnowledgeDocumentVO;
import com.example.edubackend.dto.AiKnowledgeSearchDTO;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IAiKnowledgeRetrievalService;
import com.example.edubackend.service.IAiKnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai-knowledge")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "TEACHER", "STUDENT"})
public class AiKnowledgeController {

    private final IAiKnowledgeService knowledgeService;
    private final IAiKnowledgeRetrievalService retrievalService;

    @PostMapping("/documents")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<AiKnowledgeDocumentVO> createTextDocument(@Valid @RequestBody AiKnowledgeCreateDTO dto) {
        SysUser user = UserContext.getUser();
        return Result.success(knowledgeService.createTextDocument(dto, user));
    }

    @PostMapping("/documents/upload")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<AiKnowledgeDocumentVO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam String courseCategory,
            @RequestParam(required = false) String knowledgePoint,
            @RequestParam(defaultValue = "PRIVATE") String visibility,
            @RequestParam(defaultValue = "TEACHER_ONLY") String accessScope) {
        SysUser user = UserContext.getUser();
        return Result.success(knowledgeService.uploadDocument(file, title, courseCategory, knowledgePoint, visibility, accessScope, user));
    }

    @GetMapping("/documents")
    public Result<List<AiKnowledgeDocumentVO>> listDocuments(@RequestParam(required = false) String courseCategory) {
        SysUser user = UserContext.getUser();
        return Result.success(knowledgeService.listDocuments(courseCategory, user));
    }

    @DeleteMapping("/documents/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> deleteDocument(@PathVariable Long id) {
        SysUser user = UserContext.getUser();
        knowledgeService.deleteDocument(id, user);
        return Result.success("知识文档已删除");
    }

    @PostMapping("/search")
    public Result<List<AiKnowledgeChunkVO>> search(@Valid @RequestBody AiKnowledgeSearchDTO dto) {
        SysUser user = UserContext.getUser();
        return Result.success(retrievalService.search(
                dto.getQuestion(),
                dto.getCourseCategory(),
                dto.getKnowledgePoint(),
                user,
                dto.getLimit() == null ? 5 : dto.getLimit()
        ));
    }
}
