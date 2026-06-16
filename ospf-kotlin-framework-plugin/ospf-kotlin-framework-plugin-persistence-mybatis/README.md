# ospf-kotlin-framework-plugin-persistence-mybatis

:us: English | :cn: [简体中文](README_ch.md)

MyBatis-Plus-based relational persistence plugin for the OSPF Kotlin framework.

## Public API

| Symbol | Kind | Description |
| --- | --- | --- |
| `MybatisColumnNameResolver` | typealias | `PersistenceFieldResolver<String>` |
| `MybatisRepository<E, M>` | abstract class | Base repository implementing `ExpressionRepository<E>` on MyBatis-Plus |
| `MybatisBooleanTranslator<T>` | class | `BooleanExpression` → `QueryWrapper`/`UpdateWrapper` conditions |
| `MybatisScalarSql` | data class | Parameterized SQL fragment with `sql`, `params`, `isColumnOnly` |
| `MybatisScalarTranslator` | class | `ScalarExpression<*>` → parameterized SQL fragment |
| `MybatisOrderByTranslator<T>` | class | `SortBy` → `QueryWrapper` ORDER BY |
| `MybatisUpdateTranslator<T>` | class | `UpdateAssignments` → `UpdateWrapper` SET |
| `MybatisValueConverter` | object | Converts OSPF custom types to JDBC-compatible types |

## Quick Start

```kotlin
class OrderRepository(
    mapper: OrderMapper
) : MybatisRepository<Order, OrderMapper>(
    mapper = mapper,
    resolveColumnName = MybatisRepository.simpleColumnResolver()
) {
    // No mapToEntity needed; MyBatis-Plus handles it
}

// Query
val results = repository.find(
    where = OrderPO::code eq "ORD-001"
)

// Count
val total = repository.count(where)

// Update
repository.update(where, assignments)

// Delete
repository.delete(where)
```

## MybatisScalarSql

`MybatisScalarTranslator` produces `MybatisScalarSql` fragments that carry parameterized SQL with positional placeholders (`{0}`, `{1}`, …). When combining left and right fragments in a binary expression, `shifted(offset)` renumbers placeholders so multiple fragments can be merged safely.

## Value Type Conversion

`MybatisValueConverter` automatically converts OSPF custom types to JDBC-compatible types before passing parameters to MyBatis-Plus wrappers. All three translators (`MybatisScalarTranslator`, `MybatisBooleanTranslator`, `MybatisUpdateTranslator`) use this converter internally.

| OSPF Kotlin Type | Converted JVM Type | SQL Type | JDBC Type (`java.sql.Types`) | Notes |
| --- | --- | --- | --- | --- |
| `UInt32` | `Int` | `INT` | `INTEGER` (4) | |
| `Int32` | `Int` | `INT` | `INTEGER` (4) | |
| `UInt64` | `Long` | `BIGINT` | `BIGINT` (-5) | |
| `Int64` | `Long` | `BIGINT` | `BIGINT` (-5) | |
| `Flt32` | `Float` | `FLOAT` | `FLOAT` (6) | |
| `Flt64` | `Double` | `DOUBLE` | `DOUBLE` (8) | |
| `FltX` | `BigDecimal` | `DECIMAL` | `DECIMAL` (3) | |
| `kotlinx.datetime.LocalDateTime` | `java.time.LocalDateTime` | `DATETIME` | `TIMESTAMP` (93) | |
| `kotlin.time.Duration` | `String` | `VARCHAR` | `VARCHAR` (12) | Stored as ISO-8601 string (`Duration.toIsoString`, e.g. `PT1H30M`) |
| `java.time.ZoneId` | `String` | `VARCHAR` | `VARCHAR` (12) | Stored as IANA zone id (e.g. `America/New_York`) |
| `java.time.ZoneOffset` | `String` | `VARCHAR` | `VARCHAR` (12) | Stored as offset id (e.g. `+08:00`) |
| `kotlinx.datetime.TimeZone` | `String` | `VARCHAR` | `VARCHAR` (12) | Stored as IANA zone id (e.g. `Europe/Berlin`) |
| `List<Enum<*>>` | `String` | `VARCHAR` | `VARCHAR` (12) | Stored as comma-separated enum names (e.g. `A,B,C`) |

You can also use the converter directly for custom scenarios:

```kotlin
val jdbcValue = MybatisValueConverter.convert(Flt64(3.14))  // returns 3.14 (Double)
val jdbcValue = MybatisValueConverter.convert(UInt32(42u))   // returns 42 (Int)
```
