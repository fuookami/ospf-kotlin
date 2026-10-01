# OSPF Kotlin Framework Plugin

:us: English | :cn: [简体中文](README_ch.md)

This module set provides persistence and message infrastructure plugins for the OSPF Kotlin framework, keeping domain quantity modeling outside plugin internals.

## Sub-Modules

| Sub-module | Description |
| --- | --- |
| `ospf-kotlin-framework-plugin-message-kafka` | Kafka message producer/consumer wrapper with topic and pattern-matching subscription |
| `ospf-kotlin-framework-plugin-persistence-ktorm` | Ktorm-based relational repository with expression-to-SQL translation |
| `ospf-kotlin-framework-plugin-persistence-mybatis` | MyBatis-Plus-based relational repository with expression-to-Wrapper translation |
| `ospf-kotlin-framework-plugin-persistence-mongodb` | MongoDB document repository with expression-to-Bson translation, plus API request/response persistence |
| `ospf-kotlin-framework-plugin-persistence-mysql` | MySQL datasource initialization and Ktorm / MyBatis-Plus / JdbcClient management |
| `ospf-kotlin-framework-plugin-persistence-postgresql` | PostgreSQL datasource initialization and Ktorm / MyBatis-Plus / JdbcClient management |
| `ospf-kotlin-framework-plugin-persistence-h2` | H2 datasource initialization and Ktorm / MyBatis-Plus / JdbcClient management |
| `ospf-kotlin-framework-plugin-persistence-jdbcclient` | Spring JdbcClient expression repository and relational SQL translation |
| `ospf-kotlin-framework-plugin-persistence-sqlite` | SQLite datasource initialization and Ktorm / MyBatis-Plus / JdbcClient management |
| `ospf-kotlin-framework-plugin-persistence-redis` | Redis sentinel client management with structured data and serialization extensions |
| `ospf-kotlin-framework-plugin-persistence-expression-ksp` | KSP compile-time processor that generates `PredicateSchema` from `@PredicateEntity` annotations |

## Relational Database Matrix

Each relational adapter has a database-specific factory in the corresponding database plugin. Factory methods return `Ret` values and share that plugin's managed datasource:

| Database | Ktorm | MyBatis-Plus | Spring JdbcClient |
| --- | --- | --- | --- |
| SQLite | `Sqlite.initKtorm` / `Sqlite.getKtormBackend` | `Sqlite.initMybatis` / `Sqlite.getMybatisBackend` | `Sqlite.initJdbcClient` / `Sqlite.getJdbcClient` |
| MySQL | `MySQL.initKtorm` / `MySQL.getKtormBackend` | `MySQL.initMybatis` / `MySQL.getMybatisBackend` | `MySQL.initJdbcClient` / `MySQL.getJdbcClient` |
| PostgreSQL | `PostgreSQL.initKtorm` / `PostgreSQL.getKtormBackend` | `PostgreSQL.initMybatis` / `PostgreSQL.getMybatisBackend` | `PostgreSQL.initJdbcClient` / `PostgreSQL.getJdbcClient` |
| H2 | `H2.initKtorm` / `H2.getKtormBackend` | `H2.initMybatis` / `H2.getMybatisBackend` | `H2.initJdbcClient` / `H2.getJdbcClient` |

The Ktorm backend pairs `Database` with the matching `RelationalQueryDialect`; pass the backend when using the relational query compiler so database-specific pagination and SQL capabilities are explicit. The MyBatis backend owns its `SqlSessionFactory`; `openSession` and `mapper` return `Ret`, while `commit`, `rollback`, and session `close` return `Try`. Close each short-lived session scope when finished; the backend does not close a datasource supplied by its database manager. Existing `MybatisRepository` construction with an externally configured `BaseMapper` remains supported. The JdbcClient backend pairs Spring `JdbcClient` with a `JdbcClientDialect` for identifier quoting, pagination, and null ordering.

The database plugin owns the shared connection pool. Call its `close(key): Try` method with that plugin's `*ClientKey` type to close the pool; this also invalidates Ktorm, MyBatis, and JdbcClient backends created for that key. There is no close-by-name overload. Directly supplied `Database`, `BaseMapper`, `JdbcClient`, or `DataSource` instances remain owned by the caller. The original nullable `Sqlite.init` and `MySQL.init` Ktorm entry points remain available for compatibility; the standardized `initKtorm` entry points return `Ret`.

For MyBatis, the selected `MybatisDialect` controls database-specific SQL. Database factories choose their matching dialect; direct repository construction with an external `BaseMapper` should also select one. The portable dialect returns `Failed` for unsupported SQL expressions.

## Persistence Field Boundary

All persistence translators resolve fields through a single concept:

```kotlin
typealias PersistenceFieldResolver<C> = (String) -> C?
```

Backend aliases stay explicit:

- `KtormColumnResolver = PersistenceFieldResolver<ColumnDeclaring<*>>`
- `MybatisColumnNameResolver = PersistenceFieldResolver<String>`
- `MongoFieldNameResolver = PersistenceFieldResolver<String>`

The translator only consumes already-mapped PO field paths and does not auto-expand domain `Quantity<V>`.

## Expression Translation Architecture

Each relational/document backend provides the same four translation components:

| Component | Ktorm | MyBatis-Plus | MongoDB | JdbcClient |
| --- | --- | --- | --- | --- |
| Boolean expression | `KtormBooleanTranslator` | `MybatisBooleanTranslator` | `MongoBooleanTranslator` | `JdbcClientBooleanTranslator` |
| Scalar expression | `KtormScalarTranslator` | `MybatisScalarTranslator` | `MongoScalarTranslator` | `JdbcClientScalarTranslator` |
| Order by | `KtormOrderByTranslator` | `MybatisOrderByTranslator` | `MongoOrderByTranslator` | `JdbcClientOrderByTranslator` |
| Update | `KtormUpdateTranslator` | `MybatisUpdateTranslator` | `MongoUpdateTranslator` | `JdbcClientUpdateTranslator` |

All boolean translators support:

- `Comparison` (eq, ne, lt, le, gt, ge) with constant reversal
- `InExpression` (in / not-in)
- `PatternMatch` (exact, prefix, suffix, contains, like)
- `NullCheck` (is-null / is-not-null)
- `AndExpression`, `OrExpression`, `NotExpression`
- `UnsupportedPredicatePolicy` (`FailFast`, `AlwaysFalse`, `ClientFilter`)

Regex pattern matching is backend-specific: MongoDB supports native regex; MyBatis-Plus and JdbcClient treat regex as unsupported under the configured `UnsupportedPredicatePolicy`; Ktorm delegates it to `PatternMatchPolicy`, and the built-in default, SQLite, PostgreSQL, and MySQL policies currently do not support regex.

All scalar translators support:

- Column references and constants
- Unary operators (negate, positive)
- Binary operators (add, subtract, multiply, divide, modulo)
- Functions (ABS, LOWER, UPPER, TRIM, LENGTH, COALESCE)

For MyBatis, `LENGTH` is rendered according to the selected database dialect. The portable dialect returns a structured failure for that expression.

## Repository Base Classes

| Backend | Base class | Key type |
| --- | --- | --- |
| Ktorm | `KtormRepository<E>` | `KtormColumnResolver` |
| MyBatis-Plus | `MybatisRepository<E, M>` | `MybatisColumnNameResolver` |
| MongoDB | `MongoRepository<E>` | `MongoFieldNameResolver` |
| Spring JdbcClient | `JdbcClientRepository<E>` | `JdbcClientColumnNameResolver` |

Each repository implements `ExpressionRepository<E>` with `find`, `count`, `update`, and `delete` operations driven by `BooleanExpression` from `math.symbol.expression`.

JdbcClient uses bound `?` parameters and a `JdbcClientDialect` supplied by a database plugin. Each of the four database modules provides its own quoting, pagination, and null-ordering rules.

## Database Integration Tests

Repository integration tests exercise CRUD across all twelve database/adapter combinations. SQLite tests use isolated temporary files and H2 tests use isolated in-memory databases, so they run without an external database service. MySQL and PostgreSQL tests create uniquely named temporary databases and remove them afterward. Those live tests are skipped unless `OSPF_TEST_MYSQL_HOST` or `OSPF_TEST_POSTGRESQL_HOST` is set; the database module READMEs list the required port and credential variables, server versions, and test commands. See [MySQL test setup](ospf-kotlin-framework-plugin-persistence-mysql/README.md) and [PostgreSQL test setup](ospf-kotlin-framework-plugin-persistence-postgresql/README.md).

## PO/DTO Quantity Ownership Example

```kotlin
data class PackagingMaterialPO(
    val widthValue: FltX,
    val widthUnitSymbol: UnitSymbol?,
    val tareWeightValue: FltX,
    val tareWeightUnitSymbol: UnitSymbol?
) {
    companion object {
        fun from(domain: PackagingMaterial): PackagingMaterialPO = TODO()
    }

    fun into(unitResolver: UnitResolver): Ret<PackagingMaterial> = TODO()
}
```

Recommended query shape:

- Query by `widthValue` / `widthUnitSymbol` in persistence expressions.
- Keep `Quantity<V> <-> PO` conversion in repository `from/into` mapping.
- Do not expect plugin translators to infer storage layout for `Quantity<V>`.

## KSP Predicate Schema Generator

`PredicateSchemaProcessor` is a KSP `SymbolProcessor` that:

1. Scans classes annotated with `@PredicateEntity`.
2. Generates a companion `PredicateSchema` object with typed field references.
3. Optionally generates a `resolver: (String) -> String?` lambda for backend field name mapping.

Usage:

```kotlin
@PredicateEntity
data class MaterialPO(
    val code: String,
    @PredicateField(name = "width_val") val widthValue: Double
)

// Generated:
object MaterialPOSchema : PredicateSchema<MaterialPO>() {
    val code = field(MaterialPO::code)
    val widthValue = field(MaterialPO::widthValue)
    val resolver: (String) -> String? = { path ->
        when (path) {
            "code" -> "code"
            "widthValue" -> "width_val"
            else -> null
        }
    }
}
```

## Kafka Serializer Strategy

Kafka transport is generic and DTO-driven. Callers provide payload DTO and serializer:

```kotlin
kafka.send(
    topic = "packaging-material",
    value = packagingMaterialDto,
    serializable = { dto -> json.encodeToString(dto) }
)

kafka.listen(
    topic = "packaging-material",
    process = { dto -> /* map dto -> domain */ },
    deserializer = { raw -> json.decodeFromString<PackagingMaterialDTO>(raw) }
)
// or:
kafka.listen(
    topic = "packaging-material",
    process = { dto: PackagingMaterialDTO ->
    // map dto -> domain
    }
)
```

The plugin does not enforce `Flt64` and does not enforce a fixed `Quantity` payload schema.

## Pattern Match Policy (Ktorm)

Ktorm boolean translator accepts a `PatternMatchPolicy` to handle LIKE/ILIKE/REGEX dialect differences:

| Policy | LIKE | Regex |
| --- | --- | --- |
| `DefaultPatternMatchPolicy` | `column.like(pattern)` | Not supported |
| `SqlitePatternMatchPolicy` | `column.like(pattern)` | Not supported |
| `PostgresPatternMatchPolicy` | `column.like(pattern)` | Not supported |
| `MySqlPatternMatchPolicy` | `column.like(pattern)` | Not supported |

MongoDB uses native `$regex` for all pattern modes; MyBatis-Plus uses `LIKE`/`NOT LIKE` with SQL wildcards.
