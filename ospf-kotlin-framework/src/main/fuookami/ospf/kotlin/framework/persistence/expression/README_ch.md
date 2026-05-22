# ospf-kotlin-framework/persistence/expression / 表达式模块

[English Documentation (README.md)](./README.md)

SQL 表达式框架层接口与模型实现。

## 架构

### 核心组件

| 组件 | 用途 |
|------|------|
| `ExpressionRepository` | 统一的表达式仓储 CRUD 接口 |
| `SortBy` | 多字段排序模型，支持空值顺序 |
| `UpdateAssignments` | UPDATE SET 语句模型（`SetValue`/`SetNull`/`SetFromExpression`） |
| `NullsOrderSupport` | NULLS FIRST/LAST 能力模型 |

### 插件翻译器

仓储实现与 translator 位于插件模块：

- Ktorm 插件：
  `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm`
- MyBatis-Plus 插件：
  `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis`
- MongoDB 插件：
  `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb`

## 使用示例

### 构造查询/更新模型

```kotlin
val where = path("status").eq("active") and path("age").gt(18)
val sortBy = SortBy.desc("createdAt")
val assignments = UpdateAssignments
    .set("status", "inactive")
    .thenSetNull("deletedAt")
```

### Resolver 函数（当前映射机制）

```kotlin
// Ktorm
val ktormResolver: KtormColumnResolver = { path -> /* resolve to ColumnDeclaring<*> */ null }

// MyBatis-Plus
val mybatisResolver: MybatisColumnNameResolver = { path -> path.substringAfterLast(".") }

// MongoDB
val mongoResolver: MongoFieldNameResolver = { path -> path.substringAfterLast(".") }
```

### Repository API

```kotlin
val users = repository.find(where, sortBy = sortBy, limit = 10, offset = 0)
val total = repository.count(where)
val updated = repository.update(where, assignments)
val deleted = repository.delete(where)
val exists = repository.exists(where)
```

## 依赖

- `ospf-kotlin-math`：`BooleanExpression`、`ScalarExpression`、`PropertyPath`

## 相关链接

- [math/symbol/expression](../../../math/src/main/fuookami/ospf/kotlin/math/symbol/expression/README_ch.md) - 核心表达式 AST
- [sql_expression.md](../../../../../../../../sql_expression.md) - 当前状态与执行清单
