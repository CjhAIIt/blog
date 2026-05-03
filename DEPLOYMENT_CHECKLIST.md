# 📋 部署执行清单

## 当前状态

✅ **已完成：**
- 前端UI重构（新设计风格）
- Maven构建完成 (52MB JAR)
- 部署脚本和文档已准备

⏳ **待执行：**
- 上传JAR到本地服务器
- 在本地服务器部署服务
- 配置公网反向代理
- 验证功能

---

## 🎯 执行步骤

### ✅ 第1步：上传JAR文件

**在你的本地机器（Windows）执行：**

```powershell
# 1. 打开 PowerShell

# 2. 测试SSH连接
ssh lab@172.18.8.107 "echo 'Connection test'"
# 输入密码: a6n107
# 预期输出: Connection test

# 3. 上传JAR文件
scp D:\blog-master\target\blog-0.0.1-SNAPSHOT.jar lab@172.18.8.107:/tmp/
# 输入密码: a6n107
# 等待上传完成（约1-2分钟）

# 4. 验证上传
ssh lab@172.18.8.107 "ls -lh /tmp/blog-0.0.1-SNAPSHOT.jar"
# 输入密码: a6n107
# 预期输出: -rw-r--r-- 1 lab lab 52M ... blog-0.0.1-SNAPSHOT.jar
```

**✓ 完成标志：** 看到JAR文件大小为52M

---

### ✅ 第2步：在本地服务器部署

**执行：**

```powershell
ssh lab@172.18.8.107
# 输入密码: a6n107
```

**然后在远程服务器上执行以下命令块：**

#### 2.1 创建目录和部署JAR

```bash
sudo mkdir -p /opt/blog
cd /opt/blog
sudo mv /tmp/blog-0.0.1-SNAPSHOT.jar /opt/blog/
sudo chown lab:lab /opt/blog/blog-0.0.1-SNAPSHOT.jar
sudo mkdir -p /opt/blog/uploads
sudo chown lab:lab /opt/blog/uploads
sudo chmod 755 /opt/blog/uploads
ls -lh /opt/blog/blog-0.0.1-SNAPSHOT.jar
```

**✓ 完成标志：** 看到JAR文件列表

#### 2.2 创建systemd服务

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

#### 2.3 启动服务

```bash
sudo systemctl daemon-reload
sudo systemctl enable blog
sudo systemctl start blog
sleep 3
sudo systemctl status blog
```

**✓ 完成标志：** 看到 `Active: active (running)`

#### 2.4 查看启动日志

```bash
sudo journalctl -u blog -f
```

**等待看到：**
```
Started Blog Service
Tomcat started on port(s): 8012
```

**按 Ctrl+C 退出日志查看**

#### 2.5 验证本地访问

```bash
curl -I http://localhost:8012/
```

**✓ 完成标志：** 看到 `HTTP/1.1 200`

#### 2.6 退出远程服务器

```bash
exit
```

---

### ✅ 第3步：配置公网反向代理

**在你的本地机器执行：**

```powershell
cd D:\blog-master
bash scripts/deploy-proxy.sh
```

**脚本会提示输入公网服务器密码：**
- 用户: ubuntu
- 密码: Cjh041217@

**✓ 完成标志：** 看到 `✅ 部署完成！`

---

### ✅ 第4步：验证部署

**在你的本地机器执行：**

```powershell
# 测试内网访问
curl -I http://172.18.8.107:8012/

# 测试外网访问
curl -I http://101.35.79.76:8012/
```

**✓ 完成标志：** 两个都返回 `HTTP/1.1 200`

**然后在浏览器打开：**
```
http://101.35.79.76:8012
```

---

## 🧪 功能验证

在浏览器中访问 http://101.35.79.76:8012，逐一检查：

- [ ] **首页加载** - 看到新设计风格（蓝色配色、干净排版）
- [ ] **导航菜单** - 文章、关于、榜单、计划都可点击
- [ ] **登录页面** - 访问 /login 可以看到登录表单
- [ ] **登录功能** - 用已有账号登录
- [ ] **写文章** - 点击"写文章"按钮
- [ ] **上传图片** - 上传封面图片
- [ ] **文章详情** - 点击文章查看详情页
- [ ] **移动端** - 用手机或浏览器DevTools查看移动版
- [ ] **静态资源** - 检查CSS/JS/图片是否加载
- [ ] **搜索功能** - 搜索文章
- [ ] **榜单** - 查看排行榜

---

## 📊 部署进度

```
[████████████████████████████████████████] 100%

✅ 前端重构完成
✅ JAR构建完成
⏳ 上传JAR到本地服务器
⏳ 在本地服务器部署
⏳ 配置公网反向代理
⏳ 验证功能
```

---

## 🆘 需要帮助？

如果遇到问题，请提供：

1. **错误信息** - 完整的错误输出
2. **执行的命令** - 你运行的具体命令
3. **当前状态** - 卡在哪一步
4. **日志内容** - 相关的日志输出

常见问题的日志查看命令：

```bash
# 查看本地服务日志
ssh lab@172.18.8.107
sudo journalctl -u blog -n 100

# 查看公网服务日志
ssh ubuntu@101.35.79.76
sudo tail -f /var/log/nginx/blog-proxy-error.log
```

---

## 📌 重要提醒

1. **数据库密码** - 必须替换 `your_db_password`
2. **网络连接** - 确保能SSH连接到两个服务器
3. **端口开放** - 确保8012端口未被占用
4. **MySQL运行** - 确保MySQL服务在本地服务器上运行

---

**准备好了吗？从第1步开始执行！**
