# core API 易用性与原版兼容计划（2026-05-14）

## 新的目标

下一轮目标是对比原始 Kotlin 版本：

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

## 计划

### P0：建立兼容矩阵

对原始 core、原始示例与四个业务项目建立“用法矩阵”，按以下类别标注：

1. 必须保持同名同语义的公开 API。
2. 可保留同名但泛型化签名变化的 API。
3. 受 `math.symbol` 迁移影响，需要一层兼容包装的 API。
4. 可接受迁移说明替代的包名或内部类型差异。
5. 当前版本缺失、退化或样板过重的 API。
6. framework/starter 层缺失、包路径变化或泛型桥接不足的 API。
7. 业务项目中出现频率高、影响面大的真实迁移阻塞点。

### P1：恢复四类数值易用入口

恢复原始版本中最常见的默认建模入口，重点减少示例和业务项目里的 converter 样板：

1. 公开 `LinearMetaModel(name, objectCategory, configuration)` 的 `Flt64`、`FltX`、`Rtn64`、`RtnX` 易用构造或工厂。
2. 公开 `QuadraticMetaModel(name, objectCategory, configuration)` 的 `Flt64`、`FltX`、`Rtn64`、`RtnX` 易用构造或工厂。
3. 检查 `LinearMechanismModel`、`QuadraticMechanismModel` 是否也需要四类数值 invoke。
4. 将示例和业务 fixture 中重复定义的 converter 收口为库侧默认能力。
5. `Flt64` 入口保持原版最短路径；`FltX/Rtn64/RtnX` 入口要有同等命名规则，避免只靠泛型尖括号才能使用。

### P2：补齐模型层快捷 DSL

对齐原版 `Model`、`LinearModel`、`QuadraticModel`、`MetaModel` 的常用入口：

1. `addConstraint(variable/monomial/symbol/polynomial)` 默认转 `eq true`。
2. 无 group 与带 group 的 `partition(...)` 重载。
3. `constraintsOfGroup(group)` 查询入口。
4. `Quantity<IntermediateSymbol>` 及其 iterable、map、multi-map 批量 `add`。
5. `addObject/minimize/maximize` 对 monomial、symbol、polynomial 的旧式快捷重载。

### P3：补齐函数符号兼容入口

以原始 `linear_function`、`quadratic_function` 测试为清单，逐一确认函数符号名称和快捷构造：

1. 已有但构造签名变化的函数，补同名 `Flt64`、`FltX`、`Rtn64`、`RtnX` 快捷构造或 adapter。
2. 已合并实现但旧名称仍常用的函数，补 typealias 或薄包装。
3. 当前缺口重点核查：`IfElseFunction`、`InListFunction`、`AtLeastPolynomialFunction`、`SatisfiedAmountPolynomialFunction`、`URealSlackFunction`、`UIntegerSlackFunction`、`MonotoneUnivariateLinearPiecewiseFunction`、`IsolineBivariateLinearPiecewiseFunction`。
4. `Not/Or/Xor/Min/Max/MinMax/MaxMin` 等逻辑与极值函数必须保持原测试写法可迁移。

### P4：整理快捷算术与聚合 DSL

统一 `math.symbol` 迁移后的快捷表达体验：

1. 明确 `sum`、`qsum`、`LinearPolynomial()`、`QuadraticPolynomial()` 的推荐导入路径。
2. 补齐 `Symbol`、变量、monomial、polynomial 与 `Flt64/FltX/Rtn64/RtnX/Int/Double/UInt64/Boolean` 的常用运算重载。
3. 避免用户为了一个模型同时导入过多 adapter 包。
4. 对冲突重载增加编译级测试，防止未来 Kotlin overload resolution 回归。

### P5：恢复示例回归覆盖

让当前示例重新承担用户文档和迁移验收职责：

1. `core_demo Demo1-17` 默认至少进入 build-only 或结构化测试。
2. 函数示例测试从单点 smoke 逐步恢复原始测试的真实模型路径。
3. `QuadraticTest` 拆成 build-only 结构测试与 solver-gated 结果测试。
4. 外部 solver 不可用时，测试应跳过集成层而不是失败或空断言。

### P6：增加 API 边界门禁

建立长期防回归机制：

1. 新增 source-compat 测试，编译一批原版短代码片段。
2. 门禁检查示例中不应再出现重复 `flt64Converter` 样板。
3. 门禁检查 `assertTrue(true)`、`assertThat(true).isTrue()` 等空断言不回流。
4. 保留既有 P6/P7 泛型和 solver 边界门禁。

### P7：业务项目迁移兼容

将 `aps`、`csp1d`、`bop`、`psp` 作为真实业务迁移验收对象，先做静态兼容与编译闭环：

1. 建立四个业务项目的 API 使用矩阵，按模块统计 core、framework、starter、solver 插件、utils 的旧用法。
2. 优先覆盖高频触点：`LinearMetaModel()`、`LinearMetaModel("name")`、`QuadraticMetaModel()`、`AbstractLinearMetaModel`、`AbstractQuadraticMetaModel`、`Pipeline<T>`、`PipelineList<T>`、`LinearIntermediateSymbolsN`、`QuadraticIntermediateSymbolsN`、`LinearExpressionSymbolsN`、`QuadraticExpressionSymbolsN`。
3. 检查变量族与索引视图：`BinVariableN`、`UIntVariableN`、`URealVariableN`、`PctVariableN`、`VariableItem`、`token.variable.vectorView`、`belongsTo`。
4. 检查 solver 入口：`LinearSolverBuilder`、`QuadraticSolverBuilder`、`ColumnGeneratorSolverBuilder`、`Solver`、`solveLP`、`solveMILP`、`SolverConfig`、`SolverOutput`。
5. 检查 starter 依赖：`ospf-kotlin-starter-gantt-scheduling` 与 `ospf-kotlin-starter-csp1d` 应继续一次性导出业务项目常用依赖。
6. 对四个业务项目建立 source-compat fixture 或外部编译 profile，默认只要求编译和 build-only 注册链路，不默认运行真实优化任务。

### P8：framework 与 starter 兼容补齐

业务项目暴露出兼容范围已超出 core，需要把 framework/starter 纳入迁移面：

1. `ospf-kotlin-framework/src/main/.../framework/model/Pipeline.kt` 对齐原版 `Pipeline`、`PipelineList`、`invoke`、`refresh` 等常用入口。
2. `ospf-kotlin-framework/src/main/.../framework/solver/*` 对齐列生成、组合求解、Benders、并行求解等业务项目调用方式。
3. `ospf-kotlin-framework-gantt-scheduling` 对齐 APS/PSP 使用的 bunch/task/capacity/produce/resource 上下文与 service limit 入口。
4. `ospf-kotlin-framework-csp1d` 对齐 CSP1D 使用的 cutting plan、produce、yield、length assignment、wasting minimization 上下文入口。
5. starter POM 确认导出当前业务项目原来依赖的一组模块，避免业务项目从一个 starter 迁移成大量手工依赖。
6. framework 层若引入泛型桥接，同样提供 `Flt64/FltX/Rtn64/RtnX` 类型别名或薄包装，避免业务上下文全部显式声明 `V`。

### P9：math 数值转换桥接

为四类数值易用入口提供统一的 math 层转换能力：

1. 在 math 中新增 companion-provider 接口，表达 `Flt64 <-> V` 转换能力。命名可在实现时确定，例如 `Flt64ConvertibleConstants<V>` 或 `Flt64Bridge<V>`。
2. 接口至少包含 `fromFlt64(value: Flt64): V` 与 `toFlt64(value: V): Flt64`，`toFlt64` 可复用数值实例已有的 `toFlt64()`。
3. `Flt64.Companion`、`FltX.Companion`、`Rtn64.Companion`、`RtnX.Companion` 实现该接口。
4. 复用现有 companion reflection resolver 模式，提供 `resolveFlt64Bridge<V>()` 或等价解析函数。
5. core/framework 的默认 converter 优先依赖该 companion-provider，而不是在每个模型、函数或 solver 入口中硬编码四套转换。
6. 增加四类型转换边界测试：`Flt64 -> V -> Flt64`、常量转换、负数/小数/零/一、`Rtn64/RtnX` 的精度与舍入语义。

## 详细步骤

### 步骤 1：差异提取

1. 扫描原始 core 公开类、接口、顶层函数和 typealias。
2. 扫描原始示例中实际调用的 API，优先统计 `core_demo`、`linear_function`、`quadratic_function`、`QuadraticTest`。
3. 扫描 `aps`、`csp1d`、`bop`、`psp` 的 import、类型引用、构造调用、DSL 调用、solver 调用和 starter 依赖。
4. 扫描当前仓库对应 API，生成缺口清单。
5. 将缺口按 P1-P9 分类，避免把包名迁移误判为功能缺失。

已发现的业务项目高频触点：

1. `aps`：`LinearMetaModel`、`AbstractLinearMetaModel`、Gantt starter、`ColumnGeneratorSolverBuilder`、`LinearSolverBuilder`、`LinearIntermediateSymbolsN`、`MaxFunction`、`SlackFunction`、`BinaryzationFunction`。
2. `csp1d`：`LinearMetaModel`、`AbstractLinearMetaModel`、CSP1D starter、列生成、`UIntVariableN`、`BinVariableN`、`sum(...)`、`belongsTo`、`vectorView`、`MaskingFunction`、`BinaryzationFunction`。
3. `bop`：`LinearMetaModel`、`QuadraticMetaModel`、`AbstractQuadraticMetaModel`、二次函数符号、`QuadraticIntermediateSymbolsN`、`QuadraticSolverBuilder`、`SlackRangeFunction`、`AbsFunction`。
4. `psp`：`LinearMetaModel`、`LinearMetaModel("id")`、Gantt starter、`Solver.solveMILP`、`LinearIntermediateSymbolsN`、`CeilingFunction`、`SlackFunction(threshold=...)`、能量/容量编译上下文。

### 步骤 2：默认建模入口补齐

1. 调整 `LinearMetaModel` 与 `QuadraticMetaModel` 的 companion 或构造入口，使 `Flt64` 默认构造在跨模块示例中可见。
2. 为必须显式选类型的入口补 `Flt64/FltX/Rtn64/RtnX` 四套工厂或命名入口。
3. 增加 API 测试：不传 converter 或只选择数值类型时可构建、添加变量、添加约束、添加目标、dump mechanism。
4. 替换 `core_demo` 与业务 fixture 中重复 converter 样板，确认示例更接近原版。

### 步骤 3：模型 DSL 补齐

1. 在 `Model.kt` 与 `MetaModel.kt` 中补齐旧式快捷重载。
2. 对 `group`、`lazy`、`name`、`displayName`、`args`、`withRangeSet` 参数保持原版调用顺序和默认值。
3. 补 `constraintsOfGroup`，优先复用当前 `indicesOfConstraintGroup` 与 constraint group metadata。
4. 新增编译级测试覆盖无 group 与带 group 两套写法。

### 步骤 4：函数符号兼容补齐

1. 将原始函数测试逐个映射到当前函数实现。
2. 对缺旧名但已有语义的函数添加薄包装。
3. 对语义尚未迁移的函数补最小可用实现，并加结构化注册测试。
4. 保证 `registerAuxiliaryTokens` 与 `registerConstraints` 在 build-only 测试中可验证。

### 步骤 5：示例测试恢复

1. 先恢复 `Demo1`、`Demo2`、`Demo3` 等短链路 build-only 测试。
2. 再恢复长链路或耗时示例的结构化测试。
3. 对需要 SCIP/Gurobi 的测试加 profile 或条件跳过。
4. 将 `CoreDemoTest` 从只测 `GenericNumberDemo` 扩展为覆盖原版 `Demo1-17` 的轻量验收入口。

### 步骤 6：验收与文档收口

1. 更新 README/README_ch 中的迁移说明与默认导入建议。
2. 更新门禁脚本，纳入新兼容规则。
3. 跑 core API 兼容测试、example build-only 测试、泛型窄测和 P6/P7 门禁。
4. 在本文件记录最终验收命令和结果。

### 步骤 7：业务项目 source-compat 验收

1. 为 `aps`、`csp1d`、`bop`、`psp` 各选一个最小代表模块做编译验收，优先选择只依赖建模与 framework 的 domain context。
2. 对每个项目补一个 fixture，覆盖其真实高频 API：变量创建、符号容器创建、pipeline 注册、约束/目标添加、solver builder 构造。
3. 如外部项目因 POIT 父 POM、私有依赖、SCIP/Gurobi 本地库缺失无法直接编译，则在当前仓库建立 source-compat 测试复制最小调用片段。
4. 对 starter 做依赖闭包检查，确认业务项目仍可通过 starter 引入所需 framework/core 模块。
5. 最终保留四个业务项目的“编译级兼容证据”和“不可自动化外部依赖清单”。

### 步骤 8：math 转换接口落地

1. 在 `Numbers.kt` 或相邻 concept 文件中定义 companion-provider 转换接口，并补中英双语注释。
2. 在 `Floating.kt`、`Rational.kt` 中让 `Flt64/FltX/Rtn64/RtnX` companion 实现该接口。
3. 增加解析函数，沿用现有 `resolveCompanionProvider` 风格。
4. 将 core/framework 里需要默认 converter 的入口改为依赖该接口。
5. 增加 math 单测与 core source-compat 测试，确认四类型入口不再手写 converter。

## 修改清单

预计涉及以下文件或模块：

1. `ospf-kotlin-core/src/main/.../core/model/basic/Model.kt`
   - 补 `addObject/minimize/maximize/addConstraint/partition` 兼容重载。

2. `ospf-kotlin-core/src/main/.../core/model/mechanism/MetaModel.kt`
   - 公开默认 `Flt64` 构造入口。
   - 补 `Quantity<IntermediateSymbol>` 批量 `add`。
   - 补 `constraintsOfGroup`。
   - 对齐原版 group 参数重载。

3. `ospf-kotlin-core/src/main/.../core/model/mechanism/MathInequalityDsl.kt`
   - 补齐旧式比较别名和常用 RHS 类型。
   - 增加 overload resolution 边界测试。

4. `ospf-kotlin-math/src/main/.../math/symbol/adapter/flt64/QuickDsl.kt`
   - 明确或补齐 `sum/qsum`、快捷 polynomial 构造与四类型默认聚合入口。

5. `ospf-kotlin-core/src/main/.../core/intermediate_symbol/function/*.kt`
   - 补函数符号旧名称、快捷构造和薄包装。
   - 优先覆盖原始 function tests 中出现的函数。

6. `ospf-kotlin-example/src/main/.../example/core_demo/*.kt`
   - 清理重复 converter 样板。
   - 保持示例主体表达接近原始版本。

7. `ospf-kotlin-example/src/test/...`
   - 恢复 `Demo1-17`、函数示例、二次模型示例的结构化测试。
   - 拆分 build-only 与 solver-gated 验收。

8. `ospf-kotlin-core/scripts/check-c8-guards.ps1`
   - 增加 API 易用性门禁，例如 converter 样板回流、空 smoke 回流。

9. `ospf-kotlin-core/README.md` 与 `ospf-kotlin-core/README_ch.md`
   - 补充兼容策略、导入路径和泛型默认写法说明。

10. `ospf-kotlin-framework/src/main/.../framework/model/Pipeline.kt`
    - 对齐 `Pipeline`、`PipelineList`、`invoke`、`refresh` 等业务项目常用入口。

11. `ospf-kotlin-framework/src/main/.../framework/solver/*.kt`
    - 对齐 `ColumnGeneratorSolverBuilder`、组合求解、Benders、并行求解入口。

12. `ospf-kotlin-framework-gantt-scheduling/**`
    - 覆盖 `aps`、`psp` 对 Gantt starter 和上下文的真实调用。

13. `ospf-kotlin-framework-csp1d/**`
    - 覆盖 `csp1d` 对 CSP1D starter 和上下文的真实调用。

14. `ospf-kotlin-starters/**/pom.xml`
    - 检查 starter 导出依赖，避免业务项目迁移后手工补大量模块。

15. `ospf-kotlin-core/src/test/...` 或 `ospf-kotlin-example/src/test/...`
    - 新增四个业务项目 source-compat fixture，覆盖最小真实业务 API 片段。

16. `ospf-kotlin-math/src/main/.../math/algebra/concept/Numbers.kt`
    - 新增 `Flt64 <-> V` companion-provider 转换接口与 resolver。

17. `ospf-kotlin-math/src/main/.../math/algebra/number/Floating.kt`
    - 让 `Flt64`、`FltX` companion 实现转换接口。

18. `ospf-kotlin-math/src/main/.../math/algebra/number/Rational.kt`
    - 让 `Rtn64`、`RtnX` companion 实现转换接口。

19. `ospf-kotlin-math/src/test/...`
    - 增加 `Flt64/FltX/Rtn64/RtnX` 互转与 companion resolver 测试。

## 验收标准

### API 兼容验收

1. 原始示例中的核心建模写法可在当前仓库中以最小改动编译。
2. 四个业务项目中的代表性建模写法可在当前仓库中以最小改动编译，至少覆盖 `aps`、`csp1d`、`bop`、`psp` 各一个 fixture。
3. `LinearMetaModel("demo")`、`LinearMetaModel()` 与 `QuadraticMetaModel()` 可跨模块直接使用。
4. 泛型化后必须指定数值类型的易用入口，均提供 `Flt64/FltX/Rtn64/RtnX` 四个版本或等价工厂。
5. 示例和业务 fixture 中不再需要手写重复 `flt64Converter` 或四类型 converter。
6. `addConstraint`、`partition`、`constraintsOfGroup`、`minimize/maximize` 常用旧式写法均有测试覆盖。
7. 原始函数符号测试与业务项目中出现的公开名称，要么可同名使用，要么有明确兼容替代和测试。
8. `Pipeline`、`PipelineList`、`AbstractLinearMetaModel`、`AbstractQuadraticMetaModel`、`LinearIntermediateSymbolsN`、`QuadraticIntermediateSymbolsN`、solver builder 和 starter 依赖均有编译级兼容证据。

### 功能验收

1. build-only 示例能证明变量、符号、约束、目标和 mechanism dump 结构正确。
2. 函数符号注册测试能断言辅助变量、约束数量、约束名称、关键 token 参与和目标项。
3. 泛型链路仍覆盖 `Flt64`、`Rtn64`、`FltX`、`RtnX`。
4. solver-gated 测试在 SCIP/Gurobi 可用时复跑原始示例关键结果。
5. 四个业务项目 fixture 能证明变量族、符号容器、pipeline 注册、约束/目标添加、starter 依赖闭包和 solver builder 构造未退化。
6. math 层 `Flt64 <-> V` companion-provider 覆盖 `Flt64/FltX/Rtn64/RtnX`，core/framework 默认 converter 均可复用它。

### 回归命令

默认验收命令暂定：

```powershell
mvn --% -pl ospf-kotlin-core -am -Dtest=ApiCompatibilityTest,GenericLinearMetaModelBuildTest,GenericQuadraticMetaModelBuildTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn --% -pl ospf-kotlin-math -am -Dtest=*Flt64BridgeTest,*NumberConversionTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn --% -pl ospf-kotlin-example -am -Pcore-demo-only -Dtest=CoreDemoTest,GenericNumberDemoTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn --% -pl ospf-kotlin-example -am -Pbuild-only-function-tests -Dsurefire.failIfNoSpecifiedTests=false test
mvn --% -pl ospf-kotlin-framework -am -Dtest=*CompatTest,*VBridgeTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn --% -pl ospf-kotlin-framework-gantt-scheduling -am -DskipTests compile
mvn --% -pl ospf-kotlin-framework-csp1d -am -DskipTests compile
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7
```

solver 可用时追加：

```powershell
mvn --% -pl ospf-kotlin-example -am -Psolver-integration-tests -Dsurefire.failIfNoSpecifiedTests=false test
```

外部业务项目具备依赖条件时追加：

```powershell
mvn --% -f E:/workspace/poit/aps/pom.xml -DskipTests compile
mvn --% -f E:/workspace/poit/csp1d/pom.xml -DskipTests compile
mvn --% -f E:/workspace/poit/bop/pom.xml -DskipTests compile
mvn --% -f E:/workspace/poit/psp/pom.xml -DskipTests compile
```

若 POIT 父 POM、私有依赖或本地 solver 库不可用，则以当前仓库 source-compat fixture 替代上述外部编译，并在本文件记录阻塞原因。

### 文档验收

1. `daily.md` 从上一轮收口报告切换为本轮执行计划。
2. README 双语互链继续保留。
3. 新增公开 API 若包含注释，注释需中英双语。
4. 本轮完成后，`daily.md` 应再次更新为实际执行结果、测试证据、残余风险和后续建议。

---

## 执行进度

### P9：math companion-provider 转换接口 — 已完成

**新增文件：**

- `ospf-kotlin-math/src/main/.../concept/Flt64Bridge.kt` — `Flt64Bridge<V>` 接口 + `resolveFlt64Bridge<V>()` 解析函数
- `ospf-kotlin-math/src/test/.../concept/Flt64BridgeTest.kt` — 四类型转换测试

**修改文件：**

- `ospf-kotlin-math/src/main/.../number/Floating.kt` — Flt64/FltX companion 实现 `Flt64Bridge`
- `ospf-kotlin-math/src/main/.../number/Rational.kt` — Rtn64/RtnX companion 实现 `Flt64Bridge`
- `ospf-kotlin-core/src/main/.../solver/value/IntoValue.kt` — 新增 `fromBridge()` 适配器 + `toIntoValue()` 扩展

**设计决策：**

- `Flt64Bridge` 继承 `HasZero<V>` + `HasOne<V>`，不继承 `HasInfinity<V>`（避免 nullable 签名冲突），infinity/negativeInfinity 由 core 层 `IntoValue<V>` 默认实现提供
- `fromValue` 默认实现为 `value.toFlt64()`，复用 `RealNumber` 已有方法
- `Flt64Bridge` 定义在 math 层（不依赖 core），`IntoValue` 适配器在 core 层，保持模块依赖方向正确

**测试证据：**

- `Flt64BridgeTest` 7 个测试全部通过（含用户补充修复后的 Rtn64/RtnX 转换）

**残余风险：**

- Rtn64/RtnX 的 `intoValue` 对非整数 Flt64 值有精度截断（符合有理数语义），后续如需精确转换可扩展接口
