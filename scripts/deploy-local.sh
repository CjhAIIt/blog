#!/bin/bash
# 快速部署参考 - 在本地服务器上执行此脚本

# 将此脚本保存为 deploy-local.sh，然后在本地服务器上执行：
# bash deploy-local.sh

set -e

echo "=========================================="
echo "博客本地部署脚本"
echo "=========================================="
echo ""

# 配置
BLOG_HOME="/opt/blog"
BLOG_PORT="8012"
JAR_FILE="blog-0.0.1-SNAPSHOT.jar"
DB_PASSWORD="${1:-your_db_password}"

if [ "$DB_PASSWORD" = "your_db_password" ]; then
    echo "⚠️  警告：使用默认数据库密码"
    echo "请提供实际的MySQL密码作为参数："
    echo "  bash deploy-local.sh <mysql_password>"
    echo ""
    read -p "请输入MySQL root密码: " DB_PASSWORD
fi

echo "📋 部署配置"
echo "项目目录: $BLOG_HOME"
echo "端口: $BLOG_PORT"
echo "JAR文件: $JAR_FILE"
echo ""

# Step 1: 检查JAR文件
echo "Step 1: 检查JAR文件..."
if [ ! -f "/tmp/$JAR_FILE" ]; then
    echo "❌ 错误：JAR文件不存在 /tmp/$JAR_FILE"
    echo "请先上传JAR文件"
    exit 1
fi
echo "✅ JAR文件已找到"
echo ""

# Step 2: 停止旧服务
echo "Step 2: 停止旧服务..."
if systemctl is-active --quiet blog 2>/dev/null; then
    sudo systemctl stop blog
    sleep 2
    echo "✅ 旧服务已停止"
else
    echo "✅ 没有运行中的服务"
fi
echo ""

# Step 3: 创建项目目录
echo "Step 3: 创建项目目录..."
sudo mkdir -p $BLOG_HOME
sudo chown $USER:$USER $BLOG_HOME
echo "✅ 项目目录已创建"
echo ""

# Step 4: 部署JAR
echo "Step 4: 部署JAR文件..."
sudo mv /tmp/$JAR_FILE $BLOG_HOME/
sudo chown $USER:$USER $BLOG_HOME/$JAR_FILE
ls -lh $BLOG_HOME/$JAR_FILE
echo "✅ JAR文件已部署"
echo ""

# Step 5: 创建上传目录
echo "Step 5: 创建上传目录..."
sudo mkdir -p $BLOG_HOME/uploads
sudo chown $USER:$USER $BLOG_HOME/uploads
sudo chmod 755 $BLOG_HOME/uploads
echo "✅ 上传目录已创建"
echo ""

# Step 6: 创建systemd服务
echo "Step 6: 创建systemd服务..."
sudo cat > /etc/systemd/system/blog.service << EOF
[Unit]
Description=Blog Service
After=network.target mysql.service

[Service]
Type=simple
User=$USER
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
echo "✅ systemd服务已创建"
echo ""

# Step 7: 启动服务
echo "Step 7: 启动服务..."
sudo systemctl daemon-reload
sudo systemctl enable blog
sudo systemctl start blog
sleep 3
echo "✅ 服务已启动"
echo ""

# Step 8: 验证服务
echo "Step 8: 验证服务..."
if sudo systemctl is-active --quiet blog; then
    echo "✅ 服务运行中"
else
    echo "❌ 服务启动失败"
    sudo journalctl -u blog -n 20
    exit 1
fi
echo ""

# Step 9: 检查端口
echo "Step 9: 检查端口..."
if netstat -tlnp 2>/dev/null | grep -q ":$BLOG_PORT" || ss -tlnp 2>/dev/null | grep -q ":$BLOG_PORT"; then
    echo "✅ 端口 $BLOG_PORT 已监听"
else
    echo "⚠️  端口 $BLOG_PORT 未立即监听，等待..."
    sleep 5
fi
echo ""

# Step 10: 测试访问
echo "Step 10: 测试本地访问..."
if curl -s -I http://localhost:$BLOG_PORT/ | grep -q "HTTP"; then
    echo "✅ 本地访问成功"
else
    echo "⚠️  本地访问测试失败"
    echo "查看日志: sudo journalctl -u blog -f"
fi
echo ""

echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
echo "📍 本地访问: http://localhost:$BLOG_PORT"
echo "📍 内网访问: http://172.18.8.107:$BLOG_PORT"
echo ""
echo "查看日志: sudo journalctl -u blog -f"
echo ""
