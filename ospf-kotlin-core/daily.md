# OSPF Kotlin 泛型化交接计划（P13）

记录日期：2026-05-06
适用范围：ospf-kotlin-core、ospf-kotlin-math

## 1. 目标与边界

目标：完成 math.symbol 与 core 的主链泛型化，公开 API 以 V : RealNumber<V>, NumberField<V> 为主线。
约束：
- 公开主链必须 V 化（evaluate / invoke / register / addConstraint / solve）。
- solver 主链也必须 V 化；仅外部 solver 适配边界可保留 Flt64。
- 策略数值层例外：heuristic 中概率、温度、权重、变异率等超参数可保留 Flt64。
- MathFunctionSymbol.register 必须严格双阶段：
  1. MetaModel -> MechanismModel 转换时，在 MetaModel 的 tokenTable 拷贝上注册中间变量。
  2. MechanismModel 构建完成后，再注册函数符号附加约束。
- 禁止桥接别名回流；禁止 import as。

## 2. 当前真实状态（重新扫描基线）

| 检查项 | 当前值 |
|---|---:|
| math.symbol 公开签名含 Flt64 | 146 |
| 其中 adapter/flt64 | 145 |
| 其中非 adapter/flt64 | 1 |
| core 公开签名含 Flt64 | 392 |
| 其中 core/solver | 16 |
| 其中 core/solver/heuristic | 7 |
| 其中 core/model/callback | 4 |
| 其中非 core/solver | 376 |
| core/intermediate_symbol/function 公开签名含 Flt64 | 200 |
| import as | 0 |
| Suppress(UNCHECKED_CAST) | 0 |
| typealias *Flt64 | 1 |

审阅结论：
- function 目录存在系统性问题，不是仅 SlackFunction 单点问题。
- solver/callback 仍有 Flt64 公开签名，和“solver 主链 V 化”目标冲突。
- math.symbol 非 adapter 区仍残留 1 处 Flt64 别名（QuadraticInequality）。

## 3. 需要改动的地方（全量清单）

### 3.1 core/intermediate_symbol/function（全部纳入 WP-2 模板修正）
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Abs.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/And.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/BalanceTernaryzation.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Binaryzation.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/BivariateLinearPiecewise.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Ceiling.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Cos.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/First.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Floor.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/FunctionSymbol.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/If.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/IfIn.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/IfThen.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Imply.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Inequality.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/InStepRange.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Masking.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Max.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/MinMax.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Mod.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/OneOf.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Product.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/QuadraticInStepRange.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/QuadraticLinear.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/QuadraticMaskingRange.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/QuadraticMin.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Rounding.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/SameAs.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/SatisfiedAmount.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/SatisfiedAmountInequality.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Semi.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Sigmoid.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Sin.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Slack.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/SlackRange.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/UnivariateLinearPiecewise.kt

### 3.2 core 主链其他文件
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/IntermediateSymbol.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/basic/ModelView.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/basic/MultiObject.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/callback/CallBackModel.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/callback/CallBackModelInterface.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/Cell.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/LinearTriadModel.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/MechanismModelDumpingStatus.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/QuadraticTetradModel.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/SparseMatrix.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/Constraint.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/LinearConstraintInput.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MathInequalityDsl.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MathInequalityFlatten.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MechanismModel.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaConstraint.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaModel.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/Gap.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/heuristic/Normalization.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/heuristic/ParticleSwarmHeuristicSolver.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/heuristic/Selection.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/output/SolverOutput.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/value/IntoValue.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/value/SolveValueConversionContext.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/value/SolveValueValidation.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/Token.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenList.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenTable.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/variable/AbstractVariableItem.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/variable/AnyVariable.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/variable/VariableCombinationItem.kt
- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/variable/VariableIndependentItem.kt

### 3.3 math.symbol 的 Flt64 adapter 与残留点

adapter 文件（保留为边界，但需做 API 可见性与文档收口）：
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/CombineTerms.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/Compile.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/Convert.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/Differentiate.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/Evaluate.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/Inequality.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/Latex.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/MatrixForm.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/QuickDsl.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/QuickOps.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/Serde.kt
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64/ValueProvider.kt

非 adapter 残留（必须清零）：
- ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/inequality/QuadraticInequality.kt

## 4. 提交级清单（按顺序交接执行）

### C1：基线冻结与扫描脚本标准化
改动：
- 新增 scripts/scan-p13-mainchain.ps1，固化本页全部计数口径。
- 输出 scripts/scan-p13-mainchain-result.json（含每类计数、命中路径）。
验收：
- 脚本在本地可重复执行，输出字段齐全，计数与本页一致。

### C2：MathFunctionSymbol 契约与 register 双阶段收口
改动：
- FunctionSymbol.kt：把 registerAuxiliaryTokens/registerConstraints 的主接口改为 V 主链；Flt64 仅保留在 adapter 层。
- MetaModel.kt：在 tokenTable 拷贝上统一执行 registerAuxiliaryTokens。
- MechanismModel.kt：模型构建后统一执行 registerConstraints。
验收：
- MetaModel -> MechanismModel 路径不再依赖 Flt64 类型化 register 主接口。
- 双阶段执行顺序有测试覆盖（至少 1 个线性、1 个二次函数符号）。

### C3：function 第一批模板改造（基础算子）
改动文件：
- Abs.kt、Ceiling.kt、Floor.kt、Cos.kt、Sin.kt、Sigmoid.kt、Mod.kt、Semi.kt、Rounding.kt
改动要求：
- evaluate/invoke/polyX/register 公开签名改为 V。
- 去除 intoValue(value: Flt64) / fromValue(value: Flt64) 作为公开主路径。
验收：
- 上述文件公开签名 Flt64 命中为 0。

### C4：function 第二批模板改造（逻辑与条件）
改动文件：
- If.kt、IfIn.kt、IfThen.kt、Imply.kt、Inequality.kt、OneOf.kt、SameAs.kt、And.kt
改动要求：
- 构造入口支持 LinearPolynomial<V> / LinearIntermediateSymbol<V>。
- Flt64 重载仅做薄委托，不再作为主入口。
验收：
- 上述文件无 LinearPolynomial<Flt64> 主入口签名。

### C5：function 第三批模板改造（分段/范围/统计）
改动文件：
- InStepRange.kt、Masking.kt、Slack.kt、SlackRange.kt、SatisfiedAmount.kt、SatisfiedAmountInequality.kt、MinMax.kt、Max.kt、First.kt、UnivariateLinearPiecewise.kt、BivariateLinearPiecewise.kt、BalanceTernaryzation.kt、Binaryzation.kt
改动要求：
- 统一 ToLinearPolynomial<V> 工厂方法。
- prepare/evaluate 主链全部 V 化。
验收：
- core/intermediate_symbol/function 公开签名含 Flt64 从 200 下降到 0。

### C6：二次函数与 Product 专项
改动文件：
- Product.kt、QuadraticLinear.kt、QuadraticMin.kt、QuadraticMaskingRange.kt、QuadraticInStepRange.kt
改动要求：
- 二次函数全部采用双阶段 register。
- 数值比较（如 range check）通过 converter，不允许 asFlt64().toDouble() 直比。
验收：
- 上述文件公开签名中 Flt64 为 0（solver adapter 方法除外且需标注）。

### C7：IntermediateSymbol/Variable/Token 主链 V 化
改动文件：
- IntermediateSymbol.kt、Token.kt、TokenList.kt、TokenTable.kt
- AbstractVariableItem.kt、AnyVariable.kt、VariableCombinationItem.kt、VariableIndependentItem.kt
改动要求：
- 公开 evaluate 与变量组合运算签名 V 化。
- token 注册与缓存路径不暴露 Flt64 类型。
验收：
- 上述文件公开签名 Flt64 命中为 0。

### C8：mechanism 主链 V 化与桥接清理
改动文件：
- Constraint.kt、LinearConstraintInput.kt、MathInequalityDsl.kt、MathInequalityFlatten.kt、MetaConstraint.kt、MetaModel.kt、MechanismModel.kt
改动要求：
- 删除桥接语义残留，公开 DSL 与 addConstraint 主链只暴露 V。
- flatten/conversion 放入明确 adapter 边界，不在主链泄漏 Flt64。
验收：
- model/mechanism 公开签名 Flt64 命中为 0。
- MathInequalityDsl.kt 无桥接型 API。

### C9：callback 主链 V 化
改动文件：
- model/callback/CallBackModelInterface.kt、model/callback/CallBackModel.kt
- model/basic/ModelView.kt、model/basic/MultiObject.kt
改动要求：
- callback 抽象接口、回调输入/输出类型全部改为 V。
- 禁止在泛型接口里依赖 V.companion 默认值。
验收：
- core/model/callback 公开签名 Flt64 从 4 下降到 0。

### C10：solver 主链 V 化（策略数值层例外）
改动文件：
- solver/output/SolverOutput.kt、solver/value/IntoValue.kt、solver/value/SolveValueConversionContext.kt、solver/value/SolveValueValidation.kt、solver/Gap.kt
- solver/heuristic/Normalization.kt、solver/heuristic/Selection.kt、solver/heuristic/ParticleSwarmHeuristicSolver.kt
改动要求：
- solver 对外主接口改为 V；Flt64 仅保留在外部求解器适配边界。
- heuristic 仅“策略数值参数”可保留 Flt64，并在代码注释中标注 PolicyNumericException。
验收：
- core/solver 非策略数值签名 Flt64 命中为 0。
- 所有策略层 Flt64 命中均在白名单文件与字段。

### C11：math.symbol 收口
改动文件：
- inequality/QuadraticInequality.kt（移除 typealias QuadraticInequality = QuadraticInequalityOf<Flt64> 或迁移到 adapter）
- adapter/flt64/*（仅调整可见性与文档，不改语义）
改动要求：
- 非 adapter 公开 API 不出现 Flt64。
验收：
- math.symbol 非 adapter/flt64 公开签名 Flt64 从 1 下降到 0。
- 全仓 typealias *Flt64 命中为 0。

### C12：全仓验收与交接文档回填
改动：
- 执行 compile/test/scan 门禁；将结果回填本文件“执行结果”章节。
验收：
- 见第 6 节全部硬门禁通过。

## 5. 详细执行计划（操作步骤）

1. 先完成 C1，冻结扫描口径，避免多会话口径漂移。
2. 按 C2-C6 先打通 function 体系（这是当前最大阻塞点）。
3. 再做 C7-C8，统一变量/token/mechanism 主链到 V。
4. 接着完成 C9-C10，确保 callback/solver 主链 V 化，同时保留策略数值层例外。
5. 最后做 C11-C12，清除 math.symbol 非 adapter 残留并完成验收回填。
6. 每个提交完成后都必须更新 scripts/scan-p13-mainchain-result.json，并在本文件追加“提交号 -> 计数变化”。

## 6. 验收标准（硬门禁）

### 6.1 扫描门禁

必须为 0：
- 全仓 import as
- 全仓 Suppress(UNCHECKED_CAST)
- 全仓 typealias *Flt64
- ospf-kotlin-math/src/main/.../math/symbol 中非 adapter/flt64 的公开签名 Flt64
- ospf-kotlin-core/src/main/.../core/intermediate_symbol/function 公开签名 Flt64
- ospf-kotlin-core/src/main/.../core/model/callback 公开签名 Flt64
- ospf-kotlin-core/src/main/.../core/model/mechanism 公开签名 Flt64

允许保留（白名单）：
- core/solver/heuristic 中策略数值参数（概率、温度、权重、变异率、随机采样系数）
- 外部 solver adapter 边界（需有 SolverBoundary 注释）

### 6.2 构建与测试门禁

- mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile 必须通过。
- mvn -pl ospf-kotlin-core -am test 必须通过（允许历史已知失败仅在未新增时保持不扩散，并在报告中点名）。
- 对 function、mechanism、callback 至少新增 1 组非 Flt64 类型集成测试（如 Rational/Decimal 类型）。

## 7. 交接执行记录（由下个会话回填）

| 提交 | 范围 | 扫描变化 | 编译 | 测试 | 备注 |
|---|---|---|---|---|---|
| C1 | 基线脚本 |  |  |  |  |
| C2 | register 双阶段 |  |  |  |  |
| C3 | function 批次1 |  |  |  |  |
| C4 | function 批次2 |  |  |  |  |
| C5 | function 批次3 |  |  |  |  |
| C6 | 二次函数/Product |  |  |  |  |
| C7 | symbol/variable/token |  |  |  |  |
| C8 | mechanism |  |  |  |  |
| C9 | callback |  |  |  |  |
| C10 | solver/heuristic |  |  |  |  |
| C11 | math.symbol 收口 |  |  |  |  |
| C12 | 全仓验收 |  |  |  |  |

## 8. 会话交接状态（2026-05-06）

### 8.1 本次会话完成项

| 完成项 | 说明 |
|---|---|
| CanonicalInequality 泛型化 | `CanonicalInequality<T : Ring<T>>` 替代原 `CanonicalInequalityOf<Flt64>` typealias；Serde.kt 全部更新 |
| operation → adapter/flt64 import 修正 | MathInequalityDsl.kt、MechanismModel.kt、MetaModel.kt、SymbolQuantityOps.kt、测试文件共 6 处 stale import 修正 |
| MathFunctionSymbol 接口简化 | 双类型参数 `MathFunctionSymbol<V, F>` → 单类型参数 `MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V>` |
| MathFunctionSymbolBase 可见性修正 | internal → public，消除 public sub-interface exposes internal supertype 错误 |
| V-typed register 默认方法移除 | 因 JVM 类型擦除冲突（V 与 Flt64 在字节码层面签名相同导致 accidental override），移除接口中 V-typed registerAuxiliaryTokens/registerConstraints 默认方法 |
| 三模块编译通过 | ospf-kotlin-math、ospf-kotlin-quantities、ospf-kotlin-core 均编译成功 |

### 8.2 当前扫描指标

| 指标 | 当前值 | 目标值 |
|---|---:|---:|
| import as | 0 | 0 |
| Suppress(UNCHECKED_CAST) | 0 | 0 |
| typealias *Flt64 (math/symbol) | 1 (QuadraticInequality) | 0 |
| math/symbol 非 adapter 公开签名 Flt64 | 0 | 0 |
| core/intermediate_symbol/function Flt64 引用 | ~1258 | 0 |
| core/model/callback 公开签名 Flt64 | 4 | 0 |
| core/model/mechanism 公开签名 Flt64 | 未扫描 | 0 |
| operation/ 空桩文件 | 6 | 0 或合并 |

### 8.3 C1 待改进项

**状态：未开始**

- 需创建 `scripts/scan-p13-mainchain.ps1`，固化第 2 节全部计数口径
- 需创建 `scripts/scan-p13-mainchain-result.json`，含每类计数与命中路径
- 扫描口径需覆盖：import as、Suppress(UNCHECKED_CAST)、typealias *Flt64、非 adapter Flt64 公开签名、core 各子目录 Flt64 计数
- 脚本需在本地可重复执行

### 8.4 C2 待改进项

**状态：部分完成（接口简化已做，V 主链 register 未完成）**

核心问题：`MathFunctionSymbolBase` 的 register 方法仍为 Flt64 签名：
```kotlin
interface MathFunctionSymbolBase {
    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<Flt64>): Try
    fun registerConstraints(model: AbstractLinearMechanismModel<Flt64>): Try
}
```

**阻塞原因**：JVM 类型擦除使得 V-typed 与 Flt64-typed 同名方法在字节码层面签名冲突。曾尝试在 `MathFunctionSymbol<V>` 接口中添加 V-typed 默认方法，编译器报 accidental override。

**可行方案（待验证）**：
1. 将 `MathFunctionSymbolBase` 的 register 方法改为 V-typed，Flt64 实现通过 adapter 层转换
2. 或将 register 方法从接口中移出，改为独立注册函数（如 `fun registerFunctionSymbol(symbol: MathFunctionSymbol<V>, tokens: AddableTokenCollection<V>, model: AbstractLinearMechanismModel<V>)`）
3. 或使用不同方法名区分（如 `registerAuxiliaryTokensV` / `registerAuxiliaryTokensFlt64`），Flt64 版本标记为 adapter boundary

**其他待完成**：
- MetaModel.kt 在 tokenTable 拷贝上统一执行 registerAuxiliaryTokens
- MechanismModel.kt 模型构建后统一执行 registerConstraints
- 双阶段执行顺序需测试覆盖（至少 1 个线性、1 个二次函数符号）

### 8.5 C3 待改进项

**状态：未开始**

9 个基础算子文件仍有 Flt64 公开签名（扫描约 36 处命中）：
- Abs.kt、Ceiling.kt、Floor.kt、Cos.kt、Sin.kt、Sigmoid.kt、Mod.kt、Semi.kt、Rounding.kt

**改动要求**：
- evaluate/invoke/polyX/register 公开签名改为 V
- 去除 `intoValue(value: Flt64)` / `fromValue(value: Flt64)` 作为公开主路径
- Flt64 重载仅做薄委托，不再作为主入口

**模板参考**：改造后的文件应遵循以下模式：
```kotlin
class XxxFunction<V>(...) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    // V-typed 公开 API
    fun evaluate(values: Map<Symbol, V>): V? = ...
    // Flt64 便捷入口（薄委托）
    companion object {
        operator fun invoke(x: LinearPolynomial<Flt64>, ...): LinearFunctionSymbolAdapter<Flt64> = ...
    }
}
```

### 8.6 C4 待改进项

**状态：未开始**

8 个逻辑与条件文件仍有 Flt64 公开签名（扫描约 38 处命中）：
- If.kt、IfIn.kt、IfThen.kt、Imply.kt、Inequality.kt、OneOf.kt、SameAs.kt、And.kt

**改动要求**：
- 构造入口支持 `LinearPolynomial<V>` / `LinearIntermediateSymbol<V>`
- Flt64 重载仅做薄委托，不再作为主入口
- 上述文件无 `LinearPolynomial<Flt64>` 主入口签名

**注意**：逻辑函数（And、Imply 等）通常涉及布尔/二值变量，V 化时需确保比较运算通过 converter 而非 `asFlt64().toDouble()` 直比。

### 8.7 关键架构决策记录

1. **MathFunctionSymbolBase 保留 Flt64 签名**：当前 register 方法在 `MathFunctionSymbolBase` 上仍为 Flt64，这是 solver 边界（token collection 和 mechanism model 均为 Flt64 类型化）。V 主链 register 需要找到绕过 JVM 擦除冲突的方案。
2. **CanonicalInequality 已泛型化**：`CanonicalInequality<T : Ring<T>>` 替代了原 `CanonicalInequalityOf<Flt64>` typealias，math/symbol 非 adapter Flt64 公开签名已清零。
3. **operation/ 包拆分**：Flt64 特定扩展函数已移至 `adapter/flt64/`，`operation/` 仅保留 V-generic 内部操作。6 个空桩文件待清理或合并。
4. **LinearFunctionSymbolAdapter**：作为 `MathFunctionSymbol<V>` → `LinearIntermediateSymbol<V>` 的适配器，`pos`/`neg`/`polyX` 属性通过 `delegate as? SlackFunction<V>` 安全转换获取。
