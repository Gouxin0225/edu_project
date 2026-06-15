# Edu Platform 生产环境部署手册 (目录: /opt/edu-platform)

本文档提供将 Edu Platform 部署到 `/opt/edu-platform` 目录的完整命令。

## 1. 准备工作

### 1.1 系统依赖安装 (以 Ubuntu 为例)
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven nodejs npm nginx redis-server mysql-server git curl

# 安装 Node.js 20 (如果系统版本过低)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

### 1.2 创建部署目录
```bash
sudo mkdir -p /opt/edu-platform
sudo mkdir -p /opt/edu-platform/uploads
sudo chown -R $USER:$USER /opt/edu-platform
```

## 2. 放置代码
```bash
# 将项目代码拷贝到 /opt/edu-platform
# 如果使用 Git:
# git clone <repo_url> /opt/edu-platform
# 或者从当前目录拷贝:
cp -r . /opt/edu-platform/
cd /opt/edu-platform
```

## 3. 数据库初始化

### 3.1 创建数据库和用户
```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS edu_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'edu_app'@'localhost' IDENTIFIED BY 'EduApp@123';"
sudo mysql -e "GRANT ALL PRIVILEGES ON edu_db.* TO 'edu_app'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"
```

### 3.2 导入 SQL 脚本
```bash
cd /opt/edu-platform
mysql -uedu_app -pEduApp@123 edu_db < init_db.sql
mysql -uedu_app -pEduApp@123 edu_db < init_template.sql
mysql -uedu_app -pEduApp@123 edu_db < ai_chat_tables.sql
mysql -uedu_app -pEduApp@123 edu_db < ai_knowledge_tables.sql
mysql -uedu_app -pEduApp@123 edu_db < ai_knowledge_optimization.sql
mysql -uedu_app -pEduApp@123 edu_db < outline_exam_tables.sql
mysql -uedu_app -pEduApp@123 edu_db < ai_action_tables.sql
mysql -uedu_app -pEduApp@123 edu_db < student_visit_record_tables.sql
mysql -uedu_app -pEduApp@123 edu_db < exam_survey_session_fixes.sql
mysql -uedu_app -pEduApp@123 edu_db < must_change_password.sql
```

## 4. 后端部署 (edu-backend)

### 4.1 编译打包
```bash
cd /opt/edu-platform/edu-backend
mvn clean package -DskipTests
```

### 4.2 配置环境变量
创建配置文件 `/etc/edu-platform/edu-backend.env`:
```bash
sudo mkdir -p /etc/edu-platform
sudo tee /etc/edu-platform/edu-backend.env <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://127.0.0.1:3306/edu_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=edu_app
DB_PASSWORD=EduApp@123
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
JWT_SECRET=y0ur_super_secret_jwt_key_at_least_32_chars
FILE_UPLOAD_DIR=/opt/edu-platform/uploads
EOF
sudo chown root:root /etc/edu-platform/edu-backend.env
sudo chmod 600 /etc/edu-platform/edu-backend.env
```

### 4.3 创建 Systemd 服务
```bash
sudo tee /etc/systemd/system/edu-backend.service <<EOF
[Unit]
Description=Edu Platform Backend
After=network.target mysql.service redis.service

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/edu-platform/edu-backend
EnvironmentFile=/etc/edu-platform/edu-backend.env
ExecStart=/usr/bin/java -jar /opt/edu-platform/edu-backend/target/edu-backend-1.0.0.jar
Restart=always

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now edu-backend
```

## 5. 前端部署 (edu-frontend)

### 5.1 编译打包
```bash
cd /opt/edu-platform/edu-frontend
npm install
npm run build
```

### 5.2 配置 Nginx
创建 Nginx 配置文件 `/etc/nginx/sites-available/edu-platform`:
```bash
sudo tee /etc/nginx/sites-available/edu-platform <<EOF
server {
    listen 80;
    server_name _;

    root /opt/edu-platform/edu-frontend/dist;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location ~ ^/(api|auth|user|admin|teacher) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }
}
EOF

sudo ln -sf /etc/nginx/sites-available/edu-platform /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

## 6. 验证与运维

### 6.1 查看状态
```bash
systemctl status edu-backend
journalctl -u edu-backend -f
```

### 6.2 常用账号
- 管理员: admin / admin123
- 教师: teacher001 / password123
- 学生: student001 / password123
