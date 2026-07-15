# ospf-kotlin-framework-plugin-persistence-sqlite

:us: [English](README.md) | :cn: 简体中文

SQLite 数据源初始化插件，提供 Ktorm `Database` 管理。

## 公开 API

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `SqliteClientKey` | data class | 客户端查找键（名称） |
| `SqliteConfigBuilder` | data class | `SqliteConfig` 的流式构建器 |
| `SqliteConfig` | data class | SQLite 连接配置（URL、名称、连接池设置） |
| `Sqlite` | object | 客户端管理器；按键索引数据源实例，返回 Ktorm `Database` |

## 快速开始

```kotlin
val database = Sqlite.init {
    url = "data/mydb.sqlite"
    name = "my-app"
}!!

// 配合 KtormRepository 使用
class OrderRepository(
    database: Database,
    table: Table<*>
) : KtormRepository<Order>(database, table, ...) {
    ...
}
```

## 连接池

使用 Apache Commons DBCP2，可配置 `maxTotal`、`maxIdle` 和 `maxOpenPreparedStatements`。额外连接属性可通过 `SqliteConfigBuilder.properties` 设置。
