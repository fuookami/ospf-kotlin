# SQL Expression 设计与实施计划（交接文档）

> 状态：方案收敛完成，可直接进入编码。  
> 日期：2026-04-04  
> 目标读者：下一个执行环境（无上下文接入）。

## 1. 最终结论

采用“双层表达式”分层：

1. `math.symbol`：承载通用表达式语义（标量、布尔、比较、`IN`、`PatternMatch`），不依赖 ORM/SQL 方言。
2. `framework.persistence`：承载 SQL 专属能力（字段映射、`SortBy`、`UpdateAssignment`、方言翻译、查询执行）。

命名收敛：

1. 使用 `BooleanExpression`，不使用 `NonLinearExpression`。
2. 使用 `ScalarExpression<T>` + `BooleanExpression` 并行体系。
3. SQL 独有能力（`LIKE ESCAPE`、`ILIKE`、`EXISTS`、子查询、`UPDATE SET` 细节、raw sql）只放 framework。

## 2. 目标与非目标

### 2.1 目标

1. 支持复杂逻辑：`(A and B) or not C`。
2. 支持谓词：`Comparison / In / PatternMatch / IsNull / IsNotNull`。
3. 支持查询排序能力：`SortBy`（多字段、方向、空值顺序）。
4. 支持更新赋值能力：`UpdateAssignment`（`SET a = ... , b = ...`）。
5. 支持 JSON 往返、normalize、evaluate。
6. 在 framework 落地 Ktorm Translator（MVP）。

### 2.2 非目标（首期不做）

1. 不做运行时 lambda 字节码 AST 还原。
2. 不做多 ORM 全量适配（首期只做 Ktorm）。
3. 不在 `math.symbol` 引入 SQL 方言细节。
4. 不做复杂子查询自动推导。

## 3. 边界与依赖

1. `ospf-kotlin-math` 不依赖 `ktorm/jdbc/framework`。
2. `ospf-kotlin-framework` 依赖 `ospf-kotlin-math` 并做翻译。
3. `math.symbol` 只定义语义，不定义 SQL 文本。
4. `PO 字段 <-> Column` 映射、`SortBy`、`UpdateAssignment` 执行只放 `framework.persistence`。

## 4. math.symbol 模型

### 4.1 ScalarExpression（建议）

1. `sealed interface ScalarExpression<out T>`
2. `ConstantExpression<T>(value)`
3. `ReferenceExpression<T>(path: PropertyPath, type?)`
4. `UnaryScalarExpression<T>(operator, operand)`
5. `BinaryScalarExpression<T>(operator, left, right)`
6. `FunctionScalarExpression<T>(name, args)`
7. `CustomScalarExpression<T>(id, payload)`

### 4.2 BooleanExpression（建议）

1. `sealed interface BooleanExpression`
2. `BooleanConstantExpression(value)`
3. `ComparisonExpression<T>(left, comparator, right)`
4. `InExpression<T>(target, candidates, negated)`
5. `PatternMatchExpression(target, pattern, mode, caseSensitive, escapeChar?)`
6. `NullCheckExpression(target, isNull)`
7. `AndExpression(operands)`
8. `OrExpression(operands)`
9. `NotExpression(operand)`
10. `CustomBooleanExpression(id, payload)`

### 4.3 扩展机制（建议）

1. `ScalarOperatorRegistry`
2. `BooleanOperatorRegistry`
3. `ExpressionFunctionRegistry`

### 4.4 PathSymbol 桥接（新增约束）

目的：

1. 保持 SQL 字段引用的主语义在 `PropertyPath` / `ReferenceExpression`。
2. 在需要复用现有 `math.symbol.Symbol` 运算能力时，提供桥接而非替代。

建议：

1. 新增 `PathSymbol(path: PropertyPath)`，实现 `Symbol`（必要时实现 `IdentifiedSymbol` 以提供稳定身份）。
2. `PathSymbol.name = path.value`，`symbolId = "path:${path.value}"`。
3. `PathSymbol` 仅作为适配器，不作为 SQL AST 主节点；SQL AST 仍以 `ReferenceExpression(path)` 为准。
4. 提供 `PropertyPath <-> PathSymbol` 的显式转换函数，避免隐式魔法。

## 5. 需补齐的符号运算能力

当前缺口是“算术 + 单比较”，缺少逻辑树。必须补齐：

1. `And/Or/Not` 与布尔常量节点。
2. `In/PatternMatch/IsNull` 节点。
3. normalize：扁平化、常量折叠、去重、双重否定、德摩根。
4. `evaluateBoolean(...)` 本地求值。
5. JSON 序列化/反序列化。
6. DSL：`and/or/not/in/patternMatch/isNull`。

## 6. framework.persistence 设计

### 6.1 字段映射层（核心）

新增 `EntityMeta<E>` / `FieldBinding`：

1. `poPath -> ColumnDeclaring<*>` 显式映射。
2. 支持单表嵌套路径（MVP）。
3. 提供类型转换钩子（复用现有 `transform` 思路）。
4. `poPath` 推荐统一为 `PropertyPath`，避免字符串拼写漂移。

### 6.2 SortBy 能力（必须项）

新增排序模型（放 framework，不放 math）：

1. `data class SortBy(val items: List<SortItem>)`
2. `data class SortItem(val path: String, val direction: SortDirection, val nulls: NullsOrder?)`
3. `enum class SortDirection { Asc, Desc }`
4. `enum class NullsOrder { NullsFirst, NullsLast }`

说明：

1. `path` 复用 `EntityMeta` 字段解析。
2. `NullsOrder` 若方言不支持，采用降级策略（文档化）。

### 6.3 UpdateAssignment 能力（新增必须项）

新增更新赋值模型（放 framework，不放 math）：

1. `data class UpdateAssignments(val items: List<UpdateAssignment>)`
2. `sealed interface UpdateAssignment`
3. `data class SetValue(val path: String, val value: ScalarExpression<*>) : UpdateAssignment`
4. `data class SetNull(val path: String) : UpdateAssignment`
5. `data class SetFromExpression(val path: String, val expression: ScalarExpression<*>) : UpdateAssignment`

说明：

1. `path` 通过 `EntityMeta` 解析为具体 Column。
2. `value/expression` 允许常量、字段引用、函数表达式。
3. SQL 专属赋值行为（如数据库函数、方言 cast）放 translator 策略层。

### 6.4 Ktorm Translator（MVP）

新增：

1. `KtormBooleanExpressionTranslator`：`BooleanExpression -> ColumnDeclaring<Boolean>`。
2. `KtormOrderByTranslator`：`SortBy -> List<OrderByExpression>`（或直接 apply 到 query）。
3. `KtormUpdateAssignmentTranslator`：`UpdateAssignments -> updateBuilder.set(...)` 执行动作。

MVP 支持：

1. 条件：`Comparison/In/PatternMatch/NullCheck/And/Or/Not/BoolConst`。
2. 排序：多字段 `ASC/DESC`，空值顺序尽力映射。
3. 更新：`SetValue/SetNull/SetFromExpression`。

### 6.5 Repository 查询与更新接口

统一入口建议：

1. `find(where: BooleanExpression): List<T>`
2. `find(where: BooleanExpression, sortBy: SortBy?, limit: Int?, offset: Int?): List<T>`
3. `count(where: BooleanExpression): Long`
4. `update(where: BooleanExpression, assignments: UpdateAssignments): Int`

## 7. 分阶段实施计划

### Phase 0（0.5 天）：脚手架

实施项：

1. 创建 `math.symbol.expression` 与 `framework.persistence.expression` 包结构。
2. 创建基础 README 占位说明。

验收：

1. 结构可编译，无行为变化。

### Phase 1（1 天）：math.symbol 核心 AST

新增（建议）：

1. `.../expression/ScalarExpression.kt`
2. `.../expression/BooleanExpression.kt`
3. `.../expression/ExpressionOperator.kt`
4. `.../expression/PropertyPath.kt`
5. `.../expression/PathSymbol.kt`
6. `.../expression/ExpressionFactory.kt`

验收：

1. AST 可完整表达比较与逻辑组合。

### Phase 2（1.5 天）：DSL/parser/serde/normalize/evaluate

新增（建议）：

1. `.../expression/dsl/ExpressionDsl.kt`
2. `.../expression/parser/Token.kt`
3. `.../expression/parser/Lexer.kt`
4. `.../expression/parser/Parser.kt`
5. `.../expression/serde/ExpressionSerde.kt`
6. `.../expression/operation/Normalize.kt`
7. `.../expression/operation/EvaluateBoolean.kt`

验收：

1. parse + dsl + serde round-trip 通过。
2. normalize 与 evaluate 用例通过。
3. 若启用文本路径解析，则支持 `a.b.c` 形式的 `PropertyPath` 词法/语法。

### Phase 3（2 天）：framework 查询适配（含 SortBy）

新增（建议）：

1. `.../persistence/expression/EntityMeta.kt`
2. `.../persistence/expression/FieldBinding.kt`
3. `.../persistence/expression/SortBy.kt`
4. `.../persistence/expression/KtormBooleanTranslator.kt`
5. `.../persistence/expression/KtormOrderByTranslator.kt`
6. `.../persistence/expression/PatternMatchSqlPolicy.kt`
7. `.../persistence/expression/RepositoryQueryApi.kt`

验收：

1. where 条件翻译正确。
2. `SortBy` 多字段排序可执行。
3. 分页 + 排序 + 条件组合可执行。

### Phase 4（1 天）：framework 更新适配（UpdateAssignment）

新增（建议）：

1. `.../persistence/expression/UpdateAssignment.kt`
2. `.../persistence/expression/KtormUpdateAssignmentTranslator.kt`
3. `.../persistence/expression/RepositoryUpdateApi.kt`（或并入 QueryApi）

实施项：

1. 将 `UpdateAssignments` 翻译为 Ktorm update builder。
2. 支持 where + set 的批量更新。
3. 增加赋值类型与列类型兼容校验。

验收：

1. `update(where, assignments)` 正确返回影响行数。
2. `SetValue/SetNull/SetFromExpression` 用例全部通过。

### Phase 5（1 天）：兼容迁移

实施项：

1. 保持旧 `symbol.parser.Expr` 可用。
2. 需要时增加桥接：`legacy Expr -> Scalar/BooleanExpression`。
3. 更新文档与示例。

验收：

1. 老功能无回归，新接口可并存。

## 8. 测试计划

math 模块新增测试（建议）：

1. `.../expression/parser/BooleanParserTest.kt`
2. `.../expression/dsl/BooleanDslTest.kt`
3. `.../expression/serde/ExpressionSerdeTest.kt`
4. `.../expression/operation/NormalizeTest.kt`
5. `.../expression/operation/EvaluateBooleanTest.kt`

framework 模块新增测试（建议）：

1. `.../persistence/expression/EntityMetaTest.kt`
2. `.../persistence/expression/KtormBooleanTranslatorTest.kt`
3. `.../persistence/expression/KtormOrderByTranslatorTest.kt`
4. `.../persistence/expression/KtormUpdateAssignmentTranslatorTest.kt`
5. `.../persistence/expression/SqliteIntegrationTest.kt`

建议命令：

```powershell
mvn -pl ospf-kotlin-math -Dtest=BooleanParserTest,BooleanDslTest,ExpressionSerdeTest,NormalizeTest,EvaluateBooleanTest test
mvn -pl ospf-kotlin-framework -Dtest=EntityMetaTest,KtormBooleanTranslatorTest,KtormOrderByTranslatorTest,KtormUpdateAssignmentTranslatorTest,SqliteIntegrationTest test
```

## 9. 风险与控制

风险：

1. `PatternMatch` 方言差异。
2. `ReferenceExpression` 与列类型不一致。
3. `NullsFirst/NullsLast` 在不同数据库支持度不同。
4. 更新赋值表达式的类型推断错误可能导致运行时 SQL 异常。
5. 将 `PropertyPath` 直接当作 `Symbol.name` 参与运算时，可能出现身份歧义或冲突。

控制：

1. `PatternMatchSqlPolicy` 统一封装方言策略。
2. `EntityMeta` 做类型校验，Translator 早失败。
3. `NullsOrder` 提供方言降级与明确文档。
4. `UpdateAssignment` 进入 translator 前做列类型兼容校验。
5. `PathSymbol` 实现 `IdentifiedSymbol` 并固定 `symbolId = "path:${path.value}"`。

## 10. 交接执行清单

1. 严格按 Phase 0 -> 5 执行。
2. 每阶段先补测试，再补实现。
3. 先打通 Ktorm MVP：`where + sortBy + page + update(assignments)`。
4. 保持 `math.symbol` 无 SQL 依赖。

## 11. 结论

1. `math.symbol` 负责通用表达式语义与逻辑能力。
2. `framework.persistence` 负责 SQL 翻译、字段映射、`SortBy`、`UpdateAssignment` 执行。
3. 下一环境可按本文直接实施。
