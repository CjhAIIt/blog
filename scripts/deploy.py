#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
博客反向代理部署脚本 - Windows/Linux通用版本
"""

import subprocess
import sys
import os

def run_command(cmd, description=""):
    """执行命令"""
    if description:
        print(f"\n🔧 {description}")
    print(f"执行: {cmd}")
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if result.stdout:
        print(result.stdout)
    if result.stderr:
        print(result.stderr, file=sys.stderr)
    return result.returncode == 0

def main():
    print("=" * 50)
    print("博客反向代理 - 快速部署")
    print("=" * 50)
    print()

    # 配置
    PUBLIC_IP = "101.35.79.76"
    PUBLIC_USER = "ubuntu"
    PUBLIC_PASSWORD = "Cjh041217@"
    LOCAL_SERVER = "172.18.8.107"
    LOCAL_PORT = "8012"
    PUBLIC_PORT = "8012"

    # Nginx配置内容
    nginx_config = """upstream blog_backend {
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
}"""

    # 步骤1: 验证本地服务
    print("📋 步骤1: 验证本地博客服务...")
    try:
        result = subprocess.run(
            f"curl -s http://{LOCAL_SERVER}:{LOCAL_PORT} > /dev/null",
            shell=True,
            timeout=5
        )
        if result.returncode == 0:
            print("✅ 本地服务可访问")
        else:
            print(f"⚠️  本地服务可能未运行，请确保 {LOCAL_SERVER}:{LOCAL_PORT} 可访问")
    except Exception as e:
        print(f"⚠️  无法验证本地服务: {e}")

    print()

    # 步骤2: 生成临时配置文件
    print("📋 步骤2: 生成Nginx配置...")
    config_file = "/tmp/blog-proxy.conf"
    try:
        with open(config_file, "w") as f:
            f.write(nginx_config)
        print(f"✅ 配置文件已生成: {config_file}")
    except Exception as e:
        print(f"❌ 生成配置文件失败: {e}")
        return False

    print()

    # 步骤3: 连接到公网服务器并配置
    print("📋 步骤3: 连接到公网服务器并配置...")
    print(f"服务器: {PUBLIC_IP}")
    print(f"用户: {PUBLIC_USER}")
    print()

    # 使用SSH连接并执行配置
    print("🔧 配置Nginx...")

    # 创建远程执行脚本
    remote_script = f"""
set -e

# 安装Nginx
if ! command -v nginx &> /dev/null; then
    echo "📦 安装Nginx..."
    sudo apt-get update > /dev/null 2>&1
    sudo apt-get install -y nginx > /dev/null 2>&1
fi

# 创建配置
sudo tee /etc/nginx/sites-available/blog-proxy > /dev/null << 'NGINX_EOF'
{nginx_config}
NGINX_EOF

# 启用配置
sudo rm -f /etc/nginx/sites-enabled/blog-proxy
sudo ln -s /etc/nginx/sites-available/blog-proxy /etc/nginx/sites-enabled/blog-proxy

# 测试配置
sudo nginx -t

# 重启Nginx
sudo systemctl restart nginx

echo "✅ Nginx配置完成"
"""

    # 使用SSH执行
    ssh_cmd = f'ssh -o StrictHostKeyChecking=no {PUBLIC_USER}@{PUBLIC_IP}'

    try:
        # 尝试使用SSH密钥（如果有）
        result = subprocess.run(
            f'{ssh_cmd} "bash -s" << \'EOF\'\n{remote_script}\nEOF',
            shell=True,
            capture_output=True,
            text=True,
            timeout=120
        )

        print(result.stdout)
        if result.stderr:
            print(result.stderr, file=sys.stderr)

        if result.returncode != 0:
            print("\n⚠️  SSH连接失败，请检查:")
            print("1. SSH密钥是否配置")
            print("2. 网络连接是否正常")
            print("3. 服务器地址和用户名是否正确")
            print("\n💡 手动配置方法:")
            print(f"ssh {PUBLIC_USER}@{PUBLIC_IP}")
            print("然后复制以下配置到 /etc/nginx/sites-available/blog-proxy:")
            print(nginx_config)
            return False
    except subprocess.TimeoutExpired:
        print("❌ SSH连接超时")
        return False
    except Exception as e:
        print(f"❌ SSH执行失败: {e}")
        return False

    print()
    print("=" * 50)
    print("✅ 部署成功！")
    print("=" * 50)
    print()
    print("🌐 访问地址:")
    print(f"   http://{PUBLIC_IP}:{PUBLIC_PORT}")
    print()
    print("📊 测试命令:")
    print(f"   curl http://{PUBLIC_IP}:{PUBLIC_PORT}")
    print()
    print("📋 查看日志:")
    print(f"   ssh {PUBLIC_USER}@{PUBLIC_IP}")
    print("   tail -f /var/log/nginx/blog-proxy-access.log")
    print("   tail -f /var/log/nginx/blog-proxy-error.log")
    print()

    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
