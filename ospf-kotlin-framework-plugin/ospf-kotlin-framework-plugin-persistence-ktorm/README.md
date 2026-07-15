# ospf-kotlin-framework-plugin-persistence-ktorm

:us: English | :cn: [简体中文](README_ch.md)

Ktorm-based relational persistence plugin for the OSPF Kotlin framework.

## Public API

| Symbol | Kind | Description |
| --- | --- | --- |
| `KtormColumnResolver` | typealias | `PersistenceFieldResolver<ColumnDeclaring<*>>` |
| `ColumnNameResolver` | typealias | `(String) -> String?` |
| `KtormRepository<E>` | abstract class | Base repository implementing `ExpressionRepository<E>` on Ktorm |
| `KtormBooleanTranslator` | class | `BooleanExpression` → `ColumnDeclaring<Boolean>` |
| `KtormScalarTranslator` | class | `ScalarExpression<*>` → Ktorm `ScalarExpression<*>` |
| `KtormOrderByTranslator` | class | `SortBy` → `OrderByExpression` |
| `KtormUpdateTranslator` | class | `UpdateAssignments` → Ktorm UPDATE |
| `PatternMatchPolicy` | interface | LIKE/ILIKE/REGEX dialect strategy |
| `DefaultPatternMatchPolicy` | object | Standard SQL LIKE |
| `SqlitePatternMatchPolicy` | object | SQLite LIKE |
| `PostgresPatternMatchPolicy` | object | PostgreSQL LIKE |
| `MySqlPatternMatchPolicy` | object | MySQL LIKE |
| `KtormColumnBinder<T>` | class | Strong-typed column binder over a Ktorm `Table`, implements `ColumnBinder<ColumnDeclaring<*>>` |
| `asKtormResolver` | extension | Convert a `KtormColumnBinder` into a `KtormColumnResolver` |
| `HasColumnMapping.ktormResolver(table)` | extension | Build a `KtormColumnResolver` from a KSP-generated `HasColumnMapping` schema + Ktorm table |
| `ktormResolver(table, columnMapping)` | function | Build a `KtormColumnResolver` from a Ktorm table + explicit mapping |

## Quick Start

```kotlin
class OrderRepository(
    database: Database,
    table: Table<*>
) : KtormRepository<Order>(
    database = database,
    table = table,
    resolveColumn = KtormRepository.tableColumnResolver(table)
) {
    override fun mapToEntity(row: QueryRowSet): Order? = TODO()
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

## Strong-Typed Column Binding

`KtormColumnBinder` bridges a KSP-generated schema (`HasColumnMapping`) to a Ktorm table, producing a `KtormColumnResolver` that maps property paths to Ktorm `ColumnDeclaring` columns. It avoids hand-written `when (path)` resolvers.

```kotlin
import fuookami.ospf.kotlin.framework.persistence.expression.HasColumnMapping
import fuookami.ospf.kotlin.framework.persistence.expression.ktormResolver

// KSP-generated schema (implements HasColumnMapping via generateColumnMapping = true)
object Users : PredicateSchema<User>(), HasColumnMapping {
    override val columnMapping: Map<String, String> = mapOf(
        "id" to "user_id",
        "status" to "user_status"
    )
    val id = field(User::id)
    val status = field(User::status)
}

object UsersTable : Table<Nothing>("users") {
    val userId = int("user_id")
    val userStatus = varchar("user_status")
}

class UserRepository(db: Database) : KtormRepository<User>(
    database = db,
    table = UsersTable,
    resolveColumn = Users.ktormResolver(UsersTable)
) {
    override fun mapToEntity(row: QueryRowSet): User? = TODO()
}

// Predicate built from typed schema fields; columns resolved via columnMapping
val where = Users.predicate { status eq "active" }
val users = repository.find(where)
```

Use the explicit-mapping overload when you cannot annotate the entity:

```kotlin
val resolver = ktormResolver(UsersTable, mapOf("id" to "user_id", "status" to "user_status"))
```

`KtormColumnBinder` falls back to the raw path when a path is absent from `columnMapping`, matching the table column name directly.

## PatternMatchPolicy

Pass a `PatternMatchPolicy` to `KtormRepository` to control how `PatternMatch` expressions are translated. Default uses standard SQL `LIKE`. Custom implementations can override `translateLike` and `translateRegex` for dialect-specific behavior.

## Column Type Extensions

`SqlType.kt` provides extension functions on `BaseTable<*>` for declaring columns backed by OSPF custom types. Each function defines bidirectional transforms between Kotlin types and JDBC/SQL types.

| Function | Kotlin Type | SQL Type | JDBC Type (`java.sql.Types`) | Notes |
| --- | --- | --- | --- | --- |
| `ui32` | `UInt32` | `INT` | `INTEGER` (4) | |
| `i32` | `Int32` | `INT` | `INTEGER` (4) | |
| `ui64` | `UInt64` | `BIGINT` | `BIGINT` (-5) | |
| `i64` | `Int64` | `BIGINT` | `BIGINT` (-5) | |
| `f32` | `Flt32` | `FLOAT` | `FLOAT` (6) | |
| `f64` | `Flt64` | `DOUBLE` | `DOUBLE` (8) | |
| `fltx` | `FltX` | `DECIMAL` | `DECIMAL` (3) | Default scale = 2 |
| `fltx` | `FltX` | `DECIMAL` | `DECIMAL` (3) | Custom rounding mode |
| `kotlinDatetime` | `kotlinx.datetime.LocalDateTime` | `DATETIME` | `TIMESTAMP` (93) | |
| `instant` | `kotlin.time.Instant` | `TIMESTAMP` | `TIMESTAMP` (93) | |
| `duration` | `kotlin.time.Duration` | `VARCHAR` | `VARCHAR` (12) | Stored as ISO-8601 string (`Duration.toIsoString`) |
| `zoneId` | `java.time.ZoneId` | `VARCHAR` | `VARCHAR` (12) | Stored as IANA zone id (e.g. `America/New_York`) |
| `zoneOffset` | `java.time.ZoneOffset` | `VARCHAR` | `VARCHAR` (12) | Stored as offset id (e.g. `+08:00`) |
| `kotlinTimeZone` | `kotlinx.datetime.TimeZone` | `VARCHAR` | `VARCHAR` (12) | Stored as IANA zone id (e.g. `Europe/Berlin`) |
| `enums<T>` | `List<T : Enum<*>>` | `VARCHAR` | `VARCHAR` (12) | Stored as comma-separated enum names (e.g. `A,B,C`) |
