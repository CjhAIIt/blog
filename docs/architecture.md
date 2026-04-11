# 系统架构文档 (Architecture)

## 系统边界与概述
本项目是一个面向实验室与内网学习记录场景的博客系统。它基于 Spring Boot 3、Spring Data JPA 与 Thymeleaf 构建，并通过 MySQL 作为核心存储。
系统采用服务端渲染（SSR）以适应内网或低带宽环境。系统内聚了文章创作、互动（点赞/评论/通知）、个人空间管理以及计划聚合等模块。

## 模块说明
1. **内容创作 (Posts & Markdown)**：核心业务模块，支持 Markdown 写作、草稿箱管理、实时预览及定时发布。上传的图片或封面统一存储到本地文件系统（`./uploads`）。
2. **计划与聚合 (Plans)**：供学生将一系列具有相关性的文章（如项目连载、课程笔记）聚合在一个“计划”下。
3. **互动与通知 (Comments, Likes & Notifications)**：包括对文章的点赞、多级评论，以及站内通知（新评论、点赞等）。
4. **用户与空间 (Users & Space)**：用户管理及实名认证（实验室/内网安全需要），用户可以进入“个人空间”管理自己的草稿、文章和资料。
5. **全文搜索 (Search)**：使用 MySQL `FULLTEXT ngram` 索引支持中英文搜索，基于相关性或时间进行排序。

## 关键业务流
- **发文流**：
  用户新建草稿 (或导入 Markdown) -> 实时编辑与预览 -> 上传图片/封面 (落盘) -> 选择发布状态 (发布/定时) -> 落库 (`posts` 表)。
- **评论互动流**：
  用户阅读文章 -> 发起评论/回复 -> 系统写入 `comments` 表 -> 触发事件生成 `notifications` 通知给被评论者。
- **搜索流**：
  用户在前端输入关键词 -> `/search` 接口 -> `PostService` -> `PostRepository.searchPublishedByFullText*` (使用 `MATCH AGAINST` 语句) -> 渲染搜索结果页。
- **导出流**：
  用户请求导出 -> `PersonalBlogExportService` 聚合用户的全部发布文章及依赖静态资源 -> 打包成离线 HTML + 资源 -> 提供 ZIP 下载。
