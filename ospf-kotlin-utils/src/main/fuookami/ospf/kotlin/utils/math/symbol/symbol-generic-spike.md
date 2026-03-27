# symbol generic spike（2026-03-27）

## 已落地能力

1. 代数概念层与 law-check 已落地并有测试：
   - `math/algebra/concept/*`
   - `math/algebra/law/*`
   - `operator/Tolerance.kt`
2. `symbol/generic` 基础类型已落地：
   - `Coefficient.kt`
   - `Exponent.kt`
3. `Linear/Quadratic/Canonical` 泛型结构与基础运算已落地：
   - `LinearGeneric.kt`
   - `QuadraticGeneric.kt`
   - `CanonicalGeneric.kt`
4. 泛型微分与矩阵化辅助已落地：
   - `DifferentiateGeneric.kt`
   - `MatrixFormGeneric.kt`
5. `Flt64` 主路径已完成部分 operation 挂接：
   - `operation/CombineTerms.kt`
   - `operation/Evaluate.kt`（polynomial 级 map/ordered/partialEvaluate(map)）
   - `operation/Differentiate.kt`（Linear/Quadratic derivative/gradient）
   - `operation/MatrixForm.kt`（Linear/Quadratic toMatrixForm）
6. 关键回归测试已覆盖并通过：
   - `LinearGenericTest.kt`
   - `QuadraticGenericTest.kt`
   - `CanonicalGenericTest.kt`
   - 以及 `CombineTermsTest`、`EvaluateTest`、`DifferentiateTest`、`MatrixFormTest`、`CanonicalOperationTest` 组合回归。

## 剩余问题

1. `operation/*` 仍是 `Flt64` 主签名，generic 能力目前通过桥接复用，尚未形成对外泛型 API。
2. `Canonical` 的高次语义路径（尤其 Hessian 的非二次失败分支）还未抽象到 generic 统一契约。
3. parser/latex/serde/dsl 仍以现有表达式模型工作，尚未引入 generic coefficient/exponent 序列化策略。
4. core 侧 Day 11 的模块级回归仍受外部仓库依赖中断影响，尚无完整 core 证据链。
5. `operation/Compile.kt` 仍未接入 generic 算子实现。

## 建议下一步

1. 先完成 `operation/Compile.kt` 泛型化收口（至少 Linear/Quadratic）。
2. 在不破坏现有调用的前提下，新增一层 `Generic*Operation` 对外 API，再逐步迁移旧 API。
3. 为 generic 路径补一组端到端回归：`evaluate -> gradient -> matrixForm -> compile` 一致性校验。
4. 外部仓库恢复后补跑 core 回归，验证 adapter 链与 generic bridge 的兼容性。

