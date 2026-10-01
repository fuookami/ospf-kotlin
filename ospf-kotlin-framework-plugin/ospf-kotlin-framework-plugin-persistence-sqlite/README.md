# ospf-kotlin-framework-plugin-persistence-sqlite

:us: English | :cn: [简体中文](README_ch.md)

SQLite persistence plugin with a shared DBCP datasource for Spring JdbcClient, Ktorm, and MyBatis-Plus.

## Public API

| Symbol | Kind | Description |
| --- | --- | --- |
| `SqliteClientKey` | data class | Client lookup key (name) |
| `SqliteConfigBuilder` / `SqliteConfig` | configuration | File path, client name, connection properties, and pool settings |
| `Sqlite.initJdbcClient` | factory | Returns `Ret<JdbcClientBackend>` |
| `Sqlite.initKtorm` | factory | Returns `Ret<KtormBackend>` with the SQLite query dialect |
| `Sqlite.initMybatis` | factory | Returns `Ret<MybatisBackend>` configured for SQLite |
| `Sqlite.get*` | lookup | Gets a backend by config, key, or name |
| `Sqlite.close` | lifecycle | Closes and removes the shared datasource; returns `Try` |

## Quick Start

`url` is a database file path, not a JDBC URL. The three adapter factories return `Ret`; handle `Ok`, `Failed`, and `Fatal` before using the backend.

```kotlin
val backend = Sqlite.initJdbcClient {
    url = "data/orders.sqlite"
    name = "orders"
}

if (backend.ok) {
    val client = backend.value!!.client
}
```

`Sqlite.initKtorm { ... }` returns a `KtormBackend` containing both `Database` and the SQLite expression dialect. `Sqlite.initMybatis { ... }` returns a MyBatis-Plus backend whose sessions own their transactions, while the plugin retains ownership of the pool.

The legacy `Sqlite.init` and `Sqlite(config)` / `Sqlite(key)` / `Sqlite(name)` Ktorm entry points continue to return nullable `Database` values. New factories use structured errors instead of the legacy nullable result.

## Connection Pool

All adapter backends for the same client name reuse one Apache Commons DBCP2 datasource. Configure `maxTotal`, `maxIdle`, `maxOpenPreparedStatements`, and JDBC connection properties through `SqliteConfigBuilder`. Call `Sqlite.close(SqliteClientKey("orders"))` when the plugin-managed pool is no longer needed; repeated closes succeed. Closing it also affects existing Ktorm, JdbcClient, and MyBatis backends for that key.
