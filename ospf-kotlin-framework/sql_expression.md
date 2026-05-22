# SQL Expression 当前状态与后续事项

更新日期：2026-05-22

本文合并并替代以下旧文档：

- `ospf-kotlin-framework/sql_expression.md`
- `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression/daily.md`

## 1. 总结

SQL Expression 已从“设计与实施计划”进入“已有实现但需要补齐质量与一致性”的状态。

本轮（2026-05-22）执行结果：

1. 已完成 P0：修复 `MybatisRepository.update` where 条件丢失与 `find` 分页 `last(...)` 覆盖问题。
2. 已完成 P1：`README.md` / `README_ch.md` 与当前 resolver 架构同步，移除 `EntityMeta` / `FieldBinding` 旧描述并修复文档链接。
3. 已完成 P2：Ktorm 补齐 `InExpression` 与 `Lt/Le/Gt/Ge`，并实现 `NullsOrder` 降级排序。
4. 已完成 P2：Ktorm/MyBatis/Mongo 统一 unsupported 表达式策略为“恒假条件/空结果策略”，避免误解释为无条件查询。
5. 已完成 P3：补齐三插件 translator 行为测试与 repository 集成测试。
6. 已完成 P3：framework + 三插件联测通过。
7. 已完成 P4：Ktorm/MyBatis/Mongo 新增 scalar translator，支持列引用、常量与基础算术表达式下推。
8. 已完成 P5：Ktorm/MyBatis/Mongo 支持列-列比较与基础算术 scalar expression 比较。
9. 已完成 P6：新增 `UnsupportedPredicatePolicy` 与 `PredicateTranslation`，repository 默认保持 `AlwaysFalse`，并支持 `FailFast` 与未实现 `ClientFilter` 的明确失败。

当前事实：

1. `ospf-kotlin-math` 的通用表达式 AST、DSL、parser、serde、normalize、evaluate 仍存在并有单元测试。
2. `ospf-kotlin-framework` 主模块保留表达式仓储公共接口与模型：`ExpressionRepository`、`SortBy`、`UpdateAssignments`。
3. ORM/存储适配已迁移到插件模块：Ktorm、MyBatis-Plus、MongoDB。
4. 旧计划中的 `EntityMeta` / `FieldBinding` 不再存在于当前代码中，实际实现改为 resolver 函数：
   - Ktorm：`KtormColumnResolver = (String) -> ColumnDeclaring<*>?`
   - MyBatis：`MybatisColumnNameResolver = (String) -> String?`
   - MongoDB：`MongoFieldNameResolver = (String) -> String?`
5. `daily.md` 中的 P8 泛型化、solver 边界、Rust 功能对齐等内容属于 core/framework 更大范围重构，不再放在 SQL Expression 文档中维护。

## 2. 当前代码结构

### 2.1 math 表达式层

位置：`ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/expression`

已存在：

- `PropertyPath.kt`
- `PathSymbol.kt`
- `ScalarExpression.kt`
- `BooleanExpression.kt`
- `ExpressionOperator.kt`
- `dsl/ExpressionDsl.kt`
- `parser/Lexer.kt`
- `parser/Parser.kt`
- `parser/Token.kt`
- `serde/ExpressionSerde.kt`
- `operation/Normalize.kt`
- `operation/EvaluateBoolean.kt`
- `operation/NumericOps.kt`

已存在测试：

- `PropertyPathPathSymbolTest.kt`
- `ExpressionASTTest.kt`
- `BooleanDslTest.kt`
- `BooleanParserTest.kt`
- `ExpressionSerdeTest.kt`
- `NormalizeTest.kt`
- `EvaluateBooleanTest.kt`

### 2.2 framework 公共模型层

位置：`ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression`

已存在：

- `RepositoryApi.kt`
  - `ExpressionRepository<E>`
  - `find(where)`
  - `find(where, sortBy, limit, offset)`
  - `count(where)`
  - `update(where, assignments)`
  - `delete(where)`
  - `exists(where)`
- `SortBy.kt`
  - `SortBy`
  - `SortItem`
  - `SortDirection`
  - `NullsOrder`
  - `NullsOrderSupport`
- `UpdateAssignment.kt`
  - `UpdateAssignments`
  - `SetValue`
  - `SetNull`
  - `SetFromExpression`
- `UnsupportedPredicatePolicy.kt`
  - `UnsupportedPredicatePolicy`
  - `PredicateTranslation`

已存在测试：

- `SortByTest.kt`
- `UpdateAssignmentTest.kt`

### 2.3 Ktorm 插件

位置：`ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm`

已存在：

- `KtormRepository.kt`
- `translator/KtormBooleanTranslator.kt`
- `translator/KtormScalarTranslator.kt`
- `translator/KtormOrderByTranslator.kt`
- `translator/KtormUpdateTranslator.kt`
- `translator/PatternMatchPolicy.kt`

已存在测试：

- `KtormBooleanTranslatorTest.kt`
- `KtormScalarTranslatorTest.kt`
- `KtormOrderByTranslatorTest.kt`
- `KtormUpdateTranslatorTest.kt`
- `KtormRepositoryIntegrationTest.kt`

### 2.4 MyBatis-Plus 插件

位置：`ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis`

已存在：

- `MybatisRepository.kt`
- `translator/MybatisBooleanTranslator.kt`
- `translator/MybatisScalarTranslator.kt`
- `translator/MybatisOrderByTranslator.kt`
- `translator/MybatisUpdateTranslator.kt`

已存在测试：

- `MybatisBooleanTranslatorTest.kt`
- `MybatisScalarTranslatorTest.kt`
- `MybatisOrderByTranslatorTest.kt`
- `MybatisUpdateTranslatorTest.kt`
- `MybatisRepositoryTest.kt`

### 2.5 MongoDB 插件

位置：`ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb`

已存在：

- `MongoRepository.kt`
- `translator/MongoBooleanTranslator.kt`
- `translator/MongoScalarTranslator.kt`
- `translator/MongoOrderByTranslator.kt`
- `translator/MongoUpdateTranslator.kt`

已存在测试：

- `MongoBooleanTranslatorTest.kt`
- `MongoScalarTranslatorTest.kt`
- `MongoOrderByTranslatorTest.kt`
- `MongoUpdateTranslatorTest.kt`
- `MongoRepositoryTest.kt`

## 3. 完成情况矩阵

| 能力 | 当前状态 | 说明 |
|------|----------|------|
| math AST | 已完成 | `ScalarExpression` / `BooleanExpression` 已存在 |
| math DSL/parser/serde/normalize/evaluate | 已完成 | 有对应实现与测试 |
| 公共排序模型 | 已完成 | `SortBy` 支持多字段、方向、nulls 配置 |
| 公共更新模型 | 已完成 | `UpdateAssignments` 支持 value/null/expression |
| 公共仓储接口 | 已完成 | `ExpressionRepository` 已存在 |
| Ktorm 插件骨架 | 已完成 | repository + 三类 translator 已存在 |
| MyBatis 插件骨架 | 已完成 | repository + 三类 translator 已存在 |
| MongoDB 插件骨架 | 已完成 | repository + 三类 translator 已存在 |
| `EntityMeta` / `FieldBinding` | 已取消或被替代 | 当前代码使用 resolver 函数，不再保留这两个模型 |
| 多 ORM 集成测试 | 已完成（本轮范围） | 已新增 Ktorm/MyBatis/Mongo repository 集成测试 |
| translator 行为完整性 | 已完成（本轮范围） | 三插件已统一 unsupported 语义并补行为断言 |
| scalar expression 下推 | 已完成（Predicate 范围） | 三插件支持列引用、常量与基础算术表达式 |
| 列-列比较 | 已完成 | 三插件支持 `Comparison(ScalarReference, ScalarReference)` |
| unsupported predicate 策略 | 已完成 | 默认 `AlwaysFalse`，支持 `FailFast`，`ClientFilter` 明确失败 |

## 4. P4-P6 执行结果与后续计划

### 4.1 范围定位

`ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression` 的职责应定位为 Predicate DSL 与仓储 predicate 契约，而不是完整数据库查询 DSL。

本包应负责：

1. 跨后端 predicate 模型：`BooleanExpression` / `ScalarExpression` 到后端过滤条件的翻译。
2. 与 predicate 紧密相关的公共仓储契约：`ExpressionRepository`、`SortBy`、`UpdateAssignments`。
3. 三个已接入后端（Ktorm / MyBatis-Plus / MongoDB）的 predicate translator 一致语义。

本包不应负责：

1. 函数表达式的完整数据库函数语义。
2. 聚合、`group by`、`join`、projection/select 字段。
3. 事务、批量操作、乐观锁、upsert。
4. 复杂执行策略的业务决策，例如是否客户端过滤。此类能力应由更高层仓储接口或查询接口提供。

作为 Predicate DSL，本轮已补齐：

1. 列-列比较翻译（如 `startAt < endAt`）。
2. 基础 `ScalarExpression` 下推（如 `price * quantity > 100`）。
3. unsupported predicate 策略从 translator 内部硬编码恒假，演进为仓储层可配置策略。

### 4.2 P4：抽象 ScalarExpression translator

状态：已完成。

已为 Ktorm / MyBatis-Plus / MongoDB 分别建立内部 scalar translator，使 boolean translator 不再只识别 `ScalarReference` 与 `ScalarConstant`。

完成清单：

1. Ktorm
   - 新增或内聚实现：`KtormScalarTranslator`。
   - 输入：`fuookami.ospf.kotlin.math.symbol.expression.ScalarExpression<*>`。
   - 输出：Ktorm 可用于 predicate 的 `org.ktorm.expression.ScalarExpression<*>` 或 `ColumnDeclaring<*>`。
   - 至少支持：
     - `ScalarReference`
     - `ScalarConstant`
     - 当前 math AST 中已有的基础算术表达式（加、减、乘、除等，如类型名不同，以当前代码为准）。
   - 不支持的 scalar expression 不应静默变成空条件。

2. MyBatis-Plus
   - 新增或内聚实现：`MybatisScalarTranslator`。
   - 输出建议为带参数的 SQL 片段模型，而不是直接拼接用户值。
   - 建议模型：
     - `sql: String`
     - `params: List<Any?>`
     - `isColumnOnly: Boolean`
   - 列引用通过 `MybatisColumnNameResolver` 解析。
   - 常量必须参数化，避免把值直接拼进 SQL。
   - 基础算术表达式可翻译成 `(left + right)`、`(left - right)` 等片段。

3. MongoDB
   - 新增或内聚实现：`MongoScalarTranslator`。
   - 输出建议为 Mongo `$expr` 可用的表达式值。
   - 至少支持：
     - `ScalarReference` -> `"$fieldName"`
     - `ScalarConstant` -> 原始常量值
     - 基础算术表达式 -> `$add` / `$subtract` / `$multiply` / `$divide`
   - 普通列-常量比较仍可继续使用 `Filters.eq/gt/...`；一旦比较任一侧不是普通列-常量形态，则走 `$expr`。

测试完成：

1. 每个插件新增 scalar translator 单测。
2. 覆盖列引用、常量、基础算术表达式、未知路径、unsupported scalar expression。
3. MyBatis 测试必须断言常量没有被直接拼入 SQL 片段。

### 4.3 P5：补齐列-列比较与标量表达式比较

状态：已完成。

`Comparison(left, right)` 不再只支持列-常量 / 常量-列，也支持：

1. 列-列比较：`path("a").gt(path("b"))`。
2. 标量表达式-常量比较：`(path("price") * path("quantity")).gt(100)`。
3. 常量-标量表达式比较：`100.lt(path("price") * path("quantity"))`。
4. 标量表达式-标量表达式比较。

完成清单：

1. `KtormBooleanTranslator.kt`
   - `translateComparison` 改为优先调用 Ktorm scalar translator。
   - 对 `Eq/Ne/Lt/Le/Gt/Ge` 统一构造 Ktorm `BinaryExpression`。
   - 保留当前 `NullCheck`、`InExpression`、`PatternMatch` 行为。
   - `InExpression` 候选项如包含非 `ScalarConstant`，下一阶段可先按 unsupported 策略处理，不强行下推。

2. `MybatisBooleanTranslator.kt`
   - `translateComparison` 改为支持：
     - 简单列-常量继续用 `wrapper.eq/gt/...` 等安全 API。
     - 列-列与复杂 scalar expression 使用参数化 `apply` 或等价安全机制。
   - 生成 SQL 时必须保持 resolver 后的列名，不使用原始 path。
   - 禁止把常量值直接拼进 SQL。

3. `MongoBooleanTranslator.kt`
   - 简单列-常量继续用 `Filters.eq/gt/...`。
   - 列-列、算术表达式比较使用 `$expr`：
     - `Eq` -> `$eq`
     - `Ne` -> `$ne`
     - `Lt` -> `$lt`
     - `Le` -> `$lte`
     - `Gt` -> `$gt`
     - `Ge` -> `$gte`
   - 保持当前 `IsNull` / `IsNotNull` 的 null/missing 语义。

测试完成：

1. Ktorm
   - translator 单测断言列-列比较生成二元表达式。
   - repository 集成测试覆盖 SQLite 中列-列过滤。
   - 如能稳定生成 SQL，补一条基础算术表达式过滤集成测试。

2. MyBatis-Plus
   - `QueryWrapper` / `UpdateWrapper` 条件测试覆盖列-列比较。
   - 测试复杂 scalar expression 比较时常量参数化。
   - 确认 `update(where, assignments)` 在复杂 where 下仍保留条件。

3. MongoDB
   - Bson JSON 测试覆盖 `$expr` 列-列比较。
   - Bson JSON 测试覆盖 `$expr` 算术表达式比较。
   - repository 测试覆盖复杂 predicate 传入 collection API。

### 4.4 P6：unsupported predicate 策略

状态：已完成。

已把 translator 中硬编码的 `alwaysFalse()` 语义保留为默认安全策略，并抽象成可配置策略，避免未来无法区分“必须失败”“可降级为空结果”“可交给上层客户端过滤”。

已新增公共模型：

```kotlin
enum class UnsupportedPredicatePolicy {
    FailFast,
    AlwaysFalse,
    ClientFilter
}
```

已新增翻译结果模型：

```kotlin
sealed class PredicateTranslation<out T> {
    data class Translated<T>(val value: T) : PredicateTranslation<T>()
    data class Unsupported(
        val reason: String,
        val expression: BooleanExpression? = null
    ) : PredicateTranslation<Nothing>()
}
```

落地策略状态：

1. 第一阶段可不一次性改造所有 public API，先在 translator 内部引入 helper，使 unsupported 都携带 reason。
2. repository 构造参数增加 `unsupportedPredicatePolicy`，默认 `AlwaysFalse`，保持现有行为兼容。
3. `FailFast`：遇到 unsupported 直接抛 `IllegalArgumentException`，错误消息包含表达式类型与原因。
4. `AlwaysFalse`：保持当前恒假条件策略。
5. `ClientFilter`：本阶段只保留枚举与错误提示，不强制实现客户端过滤。若尚未实现，应明确 fail fast，避免误以为已过滤。

测试完成：

1. 三插件 translator/repository 都覆盖默认 `AlwaysFalse`。
2. 三插件 repository 覆盖 `FailFast`。
3. `ClientFilter` 如未实现，必须有测试证明会明确失败，而不是返回未过滤数据。

### 4.5 P7：函数表达式跨层复用（P4-P6 后下一轮目标）

目标：在 P4-P6 完成后，复用现有 `ScalarFunction` 支持函数表达式，使 `abs` 这类逻辑数学函数符号能够同时服务于本地数学求值、Predicate DSL、数据库下推和符号代数能力判断。

注意：当前 math AST 已经存在 `ScalarFunction<T>`，不要新增平行的 `FunctionExpression` 类型。下一轮应围绕现有模型补语义、注册表、translator 方言映射和验收测试。

现有基础：

1. `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/expression/ScalarExpression.kt` 已有：
   - `ScalarFunction<T>(name, arguments)`
   - `ScalarExpressionFactory.function(name, arguments)`
2. `ExpressionSerde.kt` 已支持 `ScalarFunction` serde。
3. `Normalize.kt` 已将 `ScalarFunction` 纳入结构 key。
4. `EvaluateBoolean.kt` 当前对 `ScalarFunction` 返回 `null`，需要补函数求值语义。

设计原则：

1. `ScalarFunction("abs", ...)` 表达的是跨层共享的逻辑函数符号，而不是 SQL 函数字符串。
2. 同一个函数符号由不同层声明自己的解释：
   - math evaluator：本地求值，例如 `abs(x)` -> `kotlin.math.abs(x)`。
   - Ktorm translator：数据库下推，例如 `ABS(column)`。
   - MyBatis translator：参数化 SQL 片段，例如 `ABS(column)`。
   - Mongo translator：`$expr` 表达式，例如 `{ "$abs": "$field" }`。
   - symbol algebra：只判断是否能进入线性/二次/多项式闭包，不默认接受所有函数。
3. 函数是否可求值、可下推、可转多项式必须由能力注册表决定，不能按函数名拼接 SQL 或静默放行。
4. `abs` 可以作为数学符号表达式复用，但默认不属于线性/二次多项式闭包；如要用于优化模型，应由更高层做线性化或辅助变量转换。

建议新增或补充的公共模型：

```kotlin
object ScalarFunctionNames {
    const val Abs = "abs"
    const val Lower = "lower"
    const val Upper = "upper"
    const val Trim = "trim"
    const val Length = "length"
    const val Coalesce = "coalesce"
}
```

```kotlin
interface ScalarFunctionRegistry<R> {
    fun translate(name: String, arguments: List<R>): R?
}
```

如本地求值需要独立注册表，可使用：

```kotlin
interface ScalarFunctionEvaluator {
    fun evaluate(name: String, arguments: List<Any?>): Any?
}
```

建议修改清单：

1. math expression 层
   - 为 `ScalarFunction` 增加标准函数名常量或等价集中定义，避免各模块硬编码字符串。
   - 在 `EvaluateBoolean.kt` 或相邻 operation 文件中引入函数 evaluator。
   - 至少支持本地求值：
     - `abs(number)`
     - `lower(string)`
     - `upper(string)`
     - `length(string)`
     - `coalesce(a, b, ...)`
   - 函数参数数量或类型不匹配时返回明确失败，不要把失败误解释为 `null` 值。

2. Predicate DSL 层
   - 在 expression DSL 中补便捷函数：
     - `abs(expr)`
     - `lower(expr)`
     - `upper(expr)`
     - `length(expr)`
     - `coalesce(vararg expr)`
   - DSL 只负责构造 `ScalarFunction`，不绑定任何数据库方言。

3. Ktorm 插件
   - 在 P4 的 `KtormScalarTranslator` 中接入 Ktorm 函数注册表。
   - 至少支持 `abs`。
   - 字符串函数如 `lower/upper/length/trim` 可按 Ktorm API 能力逐步补，不能稳定生成时先标记 unsupported。

4. MyBatis-Plus 插件
   - 在 P4 的 `MybatisScalarTranslator` 中接入 MyBatis 函数注册表。
   - 输出必须继续是参数化 SQL 片段。
   - `abs(column)` 可生成 `ABS(column)`。
   - `lower(column)` 可生成 `LOWER(column)`。
   - 禁止 unknown function 直接生成 `name(args...)`。

5. MongoDB 插件
   - 在 P4 的 `MongoScalarTranslator` 中接入 Mongo 函数注册表。
   - 至少支持：
     - `abs` -> `$abs`
     - `lower` -> `$toLower`
     - `upper` -> `$toUpper`
     - `length` -> `$strLenCP`
     - `coalesce` -> `$ifNull` 链式表达式或等价表达。
   - 未支持函数必须走 P6 的 unsupported predicate 策略。

6. 符号代数边界
   - 不要让任意 `ScalarFunction` 自动转换为 `LinearPolynomial` / `QuadraticPolynomial` / `CanonicalPolynomial`。
   - 如已有 scalar expression 到 polynomial 的转换入口，应明确：
     - `abs(x)` 默认 unsupported。
     - `pow(x, 2)` 如未来支持，必须单独判断指数为常量 2 且目标 polynomial 类型允许。
     - 字符串函数永远不进入数值多项式转换。

P7 测试要求：

1. math 层
   - `ScalarFunction("abs", ...)` 作为数学符号函数的 AST 行为：
     - `abs(x)` 可表达为 `ScalarFunction`。
     - `collectReferences()` 能收集 `x`。
     - `isConstant()` 对 `abs(3)` 为 `true`，对 `abs(x)` 为 `false`。
   - `ScalarFunction` serde round-trip，至少覆盖 `abs(x)`。
   - `ScalarFunction(abs)` 的 normalize/structural key 稳定；除非明确实现化简规则，不要把 `abs(abs(x))` 隐式改写。
   - `abs/lower/upper/length/coalesce` 本地 evaluate，其中 `abs(-3)` 和 `x = -5` 时的 `abs(x)` 必须返回正值。
   - `abs` 参数数量或类型不匹配时有明确失败行为，不要被误解释为 `null` 值、SQL unsupported 或空结果。
   - 符号代数边界测试证明 `abs(x)` 不会被默认转换为 `LinearPolynomial` / `QuadraticPolynomial` / `CanonicalPolynomial`。
   - unknown function 和参数错误有明确失败行为。

2. Predicate DSL
   - DSL 构造出的表达式确认为 `ScalarFunction`。
   - 函数表达式可参与 `Comparison`，例如 `abs(path("balance")).gt(10)`。

3. Ktorm / MyBatis / MongoDB
   - `abs(path("x")).gt(10)` 可翻译并进入 where。
   - 至少一个字符串函数比较可翻译；若某后端暂不支持，应按 P6 策略明确失败或恒假。
   - unknown function 不会被拼接成 SQL 或 Mongo 表达式。

4. 符号代数
   - 测试证明 `abs(x)` 不会被默认转换为线性/二次多项式。
   - 如实现任何 algebra-compatible function，必须有单独正反例测试。

P7 验收标准：

1. 不新增 `FunctionExpression`，复用现有 `ScalarFunction`。
2. 标准函数名集中定义，translator 与 evaluator 不散落硬编码字符串。
3. `abs` 在 `ospf-kotlin-math` 层有 AST、serde、normalize/evaluate、符号代数边界单元测试，证明它首先是数学符号函数。
4. unknown function 在三插件中均不会变成无条件查询，也不会拼接成不受控 SQL。
5. `abs` 至少在 math evaluate、Ktorm、MyBatis、MongoDB 四处有明确支持或明确 unsupported 行为。
6. `abs` 等函数可作为通用数学符号表达式存在，但不会默认进入多项式符号运算闭包。
7. README 与本文件同步更新函数表达式的边界说明。

### 4.6 修改边界与注意事项

1. P4-P6 不要把函数表达式、聚合、`group by`、`join`、projection/select 字段塞进本包；函数表达式放到 P7 单独执行。
2. 不要改变 `ExpressionRepository` 现有方法签名，除非确有必要；优先通过构造参数和内部 translator 增强实现。
3. 不要把 MyBatis 常量直接拼接到 SQL 字符串。
4. 不要改变 MongoDB 当前空值语义：
   - `IsNull` 覆盖 `null` 与 missing。
   - `IsNotNull` 要求字段存在且非 `null`。
5. 不要移除当前恒假默认策略；它是现阶段避免 unsupported 变成无条件查询的安全基线。
6. 所有新增注释遵循项目规则，使用中英双语。

### 4.7 P4-P6 验收结果

状态：已完成。

功能验收：

1. Ktorm / MyBatis-Plus / MongoDB 均支持列-列比较。
2. Ktorm / MyBatis-Plus / MongoDB 均支持至少一组基础算术 scalar expression 下推。
3. unsupported predicate 默认仍返回空结果或恒假过滤，不会变成无条件查询。
4. `FailFast` 策略能给出明确异常。
5. 未实现的 `ClientFilter` 不会静默返回未过滤数据。

测试验收：

1. 新增或更新以下测试：
   - `KtormBooleanTranslatorTest.kt`
   - `KtormRepositoryIntegrationTest.kt`
   - `MybatisBooleanTranslatorTest.kt`
   - `MybatisRepositoryTest.kt`
   - `MongoBooleanTranslatorTest.kt`
   - `MongoRepositoryTest.kt`
2. 如新增 scalar translator 文件，应新增对应测试文件。
3. 所有新增测试名继续使用中英双语 `DisplayName`。

命令验收：

```powershell
mvn -pl ospf-kotlin-math "-Dtest=PropertyPathPathSymbolTest,ExpressionASTTest,BooleanDslTest,BooleanParserTest,ExpressionSerdeTest,NormalizeTest,EvaluateBooleanTest" test
mvn -pl ospf-kotlin-framework -Dtest=SortByTest,UpdateAssignmentTest test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb test
```

文档验收：

1. 更新本文件当前状态与完成情况矩阵。
2. 如新增 public 类型，更新 `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression/README.md` 与 `README_ch.md`。
3. README 的中英文互链与相对链接必须能解析。

## 5. 当前阶段结论

1. 本文原定义的 P0/P1/P2/P3 已全部完成。
2. 本轮新增的 P4/P5/P6 已全部完成。
3. P7 函数表达式跨层复用是下一轮目标，不属于本轮执行范围。

## 6. 建议验证命令

```powershell
mvn -pl ospf-kotlin-math "-Dtest=PropertyPathPathSymbolTest,ExpressionASTTest,BooleanDslTest,BooleanParserTest,ExpressionSerdeTest,NormalizeTest,EvaluateBooleanTest" test
mvn -pl ospf-kotlin-framework -Dtest=SortByTest,UpdateAssignmentTest test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb test
```

如要做最终验收，应运行包含 framework plugin 的完整构建，而不仅是上述窄测试。

## 7. 旧计划处理说明

旧文档中以下内容已经归档，不再作为当前执行清单：

1. Phase 0-5 的早期实施计划。
2. Phase F0-F7 的逐日计划。
3. `EntityMeta` / `FieldBinding` 的设计草案。
4. P8 泛型化、solver/framework 入口命名、Rust 功能缺口等跨模块事项。

这些内容如需继续维护，应迁移到对应模块的专项文档，而不是放在 SQL Expression 文档中。
