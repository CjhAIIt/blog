# 🚀 博客部署 - 快速开始指南

## 📦 已准备的文件

✅ JAR包已构建：`D:\blog-master\target\blog-0.0.1-SNAPSHOT.jar` (52MB)

✅ 部署脚本已准备：
- `scripts/deploy-local.sh` - 本地服务器部署脚本
- `scripts/deploy-proxy.sh` - 公网服务器反向代理配置
- `scripts/setup-reverse-proxy.sh` - Nginx配置脚本

✅ 文档已准备：
- `DEPLOYMENT_STEPS.md` - 详细部署步骤
- `docs/DEPLOYMENT_GUIDE.md` - 完整部署指南

---

## 🎯 三步快速部署

### 第一步：上传JAR到本地服务器

在你的本地机器（Windows）打开 PowerShell，执行：

```powershell
# 1. 测试连接
ssh lab@172.18.8.107 "echo 'OK'"
# 输入密码: a6n107

# 2. 上传JAR
scp D:\blog-master\target\blog-0.0.1-SNAPSHOT.jar lab@172.18.8.107:/tmp/
# 输入密码: a6n107

# 3. 验证上传
ssh lab@172.18.8.107 "ls -lh /tmp/blog-0.0.1-SNAPSHOT.jar"
```

---

### 第二步：在本地服务器上部署

SSH到本地服务器：

```powershell
ssh lab@172.18.8.107
# 输入密码: a6n107
```

然后在远程服务器上执行（复制粘贴以下所有命令）：

```bash
# 创建项目目录
sudo mkdir -p /opt/blog
cd /opt/blog

# 移动JAR
sudo mv /tmp/blog-0.0.1-SNAPSHOT.jar /opt/blog/
sudo chown lab:lab /opt/blog/blog-0.0.1-SNAPSHOT.jar

# 创建上传目录
sudo mkdir -p /opt/blog/uploads
sudo chown lab:lab /opt/blog/uploads
sudo chmod 755 /opt/blog/uploads

# 创建systemd服务
sudo cat > /etc/systemd/system/blog.service << 'EOF'
[Unit]
Description=Blog Service
After=network.target mysql.service

[Service]
Type=simple
User=lab
WorkingDirectory=/opt/blog
ExecStart=/usr/bin/java -jar /opt/blog/blog-0.0.1-SNAPSHOT.jar \
  --server.port=8012 \
  --spring.datasource.url=jdbc:mysql://localhost:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai \
  --spring.datasource.username=root \
  --spring.datasource.password=your_db_password \
  --app.storage.upload-dir=/opt/blog/uploads

Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable blog
sudo systemctl start blog

# 检查状态
sudo systemctl status blog

# 查看日志（按Ctrl+C退出）
sudo journalctl -u blog -f
```

**⚠️ 重要：** 将上面的 `your_db_password` 替换为实际的MySQL root密码

等待看到日志中出现：
```
Started Blog Service
Tomcat started on port(s): 8012
```

然后按 `Ctrl+C` 退出，执行：

```bash
# 验证本地访问
curl -I http://localhost:8012/

# 退出远程服务器
exit
```

---

### 第三步：配置公网反向代理

回到你的本地机器，执行：

```powershell
cd D:\blog-master
bash scripts/deploy-proxy.sh
```

这个脚本会自动：
- 连接到公网服务器 (101.35.79.76)
- 配置Nginx反向代理
- 重启Nginx服务

---

## ✅ 验证部署

### 验证本地访问

```powershell
# 测试内网
curl -I http://172.18.8.107:8012/

# 或在浏览器打开
# http://172.18.8.107:8012
```

### 验证外网访问

```powershell
# 测试外网
curl -I http://101.35.79.76:8012/

# 或在浏览器打开
# http://101.35.79.76:8012
```

---

## 🧪 功能测试清单

部署完成后，在浏览器中访问 http://101.35.79.76:8012，检查：

- [ ] 首页加载（新设计风格）
- [ ] 导航菜单可用
- [ ] 登录功能正常
- [ ] 可以写新文章
- [ ] 可以上传图片
- [ ] 文章详情页显示正确
- [ ] 移动端响应式
- [ ] 静态资源加载（CSS/JS/图片）

---

## 🔧 常用命令

### 本地服务器

```bash
# SSH到本地服务器
ssh lab@172.18.8.107

# 查看服务状态
sudo systemctl status blog

# 查看实时日志
sudo journalctl -u blog -f

# 重启服务
sudo systemctl restart blog

# 停止服务
sudo systemctl stop blog
```

### 公网服务器

```bash
# SSH到公网服务器
ssh ubuntu@101.35.79.76
# 密码: Cjh041217@

# 查看Nginx日志
sudo tail -f /var/log/nginx/blog-proxy-access.log
sudo tail -f /var/log/nginx/blog-proxy-error.log

# 重启Nginx
sudo systemctl restart nginx
```

---

## 📝 部署配置参考

**本地服务器：**
- IP: 172.18.8.107
- 用户: lab
- 密码: a6n107
- 项目目录: /opt/blog
- 端口: 8012

**公网服务器：**
- IP: 101.35.79.76
- 用户: ubuntu
- 密码: Cjh041217@
- 反向代理端口: 8012

**数据库：**
- 主机: localhost
- 数据库: blogdb
- 用户: root
- 密码: (需要替换)

---

## 🐛 故障排查

### 502 Bad Gateway
```bash
# 检查本地服务
ssh lab@172.18.8.107
sudo systemctl status blog
sudo journalctl -u blog -n 50
```

### 无法上传文件
```bash
# 检查上传目录权限
ssh lab@172.18.8.107
sudo chown lab:lab /opt/blog/uploads
sudo chmod 755 /opt/blog/uploads
```

### 静态资源404
```bash
# 检查Nginx日志
ssh ubuntu@101.35.79.76
sudo tail -f /var/log/nginx/blog-proxy-error.log
```

---

## 📌 下一步

1. **执行上述三个步骤**
2. **验证所有功能正常**
3. **监控日志和性能**
4. **考虑配置HTTPS**

---

**准备好开始部署了吗？按照上述步骤执行即可！**
