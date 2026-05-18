# core API 易用性与原版兼容计划（2026-05-14 起）

## 目标

本轮目标是对比原始 Kotlin 版本：

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

除上述两项架构变化外，用户侧应尽量保留原始版本的接口名称、快捷接口、建模表达方式和示例组织方式。迁移后的示例和业务项目不应依赖大量样板代码才能表达原始模型。

本轮新增四个完整原始业务项目作为真实迁移语料。它们不是简单示例，实际覆盖 starter、framework、Gantt scheduling、CSP1D、solver 插件、列生成、`Pipeline`/`PipelineList`、`AbstractLinearMetaModel`/`AbstractQuadraticMetaModel`、`LinearIntermediateSymbolsN`/`QuadraticIntermediateSymbolsN`、变量族和解分析链路。因此本轮计划需要从 core API 易用性扩展为“core + framework + starter + 业务项目编译链路”的兼容闭环。

业务项目 APS/CSP1D/BOP/PSP 不需要纳入默认直接编译门禁。当前仓库默认门禁只要求 in-repo source-compat fixture、starter/framework 依赖闭包和 build-only 结构验证通过；外部仓库直接编译属于项目侧升级工作，不作为本仓库默认 release gate 的阻塞项。

由于本次 core 已泛型化，凡是迁移后必须显式指定数值类型的易用入口，都不能只补 `Flt64`。默认兼容层至少提供 `Flt64`、`FltX`、`Rtn64`、`RtnX` 四个版本；`Flt64` 继续作为原版业务项目的最小迁移路径，另外三种类型用于验证泛型能力没有退化为单类型适配。

## 总体原则

1. 原始示例与四个业务项目共同作为功能与易用性的基准。若原代码中存在稳定公开用法，当前版本应提供同名接口、兼容包装或清晰的一步替代。
2. 架构变更只允许影响内部实现和必要类型参数，不应迫使 `Flt64/FltX/Rtn64/RtnX` 用户手写 converter、低层 flatten data 或冗长构造器。
3. `Flt64` 默认路径必须足够顺手：`LinearMetaModel("name")`、`QuadraticMetaModel()`、常用函数符号和 solver 调用应保持接近原版体验。
4. 泛型路径必须保持一等公民：新增兼容接口不能只服务 `Flt64`。凡是因为泛型化必须暴露类型选择的易用入口，至少提供 `Flt64`、`FltX`、`Rtn64`、`RtnX` 四套入口或等价工厂。
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
powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6
powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7
```

结果：P6/P7 静态门禁均通过。

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

## 后续建议（P17+）

1. 将 `check-migration-compat.ps1` 拆分为可并行 CI job（core source-compat、example default、profiles、static guards），缩短反馈时延。
2. 在不破坏兼容层前提下，逐步消减 allowlist/baseline（converter 样板、deprecated 调用、whitelist 特例）。
3. 对 `framework_demo/demo2` 增补更多结构断言，尤其是 `service/limits` 与 pipeline 组合层，降低后续 API 漂移回归风险。
4. 保持 `check-c8-guards.ps1` 的 P14/P16 规则开启，禁止旧 import 与 non-default 目录回流。
5. 外部 APS/CSP1D/BOP/PSP 仓库继续按项目侧节奏迁移，不纳入当前仓库默认门禁。
