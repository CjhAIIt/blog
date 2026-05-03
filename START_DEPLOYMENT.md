# 🎯 博客部署 - 最终执行指南

## 📦 部署包内容

你现在拥有完整的部署包：

```
D:\blog-master\
├── target/
│   └── blog-0.0.1-SNAPSHOT.jar (52MB) ✅ 已构建
├── scripts/
│   ├── deploy-check.sh          ✅ 环境检查
│   ├── deploy-local.sh          ✅ 本地部署
│   ├── deploy-proxy.sh          ✅ 反向代理配置
│   └── setup-reverse-proxy.sh   ✅ Nginx配置
├── docs/
│   ├── DEPLOYMENT_GUIDE.md      ✅ 完整指南
│   └── FRONTEND_REFACTOR_REPORT.md ✅ UI重构报告
├── QUICK_START.md               ✅ 快速开始
├── DEPLOYMENT_STEPS.md          ✅ 详细步骤
└── DEPLOYMENT_CHECKLIST.md      ✅ 执行清单
```

---

## 🚀 三步快速部署

### 第一步：上传JAR（5分钟）

在你的本地机器打开 PowerShell，执行：

```powershell
# 上传JAR到本地服务器
scp D:\blog-master\target\blog-0.0.1-SNAPSHOT.jar lab@172.18.8.107:/tmp/
```

密码：`a6n107`

---

### 第二步：部署服务（10分钟）

SSH到本地服务器：

```powershell
ssh lab@172.18.8.107
```

密码：`a6n107`

然后在远程服务器执行（复制整个代码块）：

```bash
# 部署JAR和创建服务
sudo mkdir -p /opt/blog && cd /opt/blog
sudo mv /tmp/blog-0.0.1-SNAPSHOT.jar /opt/blog/
sudo chown lab:lab /opt/blog/blog-0.0.1-SNAPSHOT.jar
sudo mkdir -p /opt/blog/uploads && sudo chown lab:lab /opt/blog/uploads && sudo chmod 755 /opt/blog/uploads

# 创建systemd服务（注意替换数据库密码）
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
sleep 3

# 验证
sudo systemctl status blog
sudo journalctl -u blog -f
```

等待看到 `Tomcat started on port(s): 8012`，然后按 Ctrl+C 退出，执行 `exit` 离开远程服务器。

---

### 第三步：配置反向代理（5分钟）

回到你的本地机器，执行：

```powershell
cd D:\blog-master
bash scripts/deploy-proxy.sh
```

脚本会自动配置公网服务器的Nginx反向代理。

---

## ✅ 验证部署

```powershell
# 测试内网访问
curl -I http://172.18.8.107:8012/

# 测试外网访问
curl -I http://101.35.79.76:8012/

# 在浏览器打开
# http://101.35.79.76:8012
```

---

## 📋 完整清单

| 步骤 | 任务 | 状态 | 预计时间 |
|------|------|------|---------|
| 1 | 上传JAR到本地服务器 | ⏳ 待执行 | 5分钟 |
| 2 | 在本地服务器部署服务 | ⏳ 待执行 | 10分钟 |
| 3 | 配置公网反向代理 | ⏳ 待执行 | 5分钟 |
| 4 | 验证功能 | ⏳ 待执行 | 5分钟 |

**总计：约25分钟**

---

## 🔑 关键信息

**本地服务器 (172.18.8.107)：**
- 用户: lab
- 密码: a6n107
- 项目目录: /opt/blog
- 端口: 8012

**公网服务器 (101.35.79.76)：**
- 用户: ubuntu
- 密码: Cjh041217@
- 反向代理端口: 8012

**数据库：**
- 主机: localhost
- 数据库: blogdb
- 用户: root
- 密码: **需要替换**

---

## 📚 详细文档

如需详细信息，查看：
- `DEPLOYMENT_CHECKLIST.md` - 逐步执行清单
- `DEPLOYMENT_STEPS.md` - 详细步骤说明
- `docs/DEPLOYMENT_GUIDE.md` - 完整部署指南
- `QUICK_START.md` - 快速参考

---

## 🎯 现在就开始！

**第一步：上传JAR**

在你的本地机器打开 PowerShell，执行：

```powershell
scp D:\blog-master\target\blog-0.0.1-SNAPSHOT.jar lab@172.18.8.107:/tmp/
```

完成后告诉我，我会指导你进行下一步。

---

**准备好了吗？开始执行第一步！**
