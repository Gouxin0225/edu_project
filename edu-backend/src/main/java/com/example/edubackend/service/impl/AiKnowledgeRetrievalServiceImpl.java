package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.dto.AiKnowledgeChunkVO;
import com.example.edubackend.entity.AiKnowledgeChunk;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.mapper.AiKnowledgeChunkMapper;
import com.example.edubackend.service.IAiKnowledgeRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiKnowledgeRetrievalServiceImpl implements IAiKnowledgeRetrievalService {

    private static final int MAX_CANDIDATES = 80;
    private static final int MAX_CONTEXT_CHARS = 3600;

    private final AiKnowledgeChunkMapper chunkMapper;

    @Override
    public List<AiKnowledgeChunkVO> search(String question, String courseCategory, String knowledgePoint, SysUser user, int limit) {
        if (user == null || !StringUtils.hasText(question)) {
            return List.of();
        }

        int actualLimit = Math.max(1, Math.min(limit <= 0 ? 5 : limit, 8));
        List<String> keywords = extractKeywords(question, courseCategory, knowledgePoint);

        LambdaQueryWrapper<AiKnowledgeChunk> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(courseCategory)) {
            query.eq(AiKnowledgeChunk::getCourseCategory, courseCategory.trim());
        }
        if (StringUtils.hasText(knowledgePoint)) {
            query.and(wrapper -> wrapper
                    .like(AiKnowledgeChunk::getKnowledgePoint, knowledgePoint.trim())
                    .or()
                    .like(AiKnowledgeChunk::getContent, knowledgePoint.trim()));
        }

        // 学生只能检索公开知识；教师可检索公开知识和自己的私有知识，避免跨课程/跨所有者误读。
        if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            query.eq(AiKnowledgeChunk::getVisibility, "PUBLIC");
            query.eq(AiKnowledgeChunk::getAccessScope, "STUDENT_SAFE");
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            // 管理员知识库列表可见全部文档，检索范围保持一致。
        } else {
            query.and(wrapper -> wrapper.eq(AiKnowledgeChunk::getVisibility, "PUBLIC")
                    .or(owner -> owner.eq(AiKnowledgeChunk::getOwnerId, user.getId())
                            .eq(AiKnowledgeChunk::getOwnerRole, user.getRole())));
        }

        if (!keywords.isEmpty()) {
            query.and(wrapper -> {
                for (int i = 0; i < Math.min(keywords.size(), 8); i++) {
                    String keyword = keywords.get(i);
                    if (i == 0) {
                        wrapper.like(AiKnowledgeChunk::getContent, keyword);
                    } else {
                        wrapper.or().like(AiKnowledgeChunk::getContent, keyword);
                    }
                }
            });
        }
        query.orderByDesc(AiKnowledgeChunk::getCreateTime).last("LIMIT " + MAX_CANDIDATES);

        List<AiKnowledgeChunk> candidates = chunkMapper.selectList(query);
        return candidates.stream()
                .map(chunk -> toScoredVO(chunk, keywords, courseCategory, knowledgePoint))
                .filter(vo -> vo.getScore() > 0 || keywords.isEmpty())
                .sorted(Comparator.comparing(AiKnowledgeChunkVO::getScore).reversed()
                        .thenComparing(AiKnowledgeChunkVO::getChunkId))
                .limit(actualLimit)
                .map(new RefAssigner()::assign)
                .toList();
    }

    @Override
    public String buildRagContext(String question, String courseCategory, String knowledgePoint, SysUser user, int limit) {
        List<AiKnowledgeChunkVO> chunks = search(question, courseCategory, knowledgePoint, user, limit);
        if (chunks.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("【知识库召回】回答涉及下列片段时，请在对应句子末尾使用 [K编号] 引用来源。\n");
        int used = 0;
        for (AiKnowledgeChunkVO chunk : chunks) {
            String line = String.format(
                    "%s 来源:%s%s；课程:%s；知识点:%s；片段:%s\n",
                    chunk.getRef(),
                    chunk.getSourceTitle(),
                    StringUtils.hasText(chunk.getSourceName()) ? " / " + chunk.getSourceName() : "",
                    chunk.getCourseCategory(),
                    safeText(chunk.getKnowledgePoint()),
                    compact(chunk.getContent(), 520)
            );
            if (used + line.length() > MAX_CONTEXT_CHARS) {
                break;
            }
            builder.append(line);
            used += line.length();
        }
        return builder.toString();
    }

    private AiKnowledgeChunkVO toScoredVO(
            AiKnowledgeChunk chunk,
            List<String> keywords,
            String courseCategory,
            String knowledgePoint) {
        AiKnowledgeChunkVO vo = new AiKnowledgeChunkVO();
        vo.setChunkId(chunk.getId());
        vo.setDocumentId(chunk.getDocumentId());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent());
        vo.setSourceTitle(chunk.getSourceTitle());
        vo.setSourceName(chunk.getSourceName());
        vo.setCourseCategory(chunk.getCourseCategory());
        vo.setKnowledgePoint(chunk.getKnowledgePoint());
        vo.setAccessScope(chunk.getAccessScope());
        vo.setScore(score(chunk, keywords, courseCategory, knowledgePoint));
        return vo;
    }

    private int score(AiKnowledgeChunk chunk, List<String> keywords, String courseCategory, String knowledgePoint) {
        int score = 0;
        if (StringUtils.hasText(courseCategory) && courseCategory.equalsIgnoreCase(chunk.getCourseCategory())) {
            score += 10;
        }
        if (StringUtils.hasText(knowledgePoint) && contains(chunk.getKnowledgePoint(), knowledgePoint)) {
            score += 20;
        }
        String content = chunk.getContent() == null ? "" : chunk.getContent().toLowerCase();
        for (String keyword : keywords) {
            String lower = keyword.toLowerCase();
            if (content.contains(lower)) {
                score += Math.max(2, Math.min(12, lower.length()));
            }
            if (contains(chunk.getKnowledgePoint(), keyword)) {
                score += 8;
            }
            if (contains(chunk.getSourceTitle(), keyword)) {
                score += 4;
            }
        }
        return score;
    }

    private List<String> extractKeywords(String question, String courseCategory, String knowledgePoint) {
        Set<String> keywords = new LinkedHashSet<>();
        addKeyword(keywords, courseCategory);
        addKeyword(keywords, knowledgePoint);
        if (StringUtils.hasText(question)) {
            String normalized = question.replaceAll("[，。！？、；：,.!?;:()（）\\[\\]{}\"'`~@#$%^&*_+=|\\\\/<>\\s]+", " ");
            for (String part : normalized.split(" ")) {
                addKeyword(keywords, part);
            }
            for (int i = 0; i < question.length() - 1; i++) {
                String token = question.substring(i, Math.min(question.length(), i + 2)).trim();
                addKeyword(keywords, token);
            }
        }
        return new ArrayList<>(keywords);
    }

    private void addKeyword(Set<String> keywords, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String value = keyword.trim();
        if (value.length() < 2) {
            return;
        }
        keywords.add(value.length() > 30 ? value.substring(0, 30) : value);
    }

    private boolean contains(String text, String keyword) {
        return StringUtils.hasText(text)
                && StringUtils.hasText(keyword)
                && text.toLowerCase().contains(keyword.toLowerCase());
    }

    private String compact(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String compacted = value.replaceAll("\\s+", " ").trim();
        return compacted.length() <= maxLength ? compacted : compacted.substring(0, maxLength - 3) + "...";
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value : "未指定";
    }

    private static class RefAssigner {
        private int index = 1;

        private AiKnowledgeChunkVO assign(AiKnowledgeChunkVO vo) {
            vo.setRef("[K" + index++ + "]");
            return vo;
        }
    }
}
