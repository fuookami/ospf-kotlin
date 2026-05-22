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
| `UnsupportedPredicatePolicy` | 无法下推谓词的处理策略（`FailFast`/`AlwaysFalse`/`ClientFilter`） |
| `PredicateTranslation` | 面向后续 translator API 的结构化谓词翻译结果 |

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

当插件支持时，标量谓词可以比较列与基础算术表达式：

```kotlin
val columnComparison = path("startAt").lt(path("endAt"))
val amountCheck = (path("price") * path("quantity")).gt(100)
```

标量函数复用 `ospf-kotlin-math` 的 `ScalarFunction` 节点。它们是逻辑函数符号，不是原始 SQL 字符串：

```kotlin
val absoluteBalance = abs(path("balance")).gt(10)
val normalizedName = lower(path("name")).eq("alice")
```

标准函数名集中定义在 `ScalarFunctionNames`。当前内置函数包括 `abs`、`lower`、`upper`、`trim`、`length` 与 `coalesce`。Ktorm、MyBatis-Plus 与 MongoDB translator 只下推已登记标准函数；未知函数继续遵循 `UnsupportedPredicatePolicy`，不会被输出为不受控 SQL 或 Mongo 表达式。

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

Repository 默认使用 `UnsupportedPredicatePolicy.AlwaysFalse`，不支持的谓词会返回空结果，而不会变成未过滤查询。构造 repository 时可选择 `FailFast` 以获得明确异常。`ClientFilter` 为保留策略，在客户端过滤实现前会明确失败。

## 依赖

- `ospf-kotlin-math`：`BooleanExpression`、`ScalarExpression`、`PropertyPath`

## 相关链接

- [math/symbol/expression](../../../../../../../../../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/expression/README_ch.md) - 核心表达式 AST
- [sql_expression.md](../../../../../../../../sql_expression.md) - 当前状态与执行清单
