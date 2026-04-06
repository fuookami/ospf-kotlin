# OSPF Kotlin Math Daily

日期：2026-04-06  
来源文档：`E:/workspace/ospf-kotlin/ospf-kotlin-framework/sql_expression.md`（2026-04-04）  
目标：提取并落地 `ospf-kotlin-math` 需实现的 SQL Expression 通用能力，形成可直接执行的分阶段计划。

## 1. 提取结果（仅 `ospf-kotlin-math` 范围）

### 1.1 必做项
1. 新增通用表达式 AST，采用并行体系：`ScalarExpression<T>` + `BooleanExpression`。
2. 新增路径抽象：`PropertyPath`，用于字段/属性引用的统一主语义。
3. 新增桥接符号：`PathSymbol(path)`，实现 `Symbol`，并实现 `IdentifiedSymbol`（`symbolId = "path:${path.value}"`）。
4. 补齐布尔逻辑能力：`And/Or/Not`、布尔常量、比较、`In`、`PatternMatch`、`NullCheck`。
5. 新增表达式扩展机制：`ScalarOperatorRegistry`、`BooleanOperatorRegistry`、`ExpressionFunctionRegistry`。
6. 新增 normalize 能力：扁平化、常量折叠、去重、双重否定消除、德摩根。
7. 新增本地求值能力：`evaluateBoolean(...)`。
8. 新增 JSON 往返：表达式序列化/反序列化。
9. 新增 DSL 与 parser：覆盖 `and/or/not/in/patternMatch/isNull`，并支持路径 `a.b.c`。
10. 保持兼容：保留现有 `symbol.parser.Expr`，按需补 `legacy Expr -> Scalar/BooleanExpression` 桥接。

### 1.2 明确不在 `math` 中实现
1. `SortBy`、`UpdateAssignment` 模型与执行。
2. `EntityMeta/FieldBinding` 字段映射。
3. Ktorm Translator 与 SQL 方言策略（含 `LIKE ESCAPE`、`ILIKE`、raw sql、子查询等）。

## 2. 当前仓库现状（与计划相关）

1. 已有基础符号体系：`math/symbol/Symbol.kt`、`SymbolIdentity.kt`。
2. 已有旧表达式 AST：`math/symbol/parser/Expr.kt`（算术与比较为主，缺逻辑树）。
3. 已有旧 DSL：`math/symbol/dsl/SymbolDsl.kt`（服务于 polynomial/inequality）。
4. 已有旧 serde：`math/symbol/serde/SymbolExpr.kt`（围绕 `Expr`）。
5. 现状可作为兼容层基础，新能力建议放入独立包：`math/symbol/expression/**`，避免一次性破坏迁移。

## 3. 详细实施计划（math 侧）

### Phase M0（0.5 天）：脚手架与边界固化

实施项：
1. 创建包结构：
   - `src/main/fuookami/ospf/kotlin/math/symbol/expression/`
   - `src/main/fuookami/ospf/kotlin/math/symbol/expression/dsl/`
   - `src/main/fuookami/ospf/kotlin/math/symbol/expression/parser/`
   - `src/main/fuookami/ospf/kotlin/math/symbol/expression/serde/`
   - `src/main/fuookami/ospf/kotlin/math/symbol/expression/operation/`
2. 在 `expression` 包内补一个简要 README（说明与旧 `parser.Expr` 并存策略与迁移方向）。
3. 固化边界：确保新包不引入 framework/ktorm 依赖。

验收标准：
1. 模块可编译。
2. 现有 `math.symbol` 测试无回归。

### Phase M1（1 天）：核心 AST + 路径桥接

新增文件（建议）：
1. `.../expression/PropertyPath.kt`
2. `.../expression/ScalarExpression.kt`
3. `.../expression/BooleanExpression.kt`
4. `.../expression/ExpressionOperator.kt`
5. `.../expression/PathSymbol.kt`
6. `.../expression/ExpressionFactory.kt`

实施细节：
1. `PropertyPath`：
   - 统一保存 `value: String`；
   - 提供 `segments` 解析；
   - 提供 `of("a", "b", "c")` 与文本解析入口（支持 `a.b.c`）。
2. `PathSymbol`：
   - `name = path.value`；
   - `symbolId = "path:${path.value}"`；
   - 提供 `PropertyPath.toPathSymbol()` 与 `Symbol.toPropertyPathOrNull()` 显式转换。
3. `ScalarExpression`：
   - `Constant/Reference/Unary/Binary/Function/Custom`。
4. `BooleanExpression`：
   - `BooleanConstant/Comparison/In/PatternMatch/NullCheck/And/Or/Not/Custom`。
5. `ExpressionOperator`：
   - 标量一元/二元操作符；
   - 比较符；
   - `PatternMatchMode`（通用语义，不携带 SQL 方言细节）。

验收标准：
1. AST 能完整表达比较 + 逻辑组合。
2. `PropertyPath` 与 `PathSymbol` 双向转换行为稳定（身份可重复计算且稳定）。

### Phase M2（1.5 天）：DSL + Parser

新增文件（建议）：
1. `.../expression/dsl/ExpressionDsl.kt`
2. `.../expression/parser/Token.kt`
3. `.../expression/parser/Lexer.kt`
4. `.../expression/parser/Parser.kt`

实施细节：
1. DSL：
   - 提供 `and/or/not`、比较、`in`、`isNull/isNotNull`、`patternMatch` 构造。
   - 保持函数风格与中缀风格并存，降低调用方迁移成本。
2. Lexer：
   - 关键字：`and`、`or`、`not`、`in`、`is`、`null`、`true`、`false`；
   - 标识符支持路径词法（`a.b.c`）。
3. Parser：
   - 优先级：`not` > `and` > `or`；
   - 支持 `( ... )` 分组；
   - 支持 `x in (...)`、`x not in (...)`、`x is null`、`x is not null`；
   - 先保证布尔表达式闭环，再扩展复杂函数参数。

验收标准：
1. DSL 构造与 parser 解析可表达同一语义树。
2. 典型复杂表达式可解析：`(A and B) or not C`、`a.b in (1,2,3)`、`name is not null`。

### Phase M3（1 天）：Serde + Normalize + Evaluate

新增文件（建议）：
1. `.../expression/serde/ExpressionSerde.kt`
2. `.../expression/operation/Normalize.kt`
3. `.../expression/operation/EvaluateBoolean.kt`

实施细节：
1. Serde：
   - 基于 `kotlinx.serialization`；
   - 支持 `ScalarExpression`/`BooleanExpression` 的 JSON 往返；
   - 保证 `PropertyPath`、`PathSymbol` 的稳定编码格式。
2. Normalize：
   - `And/Or` 扁平化；
   - 常量折叠（如 `A and true -> A`）；
   - 去重（结构等价）；
   - `not(not(x)) -> x`；
   - 德摩根：`not(A and B) -> not A or not B`，`not(A or B) -> not A and not B`。
3. Evaluate：
   - 提供 `evaluateBoolean(expression, context)`；
   - `context` 至少支持按 `PropertyPath` 取值；
   - 推荐返回 `Trivalent`（与 `null` 语义兼容），并提供 `Boolean?` 便捷桥接。

验收标准：
1. serde round-trip 结构等价。
2. normalize 规则覆盖关键重写场景。
3. evaluate 对比较、逻辑、空值检查、`in`、`patternMatch` 均可本地求值。

### Phase M4（1 天）：兼容迁移与桥接

新增文件（建议）：
1. `.../expression/adapter/LegacyExprBridge.kt`（命名可调整）

实施细节：
1. 增加桥接转换（按需）：
   - `symbol.parser.Expr -> ScalarExpression/BooleanExpression`；
   - 新表达式到旧 `Expr` 的可逆子集转换（仅在可表达时）。
2. 不移除旧 API，先并存，逐步引导调用方迁移。
3. 将桥接限制在“无 SQL 方言细节”的通用层。

验收标准：
1. 旧 `Expr` 相关能力可继续使用。
2. 新旧表达式可在公共子集上互通。

### Phase M5（0.5 天）：文档与收口

实施项：
1. 更新 `math/symbol/README.md` 与 `README_ch.md`，新增 expression 子模块说明与示例。
2. 补迁移指引：旧 `Expr` 到新 `BooleanExpression` 的对照表。
3. 在本 `daily.md` 回写实际完成项与未完成项。

验收标准：
1. 文档示例可编译（至少在测试中覆盖）。
2. 迁移指引可支撑 framework 侧接入。

## 4. 测试计划（math）

新增测试（建议）：
1. `src/test/fuookami/ospf/kotlin/math/symbol/expression/parser/BooleanParserTest.kt`
2. `src/test/fuookami/ospf/kotlin/math/symbol/expression/dsl/BooleanDslTest.kt`
3. `src/test/fuookami/ospf/kotlin/math/symbol/expression/serde/ExpressionSerdeTest.kt`
4. `src/test/fuookami/ospf/kotlin/math/symbol/expression/operation/NormalizeTest.kt`
5. `src/test/fuookami/ospf/kotlin/math/symbol/expression/operation/EvaluateBooleanTest.kt`
6. `src/test/fuookami/ospf/kotlin/math/symbol/expression/PropertyPathPathSymbolTest.kt`
7. `src/test/fuookami/ospf/kotlin/math/symbol/expression/LegacyExprBridgeTest.kt`

建议执行命令：
```powershell
mvn -pl ospf-kotlin-math "-Dtest=BooleanParserTest,BooleanDslTest,ExpressionSerdeTest,NormalizeTest,EvaluateBooleanTest,PropertyPathPathSymbolTest,LegacyExprBridgeTest" test
```

## 5. 风险与控制（math）

风险：
1. `PathSymbol` 身份冲突导致去重/比较不稳定。
2. `PatternMatch` 通用语义与后续 SQL 方言映射不一致。
3. `normalize` 规则若顺序不当，可能造成语义漂移。
4. `null` 求值语义若仅二值化，可能与 SQL 三值逻辑预期偏离。
5. 新旧 AST 并存阶段，调用方易混用接口。

控制措施：
1. 固定 `symbolId = "path:${path.value}"`，并加 identity 回归测试。
2. 在 `PatternMatchMode` 只定义中立语义，不编码方言特性。
3. normalize 规则拆分为可单测的原子变换并固定应用顺序。
4. `evaluateBoolean` 默认走 `Trivalent`，对外提供可选降级。
5. 为旧入口添加 `@Deprecated(message = "...use expression.BooleanExpression")` 的分阶段迁移标记（后续阶段执行）。

## 6. 建议执行顺序（可直接开工）

1. 先做 Phase M0 + M1（先把 AST 与路径桥接稳定下来）。✅ 已完成 (commit: 48cb8b17)
2. 再做 Phase M2（打通 DSL + parser 入口）。✅ 已完成 (commit: 9530e622)
3. 接着做 Phase M3（serde/normalize/evaluate 一次闭环）。✅ 已完成 (commit: e0bc60dd)
4. 最后做 M4 + M5（兼容迁移与文档收口）。✅ M4 已完成 (commit: cc364b01)，M5 进行中
5. 每阶段结束执行一次 `mvn -pl ospf-kotlin-math test`，并把结果回写本文件。

## 7. 完成状态

| 阶段 | 状态 | 提交 | 测试结果 |
|------|------|------|----------|
| M0 | ✅ 已完成 | 48cb8b17 | 通过 |
| M1 | ✅ 已完成 | 48cb8b17 | 通过 |
| M2 | ✅ 已完成 | 9530e622 | 通过 |
| M3 | ✅ 已完成 | e0bc60dd | 通过 (711 tests) |
| M4 | ✅ 已完成 | cc364b01 | 通过 (711 tests) |
| M5 | 🔄 进行中 | - | - |

### M3 备注
- 修复了 Trivalent/BalancedTrivalent 初始化顺序 bug，改用 sealed class + lazy 实现
- 添加了 simplifySingleOperand 最终简化步骤

### 新增文件清单

**M1 Core AST:**
- `expression/PropertyPath.kt` - 路径抽象
- `expression/PathSymbol.kt` - 路径-符号桥接
- `expression/ScalarExpression.kt` - 标量表达式 AST
- `expression/BooleanExpression.kt` - 布尔表达式 AST
- `expression/ExpressionOperator.kt` - 操作符定义
- `expression/ExpressionFactory.kt` - 工厂方法

**M2 DSL + Parser:**
- `expression/dsl/ExpressionDsl.kt` - DSL 构造
- `expression/parser/Token.kt` - Token 定义
- `expression/parser/Lexer.kt` - 词法分析器
- `expression/parser/Parser.kt` - 递归下降解析器

**M3 Serde + Normalize + Evaluate:**
- `expression/serde/ExpressionSerde.kt` - JSON 序列化
- `expression/operation/Normalize.kt` - 规范化规则
- `expression/operation/EvaluateBoolean.kt` - 本地求值

**M4 Bridge:**
- `expression/adapter/LegacyExprBridge.kt` - 新旧表达式桥接

**Bug Fix:**
- `Trivalent.kt` - 改用 sealed class 避免 URtn8 初始化问题
