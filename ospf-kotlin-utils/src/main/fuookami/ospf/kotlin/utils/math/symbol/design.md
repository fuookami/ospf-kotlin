# ospf-kotlin-utils `math/symbol` 详细设计文档（Kotlin 版）

## 1. 概述

### 1.1 背景

当前 `ospf-kotlin-core` 中已经实现了大量符号表达式能力（monomial / polynomial / inequality / function symbol），但通用代数逻辑与建模域逻辑（变量、Token、机制模型）耦合在一起，带来以下问题：

1. 复用边界不清晰：其他模块若只需要代数能力，必须依赖 core。  
2. 维护成本高：同类逻辑分散在多个文件，难以统一优化。  
3. 测试成本高：通用代数测试被迫依赖 core 的上下文对象。  
4. 迁移成本高：未来若需要多引擎、多语义层，耦合会放大。  

### 1.2 重构目标

本重构的核心目标：

1. 将“与业务无关的通用符号代数能力”下沉到 `ospf-kotlin-utils/math/symbol`。  
2. core 保留“建模域与求解流程”能力。  
3. 通过适配层逐步迁移，不破坏现有业务 API。  
4. 建立文件级实施计划与验收标准，支持分阶段落地。  

### 1.3 非目标

1. 本轮不改求解器接入与机制模型语义。  
2. 本轮不一次性清理所有 DSL 语法糖。  
3. 本轮先覆盖线性/二次能力，Canonical 作为后续阶段。  

### 1.4 约束

1. 复用现有 `Symbol`（`Symbol.kt`）和 `Category`（`Category.kt`）。  
2. 不引入 Rust 风格额外符号标识对象模型。  
3. 新增设计须兼容现有 `AbstractVariableItem`、`IntermediateSymbol`。  

---

## 2. 现状评估（As-Is）

### 2.1 现有目录与职责

`utils` 当前：

```text
ospf-kotlin-utils/.../math/symbol/
├─ Symbol.kt
└─ Category.kt
```

`core` 当前（与符号代数相关的核心目录）：

```text
ospf-kotlin-core/.../frontend/expression/
├─ monomial/
│  ├─ Monomial.kt
│  ├─ LinearMonomial.kt
│  └─ QuadraticMonomial.kt
├─ polynomial/
│  ├─ Polynomial.kt
│  ├─ LinearPolynomial.kt
│  └─ QuadraticPolynomial.kt
└─ symbol/
   ├─ IntermediateSymbol.kt
   ├─ SymbolCombination.kt
   ├─ linear_function/*
   └─ quadratic_function/*

ospf-kotlin-core/.../frontend/inequality/
├─ Sign.kt
├─ Inequality.kt
├─ LinearInequality.kt
└─ QuadraticInequality.kt
```

### 2.2 关键耦合点

| 模块 | 代表文件 | 当前耦合 | 问题 |
|---|---|---|---|
| monomial | `LinearMonomial.kt` | 直接依赖 `AbstractVariableItem`、`TokenList` | 代数层无法独立测试 |
| polynomial | `LinearPolynomial.kt` | 同时负责结构、算子、evaluate | 职责过重，改动风险大 |
| inequality | `LinearInequality.kt` | 与 core 多种表达式类型深耦合 | 规范化逻辑难复用 |
| symbol function | `IntermediateSymbol.kt` + function files | 缓存、注册、计算全部混合 | 无法分离“代数计算”与“机制建模” |

### 2.3 技术债清单

1. 同类“合并同类项/规范化”逻辑在不同层重复。  
2. 线性与二次算子扩展函数数量庞大，文件体量过大。  
3. 表达式求值依赖 Token 语义，无法单独复用。  
4. 缺少 utils 层对符号代数的系统化测试。  

---

## 3. 目标架构（To-Be）

### 3.1 分层架构

```text
┌──────────────────────────────────────────────┐
│                  ospf-kotlin-core            │
│  variable / token / mechanism / solver       │
│  intermediate symbol / function symbol        │
│  (通过 adapter 调用 utils symbol operation)    │
└──────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────┐
│             ospf-kotlin-utils:math:symbol    │
│  data model: monomial/polynomial/inequality  │
│  operation: combine/evaluate/diff/matrix     │
│  pure math, no core.frontend dependency       │
└──────────────────────────────────────────────┘
```

### 3.2 依赖规则

1. `core` 可以依赖 `utils.math.symbol`。  
2. `utils.math.symbol` 不能依赖 `core.frontend.*`。  
3. 业务函数符号、Token 注册逻辑保留在 `core`。  
4. 通用代数计算尽量通过 `utils.math.symbol.operation` 完成。  

### 3.3 兼容策略

1. 保持 core 对外 API 不变。  
2. 迁移期采用“旧实现 + 新实现对照验证”。  
3. 最终逐步删除 core 中重复的纯代数逻辑。  

---

## 4. `utils.math.symbol` 详细设计

### 4.1 包结构（新增）

```text
ospf-kotlin-utils/.../math/symbol/
├─ Symbol.kt                        (existing)
├─ Category.kt                      (existing)
├─ monomial/
│  ├─ LinearMonomial.kt
│  ├─ QuadraticMonomial.kt
│  └─ CanonicalMonomial.kt          (Phase 8)
├─ polynomial/
│  ├─ LinearPolynomial.kt
│  ├─ QuadraticPolynomial.kt
│  └─ CanonicalPolynomial.kt        (Phase 8)
├─ inequality/
│  ├─ Comparison.kt
│  ├─ LinearInequality.kt
│  └─ QuadraticInequality.kt
├─ operation/
│  ├─ CombineTerms.kt
│  ├─ Evaluate.kt
│  ├─ Differentiate.kt
│  ├─ Convert.kt
│  └─ MatrixForm.kt
├─ adapter/
│  └─ ValueProvider.kt
└─ dsl/
   └─ SymbolDsl.kt                  (Phase 8)
```

### 4.2 核心类型定义

#### 4.2.1 单项式

```kotlin
data class LinearMonomial(
    val coefficient: Flt64,
    val symbol: Symbol
)

data class QuadraticMonomial(
    val coefficient: Flt64,
    val symbol1: Symbol,
    val symbol2: Symbol? = null
) {
    val isQuadratic get() = (symbol2 != null)
}
```

不变量：

1. `LinearMonomial.symbol` 不允许为空。  
2. `QuadraticMonomial.symbol2 == null` 表示线性项。  
3. 二次项合并时必须走统一顺序归一化规则（见 4.5.2）。  

#### 4.2.2 多项式

```kotlin
data class LinearPolynomial(
    val monomials: List<LinearMonomial>,
    val constant: Flt64 = Flt64.zero
) {
    val category: Category get() = Linear
}

data class QuadraticPolynomial(
    val monomials: List<QuadraticMonomial>,
    val constant: Flt64 = Flt64.zero
) {
    val category: Category =
        if (monomials.any { it.isQuadratic }) Quadratic else Linear
}
```

设计约束：

1. 对外默认不可变，避免隐式副作用。  
2. 性能敏感路径可补充 `Mutable*Builder`（Phase 3）。  
3. `monomials` 允许未合并状态，调用方可显式 `combineTerms()`。  

#### 4.2.3 不等式

```kotlin
enum class Comparison { LT, LE, EQ, NE, GE, GT }

data class LinearInequality(
    val lhs: LinearPolynomial,
    val rhs: LinearPolynomial,
    val comparison: Comparison
)

data class QuadraticInequality(
    val lhs: QuadraticPolynomial,
    val rhs: QuadraticPolynomial,
    val comparison: Comparison
)
```

### 4.3 适配输入值模型

```kotlin
fun interface ValueProvider {
    operator fun get(symbol: Symbol): Flt64?
}

class MapValueProvider(
    private val values: Map<Symbol, Flt64>
) : ValueProvider {
    override fun get(symbol: Symbol): Flt64? = values[symbol]
}
```

补充策略：

1. `TokenTableValueProvider` 放在 core adapter 层实现，不进入 utils。  
2. 对缺失值的处理通过 `MissingValuePolicy` 控制。  

```kotlin
enum class MissingValuePolicy {
    ReturnNull,
    AsZero,
    Fail
}
```

### 4.4 错误返回策略

为兼容 Kotlin 现有风格，提供两组 API：

1. 轻量版：返回 nullable（便于替换现有逻辑）。  
2. 严格版：返回 `Ret<T>`（`utils.functional.Result`）。  

错误码建议复用现有 `ErrorCode`：

1. `IllegalArgument`  
2. `DataNotFound`（缺失值且 policy=Fail）  
3. `ApplicationError`（内部不可恢复场景）  

### 4.5 算法规范

#### 4.5.1 线性合并同类项

输入：`List<LinearMonomial>`  
处理：`HashMap<Symbol, Flt64>` 累加  
输出：过滤零系数后重建列表

复杂度：`O(n)`。

#### 4.5.2 二次合并同类项

键定义：`Pair<Symbol, Symbol?>`。

归一化规则：

1. 线性项：`(s, null)`。  
2. 二次项：若 `(a, b)`，必须规范为 `(min(a,b), max(a,b))`。  
3. 比较方式：默认按 `Symbol.name`，可通过参数注入 `Comparator<Symbol>` 覆盖。  

复杂度：`O(n)`。

#### 4.5.3 求值

规则：

1. `ReturnNull`：任一缺失值导致整体返回 `null`。  
2. `AsZero`：缺失值当 0。  
3. `Fail`：返回 `Failed(DataNotFound)`。  

#### 4.5.4 导数（线性/二次）

1. `d(Linear)/d(s)`：返回 `Flt64`。  
2. `d(Quadratic)/d(s)`：返回 `LinearPolynomial`。  
3. `gradient(order: List<Symbol>)`：按 `order` 顺序输出。  
4. `hessian(order: List<Symbol>)`：Phase 5 引入。  

#### 4.5.5 矩阵化

输出模型：

```kotlin
data class QuadraticMatrixForm(
    val q: Array<DoubleArray>,
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)
```

约定：

1. 统一采用 `x^T Q x + c^T x + d`。  
2. 若输入二次项使用 `0.5` 规范，调用侧负责转换。  
3. `order` 由调用侧显式传入。  

### 4.6 operation 接口草案

```kotlin
interface CombineTerms<out P> {
    fun combineTerms(symbolComparator: Comparator<Symbol>? = null): P
}

interface Evaluate<out T> {
    fun evaluate(provider: ValueProvider, policy: MissingValuePolicy = MissingValuePolicy.ReturnNull): T?
}

interface Differentiate<out D> {
    fun derivative(symbol: Symbol): D
}
```

### 4.7 性能策略

1. 保持“构造不强制合并”，避免链式算子中重复合并。  
2. `combineTerms` 采用局部 Map，避免全局共享状态。  
3. 提供 `combined` 与 `combineInPlace` 双版本（不可变/可变场景）。  
4. 对大表达式计算引入批量接口（Phase 6）。  

### 4.8 并发策略

1. utils 层默认无共享可变状态。  
2. 计算函数纯函数化，线程安全。  
3. 缓存策略放 core（TokenTable / model context），不下沉到 utils。  

---

## 5. core 适配层设计

### 5.1 新增目录（core）

```text
ospf-kotlin-core/.../frontend/expression/adapter/
├─ ValueProviderAdapters.kt
├─ MonomialAdapters.kt
├─ PolynomialAdapters.kt
└─ MatrixFormAdapters.kt

ospf-kotlin-core/.../frontend/inequality/adapter/
└─ InequalityAdapters.kt
```

### 5.2 适配规则

1. `AbstractVariableItem` 已实现 `Symbol`，直接透传。  
2. `IntermediateSymbol` 已实现 `Symbol`，直接透传。  
3. `Sign` 与 `Comparison` 做一一映射。  
4. core 对外 API 不变，内部替换实现。  

### 5.3 迁移优先级映射

| 迁移优先级 | core 文件 | 处理方式 |
|---|---|---|
| P0 | `expression/polynomial/LinearPolynomial.kt` | 将合并、规范化、求值委托 utils |
| P0 | `expression/polynomial/QuadraticPolynomial.kt` | 同上，先替换纯代数段 |
| P1 | `inequality/LinearInequality.kt` | 规范化与比较逻辑下沉调用 |
| P1 | `inequality/QuadraticInequality.kt` | 同上 |
| P2 | `expression/monomial/*` | 保留与 variable/token 绑定部分，剥离代数部分 |

### 5.4 不迁移范围（保留 core）

1. `symbol/linear_function/*`、`symbol/quadratic_function/*` 的注册逻辑。  
2. `IntermediateSymbol` 的 Token 缓存与 prepare 语义。  
3. 机制模型与约束注册流程。  

---

## 6. 详细实施计划（按 Rust 文档颗粒度）

### 6.1 阶段总览

| 阶段 | 名称 | 周期 | 优先级 | 目标 |
|---|---|---:|---|---|
| Phase 0 | 基线冻结 | 1-2 天 | P0 | 固化旧行为 |
| Phase 1 | utils 数据层 | 2-3 天 | P0 | 建立 monomial/polynomial/inequality |
| Phase 2 | utils 运算层（线性） | 2-3 天 | P0 | combine/evaluate/normalize |
| Phase 3 | utils 运算层（二次） | 2-3 天 | P1 | derivative/matrix/convert |
| Phase 4 | core 适配层 | 3-4 天 | P1 | 打通 core->utils |
| Phase 5 | core 线性路径切换 | 3-5 天 | P1 | 线性代数逻辑迁移 |
| Phase 6 | core 二次与不等式切换 | 4-6 天 | P1 | 二次/不等式逻辑迁移 |
| Phase 7 | 收口清理 | 2-3 天 | P2 | 删除重复实现 |
| Phase 8 | Canonical + DSL 扩展 | 4-7 天 | P2 | 补齐高级能力 |

### 6.2 Phase 0 任务清单（基线冻结）

| 编号 | 文件 | 任务 | 产出 | 验收 |
|---|---|---|---|---|
| 0.1 | `core` 新增测试目录 | 建立回归测试骨架 | 可运行测试模块 | CI 通过 |
| 0.2 | 线性样例集 | 固化加减乘、求值、规范化结果 | Golden 数据 | 与当前实现一致 |
| 0.3 | 二次样例集 | 固化二次项展开与求值 | Golden 数据 | 与当前实现一致 |
| 0.4 | 不等式样例集 | 固化 `normalizeToLessEqual` 语义 | Golden 数据 | 与当前实现一致 |

### 6.3 Phase 1 任务清单（utils 数据层）

| 编号 | 文件 | 任务 | 产出 | 验收 |
|---|---|---|---|---|
| 1.1 | `monomial/LinearMonomial.kt` | 定义线性单项式数据类 | 编译通过 | 单测通过 |
| 1.2 | `monomial/QuadraticMonomial.kt` | 定义二次单项式数据类 | 编译通过 | 单测通过 |
| 1.3 | `polynomial/LinearPolynomial.kt` | 定义线性多项式 | 编译通过 | 单测通过 |
| 1.4 | `polynomial/QuadraticPolynomial.kt` | 定义二次多项式 | 编译通过 | 单测通过 |
| 1.5 | `inequality/Comparison.kt` | 比较符号定义 | 编译通过 | 单测通过 |
| 1.6 | `inequality/*Inequality.kt` | 两类不等式模型 | 编译通过 | 单测通过 |

### 6.4 Phase 2 任务清单（线性 operation）

| 编号 | 文件 | 任务 | 产出 | 验收 |
|---|---|---|---|---|
| 2.1 | `operation/CombineTerms.kt` | 线性合并同类项 | API + 实现 | 大小样例通过 |
| 2.2 | `adapter/ValueProvider.kt` | ValueProvider + Map 实现 | API + 实现 | 单测通过 |
| 2.3 | `operation/Evaluate.kt` | 线性求值（3 种缺失策略） | API + 实现 | 单测通过 |
| 2.4 | `operation/Convert.kt` | 线性标准化辅助 | API + 实现 | 单测通过 |

### 6.5 Phase 3 任务清单（二次 operation）

| 编号 | 文件 | 任务 | 产出 | 验收 |
|---|---|---|---|---|
| 3.1 | `operation/CombineTerms.kt` | 二次项归一化 + 合并 | API + 实现 | 单测通过 |
| 3.2 | `operation/Differentiate.kt` | 二次导数与梯度 | API + 实现 | 单测通过 |
| 3.3 | `operation/MatrixForm.kt` | `QuadraticMatrixForm` 转换 | API + 实现 | 单测通过 |
| 3.4 | `operation/Convert.kt` | Linear <-> Quadratic 转换 | API + 实现 | 单测通过 |

### 6.6 Phase 4 任务清单（core adapter）

| 编号 | 文件 | 任务 | 产出 | 验收 |
|---|---|---|---|---|
| 4.1 | `ValueProviderAdapters.kt` | TokenTable/Map -> ValueProvider | 适配器 | 对照测试通过 |
| 4.2 | `MonomialAdapters.kt` | core monomial <-> utils monomial | 适配器 | 对照测试通过 |
| 4.3 | `PolynomialAdapters.kt` | core polynomial <-> utils polynomial | 适配器 | 对照测试通过 |
| 4.4 | `InequalityAdapters.kt` | Sign/Comparison + inequality 转换 | 适配器 | 对照测试通过 |

### 6.7 Phase 5 任务清单（线性路径切换）

| 编号 | 文件 | 任务 | 产出 | 验收 |
|---|---|---|---|---|
| 5.1 | `core/.../LinearPolynomial.kt` | 合并与求值委托 utils | 兼容实现 | 基线零回归 |
| 5.2 | `core/.../LinearMonomial.kt` | 纯代数片段委托 utils | 兼容实现 | 基线零回归 |
| 5.3 | `core/.../LinearInequality.kt` | normalize 逻辑委托 utils | 兼容实现 | 基线零回归 |

### 6.8 Phase 6 任务清单（二次与不等式切换）

| 编号 | 文件 | 任务 | 产出 | 验收 |
|---|---|---|---|---|
| 6.1 | `core/.../QuadraticPolynomial.kt` | 合并/导数/矩阵化委托 utils | 兼容实现 | 基线零回归 |
| 6.2 | `core/.../QuadraticMonomial.kt` | 二次归一化委托 utils | 兼容实现 | 基线零回归 |
| 6.3 | `core/.../QuadraticInequality.kt` | normalize 逻辑委托 utils | 兼容实现 | 基线零回归 |

### 6.9 Phase 7 任务清单（收口清理）

| 编号 | 文件 | 任务 | 产出 | 验收 |
|---|---|---|---|---|
| 7.1 | core 重复代数代码段 | 删除已替代实现 | 代码收敛 | 全量测试通过 |
| 7.2 | 迁移注释与文档 | 补齐文档和弃用标记 | 文档完善 | 评审通过 |

### 6.10 Phase 8（扩展）

1. Canonical monomial/polynomial。  
2. DSL 构造器。  
3. 可选 parser / latex 输出。  

### 6.11 当前执行状态（2026-03-13）

| 阶段 | 状态 | 进度说明 |
|---|---|---|
| Phase 0 | ✅ 已完成 | 已建立 `core/frontend/symbol_migration` 回归测试骨架并持续扩充。 |
| Phase 1 | ✅ 已完成 | `utils.math.symbol` 数据层（monomial/polynomial/inequality）已落地。 |
| Phase 2 | ✅ 已完成 | 线性 operation（combine/evaluate/convert）已落地并有单测。 |
| Phase 3 | ✅ 已完成 | 二次 operation（combine/differentiate/matrix/convert）已落地并有单测。 |
| Phase 4 | ✅ 已完成 | core 适配层已落地：`ValueProvider/Monomial/Polynomial/MatrixForm/Inequality` adapters。 |
| Phase 5 | ✅ 已完成 | `LinearPolynomial`、`LinearMonomial`、`LinearInequality` 关键路径已委托 utils。 |
| Phase 6 | ✅ 已完成 | `QuadraticPolynomial`、`QuadraticMonomial`、`QuadraticInequality` 关键路径已委托 utils。 |
| Phase 7 | ✅ 已完成 | 重复实现收口、告警噪音清理、构建告警收口已完成。 |
| Phase 8 | 🔄 进行中 | Canonical 数据结构与基础 operation 已启动并完成首批落地。 |

Phase 7 已完成项（截至 2026-03-13）：

1. `LinearInequality` / `QuadraticInequality` 的 normalize 合并逻辑收口到 adapter helper，并保留 fallback。  
2. `LinearPolynomial` / `QuadraticPolynomial` 与 `LinearMonomial` / `QuadraticMonomial` 的 `tokenList`、`tokenTable`、`results`、`values` 求值路径已统一委托 utils。  
3. `IntermediateSymbol` 中 `ExpressionSymbol` 与 `FunctionSymbol` 的 tokenTable 缓存求值模板已抽取公共 helper，减少重复代码。  
4. 补充了 `values 覆盖优先级`、`tokenList/tokenTable 一致性`、`results 路径一致性`、`adapter 不支持 symbol` 等回归测试。  
5. `IntermediateSymbol` 新增可指定 `cacheKey` 的 `shouldPrepare` / `prepareIfNotCached` 重载，以支持 `tokenTable.cached(self, ...)` 场景统一抽象。  
6. 已完成一批函数符号 `prepare(values, tokenTable)` 模板收口：  
   `linear_function/And.kt`、`linear_function/Not.kt`、`linear_function/Binaryzation.kt`、`linear_function/MaskingRange.kt`、`quadratic_function/Binaryzation.kt`、`quadratic_function/MaskingRange.kt`。  
7. 完成上述收口后执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 26, Failures: 0, Errors: 0`（2026-03-13）。  
8. 继续完成一批 `prepare(values, tokenTable)` 模板收口：  
   `linear_function/BalanceTernaryzation.kt`、`linear_function/SatisfiedAmount.kt`（Or/And 两个实现）、`linear_function/SatisfiedAmountInequality.kt`；并保留 `SatisfiedAmount.kt` 中只检查 `tokenTable.cached(self)` 的特殊分支以避免语义变化。  
9. 再次执行 `mvn -pl ospf-kotlin-core test -DskipITs` 验证通过，`Tests run: 26, Failures: 0, Errors: 0`（2026-03-13）。  
10. 再完成一批线性函数模板收口：  
    `linear_function/Or.kt`、`linear_function/OneOf.kt`、`linear_function/SameAs.kt`、`linear_function/Semi.kt`、`linear_function/First.kt`。  
11. 执行回归测试 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 26, Failures: 0, Errors: 0`（2026-03-13）。  
12. 完成一批线性函数 `prepare(values, tokenTable)` 模板收口：  
    `linear_function/If.kt`、`linear_function/IfIn.kt`、`linear_function/IfThen.kt`、`linear_function/Mod.kt`、`linear_function/Rounding.kt`、`linear_function/Masking.kt`、`linear_function/InStepRangeFunction.kt`、`linear_function/BivariateLinearPiecewise.kt`、`linear_function/UnivariateLinearPiecewise.kt`、`linear_function/Sigmoid.kt`、`linear_function/Slack.kt`、`linear_function/SlackRange.kt`。  
13. 完成一批二次函数 `prepare(values, tokenTable)` 模板收口：  
    `quadratic_function/Linear.kt`、`quadratic_function/Max.kt`、`quadratic_function/Min.kt`、`quadratic_function/Semi.kt`、`quadratic_function/Sigmoid.kt`、`quadratic_function/InStepRangeFunction.kt`、`quadratic_function/Masking.kt`、`quadratic_function/Mod.kt`、`quadratic_function/Product.kt`、`quadratic_function/Rounding.kt`、`quadratic_function/Slack.kt`、`quadratic_function/SlackRange.kt`、`quadratic_function/UnivariateLinearPiecewise.kt`、`quadratic_function/BivariateLinearPiecewise.kt`。  
14. 再次执行 `mvn -pl ospf-kotlin-core test -DskipITs` 验证通过，`Tests run: 26, Failures: 0, Errors: 0`（2026-03-13）。  
15. 阶段性检索后，曾仅保留两个特殊缓存分支未统一：`linear_function/SatisfiedAmount.kt`（`cached(self)`）与 `linear_function/Xor.kt`（固定 `cached(this)` 分支）。  
16. `IntermediateSymbol` 新增 `shouldPrepareWithFixedCacheKey` / `prepareIfNotCachedWithFixedCacheKey`，用于表达“固定 cache key、忽略 values cache key”语义。  
17. 使用上述 helper 收口 `linear_function/SatisfiedAmount.kt` 与 `linear_function/Xor.kt` 的特殊缓存分支；再次执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 26, Failures: 0, Errors: 0`（2026-03-13）。  
18. 新增 `cache_regression/PrepareCacheKeyRegressionTest.kt`（2 个用例），覆盖 `values != null` 时 fixed cache key 与普通 cache key 的行为差异；执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 28, Failures: 0, Errors: 0`（2026-03-13）。  
19. 清理 `symbol/*` 中冗余的 `if (values.isNullOrEmpty()) prepareAndCache(null, tokenTable) else prepareAndCache(values, tokenTable)` 模板，统一为 `prepareAndCache(values, tokenTable)`，减少重复实现（涉及 `IfThen`、`InStepRangeFunction`、`Sigmoid`、`Not`、`SatisfiedAmount`、`quadratic/Binaryzation` 等）。再次执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 28, Failures: 0, Errors: 0`（2026-03-13）。  
20. 清理一批函数符号中 `values` 变量名遮蔽（shadowing）问题，统一重命名为 `evaluatedValues`（`linear/Max`、`linear/Min`、`linear/OneOf`、`linear/SameAs`、`quadratic/Max`、`quadratic/Min`、`quadratic/Product`），降低可读性和静态告警噪音；执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 28, Failures: 0, Errors: 0`（2026-03-13）。  
21. 清理 `linear/Min.kt` 与 `linear/Xor.kt` 的剩余局部告警（`unused i`、`bin` 变量遮蔽），保持行为不变；执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 28, Failures: 0, Errors: 0`（2026-03-13）。  
22. 删除 `inequality/adapter/NormalizeAdapters.kt` 中 `legacyMergeLinearMonomials` / `legacyMergeQuadraticMonomials` 纯代数 fallback，实现完全收口到 utils `combineTerms + adapter` 路径；adapter 回转失败改为显式错误，避免静默退回旧实现。  
23. 新增中间符号归一化回归用例（`InequalityNormalizeBaselineTest` 新增 2 个测试：线性/二次包含 `LinearIntermediateSymbol` 单项式），验证 adapter-only 路径稳定；执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 30, Failures: 0, Errors: 0`（2026-03-13）。  
24. 在 `expression/adapter/MonomialAdapters.kt` 与 `expression/adapter/PolynomialAdapters.kt` 中，为 `toCore*OrNull` 系列接口补充 `@Deprecated` 标记，明确迁移方向为 `toCore*Ret`（显式错误通道），避免继续扩散 silent-null fallback 风格。  
25. 清理 `MaskingRange` 线性/二次实现中未使用局部变量，同时保留 `fixedValues` 评估失败时回退 `register(model)` 的既有语义（通过 guard 表达式保持行为等价）；并对两处 `MonotoneUnivariateLinearPiecewiseFunction.x(...)` 的未使用参数加抑制注解，降低迁移期告警噪音。执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 30, Failures: 0, Errors: 0`（2026-03-13）。  
26. 修复 `linear/InStepRangeFunction` companion `invoke` 中 `args` 未透传问题（由“未使用参数”告警暴露），保持函数符号 `args` 透传语义完整。  
27. 补充 `linear/Inequality.register` 与 `callback/CallBackModel` 中迁移期保留参数的显式 `UNUSED_PARAMETER` 抑制，减少无效告警；执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 30, Failures: 0, Errors: 0`（2026-03-13）。  
28. 清理 backend solver/iis 一批静态告警噪音：  
    `LinearSolver.kt`、`QuadraticSolver.kt` 中 `computeIIS` 分支 `result` 命名遮蔽已改为 `iisResult`；`iis/Linear.kt` 中 callback 分支命名遮蔽已清理，并为 `performDeletionFiltering`（TODO 占位）添加参数未使用抑制；`iis/Quadratic.kt`（TODO 占位）添加 `UNUSED_PARAMETER` / `UNUSED_VARIABLE` 抑制。执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 30, Failures: 0, Errors: 0`（2026-03-13）。  
29. 清理 `backend/intermediate_model` 与 `frontend/model/mechanism` 的一批告警噪音：  
    `LinearTriadModel.kt` / `QuadraticTetradModel.kt` 中未使用参数、局部变量遮蔽、未使用索引变量已处理；`MechanismModel.kt` 中 `fixedVariables.mapKeys { it.key as Symbol }` 冗余强转已去除，`foldIndexed` 未使用索引改为 `_`，并为 `QuadraticMechanismModel` 的两个 TODO cut 生成函数补充 `UNUSED_PARAMETER` 抑制。  
30. `TokenTable.kt` 中为需要保留的 `mapKeys { it.key as IntermediateSymbol }`（用于规避 `cache` 重载歧义）增加函数级 `@Suppress("USELESS_CAST")`；`AdapterRoundTripTest.kt` 三处 `Ok` 强转改为 `when` 分支解包，去除测试侧 `No cast needed` 告警。再次执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 30, Failures: 0, Errors: 0`（2026-03-13）。  
31. 最新一次 `ospf-kotlin-core` 编译阶段 Kotlin 源码告警已收敛为 0（不含构建级 Maven POM 警告），Phase 7 的“告警噪音清理”子目标在 core 范围内阶段完成。  
32. 为 `inequality/adapter/InequalityAdapters.kt` 中 `toCoreInequalityOrNull`（线性/二次）补齐 `@Deprecated` 标记，与 monomial/polynomial adapter 保持一致，进一步收敛 silent-null fallback 风格到显式 `Ret` 风格。  
33. 对 `inequality/adapter/NormalizeAdapters.kt` 做结构收口：将线性/二次两套重复的“正负项拼装 + utils combineTerms + adapter 回转”流程抽取为共享 helper，保留异常语义不变（adapter 回转失败仍显式 `error`）。执行 `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 30, Failures: 0, Errors: 0`（2026-03-13）。  
34. 处理构建级 Maven 告警：在 `ospf-kotlin-utils/pom.xml` 为 `maven-surefire-plugin` 增加显式版本 `3.2.5`；再次执行 `mvn -pl ospf-kotlin-core test -DskipITs`，扫描阶段已不再出现 “plugin.version is missing” 警告，`Tests run: 30, Failures: 0, Errors: 0`（2026-03-13）。  
35. 完成 Phase 7 收口复核：`core/frontend` 内已无 `legacy*` 纯代数 fallback 代码；`toCore*OrNull` 仅作为兼容层保留且仓内无调用，迁移主路径已统一到显式 `Ret` 与 utils operation。  
36. 执行双模块验证 `mvn -pl ospf-kotlin-utils,ospf-kotlin-core test -DskipITs`：`utils`（256）+`core`（30）测试全部通过，确认 Phase 7 收口后的跨模块可编译与回归稳定性。  

Phase 7 待完成项：

无（Phase 7 已完成）。  

Phase 8 当前进展（截至 2026-03-13）：

1. 新增 Canonical 数据层：`monomial/CanonicalMonomial.kt`、`polynomial/CanonicalPolynomial.kt`，支持 `degree`/`category`（线性/二次/非线性）判定。  
2. 在 `operation/Convert.kt` 增加 canonical 与 linear/quadratic 的双向转换闭环：  
   `toCanonicalMonomial`、`toCanonicalPolynomial`、`CanonicalMonomial.toLinear/Quadratic*`、`CanonicalPolynomial.toLinear/Quadratic*`（含 `OrNull` 与 `Ret` 两组接口）。  
3. 在 `operation/CombineTerms.kt` 增加 canonical 合并能力：`Iterable<CanonicalMonomial>.combineCanonicalTerms` 与 `CanonicalPolynomial.combineTerms`。  
4. 在 `operation/Evaluate.kt` 增加 canonical 求值能力：`CanonicalMonomial`/`CanonicalPolynomial` 的 `evaluate` 与 `evaluateRet`，并沿用 `MissingValuePolicy` 语义。  
5. 新增回归测试 `operation/CanonicalOperationTest.kt`（5 个用例），覆盖 canonical 合并、求值、线性/二次往返转换、三次项拒绝转换、零次项折叠到常数项。  
6. 验证通过：  
   `mvn -pl ospf-kotlin-utils test -DskipITs`，`Tests run: 261, Failures: 0, Errors: 0`；  
   `mvn -pl ospf-kotlin-core test -DskipITs`，`Tests run: 30, Failures: 0, Errors: 0`。  

---

## 7. 测试与质量保证

### 7.1 测试目录建议

```text
ospf-kotlin-utils/src/test/fuookami/ospf/kotlin/utils/math/symbol/
├─ monomial/
├─ polynomial/
├─ inequality/
└─ operation/

ospf-kotlin-core/src/test/kotlin/fuookami/ospf/kotlin/core/frontend/symbol_migration/
├─ linear_regression/
├─ quadratic_regression/
└─ inequality_regression/
```

### 7.2 测试矩阵

| 维度 | 线性 | 二次 | 不等式 |
|---|---|---|---|
| 结构构造 | ✅ | ✅ | ✅ |
| 合并同类项 | ✅ | ✅ | - |
| 求值 | ✅ | ✅ | ✅ |
| 规范化 | ✅ | ✅ | ✅ |
| 导数/梯度 | - | ✅ | - |
| 矩阵化 | - | ✅ | - |

### 7.3 对照测试策略

1. 同一输入分别走“旧 core 实现”和“新 utils 实现”。  
2. 对比输出值、规范化结果、矩阵化结果。  
3. 随机样本 + 固定 golden 样本双轨。  

### 7.4 性能测试指标

1. 10^4 项合并耗时。  
2. 批量 evaluate 吞吐。  
3. 内存分配次数（相对旧实现不显著退化）。  

---

## 8. 风险与控制

### 8.1 风险列表

1. `Symbol` 的 `equals/hashCode` 语义在不同实现中不一致。  
2. 二次项排序规则不一致导致合并失败。  
3. 迁移期双轨维护引入临时复杂度。  
4. 大规模扩展函数重定向时行为回归。  

### 8.2 控制措施

1. 为 `Symbol` 契约新增测试（变量符号、中间符号分别验证）。  
2. 统一二次项归一化规则并写入 operation 单测。  
3. 每阶段完成后立即收敛，不长期双轨。  
4. 保持“先替换实现，后删旧逻辑”的顺序。  

### 8.3 回滚策略

1. 每个 phase 独立 PR。  
2. 保留阶段性 tag（`symbol-migration-phase-x`）。  
3. 若出现回归，按 phase 级别回退，不影响其他已稳定阶段。  

---

## 9. 里程碑与 PR 切分建议

1. PR-1：Phase 0 + Phase 1（基线 + 数据层）。  
2. PR-2：Phase 2（线性 operation）。  
3. PR-3：Phase 3（二次 operation）。  
4. PR-4：Phase 4（core adapter）。  
5. PR-5：Phase 5（线性切换）。  
6. PR-6：Phase 6（二次/不等式切换）。  
7. PR-7：Phase 7（清理收口）。  
8. PR-8：Phase 8（Canonical + DSL，按需）。  

---

## 10. 完成定义（Definition of Done）

满足以下条件才视为迁移完成：

1. `utils.math.symbol` 可独立编译、独立测试。  
2. core 中线性/二次/不等式通用代数逻辑已委托 utils。  
3. 基线回归测试全部通过。  
4. 性能不出现不可接受退化。  
5. 文档、测试、迁移说明齐全。  

---

## 11. 首批执行建议（立即可开工）

说明：本节已作为历史建议，当前执行已进入 Phase 8。  

当前建议（下一步）：

1. 继续 Phase 8 第二批：补充 Canonical 与 `MatrixForm`/`Differentiate` 的互操作能力。  
2. 为 Canonical 增加 core adapter 映射策略（优先只读路径，不改现有 core 对外 API）。  
3. 启动 Symbol DSL 的 MVP（构造器 + 最小测试），并评估 parser/latex 是否进入本轮。  

