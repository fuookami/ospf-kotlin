# ospf-kotlin-framework-plugin-persistence-h2

:us: English | :cn: [简体中文](README_ch.md)

H2 plugin that manages one DBCP2 connection pool shared by Ktorm, MyBatis-Plus, and Spring JdbcClient adapters.

## Quick Start

```kotlin
val backend = H2.initJdbcClient {
    url = "jdbc:h2:mem:orders"
    name = "orders-test"
}

if (backend.ok) {
    val client = backend.value!!.client
}
```

Provide a JDBC URL beginning with `jdbc:h2:`. The builder defaults to user `sa` and an empty password; `properties` adds JDBC connection properties. `H2.initJdbcClient`, `H2.initKtorm`, and `H2.initMybatis` all return `Ret` results and reuse the same datasource cache. The JdbcClient factory is lazy and does not test database connectivity; connection errors are returned by repository operations. Configuration errors are returned as `Ret` and do not include credentials.

## Public API

| Symbol | Description |
| --- | --- |
| `H2ConfigBuilder` / `H2Config` | Connection and DBCP pool configuration |
| `H2ClientKey` | Client lookup key (name) |
| `H2.initKtorm` / `getKtormBackend` | Returns `Ret<KtormBackend>` with the H2 expression dialect |
| `H2.initMybatis` / `getMybatisBackend` | Returns `Ret<MybatisBackend>` with the H2 MyBatis dialect |
| `H2.initJdbcClient` / `getJdbcClient` | Returns `Ret<JdbcClientBackend>` with the H2 JdbcClient dialect |
| `H2` | Lookup by key/name and managed pool close |
| `H2JdbcClientDialect` | Identifier quoting, bound pagination, and null ordering |
| `H2KtormDialect` | Ktorm SQL formatter with H2 pagination support |

All three adapters created for a key use the same DBCP datasource. `H2.close(key)` closes and removes that pool; repeated close calls succeed and previously created Ktorm, MyBatis, and JdbcClient backends for that key can no longer use it. For in-memory databases, closing the pool releases its connections. If `DB_CLOSE_DELAY=-1` is part of the JDBC URL, H2 keeps the in-memory database alive after the last connection closes.
