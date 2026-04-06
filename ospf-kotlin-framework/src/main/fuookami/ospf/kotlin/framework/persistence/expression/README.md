# ospf-kotlin-framework/persistence/expression

[中文文档 (README_ch.md)](./README_ch.md)

SQL Expression framework layer implementation for Ktorm integration.

## Architecture

### Core Components

| Component | Purpose |
|-----------|---------|
| `EntityMeta` | PO field to database column mapping |
| `FieldBinding` | Field binding definition with optional transformer |
| `SortBy` | Multi-field sorting model with nulls order support |
| `UpdateAssignment` | UPDATE SET statement model |

### Translators

| Translator | Purpose |
|------------|---------|
| `KtormBooleanTranslator` | BooleanExpression -> Ktorm WHERE condition |
| `KtormOrderByTranslator` | SortBy -> Ktorm ORDER BY clause |
| `KtormUpdateTranslator` | UpdateAssignment -> Ktorm UPDATE SET |

### Dialect Policies

| Policy | Purpose |
|--------|---------|
| `PatternMatchPolicy` | LIKE/ILIKE/REGEX dialect handling |
| `NullsOrderSupport` | NULLS FIRST/LAST support detection |

## Usage

### Define Entity Metadata

```kotlin
val userMeta = EntityMeta<User>(User::class, "users") {
    binding("id", User::id, Users.id)
    binding("name", User::name, Users.name)
    binding("email", User::email, Users.email)
    binding("status", User::status, Users.status)
}
```

### Query with BooleanExpression

```kotlin
val where = path("status").eq("active") and path("age").gt(18)
val users = repository.find(where, sortBy = SortBy.desc("createdAt"), limit = 10)
```

### Update with UpdateAssignment

```kotlin
val assignments = UpdateAssignments.set("status", "inactive") + UpdateAssignments.setNull("deletedAt")
val updated = repository.update(where, assignments)
```

## Dependencies

- `ospf-kotlin-math`: BooleanExpression, ScalarExpression, PropertyPath
- `ktorm-core`: Ktorm ORM framework
- `ktorm-support-sqlite`: SQLite dialect support

## Related

- [math/symbol/expression](../../../math/src/main/fuookami/ospf/kotlin/math/symbol/expression/README.md) - Core expression AST
- [sql_expression.md](../../../../../../../sql_expression.md) - Design document