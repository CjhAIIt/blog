#!/bin/bash

# Complete deployment script for blog service
# This script handles: upload, deploy, and configure systemd service

set -e

echo "=========================================="
echo "博客项目完整部署脚本"
echo "=========================================="
echo ""

# Configuration
LOCAL_SERVER="172.18.8.107"
LOCAL_USER="lab"
BLOG_PORT="8012"
BLOG_HOME="/opt/blog"
JAR_FILE="blog-0.0.1-SNAPSHOT.jar"
DB_PASSWORD="${DB_PASSWORD:-your_db_password}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}📋 部署配置${NC}"
echo "服务器: $LOCAL_SERVER"
echo "用户: $LOCAL_USER"
echo "端口: $BLOG_PORT"
echo "项目目录: $BLOG_HOME"
echo ""

# Step 1: Check if running on local server
echo -e "${YELLOW}Step 1: 检查部署环境${NC}"
if [ ! -f "/etc/hostname" ]; then
    echo -e "${RED}❌ 此脚本必须在Linux服务器上运行${NC}"
    exit 1
fi

HOSTNAME=$(cat /etc/hostname)
echo -e "${GREEN}✅ 运行在服务器: $HOSTNAME${NC}"
echo ""

# Step 2: Check Java
echo -e "${YELLOW}Step 2: 检查Java环境${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java未安装${NC}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1)
echo -e "${GREEN}✅ $JAVA_VERSION${NC}"
echo ""

# Step 3: Check MySQL
echo -e "${YELLOW}Step 3: 检查MySQL${NC}"
if ! systemctl is-active --quiet mysql && ! systemctl is-active --quiet mariadb; then
    echo -e "${YELLOW}⚠️  MySQL服务未运行，尝试启动...${NC}"
    sudo systemctl start mysql || sudo systemctl start mariadb || true
fi
echo -e "${GREEN}✅ MySQL检查完成${NC}"
echo ""

# Step 4: Stop old service
echo -e "${YELLOW}Step 4: 停止旧服务${NC}"
if systemctl is-active --quiet blog; then
    echo "停止现有blog服务..."
    sudo systemctl stop blog
    sleep 2
    echo -e "${GREEN}✅ 旧服务已停止${NC}"
else
    echo -e "${GREEN}✅ 没有运行中的blog服务${NC}"
fi
echo ""

# Step 5: Create project directory
echo -e "${YELLOW}Step 5: 创建项目目录${NC}"
sudo mkdir -p $BLOG_HOME
sudo chown $LOCAL_USER:$LOCAL_USER $BLOG_HOME
echo -e "${GREEN}✅ 项目目录已创建: $BLOG_HOME${NC}"
echo ""

# Step 6: Check for JAR file
echo -e "${YELLOW}Step 6: 检查JAR文件${NC}"
if [ -f "/tmp/$JAR_FILE" ]; then
    echo "找到JAR文件: /tmp/$JAR_FILE"
    JAR_SIZE=$(ls -lh /tmp/$JAR_FILE | awk '{print $5}')
    echo "文件大小: $JAR_SIZE"

    echo "移动JAR到项目目录..."
    sudo mv /tmp/$JAR_FILE $BLOG_HOME/
    sudo chown $LOCAL_USER:$LOCAL_USER $BLOG_HOME/$JAR_FILE
    echo -e "${GREEN}✅ JAR文件已部署${NC}"
else
    echo -e "${RED}❌ JAR文件未找到: /tmp/$JAR_FILE${NC}"
    echo "请先上传JAR文件到 /tmp/$JAR_FILE"
    exit 1
fi
echo ""

# Step 7: Create uploads directory
echo -e "${YELLOW}Step 7: 创建上传目录${NC}"
sudo mkdir -p $BLOG_HOME/uploads
sudo chown $LOCAL_USER:$LOCAL_USER $BLOG_HOME/uploads
sudo chmod 755 $BLOG_HOME/uploads
echo -e "${GREEN}✅ 上传目录已创建${NC}"
echo ""

# Step 8: Create systemd service
echo -e "${YELLOW}Step 8: 创建systemd服务${NC}"
sudo cat > /etc/systemd/system/blog.service << EOF
[Unit]
Description=Blog Service
After=network.target mysql.service

[Service]
Type=simple
User=$LOCAL_USER
WorkingDirectory=$BLOG_HOME
ExecStart=/usr/bin/java -jar $BLOG_HOME/$JAR_FILE \\
  --server.port=$BLOG_PORT \\
  --spring.datasource.url=jdbc:mysql://localhost:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai \\
  --spring.datasource.username=root \\
  --spring.datasource.password=$DB_PASSWORD \\
  --app.storage.upload-dir=$BLOG_HOME/uploads

Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

echo -e "${GREEN}✅ systemd服务已创建${NC}"
echo ""

# Step 9: Enable and start service
echo -e "${YELLOW}Step 9: 启用并启动服务${NC}"
sudo systemctl daemon-reload
sudo systemctl enable blog
echo "启动blog服务..."
sudo systemctl start blog
sleep 3

if systemctl is-active --quiet blog; then
    echo -e "${GREEN}✅ blog服务已启动${NC}"
else
    echo -e "${RED}❌ blog服务启动失败${NC}"
    echo "查看日志:"
    sudo journalctl -u blog -n 20
    exit 1
fi
echo ""

# Step 10: Verify service
echo -e "${YELLOW}Step 10: 验证服务${NC}"
echo "检查端口 $BLOG_PORT..."
if netstat -tlnp 2>/dev/null | grep -q ":$BLOG_PORT" || ss -tlnp 2>/dev/null | grep -q ":$BLOG_PORT"; then
    echo -e "${GREEN}✅ 端口 $BLOG_PORT 已监听${NC}"
else
    echo -e "${YELLOW}⚠️  端口 $BLOG_PORT 未立即监听，等待服务启动...${NC}"
    sleep 5
fi

echo "测试本地访问..."
if curl -s -I http://localhost:$BLOG_PORT/ | grep -q "HTTP"; then
    echo -e "${GREEN}✅ 本地访问成功${NC}"
else
    echo -e "${YELLOW}⚠️  本地访问测试失败，查看日志:${NC}"
    sudo journalctl -u blog -n 30
fi
echo ""

# Step 11: Display status
echo -e "${YELLOW}Step 11: 服务状态${NC}"
sudo systemctl status blog --no-pager
echo ""

echo "=========================================="
echo -e "${GREEN}✅ 部署完成！${NC}"
echo "=========================================="
echo ""
echo "📍 本地访问: http://localhost:$BLOG_PORT"
echo "📍 内网访问: http://172.18.8.107:$BLOG_PORT"
echo ""
echo "📋 常用命令:"
echo "  查看状态: sudo systemctl status blog"
echo "  查看日志: sudo journalctl -u blog -f"
echo "  重启服务: sudo systemctl restart blog"
echo "  停止服务: sudo systemctl stop blog"
echo ""
echo "下一步: 在公网服务器上配置反向代理"
echo ""
