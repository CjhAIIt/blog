# 搜索与检索策略 (Search Strategy)

当前系统使用 MySQL `FULLTEXT ngram` 索引支持文章内容的全文搜索。相较于低效的 `LIKE %keyword%`，`ngram` 能显著提升中英混合搜索场景的性能和相关性。

## 现状搜索能力
*   **后端实现**：使用 MySQL 的 `MATCH(title, content) AGAINST(:keyword IN NATURAL LANGUAGE MODE)`，不再对 `LONGTEXT` 字段进行全表扫描。
*   **索引建立**：在 Flyway 迁移脚本 `V2__compat_and_indexes.sql` 中创建：
    `CREATE FULLTEXT INDEX ft_posts_title_content ON posts(title, content) WITH PARSER ngram;`
*   **排序与分页**：支持基于 `相关性优先`、`最新` 或 `最热` 进行检索。如果无相关性结果，按时间进行兜底。
*   **业务逻辑**：在 `PostRepository.searchPublishedByFullText*` 中封装了相关性/时间/点赞的分页查询。
*   **UI 体验**：搜索页支持多维度过滤（按分类、排序），以及空状态学生文案引导（“没有找到与 XXX 相关的结果”）。

## 搜索后端路线对比与演进
系统目前的架构属于“路线 A（最小可见收益方案）”。若未来出现检索瓶颈或要求提升搜索体验（如即时补全、拼写容错），建议如下：

*   **路线 A (当前)**：MySQL FULLTEXT + ngram
    *   **优点**：无需引入额外服务，架构轻量，直接支持中文。
    *   **适用**：1–50 万篇文章规模的内网项目。
*   **路线 B (进阶)**：轻量搜索引擎（Meilisearch / Typesense）
    *   **优点**：支持 typo tolerance（拼写容错）、低延迟即时搜索。
    *   **架构变更**：需引入新的容器/服务，并通过事件机制（如发布文章时异步写入引擎）保证索引一致性。
*   **路线 C (重型)**：OpenSearch / Elastic
    *   **优点**：分布式、高可用。
    *   **适用**：数据规模大、存在集群或基础设施支持。

## 性能基线与索引重建
*   目前由于使用了 `ngram`，索引本身在文章新增/更新时自动由 MySQL 维护。如果因硬件变动等原因需要重建全文索引，可以使用 `OPTIMIZE TABLE posts;`（注意这可能会锁表，建议在低峰期进行）。
*   **性能目标**：1 万篇内容为 LONGTEXT 的文章下，单并发查询延迟预期在 `< 100ms`。
