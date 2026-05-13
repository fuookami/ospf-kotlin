# core 泛型化验收交接（2026-05-13）

## 目标

将 `ospf-kotlin-core` 的泛型化从“能编译、能冒烟运行”推进到“可作为验收标准长期防回归”。

验收核心是：`Flt64`、`Rtn64`、`FltX`、`RtnX` 四种数值类型必须在 core 主链路中可用，且测试要能证明以下链路不是表面泛型：

1. 建模 API 可用：变量、约束、目标、函数符号、机制模型均可用 `V` 构造。
2. 中间模型语义正确：约束系数、rhs、目标函数、bounds、fixedVariables、token 映射在 dump 后不丢失、不退化。
3. 函数符号可用：evaluate、prepare、registerAuxiliaryTokens、registerConstraints 对四类型均有语义断言。
4. 求解桥接可用：`solveV`、solution pool、输出 solution、token 回填对四类型有显式回归测试。
5. example 可作为用户样例：`ospf-kotlin-example` 中的泛型 demo 必须进入测试，而不是只作为可编译文件存在。

外部 solver adapter 若只能处理 double，可继续以 `Flt64` 作为 solver 边界类型；但建模层、机制模型层、函数符号主 API、solver `V` 入口与输出主视图不应退化成 `Flt64` 专属 API。

## 总体原则

1. 泛型主类型统一记为 `V`，约束为 `where V : RealNumber<V>, V : NumberField<V>`。
2. `Flt64` 仅允许出现在兼容层、solver 边界、`adapter.flt64` 包、测试基准和明确的数值转换边界。
3. `IntoValue<V>` 是边界转换能力，不应替代内部泛型数据结构的语义测试。
4. `Token`、`Cell`、constraint、sub-object 若内部以 `Flt64` 做 solver 兼容存储，必须提供稳定的 `V` 视图，并由测试覆盖。
5. `solveV` 是 core 对外泛型求解主入口；若内部需要转成 `Flt64`，必须有明确转换实现和测试，不应只依赖 unchecked cast 作为“转换”。
6. `FeasibleSolverOutput<V>` 的主输出语义应清晰：solution 必须是 `V`；目标值、best bound 等字段若暂时仍为 `Flt64`，必须标记为待收口并有兼容迁移计划。
7. 测试不能只断言“成功”或“数量大于 0”；关键路径必须断言类型、系数、rhs、目标值、solution、bounds、回调或状态透传。
8. 门禁应覆盖 `.toDouble()`、`Flt64` 主 API 泄漏、unsafe cast 聚集点和已知协程风险点，防止回流。

## 已完成事项

1. core 泛型化基础已建立：
   - `LinearMetaModel<V>`、`QuadraticMetaModel<V>` 已支持四类型基础构建与机制模型 dump。
   - `GenericNumberCases` 已提供 `Flt64`、`Rtn64`、`FltX`、`RtnX` 的测试用例和 converter。
   - 线性/二次泛型 build 测试已存在：`GenericLinearMetaModelBuildTest`、`GenericQuadraticMetaModelBuildTest`。

2. 函数符号泛型覆盖已有雏形：
   - 已覆盖部分 evaluate/prepare 路径：first、product、quadratic、piecewise、discrete、slack/masking/satisfied amount 等。
   - 已覆盖部分 register 路径：线性函数符号、二次函数符号、piecewise、conditional、rounding、same-as、satisfied amount 等。
   - 部分测试已断言生成 constraint 的 cell coefficient 类型保持为 `V`，不是直接泄漏 `Flt64`。

3. Benders cut 相关泛型回归已有覆盖：
   - `GenericBendersCutRegressionTest` 覆盖线性/二次 cut by-id 与 direct call 对齐。
   - `BendersCutTypedByIdApiTest` 覆盖 typed by-id API 与 legacy by-id 的兼容。

4. core 边界治理已有基础：
   - `.toDouble()` 主链路门禁已收敛到 `SolveValueConversionContext.kt`。
   - `SolveValueConversionContextTest`、`SolveValueValidationTest` 已覆盖 NaN/infinity 的 strict/allow 策略。
   - `CoreToDoubleBridgeGuardTest` 已防止 core 主代码新增散落 `.toDouble()`。

5. framework 与异步治理已完成较多前置工作：
   - framework solver `V` bridge、Benders/ColumnGeneration 同步异步桥接已补测试。
   - `GlobalScope`、`DelicateCoroutinesApi`、`isClosedForSend`、`ClosedSendChannelException` 已从主要模块清理并纳入门禁。

6. example 中已有泛型 demo 文件：
   - `GenericNumberDemo` 已展示四类型线性/二次 build and dump。
   - 但该 demo 尚未被 example 测试调用，目前不能算验收闭环。

## 待办事项

### P0：补齐 `solveV` 四类型求解验收

背景：

- `AbstractLinearSolver.solveV(model: MechanismModel<V>, converter)` 和 `AbstractQuadraticSolver.solveV(model: MechanismModel<V>, converter)` 当前通过 `SolverBoundaryCasts.cast*MechanismModelStar` 进入 `Flt64` 求解路径。
- 注释表达为“convert to Flt64”，但实现本质是 unchecked cast。
- 当前测试没有覆盖四类型从 `MetaModel<V>`/`MechanismModel<V>` 走到 `solveV` 输出的完整链路。

详细步骤：

1. 新增 fake linear solver 与 fake quadratic solver 测试桩。
2. 覆盖 `solveV(LinearTriadModelView, converter)` 与 `solveV(QuadraticTetradModelView, converter)`。
3. 覆盖 `solveV(MechanismModel<V>, converter)`。
4. 覆盖 `solveV(..., solutionAmount, converter)` 的 solution pool 转换。
5. 对 `Flt64`、`Rtn64`、`FltX`、`RtnX` 分别断言：
   - 返回 `Ret` 为 `Ok`。
   - `solution` 元素运行时类型与 `V` 一致。
   - `solution` 数值等于 fake solver 返回的 `Flt64` 结果经 converter 转换后的值。
   - solution pool 每条 solution 都转换为 `V`。
   - callback 若存在，应收到同一份转换后结果。

改动清单：

- 新增测试：`ospf-kotlin-core/src/test/.../solver/GenericSolveVBridgeTest.kt`
- 必要时新增测试工具：`GenericFakeLinearSolver`、`GenericFakeQuadraticSolver`
- 如测试暴露 cast 问题，修改：
  - `ospf-kotlin-core/src/main/.../solver/LinearSolver.kt`
  - `ospf-kotlin-core/src/main/.../solver/QuadraticSolver.kt`
  - `ospf-kotlin-core/src/main/.../model/mechanism/MechanismModel.kt`
  - `ospf-kotlin-core/src/main/.../intermediate_symbol/SolverBoundaryCasts.kt`

验收标准：

- `mvn --% -pl ospf-kotlin-core -am -Dtest=GenericSolveVBridgeTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
- 四类型均覆盖线性、二次、solution pool。
- 测试能在删除 converter 转换、误用 `Flt64` solution 或错误 cast 时失败。

### P0：明确并收口 `FeasibleSolverOutput<V>` 主输出类型

背景：

- 当前 `FeasibleSolverOutput<V>` 只有 `solution: Solution<V>` 是泛型。
- `obj`、`possibleBestObj`、`gap`、`bestBound`、`mipGap` 仍是 `Flt64`。
- 这与“约束/目标/解输出主类型签名中不泄漏 `Flt64`”的目标不完全一致。

详细步骤：

1. 决策输出字段策略：
   - 推荐：新增 `objValue: V`、`possibleBestObjValue: V?`、`bestBoundValue: V?` 作为主 API。
   - `gap/mipGap` 可保留 `Flt64`，因为其语义是比例/误差指标。
   - 旧 `obj/possibleBestObj/bestBound` 可保留为 deprecated 兼容字段或 adapter 字段。
2. 修改 `FeasibleSolverOutput<Flt64>.convertTo(converter)`，保证目标相关字段同步转换。
3. 补充输出转换测试，覆盖四类型。
4. 检查 framework/core-plugin 是否依赖旧字段，必要时先加兼容 wrapper，避免大范围破坏。

改动清单：

- 修改：`ospf-kotlin-core/src/main/.../solver/output/SolverOutput.kt`
- 新增/修改测试：
  - `SolverOutputCompatibilityTest`
  - `GenericSolverOutputConversionTest`
- 可能影响：
  - `ospf-kotlin-framework`
  - `ospf-kotlin-core-plugin`
  - `ospf-kotlin-example`

验收标准：

- `FeasibleSolverOutput<Rtn64>`、`FeasibleSolverOutput<FltX>`、`FeasibleSolverOutput<RtnX>` 可直接读取 V-typed objective。
- 旧 `Flt64` 字段仍可兼容编译，若保留则必须标记 deprecated。
- 删除 `convertTo` 中目标值转换应导致测试失败。

### P1：强化 MetaModel 与 MechanismModel 语义测试

背景：

- 当前 `GenericLinearMetaModelBuildTest`、`GenericQuadraticMetaModelBuildTest` 主要断言 build/dump 成功、约束数量、目标方向。
- 缺少对系数、rhs、objective、bounds、fixedVariables 的精确断言。

详细步骤：

1. 线性模型增加非平凡用例：
   - 正负系数、常数项、`LE/GE/EQ`。
   - bounds 约束与普通约束混合。
   - objective 包含常数项与多个变量。
   - fixedVariables 触发 rhs 与 objective 常数重算。
2. 二次模型增加非平凡用例：
   - quadratic + linear monomial 混合。
   - 对称变量项、平方项、常数项。
   - fixedVariables 分别固定一端、两端、不固定。
3. 对机制模型断言：
   - constraint 数量。
   - 每条 constraint 的 sign、rhs。
   - 每个 cell 的 coefficient 类型和值。
   - objectFunction category、cells、constant。
4. 对 triad/tetrad dump 断言：
   - sparse lhs 系数。
   - rhs。
   - objective cells。
   - variable bounds。

改动清单：

- 修改：
  - `GenericLinearMetaModelBuildTest.kt`
  - `GenericQuadraticMetaModelBuildTest.kt`
- 可新增 helper：
  - `GenericMechanismAssertions.kt`

验收标准：

- 四类型均覆盖线性和二次语义断言。
- 测试不再只依赖 `constraints.size`。
- 若 converter 的 `fromValue/intoValue` 被错误绕过，测试会失败。

### P1：强化函数符号 register 语义断言

背景：

- 当前 evaluate 类测试较扎实，但 register 类测试大量只断言 `after > before`。
- 对用户而言，函数符号是否可用取决于生成约束语义，不只是能追加约束。

详细步骤：

1. 为每组函数符号建立最小可验证 case：
   - abs/and/or/not/xor/if
   - max/min/slack/slack range
   - floor/ceiling/round/mod/bin/balance/in-step
   - product/quadratic linear/quadratic min/quadratic masking/quadratic in-step
   - satisfied amount/same-as/one-of/conditional
2. 对 register 后的新增约束断言：
   - 新增约束数量。
   - 关键变量/token 是否存在。
   - rhs 与 sign 是否符合预期。
   - cell coefficient 类型为 `V` 且数值正确。
3. 对若干代表函数增加“样例解满足性”：
   - 给 token 设置 solver result。
   - 调用 `isTrue()` 或 evaluate 验证约束语义。

改动清单：

- 修改 `ospf-kotlin-core/src/test/.../intermediate_symbol/function/*GenericRegistrationTest.kt`
- 可能新增公共断言工具：
  - `FunctionConstraintAssertions.kt`

验收标准：

- 每个 register 测试至少断言一条生成约束的 sign/rhs/coefficient/token。
- 每类函数至少有一个四类型样例解语义测试。
- 删除或反转关键约束生成逻辑时测试失败。

### P1：补齐 Token、Cache、Bound 四类型测试

背景：

- `Token<V>` 内部存储 `Flt64?`，公开 `result: V?` 通过 converter 转换。
- 当前缺少针对 `result/resultFlt64/setResultFromV/resultAsV/lowerBoundAsV/upperBoundAsV/containsInBounds` 的四类型测试。

详细步骤：

1. 新增四类型 token result 测试：
   - solver 写入 `Flt64` 后，`result` 返回 `V`。
   - `setResultFromV` 后，`resultFlt64` 与 `result` 均正确。
2. 新增 bounds 测试：
   - `lowerBoundAsV/upperBoundAsV` 类型和值正确。
   - `containsInBounds` 对四类型正确。
3. 新增 cache 测试：
   - `cachedValue`、`cache`、`cacheSolverIfNotCached` 对 solution/fixedValues 的 key 不串。
   - `Rtn64/RtnX` 不因为 converter 缓存或 equality 行为产生误判。

改动清单：

- 新增：
  - `GenericTokenBridgeTest.kt`
  - `GenericTokenCacheTest.kt`
- 修改已有：
  - `GenericTokenTableRegressionTest.kt`
  - `TokenCacheContextsTest.kt`

验收标准：

- 四类型全部覆盖 token result、bounds、cache。
- 删除 converter 或改错 cache key 时测试失败。

### P1：把 example 泛型 demo 纳入测试

背景：

- `GenericNumberDemo` 已存在，但没有被测试调用。
- `CoreDemoTest` 当前只是 smoke。

详细步骤：

1. 修改 `GenericNumberDemo.runBuildAndDump()`，返回结构化 summary：
   - 四种类型名称。
   - 每种类型的 linear/quadratic build 状态。
   - 约束数量、目标方向、关键系数。
2. 修改 `CoreDemoTest`，调用 `GenericNumberDemo.runBuildAndDump()`。
3. 断言四类型全部出现，线性/二次均成功，关键字段符合预期。
4. 避免 example 测试依赖真实外部 solver，仅验证 build/dump。

改动清单：

- 修改：
  - `ospf-kotlin-example/src/main/.../core_demo/GenericNumberDemo.kt`
  - `ospf-kotlin-example/src/test/.../CoreDemoTest.kt`
- 可选新增：
  - `GenericNumberDemoTest.kt`

验收标准：

- `mvn --% -pl ospf-kotlin-example -am -Dtest=CoreDemoTest,GenericNumberDemoTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
- 删除 `Rtn64/RtnX` demo 分支时测试失败。

### P2：补强门禁，防止 `Flt64` 主 API 回流

背景：

- 当前 `.toDouble()` 门禁有效，但不能发现公共 API 中新增 `Flt64` 主签名。
- `Flt64` 在 core 中仍有合法边界使用，因此需要白名单式门禁，而不是全仓零容忍。

详细步骤：

1. 扫描 core main API 中的 `Flt64` 类型签名。
2. 分类为：
   - solver adapter 边界允许。
   - deprecated compat 允许但需标注。
   - 主泛型链路禁止。
3. 在 `check-c8-guards.ps1` 中新增规则：
   - 禁止新增主链路 `.toDouble()`。
   - 禁止 `FeasibleSolverOutput` 等主输出新增非兼容 `Flt64` 字段。
   - 禁止新增散落 `@Suppress("UNCHECKED_CAST")`，集中到 `SolverBoundaryCasts` 或明确白名单。
4. 新增测试或脚本输出清晰的违规文件行号。

改动清单：

- 修改：`ospf-kotlin-core/scripts/check-c8-guards.ps1`
- 可能修改：`CoreToDoubleBridgeGuardTest.kt`
- 新增：`CoreFlt64ApiLeakageGuardTest.kt`

验收标准：

- P6/P7 门禁通过。
- 在主模型/函数/输出 API 新增 `Flt64` 字段或签名时门禁失败。

### P2：补齐数值转换精度与异常策略测试

背景：

- 当前 strict/allow 主要覆盖 `Flt64.nan/infinity`。
- `Rtn64/RtnX/FltX` 到 solver 边界的精度损失、不可表示值、round-trip 策略未系统覆盖。

详细步骤：

1. 为四类型 converter 增加 round-trip 测试：
   - `Flt64 -> V -> Flt64`
   - `V -> Flt64 -> V`
2. 覆盖分数、小数、大数、负数、零、一、无穷或不支持值。
3. 明确 strict 策略下哪些类型应拒绝精度损失。
4. 如果当前 converter 无法检测精度损失，先记录为兼容限制，再设计后续实现。

改动清单：

- 新增：
  - `GenericNumberConverterTest.kt`
  - `SolveValuePrecisionPolicyTest.kt`
- 可能修改：
  - `IntoValue.kt`
  - `SolveValueConversionContext.kt`

验收标准：

- 四类型 converter 行为被固定。
- 精度策略变化会导致测试明确失败，而不是静默改变。

### P3：整理旧 `Flt64` 兼容 API 与文档

背景：

- core 内仍保留大量 deprecated `Flt64` overload，这是兼容需要，但需要有清晰迁移路径。

详细步骤：

1. 列出所有 deprecated `Flt64` API。
2. 标注替代 API：
   - `solveV`
   - V-typed meta/model/function constructor
   - V-typed token/result accessor
3. 更新 README 或迁移说明。
4. 保持旧 API 测试只验证兼容，不再作为主业务实现验收。

改动清单：

- 修改：
  - `README.md`
  - `README_ch.md`
  - core 相关 KDoc
- 可新增：
  - `docs/core-genericization.md`

验收标准：

- README 中有四类型最小样例。
- 旧 API 标注 deprecated 且有替代入口。
- 用户可从 example 找到可运行的泛型 demo。

## 建议执行顺序

1. 先做 P0 `solveV` 四类型测试，因为它最能暴露“表面泛型”。
2. 再决策并收口 `FeasibleSolverOutput<V>`，避免后续测试围绕错误 API 写太多。
3. 然后强化 MetaModel/MechanismModel 与函数符号语义测试。
4. 接着把 `GenericNumberDemo` 接入 example 测试，形成用户侧验收。
5. 最后补门禁与文档，防止回归。

## 建议起手命令

1. `git status -sb`
2. `mvn --% -pl ospf-kotlin-core -am -Dtest=GenericLinearMetaModelBuildTest,GenericQuadraticMetaModelBuildTest,FunctionSymbolGenericRegistrationTest,QuadraticFunctionGenericEvaluationTest -Dsurefire.failIfNoSpecifiedTests=false test`
3. `mvn --% -pl ospf-kotlin-example -am -Dtest=CoreDemoTest -Dsurefire.failIfNoSpecifiedTests=false test`
4. `pwsh.exe -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`
5. `pwsh.exe -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`

## 当前审查备注

本轮审查曾尝试并行运行 core 泛型相关测试与 example 测试，两条 Maven 命令均在约 124 秒超时；未得到失败结论。下一轮应串行运行窄范围测试，避免并行构建互相拖慢。

## 本轮执行进展（2026-05-13）

### P0 进展更新

1. 已强化 `GenericSolveVBridgeTest`：
   - 对线性/二次的 `solveV(triad|tetrad)`、`solveV(mechanism)`、`solveV(..., solutionAmount)` 四类型路径补齐 objective 相关断言。
   - 新增 `objValue/possibleBestObjValue/bestBoundValue` 的四类型转换断言，同时校验 legacy `obj/possibleBestObj/bestBound` 与预期一致。
   - 新增 `solvingStatusCallBack` 调用次数断言（每个 number case 4 次），防止桥接路径丢回调。
2. 已修复 framework 侧 `SolverOutput.convertTo` 漏转字段问题：
   - 在 `QuadraticBendersDecompositionSolver.kt` 中补上 `objValue`、`possibleBestObjValue`、`bestBoundValue` 的 converter 转换。
3. 已补回归测试，防止回归：
   - `BendersSolverVBridgeTest` 增加 `objValue/possibleBestObjValue` 断言，验证 converter（plusOne）确实生效。

### 本轮测试结果

1. `mvn --% -pl ospf-kotlin-core -am -Dtest=GenericSolveVBridgeTest,GenericSolverOutputConversionTest,SolverOutputCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
2. `mvn --% -pl ospf-kotlin-framework -am -Dtest=BendersSolverVBridgeTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

### 下一步建议（P0 收尾）

1. 将 `GenericSolveVBridgeTest` 中对 deprecated 旧字段断言加 `@Suppress("DEPRECATION")`，保持日志干净。
2. 评估是否在 `FeasibleSolverOutput<V>` 上收紧默认值策略，避免外部直接构造 `V != Flt64` 时误用默认 cast。
3. P0 完成后切换到 P1 的 `MetaModel/MechanismModel` 语义断言深化。

## 本轮执行进展（2026-05-13，P1-函数符号注册语义加固）

### 已完成

1. 将函数符号注册测试从“汇总 after>before”升级为“逐函数独立断言新增约束”：
   - `FunctionSymbolGenericRegistrationTest`
   - `FunctionSymbolRoundingGenericRegistrationTest`
   - `FunctionSymbolConditionalGenericRegistrationTest`
   - `FunctionSymbolPiecewiseGenericRegistrationTest`
   - `FunctionSymbolSameAsGenericRegistrationTest`
   - `FunctionSymbolSatisfiedAmountInequalityGenericRegistrationTest`
   - `FunctionSymbolConstraintInputVFactoryTest`
2. 新断言覆盖：
   - 每个函数 `registerConstraints` 单独成功。
   - 每个函数必须单独追加约束（不再依赖其他函数“带过”）。
   - 每次新增约束都包含可检查的系数项。
   - 新增系数运行时类型必须保持 `V`，防止 `Flt64` 泄漏。

### 测试结果

1. `mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolGenericRegistrationTest,FunctionSymbolRoundingGenericRegistrationTest,FunctionSymbolConditionalGenericRegistrationTest,FunctionSymbolPiecewiseGenericRegistrationTest,FunctionSymbolSameAsGenericRegistrationTest,FunctionSymbolSatisfiedAmountInequalityGenericRegistrationTest,FunctionSymbolConstraintInputVFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过（默认标签过滤下，未执行 rounding）。
2. `mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolRoundingGenericRegistrationTest -Dospf.kotlin.test.excludedGroups= -Dsurefire.failIfNoSpecifiedTests=false test` 通过（含 slow/very-slow，4/4 通过）。

### 下一步

1. 在以上文件中继续把“新增约束”断言升级为“语义断言”：
   - 至少挑每类函数一个代表用例，断言新增约束的 `sign/rhs`。
   - 断言关键 token 参与（例如输入变量 token、辅助 token、输出 token）。
2. 再补一轮回归命令，确保慢测和默认门禁都稳定。

## 本轮执行进展（2026-05-13，P0-FeasibleSolverOutput 默认值策略收口）

### 已完成

1. 收紧 `FeasibleSolverOutput<V>` 默认目标值回退策略：
   - 修改 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/output/SolverOutput.kt`。
   - 新增 `castLegacyFlt64ToValueOrThrow`，当 `solution` 已是非 `Flt64` 元素时，默认 `objValue/possibleBestObjValue/bestBoundValue` 回退会直接抛错，避免静默错误类型。
   - 保留旧字段兼容（`obj/possibleBestObj/bestBound`），但默认回退不再允许“看起来是泛型、实际是 Flt64”的误用路径。
2. 新增回归测试：
   - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/solver/output/FeasibleSolverOutputLegacyFallbackGuardTest.kt`
   - 覆盖两类行为：
     - 非 `Flt64` solution 且未显式传 `objValue` 时必须失败。
     - 显式传入 V-typed `objValue/possibleBestObjValue` 时允许通过。

### 测试结果

1. `mvn --% -pl ospf-kotlin-core -am -Dtest=GenericSolveVBridgeTest,SolverOutputCompatibilityTest,GenericSolverOutputConversionTest,FeasibleSolverOutputLegacyFallbackGuardTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
2. `mvn --% -pl ospf-kotlin-framework -am -Dtest=BendersSolverVBridgeTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

### 现状结论

- `solveV` 四类型桥接、solution pool、目标值 V 视图转换链路保持稳定。
- `FeasibleSolverOutput<V>` 的默认 cast 误用风险已显式收口（从“静默错型”变为“可观测失败”）。
- P0 目标可视为完成收尾，建议切换到 P1 的机制模型/函数符号语义断言深化。

## 本轮执行进展（2026-05-13，P1-函数注册语义断言第二轮）

### 已完成

1. 在以下 6 个注册测试中，将断言从“仅新增约束 + 系数类型”升级为“新增约束 + 系数类型 + rhs 类型 + sign 可比性 + 输入 token 参与”：
   - `FunctionSymbolConditionalGenericRegistrationTest`
   - `FunctionSymbolPiecewiseGenericRegistrationTest`
   - `FunctionSymbolSameAsGenericRegistrationTest`
   - `FunctionSymbolSatisfiedAmountInequalityGenericRegistrationTest`
   - `FunctionSymbolConstraintInputVFactoryTest`
   - `FunctionSymbolRoundingGenericRegistrationTest`
2. 具体新增断言维度：
   - 追加约束后 `rhs` 保持 `V` 运行时类型。
   - 追加约束包含输入 token（按函数语义分别校验 `x` 或 `x/y`）。
   - 追加约束的 sign/comparison 处于 `LE/GE/EQ` 可比较集合。
   - 保留原有“约束数量增长 + 系数类型保持 `V`”断言。

### 测试结果

1. 默认标签回归：
   - `mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolConditionalGenericRegistrationTest,FunctionSymbolPiecewiseGenericRegistrationTest,FunctionSymbolSameAsGenericRegistrationTest,FunctionSymbolSatisfiedAmountInequalityGenericRegistrationTest,FunctionSymbolConstraintInputVFactoryTest,FunctionSymbolRoundingGenericRegistrationTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
2. rounding 慢测全集：
   - `mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolRoundingGenericRegistrationTest -Dospf.kotlin.test.excludedGroups= -Dsurefire.failIfNoSpecifiedTests=false test` 通过（4/4）。

### 现状结论

- 函数符号注册测试从“结构冒烟”进一步提升到“约束语义冒烟”。
- 删除/反转关键约束方向、替换为错误 rhs 类型、遗漏输入 token 引用时，当前测试将更容易失败。
