# ospf-kotlin-framework-plugin-persistence-postgresql

:us: English | :cn: [简体中文](README_ch.md)

PostgreSQL plugin that manages a shared DBCP2 connection pool for Ktorm, MyBatis-Plus, and Spring JdbcClient adapters.

## Quick Start

```kotlin
val backend = PostgreSQL.initJdbcClient {
    url = "localhost:5432"
    name = "orders"
    database = "production"
    userName = "app"
    password = "secret"
}

if (backend.ok) {
    val client = backend.value!!.client
}
```

`url` is the PostgreSQL host and optional port; the database name is supplied separately. `properties` adds JDBC connection properties. The factory creates a lazy JdbcClient and does not test database connectivity; connection errors are returned by repository operations. The builder returns structured `Ret` errors for incomplete or invalid configuration without including credentials in messages.

## Public API

| Symbol | Description |
| --- | --- |
| `PostgreSQLConfigBuilder` / `PostgreSQLConfig` | Connection and DBCP pool configuration |
| `PostgreSQLClientKey` | Exact client lookup key (name + database) |
| `PostgreSQL.initKtorm` / `getKtormBackend` | Returns `Ret<KtormBackend>` with the PostgreSQL expression dialect |
| `PostgreSQL.initMybatis` / `getMybatisBackend` | Returns `Ret<MybatisBackend>` with the PostgreSQL MyBatis dialect |
| `PostgreSQL.initJdbcClient` / `getJdbcClient` | Returns `Ret<JdbcClientBackend>` with the PostgreSQL JdbcClient dialect |
| `PostgreSQL` | Lookup by key/name and managed pool close |
| `PostgreSQLJdbcClientDialect` | Identifier quoting, bound pagination, and native null ordering |

All three adapter factories and lookup methods return structured `Ret` results. Use `PostgreSQL.getJdbcClient(key)` for exact-key lookup. `PostgreSQL.close(key)` closes the shared managed pool and is idempotent when the key is already closed; Ktorm, MyBatis, and JdbcClient backends created for that key can no longer use the pool. JdbcClient instances created from an external `DataSource` remain caller-owned and are not managed by this plugin.

## Live Integration Test

The three live tests (`PostgreSQLMybatisIntegrationTest`, `PostgreSQLKtormLiveIntegrationTest`, and `PostgreSQLJdbcClientLiveIntegrationTest`) are skipped unless `OSPF_TEST_POSTGRESQL_HOST` is set. Set `OSPF_TEST_POSTGRESQL_USER` and `OSPF_TEST_POSTGRESQL_PASSWORD`; `OSPF_TEST_POSTGRESQL_PORT` is optional and defaults to `5432`. The account needs the PostgreSQL `CREATEDB` attribute and permission to create schemas and tables and perform CRUD in the temporary database. The MyBatis and JdbcClient tests verify PostgreSQL 17.11. Each test creates a uniquely named database and drops it afterward; none uses an existing application database.

```powershell
$env:OSPF_TEST_POSTGRESQL_HOST = "127.0.0.1"
$env:OSPF_TEST_POSTGRESQL_PORT = "5432"
$env:OSPF_TEST_POSTGRESQL_USER = "<test account>"
$env:OSPF_TEST_POSTGRESQL_PASSWORD = "<test password>"
mvn -pl ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-postgresql -am '-Dtest=PostgreSQLMybatisIntegrationTest,PostgreSQLKtormLiveIntegrationTest,PostgreSQLJdbcClientLiveIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

If the host is set, missing credentials, connection errors, an unexpected server version in the version-checking tests, or insufficient privileges fail the corresponding test.
