# ospf-kotlin-framework-plugin-persistence-mybatis

:us: [English](README.md) | :cn: 简体中文

基于 MyBatis-Plus 的关系型持久化插件。

## 公开 API

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `MybatisColumnNameResolver` | typealias | `PersistenceFieldResolver<String>` |
| `MybatisRepository<E, M>` | abstract class | 基于 MyBatis-Plus 实现 `ExpressionRepository<E>` 的仓储基类 |
| `MybatisDialect` | interface | 数据库分页、标识符引用、NULL 排序和标量函数规则 |
| `MybatisBackendFactory` | object | 围绕外部管理的 `DataSource` 创建线程安全的 MyBatis-Plus `SqlSessionFactory` |
| `MybatisBackend` | class | 使用配置的 SQL 方言打开 mapper 会话 |
| `MybatisSession<E, M>` | class | 提供会话作用域 mapper，以及显式的 `commit`、`rollback`、`close` 结果 |
| `MybatisDialectFailure` / `MybatisSqlClause` | data class | 结构化方言错误和生成的 SQL 子句 |
| `MybatisBooleanTranslator<T>` | class | `BooleanExpression` → `QueryWrapper`/`UpdateWrapper` 条件 |
| `MybatisScalarSql` | data class | 参数化 SQL 片段，含 `sql`、`params`、`isColumnOnly` |
| `MybatisScalarTranslator` | class | `ScalarExpression<*>` → 参数化 SQL 片段 |
| `MybatisOrderByTranslator<T>` | class | `SortBy` → `QueryWrapper` ORDER BY |
| `MybatisUpdateTranslator<T>` | class | `UpdateAssignments` → `UpdateWrapper` SET |
| `MybatisValueConverter` | object | 将 OSPF 自定义类型转换为 JDBC 兼容类型 |
| `MybatisColumnBinder` | class | 强类型列绑定器，将属性 path 映射为 MyBatis 列名 |
| `asMybatisResolver` | extension | 将 `MybatisColumnBinder` 转为 `MybatisColumnNameResolver` |
| `HasColumnMapping.mybatisResolver()` | extension | 从 KSP 生成的 `HasColumnMapping` schema 创建 `MybatisColumnNameResolver` |
| `mybatisResolver(columnMapping)` | function | 从显式属性到列名映射创建 `MybatisColumnNameResolver` |

## 快速开始

```kotlin
class OrderRepository(
    mapper: OrderMapper
) : MybatisRepository<Order, OrderMapper>(
    mapper = mapper,
    resolveColumnName = MybatisRepository.simpleColumnResolver()
) {
    // 无需 mapToEntity；MyBatis-Plus 自动处理
}

// 查询
val results = repository.find(
    where = OrderPO::code eq "ORD-001"
)

// 计数
val total = repository.count(where)

// 更新
repository.update(where, assignments)

// 删除
repository.delete(where)
```

## 仓储结果

仓储操作返回 `Ret<T>`：`find` 返回 `Ret<List<E>>`，`count` 返回 `Ret<Long>`，`exists` 返回 `Ret<Boolean>`，`update`/`delete` 返回 `Ret<Int>`。
请显式处理 `Ok`、`Failed` 和 `Fatal`；转换成功值时也可以使用 `map`，失败会保留在结果中。不支持谓词的默认策略为 `FailFast`。
`AlwaysFalse` 需要显式启用，谓词包含不支持节点时整次操作返回空结果或零。`ClientFilter` 会返回结构化失败。

## 数据库方言与会话

保留旧的 `MybatisRepository(mapper, resolver, ...)` 构造方式，供外部 mapper 使用。数据库已知时，请把方言作为最后一个参数传入。`MybatisDialect.SQLite`、`.MySQL`、`.PostgreSQL` 和 `.H2` 分别提供匹配的分页及 NULL 排序规则。默认的 `Portable` 方言供旧调用方使用；在指定具体方言前，只有 OFFSET 的分页和 `LENGTH` 会返回结构化失败。

resolver 的值是数据库中的物理列名，应保留其实际大小写。H2 默认模式下，未引用 DDL 标识符会存为大写，因此显式 H2 resolver 应将 `displayName` 映射为 `DISPLAY_NAME`。若 H2 列是用引号创建且区分大小写，请映射其精确名称，例如 `display_name`。

数据库插件可使用自身连接池中的 `DataSource` 创建共享 backend。backend 不拥有连接池，连接池由数据库插件关闭。session 只管理自身的 `SqlSession`，mapper 操作应在 session 生命周期内完成：

```kotlin
val backend = MybatisBackendFactory.create(dataSource, MybatisDialect.PostgreSQL)
val session = backend.valueOrFail().openSession<User, UserMapper>(UserMapper::class.java).valueOrFail()
val mapper = session.mapper().valueOrFail()
val repository = UserRepository(mapper, MybatisDialect.PostgreSQL)
repository.update(where, assignments).valueOrFail()
session.commit().valueOrFail()
session.close().valueOrFail()
```

session 默认 `autoCommit = false`，请显式调用 `commit()` 或 `rollback()` 并处理返回的 `Try`。关闭后调用 `mapper()`、`commit()` 或 `rollback()` 会返回结构化失败。

内置方言在 MySQL、PostgreSQL 和 H2 中使用 `CHAR_LENGTH`，在 SQLite 中使用 `LENGTH`。Portable 方言不会猜测这两类函数的语义差异。未知函数、参数数量错误、非法标识符或不支持的分页会在执行 mapper 前返回结构化失败。

## 强类型列绑定

`MybatisColumnBinder` 将 AST path 映射到后端列名。配合 KSP 生成的 `HasColumnMapping`，无需手写字符串映射即可把属性名解析为物理列：

```kotlin
import fuookami.ospf.kotlin.framework.persistence.expression.ColumnNamingStrategy
import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity
import fuookami.ospf.kotlin.framework.persistence.expression.mybatisResolver

@PredicateEntity(
    schemaName = "UserSchema",
    generateColumnMapping = true,
    namingStrategy = ColumnNamingStrategy.SnakeCase
)
data class User(val id: Long, val userName: String, val status: String)

// UserSchema（KSP 生成）实现 HasColumnMapping
val resolver: MybatisColumnNameResolver = UserSchema.mybatisResolver()

class UserRepository(mapper: UserMapper) : MybatisRepository<User, UserMapper>(
    mapper = mapper,
    resolveColumnName = UserSchema.mybatisResolver()
)

// 谓词使用 schema 属性名，resolver 映射到列名
val where = UserSchema.predicate { (status eq "active") and (userName like "%test%") }
val users = repository.find(where)
```

`columnMapping` 无对应项时，`MybatisColumnBinder` 回退到驼峰转蛇形（camelCase → snake_case），符合 MyBatis 常见约定。

`ColumnBinder<C>.toResolver()` 可将任意 binder 转为 `PersistenceFieldResolver<C>`，`MybatisColumnBinder.asMybatisResolver()` 返回专用的 `MybatisColumnNameResolver`。

## MybatisScalarSql

`MybatisScalarTranslator` 生成 `MybatisScalarSql` 片段，携带参数化 SQL 和位置占位符（`{0}`、`{1}`、…）。在二元表达式中合并左右片段时，`shifted(offset)` 对占位符重新编号，确保多个片段可以安全合并。

## 值类型转换

`MybatisValueConverter` 在将参数传递给 MyBatis-Plus Wrapper 之前，自动将 OSPF 自定义类型转换为 JDBC 兼容类型。三个翻译器（`MybatisScalarTranslator`、`MybatisBooleanTranslator`、`MybatisUpdateTranslator`）内部均使用此转换器。

| OSPF Kotlin 类型 | 转换后 JVM 类型 | SQL 类型 | JDBC 类型（`java.sql.Types`） | 备注 |
| --- | --- | --- | --- | --- |
| `UInt32` | `Int` | `INT` | `INTEGER` (4) | |
| `Int32` | `Int` | `INT` | `INTEGER` (4) | |
| `UInt64` | `Long` | `BIGINT` | `BIGINT` (-5) | |
| `Int64` | `Long` | `BIGINT` | `BIGINT` (-5) | |
| `Flt32` | `Float` | `FLOAT` | `FLOAT` (6) | |
| `Flt64` | `Double` | `DOUBLE` | `DOUBLE` (8) | |
| `FltX` | `BigDecimal` | `DECIMAL` | `DECIMAL` (3) | |
| `kotlinx.datetime.LocalDateTime` | `java.time.LocalDateTime` | `DATETIME` | `TIMESTAMP` (93) | |
| `kotlin.time.Duration` | `String` | `VARCHAR` | `VARCHAR` (12) | 以 ISO-8601 字符串存储（`Duration.toIsoString`，如 `PT1H30M`） |
| `java.time.ZoneId` | `String` | `VARCHAR` | `VARCHAR` (12) | 以 IANA 时区标识存储（如 `America/New_York`） |
| `java.time.ZoneOffset` | `String` | `VARCHAR` | `VARCHAR` (12) | 以偏移量标识存储（如 `+08:00`） |
| `kotlinx.datetime.TimeZone` | `String` | `VARCHAR` | `VARCHAR` (12) | 以 IANA 时区标识存储（如 `Europe/Berlin`） |
| `List<Enum<*>>` | `String` | `VARCHAR` | `VARCHAR` (12) | 以逗号分隔的枚举名称存储（如 `A,B,C`） |

也可以直接使用转换器处理自定义场景：

```kotlin
val jdbcValue = MybatisValueConverter.convert(Flt64(3.14))  // 返回 3.14 (Double)
val jdbcValue = MybatisValueConverter.convert(UInt32(42u))   // 返回 42 (Int)
```
