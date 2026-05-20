package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.AiResultActionConfirmDTO;
import com.example.edubackend.dto.AiResultActionConfirmVO;
import com.example.edubackend.dto.AiResultActionPreviewDTO;
import com.example.edubackend.dto.AiResultActionPreviewVO;
import com.example.edubackend.dto.StudentReviewNoteVO;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IAiResultActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-actions")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "TEACHER", "ASSISTANT", "STUDENT"})
public class AiResultActionController {

    private final IAiResultActionService aiResultActionService;

    @PostMapping("/preview")
    public Result<AiResultActionPreviewVO> preview(@Valid @RequestBody AiResultActionPreviewDTO dto) {
        SysUser user = UserContext.getUser();
        return Result.success(aiResultActionService.preview(dto, user));
    }

    @PostMapping("/confirm")
    public Result<AiResultActionConfirmVO> confirm(@Valid @RequestBody AiResultActionConfirmDTO dto) {
        SysUser user = UserContext.getUser();
        return Result.success(aiResultActionService.confirm(dto, user));
    }

    @GetMapping("/review-notes")
    @RequireRole({"STUDENT"})
    public Result<List<StudentReviewNoteVO>> listMyReviewNotes() {
        SysUser user = UserContext.getUser();
        return Result.success(aiResultActionService.listMyReviewNotes(user));
    }
}
