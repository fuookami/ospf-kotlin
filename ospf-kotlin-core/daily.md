# P20 性能基准与工程深化交接（2026-05-20）

## 最新结论

`math`、`multiarray`、`quantities`、`core` 已按 P18/P19 目标完成架构收口：符号运算能力已迁移到正式 `math.symbol` 泛型体系，core 主链路保持泛型化，旧兼容迁移层、旧 adapter/bridge、迁移期命名和默认 Flt64 特化工厂已删除或边界化。

当前可以按项目内验收口径判定：功能与接口已恢复到原 Kotlin 版本预期；后续不再追求平滑迁移，不新增兼容层，只按正式 API、正式命名和正式边界继续做工程深化。

P19-1 至 P19-6 已完成第一轮简化与性能优化。最新相关提交：

1. `0c75425e fix(multiarray): restore BlockMultiArray map constructor`
2. `7c845618 chore(framework-gantt): harden task time conflict null checks`
3. `c766658e chore(framework-gantt): refine task limit null-guards in P19-6`
4. `38cb5460 chore(framework-gantt): harden P19-6 todo and null-safety paths`
5. `d3f87825 chore(gantt): 完成 P19-5 时间区间与日历扫描优化`
6. `2bf56808 chore(core-plugin): 批量优化 solver dump 数组与分块构建`
7. `cedc9e27 chore(core): 完成 P19-3 flatten dump 优化与内存清理策略`

## 已完成事项摘要

以下只保留 P18/P19 已完成结论，不再保留逐批流水账。

1. 已完成从 core 自有表达式体系到 `math.symbol` 正式符号体系的迁移。
2. 已删除旧兼容面：`math.symbol.adapter.*`、`FunctionCompat`、`MetaModelFlt64Adapter`、旧 bridge/compat 入口等。
3. 已把 core 主链路泛型化，并验证 `Flt64`、`FltX`、`Rtn64`、`RtnX` 等数值路径不会退化为单 Flt64 适配。
4. 已清理 P18 迁移期命名，公开声明级 `Type*` / `*V` 迁移命名已清零；保留的 Flt64 命名仅表达真实快捷层、解析器或 solver 边界职责。
5. 已把 `IntermediateSymbol`、`SymbolCombination` 等工厂从默认 Flt64 特化改为显式 `RealNumberConstants<V>` 泛型入口。
6. 已恢复 example 默认构建，把曾经的 non-default demo 迁回默认源码集，并增强 core demo、function build-only、business source-compat、framework/starter compat 等验收 profile。
7. 已建立并扩展 `check-c8-guards.ps1` 与 `check-migration-compat.ps1`，覆盖兼容层回流、旧命名回流、旧 import 回流、危险 hard-cast、空测试等静态风险。
8. 已完成 P19 第一轮性能与结构优化：multiarray 索引/迭代、math 符号合并、core flatten/dump/sparse transpose、solver dump 数组构建、framework 时间扫描。
9. 已完成 P19 第一轮中低优先级技术债清理：部分关键 TODO 语义化、部分 `!!` 改为显式不变量检查。
10. 已修复 P19 复核发现的 `BlockMultiArray(shape, Map<List<Int>, T>)` 公开构造入口回归，内部仍保留 `IndexKey` 优化路径。

## P20 目标

P20 是性能基准与工程深化阶段，不是迁移阶段。目标是把 P19 的“结构上更干净、热点上更快”推进到可量化、可维护、可持续演进。

1. 建立可重复运行的 benchmark baseline，用数据验证 multiarray、math、core、core-plugin 的关键路径。
2. 在不改变公开 API 的前提下拆分局部大文件，优先降低 `LinearTriadModel` / `QuadraticTetradModel` 的维护成本。
3. 抽取 solver 插件中 solver 无关的数据准备逻辑，减少 Gurobi/Gurobi11/Cplex/SCIP 重复代码。
4. 继续保持无兼容层原则：不新增 `compat`、`bridge`、旧包路径或仅为平滑迁移存在的包装。
5. 保持泛型核心与边界层清晰：core/math 主链路泛型化；Flt64 快捷层和 solver double 转换只允许出现在显式边界。

## 总体原则

1. 先基准，后优化。没有 baseline 的性能改动只能做低风险整理，不做大规模重写。
2. P20 默认不改变公开 API；如必须调整公开入口，先在 `daily.md` 记录理由、影响范围和验收方式。
3. 不新增迁移兼容层。恢复公开 API 回归可以做，但不能为了平滑迁移保留旧实现分支。
4. 大文件拆分以行为保持为第一目标，只移动内聚逻辑，不混入语义变化。
5. solver 公共化只抽 solver 无关的数据准备层，不抽真实 solver API 调用层。
6. solver 的 `.toDouble()`、Flt64 转换和外部 solver 类型适配必须留在 solver 边界，不能回流到 core 泛型主链路。
7. 新增测试不能是空 smoke。必须断言结构、数量、边界值、类型保持、异常信息、dump 结果或 benchmark 可运行性。
8. 写注释时遵守项目规则：中英双语；不添加版权声明。
9. README / README_ch 如涉及用户入口或 benchmark 使用方式变更，必须保持互链和同步说明。
10. 每个 P20 子任务独立提交，完成后更新本文件对应小节的状态、实际修改清单、验收命令和结果。

## P20 计划

### P20-1：性能基准 baseline

状态：已完成（第一批 baseline，覆盖 `multiarray`/`math`/`core`）。

目标：建立可重复运行的性能基准，覆盖 P19 已优化和 P20 计划继续深化的热点路径。

详细步骤：

1. 先调查现有 benchmark 基础：
   - `ospf-kotlin-utils` 已有 JMH 依赖和 `src/benchmark` 配置，可作为参考。
   - 目标模块目前未发现独立 benchmark 文件，优先选择最小侵入方案。
2. 设计 benchmark 承载方式：
   - 优先方案 A：新增专用 benchmark module，例如 `ospf-kotlin-benchmark`，依赖目标模块并集中放置 JMH。
   - 备选方案 B：在目标模块增加 `src/benchmark` 和 profile，但要避免默认构建变重。
3. 建立第一批 benchmark：
   - `multiarray`：`BlockMultiArray.get/set/getOrSet/contains/remove`、`AccessOrder` iterator、`Shape.index/vector`、`MultiArray.fromList/flatten`。
   - `math`：linear/quadratic combine、mutable combine、matrix form conversion、symbol evaluate / ordered evaluate。
   - `core`：flatten、model dump、`SparseMatrix.transpose`、`LinearTriadModel` / `QuadraticTetradModel` 构建与导出。
   - `core-plugin`：变量数组构建、边界转换、初始解收集、约束分块构建；默认不调用真实 solver。
4. 设计输入规模：
   - small：用于快速 CI 验证 benchmark 可运行。
   - medium：用于本地趋势对比。
   - large：只作为手动 profile，不进入默认验收。
5. 固化 baseline 输出：
   - 记录 JVM、Maven 命令、样本规模、关键指标。
   - 不把机器相关的绝对性能数值作为 CI 硬门禁，先用 baseline 文档和可运行性验收。

预计修改清单：

- 新增 benchmark module 或目标模块 benchmark profile。
- 新增 benchmark 源码目录。
- 新增或更新 Maven profile。
- 如涉及使用方式，更新 `README.md` 与 `README_ch.md`。
- 更新 `ospf-kotlin-core/daily.md` 的 P20-1 状态与结果。

验收标准：

1. benchmark 代码能编译并能通过指定 profile 运行 small 规模。
2. 至少覆盖 `multiarray`、`math`、`core` 三类热点；`core-plugin` 可作为同批或紧随其后的补充。
3. 默认 `mvn test` 不因 benchmark 变慢。
4. `mvn --% -pl ospf-kotlin-multiarray -am test`、相关 math/core 目标测试通过。
5. P6/P7 静态门禁通过。
6. `git diff --check` 通过。

本批实际修改清单：

1. 根聚合 `pom.xml` 增加 module：`ospf-kotlin-benchmark`。
2. 新增 `ospf-kotlin-benchmark/pom.xml`：
   - 依赖 `multiarray`/`math`/`core` 与 `jmh-core:1.37`。
   - 引入 JMH bytecode generator 流程（`JmhBytecodeGenerator` + generated sources 编译）。
   - 保留 `bench` profile，默认构建不跑 benchmark。
3. 新增 benchmark 入口：
   - `ospf-kotlin-benchmark/src/main/fuookami/ospf/kotlin/benchmark/BenchmarkRunner.kt`
4. 新增第一批 benchmark：
   - `.../multiarray/MultiArrayHotPathBenchmark.kt`
   - `.../math/SymbolCombineBenchmark.kt`
   - `.../core/CoreHotPathBenchmark.kt`

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile`：通过。
2. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1"`：通过（small 烟测可运行）。
3. `mvn --% -pl ospf-kotlin-multiarray -am test`：通过。
4. `mvn --% -pl ospf-kotlin-math -Dtest=CombineTermsTest,QuickDslTest,MatrixFormConversionTest,MatrixFormTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过。
5. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过（本轮实际运行到 `MathInequalityFlattenTest`、`SparseMatrixTransposeTest`、`FlattenUtilityTest`）。
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
7. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
8. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

### P20-2：core 大文件局部拆分

状态：已完成（第一批：dump/export helper 职责拆分，行为保持）。

目标：降低 `LinearTriadModel.kt` 与 `QuadraticTetradModel.kt` 的维护成本，保持公开 API 和行为不变。

详细步骤：

1. 先用 `git diff --stat`、文件行数和函数分布确认拆分范围。
2. 优先拆分内聚且边界清晰的逻辑：
   - dump / export helper。
   - elastic constraint builder。
   - normalize / copy / clone helper。
   - expression flatten / constraint flatten helper。
   - memory cleanup policy 使用点整理。
3. 每批只拆一个职责，禁止在同一 commit 中混入行为优化。
4. 新 helper 默认使用 `internal` 或文件级 `private`，避免扩大 API 面。
5. 拆分后对比关键 dump / flatten / copy 测试，确保输出结构、变量数量、约束数量和类型保持不变。

预计修改清单：

- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/frontend/model/LinearTriadModel.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/frontend/model/QuadraticTetradModel.kt`
- 新增 core internal helper 文件，具体名称由执行会话按职责决定。
- 相关 core tests。
- `ospf-kotlin-core/daily.md`

验收标准：

1. 公开 API 不变；如不可避免，必须先记录理由并补 source-compat 验收。
2. 目标测试通过，至少覆盖 flatten、dump、copy/clone、elastic builder 相关路径。
3. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过；如测试名不存在，记录实际运行到的测试。
4. `mvn --% -pl ospf-kotlin-core -DskipTests test-compile` 通过。
5. P6/P7 静态门禁通过。
6. `git diff --check` 通过。

本批实际修改清单：

1. 新增 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/LinearTriadDumpBuilders.kt`：
   - 抽出 `LinearTriadModel` 的 dump helper：`dumpLinearTriadVariables`、`dumpLinearTriadConstraints`、`dumpLinearTriadConstraintsAsync`、`dumpLinearTriadObjectives`。
   - 抽出 `buildLinearSparseLhs`，供模型内复用。
2. 新增 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/QuadraticTetradDumpBuilders.kt`：
   - 抽出 `QuadraticTetradModel` 的 dump helper：`dumpQuadraticTetradVariables`、`dumpQuadraticTetradConstraints`、`dumpQuadraticTetradConstraintsAsync`、`dumpQuadraticTetradObjectives`。
   - 抽出 `buildQuadraticSparseLhs`，供模型内复用。
3. 更新 `LinearTriadModel.kt`：
   - 删除类内重复的 dump helper 与旧 `buildSparseLhs`。
   - `from(...)`/`invoke(...)` 等路径改为调用新 helper。
   - 原先 `buildSparseLhs` 调用点改为 `buildLinearSparseLhs`。
4. 更新 `QuadraticTetradModel.kt`：
   - 删除类内重复的 dump helper 与旧 `buildSparseLhs`。
   - `from(...)`/`invoke(...)` 等路径改为调用新 helper。
   - 原先 `buildSparseLhs` 调用点改为 `buildQuadraticSparseLhs`。
5. 更新 `ospf-kotlin-core/scripts/p7-whitelist.json`：
   - 增加两个新 helper 文件在 P7 `<Flt64>` 与 `<*>` 白名单中的计数，保持 guard 口径一致。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过（实际运行到 `FlattenUtilityTest`、`MathInequalityFlattenTest`、`SparseMatrixTransposeTest`）。
2. `mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile`：通过。
3. `mvn --% -pl ospf-kotlin-core -DskipTests test-compile`：本机单模块口径存在既有依赖解析差异；按聚合口径复现并通过：`mvn --% -pl ospf-kotlin-core -am -DskipTests -Dgpg.skip=true test-compile`。
4. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
5. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过（已包含新 helper 文件 whitelist 更新）。
6. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

### P20-3：solver 插件数据准备公共化

状态：已完成（第一批：变量边界/名称/初始解与约束分块大小公共化，保持 solver API 层独立）。

目标：减少 Gurobi/Gurobi11/Cplex/SCIP solver dump 数据准备重复，同时保持各 solver API 调用层独立。

详细步骤：

1. 对比以下插件的变量数组、边界数组、目标系数、初始解、约束 chunk 构建路径：
   - `ospf-kotlin-core-plugin-gurobi`
   - `ospf-kotlin-core-plugin-gurobi11`
   - `ospf-kotlin-core-plugin-cplex`
   - `ospf-kotlin-core-plugin-scip`
2. 抽取 solver 无关的数据准备结构：
   - variable index/name map。
   - lower / upper bound 数组。
   - objective coefficient 数组。
   - initial solution 数组。
   - sparse row / chunk 数据。
3. 保留 solver API 层差异：
   - 不抽 `GRBModel`、`IloCplex`、SCIP 原生对象调用。
   - 不把 solver 特定异常和参数强行统一。
4. 保持 Flt64 / Double 边界显式：
   - `.toDouble()` 只允许留在 solver dump/solve 边界或既有白名单。
   - 不把 Double 渗透到 core 泛型模型。
5. 为公共数据准备层补结构测试，不依赖真实 solver license。

预计修改清单：

- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi*/src/main/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/...`
- 新增 solver dump 公共 helper，位置由执行会话根据现有模块边界决定。
- 相关 plugin tests 或 compile-only fixture。
- `ospf-kotlin-core/daily.md`

验收标准：

1. 相关插件 `-DskipTests compile` 通过。
2. 不要求真实外部 solver 参与默认验收；如本机有 license，可额外运行条件集成测试并记录。
3. 公共 helper 有结构断言测试或 compile-only fixture 覆盖。
4. P6/P7 静态门禁通过。
5. `git diff --check` 通过。

本批实际修改清单：

1. 新增 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/ModelingPreparation.kt`：
   - 抽取 solver 无关变量数据准备：`prepareVariableDumpingData(variables, scopeName)`。
   - 抽取约束分块大小计算：`computeConstraintSegmentSize(constraintSize, availableProcessors)`。
2. 新增 `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/solver/ModelingPreparationTest.kt`：
   - 断言 lower/upper bounds、names、initialResults 的结构与值。
   - 断言约束分块大小在边界规模下的行为。
3. 更新 `ospf-kotlin-core-plugin-gurobi`：
   - `GurobiLinearSolver.kt`、`GurobiQuadraticSolver.kt` 改为复用 `prepareVariableDumpingData`。
   - 删除插件内重复 `computeConstraintSegmentSize` 私有实现，改为复用 core helper。
4. 更新 `ospf-kotlin-core-plugin-gurobi11`：
   - `GurobiLinearSolver.kt`、`GurobiQuadraticSolver.kt` 改为复用 `prepareVariableDumpingData`。
   - 删除插件内重复 `computeConstraintSegmentSize` 私有实现，改为复用 core helper。
5. 更新 `ospf-kotlin-core-plugin-cplex`：
   - `CplexLinearSolver.kt`、`CplexQuadraticSolver.kt` 改为复用 `prepareVariableDumpingData`。
   - 删除插件内重复 `computeConstraintSegmentSize` 私有实现，改为复用 core helper。
6. 更新 `ospf-kotlin-core-plugin-scip`：
   - `ScipLinearSolver.kt`、`ScipQuadraticSolver.kt` 改为复用 `prepareVariableDumpingData`。
   - 删除插件内重复 `computeConstraintSegmentSize` 私有实现，改为复用 core helper。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am -DskipTests -Dgpg.skip=true compile`：通过。
2. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11 -am -DskipTests -Dgpg.skip=true compile`：通过。
3. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex -am -DskipTests -Dgpg.skip=true compile`：通过。
4. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests -Dgpg.skip=true compile`：通过。
5. `mvn --% -pl ospf-kotlin-core -am -Dtest=ModelingPreparationTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过（2 tests, 0 failure）。
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
7. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
8. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

### P20-4：后续技术债与文档边界

状态：待执行，可穿插在 P20-1 至 P20-3 之间。

目标：把 P19 后剩余的中低优先级技术债纳入持续清理，不阻塞主线。

详细步骤：

1. 继续扫描 main 源码中的 `!!`，只处理能明确表达不变量的路径。
2. 继续归档或语义化 `TODO("not implemented yet")`，优先处理关键路径。
3. 检查 README / README_ch 中泛型核心层、Flt64 快捷层、solver 边界层的说明是否需要更新。
4. 修正明显乱码、过期迁移叙事和已失效命令。

预计修改清单：

- 视扫描结果决定，优先 core、math、core-plugin、framework 关键路径。
- README / README_ch（仅当用户入口或说明变化）。
- `ospf-kotlin-core/daily.md`

验收标准：

1. 修改到的模块至少 `test-compile` 或 `compile` 通过。
2. 新增异常信息必须可定位具体不变量。
3. README / README_ch 如有一方修改，另一方同步并保持互链。
4. P6/P7 静态门禁通过。
5. `git diff --check` 通过。

## 推荐执行顺序

1. P20-1 benchmark baseline：先建立数据口径，避免盲目优化。
2. P20-2 core 大文件拆分：拆结构，不改行为。
3. P20-3 solver 插件公共化：在 benchmark 和 core dump 结构稳定后做。
4. P20-4 技术债与文档边界：穿插执行，每批保持小改动。

## 推荐验收命令

按改动范围选择，不要求每批都完整执行全部命令。

1. `mvn --% -pl ospf-kotlin-multiarray -am test`
2. `mvn --% -pl ospf-kotlin-math -Dtest=CombineTermsTest,QuickDslTest,MatrixFormConversionTest,MatrixFormTest -Dsurefire.failIfNoSpecifiedTests=false test`
3. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test`
4. `mvn --% -pl ospf-kotlin-core -DskipTests test-compile`
5. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am -DskipTests compile`
6. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11 -am -DskipTests compile`
7. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex -am -DskipTests compile`
8. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile`
9. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`
10. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`
11. `git diff --check`

12. `mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile`
13. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1"`
14. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*SymbolCombineBenchmark.combineLinearPolynomialGeneric.* small 1 1 1"`
15. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CoreHotPathBenchmark.sparseMatrixTranspose.* small 1 1 1"`

benchmark 数值只用于同机趋势对比与回归观察，不作为跨机器硬门禁。

## 交接注意事项

1. 下一个会话从 P20-1 开始，不要重新打开 P18/P19 迁移目标。
2. 不要新增兼容层；P20 的默认姿态是正式 API 继续深化。
3. 不要把真实外部 solver 作为默认阻塞项；默认以编译、结构测试和边界转换测试为主。
4. 大文件拆分必须小步提交，避免行为变化和移动代码混在一起。
5. 每个子任务完成后，更新本文件对应小节的状态、实际修改清单、验收命令和结果，再提交独立 commit。
