# ospf-kotlin-framework-plugin-persistence-mysql

:us: English | :cn: [简体中文](README_ch.md)

MySQL persistence plugin providing managed Ktorm, MyBatis-Plus, and Spring JdbcClient adapters.

## Public API

| Symbol | Kind | Description |
| --- | --- | --- |
| `MySQLClientKey` | data class | Client lookup key (name + database) |
| `MySQLConfigBuilder` | data class | Fluent builder for `MySQLConfig` |
| `MySQLConfig` | data class | MySQL connection configuration (url, name, database, credentials, pool settings) |
| `MySQL` | object | Client manager; indexes a shared DBCP datasource by key and exposes all three adapter entry points |
| `MySQLJdbcClientDialect` | object | JdbcClient SQL dialect for identifier quoting, pagination, and null ordering |
| `MySQL.initKtorm` / `getKtormBackend` | factory | Returns `Ret<KtormBackend>` with the MySQL expression dialect |
| `MySQL.initMybatis` / `getMybatisBackend` | factory | Returns `Ret<MybatisBackend>` with the MySQL MyBatis dialect |
| `MySQL.initJdbcClient` / `getJdbcClient` | factory | Returns `Ret<JdbcClientBackend>` with the MySQL JdbcClient dialect |

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

The JdbcClient API returns `Ret` and uses the same DBCP datasource cache as Ktorm:

```kotlin
val backend = MySQL.initJdbcClient {
    url = "localhost:3306"
    name = "my-app"
    database = "production"
    userName = "root"
    password = "secret"
}

if (backend.ok) {
    val jdbcClient = backend.value!!.client
}
```

For new code, `MySQL.initKtorm { ... }` returns `Ret<KtormBackend>` with the matching expression dialect, and `MySQL.initMybatis { ... }` returns `Ret<MybatisBackend>`. The legacy `MySQL.init` and nullable Ktorm lookup overloads remain available for compatibility. All three new adapter factories and their key/name lookups return structured `Ret` results.

The factory creates a lazy JdbcClient and does not test database connectivity; connection errors are returned by repository operations. Use `MySQL.getJdbcClient(MySQLClientKey("my-app", "production"))` for exact-key lookup. A missing key returns a failed `Ret`. All three adapters share the same DBCP datasource for a key. Close the managed pool with `MySQL.close(key)` when the application is done with it; repeated close calls succeed, and closing it also affects existing Ktorm, MyBatis, and JdbcClient backends for that key.

## Connection Pool

Uses Apache Commons DBCP2 with configurable `maxTotal`, `maxIdle`, and `maxOpenPreparedStatements`. Additional connection properties can be set via `MySQLConfigBuilder.properties`. Configuration failures from the JdbcClient entry points are structured errors and do not include credentials.

## Live Integration Test

The three live tests (`MySQLMybatisIntegrationTest`, `MySQLKtormLiveIntegrationTest`, and `MySQLJdbcClientLiveIntegrationTest`) are skipped unless `OSPF_TEST_MYSQL_HOST` is set. Set `OSPF_TEST_MYSQL_USER` and `OSPF_TEST_MYSQL_PASSWORD`; `OSPF_TEST_MYSQL_PORT` is optional and defaults to `3306`. The account needs permission to create and drop databases and to create tables and perform CRUD in the temporary database. The MyBatis and JdbcClient tests verify MySQL 8.4. Each test creates a uniquely named database and removes it afterward; none uses an existing application database.

```powershell
$env:OSPF_TEST_MYSQL_HOST = "127.0.0.1"
$env:OSPF_TEST_MYSQL_PORT = "3306"
$env:OSPF_TEST_MYSQL_USER = "<test account>"
$env:OSPF_TEST_MYSQL_PASSWORD = "<test password>"
mvn -pl ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mysql -am '-Dtest=MySQLMybatisIntegrationTest,MySQLKtormLiveIntegrationTest,MySQLJdbcClientLiveIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

If the host is set, missing credentials, connection errors, an unexpected server version in the version-checking tests, or insufficient privileges fail the corresponding test.
