#!/bin/bash

# 博客项目部署脚本 - 第一阶段：检查本地服务器环境
# 使用方法：bash deploy-check.sh

set -e

echo "=========================================="
echo "博客项目部署 - 环境检查阶段"
echo "=========================================="
echo ""

# 配置
LOCAL_SERVER="172.18.8.107"
LOCAL_USER="lab"
BLOG_PORT="8012"
BLOG_HOME="/opt/blog"
BACKUP_DIR="/opt/blog-backup-$(date +%Y%m%d-%H%M%S)"

echo "📋 检查本地服务器环境..."
echo "服务器地址: $LOCAL_SERVER"
echo "用户: $LOCAL_USER"
echo "博客端口: $BLOG_PORT"
echo ""

# 检查SSH连接
echo "🔗 测试SSH连接..."
if ssh -o ConnectTimeout=5 "$LOCAL_USER@$LOCAL_SERVER" "echo 'SSH连接成功'" 2>/dev/null; then
    echo "✅ SSH连接正常"
else
    echo "❌ SSH连接失败，请检查网络和凭证"
    exit 1
fi

echo ""
echo "📊 检查本地服务器运行的服务..."

# 检查Java进程
echo ""
echo "1️⃣  检查Java进程..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "ps aux | grep -i java | grep -v grep" || echo "⚠️  未找到Java进程"

# 检查systemd服务
echo ""
echo "2️⃣  检查systemd服务..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "systemctl list-units --type=service --state=running | grep -i blog" || echo "⚠️  未找到blog相关systemd服务"

# 检查pm2进程
echo ""
echo "3️⃣  检查pm2进程..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "pm2 list 2>/dev/null || echo '未安装pm2'" || true

# 检查Docker容器
echo ""
echo "4️⃣  检查Docker容器..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "docker ps 2>/dev/null | grep -i blog || echo '未找到blog相关容器'" || true

# 检查端口占用
echo ""
echo "5️⃣  检查8012端口占用..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "netstat -tlnp 2>/dev/null | grep 8012 || ss -tlnp 2>/dev/null | grep 8012 || echo '8012端口未被占用'" || true

# 检查MySQL
echo ""
echo "6️⃣  检查MySQL服务..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "systemctl status mysql 2>/dev/null || systemctl status mariadb 2>/dev/null || docker ps | grep -i mysql || echo '⚠️  未找到MySQL服务'" || true

# 检查项目目录
echo ""
echo "7️⃣  检查项目目录..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "ls -la $BLOG_HOME 2>/dev/null || echo '项目目录不存在: $BLOG_HOME'" || true

# 检查上传文件目录
echo ""
echo "8️⃣  检查上传文件目录..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "find /opt -name 'uploads' -o -name 'storage' -o -name 'public' 2>/dev/null | head -10" || true

# 检查配置文件
echo ""
echo "9️⃣  检查配置文件..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "find /opt -name 'application*.properties' -o -name 'application*.yml' -o -name '.env' 2>/dev/null | head -10" || true

# 检查日志
echo ""
echo "🔟 检查日志文件..."
ssh "$LOCAL_USER@$LOCAL_SERVER" "find /opt -name '*.log' 2>/dev/null | head -5" || true

echo ""
echo "=========================================="
echo "✅ 环境检查完成"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 根据上述检查结果，确认当前服务的运行方式"
echo "2. 执行 bash deploy-backup.sh 备份旧服务"
echo "3. 执行 bash deploy-build.sh 构建新项目"
echo "4. 执行 bash deploy-start.sh 启动新服务"
echo ""
