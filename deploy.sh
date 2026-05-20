#!/bin/bash

set -e

# ============================================
# edu-platform 自动化部署脚本
# ============================================

# 配置变量
DB_NAME="edu_db"
DB_USER="edu"
DB_PASS="openlab123"
APP_DIR="/edu-platform"
BACKEND_DIR="$APP_DIR/edu-backend"
FRONTEND_DIR="$APP_DIR/edu-frontend"
UPLOAD_DIR="$APP_DIR/uploads"
LOG_FILE="/var/log/edu-deploy.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

# 检查是否为 root 用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        error "请使用 root 用户运行此脚本"
    fi
}

# 1. 安装系统依赖
install_dependencies() {
    log "安装系统依赖..."
    
    # 更新 apt
    apt update -qq
    
    # 安装 Java 17
    apt install -y openjdk-17-jdk > /dev/null 2>&1
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
    
    # 安装 Node.js 18
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - > /dev/null 2>&1
    apt install -y nodejs > /dev/null 2>&1
    
    # 安装 MySQL
    apt install -y mysql-server > /dev/null 2>&1
    
    # 安装 Nginx
    apt install -y nginx > /dev/null 2>&1
    
    # 安装 Maven
    apt install -y maven > /dev/null 2>&1
    
    log "系统依赖安装完成"
}

# 2. 配置 MySQL
setup_mysql() {
    log "配置 MySQL..."
    
    # 启动 MySQL 服务
    systemctl start mysql
    systemctl enable mysql
    
    # 设置 root 密码并创建数据库和用户
    mysql << EOF 2>/dev/null || true
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
EOF
    
    # 如果上面的方法不行，尝试另一种方式
    if ! mysql -u root -p"$DB_PASS" -e "SELECT 1" > /dev/null 2>&1; then
        warn "尝试使用默认方式配置 MySQL..."
        mysql << EOF
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF
    fi
    
    log "MySQL 配置完成"
}

# 3. 创建应用目录
create_dirs() {
    log "创建应用目录..."
    
    mkdir -p $APP_DIR
    mkdir -p $UPLOAD_DIR
    mkdir -p /var/log
    
    log "目录创建完成"
}

# 4. 初始化数据库表
initialize_database() {
    log "初始化数据库表..."

    local scripts=(
        "init_db.sql"
        "init_template.sql"
        "ai_chat_tables.sql"
        "ai_knowledge_tables.sql"
        "ai_knowledge_optimization.sql"
        "outline_exam_tables.sql"
        "ai_action_tables.sql"
        "student_visit_record_tables.sql"
        "exam_survey_session_fixes.sql"
        "must_change_password.sql"
    )

    for script in "${scripts[@]}"; do
        if [ -f "$APP_DIR/$script" ]; then
            mysql -uroot -proot "$DB_NAME" --force < "$APP_DIR/$script"
            log "已执行 $script"
        else
            warn "未找到数据库脚本: $APP_DIR/$script"
        fi
    done

    log "数据库表初始化完成"
}

# 5. 配置后端
setup_backend() {
    log "配置后端..."
    
    # 修改 application.yml 中的数据库密码
    if [ -f "$BACKEND_DIR/src/main/resources/application.yml" ]; then
        sed -i "s/password:.*/password: $DB_PASS/" $BACKEND_DIR/src/main/resources/application.yml
        log "数据库密码已更新"
    fi
    
    # 打包后端
    cd $BACKEND_DIR
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
    mvn clean package -DskipTests -q
    
    if [ ! -f "$BACKEND_DIR/target/edu-backend-1.0.0.jar" ]; then
        error "后端打包失败"
    fi
    
    log "后端打包完成"
}

# 6. 配置前端
setup_frontend() {
    log "配置前端..."
    
    # 安装依赖
    cd $FRONTEND_DIR
    npm install --silent
    
    # 修改 vite.config.ts 中的代理（生产环境）
    cat > $FRONTEND_DIR/vite.config.ts << 'EOF'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 80,
    host: '0.0.0.0',
    proxy: {
      '/auth': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/user': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/admin': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/teacher': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'static'
  }
})
EOF
    
    # 修改 request.ts 中的 baseURL
    if [ -f "$FRONTEND_DIR/src/utils/request.ts" ]; then
        sed -i "s|baseURL:.*|baseURL: '/'|" $FRONTEND_DIR/src/utils/request.ts
    fi
    
    # 打包前端
    npm run build --silent
    
    if [ ! -d "$FRONTEND_DIR/dist" ]; then
        error "前端打包失败"
    fi
    
    log "前端打包完成"
}

# 7. 配置 Nginx
setup_nginx() {
    log "配置 Nginx..."
    
    cat > /etc/nginx/sites-available/edu-platform << 'EOF'
server {
    listen 80;
    server_name _;

    client_max_body_size 100M;

    # 前端静态文件
    location / {
        root /edu-platform/edu-frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /auth {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /user {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /admin {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /teacher {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
EOF
    
    # 启用站点配置
    ln -sf /etc/nginx/sites-available/edu-platform /etc/nginx/sites-enabled/
    
    # 测试 Nginx 配置
    nginx -t
    
    # 重载 Nginx
    systemctl reload nginx
    
    log "Nginx 配置完成"
}

# 8. 创建 Systemd 服务
create_service() {
    log "创建 Systemd 服务..."
    
    cat > /etc/systemd/system/edu-backend.service << EOF
[Unit]
Description=Edu Platform Backend
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=$BACKEND_DIR
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk"
ExecStart=/usr/lib/jvm/java-17-openjdk/bin/java -jar $BACKEND_DIR/target/edu-backend-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
    
    systemctl daemon-reload
    systemctl enable edu-backend
    
    log "Systemd 服务创建完成"
}

# 9. 启动应用
start_app() {
    log "启动应用..."
    
    # 启动后端
    systemctl start edu-backend
    
    # 等待后端启动
    sleep 5
    
    # 检查后端状态
    if systemctl is-active edu-backend > /dev/null; then
        log "后端启动成功"
    else
        warn "后端启动中，请检查日志"
    fi
    
    # 设置文件权限
    chown -R www-data:www-data $UPLOAD_DIR
    chmod -R 755 $UPLOAD_DIR
    
    log "应用启动完成"
}

# 10. 防火墙配置
setup_firewall() {
    log "配置防火墙..."
    
    # 安装 ufw 如果没有
    apt install -y ufw > /dev/null 2>&1
    
    # 开放端口
    ufw allow 22/tcp
    ufw allow 80/tcp
    ufw allow 443/tcp
    
    # 启用防火墙（如果未启用）
    echo "y" | ufw enable > /dev/null 2>&1 || true
    
    log "防火墙配置完成"
}

# 11. 显示部署信息
show_info() {
    echo ""
    echo "========================================"
    echo "   edu-platform 部署完成!"
    echo "========================================"
    echo ""
    echo "数据库信息:"
    echo "  数据库名: $DB_NAME"
    echo "  用户名: $DB_USER"
    echo "  密码: $DB_PASS"
    echo ""
    echo "应用信息:"
    echo "  后端地址: http://localhost:8080"
    echo "  前端地址: http://localhost"
    echo ""
    echo "服务管理命令:"
    echo "  后端服务: systemctl start|stop|restart edu-backend"
    echo "  后端日志: journalctl -u edu-backend -f"
    echo "  Nginx: systemctl reload|restart nginx"
    echo ""
    echo "测试用户:"
    echo "  教师: teacher001 / password123"
    echo "  学生: student001 / password123"
    echo "  管理员: admin / admin123"
    echo ""
}

# 主函数
main() {
    log "开始部署 edu-platform..."
    
    check_root
    install_dependencies
    setup_mysql
    create_dirs
    initialize_database
    setup_backend
    setup_frontend
    setup_nginx
    create_service
    start_app
    setup_firewall
    show_info
    
    log "部署完成!"
}

main "$@"
