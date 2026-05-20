# core/math 符号运算迁移与兼容层删除记录（2026-05-14 起）

## 最新结论：无兼容层收口（2026-05-19）

当前路线已从“补齐原版兼容层、平滑迁移”调整为“删除兼容层，仅保留正式 math/core 设计”。因此验收重点不再是保留旧入口，而是确认符号运算能力已经落到正式 API，core 主链路保持泛型化，example/framework 使用显式替代链路。

已完成的当前状态：

1. `math.symbol.adapter.*` 源码目录、`FunctionCompat.kt`、`MetaModelFlt64Adapter.kt` 已删除，旧 `Bridged*` 命名不再作为源码入口使用。
2. Flt64 快捷建模 DSL 落在正式 `math.symbol.operation.*` 下，不再通过 adapter 包提供。
3. core/example/framework 默认源码不再依赖旧 `solver(metaModel)` 兼容调用；example 侧如需保持 demo 简洁，使用显式 `dump(metaModel) -> dump(mechanism) -> solver(triad)` helper。
4. 旧无 converter 的 `SlackFunction(...)` / `AbsFunction.fromLinearPolynomial(...)` / `BinaryzationFunction.fromLinearPolynomial(...)` 测试残留已迁到正式泛型构造器与 `LinearFunctionSymbolAdapter`。
5. P17 静态门禁已加入 `check-c8-guards.ps1`，禁止兼容层路径、旧符号/import、example 旧 solver 调用、旧无 converter Slack 调用回流。

当前可确认结论：core 已在架构上完成符号运算能力向 math 正式符号体系的迁移，且不再保留兼容层作为公开或默认源码依赖。功能和接口的目标口径已更新为“按最佳设计还原建模能力”，而不是“逐项保留原 Kotlin 版本旧调用形态”。

## 目标

本轮最初目标是对比原始 Kotlin 版本：

- 原始 core：`E:/workspace/ospf-kotlin-main`
- 原始示例：`E:/workspace/ospf/examples/ospf-kotlin-example`
- 原始业务项目：`E:/workspace/poit/aps`
- 原始业务项目：`E:/workspace/poit/csp1d`
- 原始业务项目：`E:/workspace/poit/bop`
- 原始业务项目：`E:/workspace/poit/psp`
- 当前迁移仓库：`E:/workspace/ospf-kotlin`

在保留两项既定架构变更的前提下，让当前版本与原始版本在功能和易用性上保持一致：

1. 符号运算能力从 core 自有表达式体系迁移至 `math.symbol`。
2. core 主链路完成泛型化，数值类型主参数记为 `V`。

最新决策：不再为了平滑迁移保留兼容层。原始版本的建模能力、示例表达和业务覆盖仍作为功能基准，但旧包路径、旧构造器和旧 solver 入口应迁到正式 math/core API，而不是通过兼容层复刻。

本轮新增四个完整原始业务项目作为真实迁移语料。它们不是简单示例，实际覆盖 starter、framework、Gantt scheduling、CSP1D、solver 插件、列生成、`Pipeline`/`PipelineList`、`AbstractLinearMetaModel`/`AbstractQuadraticMetaModel`、`LinearIntermediateSymbolsN`/`QuadraticIntermediateSymbolsN`、变量族和解分析链路。因此本轮计划需要从 core API 易用性扩展为“core + framework + starter + 业务项目编译链路”的兼容闭环。

业务项目 APS/CSP1D/BOP/PSP 不需要纳入默认直接编译门禁。当前仓库默认门禁只要求 in-repo source-compat fixture、starter/framework 依赖闭包和 build-only 结构验证通过；外部仓库直接编译属于项目侧升级工作，不作为本仓库默认 release gate 的阻塞项。

由于本次 core 已泛型化，凡是迁移后必须显式指定数值类型的易用入口，都不能只补 `Flt64`。正式 API 至少需要能支撑 `Flt64`、`FltX`、`Rtn64`、`RtnX` 四类路径；`Flt64` 继续作为示例和业务项目的最小迁移路径，另外三种类型用于验证泛型能力没有退化为单类型适配。

## 总体原则

1. 原始示例与四个业务项目共同作为功能与易用性的基准。若原代码中存在稳定公开用法，当前版本应提供正式 API 或清晰的一步替代，不再新增兼容包装。
2. 架构变更只允许影响内部实现和必要类型参数，不应迫使 `Flt64/FltX/Rtn64/RtnX` 用户手写 converter、低层 flatten data 或冗长构造器。
3. `Flt64` 默认路径必须足够顺手：`LinearMetaModel("name")`、`QuadraticMetaModel()`、常用函数符号和 solver 调用应保持接近原版体验。
4. 泛型路径必须保持一等公民：新增正式入口不能只服务 `Flt64`。凡是因为泛型化必须暴露类型选择的易用入口，至少提供 `Flt64`、`FltX`、`Rtn64`、`RtnX` 四套入口或等价工厂。
5. 示例测试分层执行：默认回归使用 build-only 或结构化断言，不强依赖外部 solver；solver 存在时再跑 SCIP/Gurobi 集成验证。
6. 功能迁移优先于包名完全复刻。`frontend/backend` 包拆平属于已接受架构演进，但应通过 import 聚合、别名或文档降低迁移成本。
7. 快捷 DSL 必须有边界测试。`sum/qsum`、`eq/leq/geq/ls/gr/neq`、`partition`、`constraintsOfGroup`、函数符号别名等都要有编译级或结构级防回归。
8. 不以空 smoke 作为验收。所有新增或恢复测试必须断言可观察结构、约束数量、目标项、符号注册、求解结果或错误状态。
9. 业务项目迁移优先做编译与 build-only 结构闭环，不把真实 solver 可用性、外部数据服务或 POIT 父 POM 作为默认阻塞项；solver 相关路径用 profile 或条件跳过验证。
10. starter 与 framework 是兼容面的一部分。`ospf-kotlin-starter-gantt-scheduling`、`ospf-kotlin-starter-csp1d`、`framework.model`、`framework.solver`、`framework.gantt_scheduling` 的公开入口要跟随原版业务用法检查。
11. math 层可新增 companion-provider 转换接口，统一表达 `Flt64 <-> V` 的转换能力，避免 core/framework 为四种数值类型散落硬编码 converter。

## 当前阶段结论（2026-05-19）

P16 已完成：`framework_demo/demo2` 与 `heuristic_demo` 已恢复到 `ospf-kotlin-example` 默认源码集，`src/non-default-main` / `src/non-default-test` 已移除，默认 `compile/test` 可直接通过。

默认迁移门禁维持闭环：core source-compat、example 默认构建、core demo、function build-only、business source-compat、framework/starter compat 与 P6/P7（含 P16）静态门禁均已通过。

P17 无兼容层收口已推进：兼容层目录与旧入口回流检查已加入静态门禁；当前源码目标从“兼容恢复”切换为“正式 API 替代”。

## 已完成事项

### P0：兼容矩阵与迁移范围识别

1. 已把原始 core、原始 example、APS/CSP1D/BOP/PSP 作为迁移语料。
2. 已确认高频迁移触点覆盖 core、framework、starter、solver 插件、变量族、函数符号、pipeline、列生成和 mechanism model 链路。
3. 已建立业务 source-compat fixture，用于替代外部仓库默认直接编译门禁。

### P1：四类数值易用入口

1. 已恢复 `Flt64` 默认建模路径，并补齐 `FltX`、`Rtn64`、`RtnX` 相关入口验证。
2. 已把常见 converter 入口收敛到库侧能力，例如 `IntoValue.Identity`、`FltX.toIntoValue()`、`Rtn64.toIntoValue()`、`RtnX.toIntoValue()`。
3. 已通过 source-compat 和泛型测试防止新 API 退化为单 `Flt64` 适配。

### P2/P3/P4：模型 DSL、函数符号与快捷算术

1. 已恢复核心建模 DSL、函数符号构造和常用表达式入口的主要兼容路径。
2. 已补充 build-only 函数测试，覆盖线性函数、条件函数、二次函数、ProductFunction、QuadraticLinearFunction 等结构注册路径。
3. 已修复 `MathInequalityFlatten.kt` 中 `toLinearFlattenData` 对非变量符号的危险硬转，改为 `as?` 与明确失败信息，避免 ClassCast 回流。

### P5：示例回归覆盖

1. `core_demo Demo1-17` 已进入 build-only 结构测试。
2. 函数示例测试已从空 smoke 转为带结构断言的 build-only 测试。
3. 二次示例已拆分为 build-only 结构测试和 solver-gated 结果测试。
4. solver 不可用时，solver-gated 测试通过条件跳过，不阻塞默认门禁。

### P6/P7：API 边界门禁与业务 source-compat

1. 已扩展 `check-c8-guards.ps1`，新增 P10/P11/P12/P14/P16 静态检查。
2. 已覆盖空断言、converter 样板回流、solver-gated 空测试、危险 flatten hard-cast、默认 example 旧 `core.frontend.*` import 回流等风险。
3. `business-source-compat` profile 已覆盖 APS、CSP1D、BOP、PSP 四类业务高频用法的 in-repo fixture。
4. `framework-starter-compat` profile 已覆盖 starter/framework 依赖闭包和常用入口。

### P8/P9：framework/starter 与 math 转换桥

1. framework/starter 已纳入兼容验收面。
2. math 层数值转换桥已用于减少 core/example 中的手写 converter 需求。
3. README 与 README_ch 已补充迁移入口、四类 converter 推荐入口和门禁命令。

### P10/P11/P12：二次模型与 solver-gated 分层

1. 已修复 `QuadraticProductBuildOnlyStructureTest` 的既有断言问题。
2. 已新增或增强二次函数 build-only 测试、evaluate 测试和 solver-gated 测试。
3. BOP 风格业务 fixture 已增强二次约束与 `QuadraticMechanismModel` 验证。

### P13：外部业务仓库直接编译

1. 已扫描 APS/CSP1D/BOP/PSP 的旧 import 与迁移触点。
2. 已形成迁移映射与扫描脚本，供外部项目侧升级使用。
3. 明确结论：外部业务仓库直接编译不纳入默认门禁，本仓库以 source-compat fixture 作为 release gate 证据。

### P14：example 默认构建恢复

1. 已确认默认 example 编译阻塞主要来自 `framework_demo/demo2` 与 `heuristic_demo`。
2. 已将这两组尚未迁移完成的 demo 临时移出默认源码集：
   - `ospf-kotlin-example/src/non-default-main/.../framework_demo/demo2`
   - `ospf-kotlin-example/src/non-default-main/.../heuristic_demo`
   - `ospf-kotlin-example/src/non-default-test/.../framework_demo/demo2`
   - `ospf-kotlin-example/src/non-default-test/.../linear_function/SemiTest.kt`
   - `ospf-kotlin-example/src/non-default-test/.../linear_function/ULPTest.kt`
   - `ospf-kotlin-example/src/non-default-test/.../linear_function/XorTest.kt`
3. 已修复默认测试中的 `SlackRangeFunction` 旧 `threshold` 调用漂移。
4. 默认 `ospf-kotlin-example` 的 `compile/test` 已恢复。

### P15：统一 release gate 与文档收口

1. 已新增 `ospf-kotlin-core/scripts/check-migration-compat.ps1`。
2. 已把默认迁移门禁串联为 core source-compat、math bridge/DSL、example 默认 compile/test、核心 demo、函数 build-only、业务 source-compat、framework/starter 和 P6/P7 静态门禁。
3. 已更新 README、README_ch、`docs/p7-business-compat-matrix.md`。
4. 已确认统一脚本覆盖面正确，但串行执行耗时较长，不适合作为唯一快速反馈入口。

### P16：非默认 demo 迁回默认构建

1. `framework_demo/demo2` 与 `heuristic_demo` 已从 `src/non-default-*` 迁回默认 `src/main` / `src/test` 源码集。
2. `ospf-kotlin-example/src/non-default-main` 与 `ospf-kotlin-example/src/non-default-test` 已删除，不再作为默认构建旁路。
3. 新增并启用 P16 静态门禁：检查 non-default 残留目录，以及默认源码集旧 `core.frontend.*` / `core.backend.*` / `utils.math.*` import 回流。
4. `FrameworkDemoTest`、`HeuristicDemoTest`、`framework_demo/demo2` 相关测试与线性函数回迁测试在默认 `example test` 下可执行并通过。
5. README、README_ch 与迁移脚本文案已更新到 P16 状态。

## 已验证命令与结果

以下命令已在 2026-05-19 复核通过：

```powershell
pwsh.exe -NoProfile -Command "mvn -pl ospf-kotlin-example -am -DskipTests compile"
```

结果：2026-05-19 18:41（Asia/Shanghai）默认 example 编译通过。JVM 输出过 CodeHeap full warning，但 Maven 结果为 `BUILD SUCCESS`。

```powershell
pwsh.exe -NoProfile -Command "mvn -pl ospf-kotlin-example -am -DskipTests test-compile"
```

结果：2026-05-19 19:19（Asia/Shanghai）默认 example 测试编译通过。线性/二次函数测试已迁到正式泛型构造器、显式 converter 与 mechanism/triad 或 tetrad 求解链路。

```powershell
mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：`SourceCompatTest` 与 `MathInequalityFlattenTest` 共 32 个测试通过。

```powershell
mvn --% -pl ospf-kotlin-example -am -DskipTests compile
```

结果：默认 example 编译通过。

```powershell
mvn --% -pl ospf-kotlin-example -am -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：默认 example 测试通过，example 模块 97 个测试，0 失败，0 错误，6 个 solver-gated 测试按条件跳过。

```powershell
mvn --% -pl ospf-kotlin-example -am -Pcore-demo-only -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：19 个测试通过。

```powershell
mvn --% -pl ospf-kotlin-example -am -Pbuild-only-function-tests -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：9 个测试通过。

```powershell
mvn --% -pl ospf-kotlin-example -am -Pbusiness-source-compat -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：4 个测试通过。

```powershell
mvn --% -pl ospf-kotlin-example -am -Pframework-starter-compat -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：5 个测试通过。

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7
```

结果：2026-05-19 19:20（Asia/Shanghai）P6/P7 静态门禁均通过，包含新增 P17 无兼容层回流检查。

## 剩余风险与注意事项

1. 默认门禁仍不覆盖真实 SCIP/JNI solver 集成。需要 solver 验证时使用 `-Psolver-integration-tests` 或 `check-migration-compat.ps1 -WithSolverIntegration`。
2. `check-migration-compat.ps1` 目前串行执行耗时较长，一次完整运行通常超过 20 分钟，建议后续拆分为 CI 子步骤并配置单步超时。
3. 仍存在基线内手写 converter 与 deprecated 调用。当前门禁目标是防回流，不是一次性清零历史技术债。
4. P7 whitelist 仍需与后续重构保持同步；若新增 `<*>` 使用文件，需要同步更新 `p7-whitelist.json` 并给出变更理由。
5. 外部 APS/CSP1D/BOP/PSP 仓库直接编译依旧不是默认门禁；其升级状态在文档跟踪即可，不阻塞当前仓库 release gate。

## P16 执行记录（已完成）

### P16 目标

恢复 `framework_demo/demo2` 与 `heuristic_demo` 到 `ospf-kotlin-example` 默认源码集，使默认 `compile/test` 不再依赖临时隔离目录即可通过。

P16 完成后，`src/non-default-main` 与 `src/non-default-test` 应清空、删除，或仅保留明确不可自动化且有 profile 说明的外部环境样例。当前这两个目录中的 demo 不应继续作为默认构建之外的长期遗留。

### P16 优先级

1. 优先恢复 `heuristic_demo`。体量较小，主要阻塞点是 `CallBackModel` 构造入口从公开构造方式变化为当前 internal/受限入口。
2. 再恢复 `framework_demo/demo2`。该 demo 体量大，应按模块拆解迁移，不要一次性大改。
3. 每迁回一块都补测试，测试必须有结构断言或最小行为断言，不能只做空 smoke。

### P16 详细步骤

#### 步骤 1：建立 non-default 独立编译 profile

1. 在 `ospf-kotlin-example/pom.xml` 中新增 profile，例如 `non-default-demo-compat`。
2. 将 `src/non-default-main` 加入该 profile 的 main source roots。
3. 将 `src/non-default-test` 加入该 profile 的 test source roots。
4. 先只要求 test-compile 或 compile，避免一开始运行未迁移测试。
5. 执行：

```powershell
mvn --% -pl ospf-kotlin-example -am -Pnon-default-demo-compat -DskipTests test-compile
```

6. 记录完整错误清单，按 `heuristic_demo` 与 `framework_demo/demo2` 分组。

#### 步骤 2：恢复 `heuristic_demo`

1. 检查 `src/non-default-main/.../heuristic_demo/Demo1.kt` 与 `Demo2.kt`。
2. 定位 `CallBackModel` 当前公开替代入口。
3. 若缺少合理公开入口，优先在库侧增加小而明确的兼容工厂或 adapter，不要在 demo 中复制内部构造逻辑。
4. 将可编译的 `heuristic_demo` 迁回：
   - `src/non-default-main/.../heuristic_demo` -> `src/main/.../heuristic_demo`
5. 恢复或新增对应测试到默认 `src/test`。
6. 测试断言至少覆盖：
   - callback model 能构建。
   - mechanism 或核心求解上下文能完成最小注册链路。
   - 不依赖真实外部 solver 时也能 build-only 验证。

#### 步骤 3：拆分恢复 `framework_demo/demo2`

按以下顺序迁移，保证每一层都能单独编译或测试：

1. DTO 与基础工具层：
   - `infrastructure/dto`
   - `Diagnostics.kt`
   - `FeasibilityDiagnostics.kt`
   - `TryHelpers.kt`
2. 纯 domain model：
   - `domain/*/model`
   - 避免先迁移 solver 或 pipeline 入口。
3. aggregation 与 context：
   - `Aggregation.kt`
   - `*Context.kt`
   - 检查 `AbstractLinearMetaModel`、变量族、符号容器和 `PipelineList` 新 API。
4. service 与 limits：
   - `service/limits`
   - 优先处理 `register`、`addConstraint`、`addObject`、`sum/qsum`、`SlackRangeFunction`、函数符号注册等签名漂移。
5. pipeline 与 solver infrastructure：
   - `DomainPipeline.kt`
   - `Solver.kt`
   - `BendersSolver.kt`
   - `BendersStrategy.kt`
   - 对真实 solver 依赖进行 profile-gated 或 build-only 替代。
6. 应用入口：
   - `FullLoadApplication.kt`
   - `LoadingOrderApplication.kt`
   - `PredistributionApplication.kt`
   - `WeightRecommendationApplication.kt`
   - 默认构建只要求编译和结构化注册，不默认运行真实优化任务。
7. 测试：
   - `BendersStrategyTest.kt`
   - `DiagnosticsTest.kt`
   - `FeasibilityDiagnosticsTest.kt`
   - `RequestDTOTest.kt`
   - 将测试改为结构断言或纯函数断言，solver 相关路径用 profile 或条件跳过。

#### 步骤 4：迁回默认源码集

1. 每完成一个模块，优先用 `git mv` 从 `src/non-default-*` 迁回 `src/main` 或 `src/test` 对应路径。
2. 避免复制后删除导致历史不清。
3. 迁回后执行默认编译：

```powershell
mvn --% -pl ospf-kotlin-example -am -DskipTests compile
```

4. 对测试迁回执行：

```powershell
mvn --% -pl ospf-kotlin-example -am -Dsurefire.failIfNoSpecifiedTests=false test
```

#### 步骤 5：更新门禁与文档

1. 在 `check-c8-guards.ps1` 中新增 P16 guard：
   - 禁止 `framework_demo/demo2` 与 `heuristic_demo` 长期留在 `src/non-default-*`。
   - 默认源码集禁止旧 `core.frontend.*`、`core.backend.*`、`utils.math.*` import 回流。
2. 在 `check-migration-compat.ps1` 中纳入 P16 检查。
3. 更新 README/README_ch，说明 `framework_demo/demo2` 与 `heuristic_demo` 已恢复默认构建。
4. 更新本文件的 P16 状态。

## P16 预计修改清单

### 需要修改或新增

1. `ospf-kotlin-example/pom.xml`
   - 新增 `non-default-demo-compat` profile。
   - P16 结束时视情况删除该临时 profile，或保留为历史迁移检查 profile。
2. `ospf-kotlin-example/src/non-default-main/.../heuristic_demo/*`
   - 迁移并最终迁回 `src/main`。
3. `ospf-kotlin-example/src/non-default-main/.../framework_demo/demo2/**`
   - 分模块迁移并最终迁回 `src/main`。
4. `ospf-kotlin-example/src/non-default-test/**`
   - 迁移可恢复测试并最终迁回 `src/test`。
5. `ospf-kotlin-core/scripts/check-c8-guards.ps1`
   - 新增 P16 防回归检查。
6. `ospf-kotlin-core/scripts/check-migration-compat.ps1`
   - 纳入 P16 门禁。
7. `README.md`
   - 更新迁移门禁说明。
8. `README_ch.md`
   - 更新迁移门禁说明。
9. `ospf-kotlin-core/daily.md`
   - 记录 P16 迁移结果、命令和结论。

### 可能需要修改

1. `ospf-kotlin-core/src/main/**`
   - 如果 `heuristic_demo` 或 `framework_demo/demo2` 暴露出缺失的公开兼容入口，优先在库侧补薄入口。
2. `ospf-kotlin-framework/src/main/**`
   - 如果 `Pipeline`、`PipelineList`、framework solver 或 Benders 入口存在签名漂移，按原业务用法补兼容包装。
3. `ospf-kotlin-framework-gantt-scheduling/**`
   - 如果 demo2 的航空装载上下文依赖 gantt scheduling 旧入口，需要补 framework 层薄适配。

### 不应修改

1. 不要把外部 APS/CSP1D/BOP/PSP 仓库直接编译加入默认门禁。
2. 不要为了通过编译删除 demo 逻辑或改成空实现。
3. 不要新增空 `assertTrue(true)`、`assertThat(true).isTrue()` 或无条件 `assumeTrue(true)`。
4. 不要把真实 SCIP/Gurobi/JNI 环境作为默认门禁依赖。

## P16 验收标准

### 必须通过

```powershell
mvn --% -pl ospf-kotlin-example -am -DskipTests compile
```

```powershell
mvn --% -pl ospf-kotlin-example -am -Dsurefire.failIfNoSpecifiedTests=false test
```

```powershell
mvn --% -pl ospf-kotlin-example -am -Pcore-demo-only -Dsurefire.failIfNoSpecifiedTests=false test
```

```powershell
mvn --% -pl ospf-kotlin-example -am -Pbuild-only-function-tests -Dsurefire.failIfNoSpecifiedTests=false test
```

```powershell
mvn --% -pl ospf-kotlin-example -am -Pbusiness-source-compat -Dsurefire.failIfNoSpecifiedTests=false test
```

```powershell
mvn --% -pl ospf-kotlin-example -am -Pframework-starter-compat -Dsurefire.failIfNoSpecifiedTests=false test
```

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6
powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7
```

### P16 新增验收

1. `framework_demo/demo2` 不再位于 `src/non-default-main` 或 `src/non-default-test`。
2. `heuristic_demo` 不再位于 `src/non-default-main`。
3. 默认 `src/main` 与 `src/test` 中不出现旧 `core.frontend.*` import。
4. 默认 `src/main` 与 `src/test` 中不出现旧 `core.backend.*` import。
5. 默认 `src/main` 与 `src/test` 中不出现旧 `utils.math.*` import。
6. 迁回的 demo 测试必须有可观察断言：
   - DTO/诊断类测试断言字段、状态或错误消息。
   - build-only 模型测试断言变量数量、约束数量、目标项、符号注册或 mechanism model 结构。
   - solver 相关测试在默认门禁中不强依赖真实 solver。
7. `check-migration-compat.ps1` 可继续作为完整门禁入口；若耗时过长，必须至少保证拆分后的子命令在文档中可复现。

### 可选验收

具备 SCIP/JNI 环境时执行：

```powershell
mvn --% -pl ospf-kotlin-example -am -Psolver-integration-tests -Dsurefire.failIfNoSpecifiedTests=false test
```

或：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-migration-compat.ps1 -WithSolverIntegration
```

solver 集成通过可作为增强证据；失败时需要区分代码问题与本地 JNI/DLL 环境问题。

## P18：命名回归收口（进行中）

### P18 目标

兼容层删除后，正式 API 已不需要继续用迁移期命名区分“原版本”与“泛型化版本”。P18 的目标是把因并存兼容层而产生的临时命名回归到正式 API 应有的原名，使代码语义从“迁移态”回到“唯一正式实现态”。

优先处理以下命名：

1. `Typed*` / `*Typed*`：若只是为了区分旧 adapter/bridged 版本，应回归原名。
2. `*V` / `*VTyped*`：若只是为了区分泛型版与 Flt64 兼容版，应回归原名。
3. `Type*` / `*Type*`：若不是领域语义，而是迁移期的类型化前缀/后缀，应回归原名。
4. 测试、文档、门禁脚本中的迁移期命名也要同步更新，避免新 API 继续暴露“兼容迁移”痕迹。

### P18 判定边界

以下命名不应机械回滚：

1. 类型参数 `V` 保留，它是 core 泛型主链路的一部分。
2. `View`、`Input`、`Adapter`、`Converter` 等表达真实架构职责的后缀保留。
3. Flt64 solver 边界、输出兼容字段、第三方 solver 插件中的真实 Flt64 语义保留。
4. 若原名已被正式不同概念占用，先重命名迁移痕迹，再评估是否需要合并抽象，不能用简单替换制造歧义。

### P18 初始验收

1. 扫描 Kotlin 源码和测试中的 `Typed`、`Type[A-Z]`、`[A-Za-z0-9]V` 等迁移期命名候选，并形成清单。
2. 第一批优先回归 math symbol operation 下由 bridged/adapter 迁移来的文件和测试命名。
3. P6/P7/P17 静态门禁保持通过。
4. `mvn -pl ospf-kotlin-math -am -DskipTests test-compile` 与 `mvn -pl ospf-kotlin-example -am -DskipTests test-compile` 至少通过相关受影响模块。

### P18 第一批执行记录（2026-05-19）

1. 已提交无兼容层迁移 checkpoint：`0358d2d1 Remove compatibility layer and start P18 naming cleanup`。
2. math symbol operation 第一批命名回归：
   - `TypedQuickDsl` -> `QuickDsl`
   - `TypedQuickOps` -> `QuickOps`
   - `TypedInequalityDsl` -> `InequalityDsl`
   - `TypedLinearMatrixForm` -> `LinearMatrixForm`
   - `TypedQuadraticMatrixForm` -> `QuadraticMatrixForm`
   - `toTypedMatrixForm` -> `toMatrixForm`
   - `typedLinearPolynomialFromMatrixForm` -> `linearPolynomialFromMatrixForm`
   - `typedQuadraticPolynomialFromMatrixForm` -> `quadraticPolynomialFromMatrixForm`
3. 为避免 Flt64 专用快捷层继续占用泛型正式 API 名，已将 Flt64 专用文件/API 显式命名：
   - `Flt64QuickDsl.kt`
   - `Flt64QuickOps.kt`
   - `Flt64MatrixForm.kt`
   - `toFlt64MatrixForm`
   - `flt64LinearPolynomialFromMatrixForm`
   - `flt64QuadraticPolynomialFromMatrixForm`
4. 已新增 P18 静态门禁：
   - `check-c8-guards.ps1` 禁止 `math.symbol.operation` 正式 API 中回流 `TypedQuickDsl`、`TypedQuickOps`、`TypedInequalityDsl`、`Typed*MatrixForm`、`toTypedMatrixForm` 与 `typed*PolynomialFromMatrixForm`。
5. 当前已通过：
   - `pwsh.exe -NoProfile -Command "mvn -pl ospf-kotlin-math -am -DskipTests test-compile"`：通过。存在 JVM CodeHeap warning，但 Maven `BUILD SUCCESS`。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过，含新增 `P18-1`。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过，含新增 `P18-1` 与更新后的 Flt64 whitelist。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-math -Dtest=QuickDslTest,MatrixFormConversionTest,MatrixFormTest -Dsurefire.failIfNoSpecifiedTests=false test"`：31/31 通过。
   - `pwsh.exe -NoProfile -Command "mvn -pl ospf-kotlin-example -am -DskipTests test-compile"`：通过。第一次执行因 6 分钟工具超时留下 Maven/Java 进程；已只清理该验证链路后重跑，第二次 Maven `BUILD SUCCESS`。日志仍出现 JVM CodeHeap warning。

### P18 第二批执行记录（2026-05-19）

core 与 solver 边界命名已按“泛型正式 API 使用原名，Flt64 求解器边界显式命名”的原则继续收口：

1. core 机制层与函数符号迁移期命名回归：
   - `LinearConstraintInputV` -> `LinearConstraintInput`
   - 旧 Flt64 边界输入 -> `Flt64LinearConstraintInput`
   - `AbstractCallBackModelInterfaceV` -> `AbstractCallBackModelInterface`
   - `CallBackModelInterfaceV` -> `CallBackModelInterface`
   - `MultiObjectiveModelInterfaceV` -> `MultiObjectiveModelInterface`
   - `nonzeroIndicatorConstraintsV` -> `nonzeroIndicatorConstraints`
   - `simpleIndicatorConstraintsV` -> `simpleIndicatorConstraints`
   - `linearInequalityAsV` / `quadraticInequalityAsV` -> `linearInequalityAs` / `quadraticInequalityAs`
   - `mapValuesToV` -> `mapValues`
   - `dependencyAsIntermediateV` -> `dependencyAsIntermediate`
   - `expressionRangeVFromFlt64` -> `expressionRangeFromFlt64`
   - `fullExpressionRangeV` -> `fullExpressionRange`
   - `flattenedMonomialsAsV` -> `flattenedMonomials`
   - `IfFunction.typed(...)`、`IfThenFunction.typed(...)`、`SatisfiedAmountInequalityFunction.typed(...)` 等工厂 -> `from(...)`
2. Benders cut API 已完成分层命名：
   - 泛型正式 API 使用原名：`generateOptimalCut`、`generateFeasibleCut`、`generateOptimalCutById`、`generateFeasibleCutById`。
   - 求解器 Flt64 raw dual 边界显式命名：`generateFlt64OptimalCut`、`generateFlt64FeasibleCut`。
   - copt/cplex/gurobi/gurobi11/mindopt/scip Benders solver 插件已同步使用 `generateFlt64*Cut`。
   - `BendersCutTypedByIdApiTest` 已回归为 `BendersCutByIdApiTest`。
3. P18 静态门禁已扩展：
   - `P18-2` 禁止 core 中 `LinearConstraintInputV`、callback `*V`、函数符号 `typed(...)`、Benders `generate*CutV` / `generate*CutByIdV`、`TypedById` 等迁移期命名回流。
   - `P18-3` 禁止 core-plugin Benders solver 继续用泛型原名调用 raw Flt64 dual 边界，要求使用 `generateFlt64*Cut`。
4. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile"`：通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolConstraintInputFactoryTest,FunctionSymbolSatisfiedAmountInequalityGenericRegistrationTest,SatisfiedAmountFunctionsGenericEvaluateTest,MultiObjectCallBackModelTest,ExpressionRangeSolverBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test"`：15/15 通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=BendersCutApiTest,BendersCutByIdApiTest,GenericBendersCutRegressionTest,QuadraticMechanismModelCutTest -Dsurefire.failIfNoSpecifiedTests=false test"`：14/14 通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile"`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过，含 `P18-1/P18-2/P18-3`。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过，含 `P18-1/P18-2/P18-3`。

注意：上述 Maven 验证仍会输出 JVM CodeHeap warning，但 Maven 结果均为 `BUILD SUCCESS`。

### P18 第三批执行记录（2026-05-19）

继续按“泛型正式 API 回归原名，Flt64 求解器边界显式命名”的原则处理剩余高信号迁移命名：

1. solver 泛型入口已从迁移期 `solveV(...)` 回归为正式 `solve(...)`：
   - `AbstractLinearSolver.solveV(...)` -> `AbstractLinearSolver.solve(...)`
   - `AbstractQuadraticSolver.solveV(...)` -> `AbstractQuadraticSolver.solve(...)`
   - 原 `SolverExt.kt` 中只负责转发到 `solveV(...)` 的泛型扩展已移除，避免正式 API 再经过迁移期桥接名。
   - `GenericSolveVBridgeTest` 已回归为 `GenericSolveTest`，测试用例与测试桩命名同步去除 `SolveV/Bridge` 痕迹。
2. token 泛型结果访问命名已回归：
   - `setResultFromV(...)` -> `setResult(...)`
   - `resultAsV(...)` -> `result(converter)`
   - `lowerBoundAsV(...)` / `upperBoundAsV(...)` -> `lowerBound(converter)` / `upperBound(converter)`
   - `resultFlt64` 保留，继续表达真实求解器边界视图。
3. 已删除 math 测试中的临时探针文件：
   - `TmpDefaultTypeArgCheck.kt`
4. P18 静态门禁已扩展：
   - `P18-2` 额外禁止 `solveV`、`GenericSolveVBridgeTest`、`Recording*SolveVBridgeSolver`、`setResultFromV`、`resultAsV`、`lowerBoundAsV`、`upperBoundAsV` 回流。
5. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile"`：通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=GenericSolveTest,GenericTokenConversionTest -Dsurefire.failIfNoSpecifiedTests=false test"`：3/3 通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过，含扩展后的 `P18-2`。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过，含扩展后的 `P18-2`。

### P18 第四批执行记录（2026-05-19）

继续清理测试、包名与叙事层面的 `Bridge` / `Migration` 命名，只保留真实边界职责：

1. 测试命名已从迁移叙事回归为行为回归或转换语义：
   - `FunctionSymbolMigrationTest` -> `FunctionSymbolRegressionTest`
   - `GenericTokenBridgeTest` -> `GenericTokenConversionTest`
   - `MatrixFormBridgeTest` -> `MatrixFormConversionTest`
   - `core.symbol_migration.*` 测试包 -> `core.symbol_regression.*`
2. 已修正 `FunctionSymbolRegressionTest` 中过期断言：`LinearFunctionSymbolAdapter.flattenedMonomials` 对 `SlackFunction` 这类 `HasResultPolynomial` delegate 会暴露结果多项式，而不是返回空集合。
3. 已确认以下命名表达真实边界或领域职责，暂不机械回滚：
   - `Flt64Bridge` 与 `IntoValue.fromBridge(...)`：表达 Flt64/value 桥接边界。
   - solver boundary 与 `.toDouble()` conversion guard：表达求解器边界防线。
   - `SolveValue*`、`TypedValueRange`、`ParserTypedEntryTest` / `parseTyped*`、`VariableTypeKind`、`typeName`、`toTypedArray()`：属于领域、解析或标准库语义。
4. P18 静态门禁已扩展：
   - `P18-2` 额外禁止 `FunctionSymbolMigrationTest`、`GenericTokenBridgeTest`、`MatrixFormBridgeTest`、`runBridgeCase`、`token_bridge`、`bridgedResult`、`matrixBridge`、`symbol_migration` 回流。
5. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-math -Dtest=MatrixFormConversionTest,MatrixFormTest,QuickDslTest -Dsurefire.failIfNoSpecifiedTests=false test"`：31/31 通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolRegressionTest,GenericTokenConversionTest,PrepareCacheKeyRegressionTest,LinearPolynomialBaselineTest,QuadraticPolynomialBaselineTest -Dsurefire.failIfNoSpecifiedTests=false clean test"`：20/20 通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。

### P18 第五批执行记录（2026-05-19）

继续清理测试名中的历史 `Bridge` 叙事，仅保留真实 `Flt64Bridge` 转换接口：

1. 测试命名已按实际职责回归：
   - `ExpressionRangeBridgeTest` -> `ExpressionRangeSolverBoundaryTest`
   - `ModelBuildingStatusBridgeTest` -> `ModelBuildingStatusMappingTest`
   - `ProductFunctionSolverBridgeEvaluationTest` -> `ProductFunctionSolverBoundaryEvaluationTest`
   - `FunctionSymbolToDoubleBridgeGuardTest` -> `FunctionSymbolToDoubleConversionGuardTest`
   - `CoreToDoubleBridgeGuardTest` -> `CoreToDoubleConversionGuardTest`
2. 测试内部 `solver_bridge` / `solverBridge` 叙事已改为 `solver_boundary` / `solverBoundary`。
3. P18 静态门禁已扩展，禁止以上旧测试名与旧测试变量名回流。

### P18 第六批执行记录（2026-05-19）

继续清理 `Typed` helper 命名，按“泛型正式路径使用原名，Flt64 边界显式命名”的原则处理 JVM 擦除冲突：

1. 泛型 flatten-data evaluation helper 已回归正式原名：
   - `evaluateTypedFlattenDataWithResults` -> `evaluateFlattenDataWithResults`
   - `evaluateTypedQuadraticFlattenDataWithResults` -> `evaluateQuadraticFlattenDataWithResults`
2. 原 Flt64 helper 已显式标注边界：
   - `evaluateFlattenDataWithResults` -> `evaluateFlt64FlattenDataWithResults`
   - `evaluateQuadraticFlattenDataWithResults` -> `evaluateFlt64QuadraticFlattenDataWithResults`
3. P18 静态门禁已扩展，禁止 `evaluateTypedFlattenDataWithResults` 与 `evaluateTypedQuadraticFlattenDataWithResults` 回流。
4. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=FunctionSymbolRegressionTest,ExpressionRangeSolverBoundaryTest,ModelBuildingStatusMappingTest,ProductFunctionSolverBoundaryEvaluationTest,FunctionSymbolToDoubleConversionGuardTest,CoreToDoubleConversionGuardTest -Dsurefire.failIfNoSpecifiedTests=false test"`：22/22 通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
5. `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile"` 曾因 5 分钟工具超时中断；已确认无后台 Maven/Java 残留，随后用上述 `-am` 目标测试完成编译链路验证。日志仍出现 JVM CodeHeap warning，但 Maven 结果为 `BUILD SUCCESS`。

### P18 第七批执行记录（2026-05-20）

继续清理 main 源码中只用于区分“泛型值”的局部 `*V` 命名，不改变领域 API：

1. 函数符号局部变量已回归普通语义名：
   - `posV` / `negV` -> `posVar` / `negVar`
   - `mV` -> `bigMValue`
   - `epsV` -> `toleranceValue`
   - `rhsV` -> `rhsValue`
   - `lowerV` / `upperV` -> `lowerValue` / `upperValue`
   - `amountV` -> `amountValue`
2. Benders Flt64 边界局部变量已回归边界语义名：
   - `dualAsV` -> `dualByConstraint`
   - `cutsV` -> `cuts`
   - “适配器边界 / Adapter boundary” 注释已改为“求解器边界 / Solver boundary”。
3. math geometry 局部 `pV` 已回归为 `exponentValue`。
4. P18 静态门禁已扩展，禁止上述 core 侧局部旧名回流。
5. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=BendersCutApiTest,BendersCutByIdApiTest,GenericBendersCutRegressionTest,QuadraticMechanismModelCutTest,FunctionSymbolRegressionTest -Dsurefire.failIfNoSpecifiedTests=false test"`：23/23 通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。

### P18 第八批执行记录（2026-05-20）

继续清理测试与注释中的迁移期局部命名和 `bridge` 叙事，避免正式实现继续呈现“兼容桥接”语义：

1. 泛型数值转换测试局部变量已回归普通转换语义：
   - `intoV` -> `convertedValue`
   - `backToV` -> `roundTripValue`
2. 求解器边界注释已从 `bridge` 叙事改为 `conversion` 叙事：
   - `MechanismModel.kt`
   - `TokenTable.kt`
   - `SolverBoundaryCasts.kt`
   - `FunctionSymbol.kt`
3. P18 静态门禁已扩展，禁止 `intoV` / `backToV` 回流。
4. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=GenericNumberConverterTest,GenericTokenConversionTest,ExpressionRangeSolverBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test"`：9/9 通过。日志仍出现 JVM CodeHeap warning，但 Maven 结果为 `BUILD SUCCESS`。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。

残留扫描结论：剩余 `Bridge`、`Typed`、`Type*`、`*V` 命名需要按语义判定。真实 Flt64 边界、solver 边界、解析 typed entry、value range、变量类型枚举、标准库调用与普通局部变量 `v` 等不构成公开 API 迁移痕迹；`mV`、`rhsV`、`dualAsV`、`intoV`、`backToV` 等迁移期风格局部名已纳入 P18 防回流规则。

### P18 第九批执行记录（2026-05-20）

继续把“兼容桥接”叙事从正式 API 中移除，并修复 example 全链路编译暴露出的 heuristic 插件泛型形状问题：

1. math/core 数值转换接口已从 `Bridge` 命名回归为正式转换语义：
   - `Flt64Bridge` -> `Flt64ValueConverter`
   - `Flt64BridgeTest` -> `Flt64ValueConverterTest`
   - `resolveFlt64Bridge` -> `resolveFlt64ValueConverter`
   - `IntoValue.fromBridge(...)` -> `IntoValue.fromConverter(...)`
   - `Flt64BridgeAdapter` -> `Flt64ValueConverterAdapter`
2. math 快捷 DSL 与文档已同步去除 `bridge` 叙事：
   - `QuickDsl` / `QuickOps` / `InequalityDsl` 使用 `converter` 参数与局部名。
   - math expression README/README_ch 将 PathSymbol 相关说明改为 adapter/legacy AST 语义。
3. heuristic 公共模型已拆分 `ObjValue` 与 `V`：
   - `Individual<ObjValue, V>`
   - `Population<T, ObjValue, V>`
   - `SolutionWithFitness<ObjValue, V>`
   - `refreshGoodIndividuals(..., model: AbstractCallBackModelInterface<*, ObjValue, V>)`
4. core 与 heuristic 插件算法已同步三参 callback 形状，避免多目标别名把目标值类型误当成解变量类型：
   - core `ParticleSwarmHeuristicSolver`、`Particle`、`HeuristicResult` 已拆为 `ObjValue, V`。
   - GA/GWO/MVO/PSO/SAA/SCA 的 policy、population、individual、running callback 与 `MulObj*` typealias 均改为 `Obj, ObjValue, V`。
   - `MulObjGA`、`MulObjGWO`、`MulObjMVO`、`MulObjPSO`、`MulObjSAA`、`MulObjSCA` 现在使用 `List<Flt64>` 作为 objective value，`Flt64` 作为 solution value。
5. P18 静态门禁已扩展：
   - `P18-4` 禁止 `Flt64Bridge`、`Flt64BridgeTest`、`resolveFlt64Bridge`、`fromBridge` 回流。
   - `P18-5` 禁止 heuristic API 回退到旧两参 callback、单泛型 `Individual/SolutionWithFitness/Chromosome/Wolf/Universe/Particle` 与旧多目标 typealias 形状。
6. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-math -Dtest=Flt64ValueConverterTest,ConceptCompileTest,QuickDslTest,MatrixFormConversionTest,MatrixFormTest -Dsurefire.failIfNoSpecifiedTests=false test"`：41/41 通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=MathInequalityFlattenTest,GenericNumberConverterTest,GenericTokenConversionTest,ExpressionRangeSolverBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test"`：18/18 通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=ParticleSwarmHeuristicSolverTest -Dsurefire.failIfNoSpecifiedTests=false test"`：1/1 通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -DskipTests compile"`：通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am -DskipTests compile"`：通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-example -am -DskipTests test-compile"`：通过。第一次 6 分钟工具超时，第二次 8 分钟 03 秒完成并 `BUILD SUCCESS`。
  - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过，含 `P18-4/P18-5`。
  - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过，含 `P18-4/P18-5`。

说明：上述 Maven 验证统一使用 `MAVEN_OPTS='-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=256m'` 降低 CodeHeap full 对长链路编译的影响；example 长链路末尾仍有 JVM CodeHeap warning，但 Maven 结果为 `BUILD SUCCESS`。

### P18 第十批执行记录（2026-05-20）

继续收口 `SymbolCombination` 的迁移期工厂命名，把仍然固定为 `Flt64` 的空符号工厂改成显式泛型入口，并把默认 `Flt64` 入口迁到前缀化边界名：

1. `LinearIntermediateSymbols` / `QuadraticIntermediateSymbols` 现在要求显式传入 `zero`，用于构造 `V` 版本的空符号组合。
2. 新增 `Flt64LinearIntermediateSymbols` / `Flt64QuadraticIntermediateSymbols` 作为显式 Flt64 边界入口。
3. `map` / `flatMap` 统一改为 `LinearPolynomial<V>` 泛型签名，不再固定 `Flt64`。
4. 修复四维 `map` / `flatMap` 的下标错误，`l4[v[4]]` 已回归为 `l4[v[3]]`。
5. 新增门禁 `P18-6`，禁止 `SymbolCombination` 再出现迁移期 Flt64 特化工厂签名。
6. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=SymbolCombinationGenericFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test"`：通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-example -am -DskipTests compile"`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过，含 `P18-6`。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过，含 `P18-6`。

### P18 第十一批执行记录（2026-05-20）

继续扫描后发现 `IntermediateSymbol.kt` 的 companion 工厂仍有“正式泛型类型名，但默认构造为 `Flt64`”的残留。已收口为显式数值域设计：

1. `LinearIntermediateSymbol.empty` / `QuadraticIntermediateSymbol.empty` 改为接收 `RealNumberConstants<V>`，按调用方给定的数值域构造空符号；`QuadraticIntermediateSymbol.empty` 显式保持 `Quadratic` 分类。
2. `LinearExpressionSymbol` 的变量项、已有 symbol、`LinearPolynomial`、`LinearMonomial`、`MutableLinearPolynomial`、typed constant、empty 工厂全部改为 `V` 泛型入口；无类型来源的入口要求显式传入 `Flt64` / `Rtn64` 等 constants。
3. `QuadraticExpressionSymbol` 同步改为 `V` 泛型入口，并补齐 `LinearPolynomial`、`LinearMonomial`、`QuadraticPolynomial`、`QuadraticMonomial`、`MutableQuadraticPolynomial` 工厂。
4. 仓库内 Flt64 模型的空表达式/空中间符号调用已改为显式 `LinearExpressionSymbol(Flt64, ...)` 或 `LinearIntermediateSymbol.empty(Flt64, ...)`；变量直接包裹的测试入口已改为 `LinearExpressionSymbol(x, Flt64, ...)` / `QuadraticExpressionSymbol(x, Flt64, ...)`。
5. 新增 `IntermediateSymbolGenericFactoryTest`，覆盖 Rtn64 的空符号、变量项、symbol 包装、单项式、多项式、mutable polynomial 与 constant 工厂。
6. 新增门禁 `P18-7`，禁止 `IntermediateSymbol.kt` 的 companion 工厂回退到 Flt64 特化签名、typed constant 转 Flt64、无 constants 的空工厂。
7. 已验证命令：
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -DskipTests compile"`：通过。
   - `pwsh.exe -NoProfile -Command "mvn --% -pl ospf-kotlin-core -am -Dtest=IntermediateSymbolGenericFactoryTest,SymbolCombinationGenericFactoryTest,MinimizeMaximizeSymbolTest -Dsurefire.failIfNoSpecifiedTests=false test"`：通过，13/13。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过，含 `P18-7`。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过，含 `P18-7`。
   - `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=768m -XX:NonProfiledCodeHeapSize=256m -XX:ProfiledCodeHeapSize=256m"; & mvn -pl ospf-kotlin-example -am -DskipTests compile'`：通过。
   - 验证中默认 JVM CodeHeap 较小会拖慢或触发 warning；增大 CodeHeap 后完整 example 编译通过。

### P18 第十二批执行记录（2026-05-20）

继续按“不要平滑兼容层，只保留正式设计”扫描后，进一步清理 `SymbolCombination` 与 callback 模型公开面：

1. 删除 `Flt64LinearIntermediateSymbols` / `Flt64QuadraticIntermediateSymbols` 特化对象；`LinearIntermediateSymbols` / `QuadraticIntermediateSymbols` 改为接收 `RealNumberConstants<V>`，由调用点显式选择 `Flt64` / `Rtn64` 等数值域。
2. `QuantityLinearIntermediateSymbol` 从固定 `Quantity<LinearIntermediateSymbol<Flt64>>` 改为泛型别名 `QuantityLinearIntermediateSymbol<V>`；demo2 业务模型显式标注 `QuantityLinearIntermediateSymbol<Flt64>`。
3. `AbstractCallBackModelInterface<Obj, V, TV>` 的迁移期类型参数命名改为正式语义：`AbstractCallBackModelInterface<Obj, ObjValue, SolutionValue>`，区分目标值类型与解变量值类型。
4. `SymbolCombinationGenericFactoryTest` 增补 `QuantityLinearIntermediateSymbol<Rtn64>` 覆盖，证明单数物理量别名不再绑定 Flt64。
5. `P18-5` / `P18-6` 门禁扩展：
   - 禁止 callback 接口回退到 `Obj, V, TV` 形状。
   - 禁止 `Flt64*IntermediateSymbols` 特化对象与旧固定 Flt64 单数物理量别名回流。
6. 扫描结论：公开声明级 `Type*` / `*V` 迁移命名已清零；剩余 `Flt64NumberParser` 属于 math 解析器的显式 Flt64 实现，不是兼容层残留。
7. 已验证命令：
   - `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=768m -XX:NonProfiledCodeHeapSize=256m -XX:ProfiledCodeHeapSize=256m"; mvn --% -pl ospf-kotlin-core -am -Dtest=SymbolCombinationGenericFactoryTest,IntermediateSymbolGenericFactoryTest,MinimizeMaximizeSymbolTest -Dsurefire.failIfNoSpecifiedTests=false test'`：通过，14/14。
   - `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-example -am -DskipTests compile'`：通过。
   - `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am -DskipTests compile'`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。

## 后续建议（P18+）

1. 保持 `check-c8-guards.ps1` 的 P17 规则开启，禁止 `math.symbol.adapter.*`、`FunctionCompat`、`MetaModelFlt64Adapter`、旧 solver 入口和旧 Slack 调用回流。
2. 将 `check-migration-compat.ps1` 拆分为可并行 CI job（core source-compat、example default、profiles、static guards），缩短反馈时延。
3. 在无兼容层前提下，逐步消减 allowlist/baseline（converter 样板、deprecated 调用、whitelist 特例）。
4. 对 `framework_demo/demo2` 增补更多结构断言，尤其是 `service/limits` 与 pipeline 组合层，降低后续 API 漂移回归风险。
5. 继续扫描剩余 `Type*` / `*V` 候选，优先处理只为迁移并存而产生的命名；真实领域语义、类型参数 `V`、View/Input/Converter/Adapter 等职责命名不机械回滚。
6. 外部 APS/CSP1D/BOP/PSP 仓库继续按项目侧节奏迁移，不纳入当前仓库默认门禁。
