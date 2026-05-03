#!/bin/bash
# 完整自动化部署脚本
# 此脚本将自动执行所有部署步骤

set -e

echo "=========================================="
echo "博客项目自动化部署"
echo "=========================================="
echo ""

# 配置
LOCAL_SERVER="172.18.8.107"
LOCAL_USER="lab"
LOCAL_PASSWORD="a6n107"
JAR_FILE="D:/blog-master/target/blog-0.0.1-SNAPSHOT.jar"
REMOTE_JAR="/tmp/blog-0.0.1-SNAPSHOT.jar"
DB_PASSWORD="${1:-your_db_password}"

# 检查JAR文件
echo "Step 1: 检查JAR文件..."
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR文件不存在: $JAR_FILE"
    exit 1
fi
JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
echo "✅ JAR文件已找到: $JAR_SIZE"
echo ""

# 创建远程部署脚本
echo "Step 2: 创建远程部署脚本..."
DEPLOY_SCRIPT=$(cat << 'DEPLOY_EOF'
#!/bin/bash
set -e

echo "=========================================="
echo "在远程服务器上执行部署"
echo "=========================================="
echo ""

BLOG_HOME="/opt/blog"
BLOG_PORT="8012"
JAR_FILE="blog-0.0.1-SNAPSHOT.jar"
DB_PASSWORD="$1"

echo "📋 部署配置"
echo "项目目录: $BLOG_HOME"
echo "端口: $BLOG_PORT"
echo ""

# Step 1: 检查JAR
echo "Step 1: 检查JAR文件..."
if [ ! -f "/tmp/$JAR_FILE" ]; then
    echo "❌ JAR文件不存在"
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

# Step 3: 创建目录
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

# Step 9: 测试访问
echo "Step 9: 测试本地访问..."
sleep 2
if curl -s -I http://localhost:$BLOG_PORT/ | grep -q "HTTP"; then
    echo "✅ 本地访问成功"
else
    echo "⚠️  本地访问测试失败，查看日志..."
    sudo journalctl -u blog -n 30
fi
echo ""

echo "=========================================="
echo "✅ 远程部署完成！"
echo "=========================================="
echo ""
echo "📍 本地访问: http://localhost:$BLOG_PORT"
echo "📍 内网访问: http://172.18.8.107:$BLOG_PORT"
echo ""

DEPLOY_EOF
)

echo "✅ 远程部署脚本已创建"
echo ""

# Step 3: 上传JAR
echo "Step 3: 上传JAR到远程服务器..."
echo "使用SCP上传文件..."

# 尝试使用SSH密钥
if [ -f ~/.ssh/id_ed25519 ]; then
    echo "使用SSH密钥认证..."
    scp -i ~/.ssh/id_ed25519 -o StrictHostKeyChecking=no "$JAR_FILE" "$LOCAL_USER@$LOCAL_SERVER:$REMOTE_JAR"
else
    echo "❌ SSH密钥不存在"
    echo "请先设置SSH密钥或使用密码认证"
    exit 1
fi

echo "✅ JAR文件已上传"
echo ""

# Step 4: 验证上传
echo "Step 4: 验证上传..."
ssh -i ~/.ssh/id_ed25519 -o StrictHostKeyChecking=no "$LOCAL_USER@$LOCAL_SERVER" "ls -lh $REMOTE_JAR"
echo "✅ 上传验证成功"
echo ""

# Step 5: 执行远程部署
echo "Step 5: 执行远程部署..."
echo "$DEPLOY_SCRIPT" | ssh -i ~/.ssh/id_ed25519 -o StrictHostKeyChecking=no "$LOCAL_USER@$LOCAL_SERVER" "bash -s $DB_PASSWORD"

if [ $? -ne 0 ]; then
    echo "❌ 远程部署失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ 本地服务器部署完成！"
echo "=========================================="
echo ""

# Step 6: 配置公网反向代理
echo "Step 6: 配置公网反向代理..."
cd D:/blog-master
bash scripts/deploy-proxy.sh

echo ""
echo "=========================================="
echo "✅ 完整部署完成！"
echo "=========================================="
echo ""
echo "验证部署："
echo "  curl -I http://172.18.8.107:8012/"
echo "  curl -I http://101.35.79.76:8012/"
echo ""
echo "在浏览器打开："
echo "  http://101.35.79.76:8012"
echo ""
