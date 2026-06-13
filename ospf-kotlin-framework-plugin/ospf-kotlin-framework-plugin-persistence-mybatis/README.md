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
