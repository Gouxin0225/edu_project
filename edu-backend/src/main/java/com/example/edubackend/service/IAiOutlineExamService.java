package com.example.edubackend.service;

import com.example.edubackend.dto.OutlineConfirmDTO;
import com.example.edubackend.dto.OutlineConfirmVO;
import com.example.edubackend.dto.OutlineDocumentVO;
import com.example.edubackend.dto.OutlineQuestionUpdateDTO;
import com.example.edubackend.dto.OutlineQuestionVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IAiOutlineExamService {
    OutlineDocumentVO upload(MultipartFile file, String title, String courseCategory, Long creatorId);

    OutlineDocumentVO getDocument(Long id, Long userId, String role);

    void deleteDocument(Long id, Long userId, String role);

    List<OutlineQuestionVO> generateQuestions(Long id, Long userId, String role);

    List<OutlineQuestionVO> listQuestions(Long id, Long userId, String role);

    OutlineQuestionVO updateQuestion(Long questionId, OutlineQuestionUpdateDTO dto, Long userId, String role);

    OutlineConfirmVO confirm(Long id, OutlineConfirmDTO dto, Long userId, String role);
}
