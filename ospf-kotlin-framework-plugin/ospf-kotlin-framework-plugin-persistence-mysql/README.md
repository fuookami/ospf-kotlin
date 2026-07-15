# ospf-kotlin-framework-plugin-persistence-mysql

:us: English | :cn: [简体中文](README_ch.md)

MySQL datasource initialization plugin providing Ktorm `Database` management.

## Public API

| Symbol | Kind | Description |
| --- | --- | --- |
| `MySQLClientKey` | data class | Client lookup key (name + database) |
| `MySQLConfigBuilder` | data class | Fluent builder for `MySQLConfig` |
| `MySQLConfig` | data class | MySQL connection configuration (url, name, database, credentials, pool settings) |
| `MySQL` | object | Client manager; indexes datasource instances by key, returns Ktorm `Database` |

## Quick Start

```kotlin
val database = MySQL.init {
    url = "localhost:3306"
    name = "my-app"
    database = "production"
    userName = "root"
    password = "secret"
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

Uses Apache Commons DBCP2 with configurable `maxTotal`, `maxIdle`, and `maxOpenPreparedStatements`. Additional connection properties can be set via `MySQLConfigBuilder.properties`.
