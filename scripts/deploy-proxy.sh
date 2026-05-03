#!/bin/bash

# SSH连接并部署反向代理
# 这个脚本会连接到公网服务器并执行反向代理配置

PUBLIC_IP="101.35.79.76"
PUBLIC_USER="ubuntu"
PUBLIC_PASSWORD="Cjh041217@"

echo "=========================================="
echo "开始部署反向代理到公网服务器"
echo "=========================================="
echo "目标服务器: $PUBLIC_IP"
echo "用户名: $PUBLIC_USER"
echo ""

# 检查sshpass是否安装
if ! command -v sshpass &> /dev/null; then
    echo "📦 安装sshpass..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get install -y sshpass
    elif command -v yum &> /dev/null; then
        sudo yum install -y sshpass
    else
        echo "❌ 无法自动安装sshpass，请手动安装"
        exit 1
    fi
fi

# 上传脚本到公网服务器
echo "📤 上传配置脚本到公网服务器..."
sshpass -p "$PUBLIC_PASSWORD" scp -o StrictHostKeyChecking=no \
    "$(dirname "$0")/setup-reverse-proxy.sh" \
    "$PUBLIC_USER@$PUBLIC_IP:/tmp/setup-reverse-proxy.sh"

if [ $? -ne 0 ]; then
    echo "❌ 上传失败"
    exit 1
fi

echo "✅ 脚本上传成功"
echo ""

# 在公网服务器上执行脚本
echo "🚀 在公网服务器上执行配置..."
sshpass -p "$PUBLIC_PASSWORD" ssh -o StrictHostKeyChecking=no \
    "$PUBLIC_USER@$PUBLIC_IP" \
    "chmod +x /tmp/setup-reverse-proxy.sh && sudo /tmp/setup-reverse-proxy.sh"

if [ $? -ne 0 ]; then
    echo "❌ 配置执行失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
echo "🌐 博客现已可通过以下地址访问:"
echo "   http://101.35.79.76:8012"
echo ""
echo "📝 后续操作:"
echo "1. 在浏览器中访问 http://101.35.79.76:8012"
echo "2. 测试所有功能（查看、写入等）"
echo "3. 如有问题，查看公网服务器日志:"
echo "   ssh ubuntu@101.35.79.76"
echo "   tail -f /var/log/nginx/blog-proxy-error.log"
echo ""
