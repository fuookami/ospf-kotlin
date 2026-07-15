# OSPF Kotlin Framework Plugin

:us: [English](README.md) | :cn: 简体中文

本模块组提供 OSPF Kotlin 框架的持久化与消息基础设施插件，同时把领域物理量建模职责留在插件外部。

## 子模块

| 子模块 | 说明 |
| --- | --- |
| `ospf-kotlin-framework-plugin-message-kafka` | Kafka 消息生产者/消费者封装，支持主题订阅和模式匹配订阅 |
| `ospf-kotlin-framework-plugin-persistence-ktorm` | 基于 Ktorm 的关系型仓储，提供表达式到 SQL 的翻译 |
| `ospf-kotlin-framework-plugin-persistence-mybatis` | 基于 MyBatis-Plus 的关系型仓储，提供表达式到 Wrapper 的翻译 |
| `ospf-kotlin-framework-plugin-persistence-mongodb` | MongoDB 文档仓储，提供表达式到 Bson 的翻译，以及 API 请求/响应持久化 |
| `ospf-kotlin-framework-plugin-persistence-mysql` | MySQL 数据源初始化和 Ktorm `Database` 管理 |
| `ospf-kotlin-framework-plugin-persistence-sqlite` | SQLite 数据源初始化和 Ktorm `Database` 管理 |
| `ospf-kotlin-framework-plugin-persistence-redis` | Redis 哨兵客户端管理，支持结构化数据和序列化扩展 |
| `ospf-kotlin-framework-plugin-persistence-expression-ksp` | KSP 编译期处理器，根据 `@PredicateEntity` 注解生成 `PredicateSchema` |

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

## 表达式翻译架构

每个关系型/文档型后端都提供相同的四个翻译组件：

| 组件 | Ktorm | MyBatis-Plus | MongoDB |
| --- | --- | --- | --- |
| 布尔表达式 | `KtormBooleanTranslator` | `MybatisBooleanTranslator` | `MongoBooleanTranslator` |
| 标量表达式 | `KtormScalarTranslator` | `MybatisScalarTranslator` | `MongoScalarTranslator` |
| 排序 | `KtormOrderByTranslator` | `MybatisOrderByTranslator` | `MongoOrderByTranslator` |
| 更新 | `KtormUpdateTranslator` | `MybatisUpdateTranslator` | `MongoUpdateTranslator` |

所有布尔翻译器支持：

- `Comparison`（eq, ne, lt, le, gt, ge）含常量反转
- `InExpression`（in / not-in）
- `PatternMatch`（exact, prefix, suffix, contains, like, regex）
- `NullCheck`（is-null / is-not-null）
- `AndExpression`、`OrExpression`、`NotExpression`
- `UnsupportedPredicatePolicy`（`FailFast`、`AlwaysFalse`、`ClientFilter`）

所有标量翻译器支持：

- 列引用和常量
- 一元运算符（negate, positive）
- 二元运算符（add, subtract, multiply, divide, modulo）
- 函数（ABS, LOWER, UPPER, TRIM, LENGTH, COALESCE）

## 仓储基类

| 后端 | 基类 | 键类型 |
| --- | --- | --- |
| Ktorm | `KtormRepository<E>` | `KtormColumnResolver` |
| MyBatis-Plus | `MybatisRepository<E, M>` | `MybatisColumnNameResolver` |
| MongoDB | `MongoRepository<E>` | `MongoFieldNameResolver` |

每个仓储实现 `ExpressionRepository<E>`，提供 `find`、`count`、`update`、`delete` 操作，由 `math.symbol.expression.BooleanExpression` 驱动。

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

## KSP 谓词 Schema 生成器

`PredicateSchemaProcessor` 是一个 KSP `SymbolProcessor`，它：

1. 扫描带有 `@PredicateEntity` 注解的类。
2. 生成伴随 `PredicateSchema` 对象，包含类型化的字段引用。
3. 可选地生成 `resolver: (String) -> String?` lambda 用于后端字段名映射。

使用方式：

```kotlin
@PredicateEntity
data class MaterialPO(
    val code: String,
    @PredicateField(name = "width_val") val widthValue: Double
)

// 生成结果：
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

## 模式匹配策略（Ktorm）

Ktorm 布尔翻译器接受 `PatternMatchPolicy` 参数以处理不同数据库的 LIKE/ILIKE/REGEX 方言差异：

| 策略 | LIKE | 正则 |
| --- | --- | --- |
| `DefaultPatternMatchPolicy` | `column.like(pattern)` | 不支持 |
| `SqlitePatternMatchPolicy` | `column.like(pattern)` | 不支持 |
| `PostgresPatternMatchPolicy` | `column.like(pattern)` | 不支持 |
| `MySqlPatternMatchPolicy` | `column.like(pattern)` | 不支持 |

MongoDB 对所有模式使用原生 `$regex`；MyBatis-Plus 使用 `LIKE`/`NOT LIKE` 配合 SQL 通配符。
