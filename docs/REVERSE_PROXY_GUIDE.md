# 博客反向代理部署指南

## 概述

本指南说明如何将本地博客服务（172.18.8.107:8012）通过公网IP（101.35.79.76）的8012端口反向代理出去。

**配置信息：**
- 本地服务器：172.18.8.107:8012
- 公网服务器：101.35.79.76:8012
- 用户名：ubuntu
- 密码：Cjh041217@

## 快速部署（推荐）

### 方式1：一键部署（最简单）

```bash
cd scripts
bash quick-deploy.sh
```

这个脚本会自动：
1. 验证本地服务可访问
2. 生成Nginx配置
3. 连接到公网服务器
4. 安装Nginx（如果需要）
5. 配置反向代理
6. 启动服务

### 方式2：分步部署

**步骤1：在公网服务器上安装Nginx**

```bash
ssh ubuntu@101.35.79.76
# 输入密码: Cjh041217@

sudo apt-get update
sudo apt-get install -y nginx
```

**步骤2：创建Nginx配置文件**

```bash
sudo nano /etc/nginx/sites-available/blog-proxy
```

复制以下内容：

```nginx
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
```

**步骤3：启用配置**

```bash
sudo ln -s /etc/nginx/sites-available/blog-proxy /etc/nginx/sites-enabled/blog-proxy
```

**步骤4：测试配置**

```bash
sudo nginx -t
```

**步骤5：启动Nginx**

```bash
sudo systemctl restart nginx
sudo systemctl enable nginx
```

## 验证部署

### 测试反向代理

```bash
# 从本地测试
curl http://101.35.79.76:8012

# 或在浏览器中访问
http://101.35.79.76:8012
```

### 查看日志

```bash
# SSH连接到公网服务器
ssh ubuntu@101.35.79.76

# 查看访问日志
tail -f /var/log/nginx/blog-proxy-access.log

# 查看错误日志
tail -f /var/log/nginx/blog-proxy-error.log
```

## 功能测试

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

## 常见问题

### 1. 连接超时

**问题：** 访问 http://101.35.79.76:8012 超时

**解决：**
- 检查本地服务是否运行：`curl http://172.18.8.107:8012`
- 检查防火墙：`sudo ufw allow 8012`
- 检查Nginx状态：`sudo systemctl status nginx`

### 2. 502 Bad Gateway

**问题：** 返回502错误

**解决：**
- 检查本地服务是否可访问
- 查看Nginx错误日志：`tail -f /var/log/nginx/blog-proxy-error.log`
- 确认本地IP地址正确

### 3. 文件上传失败

**问题：** 上传大文件失败

**解决：**
- 确认Nginx配置中 `client_max_body_size` 设置为 25M
- 重启Nginx：`sudo systemctl restart nginx`

### 4. WebSocket连接失败

**问题：** 实时功能不工作

**解决：**
- 确认Nginx配置中包含WebSocket头设置
- 检查是否有其他代理干扰

## 管理命令

```bash
# 查看Nginx状态
sudo systemctl status nginx

# 重启Nginx
sudo systemctl restart nginx

# 停止Nginx
sudo systemctl stop nginx

# 启动Nginx
sudo systemctl start nginx

# 重新加载配置（不中断连接）
sudo systemctl reload nginx

# 查看Nginx进程
ps aux | grep nginx

# 测试配置文件
sudo nginx -t

# 查看Nginx版本
nginx -v
```

## 性能优化

### 启用缓存

在Nginx配置中添加缓存策略：

```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=blog_cache:10m max_size=1g inactive=60m;

location / {
    proxy_cache blog_cache;
    proxy_cache_valid 200 10m;
    proxy_cache_use_stale error timeout invalid_header updating;
    add_header X-Cache-Status $upstream_cache_status;
    
    # ... 其他配置
}
```

### 启用Gzip压缩

在Nginx配置中添加：

```nginx
gzip on;
gzip_types text/plain text/css text/javascript application/json;
gzip_min_length 1000;
```

## 安全建议

1. **使用HTTPS**：配置SSL证书
2. **限制访问速率**：防止DDoS
3. **隐藏Nginx版本**：`server_tokens off;`
4. **定期更新**：`sudo apt-get update && sudo apt-get upgrade`
5. **监控日志**：定期检查访问和错误日志

## 回滚

如需回滚配置：

```bash
# 禁用配置
sudo rm /etc/nginx/sites-enabled/blog-proxy

# 重启Nginx
sudo systemctl restart nginx
```

## 支持

如有问题，请检查：
1. 本地服务是否运行
2. 网络连接是否正常
3. Nginx配置是否正确
4. 防火墙规则是否允许8012端口
