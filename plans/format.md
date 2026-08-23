# KDoc 中英文规范与冗余限定符清理计划

日期：2026-07-26（更新：2026-08-01）

## 1. 背景与当前状态

项目规范（`.rules/chore.md` §1.1、§1.6.4、§1.6.5）要求：

- 注释使用中英双语，中文在前、英文在后，以 ` / ` 分隔。
- `@param`/`@property`/`@return` 标签的描述应遵循同一行双语格式。
- import 排列遵循 §1.6.2 规定的层级与排序规则。

全仓库扫描发现三类格式偏差：

1. **中英文顺序不一致**：约 300+ 处 `@param`/`@property`/`@return` 描述为英文在前、中文在后（如 `the callback function / 回调函数`），与项目规范"中文在前"相反。
2. **中英文另起一行**：约 20+ 处 KDoc 描述主体或标签描述中，中文和英文分处不同行而非同一行 ` / ` 分隔。
3. **冗余限定符**：约 80+ 处代码中使用了全限定名或部分限定名（如 `fuookami.ospf.kotlin.core.variable.Integer`），可通过 import 简化为短名。

## 2. 改进目标

1. 将所有 `@param`/`@property`/`@return` 双语描述统一为"中文 / English"格式，消除英文在前的偏差。
2. 将所有 KDoc 中另起一行的双语描述合并为同一行 ` / ` 分隔格式。
3. 移除代码中可通过 import 消除的冗余限定符，同时严格遵守 import 排列与通配符合并规则。
4. 不引入任何行为变更，纯格式重构。

## 3. 边界

- 不修改代码逻辑、API 签名或行为。
- 不修改测试文件（测试文件不在扫描范围内，且测试中的冗余限定符影响较小）。
- 不修改字符串常量中的全限定名（如 KSP 处理器注册用的类名字符串）。
- 不修改存在真实歧义的限定符（如 `Integer` 与 `kotlin.Int` 冲突时保留限定符）。
- 不修改 `package` 声明和 `import` 语句以外的文件头部结构。

## 4. 通用注意事项

### 4.1 import 修改规则

修改冗余限定符时，必须同步调整 import，并严格遵守以下规则：

1. **排列顺序**：按 §1.6.2 规定——Kotlin/kotlinx 标准库 → 第三方库 → `fuookami.ospf.kotlin.*`（按模块依赖深度升序）。
2. **同包合并**：新增 import 若与已有 import 属于同一包，必须合并为通配符 `.*`。例如已有 `import fuookami.ospf.kotlin.core.variable.Binary`，新增 `Integer` 和 `Continuous` 时应合并为 `import fuookami.ospf.kotlin.core.variable.*`。
3. **无空行分隔**：所有 import 连续排列，中间不按来源分组插空行。
4. **末尾无分号**。
5. **同层排序**：同一模块层级内按首层差异路径段长度升序 → 字典序升序排列。
6. **避免引入歧义**：添加 import 前必须确认短名不会与已有 import 产生冲突。若存在冲突（如 `Integer` 与 `kotlin.Int`），保留限定符不动。

### 4.2 KDoc 修改规则

1. **顺序统一**：所有双语描述改为 `中文 / English`，分隔符统一为 ` / `（斜杠前后各一个空格）。
2. **同行合并**：将另起一行的双语描述合并到同一行。描述主体中的双语段落，合并为 `中文 / English` 单行格式。
3. **保留语义**：不增删描述内容，仅调整格式和顺序。
4. **单行 KDoc**：已符合规范的单行 KDoc（如 `/** 是否已加载用户组引用 */`）不做修改。

## 5. 分阶段计划

### Phase F1：ospf-kotlin-utils（P0）✅ 已完成

utils 是最底层模块，被所有模块依赖，优先处理。

#### F1.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `functional/Variant.kt` | ~100 | `@param callBack the callback function / 回调函数` → `@param callBack 回调函数 / the callback function` |
| `Time.kt` | ~13 | `@param lhs the left-hand side instant / 左侧时间戳` 等 |
| `serialization/Json.kt` | ~20 | `@param T object type / 对象类型` 等 |
| `context/Context.kt` | ~6 | `@param other the object to compare against / 待比较的对象` 等 |
| `functional/Quadruple.kt` | 4 | `@property first The first element / 第一个元素` 等 |
| `parallel/Common.kt` | ~6 | `@property index Task index / 任务索引` 等 |
| `parallel/Fold.kt` | ~12 | `@return normalized segment size / 规范化后的分段大小` 等 |
| `UUIDv7.kt` | 2 | `@return a 16-byte array... / 16 字节的 UUIDv7 字节数组` |

#### F1.2 中英文另起一行修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `serialization/DurationSerializer.kt` | ~4 | 描述主体中中文和英文分处不同行，合并为同行 |
| `parallel/ThreadGuard.kt` | ~2 | `* 实现 AutoCloseable 接口的关闭方法...` → `* Implementation of AutoCloseable close method...` |
| `parallel/ChannelGuard.kt` | ~2 | 同上 |
| `parallel/Common.kt` | ~2 | `* 这是核心并发控制实现...` → `* This is the core concurrency control implementation...` |

#### F1.3 冗余限定符清理

utils 模块内部冗余限定符较少，无重大项。

---

### Phase F2：ospf-kotlin-multiarray（P0）✅ 已完成

#### F2.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `Vector.kt` | ~3 | `@param v Value to check / 要检查的值` 等 |
| `MultiArrayView.kt` | ~2 | `@param v View index vector to convert / 要转换的视图索引向量` |
| `AccessOrder.kt` | ~2 | `@param v Current multi-dimensional index to advance / 要推进的当前多维索引` |
| `DataFrame.kt` | ~2 | `@return Success if column exists... / 列存在且设置成功时...` |

---

### Phase F3：ospf-kotlin-quantities（P1）✅ 已完成

#### F3.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `math/geometry/PlaneFrame3.kt` | 少量 | `@param`/`@return` 英文在前 |

---

### Phase F4：ospf-kotlin-math（P1）✅ 已完成

math 模块有 **55 个文件** 存在英文在前问题，远超初扫预期。按子模块分组：

#### F4.1 中英文顺序修正

| 子模块 | 文件数 | 典型文件 |
|--------|-------|---------|
| `math/symbol/operation/` | 10 | `MatrixForm.kt`、`LinearQuadraticOps.kt`、`LatexOps.kt`、`Latex.kt`、`Inequality.kt`、`Flt64QuickDsl.kt`、`CompileOps.kt`、`QuickOps.kt`、`Parse.kt`、`ToPolynomial.kt` |
| `math/symbol/expression/` | 10 | `parser/ScalarParser.kt`、`parser/Parser.kt`、`parser/Lexer.kt`、`operation/MathFunctions.kt`、`operation/EvaluateScalar.kt`、`operation/EvaluateBoolean.kt`、`dsl/ExpressionDsl.kt`、`BooleanExpression.kt`、`ScalarExpression.kt`、`PropertyPath.kt` |
| `math/symbol/serde/` | 3 | `SymbolIdentitySerde.kt`、`InequalitySerde.kt`、`expression/serde/ExpressionSerde.kt` |
| `math/symbol/parse/` | 3 | `PolynomialParser.kt`、`PolynomialLexer.kt`、`NumberParser.kt` |
| `math/geometry/` | 6 | `Triangulation.kt`、`Triangle.kt`、`Quadrilateral.kt`、`Projection2.kt`、`Edge.kt`、`Circle.kt` |
| `math/chaotic/` | 16 | `CircleMap.kt`、`ChebyshevMap.kt`、`ArnoldTongue.kt`、`SingerMap.kt`、`SakaryaAttractor.kt`、`RucklidgeAttractor.kt`、`RabinovichFabrikantEquation.kt`、`QiChenAttractor.kt`、`QiAttractor.kt`、`LotkaVolterraSystem.kt`、`LorenzStenfloAttractor.kt`、`LorenzMod2Attractor.kt`、`LorenzMod1Attractor.kt`、`LiuChenAttractor.kt`、`IntervalExchangeTransformation.kt`、`HindmarshRoseModel.kt` |
| `math/algebra/number/` | 1 | `Rational.kt` |
| `math/algebra/value_range/` | 1 | `ValueRange.kt` |
| `math/ordinary/` | 1 | `FltXPowerStrategy.kt` |
| `multiarray/einsum/` | 2 | `IndexLabel.kt`、`EinsumParser.kt` |
| `utils/functional/` | 1 | `CollectionExtensions.kt` |
| `utils/parallel/` | 1 | `Fold.kt`（math 模块内的副本） |

---

### Phase F5：ospf-kotlin-core（P1）✅ 已完成

#### F5.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `model/mechanism/SubObject.kt` | 少量 | `@param`/`@property` 英文在前 |
| `model/mechanism/MechanismModel.kt` | 少量 | 同上 |
| `model/mechanism/MathInequalityFlatten.kt` | 少量 | 同上 |
| `model/mechanism/LinearConstraintInput.kt` | 少量 | 同上 |
| `model/mechanism/Constraint.kt` | 少量 | 同上 |
| `model/callback/CallBackModelInterface.kt` | 少量 | 同上 |
| `model/callback/CallBackModel.kt` | 少量 | 同上 |
| `model/basic/Model.kt` | 少量 | 同上 |
| `model/basic/ConstraintSign.kt` | 少量 | 同上 |
| `symbol/function/Imply.kt` | 少量 | 同上 |
| `solver/heuristic/ParticleSwarmHeuristicSolver.kt` | 少量 | 同上 |

---

### Phase F6：ospf-kotlin-core-plugin（P0）✅ 已完成

#### F6.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `scip/ScipSolverCallBack.kt` | ~6 | `@return current callback manager instance / 当前回调管理器实例` |
| `scip/ScipSolver.kt` | 1 | `@return the load result as Try / 以Try包装的加载结果` |
| `scip/ScipQuadraticSolver.kt` | ~4 | `@return operation result / 操作结果` |
| `scip/ScipLinearSolver.kt` | ~4 | 同上 |
| `heuristic/saa/SAA.kt` | 2 | `@param V`（无描述，补充泛型说明） |
| `heuristic/sca/SCA.kt` | 1 | 同上 |
| `heuristic/pso/PSO.kt` | 1 | 同上 |
| `heuristic/mvo/MVO.kt` | 1 | 同上 |
| `heuristic/ga/GA.kt` | 1 | 同上 |
| `heuristic/gwo/GWO.kt` | 1 | 同上 |
| `heuristic/wca/WCA.kt` | 1 | 同上 |
| `heuristic/warso/WarSO.kt` | 1 | 同上 |
| `heuristic/soa/SOA.kt` | 1 | 同上 |
| `heuristic/ns/NS.kt` | 1 | 同上 |
| `heuristic/hs/HS.kt` | 1 | 同上 |
| `heuristic/hca/HCA.kt` | 1 | 同上 |
| `heuristic/gco/GCO.kt` | 1 | 同上 |
| `heuristic/evo/EVO.kt` | 1 | 同上 |
| `hexaly/HexalySolver.kt` | 少量 | `@return` 英文在前 |
| `hexaly/HexalySolverCallBack.kt` | 少量 | 同上 |

#### F6.2 中英文另起一行修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `scip/ScipSolver.kt:36-37` | 1 | `* 从 JAR 包中加载 SCIP 原生库` → `* Load SCIP native library from JAR package` |

#### F6.3 冗余限定符清理

**A. 求解器变量映射 when 分支**

以下 7 个文件存在相同模式——`Binary`/`Ternary`/`BalancedTernary`/`Percentage`/`UInteger`/`UContinuous` 使用短名，而 `Integer`/`Continuous` 使用全限定名：

| 文件 | 冗余限定符 | 处理方式 |
|------|-----------|---------|
| `scip/ScipVariable.kt` | `fuookami.ospf.kotlin.core.variable.Integer`、`fuookami.ospf.kotlin.core.variable.Continuous` | 确认无歧义后添加 import 并简化；若 `Integer` 与 `kotlin.Int` 冲突则保留限定符 |
| `mindopt/MindOPTVariable.kt` | 同上 | 同上 |
| `hexaly/HexalyVariable.kt` | 同上 | 同上 |
| `gurobi/GurobiVariable.kt` | 同上 | 同上 |
| `gurobi11/GurobiVariable.kt` | 同上 | 同上 |
| `cplex/CplexVariable.kt` | 同上 | 同上 |
| `copt/CoptVariable.kt` | 同上 | 同上 |

**注意**：若已有 `import fuookami.ospf.kotlin.core.variable.Binary` 等显式 import，新增 `Integer`/`Continuous` 时应合并为 `import fuookami.ospf.kotlin.core.variable.*`。

**B. 求解器中的 SolvingStatus 限定符**

| 文件 | 冗余限定符 | 处理方式 |
|------|-----------|---------|
| `scip/ScipQuadraticSolver.kt:474` | `fuookami.ospf.kotlin.core.solver.output.SolvingStatus(...)` | 添加 import `fuookami.ospf.kotlin.core.solver.output.*`，简化为 `SolvingStatus(...)` |
| `scip/ScipLinearSolver.kt:411` | 同上 | 同上 |

**C. heuristic 插件泛型约束**

| 文件 | 冗余限定符 | 处理方式 |
|------|-----------|---------|
| `heuristic/sca/SCA.kt:32` | `V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V>` | 添加 import `fuookami.ospf.kotlin.math.algebra.concept.*`，简化为 `V : RealNumber<V>, V : NumberField<V>` |
| `heuristic/saa/SAA.kt:37` | 同上 | 同上 |
| `heuristic/pso/PSO.kt:29` | 同上 | 同上 |
| `heuristic/mvo/MVO.kt:34` | 同上 | 同上 |
| `heuristic/ga/GA.kt:31` | 同上 | 同上 |
| `heuristic/gwo/GWO.kt` | 同上 | 同上 |
| `heuristic/wca/WCA.kt` | 同上 | 同上 |
| `heuristic/warso/WarSO.kt` | 同上 | 同上 |
| `heuristic/soa/SOA.kt` | 同上 | 同上 |
| `heuristic/ns/NS.kt` | 同上 | 同上 |
| `heuristic/hs/HS.kt` | 同上 | 同上 |
| `heuristic/hca/HCA.kt` | 同上 | 同上 |
| `heuristic/gco/GCO.kt` | 同上 | 同上 |
| `heuristic/evo/EVO.kt` | 同上 | 同上 |

---

### Phase F7：ospf-kotlin-framework-network-scheduling（P0）✅ 已完成

network-scheduling 模块有大量冗余限定符（30+ 个文件），需单独列出。

#### F7.1 冗余限定符清理

| 子模块 | 文件数 | 典型冗余限定符 |
|--------|-------|--------------|
| `network-scheduling-application/service/` | 2 | `BranchAndPriceAlgorithm.kt`、`BranchNodeSolver.kt` 中使用全限定名引用 domain 模块类型 |
| `network-scheduling-application/model/` | 4 | `BranchNode.kt`、`BranchAndPriceTrace.kt`、`BranchAndPriceStatus.kt`、`BranchDecision.kt` |
| `network-scheduling-domain-vrp-context/` | 2 | `pricing/PricingDuals.kt`、`model/Route.kt` |
| `network-scheduling-domain-route-compilation-context/` | 14 | `service/limits/FleetSizeConstraint.kt`、`shadow/VrpShadowPriceMap.kt`、`RouteCompilationContext.kt`、`model/RouteColumnPool.kt`、`model/ArtificialCoverage.kt`、`service/limits/CustomerCoverageConstraint.kt`、`model/RouteCompilation.kt`、`Aggregation.kt`、`service/PipelineListGenerator.kt`、`service/limits/PhaseOneMinimization.kt`、`service/limits/RouteCostMinimization.kt`、`service/ShadowPriceKeyCastSupport.kt`、`shadow/FleetSizeShadowPriceKey.kt`、`shadow/CustomerCoverageShadowPriceKey.kt` |
| `network-scheduling-domain-route-generation-context/` | 10 | `service/EspprcPricer.kt`、`service/RouteGraphBuilder.kt`、`service/InitialRouteGenerator.kt`、`policy/PricingColumnSelector.kt`、`policy/LabelDominancePolicy.kt`、`model/PricingResult.kt`、`model/PricingRequest.kt`、`model/EspprcLabel.kt`、`model/ForbiddenCustomers.kt`、`model/VisitedCustomers.kt` |

---

### Phase F8：ospf-kotlin-framework-csp1d（P0）✅ 已完成

#### F8.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `infrastructure/dto/RenderDTO.kt` | ~16 | `@property x X coordinate / X坐标` 等 |
| `domain/length_assignment/...` | ~20 | `@property produce Produce aggregation / 产出聚合` 等 |
| `domain/yield/...` | ~30 | `@property config Yield modeling configuration / 产出建模配置` 等 |
| `domain/cutting_plan_generation/...` | ~12 | `@property materialId the material identifier / 材料标识符` 等 |
| `application/model/Csp1dAssignment.kt` | 2 | `@property x Unsigned integer decision variable... / 无符号整数决策变量...` |

#### F8.2 中英文另起一行修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `application/.../Csp1dShadowPriceLifecycle.kt` | 少量 | 描述主体中英文另起一行 |

#### F8.3 冗余限定符清理

| 文件 | 冗余限定符 | 处理方式 |
|------|-----------|---------|
| `csp1d-application/.../Csp1dProduceContext.kt:508` | `Map<fuookami.ospf.kotlin.core.model.mechanism.Constraint<Flt64, Linear>, Flt64>` | 添加 import，简化为 `Constraint` |
| 同上 `:517` | `fuookami.ospf.kotlin.framework.model.ShadowPrice(...)` | 添加 import，简化为 `ShadowPrice` |
| `csp1d-application/.../Csp1dMilp.kt:142,163,165,171,177` | 多处 `fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*` 和 `cutting_plan_generation.model.*` | 添加 import 并简化；注意同包多个类型应合并为 `.*` |
| `csp1d-domain-produce-context/.../MaterialConstraintPipeline.kt:52` | `fuookami.ospf.kotlin.math.algebra.number.UInt64.maximum` | 确认已有 import 后简化 |
| `csp1d-domain-produce-context/.../DemandConstraintPipeline.kt:224` | `fuookami.ospf.kotlin.quantities.unit.PhysicalUnit` | 添加 import |
| `csp1d-domain-material-context/.../Product.kt:108` | 同上 | 同上 |
| `csp1d-domain-produce-context/.../Produce.kt:68` | 同上 | 同上 |

---

### Phase F9：ospf-kotlin-framework-bpp3d（P0）✅ 已完成

#### F9.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `infrastructure/QuantityContainerCore.kt` | ~6 | `@return whether the cuboid fits... / 该长方体是否可放入...` |
| `infrastructure/Placement.kt` | ~20 | `@return the 2D geometry placement / 二维几何放置` 等 |
| `infrastructure/PWLRadiusSquaredApproximation.kt` | ~8 | `@return diagnostic key-value map / 诊断键值映射` 等 |
| `infrastructure/Projection.kt` | ~6 | `@return length dimension on this projection plane / 此投影平面上的长度维度` |
| `infrastructure/PackingShape.kt` | ~3 | `@return cuboid wrapped as a PackingShape3 / 包装为 PackingShape3 的长方体` |
| `infrastructure/Container.kt` | 1 | `@return Total amount of matching units. / 满足条件的单元总数。` |
| `infrastructure/OrientationAxisPermutationMapping.kt` | 1 | `@return The axis permutation for this orientation. / 对应方向的轴排列。` |
| `infrastructure/SemanticParameter.kt` | 少量 | `@return whether this pattern is a prefix... / 此模式是否为另一个模式的前缀` |
| `domain/layer_assignment/...` | ~5 | `@return linear polynomial with the symbol... / 以该符号为单项的线性多项式` |
| `domain/layer_generation/...` | 1 | `@return solver-internal FltX value... / 供求解器使用的降低精度的 FltX 值` |

#### F9.2 冗余限定符清理

| 文件 | 冗余限定符 | 处理方式 |
|------|-----------|---------|
| `infrastructure/QuantityContainerCore.kt:182` | `fuookami.ospf.kotlin.utils.functional.Order.Greater` | 确认已有 import 后简化为 `Order.Greater` |
| `domain-item-context/.../QuantityDomainModels.kt:151` | `fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType` | 添加 import |
| `domain-item-context/.../Package.kt:30,53,104,139` | 同上 | 同上 |
| `domain-item-context/.../DemandStatistics.kt:68` | `fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item` | 添加 import |
| `domain-packing-context/.../MaterialPacker.kt:328` | `fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.itemIdOf(itemId)` | 添加 import |
| `domain-layer-generation-context/.../LayerGenerationContext.kt:1628,2059` | `fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageCategory.Pallet` | 添加 import |

---

### Phase F10：ospf-kotlin-framework-gantt-scheduling（P1）✅ 已完成

gantt-scheduling 有 **53 个文件** 存在英文在前问题，远超初扫预期。

#### F10.1 中英文顺序修正

| 子模块 | 文件数 | 典型文件 |
|--------|-------|---------|
| `gantt-scheduling-infrastructure/` | 2 | `WorkingCalendar.kt`（~40+ 处）、`TimeRange.kt` |
| `gantt-scheduling-domain-task-context/` | 6 | `TaskStepGraph.kt`、`TaskPlan.kt`、`TaskBunch.kt`、`Task.kt`、`SchedulingSolverValueAdapter.kt`、`Cost.kt` |
| `gantt-scheduling-domain-task-compilation-context/` | 17 | `TaskTime.kt`、`IterativeContext.kt`、`IterativeAggregation.kt`，以及 `service/limits/` 下 14 个文件（`TaskExecutorCostMinimization.kt`、`TaskOverMaxDelayTimeMinimization.kt`、`TaskOverMaxAdvanceTimeMinimization.kt`、`TaskNotOnTimeMinimization.kt`、`TaskDelayTimeMinimization.kt`、`TaskDelayLastEndTimeMinimization.kt`、`TaskCostMinimization.kt`、`TaskCancelMinimization.kt`、`TaskAdvanceTimeMinimiaztion.kt`、`TaskAdvanceEarliestEndTimeMinimization.kt`、`SwitchTimeMinimization.kt`、`SwitchCostMinimization.kt`、`MakespanMinimization.kt`、`ExecutorLeisureMinimization.kt`） |
| `gantt-scheduling-domain-produce-context/` | 2 | `ProductionTask.kt`、`Consumption.kt` |
| `gantt-scheduling-domain-capacity-scheduling-context/` | 13 | `CapacitySchedulingContext.kt`、`Aggregation.kt`，`model/` 下 8 个文件（`Capacity.kt`、`CapacityColumn.kt`、`CapacityColumnAggregation.kt`、`CapacityCompilation.kt`、`CapacityOrderCompilation.kt`、`CapacitySchedulingSolution.kt`、`IterativeCapacityCompilation.kt`、`ProductionAction.kt`），`service/limits/` 下 3 个文件（`OrderConstraint.kt`、`ExecutorCapacityConstraint.kt`、`CapacityCostMinimization.kt`） |
| `gantt-scheduling-domain-bunch-generation-context/` | 2 | `SlotBasedBunchGenerator.kt`、`model/Graph.kt` |
| `gantt-scheduling-domain-bunch-compilation-context/` | 7 | `SlotBasedCapacityPreSolver.kt`、`SlotBasedBunchCompilationContext.kt`、`BunchCompilationContext.kt`、`Aggregation.kt`、`model/TaskReverse.kt`、`model/SlotBasedBunchCompilation.kt`、`model/SlotBasedCapacityResult.kt` |
| `gantt-scheduling-domain-resource-context/` | 1 | `service/limits/ResourceCapacityConstraint.kt` |
| `gantt-scheduling-application/` | 3 | `service/bunch/BranchAndPriceAlgorithm.kt`（~20+ 处）、`model/task/Iteration.kt`、`model/bunch/Iteration.kt` |

#### F10.2 中英文另起一行修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `infrastructure/WorkingCalendar.kt:2972-2976` | 2 | `* 按材料解析产出数量的期望单位。` → `* Resolves the expected quantity unit...` 及 `* 只检查指定材料...` → `* Checks only...` |

#### F10.3 冗余限定符清理

| 文件 | 冗余限定符 | 处理方式 |
|------|-----------|---------|
| `domain-task-context/.../SchedulingSolverValueAdapter.kt:92` | `fuookami.ospf.kotlin.math.algebra.concept.Flt64ValueConverter<V>` | 添加 import |

---

### Phase F11：ospf-kotlin-framework（P1）✅ 已完成

#### F11.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `solver/remote/client/RemoteSolverHttpClient.kt` | ~15 | `@property code the API response code / API 响应码` 等 |
| `solver/remote/client/RemoteQuadraticSolver.kt` | 少量 | 同类 |
| `solver/remote/domain/ValueTypes.kt` | 少量 | `@param`/`@return` 英文在前 |
| `solver/remote/domain/SerializedModels.kt` | 少量 | 同上 |
| `solver/remote/domain/Errors.kt` | 少量 | 同上 |
| `solver/ParallelCombinatorialLinearSolver.kt` | 少量 | 同上 |
| `solver/ParallelCombinatorialQuadraticSolver.kt` | 少量 | 同上 |
| `solver/ParallelCombinatorialColumnGenerationSolver.kt` | 少量 | 同上 |
| `persistence/expression/SortBy.kt` | 少量 | 同上 |
| `persistence/expression/ScalarFunctionDsl.kt` | 少量 | 同上 |
| `persistence/expression/PredicateFieldFilter.kt` | 少量 | 同上 |

---

### Phase F12：ospf-kotlin-framework-plugin（P1）✅ 已完成

#### F12.1 中英文顺序修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `persistence-mongodb/.../MongoRepository.kt` | 少量 | `@param` 英文在前 |
| `persistence-ktorm/.../KtormRequestRecord.kt` | 少量 | 同上 |
| `persistence-ktorm/.../KtormLogRecord.kt` | 少量 | 同上 |
| `persistence-ktorm/.../MathExtensions.kt` | 少量 | 同上 |
| `persistence-ktorm/.../translator/KtormBooleanTranslator.kt` | 少量 | 同上 |
| `persistence-mybatis/.../translator/MybatisUpdateTranslator.kt` | 少量 | 同上 |
| `persistence-mongodb/.../translator/MongoBooleanTranslator.kt` | 少量 | 同上 |

#### F12.2 中英文另起一行修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `persistence-sqlite/.../Sqlite.kt` | ~8 | 描述主体中中文和英文分处不同行，合并为 `中文 / English` 同行格式 |
| `persistence-redis/.../Redis.kt` | ~4 | 同上 |

#### F12.3 冗余限定符清理

| 文件 | 冗余限定符 | 处理方式 |
|------|-----------|---------|
| `persistence-ktorm/.../KtormLogRecord.kt:187` | `fuookami.ospf.kotlin.utils.serialization.readFromJson(...)` | 确认已有 import 后简化 |

---

### Phase F13：ospf-kotlin-benchmark（P2）✅ 已完成

#### F13.1 中英文另起一行修正

| 文件 | 预估处数 | 说明 |
|------|---------|------|
| `multiarray/MultiArrayHotPathBenchmark.kt` | ~5 | 描述主体中文和英文分处不同行（如 `* 对稀疏分块数组执行获取和包含检查操作` → `* Performs get and contains operations on the sparse block multi-array`） |
| `math/SymbolCombineBenchmark.kt` | ~3 | 同上（如 `* 基准测试用符号实现` → `* Symbol implementation for benchmarking purposes`） |
| `coreplugin/CorePluginDumpBenchmark.kt` | 少量 | 同上 |

---

### Phase F14：ospf-kotlin-example（P2）✅ 已完成

example 模块优先级最低，有 **251 个文件** 存在英文在前问题，但影响范围最小。

#### F14.1 中英文顺序修正

| 子模块 | 文件数 | 说明 |
|--------|-------|------|
| `framework_demo/demo4/domain/` | ~40 | `@property code The 3-character IATA airport code / 3字符IATA机场代码` 等 |
| `framework_demo/demo2/` | ~10 | 多处 `AbstractVariableItem`、`Err`、`ErrorCode` 等冗余限定符伴随的 KDoc |
| `framework_demo/demo1/` | ~20 | `route_context/`、`bandwidth_context/`、`Interface.kt`、`Application.kt` 等 |
| `core_demo/Demo3.kt` | ~3 | `@property minYield the minimum yield requirement / 最低产量要求` |
| `heuristic_demo/Demo1.kt` | 1 | `@return PSO solver execution result / PSO 求解器执行结果` |
| `heuristic_demo/Demo2.kt` | 1 | 同上 |

#### F14.2 冗余限定符清理

| 文件 | 冗余限定符 | 处理方式 |
|------|-----------|---------|
| `demo2/WeightRecommendationApplication.kt` | ~10 | `fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>`、`fuookami.ospf.kotlin.utils.error.Err(...)` 等 |
| `demo2/PredistributionApplication.kt` | ~10 | 同上 |
| `demo2/FullLoadApplication.kt` | ~8 | 同上 |
| `demo2/infrastructure/BendersSolver.kt` | ~6 | `fuookami.ospf.kotlin.utils.error.Err(...)`、`fuookami.ospf.kotlin.math.algebra.number.UInt64(1)` |
| `demo2/domain/stowage/StowageContext.kt` | ~2 | `fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution` |
| `demo2/domain/stowage/service/SolutionAnalyzer.kt` | ~3 | 同上 |
| `demo2/domain/stowage/service/limits/LoadAmountLimit.kt` | ~3 | `Ok<fuookami.ospf.kotlin.utils.functional.Success, ...>` |
| `demo4/domain/task/model/Passenger.kt` | 1 | `fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.PassengerClass` |
| `demo4/domain/bunch_selection/BunchSelectionContext.kt` | 1 | `fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.Aggregation(...)` |

---

## 6. 执行策略

### 6.1 每阶段执行步骤

1. **修改 KDoc 中英文顺序**：逐文件将 `English / 中文` 改为 `中文 / English`。
2. **合并另起一行的双语描述**：将分行的中文和英文合并为同一行 `中文 / English`。
3. **清理冗余限定符**：
   a. 确认短名无歧义（检查已有 import 和同文件中是否有同名符号）。
   b. 添加必要的 import，注意与已有同包 import 合并为 `.*` 通配符。
   c. 调整 import 排列顺序，严格遵循 §1.6.2。
   d. 将代码中的全限定名替换为短名。
4. **增量编译验证**：每完成一个模块后执行 `mvn compile -pl <module> -T 0.75C`，确保无编译错误。
5. **全量验收**：所有阶段完成后执行 `mvn clean compile test-compile -T 0.75C` 和 `mvn test -T 0.75C`。

### 6.2 歧义判定规则

以下短名可能存在歧义，需逐文件确认：

| 短名 | 潜在冲突 | 判定方式 |
|------|---------|---------|
| `Integer` | `kotlin.Int` 的伴生或 Java `java.lang.Integer` | 若文件已 import `kotlin.Int` 或使用 `Int` 作为类型，则 `Integer` 可能引起混淆；但 Kotlin 中 `Integer` 不是关键字，若 `fuookami.ospf.kotlin.core.variable.Integer` 通过 import 引入，编译器优先匹配 import 的类型 |
| `Continuous` | 无已知冲突 | 可安全简化 |
| `Order` | `kotlin.comparisons.Ordering` 或其他 `Order` | 需确认文件中是否有其他 `Order` import |
| `Solution` | 多个模块有 `Solution` 类 | 需确认同文件是否已 import 其他 `Solution` |

### 6.3 import 合并示例

**修改前**：
```kotlin
import fuookami.ospf.kotlin.core.variable.Binary
import fuookami.ospf.kotlin.core.variable.Ternary
// ... 代码中使用 fuookami.ospf.kotlin.core.variable.Integer
```

**修改后**：
```kotlin
import fuookami.ospf.kotlin.core.variable.*
// ... 代码中使用 Integer
```

**修改前**：
```kotlin
import fuookami.ospf.kotlin.utils.error.Err
// ... 代码中使用 fuookami.ospf.kotlin.utils.error.ErrorCode.ApplicationError
```

**修改后**：
```kotlin
import fuookami.ospf.kotlin.utils.error.*
// ... 代码中使用 ErrorCode.ApplicationError
```

## 7. 验收标准

- [x] 所有 `@param`/`@property`/`@return` 双语描述遵循 `中文 / English` 格式，无英文在前的残留。
- [x] 所有 KDoc 描述主体中的双语内容在同一行以 ` / ` 分隔，无另起一行残留。
- [x] 代码中无非必要的全限定名或部分限定名（字符串常量、KDoc 代码示例和存在真实歧义的限定符除外）。
- [x] 所有新增 import 已与同包已有 import 合并为 `.*` 通配符（符合 §1.6.2 规则）。
- [x] 所有 import 排列顺序符合 §1.6.2 规定。
- [x] 全量编译通过：`mvn clean compile test-compile -T 0.75C`。
- [x] 全量测试通过：`mvn test -T 0.75C`。
- [x] 无行为变更，git diff 仅涉及注释格式和 import/限定符调整。

### 7.1 保留的限定符（存在真实歧义）

以下全限定名经逐文件确认后保留，不可简化：

| 文件 | 限定符 | 原因 |
|------|--------|------|
| `bpp3d-domain-item-context/.../DemandStatistics.kt:65` | `fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item` | 同文件 `Bpp3dDemandKey.Item` data class 与 `Item` 短名冲突 |
| `demo4/domain/bunch_compilation/BunchCompilationContext.kt:25` | `fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.BunchCompilationContext<` | 本地 class `BunchCompilationContext` 与框架 interface 冲突 |
| `demo4/domain/bunch_selection/BunchSelectionContext.kt:52` | `fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.Aggregation(` | `bunch_compilation.*` 和 `bunch_generation.*` 均导入 `Aggregation`，存在歧义 |
| `demo1/bandwidth_context/model/ServiceBandwidth.kt:41` | `fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.to(node)` | 局部变量 `val to` 遮蔽了导入的 `to` 函数 |

## 8. 执行记录

### 2026-08-01 第一轮：KDoc 中英文顺序与另起一行修正

使用自动化脚本完成所有模块的 KDoc 格式修正。

**修改统计**：

| 模块 | 修改文件数 |
|------|-----------|
| ospf-kotlin-utils | 47 |
| ospf-kotlin-multiarray | 7 |
| ospf-kotlin-math | 216 |
| ospf-kotlin-quantities | 38 |
| ospf-kotlin-core | 72 |
| ospf-kotlin-core-plugin | 34 |
| ospf-kotlin-framework | 30 |
| ospf-kotlin-framework-bpp3d | 86 |
| ospf-kotlin-framework-csp1d | 61 |
| ospf-kotlin-framework-gantt-scheduling | 48 |
| ospf-kotlin-framework-network-scheduling | 31 |
| ospf-kotlin-framework-plugin | 34 |
| ospf-kotlin-example | 219 |
| **合计** | **923** |

### 2026-08-01 第二轮：冗余限定符清理

逐文件检查并清理冗余限定符，对存在命名冲突的保留限定符。

**已清理**：

| 模块 | 清理内容 |
|------|---------|
| core-plugin Variable 文件（7个） | `Integer`/`Continuous` 全限定名 → 短名，import 合并为 `.*` |
| core-plugin heuristic 文件（14个） | `RealNumber`/`NumberField` 全限定名 → 短名，import 合并为 `.*` |
| core-plugin scip 文件（2个） | `SolvingStatus` 全限定名 → 短名 |
| core SolverExt.kt | `ErrorCode.ApplicationError` 全限定名 → 短名 |
| example demo2 文件（5个） | `Solution` 全限定名 → 短名 |
| example demo4 Passenger.kt | `PassengerClass` 全限定名 → 短名 |
| framework 各模块 | network-scheduling/csp1d/bpp3d/gantt-scheduling/plugin 冗余限定符清理 |

**保留（存在真实歧义）**：见 §7.1

**编译验证**：`mvn clean compile test-compile -T 0.75C` → BUILD SUCCESS
**测试验证**：`mvn test -T 0.75C` → BUILD SUCCESS

### 2026-08-02 第三轮：冗余泛型参数与 if-null 折叠

#### F15：is Ok/Failed/Fatal<*, ErrorCode, Error<ErrorCode>> 冗余泛型参数（P1）✅ 已完成

Kotlin 的 `is` 类型检查运行时泛型擦除，`is Ok` 完全等价于 `is Ok<*, ErrorCode, Error<ErrorCode>>`。
代码库中 core/framework 等模块已使用 `is Ok` 简洁风格，仅 example 模块存在冗余。

| 模块 | 处数 | 涉及文件数 |
|------|------|-----------|
| ospf-kotlin-example（src/main） | 927 | 110 |
| ospf-kotlin-framework（`is Fatal<*, *, *>`） | 2 | 1 |
| ospf-kotlin-math | 3 | 1 |
| ospf-kotlin-framework-gantt-scheduling | 3 | 1 |
| 测试文件（src/test，全模块） | 34 | 14 |
| **合计** | **969** | **127** |

**修复方式**：
- `is Ok<*, ErrorCode, Error<ErrorCode>>` → `is Ok`
- `is Failed<*, ErrorCode, Error<ErrorCode>>` → `is Failed`
- `is Fatal<*, ErrorCode, Error<ErrorCode>>` → `is Fatal`
- `is Fatal<*, *, *>` → `is Fatal`
- `is Ok<*, *, *>` → `is Ok`
- `is Failed<*, *, *>` → `is Failed`

**执行记录**：使用 Python 脚本（嵌套尖括号深度计数）批量替换，首次因正则 `[^>]+` 无法匹配嵌套泛型导致残留 `>`，回滚后用深度计数法重跑，935 处主源码 + 34 处测试全部正确替换。

#### F16：if-null-return-null 折叠（P2）

将 `if (x == null) return null` 折叠为 `x ?: return null`，与代码库现有风格统一。

| 文件 | 处数 | 说明 |
|------|------|------|
| `core/.../IntermediateSymbolExpressionSupport.kt` | 4 | ✅ 已修复 |
| `math/.../Evaluate.kt` | 2 | ✅ 已修复 |
| `multiarray/.../Vector.kt` | 1 | ✅ 已修复 |
| `math/.../EvaluateBoolean.kt:413` | 1 | ⏳ `if (left == null || right == null) return null` 涉及 `||` 不适合简单折叠 |

### 2026-08-02 第四轮：符号运算原生 API 替换为运算符 API

#### F17：MutableLinearPolynomial + LinearMonomial + verbose conversion 替换为运算符 DSL（P1）✅ 已完成

代码库提供了 `Flt64QuickDsl` 运算符 API，支持 `Symbol + Symbol`、`Symbol + Flt64`、`LinearPolynomial + Symbol` 等运算符写法，
以及 `LinearPolynomial(symbol)` 快捷构造。但 example/framework 模块中大量使用了冗余的原生 API 模式：

**冗余模式 A：MutableLinearPolynomial + += LinearMonomial + verbose conversion**

```kotlin
// 冗余写法
val poly = MutableLinearPolynomial()
poly += LinearMonomial(Flt64.one, stowage.stowage[i1, j1])
poly += LinearMonomial(Flt64.one, stowage.stowage[i2, j2])
relation = LinearPolynomial(poly.monomials, poly.constant) leq Flt64.one,

// 运算符写法
relation = (stowage.stowage[i1, j1] + stowage.stowage[i2, j2]) leq Flt64.one,
```

**冗余模式 B：LinearMonomial(coefficient, symbol) 单项式构造**

```kotlin
// 冗余写法
LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)), Flt64.zero)

// 运算符写法
LinearPolynomial(resultVar)  // Flt64QuickDsl 提供的快捷构造
```

**冗余模式 C：LinearMonomial 显式构造 + `.asMutable() +=`**

```kotlin
// 冗余写法
quantity[slot].asMutable() += LinearMonomial(unitUsage.toSolverValue(), compilation.operationTime[actionIndex, slotIndex])

// 运算符写法（需确认 asMutable 返回类型是否支持 += Flt64 * Symbol 之类的运算符）
```

**评估结论**：

| 模式 | 可替换 | 说明 |
|------|--------|------|
| 模式 A（Mutable + += LinearMonomial + verbose conversion） | ✅ 可替换 | 最常见的冗余模式，均可改为运算符表达式 |
| 模式 B（`LinearPolynomial(listOf(LinearMonomial(one, x)), zero)`） | ✅ 可替换 | 改为 `LinearPolynomial(x)` 快捷构造 |
| 模式 C（`.asMutable() += LinearMonomial(coef, sym)`） | ⚠️ 部分可替换 | 当系数为 `Flt64.one` 时可简化为 `sym.asLinearPoly()`；当系数为非常量时需评估是否可用 `coef * sym` 运算符 |
| `LinearMonomial(coefficient * scale, it.symbol)` 内 map 变换 | ❌ 不替换 | 这类是数据变换而非建模表达，运算符 API 无法简化 |
| framework/solver 中的 `converter.intoValue/fromValue` 转换 | ❌ 不替换 | 涉及泛型 V 到 Flt64 转换，运算符 API 仅支持 Flt64 |
| core 模块内的符号函数实现（BigM, Max, Slack 等） | ❌ 不替换 | 这些是库内部实现，需要在泛型 V 上操作 |

**受影响文件清单**（57 个文件，90 处冗余模式）：

##### 模式 A：MutableLinearPolynomial + verbose conversion（21 处，全部可替换）

| 文件 | 冗余处数 | 说明 |
|------|---------|------|
| `example/demo2/domain/airworthiness_security/model/MaxCLIM.kt:102` | 1 | `poly += LinearMonomial(slope, xSymbol)` → `slope * xSymbol` |
| `example/demo2/service/limits/CLIMLimit.kt:34,52` | 2 | `upper/lower += LinearMonomial(±Flt64.one, ...)` → 运算符 |
| `example/demo2/service/limits/CumulativeLoadWeightLimit.kt:51` | 1 | 同上 |
| `example/demo2/service/limits/LinearDensityLimit.kt:46` | 1 | 同上 |
| `example/demo2/service/limits/UnsymmetricalLinearDensityLimit.kt:60` | 1 | 同上 |
| `example/demo2/service/limits/ZoneLoadWeightLimit.kt:56` | 1 | 同上 |
| `example/demo2/service/limits/ItemReserveLimit.kt:40` | 1 | 同上 |
| `example/demo2/service/limits/ELDAdjacentLimit.kt:58,71` | 2 | 经典示例：`stowage[i1,j1] + stowage[i2,j2]` |
| `example/demo2/service/limits/BiologicalAdjacentLimit.kt:59,72` | 2 | 同上 |
| `example/demo2/service/limits/EmptyHatedLimit.kt:42` | 1 | 同上 |
| `example/demo2/service/limits/RecommendLoadWeightLimit.kt:39` | 1 | 同上 |
| `example/demo2/service/limits/RecommendedWeightDeviationObjective.kt:32` | 1 | 同上 |
| `example/demo2/service/limits/RecommendedWeightEqualizationLimit.kt:49,61` | 2 | 同上 |
| `example/demo2/service/limits/LateralBalanceLimit.kt:35` | 1 | 同上 |
| `example/demo2/service/limits/LongitudinalBalanceLimit.kt:38` | 1 | 同上 |
| `example/demo2/service/limits/FleetBalanceLimit.kt:75` | 1 | 同上 |
| `example/demo2/service/limits/FlightLinkLimit.kt:73` | 1 | 同上 |
| `example/demo2/service/limits/PassengerCancelMinimization.kt:45` | 1 | 同上 |
| `example/demo2/service/limits/PassengerClassChangeMinimization.kt:54` | 1 | 同上 |
| `example/demo2/service/limits/PassengerFlightChangeMinimization.kt:52` | 1 | 同上 |
| `example/heuristic_demo/Demo2.kt:56` | 1 | `obj += LinearMonomial(Flt64.one, x/y)` → `x + y` |
| `framework/gantt-scheduling/.../FleetBalance.kt:152` | 1 | 同上 |
| `framework/gantt-scheduling/.../FlightLink.kt:85` | 1 | 同上 |

##### 模式 B：`LinearPolynomial(listOf(LinearMonomial(one, x)), zero)` 快捷构造可替换（待评估）

此类分布在 `framework/gantt-scheduling/task_compilation/model/TaskTime.kt`、`framework/bpp3d/` 各 service/limits 文件等约 40+ 处。
需逐个评估：系数为 `Flt64.one` 的可改为 `LinearPolynomial(x)`，系数为其他常量的需评估是否可用 `coef * symbol`。

##### 不替换（库内部代码）

- `ospf-kotlin-core/` 中 `symbol/function/` 下所有文件（BigM、Max、Slack 等）——泛型 V 运算，运算符 API 不适用
- `ospf-kotlin-core/model/mechanism/` 中的 `converter.intoValue/fromValue` 转换——泛型 V 操作
- `ospf-kotlin-math/` 中 `symbol/operation/` 下所有文件——运算符 API 自身的定义
- `ospf-kotlin-framework/solver/BendersDecompositionSolver.kt`——泛型 V 转换

**执行记录（2026-08-02）**：已完成计划边界内 example/framework 的 Flt64 建模代码替换，涵盖 Gantt scheduling、network scheduling、CSP1D 以及 framework demo/heuristic demo；将可由 DSL 表达的显式单项式、多项式累加和冗余单符号构造改为运算符写法，并同步整理所需 `symbol.monomial.*` 导入。保留泛型 FltX、数据转换映射、solver converter、库内部实现及测试中的显式构造。`mvn compile -T 0.75C` 增量编译通过，`git diff --check` 通过。

**泛型快捷构造补充记录（2026-08-02）**：为 `LinearPolynomial<T>(symbol)` 增加 reified 泛型入口，覆盖 `FltX`、`Flt64`、`RtnX`、`Rtn64`、`UIntX`、`UInt64`、`IntX` 和 `Int64`；同时使上述数值类型的 companion 对象可作为 `Flt64ValueConverter`，因此 `QuickDsl(type)` 的泛型构造也可用。顶层泛型入口遵循常量解析器的安全策略，需要启用 companion reflection fallback；未启用时可显式传入对应 converter。`mvn clean compile test-compile -T 0.75C` 与 `mvn test -T 0.75C` 全量通过。
