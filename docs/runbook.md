# 运行与部署指南 (Runbook)

本系统适合部署在内网单机或小规模集群，基于 Java 17 + MySQL 8.0。

## 部署清单与最小配置
为了使应用正常运行并在内网稳定提供服务，以下是推荐的最小部署配置清单：
*   **数据库**：MySQL 8.0+，新建数据库 `blogdb`，编码推荐 `utf8mb4`。
*   **运行介质**：打包为可执行 Jar（`java -jar`）或通过 `systemd` 托管。

### 关键环境变量
| 环境变量名 | 描述 | 默认值 | 生产建议 |
| :--- | :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | MySQL 连接串 | `jdbc:mysql://localhost:3306/blogdb?...` | 必须配置 |
| `SPRING_DATASOURCE_USERNAME` | 数据库账号 | `root` | 必须配置 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | (空) | 必须配置 |
| `SERVER_PORT` | 服务端口 | `8012` | 按需配置 |
| `APP_SECURITY_FIELD_ENCRYPTION_SECRET` | 敏感字段加密密钥 | `blog-demo-...` | **必须在生产更换并永久妥善保管** |
| `APP_STORAGE_UPLOAD_DIR` | 上传目录 | `./uploads` | 设置绝对路径，防止路径飘移 |

## 启动方式
推荐使用 Systemd 等进程管理工具管理进程：
1. `mvnw clean package -DskipTests`
2. `java -jar target/blog-0.0.1-SNAPSHOT.jar`

## 覆盖发布
如果服务器上已经有同一套博客服务，并且要求保留数据库、`uploads/` 和环境变量文件，可以直接使用仓库里的覆盖发布脚本：

```powershell
.\scripts\Deploy-BlogPreserveData.ps1 `
  -Username lab `
  -KeyFile C:\path\to\id_ed25519 `
  -RemoteDir /home/lab/apps/blog `
  -Build
```

密码登录也可以：

```powershell
.\scripts\Deploy-BlogPreserveData.ps1 `
  -Username lab `
  -Password 'your-password' `
  -RemoteDir /home/lab/apps/blog
```

脚本行为：
*   只上传新的 `blog.jar` 到远端 `releases/`
*   仅替换运行中的 `blog.jar`
*   保留 `blog.env`
*   保留 `uploads/`
*   不删除数据库数据
*   启动失败时自动回滚到上一个 `jar`

## 迁移与回滚步骤 (Flyway)
1. 系统在启动时自动执行 `classpath:db/migration` 下的 Flyway 脚本。
2. 确保 `spring.jpa.hibernate.ddl-auto=validate`，防止 JPA 自动改表破坏 Flyway 脚本。
3. **新增表结构**：请勿直接修改 `V1__init_schema.sql` 或 `V2__compat_and_indexes.sql`，而是创建新的 `V3__xxx.sql`。

## 备份与恢复
*   **数据库**：通过 `mysqldump` 定期备份整个 `blogdb`，尤其注意包含加密的用户隐私数据。
*   **文件上传目录**：备份由 `APP_STORAGE_UPLOAD_DIR` 指定的目录（包括 `avatars`、`covers` 等）。
*   **迁移**：如果在不同服务器间迁移，务必保持相同的 `APP_SECURITY_FIELD_ENCRYPTION_SECRET`，否则用户关联的 QQ/GitHub 字段将无法解密导致系统错误。

## Nginx 反向代理示例
项目根目录提供了 `blog-public-8012.conf.tmp` 作为参考，主要配置反代到本地 18012 / 8012 端口，以及对 `/uploads/` 目录设置访问策略。
对于需要外网穿透的内网项目，可参考根目录下的 `blog-reverse-tunnel.service.tmp` 设置 SSH 反向隧道。
