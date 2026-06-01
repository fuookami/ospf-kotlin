# ospf-kotlin-math/symbol/expression / 表达式模块

:us: [English](README.md) | :cn: 简体中文

通用表达式 AST，用于 SQL 风格的布尔和标量表达式。提供与 `symbol.parse` 多项式/不等式解析器并行的新系统，增强逻辑运算能力。

## 架构概览

### 新表达式系统

| 组件 | 用途 |
|------|------|
| `PropertyPath` | 统一的路径抽象，用于字段/属性引用（`a.b.c`） |
| `PathSymbol` | `PropertyPath` 与 `Symbol` 接口之间的适配器 |
| `ScalarExpression` | 标量值 AST（Constant、Reference、Unary、Binary、Function） |
| `BooleanExpression` | 布尔逻辑 AST（And、Or、Not、Comparison、In、PatternMatch、NullCheck） |
| `ExpressionOperator` | 操作符定义（一元、二元、比较、模式匹配） |

### 包结构

```
expression/
├── PropertyPath.kt       # 路径抽象
├── PathSymbol.kt         # 路径-符号适配器
├── ScalarExpression.kt   # 标量表达式 AST + ScalarExpressionFactory
├── BooleanExpression.kt  # 布尔表达式 AST + BooleanExpressionFactory
├── ExpressionOperator.kt # 操作符定义
├── dsl/                  # DSL 构造
├── parser/               # 词法分析器和解析器
├── serde/                # JSON 序列化
└── operation/            # 规范化、求值
```

## 与多项式/不等式解析器的关系

`expression` 包处理 SQL 风格的布尔和标量表达式，而 `symbol.parse` 包处理多项式和不等式解析：

1. **`symbol.parse`** - 直接多项式/不等式解析（线性、二次、规范型）
2. **`BooleanExpression`** - 完整布尔逻辑，支持 SQL 表达式

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

### 旧版 AST

旧版 `Expr` AST 和 `LegacyExprBridge` 已移除。SQL 风格表达式请直接使用 `BooleanExpression`，多项式/不等式解析请使用 `symbol.parse` 函数。

## 与多项式/不等式解析器的主要差异

| 功能 | `symbol.parse` | `expression` |
|------|----------------|--------------|
| 侧重点 | 多项式/不等式 | SQL 布尔/标量 |
| 布尔逻辑 | 无 | 完整 `and/or/not` 支持 |
| 空值处理 | 无 | `isNull/isNotNull` 操作符 |
| 模式匹配 | 无 | 带模式的 `PatternMatch` |
| In 操作符 | 无 | `In` 集合成员判断 |
| 路径引用 | 基于符号 | `PropertyPath` 分段支持 |
| 序列化 | 直接 DTO | `kotlinx.serialization` JSON |
| 规范化 | 无 | 扁平化、常量折叠、德摩根 |

## 相关链接

- [symbol/README_ch.md](../README_ch.md) - 主符号模块文档
- [symbol/parse/](../parse/) - 多项式/不等式解析器
