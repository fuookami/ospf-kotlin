# ospf-kotlin-framework-plugin 边界说明

[English Documentation (README.md)](./README.md)

本模块组提供消息与持久化插件，同时把领域物理量建模职责留在插件外部。

## 模块范围

- `ospf-kotlin-framework-plugin-message-kafka`
- `ospf-kotlin-framework-plugin-persistence-ktorm`
- `ospf-kotlin-framework-plugin-persistence-mybatis`
- `ospf-kotlin-framework-plugin-persistence-mongodb`
- `ospf-kotlin-framework-plugin-persistence-mysql`
- `ospf-kotlin-framework-plugin-persistence-sqlite`
- `ospf-kotlin-framework-plugin-persistence-redis`
- `ospf-kotlin-framework-plugin-persistence-expression-ksp`

## 持久化字段边界

所有持久化 translator 通过统一抽象解析字段：

```kotlin
typealias PersistenceFieldResolver<C> = (String) -> C?
```

后端类型别名保持清晰：

- `KtormColumnResolver = PersistenceFieldResolver<ColumnDeclaring<*>>`
- `MybatisColumnNameResolver = PersistenceFieldResolver<String>`
- `MongoFieldNameResolver = PersistenceFieldResolver<String>`

translator 只消费已经映射到 PO 的字段路径，不会自动展开领域 `Quantity<V>`。

## PO/DTO 物理量所有权示例

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

推荐查询方式：

- 在持久化表达式中查询 `widthValue` / `widthUnitSymbol`。
- 在 repository 的 `from/into` 映射中处理 `Quantity<V> <-> PO`。
- 不依赖插件 translator 推断 `Quantity<V>` 的落库结构。

## Kafka 序列化策略

Kafka 传输能力是泛型且由 DTO 驱动，调用方提供 payload DTO 与序列化器：

```kotlin
kafka.send(
    topic = "packaging-material",
    value = packagingMaterialDto,
    serializable = { dto -> json.encodeToString(dto) }
)

kafka.listen(
    topic = "packaging-material",
    process = { dto -> /* dto -> domain 映射 */ },
    deserializer = { raw -> json.decodeFromString<PackagingMaterialDTO>(raw) }
)
// 或者：
kafka.listen(
    topic = "packaging-material",
    process = { dto: PackagingMaterialDTO ->
    // dto -> domain 映射
    }
)
```

插件层不固定 `Flt64`，也不固定 `Quantity` payload 结构。
