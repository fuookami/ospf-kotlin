# ospf-kotlin-framework-plugin-persistence-ktorm

:us: [English](README.md) | :cn: 简体中文

基于 Ktorm 的关系型持久化插件。

## 公开 API

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `KtormColumnResolver` | typealias | `PersistenceFieldResolver<ColumnDeclaring<*>>` |
| `ColumnNameResolver` | typealias | `(String) -> String?` |
| `KtormRepository<E>` | abstract class | 基于 Ktorm 实现 `ExpressionRepository<E>` 的仓储基类 |
| `KtormBooleanTranslator` | class | `BooleanExpression` → `ColumnDeclaring<Boolean>` |
| `KtormScalarTranslator` | class | `ScalarExpression<*>` → Ktorm `ScalarExpression<*>` |
| `KtormScalarBinding` | data class | 显式声明常量值与目标 `SqlType` 的绑定结果 |
| `KtormTargetConstantBinder` | typealias | 为自定义或转换 `SqlType` 提供目标类型感知的常量绑定 |
| `KtormOrderByTranslator` | class | `SortBy` → `OrderByExpression` |
| `KtormUpdateTranslator` | class | `UpdateAssignments` → Ktorm UPDATE |
| `PatternMatchPolicy` | interface | LIKE/ILIKE/REGEX 方言策略 |
| `DefaultPatternMatchPolicy` | object | 标准 SQL LIKE |
| `SqlitePatternMatchPolicy` | object | SQLite LIKE |
| `PostgresPatternMatchPolicy` | object | PostgreSQL LIKE |
| `MySqlPatternMatchPolicy` | object | MySQL LIKE |
| `KtormColumnBinder<T>` | class | 基于 Ktorm `Table` 的强类型列绑定器，实现 `ColumnBinder<ColumnDeclaring<*>>` |
| `asKtormResolver` | extension | 将 `KtormColumnBinder` 转为 `KtormColumnResolver` |
| `HasColumnMapping.ktormResolver(table)` | extension | 从 KSP 生成的 `HasColumnMapping` schema + Ktorm 表构建 `KtormColumnResolver` |
| `ktormResolver(table, columnMapping)` | function | 从 Ktorm 表 + 显式映射构建 `KtormColumnResolver` |
| `ResolveColumnBuilder` / `resolveColumn` | 类 / 函数 | 为一个或多个查询数据源注册限定列 |
| `resolveColumnWithDiagnostics` | 函数 | 构建可区分缺失、歧义和非法映射的解析器 |
| `KtormRelationalQueryCompiler` | 类 | 将数据源白名单内的关系查询计划编译为 Ktorm 查询 |

## 快速开始

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

## 强类型列绑定

`KtormColumnBinder` 提供基于 Ktorm 表的强类型列绑定能力，把属性路径映射到 Ktorm `ColumnDeclaring<*>`。配合 KSP 生成的 `HasColumnMapping`，可以避免手写字符串映射：

```kotlin
import fuookami.ospf.kotlin.framework.persistence.expression.ktormResolver

// KSP 生成：object UserSchema : PredicateSchema<User>(), HasColumnMapping { ... }
val resolver: KtormColumnResolver = UserSchema.ktormResolver(UsersTable)

class UserRepository(database: Database) : KtormRepository<User>(
    database = database,
    table = UsersTable,
    resolveColumn = UserSchema.ktormResolver(UsersTable)
) {
    override fun mapToEntity(row: QueryRowSet): User? = TODO()
}

// 强类型谓词 + 列绑定 resolver 一起使用
val where = UserSchema.predicate { (status eq "active") and (age gt 18) }
val users = repository.find(where)
```

也可以显式传入映射，或从 `KtormColumnBinder` 直接转 resolver：

```kotlin
val explicitResolver = ktormResolver(UsersTable, mapOf("status" to "user_status"))
val binderResolver = KtormColumnBinder(UsersTable, mapping).asKtormResolver()
```

无 `columnMapping` 时回退到属性名原值，再在表列中查找匹配项。

## 限定字段与版本化表映射

查询涉及多个已注册数据源时使用 `resolveColumn`。`mainTable` 和 `versionTable` 是主表 + 版本表场景的便捷作用域：

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

限定路径只从显式注册的数据源或别名解析。未限定字段只有在候选唯一时才可解析；短字段名冲突时，诊断解析器返回 `PersistenceFieldResolution.Ambiguous`，可空解析器返回 `null`。未知别名不会根据路径最后一段字段名推断。`KtormRelationalQueryCompiler` 强制要求每个 `KtormQuerySource` 提供带诊断的显式字段解析器和用于隐式根投影/`EXISTS` 投影的 `defaultColumns` 白名单，不会按 `table.columns` 自动放行物理列；未注册字段必须结构化拒绝。

标量常量与已解析列比较时，`KtormScalarTranslator` 使用目标列的 `SqlType`。转换列的值会保留为领域值，直到 Ktorm 在真实 JDBC 执行阶段调用列转换函数，因此 Repository 不需要另维护值对象解包注册表。没有目标列上下文时，默认绑定包括 `UInt64` 到 `Long`、`FltX` 到 `BigDecimal`；自定义或转换 `SqlType` 必须通过 `targetConstantBinder` 显式声明目标类型兼容的绑定。编译阶段不会调用伪造的 `PreparedStatement`，未注册、目标类型不兼容或绑定器普通异常的值都会按参数绑定错误结构化拒绝；取消异常和 `Error` 会继续传播。

## 关系查询计划

`KtormRelationalQueryCompiler` 消费 framework 提供的不可变 `RelationalQueryPlan`。适配器负责维护
`KtormQuerySource` 白名单和列映射；调用方不能提供表名、列名或 SQL 字符串。

计划支持 `Inner`、`Left` 和 `Exists` Join、限定数据源别名、列对列谓词、投影、`DISTINCT`、分组、NULL
排序、分页以及单列根键粒度计数。Join 条件必须把当前数据源与已绑定数据源通过列引用关联，不能使用常量或包含布尔常量；`Exists` 只接受一对多/多对多基数声明，并编译为相关半连接，不会因一对多明细放大根记录。`compileCount` 只接受属于根数据源的单列根键；关联数据源列和复合根键都会结构化拒绝。

计划构造时会防御性复制外层集合、表达式容器和框架已知的可变 payload 容器；`List`、`Map`、`Set` 与数组 payload 会递归快照。无法安全复制的不透明自定义 payload 或循环容器会在计划的 `validate()` 门禁中以结构化失败拒绝，绝不会被替换为伪快照。`canonical()`/`canonicalHash()` 描述规范化后的表达式形状以及类型/容器结构，并省略标量和自定义 payload 的字面量原值。

未显式提供投影时，适配器只选择数据源 `defaultColumns` 白名单中的字段；该列表仍通过同一带诊断字段映射解析，不能借此引入未注册物理列。

编译器统一返回 `Ret`，并将数据源、字段、谓词、SQL 生成和数据库失败转换为结构化
`RelationalQueryFailure`。`QueryAuditSummary` 只包含生成的 SQL 模板、方言名称和参数 SQL 类型名，不包含
绑定参数原值。执行统计提供耗时和返回行数；传入正数 `maxReturnedRows` 后，执行器会探测是否仍有下一行并设置 `truncated`，数据库无法可靠提供扫描行数时保持不可用。

当前实现依赖 Ktorm 对 Join、分组、排序和分页的方言支持。本模块提供 SQLite 真实执行测试；PostgreSQL 和
MySQL 复用相同的 Ktorm 表达式路径，但启用前仍需由消费方适配器完成对应方言能力验证。

## PatternMatchPolicy

向 `KtormRepository` 传入 `PatternMatchPolicy` 以控制 `PatternMatch` 表达式的翻译方式。默认使用标准 SQL `LIKE`。自定义实现可覆盖 `translateLike` 和 `translateRegex` 以适配特定数据库方言。

## 列类型扩展

`SqlType.kt` 为 `BaseTable<*>` 提供了一系列扩展函数，用于声明由 OSPF 自定义类型支撑的列。每个函数定义了 Kotlin 类型与 JDBC/SQL 类型之间的双向转换。

| 函数 | Kotlin 类型 | SQL 类型 | JDBC 类型（`java.sql.Types`） | 备注 |
| --- | --- | --- | --- | --- |
| `ui32` | `UInt32` | `INT` | `INTEGER` (4) | |
| `i32` | `Int32` | `INT` | `INTEGER` (4) | |
| `ui64` | `UInt64` | `BIGINT` | `BIGINT` (-5) | |
| `i64` | `Int64` | `BIGINT` | `BIGINT` (-5) | |
| `f32` | `Flt32` | `FLOAT` | `FLOAT` (6) | |
| `f64` | `Flt64` | `DOUBLE` | `DOUBLE` (8) | |
| `fltx` | `FltX` | `DECIMAL` | `DECIMAL` (3) | 默认精度 scale = 18 |
| `fltx` | `FltX` | `DECIMAL` | `DECIMAL` (3) | 自定义舍入模式 |
| `kotlinDatetime` | `kotlinx.datetime.LocalDateTime` | `DATETIME` | `TIMESTAMP` (93) | |
| `instant` | `kotlin.time.Instant` | `TIMESTAMP` | `TIMESTAMP` (93) | |
| `duration` | `kotlin.time.Duration` | `VARCHAR` | `VARCHAR` (12) | 以 ISO-8601 字符串存储（`Duration.toIsoString`） |
| `durationLong` | `kotlin.time.Duration` | `BIGINT` | `BIGINT` (-5) | 以长整数存储；单位默认毫秒，可通过 `DurationUnit` 参数指定 |
| `durationMs` | `kotlin.time.Duration` | `BIGINT` | `BIGINT` (-5) | 以截断后的整毫秒存储；非有限或不可表示的值会被拒绝 |
| `zoneId` | `java.time.ZoneId` | `VARCHAR` | `VARCHAR` (12) | 以 IANA 时区标识存储（如 `America/New_York`） |
| `zoneOffset` | `java.time.ZoneOffset` | `VARCHAR` | `VARCHAR` (12) | 以偏移量标识存储（如 `+08:00`） |
| `kotlinTimeZone` | `kotlinx.datetime.TimeZone` | `VARCHAR` | `VARCHAR` (12) | 以 IANA 时区标识存储（如 `Europe/Berlin`） |
| `enums<T>` | `List<T : Enum<*>>` | `VARCHAR` | `VARCHAR` (12) | 以逗号分隔的枚举名称存储（如 `A,B,C`） |
