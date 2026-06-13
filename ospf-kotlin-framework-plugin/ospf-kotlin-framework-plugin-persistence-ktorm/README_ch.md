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
