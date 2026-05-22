# ospf-kotlin-framework/persistence/expression

[中文文档 (README_ch.md)](./README_ch.md)

SQL Expression framework layer interfaces and models.

## Architecture

### Core Components

| Component | Purpose |
|-----------|---------|
| `ExpressionRepository` | Unified CRUD-style expression repository contract |
| `SortBy` | Multi-field sorting model with nulls order support |
| `UpdateAssignments` | UPDATE SET statement model (`SetValue`/`SetNull`/`SetFromExpression`) |
| `NullsOrderSupport` | NULLS FIRST/LAST capability model |

### Plugin Translators

Repository implementations and translators are in plugin modules:

- Ktorm plugin:
  `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm`
- MyBatis-Plus plugin:
  `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis`
- MongoDB plugin:
  `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb`

## Usage

### Build Query/Update Models

```kotlin
val where = path("status").eq("active") and path("age").gt(18)
val sortBy = SortBy.desc("createdAt")
val assignments = UpdateAssignments
    .set("status", "inactive")
    .thenSetNull("deletedAt")
```

### Resolver Functions (Current Mapping Mechanism)

```kotlin
// Ktorm
val ktormResolver: KtormColumnResolver = { path -> /* resolve to ColumnDeclaring<*> */ null }

// MyBatis-Plus
val mybatisResolver: MybatisColumnNameResolver = { path -> path.substringAfterLast(".") }

// MongoDB
val mongoResolver: MongoFieldNameResolver = { path -> path.substringAfterLast(".") }
```

### Repository API

```kotlin
val users = repository.find(where, sortBy = sortBy, limit = 10, offset = 0)
val total = repository.count(where)
val updated = repository.update(where, assignments)
val deleted = repository.delete(where)
val exists = repository.exists(where)
```

## Dependencies

- `ospf-kotlin-math`: `BooleanExpression`, `ScalarExpression`, `PropertyPath`

## Related

- [math/symbol/expression](../../../math/src/main/fuookami/ospf/kotlin/math/symbol/expression/README.md) - Core expression AST
- [sql_expression.md](../../../../../../../../sql_expression.md) - Current status and execution backlog
