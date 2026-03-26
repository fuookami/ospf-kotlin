# ospf-kotlin-utils `math/symbol` 日拆分计划

## 全局前提（必须保留）

本计划默认语境是**破坏性大版本（breaking release）**：允许 API 与实现做不兼容变更，目标是一次性全量替换，不保留兼容层或过渡路径。后续阅读和执行本文件时，均以该前提为最高优先级。

跨语言接口迁移规则：新增或迁移接口前，必须先判断该接口是否是 Rust 语言机制（所有权、借用、生命周期、引用运算等）才需要的设计；若是，则不在 Kotlin 中做同形迁移，改为 Kotlin 语义下的等价能力设计。

## 执行记录（2026-03-26）

1. 已按“Rust 语言机制特化接口不做 Kotlin 同形迁移”原则，移除 `operator/ref` 下 Rust 风格 `Ref` trait 迁移项（`AddRef`、`MulRef`、`OneRef`、`ZeroRef`）。
2. 后续新增接口统一执行前置评审：若接口仅因 Rust 语言机制存在，则不在 Kotlin 中保留同形接口，改以 Kotlin 语义下等价能力实现。
3. 已新增 `SymbolIdentity.kt`，统一 `Symbol` identity 与默认排序入口（`defaultSymbolComparator`），并替换 `CombineTerms.kt`、`Convert.kt` 内部私有 comparator。
4. 已补齐 `Comparison` 行为方法（`symbol/reverse/isStrict/includesEquality/isLessLike/isGreaterLike`），并移除 `Convert.kt` 本地方向反转逻辑。
5. 已新增 `CanonicalInequality` 及其基础转换/规范化路径；`normalizeToLessEqualForm` 对 `NE` 采用“保持原样，不隐式降级”。
6. 已新增 `LinearMatrixForm`、`LinearPolynomial.toMatrixForm`、`linearPolynomialFromMatrixForm`、`quadraticPolynomialFromMatrixForm`，补齐线性/二次矩阵正反向转换与维度校验。
7. 已新增 `evaluateOrdered(order, values)`（linear/quadratic/canonical）与 `partialEvaluate(values/provider)`（monomial/polynomial），并补齐“部分替换后常数折叠”与 `Map`/`ValueProvider` 一致性测试。

## 交接信息（2026-03-26）

### 已完成范围

- [x] Day 01: 轻量 identity 与统一排序基线
- [x] Day 02: `Comparison` 行为收口 + `CanonicalInequality`
- [x] Day 03: `LinearMatrixForm` 与 `fromMatrixForm`
- [x] Day 04: `evaluateOrdered` 与 `partialEvaluate`

### 关键落地文件

- [x] `SymbolIdentity.kt`
- [x] `inequality/Comparison.kt`
- [x] `inequality/CanonicalInequality.kt`
- [x] `operation/CombineTerms.kt`
- [x] `operation/Convert.kt`
- [x] `operation/MatrixForm.kt`
- [x] `operation/Evaluate.kt`
- [x] `src/test/.../symbol/inequality/InequalityTest.kt`
- [x] `src/test/.../symbol/operation/CombineTermsTest.kt`
- [x] `src/test/.../symbol/operation/ConvertTest.kt`
- [x] `src/test/.../symbol/operation/MatrixFormTest.kt`
- [x] `src/test/.../symbol/operation/EvaluateTest.kt`

### 已执行验证命令

- [x] `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=InequalityTest,CombineTermsTest,ConvertTest" test`
- [x] `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=CanonicalOperationTest,MatrixFormTest" test`
- [x] `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=EvaluateTest,CanonicalOperationTest" test`
- [x] `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=InequalityTest,CombineTermsTest,ConvertTest,EvaluateTest,DifferentiateTest,MatrixFormTest,CanonicalOperationTest" test`

### 下一棒建议起点（Day 05）

1. 在 `operation/Differentiate.kt` 增加 `QuadraticPolynomial.hessian(order)`。
2. 在 `operation/Differentiate.kt` 增加 `CanonicalPolynomial.hessian(order)`，当存在二次以上项时失败返回或抛错（不静默降级）。
3. 在 `DifferentiateTest.kt` / `MatrixFormTest.kt` 增加 Hessian 与 `Q` 的一致性校验。
4. 保持 `gradient(order)` 与 `hessian(order)` 的 symbol 顺序契约一致。

## 1. 目标

本计划基于 `ospf-rust-math/src/symbol` 与当前 Kotlin `utils/math/symbol` 的差距整理，目标是把剩余缺口拆成可以逐日推进的任务，并且保持与现有 `design.md` 一致：

1. 延续 `design.md` 中“Phase 0 ~ Phase 7 已完成、Phase 8 进行中”的结论。
2. 本轮直接推进到群/环/域驱动的泛型系数与指数语义，不再把 `Flt64` 作为过渡终态。
3. 允许按需要引入 Rust 对齐的 `DynSymbol` / `OwnedSymbol` / `SymbolDynId` 风格标识体系。
4. `utils.math.symbol` 继续保持不依赖 `core.frontend.*`。
5. 本轮为破坏性大版本，交付目标是全量替换，不保留兼容层。

## 2. 剩余缺口总表

| 方向 | 当前 Kotlin 状态 | 对应 Rust 能力 | 优先级 |
|------|------------------|----------------|--------|
| 轻量 identity / 排序规则 | 已实现统一 comparator 与 identity 入口（2026-03-26） | 基于稳定标识的 `OwnedSymbol`/`SymbolDynId` | P0 |
| `Comparison` 行为方法 | 已实现（2026-03-26） | `symbol/reverse/is_strict/includes_equality` | P0 |
| `CanonicalInequality` | 已实现（2026-03-26） | 已有完整模型 | P0 |
| `LinearMatrixForm` + `fromMatrixForm` | 已实现（2026-03-26） | 已有 | P0 |
| `evaluateOrdered` / `partialEvaluate` | 已实现（2026-03-26） | 已有 | P0 |
| `hessian` | 缺失 | 已有 | P0 |
| `CompileEval` / `CompileGradient` | 缺失 | 已有 | P1 |
| `LaTeX` 输出 | 缺失 | 已有 | P1 |
| `parser` | 缺失 | 已有线性/二次/Canonical/不等式解析 | P1 |
| 序列化表达式 / DSL | 缺失 | Rust 有 `serde` / `macros` | P2 |
| 群/环/域概念层闭包 | 仅有 `PlusSemiGroup/PlusGroup/NumberRing/NumberField`，缺少完整层次与统一入口 | Rust 有 `Semigroup/Monoid/Group/AbelianGroup/Ring/CommutativeRing/Field` | P2 |
| 概念与 `symbol` 的约束联动 | `symbol` 仍由 `Flt64` 驱动，未通过群/环/域接口约束 | Rust `symbol` 操作普遍由代数概念约束驱动 | P2 |
| 容差与比较契约体系 | 缺少统一 `Tolerance/TolerancedEq/TolerancedOrd` 约束入口 | Rust `operator` 有 `Tolerance` 比较套件 | P2 |
| 代数泛型与指数抽象 | `symbol` 仍锁定 `Flt64` 和隐式次数 | Rust 已泛型化到 `T` / `E` | P2 |

## 2.1 代数结构欠缺说明（群、环、域）

针对你提到的“群、环、域”部分，当前 Kotlin 主要欠缺不在“有没有接口名”，而在“是否形成可被 `symbol` 直接依赖的完整概念层”：

1. 概念闭包不完整：已有 `NumberRing/NumberField`，但缺少和 Rust 对齐的分层入口（例如 `Monoid`、`AbelianGroup`、`CommutativeRing` 等）与统一导出层。
2. 法则约束未工程化：缺少“结合律/交换律/分配律/单位元/逆元”对应的可复用验证模板，导致接口存在但语义难保证。
3. `symbol` 未绑定概念约束：`symbol` 结构和运算仍锁死在 `Flt64`，无法把“环上多项式”作为编译期约束表达出来。
4. 指数语义仍是数据层技巧：`CanonicalMonomial.factors: List<Symbol>` 用重复项模拟幂次，无法稳定承接域/环上的泛型指数运算。
5. 容差体系分离：缺少与代数概念并行的容差比较约束，影响泛型求值与优化算子的可组合性。

## 3. 先决策项

下面 4 个问题建议先冻结，否则后续实现会反复返工。

### Day 00: 约束冻结

- [ ] 确认 `normalizeToLessEqualForm` 不处理 `NE`，遇到 `NE` 时维持原样或返回失败，不做隐式降级。
- [ ] 确认 `Symbol` 标识体系采用全量升级方案（可直接对齐 Rust 风格）。
- [ ] 确认 `Compile*` 直接按泛型系数（至少 `Ring`）实现，不做 `Flt64` 限定版。
- [ ] 确认 `parser` 直接覆盖 polynomial、inequality、core 函数符号表达式。
- [ ] 确认“Rust 语言机制特化接口不做 Kotlin 同形迁移”作为后续新增接口评审前置条件。

交付物：

- [ ] 在本文件中补一段“已冻结决策”记录。
- [ ] 在 `design.md` 的 Phase 8 附录中补一个“当前约束”小节，避免后续文档漂移。

验收：

- [ ] 团队对 `NE`、identity、compile、parser 全量范围达成一致。

## 4. 每日拆分

### Day 01: 轻量 identity 与统一排序基线

目标：先把当前 `name + hashCode` 的排序归一化逻辑替换成可复用、可测试的统一规则。

建议文件：

- [ ] `Symbol.kt`
- [x] 新增 `SymbolIdentity.kt` 或 `SymbolOrder.kt`
- [x] `operation/CombineTerms.kt`
- [x] `operation/Convert.kt`
- [x] `operation/MatrixForm.kt`
- [x] `src/test/.../symbol/operation/CombineTermsTest.kt`
- [x] `src/test/.../symbol/operation/ConvertTest.kt`

说明：本轮未直接修改 `Symbol.kt`，采用新增 `IdentifiedSymbol` + `Symbol.identity()` 扩展方式完成标识体系升级。

细化步骤：

1. 升级 `Symbol` 标识体系到新版本（允许直接对齐 Rust 风格 `DynSymbol` / `OwnedSymbol` / `SymbolDynId` 思路）。
2. 抽取统一 comparator，禁止每个文件各自使用 `name + hashCode`。
3. 统一 `CanonicalMonomial`、`QuadraticMonomial` 的归一化排序规则。
4. 明确“同名不同对象”的处理策略，并补测试覆盖。
5. 确保新标识体系在 `operation/*`、`polynomial/*`、`inequality/*` 中统一生效。

验收：

- [x] `CombineTermsTest.kt` 保持通过。
- [x] 新增“同名 symbol 的排序/归并”测试。
- [x] `Convert.kt`、`MatrixForm.kt` 不再各自维护私有 comparator。
- [x] `symbol` 标识体系在 symbol 全模块统一，不再依赖临时 identity 逻辑。

### Day 02: `Comparison` 行为收口 + `CanonicalInequality`

目标：先补齐不等式语义层，再把 Canonical 不等式接进现有 convert 路径。

建议文件：

- [x] `inequality/Comparison.kt`
- [x] 新增 `inequality/CanonicalInequality.kt`
- [x] `operation/Convert.kt`
- [x] `operation/MatrixForm.kt`
- [x] `src/test/.../symbol/inequality/InequalityTest.kt`
- [x] `src/test/.../symbol/operation/ConvertTest.kt`

细化步骤：

1. 给 `Comparison` 增加 `symbol`、`reverse`、`isStrict`、`includesEquality`、`isLessLike`、`isGreaterLike`。
2. 删除 `Convert.kt` 中本地 `reversedDirection` 之类的重复逻辑，统一改走 `Comparison`。
3. 新增 `CanonicalInequality(lhs, rhs, comparison)` 数据模型。
4. 增加 `LinearInequality` / `QuadraticInequality` 到 `CanonicalInequality` 的转换接口。
5. 补 `moveAllToLhs`、`normalizeToLessEqualForm` 在 Canonical 路径的语义。

验收：

- [x] `InequalityTest.kt` 覆盖 `LT/LE/EQ/GE/GT/NE` 的基础行为。
- [x] `CanonicalInequality` 支持基础构造、反向、规范化。
- [x] `Convert.kt` 中不再散落方向反转逻辑。

### Day 03: `LinearMatrixForm` 与 `fromMatrixForm`

目标：补齐矩阵化的正反向能力，把线性和二次路径统一起来。

建议文件：

- [x] `operation/MatrixForm.kt`
- [x] `operation/Convert.kt`
- [x] `src/test/.../symbol/operation/MatrixFormTest.kt`

细化步骤：

1. 新增 `LinearMatrixForm(c, d, order)` 数据结构。
2. 为 `LinearPolynomial` 增加 `toMatrixForm(order)`。
3. 为 `LinearPolynomial`、`QuadraticPolynomial` 增加 `fromMatrixForm(...)`。
4. 明确二次矩阵的对称约定，避免调用方重复处理 `0.5` 分摊逻辑。
5. 对 `CanonicalPolynomial` 保持“先转 quadratic 再 matrix”的策略，不在这一天做高次矩阵化。

验收：

- [x] `MatrixFormTest.kt` 覆盖 linear/quadratic 的正反向转换。
- [x] `order` 缺失 symbol、重复 symbol、矩阵维度不一致三类异常路径有测试。

### Day 04: `evaluateOrdered` 与 `partialEvaluate`

目标：补齐“只给一部分值”以及“按给定顺序求值”两条路径。

建议文件：

- [x] `operation/Evaluate.kt`
- [x] `adapter/ValueProvider.kt`
- [x] `src/test/.../symbol/operation/EvaluateTest.kt`

细化步骤：

1. 为 `LinearPolynomial`、`QuadraticPolynomial`、`CanonicalPolynomial` 增加 `evaluateOrdered(order, values)`。
2. 为 monomial/polynomial 增加 `partialEvaluate(values)`，返回简化后的表达式，而不是只返回数值。
3. 明确缺失值策略在 `partialEvaluate` 下的行为，避免与当前 `ReturnNull/AsZero/Fail` 冲突。
4. 保持 `ValueProvider` 路径与 `Map<Symbol, Flt64>` 路径行为一致。

验收：

- [x] `EvaluateTest.kt` 增加 ordered evaluate 场景。
- [x] `partialEvaluate` 覆盖“只替换部分 symbol 后常数折叠”的测试。
- [x] `ValueProvider` 与 `Map` 两条路径得到相同结果。

### Day 05: `hessian` 与二阶能力

目标：把当前“只有 gradient”补成可服务二阶优化的接口。

建议文件：

- [ ] `operation/Differentiate.kt`
- [ ] `operation/MatrixForm.kt`
- [ ] `src/test/.../symbol/operation/DifferentiateTest.kt`
- [ ] `src/test/.../symbol/operation/MatrixFormTest.kt`

细化步骤：

1. 为 `QuadraticPolynomial` 增加 `hessian(order)`。
2. 为 `CanonicalPolynomial` 增加 `hessian(order)`，但当存在二次以上项时返回失败或明确抛错，不做静默降级。
3. 统一 `gradient(order)` 与 `hessian(order)` 的 symbol 顺序约定。
4. 用 `MatrixForm` 结果交叉验证 Hessian 是否正确。

验收：

- [ ] `DifferentiateTest.kt` 覆盖纯线性、纯二次、混合项、重复变量项。
- [ ] `MatrixFormTest.kt` 补 Hessian 与 `Q` 矩阵对应关系测试。

### Day 06: 编译型求值与梯度

目标：补齐 `CompileEval` / `CompileGradient`，形成批量求值入口。

建议文件：

- [ ] 新增 `operation/Compile.kt`
- [ ] `operation/Evaluate.kt`
- [ ] `operation/Differentiate.kt`
- [ ] 新增 `src/test/.../symbol/operation/CompileTest.kt`

细化步骤：

1. 设计 `compileEval(order)` 返回闭包的 API。
2. 设计 `compileGradient(order)` 返回闭包的 API。
3. 直接支持泛型系数，约束到 `Ring` / `Field`。
4. 预计算 symbol index，避免运行期反复做 map lookup。
5. 用普通 `evaluate` / `gradient` 与 compile 版做结果对照。

验收：

- [ ] `CompileTest.kt` 覆盖 linear/quadratic/canonical。
- [ ] compile 版与普通版结果一致。
- [ ] 空表达式、缺失 symbol、重复 symbol 顺序等边界条件有测试。

### Day 07: LaTeX 输出

目标：补齐表达式展示层，减少调试和文档输出成本。

建议文件：

- [ ] 新增 `operation/Latex.kt`
- [ ] `inequality/Comparison.kt`
- [ ] 新增 `src/test/.../symbol/operation/LatexTest.kt`

细化步骤：

1. 定义 `LatexOptions`，至少覆盖 `compact`、`showOnes`、`useCdot`。
2. 为 `LinearPolynomial`、`QuadraticPolynomial`、`CanonicalPolynomial` 输出 LaTeX。
3. 为三类 inequality 输出 LaTeX。
4. 统一使用 `displayName ?: name` 作为展示名。
5. 特别处理 `1/-1` 系数、负号、幂次、空多项式。

验收：

- [ ] `LatexTest.kt` 覆盖 monomial、polynomial、inequality。
- [ ] 生成结果在系数 1、负号、平方项、交叉项上可读。

### Day 08: Parser 第一阶段

目标：搭建全量语法骨架，覆盖多项式、不等式和函数符号表达式。

建议文件：

- [ ] 新增 `parser/Token.kt`
- [ ] 新增 `parser/Lexer.kt`
- [ ] 新增 `parser/Expr.kt`
- [ ] 新增 `parser/Parser.kt`
- [ ] 新增 `parser/ParseError.kt`
- [ ] 新增 `src/test/.../symbol/parser/ParserPolynomialTest.kt`

细化步骤：

1. 设计 token 集：数字、标识符、`+ - * ^ ( )`、比较符号。
2. 建立最小 AST，不直接在 parser 中拼业务对象。
3. 支持 `linear`、`quadratic`、`canonical` 三类多项式。
4. 支持括号、前缀负号、显式乘法。
5. 覆盖 core 函数符号与组合表达式。

验收：

- [ ] `ParserPolynomialTest.kt` 覆盖 `2*x + 3*y - 1`、`x*x + 2*x*y + 1`、括号与负号。
- [ ] 新增函数符号解析用例，验证 parser 已覆盖 core 函数符号表达式。

### Day 09: Parser 第二阶段

目标：把 Canonical 和 inequality 路径补齐，形成完整解析入口。

建议文件：

- [ ] `parser/Parser.kt`
- [ ] `operation/Convert.kt`
- [ ] 新增 `src/test/.../symbol/parser/ParserInequalityTest.kt`

细化步骤：

1. 增加 Canonical 解析，支持 `x^2 * y^3` 和重复乘法形式。
2. 增加 `linear` / `quadratic` / `canonical` inequality 解析。
3. 统一 parser 输出与现有 `Convert.kt` 的落地路径。
4. 明确 parse 后的 normalize 策略，是自动规范化还是保留原式。

验收：

- [ ] `ParserInequalityTest.kt` 覆盖 `<=`、`>=`、`=`、`!=`。
- [ ] Canonical 解析覆盖高次项与混合常数项。
- [ ] parser 全量覆盖 `polynomial/inequality/core function symbol` 三类语法入口。

### Day 10: 序列化表达式与 DSL

目标：补齐“可存储/可构造”的工程化能力，并与 parser、symbol 内核统一建模。

建议文件：

- [ ] 新增 `serde/SymbolExpr.kt` 或 `serialization/SymbolExpr.kt`
- [ ] 新增 `dsl/SymbolDsl.kt`
- [ ] 新增 `src/test/.../symbol/serialization/SerializationTest.kt`
- [ ] 新增 `src/test/.../symbol/dsl/DslTest.kt`

细化步骤：

1. 定义统一表达式 DTO，并直接确定序列化协议实现（如 Jackson/Kotlinx 之一）。
2. 支持 polynomial / inequality 的 toExpr / fromExpr。
3. DSL 覆盖构造便利层与常用组合语法，不限制为“第一版最小能力”。
4. 让 parser、serialization、DSL 三者共享同一套内部表达式模型。

验收：

- [ ] `SerializationTest.kt` 覆盖 round-trip。
- [ ] `DslTest.kt` 至少覆盖 linear/quadratic/canonical 的基础构造。

### Day 11: core 适配补丁

目标：把新增能力全量接入 core，完成对旧 symbol 路径的替换。

建议文件：

- [ ] `ospf-kotlin-core/.../adapter/PolynomialAdapters.kt`
- [ ] `ospf-kotlin-core/.../adapter/InequalityAdapters.kt`
- [ ] `ospf-kotlin-core/.../adapter/MatrixFormAdapters.kt`
- [ ] `ospf-kotlin-core/.../frontend/symbol_migration/*`

细化步骤：

1. 将 core 中旧 symbol 路径替换到新模型，允许 API 破坏并同步改调用侧。
2. 为 compile / parser / latex / serde / DSL 能力提供完整接入点。
3. 复用并扩展现有 symbol migration 回归路径，覆盖破坏性改动后的行为。

验收：

- [ ] core 对照测试补齐新增能力。
- [ ] core 新 API 与新 symbol 内核一致，旧接口不再保留。
- [ ] compile、latex、serde、dsl 在 core 调用链中全量打通并可回归验证。

### Day 12: 代数概念层补齐（群/环/域）

目标：把群、环、域概念层补成新版本基线，并直接成为 symbol 的约束入口。

建议文件：

- [ ] 新增 `math/algebra/concept/Semigroup.kt`
- [ ] 新增 `math/algebra/concept/Monoid.kt`
- [ ] 新增 `math/algebra/concept/Group.kt`
- [ ] 新增 `math/algebra/concept/AbelianGroup.kt`
- [ ] 新增 `math/algebra/concept/Ring.kt`
- [ ] 新增 `math/algebra/concept/CommutativeRing.kt`
- [ ] 新增 `math/algebra/concept/Field.kt`
- [ ] 新增 `math/algebra/concept/mod.kt` 或 `Concepts.kt` 统一导出入口
- [ ] `Arithmetic.kt` 按新概念层重构（允许破坏性命名和继承调整）
- [ ] 新增 `src/test/.../math/algebra/concept/ConceptCompileTest.kt`

细化步骤：

1. 先定义概念层接口继承关系，保持和 Rust 概念层次一致。
2. 将 Kotlin 现有 `PlusSemiGroup/PlusGroup/NumberRing/NumberField` 直接并入或替换为新概念层。
3. 同步引入法则接口骨架（群/环/域），不拆到后续兼容阶段。
4. 在 `symbol-daily.md` 补“旧接口 -> 新概念接口”映射表，指导一次性迁移。

验收：

- [ ] `math/algebra/concept` 可以独立被引用。
- [ ] `Arithmetic.kt` 完成新概念层重构，旧命名不再作为约束前提。
- [ ] 新增编译型测试覆盖基本继承关系。

### Day 13: 代数法则测试与容差约束套件

目标：把“群/环/域”从接口名落地到可验证语义，并补容差约束。

建议文件：

- [ ] 新增 `math/algebra/law/GroupLaw.kt`
- [ ] 新增 `math/algebra/law/RingLaw.kt`
- [ ] 新增 `math/algebra/law/FieldLaw.kt`
- [ ] 新增 `operator/Tolerance.kt`，提供 `Tolerance`、`TolerancedEq`、`TolerancedOrd`
- [ ] `operator/Precision.kt` 与 `Tolerance` 契约对齐
- [ ] 新增 `src/test/.../math/algebra/law/LawTest.kt`

细化步骤：

1. 定义可复用 law-check 工具，支持对任意实现做采样校验。
2. 把容差比较从“单一 `Precision` 用法”扩展成可被 `symbol` 运算复用的契约。
3. 对新增接口逐项执行“Rust 语言机制依赖性”评审，避免引入 Kotlin 无需的 Rust 特化 trait。
4. 覆盖 `Flt64` 与至少一个非浮点数类型。

验收：

- [ ] `LawTest.kt` 至少覆盖群、环、域的关键法则。
- [ ] `Flt64` 能通过最小 law-check 集。
- [ ] `symbol` 后续泛型化可直接复用 `Tolerance` 约束。

### Day 14: `symbol` 与群/环/域全量对接

目标：把新概念层全量接入 `symbol`，完成泛型 monomial/polynomial/operation 落地。

建议文件：

- [ ] 新增 `symbol/generic/Coefficient.kt`、`symbol/generic/Exponent.kt`
- [ ] `monomial/*`、`polynomial/*` 全量泛型化
- [ ] `operation/Evaluate.kt`、`operation/CombineTerms.kt`、`operation/Differentiate.kt`、`operation/MatrixForm.kt` 全量泛型化
- [ ] 新增 `src/test/.../symbol/generic/LinearGenericTest.kt`
- [ ] 新增 `src/test/.../symbol/generic/QuadraticGenericTest.kt`
- [ ] 新增 `src/test/.../symbol/generic/CanonicalGenericTest.kt`
- [ ] 新增 `symbol-generic-spike.md`

细化步骤：

1. `Linear/Quadratic/Canonical` 全量迁移到泛型系数与指数约束。
2. 旧 `Flt64` 专用 API 直接移除或降级为简单别名，不作为主路径维护。
3. `evaluate`、`combineTerms`、`differentiate`、`matrixForm` 跑通泛型编译与行为测试。
4. core 调用点同步升级到新泛型 API。
5. 明确“monomial/polynomial/operation 全量泛型化（系数 + 指数）”作为 Day 14 完成条件。

验收：

- [ ] `LinearGenericTest.kt`、`QuadraticGenericTest.kt`、`CanonicalGenericTest.kt` 通过。
- [ ] 至少两个不同系数类型通过同一套 symbol 运算测试。
- [ ] `symbol-generic-spike.md` 输出“已落地能力 + 剩余问题”，不再作为是否继续的开关。
- [ ] `symbol` 的 monomial/polynomial/operation 已全量泛型化并替换旧主路径。

## 5. 推荐执行顺序

建议不要按“看起来最酷的能力”排序，而是按依赖链排序：

1. Day 00 ~ Day 02：先冻结语义和 identity 基线。
2. Day 03 ~ Day 05：补齐矩阵、求值、二阶导这三个直接影响算法正确性的能力。
3. Day 06 ~ Day 10：再补 compile、latex、parser、serialization、DSL。
4. Day 11 ~ Day 14：在同一 release 分支完成 core 替换与群/环/域泛型化收口。

## 6. 每日验收命令建议

utils 侧建议优先跑定向测试，再跑模块级测试：

- [x] `mvn -pl ospf-kotlin-utils -DskipITs -Dtest=CombineTermsTest,ConvertTest test`
- [x] `mvn -pl ospf-kotlin-utils -DskipITs -Dtest=InequalityTest,MatrixFormTest,EvaluateTest,DifferentiateTest test`
- [ ] `mvn -pl ospf-kotlin-utils -DskipITs -Dtest=ConceptCompileTest,LawTest,LinearGenericTest test`
- [ ] `mvn -pl ospf-kotlin-utils -DskipITs test`

core 侧建议从 Day 03 起持续回归（每完成一组能力就跑一次）：

- [ ] `mvn -pl ospf-kotlin-core -DskipITs test`
