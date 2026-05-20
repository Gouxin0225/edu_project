package com.example.edubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.dto.*;
import com.example.edubackend.entity.*;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.HomeworkSubmissionContentMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkServiceImpl implements IHomeworkService {

    private final IAssessmentTaskService assessmentTaskService;
    private final IStudentSubmissionService submissionService;
    private final IQuestionBankService questionBankService;
    private final HomeworkSubmissionContentMapper contentMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;
    private final SysUserMapper sysUserMapper;
    private final ClassStudentRelMapper classStudentRelMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long createHomework(CreateHomeworkDTO dto, Long teacherId) {
        assertCanTargetClasses(dto.getTargetClassIds(), teacherId);
        AssessmentTask homework = new AssessmentTask();
        homework.setTitle(dto.getTitle());
        homework.setType("HOMEWORK");
        homework.setEndTime(dto.getDeadline());
        homework.setCreatorId(teacherId);
        if (dto.getTargetClassIds() != null) {
            try {
                homework.setTargetClassIds(objectMapper.writeValueAsString(dto.getTargetClassIds()));
            } catch (Exception e) {
                throw new BusinessException(500, "Failed to serialize target class ids");
            }
        } else {
            homework.setTargetClassIds("[]");
        }
        
        Map<String, Object> setting = new HashMap<>();
        setting.put("content", dto.getContent());
        setting.put("attachments", dto.getAttachments());
        try {
            homework.setSettingJson(objectMapper.writeValueAsString(setting));
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to serialize homework settings");
        }
        
        assessmentTaskService.save(homework);
        return homework.getId();
    }

    @Override
    public List<HomeworkListVO> getTeacherHomeworkList() {
        LambdaQueryWrapper<AssessmentTask> query = new LambdaQueryWrapper<AssessmentTask>()
                .eq(AssessmentTask::getType, "HOMEWORK")
                .orderByDesc(AssessmentTask::getCreateTime);
        if (!"ADMIN".equals(UserContext.getUser().getRole())) {
            query.eq(AssessmentTask::getCreatorId, UserContext.getUserId());
        }
        List<AssessmentTask> homeworks = assessmentTaskService.list(query);

        List<Long> homeworkIds = homeworks.stream().map(AssessmentTask::getId).toList();
        Map<Long, List<StudentSubmission>> latestSubmissionsByTask = getLatestSubmissionsByTaskIds(homeworkIds, null);

        List<HomeworkListVO> result = new ArrayList<>();
        for (AssessmentTask hw : homeworks) {
            HomeworkListVO vo = new HomeworkListVO();
            vo.setHomeworkId(hw.getId());
            vo.setTitle(hw.getTitle());
            vo.setDeadline(hw.getEndTime());
            vo.setContent(readHomeworkContent(hw));

            List<StudentSubmission> submissions = latestSubmissionsByTask.getOrDefault(hw.getId(), List.of());

            int total = submissions.size();
            int graded = 0;
            int pending = 0;

            for (StudentSubmission s : submissions) {
                if ("GRADED".equals(s.getStatus())) {
                    graded++;
                } else if ("SUBMITTED".equals(s.getStatus()) || "UN".equals(s.getStatus())) {
                    pending++;
                }
            }

            vo.setTotalSubmissions(total);
            vo.setGradedSubmissions(graded);
            vo.setPendingSubmissions(pending);
            result.add(vo);
        }

        return result;
    }

    @Override
    public List<HomeworkListVO> getAssistantHomeworkList(Long assistantId) {
        List<Long> assistantClassIds = getAssistantClassIds(assistantId);
        if (assistantClassIds.isEmpty()) {
            return List.of();
        }

        List<AssessmentTask> homeworks = assessmentTaskService.list(
                new LambdaQueryWrapper<AssessmentTask>()
                        .eq(AssessmentTask::getType, "HOMEWORK")
                        .orderByDesc(AssessmentTask::getCreateTime)
        ).stream()
                .filter(homework -> isTaskRelevantToClasses(homework, assistantClassIds))
                .toList();

        List<HomeworkListVO> result = new ArrayList<>();
        for (AssessmentTask homework : homeworks) {
            List<Long> relevantClassIds = relevantClassIds(homework, assistantClassIds);
            List<SysUser> students = getStudentsInClasses(relevantClassIds);
            Set<Long> studentIds = students.stream().map(SysUser::getId).collect(java.util.stream.Collectors.toSet());
            List<StudentSubmission> submissions = getLatestSubmissions(homework.getId(), studentIds);

            HomeworkListVO vo = new HomeworkListVO();
            vo.setHomeworkId(homework.getId());
            vo.setTitle(homework.getTitle());
            vo.setDeadline(homework.getEndTime());
            vo.setContent(readHomeworkContent(homework));
            vo.setTotalSubmissions(submissions.size());
            vo.setGradedSubmissions((int) submissions.stream().filter(s -> "GRADED".equals(s.getStatus())).count());
            vo.setPendingSubmissions((int) submissions.stream()
                    .filter(s -> "SUBMITTED".equals(s.getStatus()) || "RETURNED".equals(s.getStatus()))
                    .count());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<StudentHomeworkVO> getStudentHomeworkList(Long studentId) {
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            return new ArrayList<>();
        }
        Set<Long> studentClassIds = getStudentClassIds(student);
        if (studentClassIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<AssessmentTask> homeworks = assessmentTaskService.list(
                new LambdaQueryWrapper<AssessmentTask>()
                        .eq(AssessmentTask::getType, "HOMEWORK")
                        .orderByDesc(AssessmentTask::getCreateTime)
        );

        List<AssessmentTask> availableHomeworks = homeworks.stream()
                .filter(hw -> isTaskTargetedToClasses(hw, studentClassIds))
                .toList();
        List<Long> homeworkIds = availableHomeworks.stream().map(AssessmentTask::getId).toList();
        Map<Long, StudentSubmission> latestSubmissionByTask = getLatestSubmissionByTaskIds(homeworkIds, studentId);
        Set<Long> submissionIds = latestSubmissionByTask.values().stream()
                .map(StudentSubmission::getId)
                .collect(Collectors.toSet());
        Map<Long, HomeworkSubmissionContent> contentBySubmissionId = loadContentBySubmissionIds(submissionIds);

        List<StudentHomeworkVO> result = new ArrayList<>();
        for (AssessmentTask hw : availableHomeworks) {
            StudentHomeworkVO vo = new StudentHomeworkVO();
            vo.setHomeworkId(hw.getId());
            vo.setTitle(hw.getTitle());
            vo.setDeadline(hw.getEndTime());
            vo.setContent(readHomeworkContent(hw));

            StudentSubmission submission = latestSubmissionByTask.get(hw.getId());
            if (submission != null) {
                vo.setSubmissionId(submission.getId());
                vo.setStatus(submission.getStatus());
                vo.setScoreGained(submission.getTotalScoreGained());
                vo.setSubmitTime(submission.getSubmitTime());
                vo.setVersion(submission.getVersion());

                HomeworkSubmissionContent content = contentBySubmissionId.get(submission.getId());
                if (content != null) {
                    vo.setContent(content.getContent());
                }
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    @Transactional
    public void submitHomework(Long homeworkId, Long studentId, SubmitHomeworkDTO dto) {
        AssessmentTask homework = assessmentTaskService.getById(homeworkId);
        if (homework == null) {
            throw new BusinessException(404, "作业不存在");
        }
        if (!"HOMEWORK".equals(homework.getType())) {
            throw new BusinessException(400, "任务不是作业");
        }

        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null || !isTaskTargetedToStudent(homework, student)) {
            throw new BusinessException(403, "无权提交该作业");
        }

        if (homework.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "作业已截止");
        }

        StudentSubmission existing = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, homeworkId)
                        .eq(StudentSubmission::getStudentId, studentId)
                        .orderByDesc(StudentSubmission::getVersion)
                        .last("LIMIT 1")
        );

        int newVersion = 1;
        Long parentId = null;

        if (existing != null) {
            if ("GRADED".equals(existing.getStatus())) {
                throw new BusinessException(400, "作业已评分，不能再次提交");
            }
            if ("RETURNED".equals(existing.getStatus())) {
                newVersion = existing.getVersion() + 1;
                parentId = existing.getId();
            } else {
                throw new BusinessException(400, "作业正在审核中或已提交");
            }
        }

        StudentSubmission submission = new StudentSubmission();
        submission.setTaskId(homeworkId);
        submission.setStudentId(studentId);
        submission.setStatus("SUBMITTED");
        submission.setVersion(newVersion);
        submission.setParentSubmissionId(parentId);
        submission.setSubmitTime(LocalDateTime.now());
        submission.setSwitchScreenCount(0);

        submissionService.save(submission);

        HomeworkSubmissionContent content = new HomeworkSubmissionContent();
        content.setSubmissionId(submission.getId());
        content.setContent(dto.getContent());
        content.setGitLink(dto.getGitLink());
        content.setFileUrl(dto.getFileUrl());
        content.setCreateTime(LocalDateTime.now());
        contentMapper.insert(content);
    }

    @Override
    @Transactional
    public void gradeHomework(Long homeworkId, Long submissionId, GradeHomeworkDTO dto) {
        assertCanManageHomework(homeworkId);
        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, homeworkId)
                        .eq(StudentSubmission::getId, submissionId)
        );

        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }

        if (!"SUBMITTED".equals(submission.getStatus()) && !"RETURNED".equals(submission.getStatus())) {
            throw new BusinessException(400, "只能批改已提交或被打回的作业");
        }

        submission.setTotalScoreGained(dto.getScoreGained());
        submission.setTeacherComment(dto.getTeacherComment());
        submission.setStatus("GRADED");
        submissionService.updateById(submission);
    }

    @Override
    @Transactional
    public void returnHomework(Long homeworkId, Long submissionId, ReturnHomeworkDTO dto) {
        assertCanManageHomework(homeworkId);
        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, homeworkId)
                        .eq(StudentSubmission::getId, submissionId)
        );

        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }

        if (!"SUBMITTED".equals(submission.getStatus())) {
            throw new BusinessException(400, "只能打回已提交的作业");
        }

        submission.setTeacherComment(dto.getTeacherComment());
        submission.setStatus("RETURNED");
        submissionService.updateById(submission);
    }

    @Override
    public List<SubmissionHistoryVO> getSubmissionHistory(Long homeworkId, Long submissionId) {
        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, homeworkId)
                        .eq(StudentSubmission::getId, submissionId)
        );

        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }

        List<SubmissionHistoryVO> result = new ArrayList<>();
        Long currentId = submission.getId();

        while (currentId != null) {
            StudentSubmission s = submissionService.getById(currentId);
            if (s == null) break;

            SubmissionHistoryVO vo = new SubmissionHistoryVO();
            vo.setSubmissionId(s.getId());
            vo.setVersion(s.getVersion());
            vo.setStatus(s.getStatus());
            vo.setScoreGained(s.getTotalScoreGained());
            vo.setTeacherComment(s.getTeacherComment());
            vo.setAssistantComment(s.getAssistantComment());
            vo.setSubmitTime(s.getSubmitTime());

            HomeworkSubmissionContent content = contentMapper.selectOne(
                    new LambdaQueryWrapper<HomeworkSubmissionContent>()
                            .eq(HomeworkSubmissionContent::getSubmissionId, s.getId())
            );
            if (content != null) {
                vo.setContent(content.getContent());
                vo.setGitLink(content.getGitLink());
                vo.setFileUrl(content.getFileUrl());
            }

            result.add(vo);
            currentId = s.getParentSubmissionId();
        }

        return result;
    }

    @Override
    public HomeworkProgressVO getHomeworkProgress(Long homeworkId, Long assistantId) {
        AssessmentTask homework = assessmentTaskService.getById(homeworkId);
        if (homework == null) {
            throw new BusinessException(404, "作业不存在");
        }

        List<Long> assistantClassIds = getAssistantClassIds(assistantId);
        if (assistantClassIds.isEmpty()) {
            throw new BusinessException(403, "您不是任何班级的班主任");
        }

        List<Long> homeworkTargetClassIds = parseTargetClassIds(homework.getTargetClassIds());
        List<Long> relevantClassIds = homeworkTargetClassIds.isEmpty() ? 
                assistantClassIds : 
                assistantClassIds.stream()
                        .filter(homeworkTargetClassIds::contains)
                        .toList();

        if (relevantClassIds.isEmpty() && !homeworkTargetClassIds.isEmpty()) {
            throw new BusinessException(403, "您无法查看此作业的进度");
        }

        List<SysUser> studentsInClass = getStudentsInClasses(relevantClassIds);

        List<StudentSubmission> submissions = getLatestSubmissions(
                homeworkId,
                studentsInClass.stream().map(SysUser::getId).collect(java.util.stream.Collectors.toSet())
        );

        Set<Long> submittedStudentIds = submissions.stream()
                .map(StudentSubmission::getStudentId)
                .collect(java.util.stream.Collectors.toSet());

        HomeworkProgressVO vo = new HomeworkProgressVO();
        vo.setHomeworkId(homeworkId);
        vo.setTitle(homework.getTitle());
        vo.setTotalStudents(studentsInClass.size());
        vo.setSubmittedCount((int) submittedStudentIds.size());
        vo.setPendingCount(studentsInClass.size() - submittedStudentIds.size());

        List<UnsubmittedStudentVO> unsubmittedList = studentsInClass.stream()
                .filter(s -> !submittedStudentIds.contains(s.getId()))
                .map(s -> {
                    UnsubmittedStudentVO uvo = new UnsubmittedStudentVO();
                    uvo.setStudentId(s.getId());
                    uvo.setStudentName(s.getRealName());
                    uvo.setUsername(s.getUsername());
                    return uvo;
                })
                .toList();

        vo.setUnsubmittedStudents(unsubmittedList);
        return vo;
    }

    @Override
    @Transactional
    public void remindStudents(Long homeworkId, Long assistantId, List<Long> studentIds) {
        assertAssistantCanAccessHomework(homeworkId, assistantId);
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }

        List<Long> assistantClassIds = getAssistantClassIds(assistantId);
        if (assistantClassIds.isEmpty()) {
            throw new BusinessException(403, "您不是任何班级的班主任");
        }

        List<SysUser> students = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getIsDeleted, 0)
                        .in(SysUser::getId, studentIds)
        );

        for (SysUser student : students) {
            if (!studentInAnyClass(student, assistantClassIds)) {
                throw new BusinessException(403, "您只能催交本班学生");
            }
        }

        List<StudentSubmission> submissions = submissionService.list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, homeworkId)
                        .in(!studentIds.isEmpty(), StudentSubmission::getStudentId, studentIds)
        );

        Set<Long> submittedStudentIds = submissions.stream()
                .map(StudentSubmission::getStudentId)
                .collect(java.util.stream.Collectors.toSet());

        List<Long> unsubmitted = studentIds.stream()
                .filter(id -> !submittedStudentIds.contains(id))
                .toList();

        if (!unsubmitted.isEmpty()) {
            log.info("班主任 {} 催交了作业 {} 的学生: {}", assistantId, homeworkId, unsubmitted);
        }
    }

    @Override
    @Transactional
    public void addSubmissionComment(Long submissionId, Long assistantId, String comment) {
        StudentSubmission submission = submissionService.getById(submissionId);
        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }

        Long studentId = submission.getStudentId();
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }

        List<Long> assistantClassIds = getAssistantClassIds(assistantId);
        if (!studentInAnyClass(student, assistantClassIds)) {
            throw new BusinessException(403, "您只能批改本班学生的作业");
        }

        String existingComment = submission.getAssistantComment();
        String newComment = existingComment == null ? 
                comment : 
                existingComment + "\n[" + LocalDateTime.now().toString() + "] " + comment;
        
        submission.setAssistantComment(newComment);
        submissionService.updateById(submission);
    }

    @Override
    public HomeworkSubmissionDetailVO getHomeworkSubmissionDetail(Long homeworkId, Long submissionId) {
        assertCanManageHomework(homeworkId);
        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, homeworkId)
                        .eq(StudentSubmission::getId, submissionId)
        );

        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }

        HomeworkSubmissionDetailVO vo = new HomeworkSubmissionDetailVO();
        vo.setSubmissionId(submission.getId());
        vo.setStudentId(submission.getStudentId());
        vo.setStatus(submission.getStatus());
        vo.setTotalScoreGained(submission.getTotalScoreGained());
        vo.setSubmitTime(submission.getSubmitTime());
        vo.setTeacherComment(submission.getTeacherComment());
        vo.setAssistantComment(submission.getAssistantComment());

        SysUser student = sysUserMapper.selectById(submission.getStudentId());
        if (student != null) {
            vo.setStudentName(student.getRealName());
        }

        HomeworkSubmissionContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<HomeworkSubmissionContent>()
                        .eq(HomeworkSubmissionContent::getSubmissionId, submissionId)
                        .last("LIMIT 1")
        );

        if (content != null) {
            vo.setContent(content.getContent());
            vo.setGitLink(content.getGitLink());
            vo.setFileUrl(content.getFileUrl());
        }

        return vo;
    }

    @Override
    public List<SubmissionListVO> getAssistantSubmissionList(Long homeworkId, Long assistantId) {
        AssessmentTask homework = assertAssistantCanAccessHomework(homeworkId, assistantId);
        List<Long> relevantClassIds = relevantClassIds(homework, getAssistantClassIds(assistantId));
        List<SysUser> students = getStudentsInClasses(relevantClassIds);
        Map<Long, SysUser> studentMap = students.stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, student -> student));
        List<StudentSubmission> submissions = getLatestSubmissions(homeworkId, studentMap.keySet());

        return submissions.stream()
                .sorted(Comparator.comparing(StudentSubmission::getSubmitTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(submission -> {
                    SubmissionListVO vo = new SubmissionListVO();
                    vo.setSubmissionId(submission.getId());
                    vo.setStudentId(submission.getStudentId());
                    SysUser student = studentMap.get(submission.getStudentId());
                    if (student != null) {
                        vo.setStudentName(student.getRealName());
                    }
                    vo.setStatus(submission.getStatus());
                    vo.setTotalScoreGained(submission.getTotalScoreGained());
                    vo.setScoreGained(submission.getTotalScoreGained());
                    vo.setSubmitTime(submission.getSubmitTime());
                    vo.setSwitchScreenCount(submission.getSwitchScreenCount());
                    vo.setAssistantComment(submission.getAssistantComment());
                    return vo;
                })
                .toList();
    }

    @Override
    public HomeworkSubmissionDetailVO getAssistantHomeworkSubmissionDetail(Long homeworkId, Long submissionId, Long assistantId) {
        assertAssistantCanAccessHomework(homeworkId, assistantId);
        StudentSubmission submission = submissionService.getOne(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, homeworkId)
                        .eq(StudentSubmission::getId, submissionId)
        );
        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }

        SysUser student = sysUserMapper.selectById(submission.getStudentId());
        if (student == null || !studentInAnyClass(student, getAssistantClassIds(assistantId))) {
            throw new BusinessException(403, "您只能查看本班学生的作业");
        }

        HomeworkSubmissionDetailVO vo = new HomeworkSubmissionDetailVO();
        vo.setSubmissionId(submission.getId());
        vo.setStudentId(submission.getStudentId());
        vo.setStudentName(student.getRealName());
        vo.setStatus(submission.getStatus());
        vo.setTotalScoreGained(submission.getTotalScoreGained());
        vo.setSubmitTime(submission.getSubmitTime());
        vo.setTeacherComment(submission.getTeacherComment());
        vo.setAssistantComment(submission.getAssistantComment());

        HomeworkSubmissionContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<HomeworkSubmissionContent>()
                        .eq(HomeworkSubmissionContent::getSubmissionId, submissionId)
                        .last("LIMIT 1")
        );
        if (content != null) {
            vo.setContent(content.getContent());
            vo.setGitLink(content.getGitLink());
            vo.setFileUrl(content.getFileUrl());
        }
        return vo;
    }

    private List<Long> getAssistantClassIds(Long assistantId) {
        List<TeacherClassRel> rels = teacherClassRelMapper.selectList(
                new LambdaQueryWrapper<TeacherClassRel>()
                        .eq(TeacherClassRel::getTeacherId, assistantId)
        );
        return rels.stream()
                .map(TeacherClassRel::getClassId)
                .collect(java.util.stream.Collectors.toList());
    }

    private AssessmentTask assertAssistantCanAccessHomework(Long homeworkId, Long assistantId) {
        AssessmentTask homework = assessmentTaskService.getById(homeworkId);
        if (homework == null) {
            throw new BusinessException(404, "作业不存在");
        }
        if (!"HOMEWORK".equals(homework.getType())) {
            throw new BusinessException(400, "任务不是作业");
        }

        List<Long> assistantClassIds = getAssistantClassIds(assistantId);
        if (assistantClassIds.isEmpty()) {
            throw new BusinessException(403, "您不是任何班级的班主任");
        }
        if (!isTaskRelevantToClasses(homework, assistantClassIds)) {
            throw new BusinessException(403, "您无法查看此作业");
        }
        return homework;
    }

    private boolean isTaskRelevantToClasses(AssessmentTask task, List<Long> classIds) {
        List<Long> targetClassIds = parseTargetClassIds(task.getTargetClassIds());
        return targetClassIds.isEmpty() || targetClassIds.stream().anyMatch(classIds::contains);
    }

    private List<Long> relevantClassIds(AssessmentTask task, List<Long> classIds) {
        List<Long> targetClassIds = parseTargetClassIds(task.getTargetClassIds());
        if (targetClassIds.isEmpty()) {
            return classIds;
        }
        return classIds.stream().filter(targetClassIds::contains).toList();
    }

    private List<SysUser> getStudentsInClasses(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        classIds = classIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        Set<Long> studentIds = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .in(ClassStudentRel::getClassId, classIds)
                        .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .stream()
                .map(ClassStudentRel::getStudentId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getIsDeleted, 0)
                        .in(SysUser::getClassId, classIds)
        ).forEach(student -> {
            if (student.getId() != null) {
                studentIds.add(student.getId());
            }
        });
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getIsDeleted, 0)
                .in(SysUser::getId, studentIds));
    }

    private List<StudentSubmission> getLatestSubmissions(Long homeworkId, Set<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        studentIds = studentIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (studentIds.isEmpty()) {
            return List.of();
        }
        List<StudentSubmission> submissions = submissionService.list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .eq(StudentSubmission::getTaskId, homeworkId)
                        .in(StudentSubmission::getStudentId, studentIds)
                        .orderByDesc(StudentSubmission::getVersion)
        );
        Map<Long, StudentSubmission> latestByStudent = new LinkedHashMap<>();
        for (StudentSubmission submission : submissions) {
            latestByStudent.putIfAbsent(submission.getStudentId(), submission);
        }
        return new ArrayList<>(latestByStudent.values());
    }

    private Map<Long, List<StudentSubmission>> getLatestSubmissionsByTaskIds(Collection<Long> homeworkIds, Set<Long> studentIds) {
        if (homeworkIds == null || homeworkIds.isEmpty()) {
            return Map.of();
        }
        List<Long> safeHomeworkIds = homeworkIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (safeHomeworkIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> safeStudentIds = studentIds == null ? null : studentIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (safeStudentIds != null && safeStudentIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<StudentSubmission> query = new LambdaQueryWrapper<StudentSubmission>()
                .in(StudentSubmission::getTaskId, safeHomeworkIds)
                .orderByDesc(StudentSubmission::getVersion);
        if (safeStudentIds != null) {
            query.in(StudentSubmission::getStudentId, safeStudentIds);
        }
        List<StudentSubmission> submissions = submissionService.list(query);
        Map<Long, Map<Long, StudentSubmission>> latestByTaskAndStudent = new LinkedHashMap<>();
        for (StudentSubmission submission : submissions) {
            if (submission.getTaskId() == null || submission.getStudentId() == null) {
                continue;
            }
            latestByTaskAndStudent
                    .computeIfAbsent(submission.getTaskId(), id -> new LinkedHashMap<>())
                    .merge(submission.getStudentId(), submission, this::newerSubmission);
        }

        Map<Long, List<StudentSubmission>> result = new HashMap<>();
        for (Map.Entry<Long, Map<Long, StudentSubmission>> entry : latestByTaskAndStudent.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }
        return result;
    }

    private Map<Long, StudentSubmission> getLatestSubmissionByTaskIds(Collection<Long> homeworkIds, Long studentId) {
        if (homeworkIds == null || homeworkIds.isEmpty()) {
            return Map.of();
        }
        List<Long> safeHomeworkIds = homeworkIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (safeHomeworkIds.isEmpty()) {
            return Map.of();
        }
        List<StudentSubmission> submissions = submissionService.list(
                new LambdaQueryWrapper<StudentSubmission>()
                        .in(StudentSubmission::getTaskId, safeHomeworkIds)
                        .eq(StudentSubmission::getStudentId, studentId)
                        .orderByDesc(StudentSubmission::getVersion)
        );
        Map<Long, StudentSubmission> latestByTask = new HashMap<>();
        for (StudentSubmission submission : submissions) {
            latestByTask.merge(submission.getTaskId(), submission, this::newerSubmission);
        }
        return latestByTask;
    }

    private Map<Long, HomeworkSubmissionContent> loadContentBySubmissionIds(Collection<Long> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return Map.of();
        }
        return contentMapper.selectList(new LambdaQueryWrapper<HomeworkSubmissionContent>()
                        .in(HomeworkSubmissionContent::getSubmissionId, submissionIds))
                .stream()
                .collect(Collectors.toMap(
                        HomeworkSubmissionContent::getSubmissionId,
                        content -> content,
                        (left, right) -> left
                ));
    }

    private StudentSubmission newerSubmission(StudentSubmission left, StudentSubmission right) {
        int leftVersion = left.getVersion() == null ? 0 : left.getVersion();
        int rightVersion = right.getVersion() == null ? 0 : right.getVersion();
        if (leftVersion != rightVersion) {
            return leftVersion > rightVersion ? left : right;
        }
        Long leftId = left.getId() == null ? 0L : left.getId();
        Long rightId = right.getId() == null ? 0L : right.getId();
        return leftId >= rightId ? left : right;
    }

    private String readHomeworkContent(AssessmentTask homework) {
        try {
            if (homework.getSettingJson() != null) {
                Map<String, Object> setting = objectMapper.readValue(homework.getSettingJson(),
                        new TypeReference<Map<String, Object>>() {});
                Object content = setting.get("content");
                return content == null ? null : content.toString();
            }
        } catch (Exception e) {
            log.error("Failed to parse homework settings", e);
        }
        return null;
    }

    private List<Long> parseTargetClassIds(String targetClassIdsJson) {
        if (targetClassIdsJson == null || targetClassIdsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(targetClassIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("Failed to parse target class ids", e);
            return new ArrayList<>();
        }
    }

    private boolean isTaskTargetedToStudent(AssessmentTask task, SysUser student) {
        return isTaskTargetedToClasses(task, getStudentClassIds(student));
    }

    private boolean isTaskTargetedToClasses(AssessmentTask task, Set<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return false;
        }
        List<Long> targetClassIds = parseTargetClassIds(task.getTargetClassIds());
        return targetClassIds.isEmpty() || targetClassIds.stream().anyMatch(classIds::contains);
    }

    private Set<Long> getStudentClassIds(SysUser student) {
        if (student == null) {
            return Set.of();
        }
        Set<Long> classIds = classStudentRelMapper.selectList(new LambdaQueryWrapper<ClassStudentRel>()
                        .eq(ClassStudentRel::getStudentId, student.getId())
                        .eq(ClassStudentRel::getStatus, "ACTIVE"))
                .stream()
                .map(ClassStudentRel::getClassId)
                .collect(java.util.stream.Collectors.toSet());
        if (student.getClassId() != null) {
            classIds.add(student.getClassId());
        }
        return classIds;
    }

    private boolean studentInAnyClass(SysUser student, List<Long> classIds) {
        if (student == null || classIds == null || classIds.isEmpty()) {
            return false;
        }
        return getStudentClassIds(student).stream().anyMatch(classIds::contains);
    }

    private void assertCanManageHomework(Long homeworkId) {
        AssessmentTask homework = assessmentTaskService.getById(homeworkId);
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

    private void assertCanTargetClasses(List<Long> targetClassIds, Long teacherId) {
        if ("ADMIN".equals(UserContext.getUser().getRole())) {
            return;
        }
        if (targetClassIds == null || targetClassIds.isEmpty()) {
            throw new BusinessException(400, "目标班级不能为空");
        }
        List<Long> allowedClassIds = getAssistantClassIds(teacherId);
        if (!allowedClassIds.containsAll(targetClassIds)) {
            throw new BusinessException(403, "只能发布到自己负责的班级");
        }
    }
}
