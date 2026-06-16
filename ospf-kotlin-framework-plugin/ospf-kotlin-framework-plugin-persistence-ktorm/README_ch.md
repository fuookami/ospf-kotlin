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
| `KtormOrderByTranslator` | class | `SortBy` → `OrderByExpression` |
| `KtormUpdateTranslator` | class | `UpdateAssignments` → Ktorm UPDATE |
| `PatternMatchPolicy` | interface | LIKE/ILIKE/REGEX 方言策略 |
| `DefaultPatternMatchPolicy` | object | 标准 SQL LIKE |
| `SqlitePatternMatchPolicy` | object | SQLite LIKE |
| `PostgresPatternMatchPolicy` | object | PostgreSQL LIKE |
| `MySqlPatternMatchPolicy` | object | MySQL LIKE |

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
| `fltx` | `FltX` | `DECIMAL` | `DECIMAL` (3) | 默认精度 scale = 2 |
| `fltx` | `FltX` | `DECIMAL` | `DECIMAL` (3) | 自定义舍入模式 |
| `kotlinDatetime` | `kotlinx.datetime.LocalDateTime` | `DATETIME` | `TIMESTAMP` (93) | |
| `instant` | `kotlin.time.Instant` | `TIMESTAMP` | `TIMESTAMP` (93) | |
| `duration` | `kotlin.time.Duration` | `VARCHAR` | `VARCHAR` (12) | 以 ISO-8601 字符串存储（`Duration.toIsoString`） |
| `zoneId` | `java.time.ZoneId` | `VARCHAR` | `VARCHAR` (12) | 以 IANA 时区标识存储（如 `America/New_York`） |
| `zoneOffset` | `java.time.ZoneOffset` | `VARCHAR` | `VARCHAR` (12) | 以偏移量标识存储（如 `+08:00`） |
| `kotlinTimeZone` | `kotlinx.datetime.TimeZone` | `VARCHAR` | `VARCHAR` (12) | 以 IANA 时区标识存储（如 `Europe/Berlin`） |
| `enums<T>` | `List<T : Enum<*>>` | `VARCHAR` | `VARCHAR` (12) | 以逗号分隔的枚举名称存储（如 `A,B,C`） |
