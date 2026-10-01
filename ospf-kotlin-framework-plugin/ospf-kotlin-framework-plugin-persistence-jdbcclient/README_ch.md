# ospf-kotlin-framework-plugin-persistence-jdbcclient

[English](README.md) | [简体中文](README_ch.md)

基于 Spring `JdbcClient` 的表达式持久化适配。核心模块依赖 `spring-jdbc`，不选择或捆绑 JDBC 驱动。数据库插件实现 `JdbcClientDialect`，并可从其管理的连接池创建 `JdbcClientBackend`。

## 公开 API

| API | 说明 |
| --- | --- |
| `JdbcClientBackend` | 将 Spring `JdbcClient` 与数据库方言配对 |
| `JdbcClientDialect` | 提供安全标识符引用、分页和空值排序 SQL 片段 |
| `JdbcClientRepository<E>` | 使用显式 `RowMapper<E>` 为单表实现 `ExpressionRepository<E>` |
| `JdbcClientColumnBinder` | 将表达式路径映射为物理列名 |
| `JdbcClientValueConverter` | 将 OSPF 值转换为 JDBC 兼容值 |
| `JdbcClientBooleanTranslator` | 将布尔谓词翻译为参数化 SQL |
| `JdbcClientScalarTranslator` | 将标量表达式翻译为参数化 SQL |
| `JdbcClientOrderByTranslator` | 通过所选方言翻译排序规则 |
| `JdbcClientUpdateTranslator` | 执行前完整验证并翻译所有更新赋值 |

## 快速开始

```kotlin
val client = JdbcClient.create(dataSource)
val backend = JdbcClientBackend(client, applicationDialect)

class UserRepository : JdbcClientRepository<User>(
    backend = backend,
    tableName = "app_user",
    rowMapper = RowMapper { row, _ -> User(row.getLong("id"), row.getString("name")) },
    resolveColumnName = jdbcClientResolver(mapOf("name" to "user_name"))
)

val users: Ret<List<User>> = repository.find(UserSchema.predicate { name like "%Ada%" })
```

仓储操作统一返回 `Ret`，默认不支持谓词策略为 `FailFast`。调用方可显式选择 `AlwaysFalse`，此时整棵谓词会收敛为 `1 = 0`。由于适配器不会为客户端过滤而读取整张表，`ClientFilter` 会返回结构化失败。JDBC 客户端和行映射器抛出的异常会转换为失败结果。

所有值都通过 SQL 中的 `?` 按出现顺序绑定；更新参数顺序为 `SET` 参数在前、`WHERE` 参数在后。表名和列名先经过解析，再由方言逐段引用。应用需显式提供实体 `RowMapper`；本适配器不推断联表、插入或实体映射策略。

仓储不负责外部传入 `JdbcClient` 的生命周期。MySQL、PostgreSQL、H2 或其他 JDBC 数据库插件可以提供托管 Backend 工厂及方言实现。
