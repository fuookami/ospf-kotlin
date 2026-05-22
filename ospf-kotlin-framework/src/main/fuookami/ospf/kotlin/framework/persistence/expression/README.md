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
| `UnsupportedPredicatePolicy` | Policy for predicates that cannot be pushed down (`FailFast`/`AlwaysFalse`/`ClientFilter`) |
| `PredicateTranslation` | Structured predicate translation result for future translator APIs |

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

Scalar predicates can compare columns and basic arithmetic expressions when the selected plugin supports them:

```kotlin
val columnComparison = path("startAt").lt(path("endAt"))
val amountCheck = (path("price") * path("quantity")).gt(100)
```

Scalar functions reuse `ospf-kotlin-math` `ScalarFunction` nodes. They are logical function symbols, not raw SQL strings. `abs` remains in the math DSL because it is also a math function; repository-oriented string/null functions are exposed from this framework package:

```kotlin
val absoluteBalance = abs(path("balance")).gt(10)
val normalizedName = lower(path("name")).eq("alice")
```

Standard function names are centralized in `ScalarFunctionNames`. The current built-in functions are `abs`, `lower`, `upper`, `trim`, `length`, and `coalesce`. Ktorm, MyBatis-Plus, and MongoDB translators only push down registered standard functions; unknown functions follow `UnsupportedPredicatePolicy` and are never emitted as uncontrolled SQL or Mongo expressions.

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

Repositories default to `UnsupportedPredicatePolicy.AlwaysFalse`, so unsupported predicates return an empty result instead of becoming unfiltered queries. `FailFast` can be selected by repository constructors for explicit failures. `ClientFilter` is reserved and fails explicitly until client-side filtering is implemented.

## Dependencies

- `ospf-kotlin-math`: `BooleanExpression`, `ScalarExpression`, `PropertyPath`

## Related

- [math/symbol/expression](../../../../../../../../../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/expression/README.md) - Core expression AST
- [sql_expression.md](../../../../../../../../sql_expression.md) - Current status and execution backlog
