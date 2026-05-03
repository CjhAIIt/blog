# 博客部署执行指南

## 📋 部署流程概览

```
1. 上传JAR到本地服务器 (172.18.8.107)
   ↓
2. 在本地服务器上部署服务
   ↓
3. 验证本地服务运行
   ↓
4. 配置公网服务器反向代理 (101.35.79.76)
   ↓
5. 验证外网访问
```

---

## 🚀 执行步骤

### 第一步：上传JAR文件

在你的本地机器（Windows）上打开 PowerShell 或 CMD，执行：

```powershell
# 测试连接
ssh lab@172.18.8.107 "echo 'Connection OK'"
```

输入密码：`a6n107`

如果连接成功，执行上传：

```powershell
scp D:\blog-master\target\blog-0.0.1-SNAPSHOT.jar lab@172.18.8.107:/tmp/
```

输入密码：`a6n107`

**验证上传：**
```powershell
ssh lab@172.18.8.107 "ls -lh /tmp/blog-0.0.1-SNAPSHOT.jar"
```

---

### 第二步：在本地服务器上部署

SSH到本地服务器：

```powershell
ssh lab@172.18.8.107
```

输入密码：`a6n107`

然后在远程服务器上执行以下命令：

#### 2.1 创建项目目录

```bash
sudo mkdir -p /opt/blog
cd /opt/blog
```

#### 2.2 移动JAR文件

```bash
sudo mv /tmp/blog-0.0.1-SNAPSHOT.jar /opt/blog/
sudo chown lab:lab /opt/blog/blog-0.0.1-SNAPSHOT.jar
ls -lh /opt/blog/blog-0.0.1-SNAPSHOT.jar
```

#### 2.3 创建上传目录

```bash
sudo mkdir -p /opt/blog/uploads
sudo chown lab:lab /opt/blog/uploads
sudo chmod 755 /opt/blog/uploads
```

#### 2.4 创建systemd服务文件

```bash
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
```

**⚠️ 重要：** 将 `your_db_password` 替换为实际的MySQL root密码

#### 2.5 启用并启动服务

```bash
sudo systemctl daemon-reload
sudo systemctl enable blog
sudo systemctl start blog
```

#### 2.6 检查服务状态

```bash
sudo systemctl status blog
```

应该看到 `Active: active (running)`

#### 2.7 查看日志

```bash
sudo journalctl -u blog -f
```

等待看到类似的日志：
```
Started Blog Service
Tomcat started on port(s): 8012
```

按 `Ctrl+C` 退出日志查看

#### 2.8 验证本地访问

```bash
curl -I http://localhost:8012/
```

应该返回 `HTTP/1.1 200`

#### 2.9 退出远程服务器

```bash
exit
```

---

### 第三步：验证本地服务

在你的本地机器上测试：

```powershell
# 测试内网访问
curl -I http://172.18.8.107:8012/

# 或在浏览器中打开
# http://172.18.8.107:8012
```

应该看到博客首页加载。

---

### 第四步：配置公网服务器反向代理

在你的本地机器上，进入项目目录：

```powershell
cd D:\blog-master
bash scripts/deploy-proxy.sh
```

这个脚本会：
- 连接到公网服务器 (101.35.79.76)
- 安装/验证Nginx
- 配置反向代理
- 重启Nginx服务

---

### 第五步：验证外网访问

在你的本地机器上测试：

```powershell
# 测试外网访问
curl -I http://101.35.79.76:8012/
```

应该返回 `HTTP/1.1 200`

然后在浏览器中打开：
```
http://101.35.79.76:8012
```

---

## ✅ 验证清单

部署完成后，检查以下功能：

- [ ] 首页加载正常
- [ ] 导航菜单可用（文章、关于、榜单、计划）
- [ ] 登录页面可访问
- [ ] 可以登录
- [ ] 可以写新文章
- [ ] 可以上传封面图片
- [ ] 可以编辑文章
- [ ] 可以查看文章详情
- [ ] 移动端响应式正常
- [ ] 静态资源加载正常（CSS、JS、图片）
- [ ] 搜索功能正常
- [ ] 榜单显示正确

---

## 🔧 常用命令

### 本地服务器命令

```bash
# 查看服务状态
sudo systemctl status blog

# 查看实时日志
sudo journalctl -u blog -f

# 查看最近100行日志
sudo journalctl -u blog -n 100

# 重启服务
sudo systemctl restart blog

# 停止服务
sudo systemctl stop blog

# 启动服务
sudo systemctl start blog

# 查看8012端口
netstat -tlnp | grep 8012
ss -tlnp | grep 8012
```

### 公网服务器命令

```bash
# SSH到公网服务器
ssh ubuntu@101.35.79.76
# 密码: Cjh041217@

# 查看Nginx状态
sudo systemctl status nginx

# 查看Nginx访问日志
sudo tail -f /var/log/nginx/blog-proxy-access.log

# 查看Nginx错误日志
sudo tail -f /var/log/nginx/blog-proxy-error.log

# 重启Nginx
sudo systemctl restart nginx

# 测试Nginx配置
sudo nginx -t
```

---

## 🐛 故障排查

### 问题1：502 Bad Gateway

**原因：** 本地服务未运行或无法连接

**解决：**
```bash
# 在本地服务器上
sudo systemctl status blog
sudo journalctl -u blog -n 100

# 检查8012端口
netstat -tlnp | grep 8012

# 检查MySQL连接
mysql -u root -p -e "SELECT 1;"
```

### 问题2：无法上传文件

**原因：** 上传目录不存在或权限不足

**解决：**
```bash
# 在本地服务器上
sudo mkdir -p /opt/blog/uploads
sudo chown lab:lab /opt/blog/uploads
sudo chmod 755 /opt/blog/uploads
```

### 问题3：登录失败

**原因：** Cookie/Session配置问题

**解决：**
```bash
# 检查数据库中的用户表
mysql -u root -p blogdb -e "SELECT * FROM users LIMIT 5;"
```

### 问题4：静态资源404

**原因：** 反向代理路径配置问题

**解决：**
```bash
# 在公网服务器上
sudo tail -f /var/log/nginx/blog-proxy-error.log

# 测试本地资源访问
curl -I http://172.18.8.107:8012/css/site.css
```

---

## 📝 部署完成后

1. **备份配置**：保存 `/etc/systemd/system/blog.service` 和 `/etc/nginx/sites-available/blog-proxy`
2. **监控日志**：定期检查服务日志
3. **性能优化**：考虑启用缓存、Gzip压缩
4. **安全加固**：配置HTTPS、防火墙规则

---

**部署指南完成！按照上述步骤执行，有问题随时反馈。**
