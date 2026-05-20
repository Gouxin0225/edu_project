package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.OutlineConfirmDTO;
import com.example.edubackend.dto.OutlineConfirmVO;
import com.example.edubackend.dto.OutlineDocumentVO;
import com.example.edubackend.dto.OutlineQuestionUpdateDTO;
import com.example.edubackend.dto.OutlineQuestionVO;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IAiOutlineExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/outline")
@RequiredArgsConstructor
public class AiOutlineExamController {

    private final IAiOutlineExamService outlineExamService;

    @PostMapping("/upload")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<OutlineDocumentVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseCategory") String courseCategory,
            @RequestParam(value = "title", required = false) String title) {
        return Result.success(outlineExamService.upload(file, title, courseCategory, UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<OutlineDocumentVO> getDocument(@PathVariable Long id) {
        return Result.success(outlineExamService.getDocument(id, UserContext.getUserId(), UserContext.getUser().getRole()));
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> deleteDocument(@PathVariable Long id) {
        outlineExamService.deleteDocument(id, UserContext.getUserId(), UserContext.getUser().getRole());
        return Result.success("大纲已删除");
    }

    @PostMapping("/{id}/generate-questions")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<List<OutlineQuestionVO>> generateQuestions(@PathVariable Long id) {
        return Result.success(outlineExamService.generateQuestions(id, UserContext.getUserId(), UserContext.getUser().getRole()));
    }

    @GetMapping("/{id}/questions")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<List<OutlineQuestionVO>> listQuestions(@PathVariable Long id) {
        return Result.success(outlineExamService.listQuestions(id, UserContext.getUserId(), UserContext.getUser().getRole()));
    }

    @PutMapping("/questions/{questionId}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<OutlineQuestionVO> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody OutlineQuestionUpdateDTO dto) {
        return Result.success(outlineExamService.updateQuestion(questionId, dto, UserContext.getUserId(), UserContext.getUser().getRole()));
    }

    @PostMapping("/{id}/confirm")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<OutlineConfirmVO> confirm(
            @PathVariable Long id,
            @RequestBody(required = false) OutlineConfirmDTO dto) {
        return Result.success(outlineExamService.confirm(id, dto, UserContext.getUserId(), UserContext.getUser().getRole()));
    }
}
