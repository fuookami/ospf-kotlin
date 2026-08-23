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
| `KtormScalarBinding` | data class | Explicit constant-value and target-`SqlType` binding result |
| `KtormTargetConstantBinder` | typealias | Target-type-aware constant binding for custom or transformed `SqlType` values |
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
| `ResolveColumnBuilder` / `resolveColumn` | class / function | Register qualified columns for one or more query sources |
| `resolveColumnWithDiagnostics` | function | Build a resolver that distinguishes missing, ambiguous, and invalid mappings |
| `KtormRelationalQueryCompiler` | class | Compile a source-allowlisted relational query plan to Ktorm |

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

## Qualified and Versioned Column Mappings

Use `resolveColumn` when a query reads more than one registered source. `mainTable` and `versionTable` are convenience scopes for the common main-table plus version-table layout:

```kotlin
val resolver = resolveColumnWithDiagnostics {
    mainTable {
        map("id", MainTable.id)
        map("orgId", MainTable.orgId)
    }
    versionTable {
        map("name", VersionTable.name)
        map("status", VersionTable.status)
    }
}

resolver("main.id")       // MainTable.id
resolver("version.name")  // VersionTable.name
```

Qualified paths are resolved only from explicitly registered sources or aliases. An unqualified path is accepted only when it has one candidate; a short-path collision returns `PersistenceFieldResolution.Ambiguous` from the diagnostic resolver and `null` from the nullable resolver. Unknown qualifiers are not inferred from the last path segment. `KtormRelationalQueryCompiler` requires every `KtormQuerySource` to provide an explicit diagnostic field resolver and a `defaultColumns` allowlist for implicit root/`EXISTS` projections; it never falls back to `table.columns`, and unregistered physical columns are rejected structurally.

When a scalar constant is compared with a resolved column, `KtormScalarTranslator` uses the column's `SqlType`. Values for transformed columns remain domain values until Ktorm invokes the column transform during real JDBC execution, so repositories do not need a separate value-object unwrapping registry. Without a target column, the default bindings include `UInt64` to `Long` and `FltX` to `BigDecimal`; custom or transformed `SqlType` values must provide an explicit target-aware `targetConstantBinder`. Compilation never invokes a fabricated `PreparedStatement`; unregistered, target-incompatible, or ordinarily failing binder values are rejected structurally as parameter-binding failures. Cancellation exceptions and `Error` are allowed to propagate.

## Relational Query Plans

`KtormRelationalQueryCompiler` consumes the framework's `RelationalQueryPlan` container. The adapter owns the
`KtormQuerySource` allowlist and column mappings; callers cannot provide table names, column names, or SQL strings.

The plan supports `Inner`, `Left`, and `Exists` joins, qualified source aliases, column-to-column predicates,
projections, `DISTINCT`, grouping, null-aware ordering, pagination, and single-column root-granularity counts.
Join conditions must correlate the current source with an already-bound source through column references and may
not contain boolean constants. `Exists` accepts only one-to-many or many-to-many cardinality declarations and is
compiled as a correlated semi-join, so it does not multiply root rows. `compileCount` accepts only a single root key
from the root source; associated-source keys and composite root keys are rejected structurally.

The plan defensively copies outer collections, expression containers, and known mutable payload containers at
construction time. `List`, `Map`, `Set`, and array payloads are recursively snapshotted. Opaque custom payloads and
cyclic containers that cannot be copied safely are rejected by the plan's `validate()` gate as structured failures;
they are never replaced with fake snapshots. `canonical()`/`canonicalHash()` describe normalized expression shape and
type/container structure, omitting scalar and custom payload literal values.

When no explicit projection is supplied, the adapter selects only the fields named by the source's `defaultColumns`
allowlist. The list is resolved through the same diagnostic field mapping as predicates and cannot introduce an
unregistered physical column.

The compiler returns `Ret` and classifies source, column, predicate, SQL-generation, and database failures as
structured `RelationalQueryFailure` values. `QueryAuditSummary` contains the generated SQL template, dialect name,
and SQL parameter type names; it never contains bound parameter values. Execution statistics report duration and
returned rows. When a positive `maxReturnedRows` is supplied, execution probes for an additional row and sets
`truncated` accordingly. Scan counts remain unavailable unless the backend can provide them reliably.

The compiler currently relies on Ktorm's dialect support for joins, grouping, ordering, and pagination. SQLite has
real execution coverage in this module. PostgreSQL and MySQL use the same Ktorm expression path, but their dialect
compatibility must be verified by the consuming adapter before enabling a capability.

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
| `fltx` | `FltX` | `DECIMAL` | `DECIMAL` (3) | Default scale = 18 |
| `fltx` | `FltX` | `DECIMAL` | `DECIMAL` (3) | Custom rounding mode |
| `kotlinDatetime` | `kotlinx.datetime.LocalDateTime` | `DATETIME` | `TIMESTAMP` (93) | |
| `instant` | `kotlin.time.Instant` | `TIMESTAMP` | `TIMESTAMP` (93) | |
| `duration` | `kotlin.time.Duration` | `VARCHAR` | `VARCHAR` (12) | Stored as ISO-8601 string (`Duration.toIsoString`) |
| `durationLong` | `kotlin.time.Duration` | `BIGINT` | `BIGINT` (-5) | Stored as long integer; unit defaults to milliseconds, configurable via `DurationUnit` parameter |
| `durationMs` | `kotlin.time.Duration` | `BIGINT` | `BIGINT` (-5) | Stored as truncated whole milliseconds; non-finite or unrepresentable values are rejected |
| `zoneId` | `java.time.ZoneId` | `VARCHAR` | `VARCHAR` (12) | Stored as IANA zone id (e.g. `America/New_York`) |
| `zoneOffset` | `java.time.ZoneOffset` | `VARCHAR` | `VARCHAR` (12) | Stored as offset id (e.g. `+08:00`) |
| `kotlinTimeZone` | `kotlinx.datetime.TimeZone` | `VARCHAR` | `VARCHAR` (12) | Stored as IANA zone id (e.g. `Europe/Berlin`) |
| `enums<T>` | `List<T : Enum<*>>` | `VARCHAR` | `VARCHAR` (12) | Stored as comma-separated enum names (e.g. `A,B,C`) |
