package com.example.edubackend.service;

import com.example.edubackend.dto.AiResultActionConfirmDTO;
import com.example.edubackend.dto.AiResultActionConfirmVO;
import com.example.edubackend.dto.AiResultActionPreviewDTO;
import com.example.edubackend.dto.AiResultActionPreviewVO;
import com.example.edubackend.dto.StudentReviewNoteVO;
import com.example.edubackend.entity.SysUser;

import java.util.List;

public interface IAiResultActionService {

    String ACTION_CREATE_QUESTION = "CREATE_QUESTION";
    String ACTION_CREATE_REVIEW_NOTE = "CREATE_REVIEW_NOTE";

    AiResultActionPreviewVO preview(AiResultActionPreviewDTO dto, SysUser user);

    AiResultActionConfirmVO confirm(AiResultActionConfirmDTO dto, SysUser user);

    List<StudentReviewNoteVO> listMyReviewNotes(SysUser user);
}
