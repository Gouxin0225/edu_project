package com.example.edubackend.controller;

import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.*;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.IHomeworkService;
import com.example.edubackend.service.IStudentSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeworkController {

    private final IHomeworkService homeworkService;
    private final IStudentSubmissionService submissionService;
    private final AssessmentTaskMapper assessmentTaskMapper;

    @PostMapping("/homework")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Long> createHomework(@Valid @RequestBody CreateHomeworkDTO dto) {
        Long teacherId = UserContext.getUserId();
        Long homeworkId = homeworkService.createHomework(dto, teacherId);
        return Result.success(homeworkId);
    }

    @GetMapping("/homework")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<List<HomeworkListVO>> getTeacherHomeworkList() {
        List<HomeworkListVO> list = homeworkService.getTeacherHomeworkList();
        return Result.success(list);
    }

    @GetMapping("/student/homework")
    @RequireRole({"STUDENT"})
    public Result<List<StudentHomeworkVO>> getStudentHomeworkList() {
        Long studentId = UserContext.getUserId();
        List<StudentHomeworkVO> list = homeworkService.getStudentHomeworkList(studentId);
        return Result.success(list);
    }

    @GetMapping("/assistant/homework")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<List<HomeworkListVO>> getAssistantHomeworkList() {
        Long assistantId = UserContext.getUserId();
        List<HomeworkListVO> list = homeworkService.getAssistantHomeworkList(assistantId);
        return Result.success(list);
    }

    @PostMapping("/homework/{id}/submit")
    @RequireRole({"STUDENT"})
    public Result<Void> submitHomework(@PathVariable Long id, @Valid @RequestBody SubmitHomeworkDTO dto) {
        Long studentId = UserContext.getUserId();
        homeworkService.submitHomework(id, studentId, dto);
        return Result.success("提交成功");
    }

    @PutMapping("/homework/{id}/submission/{sid}/grade")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> gradeHomework(
            @PathVariable Long id,
            @PathVariable Long sid,
            @Valid @RequestBody GradeHomeworkDTO dto) {
        homeworkService.gradeHomework(id, sid, dto);
        return Result.success("批改完成");
    }

    @PutMapping("/homework/{id}/submission/{sid}/return")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> returnHomework(
            @PathVariable Long id,
            @PathVariable Long sid,
            @Valid @RequestBody ReturnHomeworkDTO dto) {
        homeworkService.returnHomework(id, sid, dto);
        return Result.success("作业已打回");
    }

    @GetMapping("/homework/{id}/submission/{sid}/history")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT", "STUDENT"})
    public Result<List<SubmissionHistoryVO>> getSubmissionHistory(
            @PathVariable Long id,
            @PathVariable Long sid) {
        assertCanViewSubmission(id, sid);
        List<SubmissionHistoryVO> history = homeworkService.getSubmissionHistory(id, sid);
        return Result.success(history);
    }

    @GetMapping("/assistant/homework/{id}/progress")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<HomeworkProgressVO> getHomeworkProgress(@PathVariable Long id) {
        Long assistantId = UserContext.getUserId();
        HomeworkProgressVO progress = homeworkService.getHomeworkProgress(id, assistantId);
        return Result.success(progress);
    }

    @GetMapping("/homework/{id}/submissions")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<List<SubmissionListVO>> getHomeworkSubmissions(@PathVariable Long id) {
        assertCanManageHomework(id);
        List<SubmissionListVO> list = submissionService.getSubmissionList(id);
        return Result.success(list);
    }

    @GetMapping("/homework/{id}/submission/{sid}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<HomeworkSubmissionDetailVO> getHomeworkSubmissionDetail(
            @PathVariable Long id,
            @PathVariable Long sid) {
        assertCanManageHomework(id);
        HomeworkSubmissionDetailVO detail = homeworkService.getHomeworkSubmissionDetail(id, sid);
        return Result.success(detail);
    }

    @DeleteMapping("/homework/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<Void> deleteHomework(@PathVariable Long id) {
        assertCanManageHomework(id);
        assessmentTaskMapper.deleteById(id);
        return Result.success("作业已删除");
    }

    @GetMapping("/assistant/homework/{id}/submissions")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<List<SubmissionListVO>> getAssistantHomeworkSubmissions(@PathVariable Long id) {
        Long assistantId = UserContext.getUserId();
        List<SubmissionListVO> list = homeworkService.getAssistantSubmissionList(id, assistantId);
        return Result.success(list);
    }

    @GetMapping("/assistant/homework/{id}/submission/{sid}")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<HomeworkSubmissionDetailVO> getAssistantHomeworkSubmissionDetail(
            @PathVariable Long id,
            @PathVariable Long sid) {
        Long assistantId = UserContext.getUserId();
        HomeworkSubmissionDetailVO detail = homeworkService.getAssistantHomeworkSubmissionDetail(id, sid, assistantId);
        return Result.success(detail);
    }

    @PostMapping("/assistant/homework/{id}/remind")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Void> remindStudents(
            @PathVariable Long id,
            @Valid @RequestBody RemindStudentsDTO dto) {
        Long assistantId = UserContext.getUserId();
        homeworkService.remindStudents(id, assistantId, dto.getStudentIds());
        return Result.success("催交成功");
    }

    @PostMapping("/assistant/submission/{sid}/comment")
    @RequireRole({"ADMIN", "TEACHER", "ASSISTANT"})
    public Result<Void> addSubmissionComment(
            @PathVariable Long sid,
            @Valid @RequestBody AddCommentDTO dto) {
        Long assistantId = UserContext.getUserId();
        homeworkService.addSubmissionComment(sid, assistantId, dto.getComment());
        return Result.success("批注已添加");
    }

    private void assertCanManageHomework(Long homeworkId) {
        AssessmentTask homework = assessmentTaskMapper.selectById(homeworkId);
        if (homework == null) {
            throw new BusinessException(404, "作业不存在");
        }
        if (!"HOMEWORK".equals(homework.getType())) {
            throw new BusinessException(400, "任务不是作业");
        }
        String role = UserContext.getUser().getRole();
        if (!"ADMIN".equals(role) && !homework.getCreatorId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "只能管理自己创建的作业");
        }
    }

    private void assertCanViewSubmission(Long homeworkId, Long submissionId) {
        String role = UserContext.getUser().getRole();
        if ("STUDENT".equals(role)) {
            StudentSubmission submission = submissionService.getById(submissionId);
            if (submission == null || !homeworkId.equals(submission.getTaskId())
                    || !UserContext.getUserId().equals(submission.getStudentId())) {
                throw new BusinessException(403, "只能查看自己的提交记录");
            }
            return;
        }
        if ("ASSISTANT".equals(role)) {
            homeworkService.getAssistantHomeworkSubmissionDetail(homeworkId, submissionId, UserContext.getUserId());
            return;
        }
        assertCanManageHomework(homeworkId);
    }
}
