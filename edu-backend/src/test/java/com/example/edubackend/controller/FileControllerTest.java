package com.example.edubackend.controller;

import com.example.edubackend.exception.BusinessException;
import com.example.edubackend.mapper.AssessmentTaskMapper;
import com.example.edubackend.mapper.HomeworkSubmissionContentMapper;
import com.example.edubackend.mapper.StudentSubmissionMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FileControllerTest {

    @Test
    void uploadFileRejectsUnsupportedExtensionAsBadRequest() {
        FileController controller = new FileController(
                mock(HomeworkSubmissionContentMapper.class),
                mock(StudentSubmissionMapper.class),
                mock(AssessmentTaskMapper.class),
                mock(SysUserMapper.class),
                mock(TeacherClassRelMapper.class)
        );
        ReflectionTestUtils.setField(controller, "allowedExtensions", "pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,png,jpg,jpeg,zip,rar,7z");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> controller.uploadFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不支持的文件类型")
                .extracting("code")
                .isEqualTo(400);
    }
}
