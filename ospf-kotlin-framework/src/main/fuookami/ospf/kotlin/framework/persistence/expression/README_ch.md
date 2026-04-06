# ospf-kotlin-framework/persistence/expression / 表达式模块

[English Documentation (README.md)](./README.md)

SQL 表达式框架层实现，用于 Ktorm 集成。

## 架构

### 核心组件

| 组件 | 用途 |
|------|------|
| `EntityMeta` | PO 字段到数据库列的映射 |
| `FieldBinding` | 字段绑定定义，支持值转换器 |
| `SortBy` | 多字段排序模型，支持空值顺序 |
| `UpdateAssignment` | UPDATE SET 语句模型 |

### 翻译器

| 翻译器 | 用途 |
|--------|------|
| `KtormBooleanTranslator` | BooleanExpression -> Ktorm WHERE 条件 |
| `KtormOrderByTranslator` | SortBy -> Ktorm ORDER BY 子句 |
| `KtormUpdateTranslator` | UpdateAssignment -> Ktorm UPDATE SET |

### 方言策略

| 策略 | 用途 |
|------|------|
| `PatternMatchPolicy` | LIKE/ILIKE/REGEX 方言处理 |
| `NullsOrderSupport` | NULLS FIRST/LAST 支持检测 |

## 使用示例

### 定义实体元数据

```kotlin
val userMeta = EntityMeta<User>(User::class, "users") {
    binding("id", User::id, Users.id)
    binding("name", User::name, Users.name)
    binding("email", User::email, Users.email)
    binding("status", User::status, Users.status)
}
```

### 使用 BooleanExpression 查询

```kotlin
val where = path("status").eq("active") and path("age").gt(18)
val users = repository.find(where, sortBy = SortBy.desc("createdAt"), limit = 10)
```

### 使用 UpdateAssignment 更新

```kotlin
val assignments = UpdateAssignments.set("status", "inactive") + UpdateAssignments.setNull("deletedAt")
val updated = repository.update(where, assignments)
```

## 依赖

- `ospf-kotlin-math`：BooleanExpression、ScalarExpression、PropertyPath
- `ktorm-core`：Ktorm ORM 框架
- `ktorm-support-sqlite`：SQLite 方言支持

## 相关链接

- [math/symbol/expression](../../../math/src/main/fuookami/ospf/kotlin/math/symbol/expression/README_ch.md) - 核心表达式 AST
- [sql_expression.md](../../../../../../../sql_expression.md) - 设计文档