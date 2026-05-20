# Edu Platform 新服务器部署步骤

本文档说明如何把当前项目完整部署到一台新服务器。默认部署目录为 `/edu-platform`，后端使用 Spring Boot + Java 17，前端使用 Vite 构建后由 Nginx 托管，数据库使用 MySQL/MariaDB，缓存使用 Redis。

## 1. 准备信息

部署前先确定下面信息：

- 服务器公网 IP 或域名：`<your-domain-or-ip>`
- 部署目录：`/edu-platform`
- 数据库名：`edu_db`
- 数据库用户：`edu_app`
- 数据库密码：`<strong-db-password>`
- JWT 密钥：`<at-least-32-random-chars>`
- AI 接口 Key：`<ai-api-key>`，如果暂时不用 AI 功能可留空
- 上传目录：`/edu-platform/uploads`

建议使用 root 或具备 sudo 权限的用户执行部署。

## 2. 安装系统依赖

### Ubuntu / Debian

```bash
apt update
apt install -y openjdk-17-jdk maven nodejs npm nginx redis-server mysql-server git curl
systemctl enable --now mysql
systemctl enable --now redis-server
systemctl enable --now nginx
```

如果系统仓库里的 Node.js 版本过低，建议安装 Node.js 18 或 20：

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
```

### Rocky / AlmaLinux / CentOS / RHEL

```bash
dnf install -y java-17-openjdk java-17-openjdk-devel maven nodejs npm nginx redis mariadb-server git curl
systemctl enable --now mariadb
systemctl enable --now redis
systemctl enable --now nginx
```

确认版本：

```bash
java -version
mvn -version
node -v
npm -v
```

## 3. 放置项目代码

把项目复制到新服务器的 `/edu-platform`。可使用 Git，也可从旧服务器打包后传输。

使用 Git：

```bash
mkdir -p /edu-platform
git clone <your-repository-url> /edu-platform
cd /edu-platform
```

使用压缩包：

```bash
mkdir -p /edu-platform
tar -xf edu-platform.tar.gz -C /edu-platform --strip-components=1
cd /edu-platform
```

创建上传目录：

```bash
mkdir -p /edu-platform/uploads
```

## 4. 初始化数据库

登录 MySQL/MariaDB 并创建数据库和用户：

```bash
mysql -uroot -p
```

在 MySQL 控制台执行：

```sql
CREATE DATABASE IF NOT EXISTS edu_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'edu_app'@'localhost' IDENTIFIED BY '<strong-db-password>';
GRANT ALL PRIVILEGES ON edu_db.* TO 'edu_app'@'localhost';
FLUSH PRIVILEGES;
```

退出后按顺序导入项目 SQL：

```bash
cd /edu-platform
mysql -uroot -p edu_db --force < init_db.sql
mysql -uroot -p edu_db --force < init_template.sql
mysql -uroot -p edu_db --force < ai_chat_tables.sql
mysql -uroot -p edu_db --force < ai_knowledge_tables.sql
mysql -uroot -p edu_db --force < ai_knowledge_optimization.sql
mysql -uroot -p edu_db --force < outline_exam_tables.sql
mysql -uroot -p edu_db --force < ai_action_tables.sql
mysql -uroot -p edu_db --force < student_visit_record_tables.sql
mysql -uroot -p edu_db --force < exam_survey_session_fixes.sql
mysql -uroot -p edu_db --force < must_change_password.sql
mysql -uroot -p edu_db --force < assistant_test_account.sql
```

验证核心表：

```bash
mysql -uroot -p edu_db -e "SHOW TABLES;"
```

至少应看到 `sys_user`、`sys_class`、`assessment_task`、`survey_task`、`ai_chat_session`、`ai_knowledge_document`、`outline_document`、`question_paper_template`、`student_visit_record` 等表。

## 5. 配置后端环境变量

生产环境建议使用 `prod` profile，不要直接改 `application.yml`。创建独立环境变量文件：

```bash
mkdir -p /etc/edu-platform
install -m 600 /dev/null /etc/edu-platform/edu-backend.env
vi /etc/edu-platform/edu-backend.env
```

写入下面内容，并替换占位符：

```bash
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

DB_URL=jdbc:mysql://127.0.0.1:3306/edu_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&connectTimeout=30000&socketTimeout=30000&tcpKeepAlive=true
DB_USERNAME=edu_app
DB_PASSWORD=<strong-db-password>

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

JWT_SECRET=<at-least-32-random-chars>
JWT_EXPIRATION=28800000

APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://<your-domain-or-ip>,http://<your-domain-or-ip>
APP_LOG_LEVEL=info

FILE_UPLOAD_DIR=/edu-platform/uploads
FILE_ALLOWED_EXTENSIONS=pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,png,jpg,jpeg,zip,rar,7z
UPLOAD_MAX_FILE_SIZE=20MB
UPLOAD_MAX_REQUEST_SIZE=25MB

AI_API_KEY=<ai-api-key>
AI_API_ENDPOINT=https://api.deepseek.com/v1/chat/completions
AI_API_MODEL=deepseek-chat
AI_CHAT_CONTEXT_SIZE=10
```

如果使用 HTTPS 域名访问，`APP_CORS_ALLOWED_ORIGIN_PATTERNS` 应包含正式域名；如果先用 IP 测试，可以临时写 `http://服务器IP`。

## 6. 构建后端

```bash
cd /edu-platform/edu-backend
JAVA_HOME=/usr/lib/jvm/java-17-openjdk mvn clean package -DskipTests
```

不同系统 Java 路径可能不同，先用下面命令确认：

```bash
readlink -f "$(which java)"
```

构建成功后应存在：

```bash
/edu-platform/edu-backend/target/edu-backend-1.0.0.jar
```

## 7. 创建后端 systemd 服务

创建 `/etc/systemd/system/edu-backend.service`：

```bash
vi /etc/systemd/system/edu-backend.service
```

写入：

```ini
[Unit]
Description=Edu Platform Backend
After=network.target mariadb.service mysql.service redis.service redis-server.service

[Service]
Type=simple
User=root
WorkingDirectory=/edu-platform/edu-backend
EnvironmentFile=/etc/edu-platform/edu-backend.env
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk"
ExecStart=/usr/lib/jvm/java-17-openjdk/bin/java -jar /edu-platform/edu-backend/target/edu-backend-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

如果 Java 实际路径不是 `/usr/lib/jvm/java-17-openjdk`，同步修改 `JAVA_HOME` 和 `ExecStart`。

启动并设置开机自启：

```bash
systemctl daemon-reload
systemctl enable --now edu-backend
systemctl status edu-backend --no-pager -l
```

验证后端：

```bash
curl -I http://127.0.0.1:8080
```

返回 `401`、`404` 或其他 HTTP 响应都说明后端已监听；如果是 `Connection refused`，查看日志：

```bash
journalctl -u edu-backend -n 200 --no-pager
```

## 8. 构建前端

```bash
cd /edu-platform/edu-frontend
npm install
npm run build
```

构建成功后应生成：

```bash
/edu-platform/edu-frontend/dist
```

生产环境由 Nginx 代理 API，不需要运行 `npm run dev`。`npm run dev` 只用于本地开发或临时调试。

## 9. 配置 Nginx

创建 Nginx 配置。Ubuntu / Debian 常用路径：

```bash
vi /etc/nginx/sites-available/edu-platform
```

Rocky / AlmaLinux / CentOS / RHEL 可使用：

```bash
vi /etc/nginx/conf.d/edu-platform.conf
```

配置内容：

```nginx
server {
    listen 80;
    server_name <your-domain-or-ip>;

    client_max_body_size 25M;

    root /edu-platform/edu-frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /auth {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /user {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /admin {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /teacher {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Ubuntu / Debian 需要启用站点：

```bash
ln -sf /etc/nginx/sites-available/edu-platform /etc/nginx/sites-enabled/edu-platform
```

检查并重载 Nginx：

```bash
nginx -t
systemctl reload nginx
```

## 10. 配置权限、防火墙和 SELinux

设置上传目录权限：

```bash
mkdir -p /edu-platform/uploads
chmod 755 /edu-platform /edu-platform/uploads
```

如果 Nginx 或后端需要写上传目录，可按实际运行用户授权。例如后端以 root 运行时无需额外授权；如果改成普通用户运行，应执行：

```bash
chown -R <app-user>:<app-user> /edu-platform/uploads
```

开放 HTTP/HTTPS 端口。

Ubuntu / Debian：

```bash
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

Rocky / AlmaLinux / CentOS / RHEL：

```bash
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload
```

如果启用了 SELinux，并且 Nginx 无法访问静态文件或代理后端，可执行：

```bash
setsebool -P httpd_can_network_connect 1
semanage fcontext -a -t httpd_sys_content_t "/edu-platform/edu-frontend/dist(/.*)?"
restorecon -Rv /edu-platform/edu-frontend/dist
```

## 11. 验收

检查服务状态：

```bash
systemctl is-active mariadb || systemctl is-active mysql
systemctl is-active redis || systemctl is-active redis-server
systemctl is-active edu-backend
systemctl is-active nginx
```

检查端口：

```bash
ss -ltnp | grep -E ':80|:8080|:3306|:6379'
```

检查后端：

```bash
curl -I http://127.0.0.1:8080
```

检查前端：

```bash
curl -I http://127.0.0.1
curl -I http://<your-domain-or-ip>
```

浏览器访问：

```text
http://<your-domain-or-ip>
```

默认测试账号通常为：

- 管理员：`admin / admin123`
- 教师：`teacher001 / password123`
- 学生：`student001 / password123`
- 班主任：`assistant001 / password123`

首次登录后应及时修改默认密码。

## 12. 后续更新流程

更新代码后按下面顺序发布：

```bash
cd /edu-platform
git pull

cd /edu-platform/edu-backend
JAVA_HOME=/usr/lib/jvm/java-17-openjdk mvn clean package -DskipTests
systemctl restart edu-backend

cd /edu-platform/edu-frontend
npm install
npm run build
systemctl reload nginx
```

如果新增或修改了数据库脚本，先备份数据库，再按脚本说明执行迁移。

备份数据库：

```bash
mysqldump -uroot -p edu_db > /root/edu_db_$(date +%F_%H%M%S).sql
```

## 13. 常见问题排查

后端没有启动：

```bash
journalctl -u edu-backend -n 200 --no-pager
```

常见原因包括 `JWT_SECRET` 未配置、数据库账号密码错误、Redis 未启动、Java 路径不正确、数据库表未初始化。

前端页面能打开但接口失败：

```bash
curl -I http://127.0.0.1:8080
curl -I http://127.0.0.1/api
nginx -t
tail -n 100 /var/log/nginx/error.log
```

常见原因包括 Nginx 代理路径漏配、后端未监听 `8080`、CORS 域名不匹配。

上传失败：

```bash
ls -ld /edu-platform/uploads
journalctl -u edu-backend -n 100 --no-pager
```

常见原因包括上传目录不存在、权限不足、`client_max_body_size` 或 `UPLOAD_MAX_FILE_SIZE` 太小。
