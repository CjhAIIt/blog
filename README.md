# Blog1 博客系统

一个基于 Spring Boot 3、Thymeleaf、Spring Security 和 MySQL 的个人博客系统。

这个项目现在已经不只是“发文章”，而是一个完整的站内内容工作台，覆盖了创作、互动、个人空间、计划管理、通知提醒、榜单浏览和博客导出等能力，同时兼顾桌面端与移动端页面。

## 项目定位

适合下面这类场景：

- 实验室或小团队内部的学习记录与项目沉淀
- 个人技术博客、系列连载和专题整理
- 需要站内互动、通知提醒和个人主页展示的内容站点

## 当前能力

### 内容创作

- 文章发布、编辑、删除
- 草稿箱与草稿续写
- Markdown 实时预览
- Markdown 文件导入为草稿
- 封面图上传、默认封面、编辑器图片上传
- 十种文章字体切换
- 定时发布

### 阅读与互动

- 分类浏览、搜索、排行榜
- 点赞
- 评论与评论回复
- 消息中心与未读提醒

### 个人空间

- 个人主页展示
- 资料编辑
- 头像上传
- QQ / GitHub 等敏感字段加密存储
- 个人博客导出

### 计划系统

- 公开计划 / 共创计划 / 我的计划
- 文章归入计划
- 计划进度展示
- 计划状态管理

### 认证与后台

- 实名资料为选填项
- 填写实名后进入管理员审核
- 不实名认证也可正常写作、导入、编辑、加入计划
- 管理员可审核实名资料

### 前端体验

- 桌面端和移动端分别维护模板
- 自动按客户端切换移动视图
- 最近已补齐注册、登录、个人空间、资料页、计划页、排行榜、搜索页等信息层级和间距设计

## 技术栈

- Java 17
- Spring Boot 3.2
- Spring MVC
- Spring Data JPA
- Spring Security
- Thymeleaf
- MySQL
- CommonMark + Jsoup
- Maven Wrapper

## 快速启动

### 1. 准备环境

需要：

- JDK 17
- MySQL 8.x 或兼容的 MySQL / MariaDB 实例

先创建数据库：

```sql
CREATE DATABASE blogdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置环境变量

项目支持通过环境变量覆盖默认配置。开发环境至少建议覆盖下面这些值：

#### Windows PowerShell

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD='your-password'
$env:APP_SECURITY_FIELD_ENCRYPTION_SECRET='replace-with-your-own-secret'
$env:SERVER_PORT='8012'
```

#### Linux / macOS

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='your-password'
export APP_SECURITY_FIELD_ENCRYPTION_SECRET='replace-with-your-own-secret'
export SERVER_PORT='8012'
```

### 3. 运行测试

```bash
# Windows
.\mvnw.cmd -q test

# Linux / macOS
./mvnw -q test
```

### 4. 启动项目

开发模式：

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

打包运行：

```bash
# Windows
.\mvnw.cmd -DskipTests package

# Linux / macOS
./mvnw -DskipTests package
```

```bash
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

### 5. 访问地址

- 首页：`http://localhost:8012/`
- 登录：`http://localhost:8012/login`
- 注册：`http://localhost:8012/register`
- 文章列表：`http://localhost:8012/posts`
- 计划页：`http://localhost:8012/plans`
- 个人空间：`http://localhost:8012/space`
- 消息中心：`http://localhost:8012/notifications`

### 6. 示例数据

当数据库为空时，系统会自动初始化：

- 管理员账号：`admin / password`
- 示例用户：`user / password`
- 示例文章与评论

适合本地快速验收页面、权限和交互链路。

## 配置项

常用配置如下：

| 变量名 | 说明 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | MySQL 连接地址 |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | 数据库驱动，默认 `com.mysql.cj.jdbc.Driver` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate DDL 策略，默认 `update` |
| `SPRING_JPA_SHOW_SQL` | 是否打印 SQL，默认 `false` |
| `SERVER_PORT` | 应用端口，默认 `8012` |
| `APP_SCHEMA_COMPATIBILITY_ENABLED` | 是否启用启动时结构兼容修复，默认 `true` |
| `APP_SECURITY_FIELD_ENCRYPTION_SECRET` | 敏感字段加密密钥 |
| `APP_SITE_SOURCE_REPO_URL` | 页面中的源码仓库地址 |
| `APP_STORAGE_UPLOAD_DIR` | 上传文件目录，默认 `./uploads` |

说明：

- 代码里存在本地开发用的默认回退值，生产环境务必用环境变量覆盖。
- `APP_SECURITY_FIELD_ENCRYPTION_SECRET` 必须自行替换，不能沿用示例值。

## 数据库与结构说明

当前数据库由 JPA + 启动时兼容修复共同维护。

### 结构维护方式

- `spring.jpa.hibernate.ddl-auto=update`
- `app.schema.compatibility.enabled=true`

### 启动时会自动处理的兼容项

- `posts.content` 升级为 `LONGTEXT`
- `posts.status`、`posts.category` 兼容旧字段类型
- `users.role`、实名审核相关字段自动补齐
- `plans.status` 从旧整数状态兼容迁移到明确字符串状态
- 用户、文章、计划、评论等常用查询索引自动补齐

### 当前数据模型里的几个关键点

- 实名资料现在是选填，不再作为发文门槛
- 计划状态现在使用明确枚举值存储，而不是 `0 / 1 / 2`
- 评论、文章、计划等列表页已针对常用读取路径补索引

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
|- model
`- service
```

## 主要页面

- `/` 首页
- `/posts` 文章列表
- `/posts/{id}` 文章详情
- `/plans` 计划分区
- `/leaderboards` 排行榜
- `/search` 搜索
- `/space` 当前用户空间
- `/users/{username}` 用户主页
- `/notifications` 消息中心
- `/admin/verifications` 实名资料审核

## 部署建议

推荐把可变内容和程序本体分开：

```text
/home/blog/
|- blog.jar
|- blog.env
`- uploads/
```

推荐 `blog.env`：

```bash
SERVER_PORT=8012
APP_SECURITY_FIELD_ENCRYPTION_SECRET=replace-with-your-own-secret
APP_SITE_SOURCE_REPO_URL=https://github.com/CjhAIIt/blog
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/blogdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_DATASOURCE_USERNAME=blogapp
SPRING_DATASOURCE_PASSWORD=replace-with-your-password
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
APP_SCHEMA_COMPATIBILITY_ENABLED=true
APP_STORAGE_UPLOAD_DIR=/home/blog/uploads
```

用户级 `systemd` 服务示例：

```ini
[Unit]
Description=Blog1 Spring Boot Application
After=network.target

[Service]
Type=simple
WorkingDirectory=/home/blog
EnvironmentFile=/home/blog/blog.env
ExecStart=/usr/bin/java -jar /home/blog/blog.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=default.target
```

部署时建议遵守：

- 只替换 `jar`
- 保留 `uploads/`
- 保留环境变量文件
- 不直接删库或清空上传目录

## 开发说明

常用命令：

```bash
git status
git add -A
git commit -m "docs: restructure readme"
git push origin <branch>
```

如果你接下来还会继续迭代这个项目，建议优先补：

- 更完整的接口 / 页面说明
- 数据库 ER 图或表关系说明
- 截图或演示 GIF
- CI、Docker、Nginx 反向代理示例

## 仓库清理建议

这些内容不建议提交到仓库：

- `target/`
- `uploads/`
- 本地数据库或临时数据目录
- IDE 配置目录
- 临时 HTML / CSS 校验文件
- 部署过程中的中间产物
