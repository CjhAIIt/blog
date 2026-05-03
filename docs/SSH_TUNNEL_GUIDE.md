# SSH反向隧道部署指南

## 概述

由于本地服务器（172.18.8.107）和公网服务器（101.35.79.76）在不同网络，无法直接通信。使用SSH反向隧道方案：

```
本地服务器 (172.18.8.107:8012)
    ↓
SSH反向隧道 (SSH连接到公网服务器)
    ↓
公网服务器 (127.0.0.1:9012)
    ↓
Nginx反向代理 (监听8012)
    ↓
公网访问 (http://101.35.79.76:8012)
```

## 快速部署

### 步骤1：在本地服务器上建立隧道

#### 方式A：PowerShell（推荐，Windows）

**一次性运行隧道：**
```powershell
cd D:\blog-master\scripts
.\tunnel.ps1
```

**安装为后台服务（开机自启）：**
```powershell
cd D:\blog-master\scripts
.\tunnel.ps1 -Install
.\tunnel.ps1 -Start
```

**管理隧道：**
```powershell
# 查看状态
.\tunnel.ps1 -Status

# 启动隧道
.\tunnel.ps1 -Start

# 停止隧道
.\tunnel.ps1 -Stop
```

#### 方式B：Batch脚本（Windows）

```batch
cd D:\blog-master\scripts
tunnel.bat
```

#### 方式C：Bash脚本（Linux/Mac）

```bash
cd scripts
bash setup-tunnel.sh
```

### 步骤2：验证隧道

在本地服务器上运行：
```bash
# 检查隧道是否建立
netstat -an | grep 9012

# 或者
ss -tlnp | grep 9012
```

在公网服务器上运行：
```bash
ssh ubuntu@101.35.79.76
netstat -an | grep 9012
```

### 步骤3：测试反向代理

```bash
# 从任何地方访问
curl http://101.35.79.76:8012

# 或在浏览器中访问
http://101.35.79.76:8012
```

## 详细说明

### 隧道工作原理

1. **本地隧道进程**：在本地服务器上运行SSH客户端
2. **SSH连接**：建立到公网服务器的SSH连接
3. **端口转发**：将公网服务器的9012端口转发到本地的8012端口
4. **Nginx反向代理**：公网服务器的Nginx监听8012，代理到127.0.0.1:9012

### 配置参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 本地服务器 | 172.18.8.107 | 内网IP |
| 本地端口 | 8012 | 博客服务端口 |
| 公网服务器 | 101.35.79.76 | 公网IP |
| 隧道端口 | 9012 | SSH转发端口 |
| 公网端口 | 8012 | 对外暴露端口 |

## 故障排查

### 问题1：SSH连接失败

**症状：** 运行隧道脚本后立即断开

**解决：**
```bash
# 测试SSH连接
ssh -v ubuntu@101.35.79.76

# 检查SSH密钥
ls ~/.ssh/id_rsa

# 如果没有密钥，生成一个
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ""

# 复制公钥到公网服务器
ssh-copy-id -i ~/.ssh/id_rsa.pub ubuntu@101.35.79.76
```

### 问题2：隧道建立但无法访问

**症状：** 隧道连接成功，但访问http://101.35.79.76:8012返回502

**解决：**
```bash
# 在公网服务器上检查
ssh ubuntu@101.35.79.76

# 检查隧道端口是否监听
netstat -tlnp | grep 9012

# 检查Nginx配置
sudo nginx -t

# 查看Nginx错误日志
sudo tail -f /var/log/nginx/blog-proxy-error.log

# 重启Nginx
sudo systemctl restart nginx
```

### 问题3：隧道断开

**症状：** 隧道连接一段时间后断开

**解决：**
- 使用PowerShell脚本的`-Install`选项安装为后台服务
- 或使用Linux的systemd服务自动重连

### 问题4：本地服务无法访问

**症状：** 隧道建立，但本地服务返回错误

**解决：**
```bash
# 检查本地服务是否运行
curl http://localhost:8012

# 检查防火墙
sudo ufw allow 8012

# 检查服务日志
# 根据你的应用查看日志
```

## 性能优化

### 启用SSH压缩

编辑隧道脚本，添加`-C`参数：
```bash
ssh -C -o StrictHostKeyChecking=no -N -R 9012:localhost:8012 ubuntu@101.35.79.76
```

### 启用连接复用

创建`~/.ssh/config`：
```
Host 101.35.79.76
    HostName 101.35.79.76
    User ubuntu
    ControlMaster auto
    ControlPath ~/.ssh/control-%h-%p-%r
    ControlPersist 600
```

## 安全建议

1. **使用SSH密钥认证**（而不是密码）
2. **限制SSH访问**：在公网服务器上配置`/etc/ssh/sshd_config`
3. **监控隧道连接**：定期检查日志
4. **定期更新**：保持SSH和系统更新
5. **使用VPN**：如果可能，使用VPN而不是SSH隧道

## 常用命令

### PowerShell

```powershell
# 查看隧道状态
Get-ScheduledTask -TaskName BlogTunnel

# 查看隧道日志
Get-EventLog -LogName System -Source TaskScheduler | Where-Object {$_.Message -like "*BlogTunnel*"}

# 手动启动隧道
Start-ScheduledTask -TaskName BlogTunnel

# 手动停止隧道
Stop-ScheduledTask -TaskName BlogTunnel

# 删除隧道服务
Unregister-ScheduledTask -TaskName BlogTunnel -Confirm:$false
```

### Linux/Mac

```bash
# 查看隧道状态
sudo systemctl status blog-tunnel.service

# 查看隧道日志
sudo journalctl -u blog-tunnel.service -f

# 重启隧道
sudo systemctl restart blog-tunnel.service

# 停止隧道
sudo systemctl stop blog-tunnel.service

# 启动隧道
sudo systemctl start blog-tunnel.service
```

## 测试功能

部署完成后，测试以下功能：

- [ ] 访问首页：http://101.35.79.76:8012
- [ ] 查看文章列表
- [ ] 查看单篇文章
- [ ] 创建新文章
- [ ] 编辑文章
- [ ] 删除文章
- [ ] 上传图片/文件
- [ ] 用户登录/注册
- [ ] 管理员功能

## 监控和维护

### 定期检查

```bash
# 检查隧道连接
ssh ubuntu@101.35.79.76 "netstat -tlnp | grep 9012"

# 检查Nginx状态
ssh ubuntu@101.35.79.76 "sudo systemctl status nginx"

# 查看访问日志
ssh ubuntu@101.35.79.76 "tail -f /var/log/nginx/blog-proxy-access.log"
```

### 日志位置

- **本地隧道日志**：PowerShell任务计划程序日志
- **公网Nginx访问日志**：`/var/log/nginx/blog-proxy-access.log`
- **公网Nginx错误日志**：`/var/log/nginx/blog-proxy-error.log`

## 回滚

如需停止反向代理：

```bash
# 停止本地隧道
.\tunnel.ps1 -Stop

# 或在公网服务器上禁用Nginx配置
ssh ubuntu@101.35.79.76
sudo rm /etc/nginx/sites-enabled/blog-proxy
sudo systemctl restart nginx
```

## 支持

如有问题，请检查：
1. SSH连接是否正常
2. 隧道端口是否监听
3. Nginx配置是否正确
4. 本地服务是否运行
5. 防火墙规则是否允许
