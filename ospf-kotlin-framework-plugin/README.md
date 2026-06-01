# ospf-kotlin-framework-plugin Boundary Guide

:us: English | :cn: [简体中文](README_ch.md)

This module set provides persistence and message plugins, and keeps domain quantity modeling outside plugin internals.

## Scope

- `ospf-kotlin-framework-plugin-message-kafka`
- `ospf-kotlin-framework-plugin-persistence-ktorm`
- `ospf-kotlin-framework-plugin-persistence-mybatis`
- `ospf-kotlin-framework-plugin-persistence-mongodb`
- `ospf-kotlin-framework-plugin-persistence-mysql`
- `ospf-kotlin-framework-plugin-persistence-sqlite`
- `ospf-kotlin-framework-plugin-persistence-redis`
- `ospf-kotlin-framework-plugin-persistence-expression-ksp`

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
