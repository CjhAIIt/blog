# Blog1 博客系统

一个基于 Spring Boot 3、Thymeleaf 和 MySQL 的个人博客系统，适合实验室内部的学习记录、项目沉淀和成员展示场景。

当前版本已经包含：

- 博客发布、编辑、删除、草稿箱
- Markdown 导入、实时预览、十种字体选择
- 点赞、评论、评论回复
- 站内消息中心与未读通知铃铛
- 个人空间、真实姓名展示、资料编辑
- 封面图与头像上传
- 独立排行榜页、搜索页、个人博客导出

## 最近更新

- 优化前端界面，提升用户体验
- 改进介绍内容，使其更自然友好
- MD编译器新增侧边栏收起功能，便于专注编写

## 目录结构

```text
src/main/java/com/example/blog
|- config
|- controller
|- dto
|- model
|- repository
`- service

src/main/resources
|- static
|- templates
`- application.properties

src/test/java/com/example/blog
|- config
`- service
```

## 功能概览

### 内容创作

- 发布、编辑、删除文章
- 草稿持久化保存
- Markdown 文件导入为草稿
- 封面图上传与默认封面
- 十种文章字体选择

### 阅读与互动

- 文章点赞
- 评论与评论回复
- 站内通知：有人评论你的博客时提醒
- 站内通知：有人回复你的评论时提醒
- 搜索与分类浏览

### 个人空间

- 个人主页展示
- 资料编辑
- 真实姓名选填，仅中文，最多 5 个字
- 头像上传
- 草稿管理
- 个人博客导出

### 排行榜

- 贡献排行榜
- 点赞排行榜
- 支持周榜、月榜、年榜

## 快速启动

### 1. 准备数据库

先创建数据库：

```sql
CREATE DATABASE blogdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 运行测试

```bash
# Windows
mvnw.cmd test

# Linux / macOS
./mvnw test
```

### 3. 打包

```bash
# Windows
mvnw.cmd package -DskipTests

# Linux / macOS
./mvnw package -DskipTests
```

### 4. 启动

```bash
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

默认访问地址：

- 首页：`http://localhost:8012`
- 登录：`http://localhost:8012/login`
- 注册：`http://localhost:8012/register`
- 文章列表：`http://localhost:8012/posts`
- 个人空间：`http://localhost:8012/space`
- 消息中心：`http://localhost:8012/notifications`

### 5. 默认账号

当数据库为空时，系统会自动创建两组示例账号：

- `admin / password`
- `user / password`

## 配置说明

项目通过环境变量覆盖默认配置：

| 变量名 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8` | MySQL 连接地址 |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `com.mysql.cj.jdbc.Driver` | 数据库驱动 |
| `SPRING_DATASOURCE_USERNAME` | `root` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `root` | 数据库密码 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | 表结构自动更新策略 |
| `SPRING_JPA_SHOW_SQL` | `true` | 是否输出 SQL |
| `SERVER_PORT` | `8012` | 应用端口 |
| `APP_SECURITY_FIELD_ENCRYPTION_SECRET` | `blog-demo-encryption-key-change-me` | 敏感字段加密密钥 |
| `APP_SITE_SOURCE_REPO_URL` | `https://github.com/CjhAIIt/blog` | 页面中的源码仓库地址 |
| `APP_STORAGE_UPLOAD_DIR` | `./uploads` | 上传文件目录 |

### Windows PowerShell 示例

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD='your-password'
$env:SERVER_PORT='8012'
$env:APP_SECURITY_FIELD_ENCRYPTION_SECRET='replace-with-your-own-secret'
```

### Linux / macOS 示例

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci'
export SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='your-password'
export SERVER_PORT='8012'
export APP_SECURITY_FIELD_ENCRYPTION_SECRET='replace-with-your-own-secret'
```

## Linux 服务器部署

推荐目录结构：

```text
/home/lab/apps/blog/
|- blog.jar
|- blog.env
`- uploads/
```

推荐 `blog.env`：

```bash
SERVER_PORT=8012
APP_SECURITY_FIELD_ENCRYPTION_SECRET=replace-with-your-own-secret
APP_SITE_SOURCE_REPO_URL=https://github.com/CjhAIIt/blog
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_DATASOURCE_USERNAME=blogapp
SPRING_DATASOURCE_PASSWORD=replace-with-your-password
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
APP_STORAGE_UPLOAD_DIR=/home/lab/apps/blog/uploads
```

用户级 `systemd` 服务示例：

```ini
[Unit]
Description=Blog1 Spring Boot Application
After=network.target

[Service]
Type=simple
WorkingDirectory=/home/lab/apps/blog
EnvironmentFile=/home/lab/apps/blog/blog.env
ExecStart=/usr/bin/java -jar /home/lab/apps/blog/blog.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=default.target
```

覆盖部署原则：

- 只替换 `blog.jar`
- 保留 `blog.env`
- 保留 `uploads/`
- 不删除数据库数据

## 开发与发布

常用 Git 命令：

```bash
git status
git add -A
git commit -m "docs: rewrite readme and clean repo"
git push origin <branch>
```

## 仓库清理说明

这些内容不应提交到仓库：

- `target/`
- `data/`
- `uploads/`
- IDE 配置目录
- 临时部署工具目录
- 日志与构建产物
