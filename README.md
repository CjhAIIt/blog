# 简单博客系统

这是一个基于Spring Boot构建的简单博客系统，支持用户注册、登录、文章发布与管理等功能。

## 功能特性

### 用户功能
- **用户注册与登录**：支持用户注册新账户，使用用户名/密码登录系统
- **用户验证**：包含用户名长度验证(3-20字符)、密码强度验证(至少6字符)、邮箱格式验证
- **重复检查**：自动检查用户名和邮箱是否已被使用

### 文章管理
- **文章发布**：已登录用户可以创建和发布新文章
- **文章编辑**：作者可以编辑自己发布的文章
- **文章查看**：所有用户可以查看文章列表和详细内容
- **文章删除**：作者可以删除自己发布的文章
- **分页显示**：文章列表支持分页浏览，每页显示5篇文章

### 搜索功能
- **关键词搜索**：支持根据关键词搜索文章标题和内容
- **搜索结果分页**：搜索结果同样支持分页显示

### 界面设计
- **响应式设计**：使用Bootstrap 5实现响应式布局，支持多设备访问
- **用户友好界面**：简洁直观的用户界面设计

## 技术栈

- **后端框架**: Spring Boot 3.2.0
- **数据访问**: Spring Data JPA, Hibernate
- **安全框架**: Spring Security
- **前端模板**: Thymeleaf
- **前端样式**: Bootstrap 5
- **数据库**: MySQL 8.0
- **构建工具**: Maven
- **Java版本**: Java 17

## 系统要求

- Java 17 或更高版本
- MySQL 8.0 或更高版本
- Maven 3.6 或更高版本

## 安装与运行

### 1. 安装Java

确保您的系统已安装Java 17或更高版本。可以通过以下命令检查：

```bash
java -version
```

### 2. 安装MySQL

确保您的系统已安装MySQL 8.0或更高版本。创建数据库：

```sql
CREATE DATABASE blogdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 安装Maven

确保您的系统已安装Maven 3.6或更高版本。可以通过以下命令检查：

```bash
mvn -version
```

### 4. 配置数据库连接

在`src/main/resources/application.properties`中配置数据库连接：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/blogdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&connectionCollation=utf8mb4_unicode_ci
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=123456
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
server.port=8081
```

### 5. 运行应用

在项目根目录下执行以下命令：

```bash
# 使用系统Maven
mvn clean package -DskipTests
java -jar target/blog-0.0.1-SNAPSHOT.jar

# 或者使用Maven Wrapper（如果Maven未安装在系统上）
# Windows
mvnw.cmd clean package -DskipTests
java -jar target/blog-0.0.1-SNAPSHOT.jar

# Linux/Mac
./mvnw clean package -DskipTests
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

### 6. 访问应用

应用启动后，可以通过浏览器访问以下地址：

- 主页: http://localhost:8081
- 登录页面: http://localhost:8081/login
- 注册页面: http://localhost:8081/register

## 项目结构

```
src/
├── main/
│   ├── java/com/example/blog/
│   │   ├── config/          # 配置类（安全配置、密码编码器等）
│   │   ├── controller/      # 控制器（处理HTTP请求）
│   │   ├── model/           # 数据模型（User, Post实体类）
│   │   ├── repository/      # 数据访问层（JPA仓库接口）
│   │   ├── service/         # 业务逻辑层
│   │   └── BlogApplication.java  # 应用程序入口
│   └── resources/
│       ├── templates/        # Thymeleaf模板
│       │   ├── posts/       # 文章相关页面
│       │   ├── layout.html  # 页面布局模板
│       │   ├── index.html   # 首页
│       │   ├── login.html   # 登录页面
│       │   ├── register.html # 注册页面
│       │   └── search.html  # 搜索结果页面
│       └── application.properties  # 应用配置
```

## 开发说明

### 添加新功能

1. 在`model`包中创建新的实体类
2. 在`repository`包中创建对应的数据访问接口
3. 在`service`包中实现业务逻辑
4. 在`controller`包中创建控制器
5. 在`templates`目录中创建前端页面

### 数据库配置

系统使用MySQL数据库，通过JPA自动创建和管理表结构。主要数据表：

- `users`：存储用户信息（用户名、密码、邮箱、创建时间）
- `posts`：存储文章信息（标题、内容、作者、创建时间）

### 安全配置

系统使用Spring Security进行安全控制：

- 密码使用BCrypt加密存储
- 基于表单的身份验证
- CSRF保护
- 会话管理

## 已知问题与解决方案

1. **Maven Wrapper路径问题**：如果遇到Maven Wrapper路径错误，可以直接使用系统安装的Maven
2. **字符编码问题**：MySQL连接URL已配置UTF-8编码以支持中文字符
3. **数据库连接问题**：确保MySQL服务已启动，并且数据库blogdb已创建

## 许可证

本项目仅用于学习和演示目的。