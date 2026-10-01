# ospf-kotlin-framework-plugin-persistence-sqlite

:us: [English](README.md) | :cn: 简体中文

SQLite 持久化插件，为 Spring JdbcClient、Ktorm 和 MyBatis-Plus 共享 DBCP 数据源。

## 公开 API

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `SqliteClientKey` | data class | 客户端查找键（名称） |
| `SqliteConfigBuilder` / `SqliteConfig` | 配置 | 文件路径、客户端名称、连接属性和连接池设置 |
| `Sqlite.initJdbcClient` | 工厂 | 返回 `Ret<JdbcClientBackend>` |
| `Sqlite.initKtorm` | 工厂 | 返回包含 SQLite 查询方言的 `Ret<KtormBackend>` |
| `Sqlite.initMybatis` | 工厂 | 返回使用 SQLite 方言的 `Ret<MybatisBackend>` |
| `Sqlite.get*` | 查找 | 按配置、键或名称获取后端 |
| `Sqlite.close` | 生命周期 | 关闭并移除共享数据源，返回 `Try` |

## 快速开始

`url` 是数据库文件路径，不是 JDBC URL。三种适配器工厂都返回 `Ret`；使用后端前应处理 `Ok`、`Failed` 和 `Fatal`。

```kotlin
val backend = Sqlite.initJdbcClient {
    url = "data/orders.sqlite"
    name = "orders"
}

if (backend.ok) {
    val client = backend.value!!.client
}
```

`Sqlite.initKtorm { ... }` 返回同时包含 `Database` 和 SQLite 表达式方言的 `KtormBackend`。`Sqlite.initMybatis { ... }` 返回 MyBatis-Plus 后端；会话管理事务，连接池仍由插件管理。

旧的 `Sqlite.init` 和 `Sqlite(config)` / `Sqlite(key)` / `Sqlite(name)` Ktorm 入口继续返回可空 `Database`。新增工厂通过结构化错误返回失败，不使用旧的可空结果。

## 连接池

同一客户端名称下的所有适配器后端共用一个 Apache Commons DBCP2 数据源。可通过 `SqliteConfigBuilder` 配置 `maxTotal`、`maxIdle`、`maxOpenPreparedStatements` 和 JDBC 连接属性。不再使用插件管理的连接池时，调用 `Sqlite.close(SqliteClientKey("orders"))`；重复关闭会成功。关闭后，该键下已有的 Ktorm、JdbcClient 和 MyBatis 后端也无法继续使用连接池。
