# ospf-kotlin-framework-plugin-persistence-sqlite

:us: English | :cn: [简体中文](README_ch.md)

SQLite datasource initialization plugin providing Ktorm `Database` management.

## Public API

| Symbol | Kind | Description |
| --- | --- | --- |
| `SqliteClientKey` | data class | Client lookup key (name) |
| `SqliteConfigBuilder` | data class | Fluent builder for `SqliteConfig` |
| `SqliteConfig` | data class | SQLite connection configuration (url, name, pool settings) |
| `Sqlite` | object | Client manager; indexes datasource instances by key, returns Ktorm `Database` |

## Quick Start

```kotlin
val database = Sqlite.init {
    url = "data/mydb.sqlite"
    name = "my-app"
}!!

// Use with KtormRepository
class OrderRepository(
    database: Database,
    table: Table<*>
) : KtormRepository<Order>(database, table, ...) {
    ...
}
```

## Connection Pool

Uses Apache Commons DBCP2 with configurable `maxTotal`, `maxIdle`, and `maxOpenPreparedStatements`. Additional connection properties can be set via `SqliteConfigBuilder.properties`.
