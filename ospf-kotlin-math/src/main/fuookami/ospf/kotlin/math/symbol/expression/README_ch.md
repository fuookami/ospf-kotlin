# ospf-kotlin-math/symbol/expression / 表达式模块

[English Documentation (README.md)](./README.md)

通用表达式 AST，用于 SQL 风格的布尔和标量表达式。提供与旧版 `symbol.parser.Expr` 并行的新系统，增强逻辑运算能力。

## 架构概览

### 新表达式系统

| 组件 | 用途 |
|------|------|
| `PropertyPath` | 统一的路径抽象，用于字段/属性引用（`a.b.c`） |
| `PathSymbol` | `PropertyPath` 与 `Symbol` 接口之间的桥接 |
| `ScalarExpression` | 标量值 AST（Constant、Reference、Unary、Binary、Function） |
| `BooleanExpression` | 布尔逻辑 AST（And、Or、Not、Comparison、In、PatternMatch、NullCheck） |
| `ExpressionOperator` | 操作符定义（一元、二元、比较、模式匹配） |

### 包结构

```
expression/
├── PropertyPath.kt       # 路径抽象
├── PathSymbol.kt         # 路径-符号桥接
├── ScalarExpression.kt   # 标量表达式 AST
├── BooleanExpression.kt  # 布尔表达式 AST
├── ExpressionOperator.kt # 操作符定义
├── ExpressionFactory.kt  # 工厂方法
├── dsl/                  # DSL 构造
├── parser/               # 词法分析器和解析器
├── serde/                # JSON 序列化
└── operation/            # 规范化、求值
```

## 并存迁移策略

### 与旧版 `Expr` 并存

新 `expression` 包与旧版 `symbol.parser.Expr` 并存：

1. **旧版 `Expr`** - 保留向后兼容，服务 polynomial/inequality 场景
2. **新版 `BooleanExpression`** - 增强逻辑运算，支持 SQL 表达式
3. **桥接层** - `adapter/LegacyExprBridge` 提供系统间转换

### 迁移路径

```kotlin
// 旧版 (parser.Expr) - 算术和比较为主
val legacyExpr = Expr.parse("x > 5")

// 新版 (BooleanExpression) - 完整布尔逻辑支持
val newExpr = BooleanExpression.parse("x > 5 and y is not null")

// 桥接转换 (Phase M4)
val converted = LegacyExprBridge.toBooleanExpression(legacyExpr)
```

### 迁移时间线

| 阶段 | 状态 | 描述 |
|------|------|------|
| M0 | 已完成 | 脚手架与包结构 |
| M1 | 已完成 | 核心 AST（PropertyPath, ScalarExpression, BooleanExpression） |
| M2 | 已完成 | DSL 与解析器 |
| M3 | 已完成 | 序列化、规范化、求值 |
| M4 | 已完成 | 旧版桥接和兼容层 |
| M5 | 进行中 | 文档 |

## 使用示例

### 使用 DSL 创建表达式

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.dsl.*

// 布尔表达式: (a > 5 and b is not null)
val expr = path("a").gt(5) and path("b").isNotNull()

// 使用解析器
val parsed = parseBooleanExpression("a > 5 and b is not null")
```

### 表达式规范化

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.operation.*

// And(A, true, And(A)) -> A (规范化后)
val normalized = normalize(complexExpr)
```

### 本地求值

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.operation.*

val expr = Comparison(ComparisonOperator.Gt, 
    ScalarReference(PropertyPath.parse("age")), 
    ScalarConstant(18))

val result = expr.evaluateWith(mapOf("age" to 25))
// result: Trivalent.True
```

### JSON 序列化

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.serde.*

val json = expr.toJsonString()
val restored = booleanExpressionFromJson(json)
```

### 旧版桥接

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.adapter.*

// 将旧版 Expr 转换为新版 BooleanExpression
val legacyExpr: Expr = Expr.Comparison(...)
val newExpr = legacyExpr.toBooleanExpressionOrNull()

// 将新表达式转换回旧版
val backToLegacy = newExpr?.toLegacyExprOrNull()
```

## 与旧版 Expr 的主要差异

| 功能 | 旧版 `Expr` | 新版 `Expression` |
|------|-------------|-------------------|
| 布尔逻辑 | 有限 | 完整 `and/or/not` 支持 |
| 空值处理 | 无 | `isNull/isNotNull` 操作符 |
| 模式匹配 | 无 | 带模式的 `PatternMatch` |
| In 操作符 | 无 | `In` 集合成员判断 |
| 路径引用 | 基础 | `PropertyPath` 分段支持 |
| 序列化 | 自定义 | `kotlinx.serialization` JSON |
| 规范化 | 无 | 扁平化、常量折叠、德摩根 |

## 相关链接

- [symbol/README_ch.md](../README_ch.md) - 主符号模块文档
- [symbol/parser/README_ch.md](../parser/README_ch.md) - 旧版解析器文档