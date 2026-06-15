# Edu Platform 手动启动指引

本文档基于当前机器上的实际启动过程整理，适用于在 `/edu-platform` 目录下手动启动数据库、Redis、后端和前端。

## 1. 服务信息

- 项目根目录：`/edu-platform`
- 数据库：MariaDB，服务名 `mariadb`
- Redis：服务名 `redis`
- 后端目录：`/edu-platform/edu-backend`
- 前端目录：`/edu-platform/edu-frontend`
- 后端地址：`http://127.0.0.1:8080`
- 前端地址：`http://127.0.0.1:3001`
- 数据库名：`edu_db`
- 数据库账号：`root`
- 数据库密码：`root`

> 本地默认值保留在 `application.yml` 中，便于开发启动。生产环境必须通过环境变量覆盖数据库密码、JWT 密钥、CORS 白名单等敏感配置。

## 2. 安全配置环境变量

生产或公网部署建议至少配置：

```bash
export SPRING_PROFILES_ACTIVE='prod'
export DB_URL='jdbc:mysql://127.0.0.1:3306/edu_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false'
export DB_USERNAME='edu_app'
export DB_PASSWORD='替换为强密码'
export REDIS_HOST='127.0.0.1'
export REDIS_PORT='6379'
export REDIS_PASSWORD='替换为Redis密码'
export JWT_SECRET='替换为至少32位随机密钥'
export APP_CORS_ALLOWED_ORIGIN_PATTERNS='https://你的前端域名'
export APP_LOG_LEVEL='info'
export FILE_UPLOAD_DIR='/edu-platform/uploads'
export FILE_ALLOWED_EXTENSIONS='pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,png,jpg,jpeg,zip,rar,7z'
export UPLOAD_MAX_FILE_SIZE='20MB'
export UPLOAD_MAX_REQUEST_SIZE='25MB'
```

开发环境如果仍通过 Vite 访问后端，默认 CORS 仅允许 `http://localhost:3001` 和 `http://127.0.0.1:3001`。

启用 `prod` profile 后，`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`JWT_SECRET`、`APP_CORS_ALLOWED_ORIGIN_PATTERNS` 缺失会导致后端启动失败，避免生产环境误用本地默认配置。

## 3. 推荐启动顺序

按下面顺序启动：

1. `mariadb`
2. `redis`
3. 后端 `edu-backend`
4. 前端 Vite 开发服务

## 4. 启动 MariaDB

启动命令：

```bash
systemctl start mariadb
```

校验命令：

```bash
systemctl is-active mariadb
mysql -uroot -proot -e "SHOW DATABASES;"
```

期望结果：

- `systemctl is-active mariadb` 返回 `active`
- `SHOW DATABASES` 结果中包含 `edu_db`

如果 `edu_db` 不存在，可先创建：

```bash
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS edu_db DEFAULT CHARACTER SET utf8mb4;"
```

## 5. 启动 Redis

启动命令：

```bash
systemctl start redis
```

校验命令：

```bash
systemctl is-active redis
redis-cli ping
```

期望结果：

- `systemctl is-active redis` 返回 `active`
- `redis-cli ping` 返回 `PONG`

## 6. 初始化数据库（仅首次或库为空时）

当前后端依赖的表不只来自 `init_db.sql`，还包含 AI 和大纲出题相关表。首次初始化建议按下面顺序执行：

```bash
mysql -uroot -proot edu_db --force < /edu-platform/init_db.sql
mysql -uroot -proot edu_db --force < /edu-platform/init_template.sql
mysql -uroot -proot edu_db --force < /edu-platform/ai_chat_tables.sql
mysql -uroot -proot edu_db --force < /edu-platform/ai_knowledge_tables.sql
mysql -uroot -proot edu_db --force < /edu-platform/ai_knowledge_optimization.sql
mysql -uroot -proot edu_db --force < /edu-platform/outline_exam_tables.sql
mysql -uroot -proot edu_db --force < /edu-platform/ai_action_tables.sql
mysql -uroot -proot edu_db --force < /edu-platform/student_visit_record_tables.sql
mysql -uroot -proot edu_db --force < /edu-platform/exam_survey_session_fixes.sql
mysql -uroot -proot edu_db --force < /edu-platform/must_change_password.sql
mysql -uroot -proot edu_db --force < /edu-platform/assistant_test_account.sql
```

校验表是否存在：

```bash
mysql -uroot -proot edu_db -e "SHOW TABLES;"
```

至少应能看到这些核心表：

- `sys_user`
- `sys_class`
- `assessment_task`
- `student_submission`
- `survey_task`
- `ai_chat_session`
- `ai_knowledge_document`
- `outline_document`
- `question_paper_template`
- `student_review_note`
- `student_visit_record`

## 7. 启动后端

### 方式 A：使用现成 systemd 服务

当前机器已存在 `edu-backend.service`，优先使用该方式：

```bash
systemctl start edu-backend
```

查看状态和日志：

```bash
systemctl status edu-backend
journalctl -u edu-backend -f
```

校验接口：

```bash
curl --noproxy '*' -I http://127.0.0.1:8080
```

期望结果：

- 服务状态为 `active (running)`
- `curl` 返回 `401` 或其他 HTTP 响应，说明后端已监听，不是连接失败

### 方式 B：手动构建并启动

如果 `edu-backend.service` 不可用，可直接手动启动：

```bash
cd /edu-platform/edu-backend
JAVA_HOME=/usr/lib/jvm/java-17-openjdk mvn clean package -DskipTests
JAVA_HOME=/usr/lib/jvm/java-17-openjdk java -jar target/edu-backend-1.0.0.jar
```

如果要后台运行：

```bash
cd /edu-platform/edu-backend
nohup /usr/lib/jvm/java-17-openjdk/bin/java -jar target/edu-backend-1.0.0.jar > /edu-platform/backend.log 2>&1 &
```

## 7. 启动前端

进入前端目录后启动 Vite：

```bash
cd /edu-platform/edu-frontend
npm run dev -- --host 0.0.0.0
```

如果需要后台运行：

```bash
cd /edu-platform/edu-frontend
setsid npm run dev -- --host 0.0.0.0 > frontend.log 2>&1 < /dev/null &
```

查看日志：

```bash
cd /edu-platform/edu-frontend
tail -f frontend.log
```

校验前端：

```bash
curl --noproxy '*' -I http://127.0.0.1:3001
```

期望结果：

- 返回 `HTTP/1.1 200 OK`

## 8. 联调校验

前端代理配置会把 `/auth`、`/user`、`/admin`、`/teacher`、`/api` 转发到后端 `8080`。

推荐使用统一冒烟测试脚本，脚本会固定绕过代理、设置连接超时，并校验后端、前端、前端代理和登录链路。登录账号必须显式指定，避免默认测试账号失效后产生误报：

```bash
cd /edu-platform
BACKEND_URL=http://127.0.0.1:8080 \
FRONTEND_URL=http://127.0.0.1:3001 \
LOGIN_USERNAME=<测试账号> \
LOGIN_PASSWORD=<测试密码> \
./scripts/system-smoke-test.sh
```

可直接验证代理链路：

```bash
curl --noproxy '*' -I http://127.0.0.1:3001/api
curl --noproxy '*' -I http://127.0.0.1:3001/auth/login
```

期望结果：

- `/api` 返回 `401 Unauthorized`
- `/auth/login` 使用 `GET` 时返回 `405 Method Not Allowed`

再做一次实际登录测试：

```bash
curl --noproxy '*' -H 'Content-Type: application/json' \
  -d '{"username":"teacher001","password":"password123"}' \
  http://127.0.0.1:3001/auth/login
```

期望结果：

- 返回 JSON
- `code` 为 `200`
- `data.token` 有值

## 9. 测试账号

项目中可见的测试账号如下：

- 教师：`teacher001 / password123`
- 学生：`student001 / password123`
- 管理员：`admin / admin123`
- 班主任：`assistant001 / password123`

## 10. 停止服务

停止数据库和后端：

```bash
systemctl stop edu-backend
systemctl stop redis
systemctl stop mariadb
```

停止前端：

```bash
pkill -f "vite --host 0.0.0.0"
```

## 11. 本次实际验证结果

本次已实际验证通过：

- `mariadb` 已启动并可访问 `edu_db`
- `redis` 已启动并返回 `PONG`
- `edu-backend.service` 已成功启动并监听 `8080`
- 前端 Vite 已成功启动并监听 `3001`
- `http://127.0.0.1:3001/auth/login` 已成功代理到后端，并能用 `teacher001 / password123` 登录
