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

## 当前审查结论

截至 2026-05-14，本轮核查确认：

1. `ospf-kotlin-core/src/test` 未发现 `assertTrue(true)` 占位断言。
2. `assertTrue(true)` 仍存在于 `ospf-kotlin-example/src/test` 的历史示例测试中，共 22 处，主要集中在 `linear_function/*Test.kt`、`quadratic_function/SemiTest.kt` 以及顶层 `QuadraticTest`、`FrameworkDemoTest`、`HeuristicDemoTest`。
3. 这些遗留占位测试不影响 core 泛型化主链路验收结论，但会造成“测试仍然只是 smoke”的外观误判，应作为下一阶段测试债务收口。

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

## 下一步计划

### P0：清理 example 历史空 smoke 测试

背景：

- `ospf-kotlin-core` 泛型化主链路已具备实质验收。
- `ospf-kotlin-example` 中仍有一批历史测试只包含 `assertTrue(true)`。
- 这些测试不验证示例可用性，也容易让后续审查误以为泛型化验收仍停留在空 smoke。

计划：

1. 盘点 `ospf-kotlin-example/src/test` 中全部 `assertTrue(true)`。
2. 按测试性质分类：
   - 纯建模示例：改为调用示例构建路径并断言 build/dump 结构。
   - 函数符号示例：断言变量、token、约束、目标或关键系数。
   - 需要外部 solver 的示例：先避免依赖真实 solver，改为 build/dump 或放入显式 integration profile。
   - framework/heuristic 示例：若当前难以稳定运行，先改成结构化构造验收或明确隔离。
3. 将每个空 smoke 替换为至少一个真实代码路径调用和一个可观察断言。
4. 保留 `CoreDemoTest` 轻量 smoke，但继续由 `GenericNumberDemoTest` 承担详细语义验收。

详细步骤：

1. 扫描占位测试：
   - 使用 `Select-String -Pattern 'assertTrue\(true\)'` 生成文件清单。
   - 记录每个文件对应的 example main 源文件或可替代的构建入口。
2. 优先处理纯函数示例：
   - `ospf-kotlin-example/src/test/.../linear_function/*Test.kt`
   - `ospf-kotlin-example/src/test/.../quadratic_function/SemiTest.kt`
   - 每个测试至少断言：构建成功、约束数量、目标方向、关键 token 或关键系数之一。
3. 再处理顶层示例：
   - `QuadraticTest`
   - `FrameworkDemoTest`
   - `HeuristicDemoTest`
   - 对依赖外部 solver 或运行成本高的场景，优先抽出 build-only 验收路径。
4. 如现有 example main 文件不暴露结构化结果：
   - 增加 summary 返回值或轻量 helper。
   - helper 只服务示例验收，不引入真实 solver 依赖。
5. 保持测试命名清晰：
   - 轻量 smoke 使用 `SmokeTest` 后缀时，必须断言真实返回值。
   - 详细语义测试使用 `Should...` 命名，表达断言目标。

验收标准：

1. `ospf-kotlin-example/src/test` 下 `assertTrue(true)` 数量为 0。
2. 所有保留的 smoke 测试都调用真实代码路径，并至少断言一个输出、状态或结构结果。
3. 纯建模示例不依赖外部 solver 即可通过。
4. 需要外部 solver 的测试不会进入默认 example 回归路径，除非提供稳定的测试桩或 profile。
5. `GenericNumberDemoTest` 继续覆盖四类型、线性/二次构建与关键系数。

### P1：新增 example 空 smoke 防回流门禁

背景：

- core 门禁已能防止 `.toDouble()`、`Flt64` 主 API、unchecked cast 等问题回流。
- 目前尚无门禁防止 example 测试重新引入 `assertTrue(true)`。

计划：

1. 在现有门禁脚本或新增轻量脚本中扫描 example test。
2. 禁止 `assertTrue(true)`、`assertThat(true).isTrue()` 等空断言模式。
3. 输出违规文件与行号，方便修复。

详细步骤：

1. 优先扩展 `ospf-kotlin-core/scripts/check-c8-guards.ps1`：
   - 新增 example test root：`ospf-kotlin-example/src/test`。
   - 新增规则：`No empty smoke assertions in example tests`。
2. 首版规则至少覆盖：
   - `assertTrue(true)`
   - `Assertions.assertTrue(true)`
   - `kotlin.test.assertTrue(true)`
3. 若未来需要兼容临时占位：
   - 必须使用显式白名单 map。
   - 白名单默认应为空。
4. 在 README 或 daily.md 中补充门禁命令。

验收标准：

1. 新门禁在当前清理后通过。
2. 任意 example test 新增 `assertTrue(true)` 时门禁失败，并输出文件路径与行号。
3. P6/P7 原有门禁仍保持通过。

### P2：可选补强 example 泛型用户样例覆盖

背景：

- `GenericNumberDemoTest` 已覆盖四类型 build/dump summary。
- 后续可进一步把用户侧示例从“构建可用”推进到“关键函数符号示例可用”。

计划：

1. 从 example 中选择 2 到 3 个代表函数示例。
2. 为每个示例提供 build-only summary。
3. 断言用户可观察的模型结构，而非依赖真实 solver。

候选范围：

1. 线性函数：abs/max/min/if/slack range。
2. 二次函数：quadratic/semi/product。
3. framework demo：只验 DTO、策略构造或 model build，不默认接外部 solver。

验收标准：

1. example 中至少 2 个非 `GenericNumberDemo` 示例具备真实结构断言。
2. 默认回归不需要本机安装 Gurobi/SCIP 等外部 solver。
3. 删除对应示例构建逻辑或关键字段时测试失败。

## 建议执行顺序

1. 先清理 `assertTrue(true)`，把 example 测试外观债务清零。
2. 再补 example 空 smoke 门禁，防止回流。
3. 最后按需要扩展更多用户样例的结构化验收。

## 建议起手命令

1. `git status -sb`
2. `Get-ChildItem -Recurse -Path 'ospf-kotlin-example/src/test' -Filter *.kt | Select-String -Pattern 'assertTrue\(true\)'`
3. `mvn --% -pl ospf-kotlin-example -Pcore-demo-only -Dtest=CoreDemoTest,GenericNumberDemoTest -Dsurefire.failIfNoSpecifiedTests=false clean test`
4. `powershell -ExecutionPolicy Bypass -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`
5. `powershell -ExecutionPolicy Bypass -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`
