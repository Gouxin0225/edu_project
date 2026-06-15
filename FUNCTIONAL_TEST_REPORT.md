# Edu Platform 综合功能测试报告 (2026-06-02 实时测试)

## 1. 测试概览
- **执行时间**: 2026年6月2日
- **测试范围**: 后端 JUnit 测试、系统冒烟测试、功能审计报告合并
- **总体结论**: **通过 (PASS)**。核心服务稳定，业务逻辑链路通畅。

## 2. 后端单元与集成测试 (JUnit)
执行命令: `cd edu-backend && JAVA_HOME=/usr/lib/jvm/java-17-openjdk mvn test`

**测试结果**:
- Tests run: 13
- Failures: 0
- Errors: 0
- Skipped: 0
- 状态: **BUILD SUCCESS**

**涵盖模块**:
- `SurveyServiceImpl`: 问卷逻辑校验
- `ExamTakingServiceImpl`: 考试提交与评分逻辑
- `StudentServiceImpl`: 学生看板与任务统计
- `AdminUserController`: 用户管理接口
- `AdminStatisticsController`: 系统全局统计接口
- `TeacherStatisticsController`: 教师维度数据统计

## 3. 系统冒烟测试 (Smoke Test)
执行命令: `./scripts/system-smoke-test.sh`

**测试结果**:
- [PASS] backend -> HTTP 401 (符合预期，未携带 Token)
- [PASS] frontend -> HTTP 200 (前端静态资源可用)
- [PASS] frontend api proxy -> HTTP 401 (API 代理链路通畅)
- [PASS] frontend login get proxy -> HTTP 405 (方法拦截正确)
- [PASS] login proxy -> HTTP 200, token present (登录及 Token 分发正常)

## 4. 详细功能审计结果 (基于现有报告)

### 4.1 角色与权限
- **管理员 (Admin)**: 拥有全局用户、班级、统计查看权限。
- **讲师 (Teacher)**: 拥有所教班级、题库、考试、作业、问卷、模板管理权限。
- **学生 (Student)**: 拥有看板、任务提交、成绩查询、错题本、申请加入班级权限。
- **班主任 (Assistant)**: 拥有班级申请审核、学生访问记录查看权限。

### 4.2 核心业务流程
- **考试流程**: 教师发布 -> 学生参加 -> 自动/手动评分 -> 成绩归档。
- **问卷流程**: 教师发布 -> 学生匿名/实名填写 -> 统计分析。
- **学习辅助**: 错题本自动收集，支持 AI 知识库关联。

### 4.3 发现的问题与优化建议
1. **统计口径**: 建议在查询中统一显式过滤 `is_deleted=0`。
2. **任务状态**: 学生看板建议细化“已提交”与“待完成”的视觉区分。
3. **前端优化**: 建议对 Element Plus 和 Echarts 进行按需加载以减小包体积。

---
*本报告由 Gemini CLI 自动化测试工具生成并存放在 /opt/FUNCTIONAL_TEST_REPORT.md*
