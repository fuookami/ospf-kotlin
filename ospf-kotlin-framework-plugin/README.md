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
| `ospf-kotlin-framework-plugin-persistence-mysql` | MySQL datasource initialization and Ktorm `Database` management |
| `ospf-kotlin-framework-plugin-persistence-sqlite` | SQLite datasource initialization and Ktorm `Database` management |
| `ospf-kotlin-framework-plugin-persistence-redis` | Redis sentinel client management with structured data and serialization extensions |
| `ospf-kotlin-framework-plugin-persistence-expression-ksp` | KSP compile-time processor that generates `PredicateSchema` from `@PredicateEntity` annotations |

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

| Component | Ktorm | MyBatis-Plus | MongoDB |
| --- | --- | --- | --- |
| Boolean expression | `KtormBooleanTranslator` | `MybatisBooleanTranslator` | `MongoBooleanTranslator` |
| Scalar expression | `KtormScalarTranslator` | `MybatisScalarTranslator` | `MongoScalarTranslator` |
| Order by | `KtormOrderByTranslator` | `MybatisOrderByTranslator` | `MongoOrderByTranslator` |
| Update | `KtormUpdateTranslator` | `MybatisUpdateTranslator` | `MongoUpdateTranslator` |

All boolean translators support:

- `Comparison` (eq, ne, lt, le, gt, ge) with constant reversal
- `InExpression` (in / not-in)
- `PatternMatch` (exact, prefix, suffix, contains, like, regex)
- `NullCheck` (is-null / is-not-null)
- `AndExpression`, `OrExpression`, `NotExpression`
- `UnsupportedPredicatePolicy` (`FailFast`, `AlwaysFalse`, `ClientFilter`)

All scalar translators support:

- Column references and constants
- Unary operators (negate, positive)
- Binary operators (add, subtract, multiply, divide, modulo)
- Functions (ABS, LOWER, UPPER, TRIM, LENGTH, COALESCE)

## Repository Base Classes

| Backend | Base class | Key type |
| --- | --- | --- |
| Ktorm | `KtormRepository<E>` | `KtormColumnResolver` |
| MyBatis-Plus | `MybatisRepository<E, M>` | `MybatisColumnNameResolver` |
| MongoDB | `MongoRepository<E>` | `MongoFieldNameResolver` |

Each repository implements `ExpressionRepository<E>` with `find`, `count`, `update`, and `delete` operations driven by `BooleanExpression` from `math.symbol.expression`.

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
