# ospf-kotlin-math/symbol/expression / 表达式模块

:us: [English](README.md) | :cn: 简体中文

通用表达式 AST，用于 SQL 风格的布尔和标量表达式。提供与 `symbol.parse` 多项式/不等式解析器并行的新系统，增强逻辑运算能力。

## 架构概览

### 新表达式系统

| 组件 | 用途 |
|------|------|
| `PropertyPath` | 统一的路径抽象，用于字段/属性引用（`a.b.c`） |
| `PathSymbol` | `PropertyPath` 与 `Symbol` 接口之间的适配器 |
| `ScalarExpression` | 标量值 AST（Constant、Reference、Unary、Binary、Function、**Conditional**、**Boolean**） |
| `BooleanExpression` | 布尔逻辑 AST（And、Or、Not、Comparison、In、PatternMatch、NullCheck） |
| `ExpressionOperator` | 操作符定义（一元、二元、比较、模式匹配） |
| `ScalarParser` | 将 Aviator 兼容的标量表达式字符串解析为 `ScalarExpression` AST |
| `evaluateScalar` | 公开标量求值器，返回 `Ret<Any?>`，支持函数表注入 |
| `MathFunctionEvaluator` | 17 个 Aviator 白名单 `math.*` 函数 |
| `Lexer` / `LexMode` | 词法分析器，布尔/标量双模式（控制一元负号处理） |

### 包结构

```
expression/
├── PropertyPath.kt       # 路径抽象
├── PathSymbol.kt         # 路径-符号适配器
├── ScalarExpression.kt   # 标量 AST（含 Conditional、Boolean）+ ScalarExpressionFactory
├── BooleanExpression.kt  # 布尔表达式 AST + BooleanExpressionFactory
├── ExpressionOperator.kt # 操作符定义
├── dsl/                  # DSL 构造（ExpressionDsl.kt）
├── parser/               # Lexer（LexMode）、布尔解析器（Parser.kt）、ScalarParser
├── serde/                # JSON 序列化（含 Conditional/Boolean 变体）
└── operation/            # EvaluateScalar、EvaluateBoolean、Normalize、MathFunctions、NumericOps
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

### 标量表达式解析

`ScalarParser` 将 Aviator 兼容的标量表达式字符串解析为 `ScalarExpression` AST。支持算术、比较、逻辑、三元 `? :`、`if/then/else/fi`、`math.*` 函数。

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.parser.*

// 算术
val sum = parseScalarExpression("x + y").value!!
// -> ScalarBinary(Add, Ref(x), Ref(y))

// 条件（三元）
val ternary = parseScalarExpression("x > 0 ? a : b").value!!
// -> ScalarConditional(Comparison(Gt, x, 0), Ref(a), Ref(b))

// if/then/else/fi（条件允许 && / ||，无需外层括号）
val cond = parseScalarExpression("if w > 787 then x else y fi").value!!
// -> ScalarConditional(Comparison(Gt, w, 787), Ref(x), Ref(y))

// 布尔作为标量（公式返回布尔）
val boolAsScalar = parseScalarExpression("x > 0 && y > 0").value!!
// -> ScalarBoolean(AndExpression([...]))

// math.* 函数（前缀剥离：math.sqrt -> "sqrt"）
val fn = parseScalarExpression("math.sqrt(x^2 + y^2)").value!!
// -> ScalarFunction("sqrt", [ScalarBinary(Add, Power(x,2), Power(y,2))])

// 常量在解析期解析
val pi = parseScalarExpression("math.PI").value!!
// -> ScalarConstant(3.14159...)
```

操作符优先级（从高到低）：`^`/`**` > 一元 `+` > `* / %` > `+ -` > 比较 > `&&`/`and` > `||`/`or` > 三元/`if`。

值得注意的行为：
- `-x^2` 解析为 `-(x^2)`（一元负号低于幂运算），与 Python/Excel/Aviator 一致。
- `**` 和 `^` 均映射到 `BinaryOperator.Power`。
- 词法分析器有两种模式：`LexMode.Boolean`（默认）将 `-3` 合并为数字；`LexMode.Scalar` 输出 `MINUS` + `NUMBER`。`parseScalarExpression` 使用标量模式。

### 标量求值

`evaluateScalar` 返回 `Ret<Any?>` 以显式处理错误，支持注入函数求值器：

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.operation.*

val context = MapEvaluationContext.fromStringMap(mapOf("x" to 3.0, "y" to 4.0))

// 基本求值
val r1 = evaluateScalar(parseScalarExpression("x + y").value!!, context).value!!  // 7.0

// 条件求值
val r2 = evaluateScalar(
    parseScalarExpression("if x > 0 then x else y fi").value!!,
    context
).value!!  // 3.0

// 使用 math.* 函数（注入 MathFunctionEvaluator）
val r3 = evaluateScalar(
    parseScalarExpression("math.pow(x, 2) + math.pow(y, 2)").value!!,
    context,
    MathFunctionEvaluator
).value!!  // 25.0

// 便捷扩展函数
val r4 = parseScalarExpression("x * 2").value!!
    .evaluateWith(mapOf("x" to 3.0)).value!!  // 6.0
```

错误语义：除零、未知函数、类型不匹配、未绑定符号引用均返回 `Failed`。`ScalarSymbolReference` 和 `ScalarCustom` 返回 `Failed`（不透明）。布尔参与算术（如 `(x > 0) + 1`）返回 `Failed`--核心求值器不做布尔到数值的隐式强转。

### math.* 函数

`MathFunctionEvaluator` 实现 `ScalarFunctionEvaluator`，覆盖 17 个 Aviator 白名单数学函数：

| 函数 | 返回类型 | 说明 |
|------|---------|------|
| `sqrt`、`exp`、`log`、`log10` | `Double` | `log` 为自然对数 |
| `sin`、`cos`、`tan`、`asin`、`acos`、`atan` | `Double` | 弧度 |
| `floor`、`ceil` | `Double` | |
| `round` | `Long` | 对齐 Aviator `math.round(3.7) = 4L` |
| `pow`、`max`、`min` | `Double` | 双参数 |
| `abs` | 委托 `DefaultScalarFunctionEvaluator` | 已存在 |

`MathFunctionEvaluator` 与 `DefaultScalarFunctionEvaluator` 组合（后者提供 `abs`、`lower`、`upper`、`trim`、`length`、`coalesce`）。若要将调用方限制在 `math.*` 白名单内，应在 AST 层面校验 `ScalarFunction.name` 是否属于 `MathFunctionEvaluator.supportedFunctions`--不能依赖求值器拒绝，因为组合链暴露了字符串函数。

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
| 条件表达式 | 无 | `if/then/else/fi`、三元 `? :` |
| 标量解析 | 无 | `parseScalarExpression`（Aviator 兼容） |
| math.* 函数 | 无 | `MathFunctionEvaluator`（17 个函数） |
| 路径引用 | 基于符号 | `PropertyPath` 分段支持 |
| 序列化 | 直接 DTO | `kotlinx.serialization` JSON |
| 规范化 | 无 | 扁平化、常量折叠、德摩根 |

## 相关链接

- [symbol/README_ch.md](../README_ch.md) - 主符号模块文档
- [symbol/parse/](../parse/) - 多项式/不等式解析器
