#!/bin/bash

# SSH反向隧道配置脚本
# 在本地服务器上运行，建立到公网服务器的反向隧道

set -e

echo "=========================================="
echo "SSH反向隧道配置"
echo "=========================================="
echo ""

# 配置
PUBLIC_IP="101.35.79.76"
PUBLIC_USER="ubuntu"
LOCAL_PORT="8012"
TUNNEL_PORT="9012"  # 隧道在公网服务器上的端口

echo "配置信息:"
echo "  公网服务器: $PUBLIC_IP"
echo "  用户: $PUBLIC_USER"
echo "  本地端口: $LOCAL_PORT"
echo "  隧道端口: $TUNNEL_PORT"
echo ""

# 创建systemd服务文件
echo "📋 创建systemd服务..."

sudo tee /etc/systemd/system/blog-tunnel.service > /dev/null << EOF
[Unit]
Description=Blog SSH Reverse Tunnel
After=network.target

[Service]
Type=simple
User=$USER
ExecStart=/usr/bin/ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -N -R $TUNNEL_PORT:localhost:$LOCAL_PORT $PUBLIC_USER@$PUBLIC_IP
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

echo "✅ 服务文件已创建"
echo ""

# 启用并启动服务
echo "🚀 启动隧道服务..."
sudo systemctl daemon-reload
sudo systemctl enable blog-tunnel.service
sudo systemctl start blog-tunnel.service

# 检查状态
sleep 2
if sudo systemctl is-active --quiet blog-tunnel.service; then
    echo "✅ 隧道服务已启动"
else
    echo "❌ 隧道服务启动失败"
    sudo systemctl status blog-tunnel.service
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ SSH反向隧道配置完成！"
echo "=========================================="
echo ""
echo "📍 隧道信息:"
echo "  本地: localhost:$LOCAL_PORT"
echo "  公网服务器: localhost:$TUNNEL_PORT"
echo ""
echo "📋 常用命令:"
echo "  查看状态: sudo systemctl status blog-tunnel.service"
echo "  查看日志: sudo journalctl -u blog-tunnel.service -f"
echo "  重启服务: sudo systemctl restart blog-tunnel.service"
echo "  停止服务: sudo systemctl stop blog-tunnel.service"
echo ""
