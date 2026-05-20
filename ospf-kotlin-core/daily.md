# core/math P18+ 后续简化与性能优化交接（2026-05-20）

## 最新结论

P18 已完成无兼容层收口：core 已在架构上把符号运算能力迁移到正式 `math.symbol` 体系，core 主链路保持泛型化，旧兼容层、旧桥接命名和默认 Flt64 特化工厂已移除或边界化。

接下来进入 P19：不再围绕“兼容原旧入口”继续补平滑层，而是在 P18+ 基础上做全面简化、性能优化和技术债清理。优化范围覆盖：

- `ospf-kotlin-multiarray`
- `ospf-kotlin-math`
- `ospf-kotlin-core`
- `ospf-kotlin-core-plugin`
- `ospf-kotlin-framework-*`

## 目标

P19 的目标是让 P18+ 后的正式设计更干净、更快、更容易维护：

1. 降低热点路径的临时对象分配，尤其是 multiarray 索引、符号合并、模型 dump、solver dump、时间区间扫描。
2. 消除 P18 迁移后剩余的重复实现，例如 Flt64 专用逻辑与泛型正式逻辑的重复。
3. 把仍然散落的 Flt64 快捷层、solver double 边界、泛型核心层边界写清楚，并用静态门禁防止回流。
4. 简化 core 中大型 builder/dump 代码，降低 `LinearTriadModel` / `QuadraticTetradModel` 的维护成本。
5. 对 framework 中真实未实现路径和长尾风险做归档、门禁或实现计划，不让 `TODO("not implemented yet")` 隐性进入关键路径。

## 总体原则

1. 继续坚持无兼容层：不要新增 `compat`、`bridge`、旧包路径或仅为平滑迁移存在的包装。
2. 泛型正式 API 使用原名；Flt64 快捷层和 solver 边界必须显式命名并隔离。
3. 优先优化已确认的热点和结构性重复，不做无证据的全局机械替换。
4. 性能优化不能牺牲泛型能力；至少用 `Flt64` 与一个非 Flt64 类型（优先 `Rtn64`）做回归验证。
5. 低层集合链式调用只有在 dump、flatten、solver 转换、multiarray 迭代、framework 时间扫描等热点路径中优先处理。
6. 不默认强制真实外部 solver 参与验收；solver 插件默认以编译、结构和边界转换验证为主，真实 solver 用 profile 或条件集成测试。
7. 不用空 smoke 验收。新增测试必须断言结构、数量、边界值、类型保持、异常信息或求解器边界转换结果。
8. 写注释时遵守项目规则：中英双语；不添加版权声明。
9. README / README_ch 如涉及用户入口变更，需保持互链和同步说明。

## 已完成事项摘要

以下为 P0-P18 已完成内容的高层摘要，不再保留逐批流水账：

1. 已完成从 core 自有表达式体系到 `math.symbol` 正式符号体系的迁移。
2. 已删除兼容层目录与旧入口，移除 `math.symbol.adapter.*`、`FunctionCompat`、`MetaModelFlt64Adapter` 等旧兼容面。
3. 已把 core 主链路泛型化，并验证 `Flt64`、`FltX`、`Rtn64`、`RtnX` 等数值路径不会退化为单 Flt64 适配。
4. 已恢复 example 默认构建，把曾经的 non-default demo 迁回默认源码集。
5. 已恢复并增强 core demo、function build-only、business source-compat、framework/starter compat 等验收 profile。
6. 已建立并扩展 `check-c8-guards.ps1` 与 `check-migration-compat.ps1`，覆盖兼容层回流、旧命名回流、旧 import 回流、危险 hard-cast、空测试等静态风险。
7. 已清理 P18 迁移期命名，公开声明级 `Type*` / `*V` 迁移命名已清零；保留的 Flt64 命名仅表达真实快捷层、解析器或 solver 边界职责。
8. 已把 `IntermediateSymbol`、`SymbolCombination` 等工厂从默认 Flt64 特化改为显式 `RealNumberConstants<V>` 泛型入口。
9. 已把 callback/heuristic 等多目标路径拆清 `ObjValue` 与 `SolutionValue`，避免目标值类型和解变量值类型混淆。
10. P18 结束时工作区已提交，最新相关提交为 `98790ce6 chore(core): 收口符号工厂泛型入口`。

## P19 扫描结论

2026-05-20 已完成一次静态扫描和抽样阅读。结论如下。

### 高优先级

1. `multiarray` 索引路径分配偏高：
   - `BlockMultiArray` 每次 `get/set/getOrSet` 都把 `IntArray` 转成 `List<Int>` 作为 map key。
   - `MultiIndexIterator.hasNext()` 每次复制当前位置并试探推进。
   - 固定维 `Shape` 的 column-major `vector()` 使用 `reversedArray()`，存在可避免的小额分配。
2. `math` 符号合并存在重复实现：
   - Flt64 专用 `combineTerms()` 与泛型 `combineLinearTerms` / `combineQuadraticTerms` 逻辑重复。
   - quadratic 合并使用 `Pair<Symbol, Symbol?>` 作为 key，热点下会产生大量临时对象。
3. `core` 模型 dump 与 elastic builder 代码体量大、重复多：
   - `LinearTriadModel.kt` 与 `QuadraticTetradModel.kt` 均超过 2k 行。
   - elastic 构造中大量 `flatMap + listOf + +` 拼接会产生中间 List。
   - 多处 `System.gc()` 应改为可配置内存策略，默认不主动触发。
4. `core-plugin` solver dump 重复明显：
   - Gurobi/Gurobi11/Cplex/Scip 等插件变量数组、约束分段、初始解、边界 double 转换逻辑相似。
   - 部分路径对变量多次 `map`，可以单趟填充 primitive/object 数组。
   - 约束 chunk 大小用 `Flt64.lg()/pow()` 计算，适合改成纯整数策略。
5. `framework-gantt-scheduling` 时间扫描可优化：
   - `WorkingCalendar` 多处对有序时间段做线性扫描。
   - `TimeRange` / `TimeWindow` 有 `filter + map`、`withIndex().indexOfFirst/Last` 等可单趟或二分优化的路径。

### 中优先级

1. `!!` 密度偏高，尤其在 core、math、core-plugin、gantt framework 中。建议逐步改成 `requireNotNull`、局部不变量 helper 或领域异常，提升错误信息。
2. `TODO("not implemented yet")` 需要归档或门禁，主要集中在 framework-gantt、bpp3d layer assignment、Mosek plugin、framework-plugin persistence。
3. Flt64 快捷层边界仍需文档化：`Flt64QuickDsl`、`Flt64QuickOps`、`Flt64MatrixForm` 可以保留，但应明确它们不是泛型核心层。
4. solver 插件重复代码短期不阻塞，但后续修改一个 solver 时容易漏另一个。
5. core 大文件需要拆分或抽 helper，尤其是 dump、elastic、export、normalize/copy 相关逻辑。

### 低优先级

1. 固定维 `Shape` 的 `lazy` 和 `reversedArray()` 属于小额性能优化，低于 sparse key 和 iterator 优先级。
2. 非热点 builder 中的 `map/filter/flatMap + toList` 不必机械改。
3. 少量注释和文案有乱码或迁移叙事残留，可顺手修复。
4. framework-bpp1d、bpp2d、csp2d、network-scheduling 当前没有明显 Kotlin 热点，暂不优先投入。
5. README/daily 可补一张“泛型核心层 / Flt64 快捷层 / solver 边界层”边界表，避免后续误用。

## P19 计划

### P19-1：multiarray 索引与迭代优化

状态（2026-05-20）：已完成第一批落地并通过模块回归。

本批实际修改清单：

- `ospf-kotlin-multiarray/src/main/fuookami/ospf/kotlin/multiarray/BlockMultiArray.kt`
- `ospf-kotlin-multiarray/src/main/fuookami/ospf/kotlin/multiarray/AccessOrder.kt`
- `ospf-kotlin-multiarray/src/main/fuookami/ospf/kotlin/multiarray/Shape.kt`
- `ospf-kotlin-multiarray/src/test/fuookami/ospf/kotlin/multiarray/BlockMultiArraySemanticTest.kt`
- `ospf-kotlin-multiarray/src/test/fuookami/ospf/kotlin/multiarray/AccessOrderIteratorContractTest.kt`

本批验收命令与结果：

1. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="..."; mvn --% -pl ospf-kotlin-multiarray -am test'`：通过。
2. `git diff --check`：通过（仅行尾符提示，无空白错误）。

目标：优先处理低风险、高收益的分配热点。

详细步骤：

1. 为 `BlockMultiArray` 设计专用 key：
   - 可选方案 A：内部 `data class IndexKey(private val indices: IntArray)`，构造时 copy，重写 `equals/hashCode`。
   - 可选方案 B：1-4 维 packed key + DynShape fallback。
   - 先选风险较低的方案 A，保留外部 `indices(): Set<List<Int>>` 行为。
2. 将 `get/set/getOrSet/contains/remove/fromMultiArray/toMultiArray` 内部切到新 key。
3. 优化 `MultiIndexIterator`：
   - 用 `count < shape.size` 或预计算 total size 判断 `hasNext()`。
   - 避免 `hasNext()` copy + `advance(temp)`。
4. 优化 `MultiArray.fromList`：
   - 避免先用 `list[0]` 全量初始化再覆盖。
   - 对 storage order 等于 shape order 的路径可直接构建。
5. 对 `Shape3/Shape4/DynShape` 的 column-major `vector()` 去掉 `reversedArray()` 临时分配。

预计修改清单：

- `ospf-kotlin-multiarray/src/main/fuookami/ospf/kotlin/multiarray/BlockMultiArray.kt`
- `ospf-kotlin-multiarray/src/main/fuookami/ospf/kotlin/multiarray/AccessOrder.kt`
- `ospf-kotlin-multiarray/src/main/fuookami/ospf/kotlin/multiarray/MultiArray.kt`
- `ospf-kotlin-multiarray/src/main/fuookami/ospf/kotlin/multiarray/Shape.kt`
- 相关 multiarray tests

验收标准：

1. `mvn --% -pl ospf-kotlin-multiarray -am test` 通过。
2. 新增或增强测试覆盖：
   - `BlockMultiArray` key 防外部数组 mutation。
   - row-major / column-major iterator hasNext/next 契约。
   - `fromList` 与 `flatten` 在两种 access order 下结果不变。
   - fixed shape 与 dyn shape column-major inverse index/vector。
3. `git diff --check` 通过。

### P19-2：math 符号合并路径统一与降分配

状态（2026-05-20）：已完成第一批落地并通过目标回归与 P6/P7 门禁。

本批实际修改清单：

- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/LinearQuadraticOps.kt`
- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/CombineTerms.kt`
- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/MutableCombineOps.kt`
- `ospf-kotlin-math/src/test/fuookami/ospf/kotlin/math/symbol/operation/CombineTermsTest.kt`

本批验收命令与结果：

1. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="..."; mvn --% -pl ospf-kotlin-math -Dtest=QuickDslTest,MatrixFormConversionTest,MatrixFormTest,CombineTermsTest,MutableCombineTest -Dsurefire.failIfNoSpecifiedTests=false test'`：通过。
2. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。

目标：把 Flt64 专用合并逻辑变成薄入口，核心合并统一走泛型实现，并减少 quadratic key 分配。

详细步骤：

1. 调整 `CombineTerms.kt`：
   - Flt64 `combineTerms()` 只转发到泛型 `combineLinearTerms` / `combineQuadraticTerms`。
   - 删除重复的 Flt64 本地合并循环。
2. 引入 quadratic term key：
   - 用专用 key class 或 value holder 代替 `Pair<Symbol, Symbol?>`。
   - 确保对称项 `x*y` 与 `y*x` 仍归一。
3. 同步 `MutableCombineOps.kt` 与 `LinearQuadraticOps.kt`。
4. 检查 `PowerVectorKey` 与 canonical 合并是否可复用，不做过度抽象。
5. 对 `evaluateLinearOrdered` / `evaluateQuadraticOrdered` 的 `order.toSet()` + `associate` 做一次性校验 helper，减少重复。

预计修改清单：

- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/CombineTerms.kt`
- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/LinearQuadraticOps.kt`
- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/MutableCombineOps.kt`
- 相关 math symbol operation tests

验收标准：

1. `mvn --% -pl ospf-kotlin-math -Dtest=QuickDslTest,MatrixFormConversionTest,MatrixFormTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
2. 新增或增强测试覆盖：
   - Flt64 与 Rtn64 的 linear/quadratic combine 同类项合并一致。
   - quadratic 对称 key 不回归。
   - 零系数过滤不回归。
3. P6/P7 静态门禁通过。

### P19-3：core flatten / dump / elastic builder 简化

状态（2026-05-20）：已完成第一批落地并通过目标回归与 P6/P7 门禁。

本批实际修改清单：

- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/flatten/FlattenUtility.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/SparseMatrix.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/MemoryCleanupPolicy.kt`（新增）
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/LinearTriadModel.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/QuadraticTetradModel.kt`
- `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/expression/flatten/FlattenUtilityTest.kt`
- `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/intermediate_model/SparseMatrixTransposeTest.kt`（新增）

本批验收命令与结果：

1. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SymbolCombinationGenericFactoryTest,IntermediateSymbolGenericFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test'`：通过。
2. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1536m -XX:NonProfiledCodeHeapSize=512m -XX:ProfiledCodeHeapSize=512m"; mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile'`：通过。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
4. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
5. `git diff --check`：通过（仅行尾符提示，无空白错误）。

目标：减少 core 热路径中间集合，拆出重复 builder，降低大文件维护成本。

详细步骤：

1. 优化 `FlattenUtility.kt`：
   - `mergeLinearFlattenDataFlt64` / `mergeQuadraticFlattenDataFlt64` 不再 `flatMap` 出全量 monomial list。
   - 直接单趟累积 monomials 和 constant。
2. 优化 `SparseMatrix.transpose()`：
   - 避免 `rows.flatMap { it.entries }.maxOfOrNull`。
   - 单趟扫描 maxCol，再填充 transpose。
3. 抽出 linear/quadratic elastic builder 公共结构：
   - 先抽小 helper，避免一次性大重构。
   - 目标是减少 repeated signs/rhs/names/sources/origins/froms/priorities 构造逻辑。
4. 把 `System.gc()` 替换为统一策略：
   - 新增轻量 memory cleanup policy 或复用现有 `memoryUseOver()`。
   - 默认不强制 GC；必要时由配置或 guard 触发。
5. 对 `LinearTriadModel.kt` / `QuadraticTetradModel.kt` 做文件级拆分时要保持 package 和 public API 不变。

预计修改清单：

- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/flatten/FlattenUtility.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/SparseMatrix.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/LinearTriadModel.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/QuadraticTetradModel.kt`
- 可能新增 `*Builder.kt` / `*Elastic.kt` helper 文件
- 相关 core tests

验收标准：

1. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SymbolCombinationGenericFactoryTest,IntermediateSymbolGenericFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
2. core `-DskipTests test-compile` 通过。
3. 新增或增强测试覆盖：
   - flatten merge 不额外丢失 non-variable 符号的错误语义。
   - transpose 空矩阵、单行、多行、最大列推断。
   - elastic model 的变量数、约束数、objective 项、slack 命名不回归。
4. P6/P7 静态门禁通过。

### P19-4：core-plugin solver dump 公共化与边界数组优化

状态（2026-05-20）：已完成第二批落地（Gurobi/Gurobi11/Cplex/SCIP）并通过目标编译与 P6/P7 门禁。

本批实际修改清单：

- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/src/main/fuookami/ospf/kotlin/core/solver/gurobi11/GurobiLinearSolver.kt`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/src/main/fuookami/ospf/kotlin/core/solver/gurobi11/GurobiQuadraticSolver.kt`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/src/main/fuookami/ospf/kotlin/core/solver/gurobi/GurobiLinearSolver.kt`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/src/main/fuookami/ospf/kotlin/core/solver/gurobi/GurobiQuadraticSolver.kt`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/fuookami/ospf/kotlin/core/solver/cplex/CplexLinearSolver.kt`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/fuookami/ospf/kotlin/core/solver/cplex/CplexQuadraticSolver.kt`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/fuookami/ospf/kotlin/core/solver/scip/ScipLinearSolver.kt`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/fuookami/ospf/kotlin/core/solver/scip/ScipQuadraticSolver.kt`

本批验收命令与结果：

1. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11 -am -DskipTests compile'`：通过。
2. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile'`：通过。
3. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am -DskipTests compile'`：通过。
4. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex -am -DskipTests compile'`：通过。
5. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
7. `git diff --check`：通过（仅行尾符提示，无空白错误）。

目标：减少 solver 插件重复代码和数组构建分配，保持 solver boundary 明确。

详细步骤：

1. 先从 Gurobi11 或 SCIP 选一个插件做样板。
2. 将变量 lower/upper/type/name 数组改成单趟填充。
3. 将初始解收集改成单趟 builder，避免 filter + map + map。
4. 将 constraint chunk 计算改成纯整数函数，例如 `chunkSize(rowCount, processors)`。
5. 识别 Gurobi/Gurobi11/Cplex/Scip 可共享的 solver dump helper，但不要把 vendor API 包装得过度抽象。
6. 保留 `toSolverDouble(...)` 作为 solver boundary，不把 `.toDouble()` 扩散回 core。

预计修改清单：

- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/...`
- 可能新增 solver dump utility 文件

验收标准：

1. 至少以下编译通过：
   - `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11 -am -DskipTests compile`
   - `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile`
2. 若改动多个 solver，相关 solver plugin 均需 `-DskipTests compile` 通过。
3. core 的 `.toDouble()` conversion guard 不新增违规。
4. P6/P7 静态门禁通过。

### P19-5：framework 时间区间与日历扫描优化

状态（2026-05-20）：已完成第一批落地（TimeRange/TimeWindow/WorkingCalendar 热点降分配）并通过目标回归与 P6/P7 门禁。

本批实际修改清单：

- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/infrastructure/TimeRange.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/infrastructure/TimeWindow.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/infrastructure/WorkingCalendar.kt`

本批验收命令与结果：

1. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="..."; mvn --% -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am test'`：未通过（被上游既有失败 `ospf-kotlin-math` 的 `Flt64BridgeTest` 阻塞，非本批改动引入）。
2. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1536m -XX:NonProfiledCodeHeapSize=512m -XX:ProfiledCodeHeapSize=512m"; mvn --% -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am -Dtest=TimeRangeDifferenceTest,TimeRangeFindTest,TimeWindowTest,WorkingCalendarTest -Dsurefire.failIfNoSpecifiedTests=false test'`：通过。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
4. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
5. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

目标：优化 gantt scheduling 中时间区间和工作日历的长链路扫描。

详细步骤：

1. 梳理 `TimeRange`、`TimeWindow`、`WorkingCalendar` 的有序性假设。
2. 对连续扫描查找前后边界的路径引入 helper：
   - 若数据始终有序，优先二分。
   - 若数据可能无序，先集中排序或明确要求。
3. 将 `filter + map`、`map + max`、`withIndex().indexOfFirst/Last` 热点改成单趟。
4. 保持现有公开 API 不变。
5. 对 TODO 路径做归档：能安全实现的实现；暂不能实现的加明确错误信息和测试。

预计修改清单：

- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/main/.../TimeRange.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/main/.../TimeWindow.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/main/.../WorkingCalendar.kt`
- 相关 infrastructure tests

验收标准：

1. `mvn --% -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am test` 通过。
2. 重点测试覆盖：
   - TimeRange difference/intersection/merge 边界。
   - TimeWindow interval / instant 转换。
   - WorkingCalendar 空日历、连续区间、break time、connection time。
3. 不新增 `TODO("not implemented yet")`。

### P19-6：中低优先级技术债清理

状态（2026-05-20）：已完成第一批落地（framework-gantt 的 `!!` 与关键路径 TODO 语义化）并通过目标回归、目标编译与 P6/P7 门禁。

本批实际修改清单：

- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/infrastructure/TimeRange.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/infrastructure/TimeWindow.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/test/kotlin/fuookami/ospf/kotlin/framework/gantt_scheduling/infrastructure/TimeRangeDifferenceTest.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure/src/test/kotlin/fuookami/ospf/kotlin/framework/gantt_scheduling/infrastructure/TimeWindowTest.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/produce/model/Consumption.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/produce/model/Produce.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/resource/model/ConnectionResource.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/resource/model/ExecutionResource.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/resource/model/StorageResource.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/model/TaskTime.kt`
- `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskStepConflictConstraint.kt`

本批验收命令与结果：

1. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1536m -XX:NonProfiledCodeHeapSize=512m -XX:ProfiledCodeHeapSize=512m"; mvn --% -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am -Dtest=TimeRangeDifferenceTest,TimeRangeFindTest,TimeWindowTest,WorkingCalendarTest -Dsurefire.failIfNoSpecifiedTests=false test'`：通过（30 tests, 0 failures）。
2. `pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context -am -DskipTests compile'`：通过。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
4. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
5. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

小批次 2（2026-05-20）补充：

- 目的：继续清理 `domain-task-compilation-context` 关键路径中的 `!!`，将可确定不变量改为显式 `requireNotNull(...)`，并统一 shadow price extractor 的空任务分支。
- 修改文件：
  - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskAdvanceEarliestEndTimeConstraint.kt`
  - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskAdvanceTimeConstraint.kt`
  - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskCompilationConstraint.kt`
  - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskDelayLastEndTimeConstraint.kt`
  - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskDelayTimeConstraint.kt`
  - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskOverMaxAdvanceTimeConstraint.kt`
  - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskOverMaxDelayTimeConstraint.kt`
- 验收命令与结果：
  1. `mvn --% -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am -DskipTests compile`：通过。
  2. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
  3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
  4. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

目标：整理风险但不阻塞主链路的问题。

详细步骤：

1. `!!` 清理：
   - 只处理 main 源码中能明确替代的 `!!`。
   - 用 `requireNotNull(value) { "..." }` 或领域 helper 表达不变量。
   - 优先处理 core dump、solver plugin、framework task/gantt。
2. TODO 归档：
   - 列出所有 `TODO("not implemented yet")`。
   - 对关键路径 TODO 改为明确异常类型和说明，或补实现。
   - 非关键路径 TODO 记录到 daily.md 后续风险。
3. 文档边界表：
   - README / README_ch 如需要，补充“泛型核心层 / Flt64 快捷层 / solver 边界层”。
4. 注释乱码与迁移叙事：
   - 修正明显乱码。
   - 删除不再适用的 bridge/compat/migration 叙事。
5. 低优先级 shape / builder 小优化可穿插处理，但不要扩大改动面。

预计修改清单：

- 视扫描结果决定，优先：
  - core main
  - math symbol operation
  - core-plugin solver boundary
  - gantt framework
  - README / README_ch（如涉及用户入口说明）

验收标准：

1. 修改到的模块至少 `test-compile` 或 `compile` 通过。
2. 涉及用户入口说明时 README 与 README_ch 同步。
3. P6/P7 静态门禁通过。
4. `git diff --check` 通过。

## 建议执行顺序

1. P19-1 multiarray：低耦合、高收益，最适合作为第一批。
2. P19-2 math combine：统一符号合并核心，给 core flatten 后续优化打基础。
3. P19-3 core flatten/dump：收益最大，但需要更严格测试。
4. P19-4 core-plugin solver dump：建议在 core dump 稳定后执行。
5. P19-5 framework gantt：结合已有 infrastructure tests 做。
6. P19-6 中低优先级清理：穿插执行，不要和大重构混在同一个 commit。

## 推荐验收命令

按改动范围选择，不要求每批都完整执行全部命令。

```powershell
pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-multiarray -am test'
```

```powershell
pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-math -Dtest=QuickDslTest,MatrixFormConversionTest,MatrixFormTest -Dsurefire.failIfNoSpecifiedTests=false test'
```

```powershell
pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SymbolCombinationGenericFactoryTest,IntermediateSymbolGenericFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test'
```

```powershell
pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-example -am -DskipTests compile'
```

```powershell
pwsh.exe -NoProfile -Command '$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=1024m -XX:NonProfiledCodeHeapSize=384m -XX:ProfiledCodeHeapSize=384m"; mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am -DskipTests compile'
```

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7
```

```powershell
git diff --check
```

完整 release gate 仍可用：

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-migration-compat.ps1
```

说明：完整脚本耗时较长，建议只在阶段性收口时执行。

## 交接注意事项

1. 当前文档是给下一个会话执行 P19 的交接入口。下一会话应先从 P19-1 开始，不要重新打开兼容层目标。
2. 不要把外部 APS/CSP1D/BOP/PSP 直接编译作为默认阻塞项；本仓库继续以 in-repo fixture 和 profile 作为默认门禁。
3. 如果引入新的静态门禁，优先加到 `check-c8-guards.ps1` 的 P18/P19 段落，保持 P6/P7 模式都可覆盖。
4. 每个 P19 子任务完成后，更新本文件对应小节的状态、实际修改清单、验收命令和结果，再提交独立 commit。
