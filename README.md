# Blog1 博客系统

一个基于 Spring Boot 3 的博客与个人空间系统，面向实验室内部学习记录、项目沉淀和成员展示场景。当前版本默认使用 MySQL 8.0，并新增了独立排行榜、服务器草稿箱、封面/头像上传、Markdown 导入和十种字体选择。

## 核心功能

- 用户注册、登录、退出
- 用户可修改自己的用户名和密码
- Markdown 文章发布、编辑、删除、分类展示
- 草稿箱，多篇草稿持久保存
- Markdown 文件导入为博客草稿
- 文章封面上传与默认封面图
- 个人头像上传与默认头像图
- 十种展示字体选择，包含鸿蒙黑体
- 文章详情页目录、阅读信息、评论区
- 文章点赞与点赞榜
- 独立排行榜页面，支持周榜、月榜、年榜
- 个人空间资料维护
- 个人博客导出为静态包，便于发布到 GitHub Pages
- QQ / GitHub 等敏感字段加密存储
- 首页分类筛选与全文关键词搜索

## 排行榜说明

排行榜已经从首页拆分到独立页面 `/leaderboards`，支持 `week`、`month`、`year` 三个统计周期：

- 贡献排行榜：按当前周期内每位同学发布的博客数量排序
- 博客点赞排行榜：按当前周期内文章的点赞数排序

统计规则：

- 周榜从本周周一 00:00 开始统计
- 月榜从本月 1 日 00:00 开始统计
- 年榜从本年 1 月 1 日 00:00 开始统计

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Spring MVC
- Thymeleaf
- Spring Security
- Spring Data JPA
- CommonMark
- MySQL 8.0
- Maven Wrapper

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
`- templates
```

## 运行环境

项目默认读取以下环境变量；未设置时使用右侧默认值：

| 变量名 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8` | MySQL 8.0 连接地址 |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `com.mysql.cj.jdbc.Driver` | 数据库驱动 |
| `SPRING_DATASOURCE_USERNAME` | `root` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `root` | 数据库密码 |
| `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT` | `org.hibernate.dialect.MySQLDialect` | Hibernate MySQL 方言 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | 表结构自动更新 |
| `SPRING_JPA_SHOW_SQL` | `true` | 是否输出 SQL |
| `SERVER_PORT` | `8012` | 服务端口 |
| `APP_SECURITY_FIELD_ENCRYPTION_SECRET` | `blog-demo-encryption-key-change-me` | 敏感字段加密密钥，生产环境必须替换 |
| `APP_SITE_SOURCE_REPO_URL` | `https://github.com/CjhAIIt/blog` | 页面中的源码仓库地址 |
| `APP_STORAGE_UPLOAD_DIR` | `./uploads` | 上传文件目录，存放头像与封面 |

## 本地启动

### 1. 构建

```bash
# Windows
mvnw.cmd clean package -DskipTests

# Linux / macOS
./mvnw clean package -DskipTests
```

### 2. 启动

```bash
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

### 3. 默认访问地址

- 首页：`http://localhost:8012`
- 登录：`http://localhost:8012/login`
- 注册：`http://localhost:8012/register`
- 文章列表：`http://localhost:8012/posts`
- 个人空间：`http://localhost:8012/space`
- 导出页面：`http://localhost:8012/space/export`

### 4. 默认账号

当数据库为空时，系统会自动创建两组示例账号：

- `admin / password`
- `user / password`

## MySQL 8.0 配置

### Linux / macOS

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&connectionCollation=utf8mb4_unicode_ci'
export SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='your-password'
export SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT='org.hibernate.dialect.MySQLDialect'
```

### Windows PowerShell

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&connectionCollation=utf8mb4_unicode_ci'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD='your-password'
$env:SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT='org.hibernate.dialect.MySQLDialect'
```

启动前请先在 MySQL 中创建数据库：

```sql
CREATE DATABASE blogdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 实验室服务器部署建议

适用于服务器已安装 Java 17 和 MySQL 8.0，且希望把上传文件与应用包分离存放的情况。

### 目录建议

```text
/home/lab/apps/blog/
|- blog.jar
|- blog.env
`- uploads/
```

### 建议环境变量文件 `blog.env`

```bash
SERVER_PORT=8012
APP_SECURITY_FIELD_ENCRYPTION_SECRET=replace-with-your-own-secret
APP_SITE_SOURCE_REPO_URL=https://github.com/CjhAIIt/blog
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=replace-with-your-password
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.MySQLDialect
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
APP_STORAGE_UPLOAD_DIR=/home/lab/apps/blog/uploads
```

### systemd 用户服务示例

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

### 覆盖部署原则

- 只替换 `blog.jar`
- 保留 `uploads/` 目录
- 保留原有 `blog.env` 中的数据库与密钥配置
- 如果服务已经存在，优先执行重启而不是删除目录重建

## Git 工作流建议

```bash
git status
git add -A
git commit -m "feat: refresh blog docs and deployment"
git push origin main
```

## 当前项目说明

- 项目当前未包含自动化测试，构建使用 `-DskipTests`
- 上传的头像和封面默认存放在项目根目录 `uploads/`
- `target/`、日志文件、IDE 配置和数据库目录不应作为部署产物提交到仓库
- 生产环境务必替换 `APP_SECURITY_FIELD_ENCRYPTION_SECRET`
