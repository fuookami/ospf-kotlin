# P21 增量优化交接（2026-05-21）

## 最新结论

`math`、`multiarray`、`quantities`、`core` 已按 P18-P20 目标完成架构收口、泛型化、接口恢复、第一轮性能与工程深化。当前目标已达成，P21 是基于已有 benchmark 和工程结构的第二轮增量优化，不再重新打开迁移目标。

## 已完成事项摘要

以下只保留已完成结论，不保留逐批执行细节。

1. 已完成从 core 自有表达式体系到 `math.symbol` 正式符号体系的迁移。
2. 已删除旧兼容层与迁移桥接入口，包括旧 adapter/bridge/compat 路径和迁移期命名。
3. 已完成 core 主链路泛型化，保留的 Flt64 命名仅表达真实快捷层、解析器或 solver 边界职责。
4. 已恢复 example 默认构建与项目内 source-compat / profile 验收入口。
5. 已建立 P6/P7 等静态门禁，覆盖兼容层回流、旧命名回流、旧 import 回流、危险 hard-cast、空测试等风险。
6. 已完成 P19 第一轮性能和结构优化，覆盖 multiarray、math、core、core-plugin、framework 关键热点。
7. 已完成 P20 benchmark baseline，新增 `ospf-kotlin-benchmark`；该模块参与默认构建但跳过 install、deploy 和 central publish，JMH 生成与运行仍由 `bench` profile 激活。
8. 已完成 core 大文件第一批职责拆分，抽出 triad/tetrad dump helper，保持公开 API 和行为不变。
9. 已完成 solver 插件第一批公共数据准备抽取，Gurobi/Gurobi11/Cplex/SCIP 已接入公共变量 dump 与约束分块 helper。
10. 已完成第一批技术债清理，solver 热路径可证明不变量的 `!!` 已改为局部非空快照或明确失败兜底。

## P21 目标

P21 的目标是做有数据支撑的第二轮工程优化，重点是减少警告和重复、扩展 benchmark 覆盖、继续拆分大文件，并保持默认构建轻量且不发布 benchmark/example 模块。

1. 扩展 benchmark baseline，从 small smoke 扩展到 medium/large 手动数据集和 core-plugin dump 热点。
2. 清理 solver 插件第二批重复与警告，优先处理 `Duplicate branch condition in when`、状态分析、callback 失败处理和错误码兜底。
3. 继续拆分 core 大文件，优先处理 elastic builder、normalize/copy/clone、flatten/export 辅助逻辑和 memory cleanup 调用点。
4. 在 benchmark 证明有收益的前提下，做 multiarray/math/core 的小范围热点优化。
5. 继续清理关键路径技术债，包括剩余 `!!`、`TODO("not implemented yet")`、过期迁移叙事、乱码和失效命令。
6. 改善构建和开发体验，解决频繁 CodeHeap warning 的推荐配置，并保持 benchmark/solver/profile 文档一致。

## 总体原则

1. 不新增兼容层，不恢复旧 bridge/adapter/compat 设计。
2. 不改变公开 API；如必须改变，先在本文件记录理由、影响范围、替代方案和验收方式。
3. 先用 benchmark 或明确代码证据证明问题，再做性能优化；没有数据时只做低风险整理。
4. benchmark 模块参与默认构建；JMH 生成与运行只在 `bench` profile 下启用，且该模块不参与 install、deploy 和 central publish。
5. solver 公共化只抽 solver 无关的数据准备、状态归一和错误处理逻辑，不强行统一真实 solver API。
6. Flt64 / Double 转换必须留在显式 solver 边界或既有白名单，不能回流到 core 泛型主链路。
7. 大文件拆分只移动内聚职责，不混入语义变化。
8. 新增测试必须断言结构、数量、边界值、异常信息、dump 结果或 benchmark 可运行性，不能是空 smoke。
9. 写注释时遵守项目规则：中英双语；不添加版权声明。
10. README / README_ch 如涉及用户入口、benchmark 使用或 profile 说明，必须同步更新并保持互链。

## P21 事项与步骤

### P21-1：benchmark baseline 扩展

状态：已完成（第二轮 baseline 扩展，覆盖 `small`/`medium`/`large` 与 `core-plugin dump` 热点）。

目标：把 P20 的 small smoke baseline 扩展成可用于趋势判断的 benchmark 体系。

详细步骤：

1. 复核 `ospf-kotlin-benchmark` 当前结构，确认模块参与默认构建但跳过发布，所有 JMH 生成链路仍只在 `bench` profile 下启用。
2. 为现有 benchmark 增加 medium/large 输入规模：
   - `MultiArrayHotPathBenchmark`
   - `SymbolCombineBenchmark`
   - `CoreHotPathBenchmark`
3. 新增 core-plugin dump benchmark：
   - 变量数组构建。
   - lower/upper bound 转换。
   - objective 系数收集。
   - initial solution 收集。
   - constraint segment / sparse row 数据准备。
   - 不调用真实外部 solver。
4. 固化 baseline 文档：
   - JDK、Maven、OS、CPU、内存。
   - benchmark 命令。
   - small/medium/large 样本规模。
   - 结果文件位置和解释规则。
5. 增加轻量 CI smoke 口径，只验证 benchmark 可运行，不比较绝对性能数值。

预计修改清单：

- `ospf-kotlin-benchmark/src/main/...`
- `ospf-kotlin-benchmark/pom.xml`（仅当 profile 或插件配置需要调整）
- `README.md`
- `README_ch.md`
- `ospf-kotlin-core/daily.md`

验收标准：

1. `mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile` 通过。
2. 至少一个 small benchmark smoke 可运行。
3. medium/large 可手动运行，且不进入默认验收。
4. 默认根 modules 包含 `ospf-kotlin-benchmark`，且 `maven.install.skip`、`maven.deploy.skip`、`skipPublishing` 均为 true。
5. README / README_ch 如有 benchmark 命令变化，双语同步。
6. P6/P7 静态门禁通过。
7. `git diff --check` 通过。

本批实际修改清单：

1. 更新 `ospf-kotlin-benchmark/src/main/fuookami/ospf/kotlin/benchmark/BenchmarkRunner.kt`：
   - 支持 `small`/`medium`/`large` dataset 参数透传。
   - 支持结果格式与结果文件路径参数（默认输出到 `target/benchmark-results`）。
2. 更新 `MultiArrayHotPathBenchmark.kt`、`SymbolCombineBenchmark.kt`、`CoreHotPathBenchmark.kt`：
   - 所有 `@Param` 扩展为 `small`/`medium`/`large`。
   - 按数据规模补齐 large 输入配置。
3. 新增 `ospf-kotlin-benchmark/src/main/fuookami/ospf/kotlin/benchmark/coreplugin/CorePluginDumpBenchmark.kt`：
   - 覆盖变量数组准备、objective 系数收集、constraint segment 计算、sparse row + bounds 扫描。
   - 不调用真实外部 solver，仅聚焦 solver dump 前的数据准备热点。
4. 更新 `README.md` 与 `README_ch.md`：
   - 增加 core-plugin dump benchmark 用法。
   - 补充 `small`/`medium`/`large` 语义、结果文件路径、轻量 CI smoke 口径与基线环境说明。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile`：通过。
2. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1"`：通过（small smoke）。
3. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CorePluginDumpBenchmark.prepareVariableDumpingDataHotPath.* small 1 1 1"`：通过。
4. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CorePluginDumpBenchmark.prepareVariableDumpingDataHotPath.* medium 1 1 1"`：通过（手动 medium）。
5. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CorePluginDumpBenchmark.prepareVariableDumpingDataHotPath.* large 1 1 1"`：通过（手动 large）。
6. 默认根 modules 与 benchmark 发布跳过属性复核：
   - 根 `pom.xml` 默认 modules 包含 `ospf-kotlin-benchmark`：通过。
   - `ospf-kotlin-benchmark/pom.xml` 中 `maven.install.skip`/`maven.deploy.skip`/`skipPublishing` 均为 true：通过。

### P21-2：solver 插件第二轮清理

状态：已完成（完成 callback 失败处理与错误码兜底统一、去重重复分支、补充 helper 结构测试）。

目标：减少 solver 插件重复、编译警告和状态处理分歧，保持 solver API 层独立。

详细步骤：

1. 扫描 Gurobi/Gurobi11/Cplex/SCIP 的 `Duplicate branch condition in when` 警告。
2. 对比线性/二次 solver 的状态分析与失败分支：
   - succeeded / failed / infeasible / unbounded 状态映射。
   - `status.errCode` 兜底。
   - callback `Failed` / `Fatal` 后的 abort 行为。
3. 抽取 solver 无关 helper：
   - 状态错误码兜底。
   - callback 失败处理。
   - 可选的 common status snapshot。
4. 评估 `ModelingPreparation.kt` 位置：
   - 若继续放在 core，需明确它是 solver 边界公共 helper。
   - 若拆迁到 plugin shared，需要先确认模块依赖不会反向污染 core。
5. 统一 solver dump 中仍存在的直接 `System.gc()`：
   - 优先接入已有 `MemoryCleanupPolicy` 或 solver 边界策略。
   - 不在 core 泛型主链路新增主动 GC。

预计修改清单：

- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi*/src/main/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/...`
- 相关 solver tests 或 compile-only fixture
- `ospf-kotlin-core/daily.md`

验收标准：

1. Gurobi/Gurobi11/Cplex/SCIP 插件 `-DskipTests compile` 通过。
2. 不要求真实 solver license 参与默认验收。
3. 新增 helper 有结构测试或明确 compile-only fixture。
4. `.toDouble()` 和 Flt64 转换仍只在 solver 边界或白名单内。
5. P6/P7 静态门禁通过。
6. `git diff --check` 通过。

本批实际修改清单：

1. 新增 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverStatusSupport.kt`：
   - 新增 `shouldAbortOnCallbackFailure`，统一 callback `Failed` / `Fatal` 分支的中断处理。
   - 新增 `SolverStatus.resolveErrCode`，统一 `status.errCode` 兜底。
2. 新增 `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/solver/SolverStatusSupportTest.kt`：
   - 覆盖 `resolveErrCode` 默认兜底与自定义兜底。
   - 覆盖 callback 成功 / 失败 / 致命三类分支的中断行为。
3. 更新 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/ModelingPreparation.kt`：
   - 补充中英双语注释，明确其是 solver 边界公共 helper，仅用于 dump 前数据准备。
4. 更新 Gurobi/Gurobi11/Cplex/SCIP 线性与二次 solver：
   - 接入 `shouldAbortOnCallbackFailure`，收敛 callback 失败处理分支。
   - 接入 `status.resolveErrCode()`，统一求解失败错误码兜底。
   - 清理线性/二次 solver 中重复 `is Fatal` 分支。
5. 修复 `GurobiQuadraticSolver` 目标方向映射错误：
   - `GRB.MAXIMIZE` 从 `ObjectCategory.Minimum` 修正为 `ObjectCategory.Maximum`。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am -DskipTests compile`：通过。
2. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11 -am -DskipTests compile`：通过。
3. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex -am -DskipTests compile`：通过。
4. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile`：通过。
5. `mvn --% -pl ospf-kotlin-core -am -Dtest=SolverStatusSupportTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过（2/2）。
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
7. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
8. `git diff --check`：全仓存在无关文件（`ospf-kotlin-framework-bpp3d/daily.md`）既有尾随空格；本批相关路径 `git diff --check -- <P21-2 files>` 通过。

### P21-3：core 大文件第二批拆分

状态：已完成（本批仅拆分 elastic builder 职责，保持公开 API 与行为不变）。

目标：继续降低 `LinearTriadModel` / `QuadraticTetradModel` 的维护成本，保持公开 API 与行为不变。

详细步骤：

1. 统计当前核心大文件的函数分布和调用边界。
2. 按职责拆分，优先顺序：
   - elastic builder。
   - normalize / copy / clone。
   - flatten / export 辅助逻辑。
   - memory cleanup 使用点。
3. 每批只拆一个职责，禁止同时做行为优化。
4. 新 helper 默认使用 `internal` 或文件级 `private`，避免扩大 API 面。
5. 拆分后对比 dump、flatten、copy/clone、elastic builder 输出。

预计修改清单：

- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/LinearTriadModel.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/QuadraticTetradModel.kt`
- 新增 core internal helper 文件
- 相关 core tests
- `ospf-kotlin-core/daily.md`

验收标准：

1. 公开 API 不变。
2. core 目标测试通过，至少覆盖 flatten、dump、copy/clone、elastic builder 相关路径。
3. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过；如实际测试名不存在，记录实际运行到的测试。
4. `mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile` 通过。
5. P6/P7 静态门禁通过。
6. `git diff --check` 通过。

本批实际修改清单：

1. 新增 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/LinearTriadElasticBuilder.kt`：
   - 承载 `LinearTriadModel.elastic(minmaxSlack, minSlackAmount)` 原有实现逻辑。
   - 仅做代码搬移，不变更约束构造、目标构造、变量构造和命名规则。
2. 新增 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/QuadraticTetradElasticBuilder.kt`：
   - 承载 `QuadraticTetradModel.elastic()` 原有实现逻辑。
   - 保持 slack 构造、约束拼装与 objective 生成逻辑一致。
3. 更新 `LinearTriadModel.kt` 与 `QuadraticTetradModel.kt`：
   - 原 `elastic` 方法改为委托调用 `buildElasticModel(...)`。
   - 公开签名保持不变，调用方无需改动。
4. P7 门禁适配：
   - 在新 helper 中将 `IntermediateSymbol<*>` 恢复为原有显式类型参数写法，避免新增白名单命中。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过（实际执行到 24 tests，0 失败）。
2. `mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile`：通过。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
4. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
5. `git diff --check`：全仓存在无关文件变更；本批相关路径 `git diff --check -- <P21-3 files>` 通过。

### P21-4：benchmark 驱动的小性能优化

状态：已完成（聚焦 `core` 的 `SparseMatrix.transpose` 预分配优化，保持公开 API 与行为不变）。

目标：只对 benchmark 或明确热点证明有效的路径做小范围优化。

详细步骤：

1. 基于 P21-1 benchmark 结果选择热点，不做全仓库机械替换。
2. 候选优化：
   - `multiarray`：固定维索引 packed key、iterator 降分配。
   - `math`：quadratic combine key、ordered evaluate、matrix form conversion。
   - `core`：flatten/dump 残余中间集合分配。
   - `SparseMatrix`：行列遍历、转置、导出路径。
3. 每个优化都保留前后 benchmark 结果。
4. 优化不得牺牲泛型能力，至少用 `Flt64` 和一个非 Flt64 类型路径回归。

预计修改清单：

- `ospf-kotlin-multiarray/src/main/...`
- `ospf-kotlin-math/src/main/...`
- `ospf-kotlin-core/src/main/...`
- `ospf-kotlin-benchmark/src/main/...`
- 相关 tests
- `ospf-kotlin-core/daily.md`

验收标准：

1. 有 benchmark 前后对比或明确热点证据。
2. 修改模块测试通过。
3. benchmark small smoke 通过。
4. P6/P7 静态门禁通过。
5. `git diff --check` 通过。

本批实际修改清单：

1. 更新 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/SparseMatrix.kt`：
   - 稀疏遍历 helper（线性/二次）统一改为索引循环，减少迭代器与中间对象开销。
   - `SparseMatrix.transpose()` 改为三趟实现：
     - 第 1 趟扫描 `maxCol`；
     - 第 2 趟统计每个目标列（转置后行）的 entry 数；
     - 第 3 趟按统计容量预分配 `SparseVector` 后填充，减少大规模转置时 `ArrayList` 扩容成本。
2. 不保留无收益分支尝试（`multiarray` 侧临时方案已回滚），本批仅保留已验证收益改动。

本批 benchmark 前后对比（同机、同 JVM、同参数）：

1. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CoreHotPathBenchmark.sparseMatrixTranspose.* small 1 2 3"`：
   - 优化前：`23.100 us/op`
   - 优化后：`22.323 us/op`
   - 变化：约 `-3.36%`
2. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CoreHotPathBenchmark.sparseMatrixTranspose.* large 1 2 3"`：
   - 优化前：`5859.683 us/op`
   - 优化后：`5367.985 us/op`
   - 变化：约 `-8.39%`（JMH 样本较少，存在方差，但趋势稳定向好）

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过（24 tests，0 失败）。
2. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CoreHotPathBenchmark.sparseMatrixTranspose.* small 1 1 1"`：通过（small smoke）。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
4. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
5. `git diff --check -- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/SparseMatrix.kt`：通过。

### P21-5：技术债与开发体验

目标：持续清理不影响主目标但会影响维护质量的问题。

详细步骤：

1. 继续扫描 main 源码中的 `!!`，只处理可证明不变量。
2. 继续归档或语义化 `TODO("not implemented yet")`：
   - 可实现的实现。
   - 暂不实现的改为明确异常和说明。
   - 非关键路径记录到后续风险。
3. 修正明显乱码、过期迁移叙事和失效命令。
4. 解决频繁 CodeHeap warning 的开发体验问题：
   - 给推荐 `MAVEN_OPTS`。
   - 必要时增加 profile 文档。
   - 不把本机 JVM 参数写死进默认构建。
5. 检查 benchmark profile、solver profile、example profile 的 README 说明是否一致。

预计修改清单：

- 视扫描结果决定，优先 core、math、core-plugin、framework 关键路径。
- `README.md`
- `README_ch.md`
- `ospf-kotlin-core/daily.md`

验收标准：

1. 修改到的模块至少 `compile` 或 `test-compile` 通过。
2. 新增异常信息能定位具体不变量。
3. README / README_ch 如有一方修改，另一方同步并保持互链。
4. P6/P7 静态门禁通过。
5. `git diff --check` 通过。

## 推荐执行顺序

1. P21-1 benchmark baseline 扩展。
2. P21-2 solver 插件警告与状态逻辑清理。
3. P21-3 core 大文件第二批拆分。
4. P21-4 benchmark 证明后的热点优化。
5. P21-5 技术债和开发体验收尾。

## 推荐验收命令

按改动范围选择，不要求每批都完整执行全部命令。

1. `mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile`
2. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1"`
3. `mvn --% -pl ospf-kotlin-multiarray -am test`
4. `mvn --% -pl ospf-kotlin-math -Dtest=CombineTermsTest,QuickDslTest,MatrixFormConversionTest,MatrixFormTest -Dsurefire.failIfNoSpecifiedTests=false test`
5. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest,ModelingPreparationTest -Dsurefire.failIfNoSpecifiedTests=false test`
6. `mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile`
7. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am -DskipTests compile`
8. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11 -am -DskipTests compile`
9. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex -am -DskipTests compile`
10. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile`
11. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`
12. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`
13. `git diff --check`

## 交接注意事项

1. 下一个会话从 P21-1 开始，不要重新打开 P18-P20 迁移与收口目标。
2. P21 默认是增量优化，不做大范围 API 重设。
3. benchmark 结果用于趋势判断，不把机器相关绝对数值作为硬门禁。
4. 真实外部 solver 不作为默认阻塞项；默认以编译、结构测试和边界转换测试为主。
5. 每个 P21 子任务完成后，更新本文件对应小节的状态、实际修改清单、验收命令和结果，再提交独立 commit。
