package com.example.edubackend.service;

import com.example.edubackend.dto.*;

import java.util.List;

public interface IHomeworkService {

    Long createHomework(CreateHomeworkDTO dto, Long teacherId);

    List<HomeworkListVO> getTeacherHomeworkList();

    List<HomeworkListVO> getAssistantHomeworkList(Long assistantId);

    List<StudentHomeworkVO> getStudentHomeworkList(Long studentId);

    void submitHomework(Long homeworkId, Long studentId, SubmitHomeworkDTO dto);

    void gradeHomework(Long homeworkId, Long submissionId, GradeHomeworkDTO dto);

    void returnHomework(Long homeworkId, Long submissionId, ReturnHomeworkDTO dto);

    List<SubmissionHistoryVO> getSubmissionHistory(Long homeworkId, Long submissionId);

    HomeworkProgressVO getHomeworkProgress(Long homeworkId, Long assistantId);

    void remindStudents(Long homeworkId, Long assistantId, List<Long> studentIds);

    void addSubmissionComment(Long submissionId, Long assistantId, String comment);

    HomeworkSubmissionDetailVO getHomeworkSubmissionDetail(Long homeworkId, Long submissionId);

    List<SubmissionListVO> getAssistantSubmissionList(Long homeworkId, Long assistantId);

    HomeworkSubmissionDetailVO getAssistantHomeworkSubmissionDetail(Long homeworkId, Long submissionId, Long assistantId);
}
