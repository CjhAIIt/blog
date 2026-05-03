#!/bin/bash

# 快速部署脚本 - 一键配置反向代理
# 使用方法: bash quick-deploy.sh

set -e

echo "=========================================="
echo "博客反向代理 - 快速部署"
echo "=========================================="
echo ""

# 配置
LOCAL_SERVER="172.18.8.107"
LOCAL_PORT="8012"
PUBLIC_IP="101.35.79.76"
PUBLIC_USER="ubuntu"
PUBLIC_PASSWORD="Cjh041217@"
PUBLIC_PORT="8012"

# 步骤1: 验证本地服务
echo "📋 步骤1: 验证本地博客服务..."
if curl -s "http://$LOCAL_SERVER:$LOCAL_PORT" > /dev/null 2>&1; then
    echo "✅ 本地服务可访问"
else
    echo "⚠️  本地服务可能未运行，请确保 $LOCAL_SERVER:$LOCAL_PORT 可访问"
fi
echo ""

# 步骤2: 生成Nginx配置
echo "📋 步骤2: 生成Nginx配置..."
NGINX_CONFIG=$(cat << 'EOF'
upstream blog_backend {
    server 172.18.8.107:8012;
    keepalive 32;
}

server {
    listen 8012;
    listen [::]:8012;
    server_name _;
    client_max_body_size 25M;
    access_log /var/log/nginx/blog-proxy-access.log;
    error_log /var/log/nginx/blog-proxy-error.log;

    location / {
        proxy_pass http://blog_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        proxy_buffering off;
    }
}
EOF
)

# 步骤3: 连接到公网服务器并配置
echo "📋 步骤3: 连接到公网服务器并配置..."
echo "服务器: $PUBLIC_IP"
echo "用户: $PUBLIC_USER"
echo ""

# 检查sshpass
if ! command -v sshpass &> /dev/null; then
    echo "📦 安装sshpass..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get install -y sshpass > /dev/null 2>&1
    fi
fi

# 执行远程配置
echo "🔧 配置Nginx..."
sshpass -p "$PUBLIC_PASSWORD" ssh -o StrictHostKeyChecking=no "$PUBLIC_USER@$PUBLIC_IP" << REMOTE_SCRIPT
set -e

# 安装Nginx
if ! command -v nginx &> /dev/null; then
    echo "📦 安装Nginx..."
    sudo apt-get update > /dev/null 2>&1
    sudo apt-get install -y nginx > /dev/null 2>&1
fi

# 创建配置
echo "$NGINX_CONFIG" | sudo tee /etc/nginx/sites-available/blog-proxy > /dev/null

# 启用配置
sudo rm -f /etc/nginx/sites-enabled/blog-proxy
sudo ln -s /etc/nginx/sites-available/blog-proxy /etc/nginx/sites-enabled/blog-proxy

# 测试配置
sudo nginx -t

# 重启Nginx
sudo systemctl restart nginx

echo "✅ Nginx配置完成"
REMOTE_SCRIPT

echo ""
echo "=========================================="
echo "✅ 部署成功！"
echo "=========================================="
echo ""
echo "🌐 访问地址:"
echo "   http://101.35.79.76:8012"
echo ""
echo "📊 测试命令:"
echo "   curl http://101.35.79.76:8012"
echo ""
echo "📋 查看日志:"
echo "   ssh ubuntu@101.35.79.76"
echo "   tail -f /var/log/nginx/blog-proxy-access.log"
echo "   tail -f /var/log/nginx/blog-proxy-error.log"
echo ""
