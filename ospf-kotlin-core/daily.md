# core 泛型化验收交接（2026-05-14）

## 目标

`ospf-kotlin-core` 的泛型化主链路已从“能编译、能冒烟运行”推进到“可作为长期防回归验收基线”。下一阶段目标是把验收外观继续收口：清理 `ospf-kotlin-example` 中遗留的空 smoke 测试，避免 `assertTrue(true)` 这类占位测试掩盖真实状态。

当前验收核心保持不变：`Flt64`、`Rtn64`、`FltX`、`RtnX` 四种数值类型必须在 core 主链路中可用，且测试要能证明以下链路不是表面泛型：

1. 建模 API 可用：变量、约束、目标、函数符号、机制模型均可用 `V` 构造。
2. 中间模型语义正确：约束系数、rhs、目标函数、bounds、fixedVariables、token 映射在 dump 后不丢失、不退化。
3. 函数符号可用：evaluate、prepare、registerAuxiliaryTokens、registerConstraints 对四类型有语义断言。
4. 求解桥接可用：`solveV`、solution pool、输出 solution、token 回填对四类型有显式回归测试。
5. example 可作为用户样例：泛型 demo 必须进入测试，并且测试必须调用真实代码路径、断言可观察结果，而不是只保留 `assertTrue(true)`。

外部 solver adapter 若只能处理 double，可继续以 `Flt64` 作为 solver 边界类型；但建模层、机制模型层、函数符号主 API、solver `V` 入口与输出主视图不应退化成 `Flt64` 专属 API。

## 总体原则

1. 泛型主类型统一记为 `V`，约束为 `where V : RealNumber<V>, V : NumberField<V>`。
2. `Flt64` 仅允许出现在兼容层、solver 边界、`adapter.flt64` 包、测试基准和明确的数值转换边界。
3. `IntoValue<V>` 是边界转换能力，不应替代内部泛型数据结构的语义测试。
4. `Token`、`Cell`、constraint、sub-object 若内部以 `Flt64` 做 solver 兼容存储，必须提供稳定的 `V` 视图，并由测试覆盖。
5. `solveV` 是 core 对外泛型求解主入口；若内部需要转成 `Flt64`，必须有明确转换实现和测试，不应只依赖 unchecked cast 作为“转换”。
6. `FeasibleSolverOutput<V>` 的主输出语义应清晰：solution 与目标主视图必须是 `V`；legacy `obj/possibleBestObj/bestBound` 字段只作为兼容视图存在。
7. 测试不能只断言“成功”或“数量大于 0”；关键路径必须断言类型、系数、rhs、目标值、solution、bounds、回调或状态透传。
8. example 测试允许保留轻量 smoke 层，但 smoke 必须调用真实示例代码路径，并至少断言一个结构化结果；禁止空断言 `assertTrue(true)`。
9. 门禁应覆盖 `.toDouble()`、`Flt64` 主 API 泄漏、unsafe cast 聚集点、已知协程风险点和 example 空 smoke 回流。

## 已完成事项摘要

1. `solveV` 四类型桥接已补齐：
   - `GenericSolveVBridgeTest` 覆盖线性/二次的 `solveV(triad|tetrad)`、`solveV(mechanism)`、`solveV(..., solutionAmount)`。
   - 已断言四类型 solution、solution pool、`objValue/possibleBestObjValue/bestBoundValue` 转换与 callback 调用。

2. `FeasibleSolverOutput<V>` 主输出类型已收口：
   - 新增并验证 `objValue`、`possibleBestObjValue`、`bestBoundValue`。
   - `FeasibleSolverOutputLegacyFallbackGuardTest` 防止非 `Flt64` solution 静默回退到错误目标值类型。

3. MetaModel 与 MechanismModel 语义测试已强化：
   - `GenericLinearMetaModelBuildTest`、`GenericQuadraticMetaModelBuildTest` 已覆盖四类型。
   - 已断言 constraint sign、rhs、coefficient 类型和值、objective constant/cells，以及 fixedVariables 后的 triad/tetrad dump 结果。

4. 函数符号泛型注册测试已从结构冒烟提升到语义冒烟：
   - 已覆盖 abs/and/or/not/xor/if、piecewise、conditional、rounding、same-as、satisfied amount、quadratic product/min/masking/in-step 等路径。
   - 注册后断言新增约束、coefficient 类型、rhs 类型、sign 可比性与关键输入 token 参与。

5. Token、Cache、Bound 与数值转换策略已补齐四类型回归：
   - `GenericTokenBridgeTest` 覆盖 `result/resultFlt64/setResultFromV/resultAsV/lowerBoundAsV/upperBoundAsV/containsInBounds`。
   - `GenericTokenCacheTest` 覆盖 `cacheSolverIfNotCached` 的 solution/fixedValues key 隔离。
   - `GenericNumberConverterTest`、`SolveValuePrecisionPolicyTest` 固定 converter round-trip 与 strict/allow rounding 策略。

6. example 泛型 demo 已进入测试闭环：
   - `GenericNumberDemo.runBuildAndDump()` 已返回结构化 summary。
   - `CoreDemoTest` 作为轻量 smoke 调用 demo。
   - `GenericNumberDemoTest` 负责四类型、线性/二次构建和关键系数断言。
   - `core-demo-only` profile 已隔离历史示例源码，仅验证 `GenericNumberDemo` 闭环。

7. 门禁与文档已有基础：
   - P6/P7 门禁通过，覆盖 `.toDouble()`、`Flt64` 主 API 泄漏、unchecked cast 等回流风险。
   - README 双语互链与泛型 API 迁移说明已补齐。

8. `ProductFunction` build-only 约束命名已补齐稳定前缀：
   - `registerConstraints` 生成的二次等式名称改为 `${name}_eq`，避免空名称导致结构化断言不稳定。
   - `QuadraticProductBuildOnlyStructureTest` 已据此通过，确保 product 示例具备可追踪的约束标签。

## 当前审查结论

截至 2026-05-14，本轮核查确认：

1. `ospf-kotlin-core/src/test` 未发现 `assertTrue(true)` 占位断言。
2. `ospf-kotlin-example/src/test` 中历史 `assertTrue(true)` 已清零（当前扫描 0 命中）。
3. example 层已从空 smoke 收口为结构化断言，默认回归路径保持 build-only，不要求外部 solver。

本轮已验证通过：

1. core 泛型关键窄测：
   - `mvn --% -pl ospf-kotlin-core -am -Dtest=GenericSolveVBridgeTest,GenericSolverOutputConversionTest,SolverOutputCompatibilityTest,FeasibleSolverOutputLegacyFallbackGuardTest,GenericLinearMetaModelBuildTest,GenericQuadraticMetaModelBuildTest,GenericTokenBridgeTest,GenericTokenCacheTest,GenericNumberConverterTest,SolveValuePrecisionPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test`
2. 函数注册默认标签回归：
   - `mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolGenericRegistrationTest,FunctionSymbolConditionalGenericRegistrationTest,FunctionSymbolPiecewiseGenericRegistrationTest,FunctionSymbolSameAsGenericRegistrationTest,FunctionSymbolSatisfiedAmountInequalityGenericRegistrationTest,FunctionSymbolConstraintInputVFactoryTest,FunctionSymbolRoundingGenericRegistrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
3. rounding 慢测全集：
   - `mvn --% -pl ospf-kotlin-core -Dtest=FunctionSymbolRoundingGenericRegistrationTest -Dospf.kotlin.test.excludedGroups= -Dsurefire.failIfNoSpecifiedTests=false test`
4. example 泛型 demo 闭环：
   - `mvn --% -pl ospf-kotlin-example -Pcore-demo-only -Dtest=CoreDemoTest,GenericNumberDemoTest -Dsurefire.failIfNoSpecifiedTests=false clean test`
5. P6/P7 门禁：
   - `powershell -ExecutionPolicy Bypass -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`
   - `powershell -ExecutionPolicy Bypass -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`
6. example 空 smoke 清零与结构化补强：
   - `Get-ChildItem -Recurse -Path 'ospf-kotlin-example/src/test' -Filter *.kt | Select-String -Pattern 'assertTrue\\(true\\)'`（0 输出）
   - `build_project(filesToRebuild=[example 已修改测试集合])`（编译通过）
   - `build_project(filesToRebuild=[LinearFunctionBuildOnlyStructureTest, ConditionalFunctionBuildOnlyStructureTest, QuadraticProductBuildOnlyStructureTest])`（编译通过）
7. example build-only 结构化测试回归：
   - `mvn --% -pl ospf-kotlin-example -am -Pbuild-only-function-tests -Dtest=LinearFunctionBuildOnlyStructureTest,ConditionalFunctionBuildOnlyStructureTest,QuadraticProductBuildOnlyStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`（3/3 通过，报告时间：2026-05-14 11:33，来源为仓库 surefire 报告）
   - 说明：后续审阅复跑该命令在 5 分钟窗口内发生超时，不影响既有通过记录，但不应表述为“审阅当轮 Maven 完整复跑通过”。
8. example 泛型 demo 闭环（联编）：
   - `mvn --% -pl ospf-kotlin-example -am -Pcore-demo-only -Dtest=CoreDemoTest,GenericNumberDemoTest -Dsurefire.failIfNoSpecifiedTests=false clean test`（2/2 通过）
9. core 泛型关键窄测（联编）：
   - `mvn --% -pl ospf-kotlin-core -am -Dtest=GenericSolveVBridgeTest,GenericSolverOutputConversionTest,SolverOutputCompatibilityTest,FeasibleSolverOutputLegacyFallbackGuardTest,GenericLinearMetaModelBuildTest,GenericQuadraticMetaModelBuildTest,GenericTokenBridgeTest,GenericTokenCacheTest,GenericNumberConverterTest,SolveValuePrecisionPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test`（15/15 通过）
10. 函数注册默认标签回归（联编）：
   - `mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolGenericRegistrationTest,FunctionSymbolConditionalGenericRegistrationTest,FunctionSymbolPiecewiseGenericRegistrationTest,FunctionSymbolSameAsGenericRegistrationTest,FunctionSymbolSatisfiedAmountInequalityGenericRegistrationTest,FunctionSymbolConstraintInputVFactoryTest,FunctionSymbolRoundingGenericRegistrationTest -Dsurefire.failIfNoSpecifiedTests=false test`（13/13 通过）
11. rounding 慢测全集：
   - `mvn --% -pl ospf-kotlin-core -Dtest=FunctionSymbolRoundingGenericRegistrationTest -Dospf.kotlin.test.excludedGroups= -Dsurefire.failIfNoSpecifiedTests=false test`（4/4 通过）
12. P6/P7 门禁复跑：
   - `powershell -ExecutionPolicy Bypass -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`（通过）
   - `powershell -ExecutionPolicy Bypass -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`（通过）

## 收口结论（审阅版）

### 范围闭环状态

1. `P0`（example 空 smoke 清理）已完成：
   - `ospf-kotlin-example/src/test` 下 `assertTrue(true)` 清零。
   - 历史 smoke 已替换为真实代码路径调用与结构化断言。
2. `P1`（example 空 smoke 防回流门禁）已完成：
   - `check-c8-guards.ps1` 增加 `P1-EX-1` 规则。
   - 规则覆盖 `assertTrue(true)`、`Assertions.assertTrue(true)`、`kotlin.test.assertTrue(true)` 及 `assertThat(true).isTrue()`。
3. `P2`（example build-only 样例补强）已完成：
   - 新增/补强 3 个 build-only 结构化测试：
     - `linear_function/LinearFunctionBuildOnlyStructureTest`
     - `linear_function/ConditionalFunctionBuildOnlyStructureTest`
     - `quadratic_function/QuadraticProductBuildOnlyStructureTest`
   - 已验证默认回归不依赖外部 solver。

### 验收标准判定

1. example 空断言清零：通过。
2. smoke 结构化断言：通过。
3. 门禁防回流：通过（P6/P7 复跑通过）。
4. core 泛型主链路回归：通过（窄测、函数注册、rounding 慢测均通过）。
5. 默认 example build-only 回归：通过（基于既有 surefire 报告；审阅当轮复跑超时，未形成新的完整 Maven 通过记录）。

### 与原计划差异说明

1. 本轮额外完成了 `ProductFunction` 约束命名稳定化：
   - `registerConstraints` 生成二次等式名称为 `${name}_eq`，减少结构断言脆弱性。
2. 为打通 `-pl ospf-kotlin-example -am` 联编链路，补充了机制层可见性与 DSL 可见性修复：
   - `MechanismModel` cut 生成函数可见性放开。
   - `MathInequalityDsl` 中 `internal infix` 暴露为 `infix`，满足跨模块调用。
   - gantt produce-context 两处 `constraintsOfGroup` 调用签名对齐。

## 残余风险与可选后续

1. `MathInequalityDsl.kt` 变更面较大（主要为可见性提升），建议后续增加针对 DSL 公开面的 API 边界测试，以防未来误收缩。
2. JVM `CodeHeap` 在长链路 Maven 回归中多次告警（不影响当前结果），若作为常态 CI 路径建议后续调高 `NonProfiledCodeHeapSize`。
3. 若后续希望进一步压缩 default 回归耗时，可将当前 `build-only-function-tests` profile 纳入标准审阅脚本。

## 审阅建议

1. 优先审阅 `P1-EX-1` 门禁规则与误报/漏报风险。
2. 审阅 `example` 测试替换是否覆盖真实用户路径（尤其是 `FrameworkDemoTest`、`HeuristicDemoTest`、`QuadraticTest`）。
3. 审阅 `MechanismModel` 与 `MathInequalityDsl` 的可见性调整是否符合长期 API 策略。
