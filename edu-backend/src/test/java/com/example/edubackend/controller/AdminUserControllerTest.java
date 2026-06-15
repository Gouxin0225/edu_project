package com.example.edubackend.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edubackend.dto.StudentInfoVO;
import com.example.edubackend.entity.SysUser;
import com.example.edubackend.mapper.ClassStudentRelMapper;
import com.example.edubackend.mapper.SysClassMapper;
import com.example.edubackend.mapper.SysUserMapper;
import com.example.edubackend.mapper.TeacherClassRelMapper;
import com.example.edubackend.result.Result;
import com.example.edubackend.service.AuthTokenService;
import com.example.edubackend.service.OperationAuditLogService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class AdminUserControllerTest {

    static {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SysUser.class
        );
    }

    @Test
    void listUsersFiltersOutDeletedUsers() {
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        AdminUserController controller = newController(sysUserMapper);
        when(sysUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<SysUser> page = invocation.getArgument(0);
                    page.setTotal(0);
                    return page;
                });

        Result<IPage<StudentInfoVO>> result = controller.listUsers(1, 10, "TEACHER", null);

        assertThat(result.getData().getTotal()).isZero();
        ArgumentCaptor<LambdaQueryWrapper<SysUser>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(sysUserMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("is_deleted")
                .contains("role");
    }

    @Test
    void listUsersAppliesExactKeywordSearchAcrossIdentityFields() {
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        AdminUserController controller = newController(sysUserMapper);
        when(sysUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<SysUser> page = invocation.getArgument(0);
                    page.setTotal(0);
                    return page;
                });

        controller.listUsers(1, 10, null, " op123456 ");

        ArgumentCaptor<LambdaQueryWrapper<SysUser>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(sysUserMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("username")
                .contains("real_name")
                .contains("phone")
                .doesNotContain("LIKE");
    }

    @Test
    void getUserStatsUsesActiveUserScopeForEveryCount() {
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        AdminUserController controller = newController(sysUserMapper);
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(24L, 10L, 3L, 11L);

        Result<Map<String, Object>> result = controller.getUserStats();

        assertThat(result.getData())
                .containsEntry("total", 24L)
                .containsEntry("teachers", 10L)
                .containsEntry("assistants", 3L)
                .containsEntry("students", 11L);
        ArgumentCaptor<LambdaQueryWrapper<SysUser>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(sysUserMapper, times(4)).selectCount(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(wrapper -> assertThat(wrapper.getSqlSegment()).contains("is_deleted"));
    }

    @Test
    void sysUserMarksIsDeletedAsTableLogicField() throws NoSuchFieldException {
        Field field = SysUser.class.getDeclaredField("isDeleted");

        assertThat(field.getAnnotation(com.baomidou.mybatisplus.annotation.TableLogic.class)).isNotNull();
    }

    private AdminUserController newController(SysUserMapper sysUserMapper) {
        return new AdminUserController(
                sysUserMapper,
                mock(TeacherClassRelMapper.class),
                mock(SysClassMapper.class),
                mock(ClassStudentRelMapper.class),
                mock(BCryptPasswordEncoder.class),
                mock(AuthTokenService.class),
                mock(OperationAuditLogService.class)
        );
    }
}
