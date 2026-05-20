package com.example.edubackend.service;

import com.example.edubackend.dto.*;
import com.example.edubackend.entity.StudentSubmission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IStudentSubmissionService extends IService<StudentSubmission> {

    List<SubmissionListVO> getSubmissionList(Long examId);

    SubmissionDetailVO getSubmissionDetail(Long examId, Long submissionId);

    void gradeSubmission(Long examId, Long submissionId, List<GradeItemDTO> gradeItems);

    void publishScore(Long examId);

    List<StudentExamRecordVO> getStudentExamRecords(Long studentId);

    StudentExamResultVO getStudentExamResult(Long examId, Long studentId);

    List<StudentExamListVO> getStudentExamList(Long studentId);

    List<ScoreExportDTO> getExportData(Long examId);

    ExamParticipationVO getExamParticipation(Long examId);
}
