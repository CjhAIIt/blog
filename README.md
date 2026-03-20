# Blog1 博客系统

一个基于 Spring Boot 3 的个人写作站点，支持 Markdown 创作、分类归档、评论互动和个人主页展示。当前版本默认使用文件型 H2 数据库，本地开箱即用；如果已有 MySQL，也可以通过环境变量切换。

## 项目特点

- 注册后可直接登录，不再依赖邮箱验证码
- Markdown 写作台支持双栏编辑、实时预览、专注模式
- 编辑页支持本地草稿、字数统计、阅读时长和文章大纲
- 文章详情页支持目录导航、阅读信息和评论区
- 个人主页支持资料展示、创作列表和 GitHub 链接
- QQ / GitHub 链接字段加密存储
- 首页支持分类浏览和关键词搜索
- 空库自动初始化默认账号和示例内容

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Spring MVC
- Thymeleaf
- Spring Security
- Spring Data JPA
- CommonMark
- H2 / MySQL
- Maven

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

## 默认运行方式

项目默认读取以下环境变量；如果不设置，会使用右侧默认值：

| 变量名 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/blogdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE` | 默认文件型 H2 数据库 |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.h2.Driver` | 数据库驱动 |
| `SPRING_DATASOURCE_USERNAME` | `sa` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 空 | 数据库密码 |
| `SPRING_JPA_DATABASE_PLATFORM` | `org.hibernate.dialect.H2Dialect` | JPA 方言 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | 表结构策略 |
| `SPRING_JPA_SHOW_SQL` | `true` | SQL 日志 |
| `SERVER_PORT` | `8012` | Web 端口 |
| `APP_SECURITY_FIELD_ENCRYPTION_SECRET` | `blog-demo-encryption-key-change-me` | 敏感字段加密密钥，生产环境必须修改 |

## 本地启动

### 1. 构建

```bash
mvn clean package -DskipTests
```

如果本机没有 Maven，也可以使用：

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

默认访问地址：

- 首页：`http://localhost:8012`
- 登录：`http://localhost:8012/login`
- 注册：`http://localhost:8012/register`
- 文章列表：`http://localhost:8012/posts`

### 3. 默认账号

当数据库为空时，会自动创建两组示例账号：

- `admin / password`
- `user / password`

## 切换到 MySQL

Linux / macOS：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&connectionCollation=utf8mb4_unicode_ci'
export SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='your-password'
export SPRING_JPA_DATABASE_PLATFORM='org.hibernate.dialect.MySQLDialect'
```

Windows PowerShell：

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&connectionCollation=utf8mb4_unicode_ci'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD='your-password'
$env:SPRING_JPA_DATABASE_PLATFORM='org.hibernate.dialect.MySQLDialect'
```

## Linux 部署示例

适用于只安装了 Java 17 的服务器。

### 1. 上传 JAR

```bash
mkdir -p ~/apps/blog/data
scp target/blog-0.0.1-SNAPSHOT.jar lab@server-ip:~/apps/blog/blog.jar
```

### 2. 准备环境变量

创建 `~/apps/blog/blog.env`：

```bash
SERVER_PORT=8012
APP_SECURITY_FIELD_ENCRYPTION_SECRET=replace-with-your-own-secret
SPRING_DATASOURCE_URL=jdbc:h2:file:/home/lab/apps/blog/data/blogdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
```

### 3. 使用 `systemd --user` 托管

创建 `~/.config/systemd/user/blog.service`：

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

启动并设置自启：

```bash
systemctl --user daemon-reload
systemctl --user enable --now blog
```

查看状态与日志：

```bash
systemctl --user status blog
journalctl --user -u blog -f
```

## 当前写作体验

- 编辑器支持双栏预览、专注写作、专注预览三种模式
- 支持快捷插入标题、引用、代码块、表格、分隔线
- 页面会实时显示字数、段落数、阅读时长和标题层级
- 浏览器本地自动保存草稿，避免刷新后内容丢失
- 文章详情页会自动生成目录，方便长文跳转

## 说明

- 当前项目未包含自动化测试，构建使用 `-DskipTests`
- 默认使用 H2 文件数据库，运行后会在项目目录下生成 `data/`
- 生产环境请务必修改 `APP_SECURITY_FIELD_ENCRYPTION_SECRET`
