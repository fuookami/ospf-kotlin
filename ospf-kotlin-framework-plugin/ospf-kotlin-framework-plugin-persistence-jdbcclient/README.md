# ospf-kotlin-framework-plugin-persistence-jdbcclient

[English](README.md) | [简体中文](README_ch.md)

Spring `JdbcClient` adapter for the expression persistence API. The core module depends on `spring-jdbc` and does not select or bundle a JDBC driver. Database plugins provide a `JdbcClientDialect` and may construct a `JdbcClientBackend` from their managed connection pool.

## Public API

| API | Description |
| --- | --- |
| `JdbcClientBackend` | Pairs a Spring `JdbcClient` with its SQL dialect |
| `JdbcClientDialect` | Supplies safe identifier quoting, pagination, and null-order SQL fragments |
| `JdbcClientRepository<E>` | Implements `ExpressionRepository<E>` for a single table using an explicit `RowMapper<E>` |
| `JdbcClientColumnBinder` | Maps expression paths to physical column names |
| `JdbcClientValueConverter` | Converts OSPF values to JDBC-compatible values |
| `JdbcClientBooleanTranslator` | Translates boolean predicates into parameterized SQL |
| `JdbcClientScalarTranslator` | Translates scalar expressions into parameterized SQL |
| `JdbcClientOrderByTranslator` | Translates sort rules through the selected dialect |
| `JdbcClientUpdateTranslator` | Validates and translates all update assignments before execution |

## Quick start

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

All repository operations return `Ret`. The default unsupported-predicate policy is `FailFast`; callers may explicitly select `AlwaysFalse`, which closes the whole predicate to `1 = 0`. `ClientFilter` returns a structured failure because this adapter does not fetch entire tables for client-side filtering. Errors from the JDBC client and row mapper are converted to failed results.

Values are bound with `?` parameters in SQL appearance order. Update values are ordered as `SET` parameters followed by `WHERE` parameters. Table and column names are resolved then quoted one identifier segment at a time by the dialect. Each application supplies its entity `RowMapper`; this adapter does not infer joins, inserts, or entity mappings.

The repository does not own an externally supplied `JdbcClient` lifecycle. Database-specific plugins can provide managed backend factories and dialect implementations for MySQL, PostgreSQL, H2, or other JDBC databases.
