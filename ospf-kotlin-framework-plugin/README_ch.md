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
| `ospf-kotlin-framework-plugin-persistence-mysql` | MySQL 数据源初始化和 Ktorm / MyBatis-Plus / JdbcClient 管理 |
| `ospf-kotlin-framework-plugin-persistence-postgresql` | PostgreSQL 数据源初始化和 Ktorm / MyBatis-Plus / JdbcClient 管理 |
| `ospf-kotlin-framework-plugin-persistence-h2` | H2 数据源初始化和 Ktorm / MyBatis-Plus / JdbcClient 管理 |
| `ospf-kotlin-framework-plugin-persistence-jdbcclient` | Spring JdbcClient 表达式仓储和关系型 SQL 翻译 |
| `ospf-kotlin-framework-plugin-persistence-sqlite` | SQLite 数据源初始化和 Ktorm / MyBatis-Plus / JdbcClient 管理 |
| `ospf-kotlin-framework-plugin-persistence-redis` | Redis 哨兵客户端管理，支持结构化数据和序列化扩展 |
| `ospf-kotlin-framework-plugin-persistence-expression-ksp` | KSP 编译期处理器，根据 `@PredicateEntity` 注解生成 `PredicateSchema` |

## 关系型数据库适配矩阵

每种关系型适配器都由对应数据库插件提供专用工厂。工厂方法返回 `Ret`，并复用该插件管理的数据源：

| 数据库 | Ktorm | MyBatis-Plus | Spring JdbcClient |
| --- | --- | --- | --- |
| SQLite | `Sqlite.initKtorm` / `Sqlite.getKtormBackend` | `Sqlite.initMybatis` / `Sqlite.getMybatisBackend` | `Sqlite.initJdbcClient` / `Sqlite.getJdbcClient` |
| MySQL | `MySQL.initKtorm` / `MySQL.getKtormBackend` | `MySQL.initMybatis` / `MySQL.getMybatisBackend` | `MySQL.initJdbcClient` / `MySQL.getJdbcClient` |
| PostgreSQL | `PostgreSQL.initKtorm` / `PostgreSQL.getKtormBackend` | `PostgreSQL.initMybatis` / `PostgreSQL.getMybatisBackend` | `PostgreSQL.initJdbcClient` / `PostgreSQL.getJdbcClient` |
| H2 | `H2.initKtorm` / `H2.getKtormBackend` | `H2.initMybatis` / `H2.getMybatisBackend` | `H2.initJdbcClient` / `H2.getJdbcClient` |

Ktorm backend 将 `Database` 与对应的 `RelationalQueryDialect` 组合；使用关系查询编译器时应传入该 backend，明确数据库的分页和 SQL 能力。MyBatis backend 管理 `SqlSessionFactory`；`openSession` 和 `mapper` 返回 `Ret`，而 `commit`、`rollback` 和 session `close` 返回 `Try`。每个短生命周期 session scope 使用完后都应关闭；backend 不负责关闭数据库插件提供的数据源。仍支持使用外部配置的 `BaseMapper` 构造 `MybatisRepository`。JdbcClient backend 将 Spring `JdbcClient` 与 `JdbcClientDialect` 配对，由方言处理标识符引用、分页和空值排序。

连接池由数据库插件统一管理。调用对应插件的 `close(key): Try` 时必须传入该插件的 `*ClientKey` 类型；关闭共享连接池后，通过该 key 创建的 Ktorm、MyBatis 和 JdbcClient backend 也会失效。当前没有按名称关闭的重载。直接传入的 `Database`、`BaseMapper`、`JdbcClient` 或 `DataSource` 仍由调用方管理。为保持兼容，原有可空返回的 `Sqlite.init` 和 `MySQL.init` Ktorm 入口继续保留；统一的 `initKtorm` 工厂返回 `Ret`。

MyBatis 使用选定的 `MybatisDialect` 控制数据库专用 SQL。数据库工厂会选择匹配的方言；通过外部 `BaseMapper` 直接构造仓储时也应指定方言。portable 方言遇到不支持的 SQL 表达式时返回 `Failed`。

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

| 组件 | Ktorm | MyBatis-Plus | MongoDB | JdbcClient |
| --- | --- | --- | --- | --- |
| 布尔表达式 | `KtormBooleanTranslator` | `MybatisBooleanTranslator` | `MongoBooleanTranslator` | `JdbcClientBooleanTranslator` |
| 标量表达式 | `KtormScalarTranslator` | `MybatisScalarTranslator` | `MongoScalarTranslator` | `JdbcClientScalarTranslator` |
| 排序 | `KtormOrderByTranslator` | `MybatisOrderByTranslator` | `MongoOrderByTranslator` | `JdbcClientOrderByTranslator` |
| 更新 | `KtormUpdateTranslator` | `MybatisUpdateTranslator` | `MongoUpdateTranslator` | `JdbcClientUpdateTranslator` |

所有布尔翻译器支持：

- `Comparison`（eq, ne, lt, le, gt, ge）含常量反转
- `InExpression`（in / not-in）
- `PatternMatch`（exact, prefix, suffix, contains, like）
- `NullCheck`（is-null / is-not-null）
- `AndExpression`、`OrExpression`、`NotExpression`
- `UnsupportedPredicatePolicy`（`FailFast`、`AlwaysFalse`、`ClientFilter`）

正则模式匹配依赖具体后端：MongoDB 支持原生 regex；MyBatis-Plus 和 JdbcClient 会按 `UnsupportedPredicatePolicy` 将 regex 作为不支持的表达式处理；Ktorm 委托给 `PatternMatchPolicy`，目前内置的 default、SQLite、PostgreSQL 和 MySQL 策略均不支持 regex。

所有标量翻译器支持：

- 列引用和常量
- 一元运算符（negate, positive）
- 二元运算符（add, subtract, multiply, divide, modulo）
- 函数（ABS, LOWER, UPPER, TRIM, LENGTH, COALESCE）

MyBatis 的 `LENGTH` SQL 会根据所选数据库方言生成。portable 方言遇到该表达式时返回结构化失败。

## 仓储基类

| 后端 | 基类 | 键类型 |
| --- | --- | --- |
| Ktorm | `KtormRepository<E>` | `KtormColumnResolver` |
| MyBatis-Plus | `MybatisRepository<E, M>` | `MybatisColumnNameResolver` |
| MongoDB | `MongoRepository<E>` | `MongoFieldNameResolver` |
| Spring JdbcClient | `JdbcClientRepository<E>` | `JdbcClientColumnNameResolver` |

每个仓储实现 `ExpressionRepository<E>`，提供 `find`、`count`、`update`、`delete` 操作，由 `math.symbol.expression.BooleanExpression` 驱动。

JdbcClient 使用绑定参数 `?`，并由数据库插件提供 `JdbcClientDialect`。四个数据库模块都会提供各自的标识符引用、分页和空值排序规则。

## 数据库集成测试

仓储集成测试会在全部十二种数据库与适配器组合上执行 CRUD。SQLite 测试使用隔离的临时文件，H2 测试使用隔离的内存数据库，无需外部数据库服务。MySQL 和 PostgreSQL 测试会创建唯一命名的临时数据库，并在结束时删除。未设置 `OSPF_TEST_MYSQL_HOST` 或 `OSPF_TEST_POSTGRESQL_HOST` 时，对应真实数据库测试会跳过；端口、凭据、服务端版本要求和命令见数据库模块 README：[MySQL 测试配置](ospf-kotlin-framework-plugin-persistence-mysql/README_ch.md)、[PostgreSQL 测试配置](ospf-kotlin-framework-plugin-persistence-postgresql/README_ch.md)。

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
