package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edubackend.annotation.RequireRole;
import com.example.edubackend.context.UserContext;
import com.example.edubackend.entity.AssessmentTask;
import com.example.edubackend.entity.HomeworkSubmissionContent;
import com.example.edubackend.entity.StudentSubmission;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.entity.TeacherClassRel;
import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.HomeworkSubmissionContentMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final HomeworkSubmissionContentMapper homeworkContentMapper;
    private final StudentSubmissionMapper studentSubmissionMapper;
    private final AssessmentTaskMapper assessmentTaskMapper;
    private final SysUserMapper sysUserMapper;
    private final TeacherClassRelMapper teacherClassRelMapper;

    @Value("${file.upload-dir:/edu-platform/uploads}")
    private String uploadDir;

    @Value("${file.allowed-extensions:pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,png,jpg,jpeg,zip,rar,7z}")
    private String allowedExtensions;

    @PostMapping("/upload")
    @RequireRole({"STUDENT"})
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        
        Long studentId = UserContext.getUserId();
        String originalFilename = file.getOriginalFilename();
        String ext = getSafeExtension(originalFilename);
        if (ext == null) {
            throw new BusinessException(400, "不支持的文件类型");
        }
        String newFilename = studentId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path targetPath = uploadPath.resolve(newFilename);
            if (!targetPath.normalize().startsWith(uploadPath.normalize())) {
                throw new BusinessException(400, "文件路径非法");
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件上传成功: {} -> {}", originalFilename, newFilename);
            return Result.success("上传成功", newFilename);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage());
            return Result.error("文件上传失败");
        }
    }

    @GetMapping("/download")
    @RequireRole({"ADMIN", "TEACHER", "STUDENT", "ASSISTANT"})
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest().build();
            }
            if (!canDownloadFile(filename)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                log.warn("文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            String lowerFilename = filename.toLowerCase(Locale.ROOT);
            if (lowerFilename.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (lowerFilename.endsWith(".doc") || lowerFilename.endsWith(".docx")) {
                contentType = "application/msword";
            } else if (lowerFilename.endsWith(".xls") || lowerFilename.endsWith(".xlsx")) {
                contentType = "application/vnd.ms-excel";
            } else if (lowerFilename.endsWith(".txt")) {
                contentType = "text/plain";
            } else if (lowerFilename.endsWith(".md")) {
                contentType = "text/markdown";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(resource.getFilename(), StandardCharsets.UTF_8)
                            .build()
                            .toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("文件路径错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("文件读取错误: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean canDownloadFile(String filename) {
        SysUser user = UserContext.getUser();
        if (user == null || user.getId() == null || user.getRole() == null) {
            return false;
        }
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        List<HomeworkSubmissionContent> contents = homeworkContentMapper.selectList(
                new LambdaQueryWrapper<HomeworkSubmissionContent>()
                        .eq(HomeworkSubmissionContent::getFileUrl, filename)
        );

        if (contents.isEmpty()) {
            return "STUDENT".equals(user.getRole()) && filename.startsWith(user.getId() + "_");
        }

        for (HomeworkSubmissionContent content : contents) {
            StudentSubmission submission = studentSubmissionMapper.selectById(content.getSubmissionId());
            if (submission == null) {
                continue;
            }
            if ("STUDENT".equals(user.getRole()) && user.getId().equals(submission.getStudentId())) {
                return true;
            }

            AssessmentTask task = assessmentTaskMapper.selectById(submission.getTaskId());
            if ("TEACHER".equals(user.getRole()) && task != null && user.getId().equals(task.getCreatorId())) {
                return true;
            }

            if ("ASSISTANT".equals(user.getRole()) && isAssistantForStudent(user.getId(), submission.getStudentId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAssistantForStudent(Long assistantId, Long studentId) {
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null || student.getClassId() == null) {
            return false;
        }
        Long count = teacherClassRelMapper.selectCount(
                new LambdaQueryWrapper<TeacherClassRel>()
                        .eq(TeacherClassRel::getTeacherId, assistantId)
                        .eq(TeacherClassRel::getClassId, student.getClassId())
        );
        return count != null && count > 0;
    }

    private String getSafeExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        String normalized = originalFilename.replace("\\", "/");
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            return null;
        }

        String ext = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensionSet().contains(ext)) {
            log.warn("拒绝上传不支持的文件类型: {}", ext);
            return null;
        }
        return "." + ext;
    }

    private Set<String> allowedExtensionSet() {
        return Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.startsWith(".") ? value.substring(1) : value)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
