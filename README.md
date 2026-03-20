# Blog1 博客系统

基于 Spring Boot 3 的个人博客项目，支持注册登录、邮箱验证码验证、Markdown 写作、评论互动、个人空间和文章分类展示。项目可以直接以可执行 JAR 方式部署；默认使用文件型 H2 数据库，若已有 MySQL 也可以通过环境变量切换。

## 功能概览

- 用户注册、登录、注销
- 邮箱验证码验证，支持模拟发送和真实 SMTP
- 文章创建、编辑、删除、详情查看
- Markdown 编辑与预览
- 评论发布与评论数统计
- 个人空间展示与资料编辑
- 分类首页与关键字搜索
- QQ / GitHub 链接字段加密存储
- 空库自动初始化默认用户和示例内容

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Spring MVC + Thymeleaf
- Spring Security
- Spring Data JPA
- CommonMark
- H2 / MySQL
- Maven

## 运行要求

- JDK 17 或更高版本
- Maven 3.9+，或直接使用仓库内的 `mvnw.cmd`

## 默认环境变量

项目已改为优先读取环境变量，未配置时使用下列默认值：

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
| `APP_VERIFICATION_CODE_EXPIRE_MINUTES` | `10` | 验证码过期时间 |
| `APP_MAIL_MOCK_MODE` | `true` | 是否仅打印验证码日志 |
| `APP_MAIL_FROM` | `no-reply@blog.local` | 邮件发件人 |
| `APP_SECURITY_FIELD_ENCRYPTION_SECRET` | `blog-demo-encryption-key-change-me` | 敏感字段加密密钥，生产环境必须修改 |
| `SPRING_MAIL_HOST` | 空 | SMTP 主机 |
| `SPRING_MAIL_PORT` | `587` | SMTP 端口 |
| `SPRING_MAIL_USERNAME` | 空 | SMTP 用户名 |
| `SPRING_MAIL_PASSWORD` | 空 | SMTP 密码 |

## 本地启动

### 1. 构建项目

```bash
mvn clean package -DskipTests
```

如果本机未安装 Maven，可以使用：

```bash
# Windows
mvnw.cmd clean package -DskipTests

# Linux / macOS
./mvnw clean package -DskipTests
```

### 2. 启动应用

```bash
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

启动后默认访问地址：

- 首页：`http://localhost:8012`
- 登录：`http://localhost:8012/login`
- 注册：`http://localhost:8012/register`
- 个人空间：`http://localhost:8012/space`

### 3. 默认初始化账号

当数据库为空时，系统会自动创建两组示例账号：

- `admin / password`
- `user / password`

## 切换到 MySQL

如果需要继续使用 MySQL，启动前设置环境变量即可：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&connectionCollation=utf8mb4_unicode_ci'
export SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='your-password'
export SPRING_JPA_DATABASE_PLATFORM='org.hibernate.dialect.MySQLDialect'
```

Windows PowerShell 示例：

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&connectionCollation=utf8mb4_unicode_ci'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD='your-password'
$env:SPRING_JPA_DATABASE_PLATFORM='org.hibernate.dialect.MySQLDialect'
```

## Linux 服务器部署

下面的流程适用于仅安装了 Java 17 的 Linux 服务器：

### 1. 上传构建产物

```bash
mkdir -p ~/apps/blog
scp target/blog-0.0.1-SNAPSHOT.jar lab@server-ip:~/apps/blog/blog.jar
```

### 2. 准备环境变量文件

在服务器创建 `~/apps/blog/blog.env`：

```bash
SERVER_PORT=8012
APP_MAIL_MOCK_MODE=true
APP_SECURITY_FIELD_ENCRYPTION_SECRET=replace-with-your-own-secret
SPRING_DATASOURCE_URL=jdbc:h2:file:/home/lab/apps/blog/data/blogdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
```

### 3. 后台启动

```bash
cd ~/apps/blog
nohup bash -c 'set -a && source ./blog.env && set +a && java -jar ./blog.jar' > blog.out.log 2>&1 &
```

### 4. 查看日志

```bash
tail -f ~/apps/blog/blog.out.log
```

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

## 说明

- 该项目当前未包含自动化测试，构建时默认使用 `-DskipTests`
- 开启真实邮件发送时，需要将 `APP_MAIL_MOCK_MODE` 设为 `false` 并补齐 SMTP 参数
- 生产环境请务必修改 `APP_SECURITY_FIELD_ENCRYPTION_SECRET`
