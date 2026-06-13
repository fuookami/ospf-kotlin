# ospf-kotlin-framework-plugin-persistence-mybatis

:us: [English](README.md) | :cn: 简体中文

基于 MyBatis-Plus 的关系型持久化插件。

## 公开 API

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `MybatisColumnNameResolver` | typealias | `PersistenceFieldResolver<String>` |
| `MybatisRepository<E, M>` | abstract class | 基于 MyBatis-Plus 实现 `ExpressionRepository<E>` 的仓储基类 |
| `MybatisBooleanTranslator<T>` | class | `BooleanExpression` → `QueryWrapper`/`UpdateWrapper` 条件 |
| `MybatisScalarSql` | data class | 参数化 SQL 片段，含 `sql`、`params`、`isColumnOnly` |
| `MybatisScalarTranslator` | class | `ScalarExpression<*>` → 参数化 SQL 片段 |
| `MybatisOrderByTranslator<T>` | class | `SortBy` → `QueryWrapper` ORDER BY |
| `MybatisUpdateTranslator<T>` | class | `UpdateAssignments` → `UpdateWrapper` SET |

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

## MybatisScalarSql

`MybatisScalarTranslator` 生成 `MybatisScalarSql` 片段，携带参数化 SQL 和位置占位符（`{0}`、`{1}`、…）。在二元表达式中合并左右片段时，`shifted(offset)` 对占位符重新编号，确保多个片段可以安全合并。
