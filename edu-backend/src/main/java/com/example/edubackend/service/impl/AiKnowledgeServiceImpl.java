package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.AiKnowledgeCreateDTO;
import com.example.edubackend.dto.AiKnowledgeDocumentVO;
import com.example.edubackend.entity.AiKnowledgeChunk;
import com.example.edubackend.entity.AiKnowledgeDocument;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AiKnowledgeChunkMapper;
import com.example.edubackend.mapper.AiKnowledgeDocumentMapper;
import com.example.edubackend.service.IAiKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AiKnowledgeServiceImpl implements IAiKnowledgeService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 120;
    private static final int MAX_TEXT_LENGTH = 200_000;

    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeChunkMapper chunkMapper;

    @Override
    @Transactional
    public AiKnowledgeDocumentVO createTextDocument(AiKnowledgeCreateDTO dto, SysUser user) {
        checkManagePermission(user);
        return createDocument(dto.getTitle(), dto.getCourseCategory(), dto.getKnowledgePoint(),
                "TEXT", "手动录入", dto.getVisibility(), dto.getAccessScope(), dto.getContent(), user);
    }

    @Override
    @Transactional
    public AiKnowledgeDocumentVO uploadDocument(
            MultipartFile file,
            String title,
            String courseCategory,
            String knowledgePoint,
            String visibility,
            String accessScope,
            SysUser user) {
        checkManagePermission(user);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (!isTextLikeFile(originalFilename)) {
            throw new BusinessException(400, "第一版知识库仅支持 .txt / .md / .csv 文本文档，可先复制内容使用录入接口");
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String documentTitle = StringUtils.hasText(title) ? title : stripExtension(originalFilename);
            return createDocument(documentTitle, courseCategory, knowledgePoint, "FILE",
                    originalFilename, visibility, accessScope, content, user);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "知识文档解析失败");
        }
    }

    @Override
    public List<AiKnowledgeDocumentVO> listDocuments(String courseCategory, SysUser user) {
        checkUser(user);
        LambdaQueryWrapper<AiKnowledgeDocument> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(courseCategory)) {
            query.eq(AiKnowledgeDocument::getCourseCategory, courseCategory.trim());
        }
        if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            query.eq(AiKnowledgeDocument::getVisibility, "PUBLIC");
            query.eq(AiKnowledgeDocument::getAccessScope, "STUDENT_SAFE");
        } else if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            query.and(wrapper -> wrapper.eq(AiKnowledgeDocument::getVisibility, "PUBLIC")
                    .or(owner -> owner.eq(AiKnowledgeDocument::getOwnerId, user.getId())
                            .eq(AiKnowledgeDocument::getOwnerRole, user.getRole())));
        }
        query.orderByDesc(AiKnowledgeDocument::getCreateTime);
        return documentMapper.selectList(query).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, SysUser user) {
        checkManagePermission(user);
        AiKnowledgeDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(404, "知识文档不存在");
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !user.getId().equals(document.getOwnerId())) {
            throw new BusinessException(403, "只能删除自己创建的知识文档");
        }
        documentMapper.deleteById(id);
        List<AiKnowledgeChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeChunk>().eq(AiKnowledgeChunk::getDocumentId, id)
        );
        for (AiKnowledgeChunk chunk : chunks) {
            chunkMapper.deleteById(chunk.getId());
        }
    }

    private AiKnowledgeDocumentVO createDocument(
            String title,
            String courseCategory,
            String knowledgePoint,
            String sourceType,
            String sourceName,
            String visibility,
            String accessScope,
            String content,
            SysUser user) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(courseCategory) || !StringUtils.hasText(content)) {
            throw new BusinessException(400, "标题、课程方向和文档内容不能为空");
        }
        String normalizedContent = normalizeContent(content);
        if (!StringUtils.hasText(normalizedContent)) {
            throw new BusinessException(400, "文档内容为空");
        }
        if (normalizedContent.length() > MAX_TEXT_LENGTH) {
            throw new BusinessException(400, "文档内容过长，请拆分后上传");
        }

        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setTitle(title.trim());
        document.setCourseCategory(courseCategory.trim());
        document.setKnowledgePoint(trimToNull(knowledgePoint));
        document.setSourceType(sourceType);
        document.setSourceName(sourceName);
        document.setVisibility(normalizeVisibility(visibility));
        document.setAccessScope(normalizeAccessScope(accessScope));
        document.setOwnerId(user.getId());
        document.setOwnerRole(user.getRole());
        document.setChunkCount(0);
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        document.setIsDeleted((byte) 0);
        documentMapper.insert(document);

        List<String> chunks = splitContent(normalizedContent);
        int index = 0;
        for (String chunkContent : chunks) {
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(index++);
            chunk.setCourseCategory(document.getCourseCategory());
            chunk.setKnowledgePoint(document.getKnowledgePoint());
            chunk.setContent(chunkContent);
            chunk.setSourceTitle(document.getTitle());
            chunk.setSourceName(document.getSourceName());
            chunk.setVisibility(document.getVisibility());
            chunk.setAccessScope(document.getAccessScope());
            chunk.setOwnerId(document.getOwnerId());
            chunk.setOwnerRole(document.getOwnerRole());
            chunk.setCreateTime(LocalDateTime.now());
            chunk.setIsDeleted((byte) 0);
            chunkMapper.insert(chunk);
        }

        document.setChunkCount(chunks.size());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
        return toVO(document);
    }

    private List<String> splitContent(String content) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            chunks.add(content.substring(start, end).trim());
            if (end >= content.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private String normalizeVisibility(String visibility) {
        return "PUBLIC".equalsIgnoreCase(visibility) ? "PUBLIC" : "PRIVATE";
    }

    private String normalizeAccessScope(String accessScope) {
        return "STUDENT_SAFE".equalsIgnoreCase(accessScope) ? "STUDENT_SAFE" : "TEACHER_ONLY";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean isTextLikeFile(String filename) {
        if (!StringUtils.hasText(filename)) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv");
    }

    private String stripExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "未命名文档";
        }
        int index = filename.lastIndexOf('.');
        return index > 0 ? filename.substring(0, index) : filename;
    }

    private void checkManagePermission(SysUser user) {
        checkUser(user);
        if (!"TEACHER".equalsIgnoreCase(user.getRole()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(403, "只有教师或管理员可以维护知识库");
        }
    }

    private void checkUser(SysUser user) {
        if (user == null || user.getId() == null || !StringUtils.hasText(user.getRole())) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
    }

    private AiKnowledgeDocumentVO toVO(AiKnowledgeDocument document) {
        AiKnowledgeDocumentVO vo = new AiKnowledgeDocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setCourseCategory(document.getCourseCategory());
        vo.setKnowledgePoint(document.getKnowledgePoint());
        vo.setSourceType(document.getSourceType());
        vo.setSourceName(document.getSourceName());
        vo.setVisibility(document.getVisibility());
        vo.setAccessScope(document.getAccessScope());
        vo.setOwnerId(document.getOwnerId());
        vo.setOwnerRole(document.getOwnerRole());
        vo.setChunkCount(document.getChunkCount());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());
        return vo;
    }
}
