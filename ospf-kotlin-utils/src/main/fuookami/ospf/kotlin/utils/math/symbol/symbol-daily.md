# ospf-kotlin-utils `math/symbol` 日拆分计划（精简版）

## 全局前提（必须保留）

本计划默认语境是**破坏性大版本（breaking release）**：允许 API 与实现做不兼容变更，目标是一次性全量替换，不保留兼容层或过渡路径。

跨语言接口迁移规则：新增或迁移接口前，必须先判断该接口是否是 Rust 语言机制（所有权、借用、生命周期、引用运算等）才需要的设计；若是，则不在 Kotlin 中做同形迁移，改为 Kotlin 语义下的等价能力设计。

## 当前唯一目标

完成 `symbol` 模块主路径收口：

1. `monomial/polynomial/operation` 不再并行维护旧 `Flt64` 主算法实现。
2. 泛型实现成为唯一主路径，`Flt64` 仅作为桥接层或别名存在。
3. utils 与 core 回归测试在该收口后保持通过。

## 未完成缺口

- [ ] `monomial/*` 与 `polynomial/*` 仍为 `Flt64` 结构定义，未完成对泛型主路径的彻底替换策略固化。
- [x] `operation/CombineTerms.kt` 已收口为 pure generic 转发（monomial/polynomial 路径）。
- [x] `operation/Evaluate.kt` polynomial 级 `evaluate/evaluateRet/partialEvaluate(provider)` 已收口到 generic 主路径。
- [x] `operation/Differentiate.kt` 已完成 linear/quadratic monomial & polynomial 路径收口；Canonical 保留高次语义路径。
- [x] `operation/MatrixForm.kt` 正向矩阵化入口已切换为 generic combine + matrix 转换主路径。
- [x] `operation/Convert.kt` 已审计为 generic 主路径（monomial/polynomial/inequality 转换与 `moveAllToLhs` 均走 generic 桥接）。

## Day 15：主路径收口计划

### A. 第一批（先做）

- [x] `operation/CombineTerms.kt`
- [x] `operation/Evaluate.kt`
- [x] `operation/Differentiate.kt`
- [x] `operation/MatrixForm.kt`

执行要求：

1. 对外 API 签名暂保持稳定。
2. 内部实现统一改为：`toGeneric* -> generic 主算法 -> to*`。
3. 删除重复本地 `Flt64` 算法实现，避免双路径漂移。

### B. 第二批（联动 core 后做）

- [x] `operation/Compile.kt`
- [x] `operation/Convert.kt`

执行要求：

1. 不直接破坏 core adapter 现有调用入口。
2. 若新增泛型签名，需保留最小 `Flt64` 兼容桥接并补测试。
3. 完成后再执行 core 定向与模块级回归。

### C. 暂缓范围（本轮不纳入主算法退场）

- `adapter/ValueProvider.kt`
- `operation/Latex.kt`
- `serde/SymbolExpr.kt`

说明：以上模块当前承担 `Flt64` 上下文/展示/序列化职责，不属于“主算法并行实现”问题。

## 验收标准（仅保留未完成）

- [ ] `symbol` 的 `monomial/polynomial/operation` 不再并行维护旧 `Flt64` 主算法实现。
- [x] `Linear/Quadratic/Canonical` 在 `Flt64` 与至少一个非浮点类型上共用同一主算法路径。
- [x] `mvn -pl ospf-kotlin-utils -DskipITs test` 通过。
- [x] `mvn -pl ospf-kotlin-core -DskipITs test` 通过。

## 验收命令（待执行）

- [x] `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=LinearGenericTest,QuadraticGenericTest,CanonicalGenericTest,MultiCoefficientGenericTest,CompileGenericTest" test`
- [x] `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=CombineTermsTest,EvaluateTest,DifferentiateTest,MatrixFormTest,ConvertTest,CanonicalOperationTest,InequalityTest" test`
- [x] `mvn -pl ospf-kotlin-utils -DskipITs test`
- [x] `mvn -pl ospf-kotlin-core -DskipITs "-Dtest=AdapterRoundTripTest,AdvancedAdapterFeatureTest" test`
- [x] `mvn -pl ospf-kotlin-core -DskipITs test`

## 执行记录模板（待回填）

### 本轮代码变更

- [x] 第一批完成：`CombineTerms/Evaluate/Differentiate/MatrixForm`
- [x] 第二批完成：`Compile/Convert`

变更文件：

- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math/symbol/operation/CombineTerms.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math/symbol/operation/Differentiate.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math/symbol/operation/Evaluate.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math/symbol/operation/MatrixForm.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math/symbol/operation/Compile.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math/symbol/generic/LinearGeneric.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math/symbol/generic/QuadraticGeneric.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math/symbol/generic/CanonicalGeneric.kt`

### 回归结果

- [x] utils 定向回归通过
- [x] utils 模块主代码编译通过（`mvn -pl ospf-kotlin-utils -DskipTests compile`）
- [x] core 定向回归通过
- [x] core 模块回归通过

命令输出摘要：

- `mvn -pl ospf-kotlin-utils -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-utils -DskipTests compile`：通过（在 `Evaluate.kt` 与 `MatrixForm.kt` 收口后复验通过）。
- `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=CombineTermsTest" test`：通过（4 tests, 0 failure）。
- `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=LinearGenericTest,QuadraticGenericTest,CanonicalGenericTest,MultiCoefficientGenericTest,CompileGenericTest" test`：通过（17 tests, 0 failure）。
- `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=CombineTermsTest,EvaluateTest,DifferentiateTest,MatrixFormTest,ConvertTest,CanonicalOperationTest,InequalityTest" test`：通过（47 tests, 0 failure）。
- `mvn -pl ospf-kotlin-utils -DskipITs test`：通过（333 tests, 0 failure）。
- `mvn -pl ospf-kotlin-core -DskipITs "-Dtest=AdapterRoundTripTest,AdvancedAdapterFeatureTest" test`：通过（8 tests, 0 failure）。
- `mvn -pl ospf-kotlin-core -DskipITs test`：通过（33 tests, 0 failure）。
- `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=LinearGenericTest,QuadraticGenericTest,CanonicalGenericTest,CompileGenericTest,CombineTermsTest,EvaluateTest,DifferentiateTest,MatrixFormTest,ConvertTest" test`：通过（54 tests, 0 failure）。
- `mvn -pl ospf-kotlin-utils -DskipITs clean test`：通过（333 tests, 0 failure）。

### 风险与阻塞

- [ ] 外部依赖波动（如 Maven 仓库连接）
- [ ] core 适配签名联动风险
- [ ] 其余阻塞项

说明：

- `TmpDefaultTypeArgCheck.kt` 已修复，`test-compile` 阻塞已解除。

## 未完成事项汇总

1. 数据模型层未收口：`monomial/*`、`polynomial/*` 仍为 `Flt64` 结构定义，尚未形成“泛型主路径 + 最薄桥接”的最终策略。

### 数据模型层收口备注（2026-03-27）

- 已完成破坏式收口：移除 `MonomialV2.kt`、`PolynomialV2.kt`、`V2Bridge.kt` 与 `V2BridgeTest.kt`，代码中不再保留 `V2` 概念。
- `operation` 与 `generic` 相关调用点已回到直接 `Linear/Quadratic/Canonical` 主类型与 `toGeneric*` 桥接，不再经过 `toV2/toLegacy`。
- 已执行 `mvn -pl ospf-kotlin-utils -DskipITs "-Dtest=LinearGenericTest,QuadraticGenericTest,CanonicalGenericTest,CompileGenericTest,CombineTermsTest,EvaluateTest,DifferentiateTest,MatrixFormTest,ConvertTest" test`：通过（54 tests, 0 failure）。
- 已执行 `mvn -pl ospf-kotlin-utils -DskipITs clean test`：通过（333 tests, 0 failure）。
- 已尝试将 `Linear/Quadratic/Canonical Monomial/Polynomial` 直接改为泛型定义；实践中发现 Kotlin 不支持当前使用的“带约束类型参数默认值”写法，且直接切换会引发 `serde/inequality/core` 全链路大规模类型参数改造。
- 该尝试已回退至可编译基线，当前代码保持稳定可构建。
- 已执行 `mvn -pl ospf-kotlin-utils -DskipTests compile`：通过（回退后复验）。
