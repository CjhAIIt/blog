#!/bin/bash

# 反向代理设置脚本
# 用于在公网服务器上配置Nginx反向代理本地博客服务

set -e

echo "=========================================="
echo "博客服务反向代理配置"
echo "=========================================="

# 配置变量
LOCAL_SERVER="172.18.8.107"
LOCAL_PORT="8012"
PUBLIC_PORT="8012"
NGINX_CONFIG="/etc/nginx/sites-available/blog-proxy"
NGINX_ENABLED="/etc/nginx/sites-enabled/blog-proxy"

echo "本地服务器: $LOCAL_SERVER:$LOCAL_PORT"
echo "公网暴露端口: $PUBLIC_PORT"
echo ""

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
    echo "❌ 此脚本需要root权限，请使用 sudo 运行"
    exit 1
fi

# 安装Nginx（如果未安装）
echo "📦 检查Nginx安装..."
if ! command -v nginx &> /dev/null; then
    echo "安装Nginx..."
    apt-get update
    apt-get install -y nginx
else
    echo "✅ Nginx已安装"
fi

# 创建Nginx配置文件
echo "⚙️  创建Nginx反向代理配置..."
cat > "$NGINX_CONFIG" << 'EOF'
upstream blog_backend {
    server 172.18.8.107:8012;
    keepalive 32;
}

server {
    listen 8012;
    listen [::]:8012;

    server_name _;

    # 增加请求体大小限制（支持文件上传）
    client_max_body_size 25M;

    # 日志
    access_log /var/log/nginx/blog-proxy-access.log;
    error_log /var/log/nginx/blog-proxy-error.log;

    location / {
        proxy_pass http://blog_backend;

        # 保留原始请求信息
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $server_name;

        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # 缓冲设置
        proxy_buffering off;
        proxy_request_buffering off;
    }
}
EOF

echo "✅ 配置文件已创建: $NGINX_CONFIG"

# 启用配置
echo "🔗 启用Nginx配置..."
if [ -L "$NGINX_ENABLED" ]; then
    rm "$NGINX_ENABLED"
fi
ln -s "$NGINX_CONFIG" "$NGINX_ENABLED"

# 测试Nginx配置
echo "🧪 测试Nginx配置..."
if nginx -t; then
    echo "✅ 配置测试通过"
else
    echo "❌ 配置测试失败"
    exit 1
fi

# 重启Nginx
echo "🔄 重启Nginx服务..."
systemctl restart nginx

# 检查服务状态
if systemctl is-active --quiet nginx; then
    echo "✅ Nginx服务已启动"
else
    echo "❌ Nginx服务启动失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ 反向代理配置完成！"
echo "=========================================="
echo ""
echo "📍 访问地址: http://101.35.79.76:8012"
echo "📍 本地服务: http://172.18.8.107:8012"
echo ""
echo "📋 常用命令:"
echo "  查看日志: tail -f /var/log/nginx/blog-proxy-access.log"
echo "  查看错误: tail -f /var/log/nginx/blog-proxy-error.log"
echo "  重启服务: sudo systemctl restart nginx"
echo "  停止服务: sudo systemctl stop nginx"
echo "  查看状态: sudo systemctl status nginx"
echo ""
