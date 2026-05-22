# Framework Plugin 泛型化与边界治理计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-plugin` 主要包含消息与持久化插件：

1. message-kafka
2. persistence-mongodb
3. persistence-mysql
4. persistence-sqlite
5. persistence-redis
6. persistence-ktorm
7. persistence-mybatis

当前插件模块基本不承担优化模型领域数值泛型化。持久化/消息层的主要原则是：**框架不决定领域物理量的落库或消息形态，使用者在 PO/DTO 层负责拆解与还原**。

## 2. 泛型化目标

1. 插件层不强行引入 `V : RealNumber<V>`，除非插件本身处理数值计算。
2. 对 expression repository，保留实体类型 `E`，字段路径解析交给使用者提供的 resolver。
3. 对持久化 translator，只翻译已经映射到 PO 字段/列的表达式，不负责把领域 `Quantity<V>` 自动拆成数据库列。
4. 对消息插件，serializer/payload schema 由使用者 DTO 决定；框架只提供传输能力。

## 3. 物理量化硬规则

插件模块虽然不直接建模坐标、长度、产能，但必须尊重使用者在 PO/DTO 层对 `Quantity<V>` 的拆解：

1. 持久化插件不自动决定 `Quantity<V>` 是单列、双列还是 JSON。
2. 使用者可在 PO 层拆成 `value + unitSymbol`，例如 `widthValue` / `widthUnitSymbol`。
3. Repository 的查询、保存、加载逻辑负责从领域对象 `Quantity<V>` 到 PO 字段的 `from/into` 映射。
4. expression translator 只处理 PO 字段路径，例如查询 `widthValue` 和 `widthUnitSymbol`，不直接理解领域 `width: Quantity<V>`。
5. 消息插件同理不规定 payload；使用者 DTO 负责保留 `value` 和 `unit`。
6. 插件 API 不应新增固定 `Flt64` 的物理量字段。

## 4. Persistence Expression 计划

### Phase P0：字段解析边界统一

当前 Ktorm/MyBatis/Mongo 分别使用 resolver：

- `KtormColumnResolver`
- `MybatisColumnNameResolver`
- `MongoFieldNameResolver`

建议只统一“路径到列/字段”的解析概念，不统一物理量落库结构：

```kotlin
interface PersistenceFieldResolver<C> {
    fun resolve(path: String): C?
}
```

验收：

- [ ] translator 不直接假设领域字段和数据库列一一对应。
- [ ] 使用者可以把领域 `width: Quantity<V>` 映射为 PO 的 `widthValue` / `widthUnitSymbol`。
- [ ] translator 只接收 PO 字段路径，不能自动展开 `Quantity<V>`。

### Phase P0.1：PO/DTO 映射示例文档

参考 `PackagingMaterialPO` 的模式补充文档：

```kotlin
data class PackagingMaterialPO(
    val widthValue: FltX,
    val widthUnitSymbol: UnitSymbol?,
    val tareWeightValue: FltX,
    val tareWeightUnitSymbol: UnitSymbol?
) {
    companion object {
        fun from(domain: PackagingMaterial): PackagingMaterialPO = ...
    }

    fun into(unitResolver: UnitResolver): Ret<PackagingMaterial> = ...
}
```

验收：

- [ ] 文档明确 PO/DTO 是物理量持久化形态的所有者。
- [ ] framework-plugin 不提供默认 `Quantity` 列拆解策略。
- [ ] 示例覆盖 value+unit 两列模式。

### Phase P1：表达式 translator 行为补齐

状态：已在 SQL Expression P0-P9 工作中完成，原专项交接文档已归档删除。

1. Ktorm 已补 `InExpression` 与比较操作。
2. MyBatis 已修复 update where 丢失和分页 `last(...)` 覆盖。
3. MongoDB 已复核并修正 null 语义。
4. unsupported expression 已统一为显式策略，默认恒假或空结果，避免无条件查询。

验收：

- [x] Ktorm/MyBatis/Mongo translator 都有行为测试，而不只是 AST 构造测试。

### Phase P2：消息插件 serializer 策略

Kafka 消息如果传递泛型数值或物理量领域对象，必须由调用方提供 DTO 和 serializer。

验收：

- [ ] 插件 API 不固定 `Flt64`。
- [ ] 文档说明 `Quantity` 的 payload 结构由使用者 DTO 决定。
- [ ] 插件不自动把 `Quantity` 序列化成某种固定结构。

## 5. 门禁

```powershell
git grep -n "Flt64\\|FltX\\|Quantity<\\|Any?" -- ospf-kotlin-framework-plugin
```

说明：

1. `Any?` 在持久化层不可完全避免，但必须集中在 ORM 边界。
2. 插件层不应新增固定 `Flt64` 领域模型。
3. 插件层不应新增默认 `Quantity` 持久化策略；PO/DTO 层负责拆解。
