# 测试策略与覆盖范围 (Testing)

为保障内网学生项目的稳定性，尤其是升级 Flyway 迁移与 FULLTEXT ngram 搜索后，测试重点放在了逻辑正确性与回归能力。

## 测试分层

### 1. 单元测试 (Unit Tests)
主要针对纯业务逻辑，如用户服务、Markdown 解析与清洗。
*   `UserServiceTest`:
    *   验证 `UserRole` 权限判断。
    *   验证 `RealNameVerificationStatus` 审核状态对应发文权限拦截。
    *   覆盖异常抛出与提示文案（如 `getPostPermissionMessage` 返回正确的学生向文案）。
*   `MarkdownServiceHtmlSanitizationTest`:
    *   验证恶意 HTML、`<script>`、`javascript:` 等注入被 Jsoup 清洗拦截。
    *   验证正常 Markdown（加粗、链接、图片）能够顺利渲染并预览。
*   `MarkdownImportCompatibilityTest`:
    *   测试 H1 标题提取逻辑，确保导入 Markdown 文件时生成的标题符合预期。

### 2. 集成测试 (Integration / Data Layer Tests)
*   `SchemaCompatibilityInitializerTest`:
    *   目前虽然项目已经迁移到了 Flyway 版本化管理（`db/migration/V1` 与 `V2`），但保留对部分兼容性初始化的验证，以确保 `spring.jpa.hibernate.ddl-auto=validate` 模式下应用正常启动。

### 3. Web 层 / E2E 冒烟测试 (Manual / E2E Baseline)
项目每次发布前（如更新 `search.html` 或修改后端 `MATCH AGAINST` 查询时），建议执行以下 Checklist：
1. **登录与审核流**：注册新账号 -> 状态变为审核中 -> 管理员通过 `admin/password` 登录后台 -> 审核通过 -> 普通用户能正常发表文章。
2. **发布与编辑**：测试编辑器实时预览 -> 上传封面图片 -> 选择分类/计划 -> 发布成功并跳转到文章详情页。
3. **搜索与检索**：测试中英混合词在 `search.html` 搜索框下的表现 -> 确保 `FULLTEXT ngram` 生效（且结果相关性排序合理）-> 验证空状态下有文案提示。
4. **评论互动**：对发布的文章发表评论 -> 回复评论 -> 检查 `notifications` 生成且未读计数 +1。
5. **排行榜与计划**：排行榜能正确汇总周榜/月榜/总榜的点赞与发布数，计划内文章的排序符合 `plan_order` 逻辑。

## 持续集成建议
目前仓库未提供 `.github/workflows`，推荐将上述 `mvnw test` 集成到 GitHub Actions 或内网 GitLab CI，在 PR 合并到 main 前自动执行。
