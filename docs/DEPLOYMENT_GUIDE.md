# 博客项目完整部署方案

## 📋 项目信息

- **项目类型**：Spring Boot 3.2.0 + Thymeleaf + MySQL
- **Java版本**：17+
- **构建工具**：Maven 3.9.14
- **当前端口**：8012
- **本地服务器**：172.18.8.107 (lab用户)
- **公网服务器**：101.35.79.76 (ubuntu用户)
- **公网访问地址**：http://101.35.79.76:8012

---

## 第一步：检查本地服务器当前状态

### 1.1 SSH连接到本地服务器

```bash
ssh lab@172.18.8.107
# 输入密码：a6n107
```

### 1.2 检查当前运行的博客服务

```bash
# 检查Java进程
ps aux | grep java | grep -v grep

# 检查systemd服务
systemctl list-units --type=service --state=running | grep -i blog

# 检查pm2进程
pm2 list

# 检查Docker容器
docker ps | grep -i blog

# 检查8012端口
netstat -tlnp | grep 8012
ss -tlnp | grep 8012

# 检查MySQL
systemctl status mysql
systemctl status mariadb
docker ps | grep mysql
```

### 1.3 检查项目目录和数据

```bash
# 查找项目目录
find /opt -name "blog*" -type d
find /home -name "blog*" -type d

# 查找上传文件目录
find /opt -name "uploads" -o -name "storage" -o -name "public"

# 查找配置文件
find /opt -name "application*.properties" -o -name "application*.yml"

# 查看MySQL数据库
mysql -u root -p -e "SHOW DATABASES;" | grep -i blog
```

---

## 第二步：备份旧服务（重要！）

### ⚠️ 风险提示
- 此步骤会备份旧服务的所有文件和配置
- **不会**删除数据库数据
- **不会**删除上传文件
- 备份文件保存在 `/opt/blog-backup-YYYYMMDD-HHMMSS/`

### 2.1 停止旧服务

```bash
# 如果是systemd服务
sudo systemctl stop blog

# 如果是pm2
pm2 stop blog
pm2 delete blog

# 如果是Docker
docker stop blog-service
docker-compose -f /path/to/docker-compose.yml down

# 如果是直接Java进程，找到PID后杀死
ps aux | grep java | grep blog
kill -9 <PID>
```

### 2.2 备份旧项目和配置

```bash
# 创建备份目录
BACKUP_DIR="/opt/blog-backup-$(date +%Y%m%d-%H%M%S)"
sudo mkdir -p $BACKUP_DIR

# 备份旧项目
sudo cp -r /opt/blog $BACKUP_DIR/blog-old || true
sudo cp -r /home/lab/blog $BACKUP_DIR/blog-old || true

# 备份配置文件
sudo cp -r /etc/systemd/system/blog* $BACKUP_DIR/ || true
sudo cp -r /etc/nginx/sites-available/blog* $BACKUP_DIR/ || true
sudo cp -r /etc/nginx/conf.d/blog* $BACKUP_DIR/ || true
sudo cp -r ~/.env $BACKUP_DIR/ || true
sudo cp -r /opt/blog/application*.properties $BACKUP_DIR/ || true
sudo cp -r /opt/blog/application*.yml $BACKUP_DIR/ || true

# 备份Docker配置
sudo cp -r /opt/docker-compose.yml $BACKUP_DIR/ || true
sudo cp -r /opt/Dockerfile $BACKUP_DIR/ || true

# 列出备份内容
ls -la $BACKUP_DIR/
```

### 2.3 验证数据库和上传文件未被删除

```bash
# 检查MySQL数据
mysql -u root -p -e "SHOW DATABASES;" | grep -i blog

# 检查上传文件
ls -la /opt/blog/uploads/ || true
ls -la /opt/blog/storage/ || true
ls -la /opt/blog/public/uploads/ || true
```

---

## 第三步：上传和构建新项目

### 3.1 在本地机器上构建JAR包

```bash
# 在你的开发机器上（Windows/Mac/Linux）
cd D:/blog-master

# 清理旧构建
mvn clean

# 构建项目（跳过测试以加快速度）
mvn package -DskipTests -q

# 验证JAR包生成
ls -lh target/blog-0.0.1-SNAPSHOT.jar
```

### 3.2 上传JAR包到本地服务器

```bash
# 在本地机器上执行
scp target/blog-0.0.1-SNAPSHOT.jar lab@172.18.8.107:/tmp/

# 或使用rsync（更快）
rsync -avz target/blog-0.0.1-SNAPSHOT.jar lab@172.18.8.107:/tmp/
```

### 3.3 在本地服务器上部署JAR包

```bash
# SSH到本地服务器
ssh lab@172.18.8.107

# 创建项目目录
sudo mkdir -p /opt/blog
cd /opt/blog

# 移动JAR包
sudo mv /tmp/blog-0.0.1-SNAPSHOT.jar /opt/blog/
sudo chown lab:lab /opt/blog/blog-0.0.1-SNAPSHOT.jar

# 创建启动脚本
cat > /opt/blog/start.sh << 'EOF'
#!/bin/bash
cd /opt/blog
java -jar blog-0.0.1-SNAPSHOT.jar \
  --server.port=8012 \
  --spring.datasource.url=jdbc:mysql://localhost:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai \
  --spring.datasource.username=root \
  --spring.datasource.password=your_db_password \
  --app.storage.upload-dir=/opt/blog/uploads
EOF

chmod +x /opt/blog/start.sh
```

---

## 第四步：配置systemd服务（推荐）

### 4.1 创建systemd服务文件

```bash
# 在本地服务器上
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

# 重新加载systemd配置
sudo systemctl daemon-reload

# 启用开机自启
sudo systemctl enable blog

# 启动服务
sudo systemctl start blog

# 检查服务状态
sudo systemctl status blog

# 查看日志
sudo journalctl -u blog -f
```

---

## 第五步：本地验证

### 5.1 检查服务是否正常运行

```bash
# 在本地服务器上
# 检查8012端口是否监听
netstat -tlnp | grep 8012
ss -tlnp | grep 8012

# 检查Java进程
ps aux | grep java | grep blog

# 查看日志
sudo journalctl -u blog -n 50
```

### 5.2 测试本地访问

```bash
# 在本地服务器上
# 测试首页
curl -I http://localhost:8012/

# 测试API
curl http://localhost:8012/api/posts

# 测试登录页
curl -I http://localhost:8012/login
```

### 5.3 浏览器验证（如果有GUI）

```
在本地服务器的浏览器中访问：
http://localhost:8012

检查：
- 首页是否加载
- 文章列表是否显示
- 登录功能是否正常
- 上传功能是否正常
```

---

## 第六步：配置公网服务器反向代理

### 6.1 SSH到公网服务器

```bash
ssh ubuntu@101.35.79.76
# 输入密码：Cjh041217@
```

### 6.2 检查Nginx是否安装

```bash
# 检查Nginx
nginx -v
sudo systemctl status nginx

# 如果未安装，执行
sudo apt-get update
sudo apt-get install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 6.3 创建Nginx反向代理配置

```bash
# 在公网服务器上
sudo cat > /etc/nginx/sites-available/blog-proxy << 'EOF'
upstream blog_backend {
    server 172.18.8.107:8012;
    keepalive 32;
}

server {
    listen 8012;
    listen [::]:8012;
    server_name _;

    # 增加请求体大小限制（支持文件上传）
    client_max_body_size 100M;

    # 日志
    access_log /var/log/nginx/blog-proxy-access.log;
    error_log /var/log/nginx/blog-proxy-error.log;

    location / {
        proxy_pass http://blog_backend;

        # 保留原始请求信息
        proxy_set_header Host $host:$server_port;
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
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;

        # 缓冲设置
        proxy_buffering off;
        proxy_request_buffering off;
    }
}
EOF

# 启用配置
sudo ln -sf /etc/nginx/sites-available/blog-proxy /etc/nginx/sites-enabled/blog-proxy

# 删除默认配置（可选）
sudo rm -f /etc/nginx/sites-enabled/default

# 测试Nginx配置
sudo nginx -t

# 重启Nginx
sudo systemctl restart nginx
```

### 6.4 配置防火墙

```bash
# 在公网服务器上
# 允许8012端口
sudo ufw allow 8012/tcp

# 检查防火墙状态
sudo ufw status
```

### 6.5 验证反向代理

```bash
# 在公网服务器上
# 测试本地反向代理
curl -I http://127.0.0.1:8012/

# 测试公网IP反向代理
curl -I http://101.35.79.76:8012/

# 查看Nginx日志
sudo tail -f /var/log/nginx/blog-proxy-access.log
sudo tail -f /var/log/nginx/blog-proxy-error.log
```

---

## 第七步：外网验证

### 7.1 在浏览器中访问

```
http://101.35.79.76:8012
```

### 7.2 验证功能清单

- [ ] 首页加载正常
- [ ] 文章列表显示
- [ ] 文章详情可打开
- [ ] 登录页面正常
- [ ] 登录功能正常
- [ ] 写文章功能正常
- [ ] 文件上传正常
- [ ] 后台管理正常
- [ ] 静态资源加载正常（CSS/JS/图片）
- [ ] API请求正常
- [ ] Cookie/Token正常

### 7.3 测试命令

```bash
# 从外部机器执行
# 测试首页
curl -I http://101.35.79.76:8012/

# 测试API
curl http://101.35.79.76:8012/api/posts

# 测试登录
curl -c cookies.txt -d "username=admin&password=password" http://101.35.79.76:8012/login

# 测试文件上传
curl -F "file=@test.jpg" http://101.35.79.76:8012/api/upload
```

---

## 故障排查

### 问题1：502 Bad Gateway

**原因**：本地服务未运行或无法连接

**解决**：
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

**原因**：上传目录不存在或权限不足

**解决**：
```bash
# 在本地服务器上
sudo mkdir -p /opt/blog/uploads
sudo chown lab:lab /opt/blog/uploads
sudo chmod 755 /opt/blog/uploads

# 检查Nginx配置中的client_max_body_size
grep client_max_body_size /etc/nginx/sites-available/blog-proxy
```

### 问题3：登录失败

**原因**：Cookie/Session配置问题

**解决**：
```bash
# 检查application.properties中的Cookie配置
cat /opt/blog/application.properties | grep -i cookie

# 检查数据库中的用户表
mysql -u root -p blogdb -e "SELECT * FROM users LIMIT 5;"
```

### 问题4：静态资源404

**原因**：反向代理路径配置问题

**解决**：
```bash
# 检查Nginx日志
sudo tail -f /var/log/nginx/blog-proxy-error.log

# 测试本地资源访问
curl -I http://172.18.8.107:8012/css/site.css
```

---

## 回滚方案

### 如果新服务出现问题，快速回滚到旧服务

```bash
# 在本地服务器上
# 1. 停止新服务
sudo systemctl stop blog

# 2. 恢复旧项目（如果有备份）
BACKUP_DIR="/opt/blog-backup-YYYYMMDD-HHMMSS"
sudo cp -r $BACKUP_DIR/blog-old /opt/blog

# 3. 启动旧服务
sudo systemctl start blog

# 4. 验证
sudo systemctl status blog
curl -I http://localhost:8012/
```

---

## 常用命令速查

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

# 查看Nginx日志
sudo tail -f /var/log/nginx/blog-proxy-access.log
sudo tail -f /var/log/nginx/blog-proxy-error.log

# 重启Nginx
sudo systemctl restart nginx

# 测试Nginx配置
sudo nginx -t

# 查看MySQL数据库
mysql -u root -p -e "SHOW DATABASES;"

# 查看上传文件
ls -la /opt/blog/uploads/
```

---

## 最终检查清单

- [ ] 本地服务器上的旧服务已备份
- [ ] 新JAR包已上传到本地服务器
- [ ] systemd服务已创建并启动
- [ ] 本地8012端口已监听
- [ ] 本地curl测试通过
- [ ] 数据库数据已保留
- [ ] 上传文件已保留
- [ ] 公网服务器Nginx已配置
- [ ] 公网8012端口已开放
- [ ] 公网反向代理测试通过
- [ ] 外网访问http://101.35.79.76:8012正常
- [ ] 所有功能验证通过

---

**部署完成！** 🎉
