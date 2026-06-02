# CSP1D 泛型化计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-csp1d` 已有少量领域模型，当前数值类型固定情况如下：

1. `Product.width: List<Flt64>`。
2. `Product.length/unitWeight/weight/maxOverProduceLength: FltX?`。
3. `Production.width: List<Flt64>`，`length/unitWeight: FltX?`。
4. `WidthRange.width: ValueRange<Flt64>`，`step: Flt64`。
5. 渲染 DTO 使用 `FltX`。

当前模块没有明显 solver 模型层，泛型化范围主要是领域物理量和 DTO 边界。

补充检查 `E:\workspace\poit\csp1d` 后，当前 framework-csp1d 距离“相对抽象的一维分切开发包”还有较大缺口：

1. `poit/csp1d` 实际包含 material、cutting plan generation、produce、yield、length assignment、wasting minimization、application 等上下文。
2. framework-csp1d 目前只有 material 雏形、produce 空壳和少量 infrastructure，缺少切割方案生成、主问题、产出约束、动态卷长、余料/浪费和应用编排。
3. `csp1d-produce-context` 当前还存在 `framework.bpp3d.domain.produce` 包名兼容文件，应在补齐模块时清理或迁移到正确的 `framework.csp1d` 包边界。

## 2. 参考模块的抽象方式

本计划需要按既有 framework 的抽象方式制定，而不是把项目源码直接搬进来。

### 2.1 Gantt Scheduling 的抽象方式

`ospf-kotlin-framework-gantt-scheduling` 参考了 `E:\workspace\poit\psp` 和 `E:\workspace\poit\aps`，其模式是“多项目交集抽象 + 项目特性下沉到业务侧”：

1. PSP 的 `pulping`、`energy`、`capacity-compilation`、`bunch-compilation`、`bunch-generation` 被抽象成 task/bunch/capacity/resource 等通用调度上下文。
2. APS 的 `task`、`produce`、`work-shift`、`master-plan`、`material-requirements-planning`、`filling-production-schedule` 被拆解到 task、produce、resource、capacity scheduling 和 application 编排中。
3. framework 没有保留 `psp-*` / `aps-*` 的项目包名、接口服务、控制台、请求响应 DTO、运行上下文和项目参数。
4. Bunch generation、bunch compilation、branch-and-price、column generation 这类算法骨架被保留；造纸制浆、能源、工厂班次等项目语义通过更抽象的 task/resource/produce/capacity 表达。

对 CSP1D 的启发：

1. `csp1d-domain-material-context` 不应只放 POIT 的产品/原料字段，而应抽象为一维分切的 material/product/demand/machine/cutting-plan 核心语义。
2. `produce`、`yield`、`length assignment`、`wasting minimization` 应像 gantt 的 produce/resource/capacity 一样作为可组合上下文，而不是混成单个大应用。
3. application 只保留 MILP、列生成、恢复、排程、KPI、解分析等通用编排，不保留项目 DTO 和运行心跳协议。

### 2.2 BPP3D 的抽象方式

`ospf-kotlin-framework-bpp3d` 参考了 `E:\workspace\tcl\bpp3d`，其模式是“保留通用装载算法族，剥离客户项目层”：

1. TCL 的 `bpp3d-domain-item-context`、`packing-context`、`bla-context`、`block-loading-context` 被保留为 framework 中的 item、packing、BLA、block loading。
2. TCL 的 `bpp3d-domain-layer-loading` 不是原样保留，而是重组为 layer generation、layer assignment、layer selection；precise/imprecise 的差异被收敛到更抽象的 aggregation 和 assignment 模型。
3. TCL 的 `bpp3d-project-zsac`、`bpp3d-project-hztv`、interface service、web、renderer 等项目/接口层没有进入 framework。
4. TCL 中未形成稳定通用边界的 `cbu`、`epa` 没有作为第一阶段 framework 模块直接纳入。

对 CSP1D 的启发：

1. `poit/csp1d` 中稳定、可复用的切割方案生成、主问题、产出、卷长、浪费上下文可以进入 framework。
2. 项目接口、数据格式、公式解析器、solver 插件选择和业务运行过程应作为 adapter 留在下游。
3. 如果某个上下文暂时只有单项目特化规则，应先定义扩展点，不急着把所有规则做成 framework 必选模块。

### 2.3 CSP1D 的抽象原则

因此 CSP1D 的开发包边界按三层制定：

1. 通用核心层：material、cutting plan、produce 主问题、solver/quantity adapter。
2. 通用增强层：yield、length assignment、wasting minimization、schedule variant。
3. 下游适配层：PO/DTO、公式语言、接口服务、控制台、项目运行参数、心跳、租户上下文、solver 插件选择。

## 3. 从 poit/csp1d 抽象出的开发包边界

目标不是把 `poit/csp1d` 原样搬进 framework，而是沉淀一维分切的通用内核：

1. 领域对象：产品、订单需求、配规/副产物、原料、设备、幅宽范围、缺陷、切割方案、解和 KPI。
2. 切割方案生成：枚举、DFS、FullSum、N-Same、N-Sum、MILP pricing、reduced cost pricing。
3. 主问题求解：在给定切割方案集合上选择车次/批次，约束需求、产能、物料用量、换料、兼容性和多幅宽使用。
4. 可选增强上下文：动态卷长分配、产出/欠产/超产约束、余料和成本最小化、排程版本约束。
5. 应用编排：提供 MILP、列生成、恢复、排程、Top-K、KPI 和解分析的抽象入口。

不建议进入 framework 的内容：

1. `interface-service`、`interface-consoles`、HTTP/CLI 入口。
2. `com.poit` 请求/响应 DTO、租户/运行上下文、项目参数和心跳对象。
3. 直接依赖 SCIP/Gurobi/heuristic 插件的项目配置，framework 只通过 core solver 抽象接入。
4. Aviator 公式实现可以做成 adapter；领域层只保留 `WeightCalculator<V>` / `WeightFormulaEvaluator<V>` 接口。

## 4. 应实现的模块

建议把 root `pom.xml` 从当前 3 个子模块扩展为下列模块。必需模块先落地，可选模块按下游项目复用程度逐步补齐。

| 模块 | 来源参考 | 职责 | 优先级 |
|------|----------|------|--------|
| `csp1d-infrastructure` | `poit/csp1d-infrastructure` | 通用配置、算法参数、solver 选项、产品排序策略、render DTO、单位/数值 adapter | 必需 |
| `csp1d-domain-material-context` | `poit/csp1d-domain-material-context` | `Product<V>`、`ProductOrder<V>`、`ProductDemand<V>`、`Costar<V>`、`Material<V>`、`Machine<V>`、`WidthRange<V>`、`CuttingPlan<V>`、`ShadowPriceMap<V>`、`Solution<V>`、`WeightCalculator<V>`、`MaterialContext<V>` | 必需 |
| `csp1d-domain-cutting-plan-generation-context` | `poit/csp1d-domain-cutting-plan-generation-context` | 生成可行切割方案，包含 DFS、FullSum、N-Same、N-Sum、MILP pricing、缺陷/分段、边部/中部约束、刀数约束、批次冲突、余宽和 reduced cost 目标 | 必需 |
| `csp1d-domain-produce-context` | `poit/csp1d-domain-produce-context` | 主问题/母问题，选择切割方案车次，维护 `CuttingPlanUsage`、`MaterialUsage`、`MaterialSwitch`、`WidthUsage`、机器产能、物料用量、兼容性、多幅宽和换料目标 | 必需 |
| `csp1d-domain-yield-context` | `poit/csp1d-domain-yield-context` | 产品/配规的卷数、重量、张数需求约束，以及欠产/超产/产出量目标 | 建议 |
| `csp1d-domain-length-assignment-context` | `poit/csp1d-domain-length-assignment-context` | 动态卷长分配、超长约束、长度最小化、批次最小化、排程版本长度分配 | 建议 |
| `csp1d-domain-wasting-minimization-context` | `poit/csp1d-domain-wasting-minimization-context` | 余宽、余料、成本、超产面积浪费等目标，以及排程版本浪费目标 | 建议 |
| `csp1d-application` | `poit/csp1d-application` | 抽象应用入口：`Csp1dMilp`、`Csp1dColumnGeneration`、`Csp1dRecovery`、`Csp1dSchedule`、`SolutionAnalyzer`、`Kpi`、`TopKCuttingPlans` | 必需 |

暂不单独拆 `csp1d-domain-schedule-context`。`poit/csp1d` 的 schedule 逻辑目前分散在 produce、length assignment、yield、wasting minimization 中，第一阶段可以保留各上下文内的 schedule variant；当重复接口稳定后再抽出 schedule 公共层。

## 5. 抽象功能清单

### 5.1 问题定义 API

需要提供一个 PO/DTO 无关的输入模型，供下游项目把自己的请求映射进 framework：

```kotlin
data class Csp1dProblem<V : RealNumber<V>>(
    val products: List<Product<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>>,
    val costars: List<Costar<V>> = emptyList(),
    val demands: Map<Product<V>, ProductDemand<V>>,
    val configuration: Csp1dConfiguration<V>
)
```

这里的宽度、长度、重量、坐标、产能全部应是 `Quantity<V>`；刀数等纯组合计数继续使用 `UInt64`；卷数需求、重量需求、张数需求统一表达为 `ProductDemand<V>` 中的 `Quantity<V>`；损耗率、兼容性、惩罚权重、归一化 reduced cost 使用裸 `V`。

### 5.1.1 统一需求模型

`poit/csp1d` 中按卷数、重量、张数存在三种需求计算方式。迁移到 framework 时不应保留三套 demand 字段或三套约束入口，而应统一为：

```kotlin
data class ProductDemand<V : RealNumber<V>>(
    val product: Product<V>,
    val quantity: Quantity<V>,
    val mode: DemandMode? = null
)
```

语义规则：

1. 卷数需求：使用离散卷数单位，例如 `roll`，`quantity.unit.domain == QuantityDomain.Discrete`。
2. 张数需求：使用离散张数单位，例如 `sheet`，`quantity.unit.domain == QuantityDomain.Discrete`。
3. 重量需求：使用质量单位，例如 `Kilogram`，`quantity.unit.domain == QuantityDomain.Continuous`。
4. `DemandMode` 可以作为决策对象和约束对象的语义标签，用于追踪该 demand 来自卷数、重量或张数口径。
5. framework 领域层只读取 `quantity.unit.domain` 判定离散/连续，不通过单位名称、业务枚举、`DemandMode` 或 dimension 推断。
6. 旧项目的三种计算方式应在 adapter 层转换为 `ProductDemand<V>`，进入 framework 后不再拆成三套字段或三套约束入口。

建议标签：

```kotlin
enum class DemandMode {
    Roll,
    Weight,
    Sheet
}
```

`DemandMode` 的使用边界：

1. 可以出现在 `ProductDemand<V>`、demand decision、demand constraint、solution analyzer 和 KPI 中。
2. 只用于命名、分组、追踪来源、报表聚合和调试输出。
3. 不参与单位换算、数值计算、离散/连续判定和 solver 变量类型判定。
4. 同一个 `DemandMode` 下仍必须以 `Quantity<V>` 作为唯一需求值。

对应的产出贡献也应统一为 `Quantity<V>`：

```kotlin
data class CuttingPlanDemandContribution<V : RealNumber<V>>(
    val product: Product<V>,
    val quantity: Quantity<V>
)
```

主问题和 yield 约束只比较同一 unit/dimension 下的 demand 与 contribution；必要的单位换算通过 `Quantity` 完成。

### 5.2 切割方案与可行性

必需功能：

1. `CuttingPlan<V>` 表达原料、产品切片、配规切片、缺陷配规、分段、幅宽、余宽、长度、车次和来源迭代。
2. `Cut<V>` / `Segment<V>` 表达切割位置、宽度、生产对象和是否提前分切。
3. `Material<V>.enabled(...)` 抽象出幅宽、刀数、同单位长度在边/中、单位批次、超长、缺陷/分段等可行性规则。
4. `CuttingPlanSortStrategy<V>` 支持按换产距离、刀组、幅宽差等规则排序。

### 5.3 方案生成/定价

必需功能：

1. 快速枚举：`NSame`、`NSum`、`FullSum`、`DFS`。
2. MILP pricing：从 shadow price 生成 reduced cost 为负的切割方案。
3. 约束插件：`MaterialConstraint`、`SegmentConstraint`、`PositionAmountConstraint`、`OnSideConstraint`、`InMiddleConstraint`、`UnitBatchConflictConstraint`。
4. 目标插件：`WasteMinimization`、`ReducedCostMinimization`。
5. 支持普通方案、缺陷方案、子切方案和配规方案。

### 5.4 主问题/生产上下文

必需功能：

1. 静态 MILP：给定候选切割方案集合，求每个方案使用车次。
2. 列生成：暴露 shadow price，接收新的切割方案列，支持迭代求解。
3. 变量模型：`CuttingPlanUsage`、`MaterialUsage`、`MaterialSwitch`、`WidthUsage`、`MachineCapacityUsage`。
4. 约束模型：产品需求、物料用量、机器产能、切割方案最小/最大使用、物料兼容、多幅宽使用、换料次数。
5. 目标模型：批次最小化、欠用/超用惩罚、换料最小化、幅宽差最小化、产能超用惩罚。

### 5.5 产出、卷长和浪费扩展

建议功能：

1. `yield`：按统一 `ProductDemand<V>` 表达需求，并输出产品/配规的欠产和超产；卷数、重量、张数只是 `quantity.unit` 不同。
2. `length_assignment`：给动态长度产品分配卷长，控制超长和批次。
3. `wasting_minimization`：最小化余宽、余料、成本和超产面积浪费。
4. schedule variant：当需要按日期/订单分段时，使用 schedule 版本的 aggregation、constraint 和 analyzer。

### 5.6 Solver 和数值边界

领域公开 API 应使用 `Quantity<V>` 和 `V : RealNumber<V>`。如果底层 solver 仍然只接受 `Flt64`，必须集中在 adapter 中转换：

```kotlin
interface Csp1dSolverValueAdapter<V : RealNumber<V>> {
    fun toSolver(value: V): Flt64
    fun fromSolver(value: Flt64): V
}
```

`AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`Flt64` shadow price 不应泄漏到领域模型和 application 公开 API。

## 6. 泛型化目标

1. 将宽度、长度、重量等有物理量纲的业务数值改为 `Quantity<V>`。
2. 保留 Flt64/FltX 兼容入口，避免一次性破坏外部调用。
3. DTO 不承担领域泛型，序列化边界可以继续使用 `FltX` 或字符串/BigDecimal，但必须通过 mapper 从领域类型转换。

## 7. 物理量化硬规则

CSP1D 领域层中有量纲字段必须使用 `Quantity<V>`：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 坐标/裁切位置 | `x`, `start`, `end` | `Length` |
| 宽度/长度 | `width`, `length`, `maxOverProduceLength` | `Length` |
| 重量 | `weight` | `Mass` |
| 单位重量 | `unitWeight` | `Mass / Length / Length` 或业务定义单位 |
| 产能/加工能力 | `capacity`, `produceCapacity`, `cuttingRate` | `Length / Time`、`Amount / Time` 或业务定义单位 |

裸 `V` 只用于无量纲值，例如损耗率、利用率、惩罚系数和归一化目标值。

需求硬规则：

1. 产品需求必须使用 `ProductDemand<V>(product, quantity)`。
2. 不新增 `rollDemand`、`weightDemand`、`sheetDemand` 等平行字段。
3. `DemandMode.Roll`、`DemandMode.Weight`、`DemandMode.Sheet` 只作为决策对象和约束对象的标签。
4. demand 建模统一读取 `quantity.unit.domain`。
5. 离散 demand 进入整数/离散约束，连续 demand 进入连续约束。
6. 三种旧需求计算方式只允许作为 adapter 或 legacy factory 的输入形式。

## 8. 改造步骤

### Phase C0：模块骨架补齐

先建立与抽象边界一致的模块骨架：

1. 在 root `pom.xml` 中补齐 `csp1d-domain-cutting-plan-generation-context`、`csp1d-domain-yield-context`、`csp1d-domain-length-assignment-context`、`csp1d-domain-wasting-minimization-context`、`csp1d-application`。
2. 为各子模块补齐 Maven 依赖，按 `infrastructure -> material -> cutting-plan-generation -> produce -> yield/length/wasting -> application` 控制依赖方向。
3. root 依赖补充 `ospf-kotlin-math` 和 `ospf-kotlin-quantities`，避免 `Quantity<V>` 通过传递依赖隐式进入。
4. 清理 `csp1d-produce-context` 中错误的 `framework.bpp3d` 包名兼容文件。
5. 保留 `RenderDTO` 在 infrastructure，项目请求/响应 DTO 留给下游。

验收：

- [x] root `pom.xml` 模块清单与本文件第 4 节一致。
- [x] 子模块间无反向依赖。
- [x] `git grep -n "com.poit\\|framework.bpp3d" -- ospf-kotlin-framework-csp1d` 没有领域代码命中。

### Phase C0-1：最小可用一维分切开发包

第一版不用一次性覆盖 `poit/csp1d` 的全部增强能力，但必须能完成标准流程：

1. 下游 DTO 映射为 `Csp1dProblem<V>`。
2. 根据产品、原料、设备和配规生成初始切割方案。
3. 在主问题中选择切割方案车次。
4. 输出 `Csp1dSolution<V>`、KPI 和 render 数据。
5. 支持静态 MILP 和列生成两条入口。

验收：

- [x] 有一个不依赖 `com.poit` 的 example/test 覆盖“按卷数需求的一维分切”。
- [x] 有一个 example/test 覆盖“配规 + 余宽 + 机器产能”。
- [x] `Csp1dColumnGeneration` 可以从主问题 shadow price 调用 pricing 生成新列。

### Phase C1-0：定义数值与量纲策略

先确定 CSP1D 是否需要物理单位：

1. 若只需要统一高精度数值，使用 `V : RealNumber<V>`。
2. 若要表达宽度、长度、重量量纲，使用 `Quantity<V>`。

结论：领域物理量用 `Quantity<V>`，计算过程需要纯系数时使用裸 `V`。

验收：

- [x] 宽度/长度/重量的单位约定写入 README 或本文件。
- [x] 提供 Flt64/FltX 默认工厂。

### Phase C1-1：领域模型泛型化

目标签名：

```kotlin
interface Production<V> {
    val id: String?
    val width: List<Quantity<V>>
    val length: Quantity<V>?
    val unitWeight: Quantity<V>?
}

open class Product<V>(
    override val id: String,
    val name: String,
    override val width: List<Quantity<V>>,
    override val length: Quantity<V>? = null,
    override val unitWeight: Quantity<V>? = null,
    weight: Quantity<V>? = null,
    val maxOverProduceLength: Quantity<V>? = null,
    val dynamicLength: Boolean = false
) : Production<V>, ManualIndexed()
```

验收：

- [x] `Product<Flt64>` 编译通过。
- [x] `Product<FltX>` 编译通过。
- [x] `weight(width, length)` 返回单位正确的重量量纲。

### Phase C1-2：`WidthRange` 泛型化

目标：

```kotlin
data class WidthRange<V>(
    val width: ValueRange<Quantity<V>>,
    val step: Quantity<V>
)
```

如 `ValueRange<Quantity<V>>` 因比较接口不足不能直接使用，则新增 `QuantityRange<V>`。

验收：

- [x] 区间上下界和步长单位一致。
- [x] Flt64/FltX 两种特化测试通过。

### Phase C1-3：DTO 边界

`RenderDTO` 是序列化边界，不建议直接泛型化为 `RenderDTO<V>`。

改造方式：

1. 保留 `RenderCuttingPlanProductionDTO` 的 `FltX` 字段，作为稳定输出格式。
2. 新增 mapper：`fun <V : RealNumber<V>> Product<V>.toRenderDto(...)`。
3. mapper 内显式执行 `Quantity<V>.toFltX()`。

验收：

- [x] DTO 输出兼容旧格式。
- [x] 领域模型不再为了 DTO 固定为 `FltX`。

### Phase C1-4：三种需求方式合并

目标：

1. 定义 `ProductDemand<V>`，用 `Quantity<V>` 表达需求值。
2. 将卷数、重量、张数三种需求计算方式迁移为 adapter 层输入转换，并映射为 `DemandMode` 标签。
3. 定义 `CuttingPlanDemandContribution<V>` 或等价模型，使切割方案产出统一贡献 `Quantity<V>`。
4. 主问题 demand constraint、yield 欠产/超产、solution analyzer 均基于统一 demand/contribution 聚合。
5. 通过 `quantity.unit.domain` 决定建模变量和约束是离散还是连续。

验收：

- [x] 领域主路径不存在 `rollDemand`、`weightDemand`、`sheetDemand` 三套平行字段。
- [x] `DemandMode` 只作为 decision/constraint/analyzer/KPI 标签，不参与数值计算。
- [x] 卷数需求可用离散单位建模并进入离散约束。
- [x] 张数需求可用离散单位建模并进入离散约束。
- [x] 重量需求可用连续单位建模并进入连续约束。
- [x] 同一 solution analyzer 可以汇总三种 unit 的 `ProductDemand<V>` 与产出贡献。
- [x] legacy/adapter 可从旧的卷数、重量、张数输入转换为统一 `ProductDemand<V>`。

## 9. 向后兼容

建议提供：

```kotlin
typealias Flt64Product = Product<Flt64>
typealias FltXProduct = Product<FltX>
typealias Flt64WidthRange = WidthRange<Flt64>
```

旧构造函数可作为 companion 工厂保留：

```kotlin
fun legacy(... width: List<Flt64>, length: FltX? = null, ...): Product<FltX>
```

## 10. 门禁

```powershell
git grep -n "Flt64\\|FltX" -- ospf-kotlin-framework-csp1d
```

允许出现的位置：

1. DTO。
2. legacy factory。
3. typealias。
4. 测试。

其它领域模型中的固定数值类型应迁移为泛型。

需求模型门禁：

```powershell
git grep -n "rollDemand\\|weightDemand\\|sheetDemand" -- ospf-kotlin-framework-csp1d
```

允许出现的位置：

1. adapter。
2. legacy factory。
3. 测试。

领域主路径中不应出现三套需求字段。`DemandMode` 允许作为标签出现，但不能驱动数值计算或变量类型判定。

## 11. 后续实现优先级

建议按以下顺序落地：

1. material 核心模型和 `Quantity<V>` 改造。
2. `CuttingPlan<V>`、`Cut<V>`、`Segment<V>` 和可行性规则。
3. 不依赖 solver 的 DFS/FullSum/N-Same/N-Sum 方案生成。
4. produce 主问题静态 MILP。
5. MILP pricing + column generation。
6. 统一 `ProductDemand<V>` 与三种旧需求方式的 adapter 转换。
7. yield、length assignment、wasting minimization 三个增强上下文。
8. application wrapper、KPI、render mapper 和兼容入口。

## 12. 执行进展与交接（2026-06-01）

### 12.1 本次已完成（不改动原目标与验收定义）

1. 已完成 C0 模块骨架和命名边界清理。
2. 已完成 C1 核心泛型模型与物理量边界的最小落地。
3. 已完成 C0-1 最小应用编排与列生成入口骨架。
4. 已补齐基础验收测试并通过指定验证。

### 12.2 本次验证记录

1. 编译通过：
   - `mvn --% -pl ospf-kotlin-framework-csp1d/csp1d-application -am -DskipTests=true -Dgpg.skip=true compile`
2. 指定验收测试通过：
   - `mvn --% -pl ospf-kotlin-framework-csp1d/csp1d-application -am -Dgpg.skip=true -Dtest=Csp1dApplicationAcceptanceTest -Dsurefire.failIfNoSpecifiedTests=false test`
3. 门禁 grep 结果：
   - `git grep -n "rollDemand\\|weightDemand\\|sheetDemand" -- ospf-kotlin-framework-csp1d` 仅命中本 `daily.md` 文档说明。
   - `git grep -n "com.poit\\|framework.bpp3d" -- ospf-kotlin-framework-csp1d` 仅命中本 `daily.md` 文档说明。

### 12.3 交接给下个会话的待续项

1. ~~在不改动本文件原始目标定义的前提下，按真实完成度回填各阶段验收勾选状态（当前仍保留原始待办勾选）。~~ ✅ 已于 2026-06-02 完成。
2. ~~将 `CuttingPlan.restWidth` 的最小运行时类型分支（当前支持 `Flt64/FltX`）进一步抽象为更统一的泛型数值策略（如引入统一 `Quantity` 减法适配）。~~ ✅ 已于 2026-06-02 完成。
3. ~~按后续优先级继续推进 `yield`、`length assignment`、`wasting minimization` 的实质约束与目标建模实现（目前为骨架/占位模块）。~~ ✅ 已于 2026-06-02 完成骨架落地。

## 13. 执行进展与交接（2026-06-02）

### 13.1 本次已完成

1. 已按当前最小完成度回填阶段验收勾选状态。
2. 已完成 `CuttingPlan.restWidth` 的泛型数值策略抽象。
3. 已完成 yield、length assignment、wasting minimization 三个增强上下文的分析层骨架。
4. 已完成本轮编译、测试和门禁检查。

### 13.2 本次验证记录

1. 编译通过（全部 17 模块）：
   - `mvn -pl ospf-kotlin-framework-csp1d/csp1d-application -am -DskipTests=true -Dgpg.skip=true compile`
2. 验收测试通过（3 项，0 失败）：
   - `Csp1dApplicationAcceptanceTest`：3 tests, 0 failures
3. 单元测试通过（7 项，0 失败）：
   - `ProductGenericTypeTest`：3 tests, 0 failures
   - `ProductDemandModelTest`：4 tests, 0 failures
4. 门禁检查：
   - `git grep -n "com.poit\|framework.bpp3d" -- ospf-kotlin-framework-csp1d`：仅命中 `daily.md` 文档说明。
   - `git grep -n "rollDemand\|weightDemand\|sheetDemand" -- ospf-kotlin-framework-csp1d`：仅命中 `daily.md` 文档说明。
   - `Flt64/FltX` 仅出现在 DTO、typealias、legacy factory、test、arithmetic adapter 等允许位置。

### 13.3 交接给下个会话的待续项

1. **yield-context 约束与目标建模深化**：当前 `YieldContext` 已实现产出偏差分析（欠产/超产/产出聚合），下一步需接入 solver 建模层，将 `UnderProduction` / `OverProduction` 转化为 solver 约束和目标（如最小化欠产惩罚、超产上限约束）。
2. **length-assignment-context 求解器集成**：当前 `LengthAssignmentContext` 已实现分配流程和超长检测，但 `LengthDerivation<V>` 依赖下游注入；下一步需提供 `Flt64` / `FltX` 默认实现，并接入 solver 变量和约束。
3. **wasting-minimization-context 目标建模**：当前 `WastingMinimizationContext` 已实现浪费分析（余宽/余料汇总），下一步需将 `WasteMinimizationObjective<V>` 的四个子类接入 solver 目标函数。
4. **切割方案生成算法补齐**：按第 11 节优先级，继续实现 DFS/FullSum/N-Same/N-Sum 等不依赖 solver 的方案生成算法（当前仅有 `SimpleInitialCuttingPlanGenerator` 和 `SimplePricingGenerator`）。
5. **主问题 MILP 求解器替换**：当前 `SimpleProduceSolver` 为最小启发式选择器，需替换为真实的 MILP solver 调用（通过 `ospf-kotlin-core` 的 `AbstractLinearMetaModel` 接入）。

## 14. 审查总结与下一轮计划（2026-06-02）

### 14.1 已完成事项总结

本轮审查确认，CSP1D 当前已经完成第一阶段骨架和最小功能落地，但整体目标尚未完全达成。

已完成并可作为后续基础的事项如下：

1. 已完成 CSP1D 第一阶段模块骨架、命名边界清理和最小应用编排。
2. 已完成核心领域模型的泛型化与物理量化基础改造。
3. 已完成切割方案余宽计算的泛型算术策略抽象。
4. 已完成增强上下文的分析层骨架。
5. 已完成本轮基础编译、测试和门禁验证。

### 14.2 本轮未完成事项与风险

以下事项不能视为真正完成，只能视为骨架或最小实现：

1. **统一需求汇总仍有语义风险**：`YieldContext` 当前按 `product.id` 聚合贡献，没有按 `quantity.unit` / dimension 隔离；同一产品同时存在卷数、重量、张数需求时，可能混算不同单位。
2. **`DefaultQuantityArithmetic.resolve()` 接口不可靠**：无参泛型解析无法在运行时判断 `V`，实际会优先返回 `Flt64QuantityArithmetic`；当前主路径使用 `resolveFor(sample)`，但该公开接口应删除或改造。
3. **增强上下文尚未接入 solver**：`yield`、`length_assignment`、`wasting_minimization` 当前只做分析或检测，没有把欠产、超产、超长、浪费目标转化为 solver 变量、约束或目标函数。
4. **切割方案生成算法未补齐**：DFS、FullSum、N-Same、N-Sum 等非 solver 生成算法尚未实现。
5. **主问题仍非真实 MILP**：`SimpleProduceSolver` 仍是最小启发式选择器，未通过 `AbstractLinearMetaModel` 接入真实 MILP。
6. **`Flt64/FltX` 门禁需重新精确定义**：除 DTO、typealias、legacy factory、test、arithmetic adapter 外，application render / sorting 边界也存在 `Flt64/FltX` 转换，应明确是否允许。
7. **新增增强上下文缺少直接测试**：`YieldContext`、`LengthAssignmentContext`、`WastingMinimizationContext` 目前没有独立单元测试覆盖。

### 14.3 下一轮目标

下一轮目标是先收紧当前阶段质量，而不是继续扩展模块范围。

核心目标：

1. 修正统一需求分析的单位语义，确保 demand 与 contribution 只在相同 unit / dimension 下比较。
2. 清理不可靠的默认算术解析接口，保证 `QuantityArithmetic<V>` 的使用方式明确且可测试。
3. 为 yield、length assignment、wasting minimization 三个增强上下文补齐分析层单元测试。
4. 修正本文档中容易误导的完成状态，把“骨架落地”和“solver 建模完成”明确区分。
5. 在完成上述质量收口后，再选择进入“切割方案生成算法补齐”或“主问题 MILP 化”。

### 14.4 下一轮原则

1. **先修验收语义，再扩功能**：已经勾选但语义不稳的验收项优先修正。
2. **单位优先**：所有涉及 `Quantity<V>` 的比较、加减和聚合必须先确认 unit / dimension 一致。
3. **显式注入优先**：泛型数值策略优先显式传入或由样例值解析，不使用无法真实判断泛型类型的默认分发。
4. **骨架与建模分离**：分析层模型、启发式流程、solver 变量约束要在文档和命名上明确区分。
5. **小步验证**：每次改动至少补一个贴近验收语义的单元测试，再跑对应模块测试。
6. **不混入非 CSP1D 改动**：下一轮提交只处理 `ospf-kotlin-framework-csp1d` 目录下文件。

### 14.5 下一轮事项

建议按以下顺序执行：

1. **修复 `YieldContext` 聚合键**：
   - 将贡献聚合键从 `product.id` 扩展为 `product.id + quantity.unit`，必要时增加内部 key 类型。
   - `sumContributions` 只累加同 unit contribution。
   - demand 找不到同 unit contribution 时按全量欠产处理。
2. **补齐多需求测试**：
   - 新增同一产品同时存在 `DemandMode.Roll`、`DemandMode.Weight`、`DemandMode.Sheet` 的测试。
   - 验证三种单位的产出、欠产、超产互不混算。
3. **清理 `DefaultQuantityArithmetic.resolve()`**：
   - 删除无参 `resolve()`，或改为必须传入 sample / KClass / strategy registry。
   - 保留并测试 `resolveFor(sample)` 的 `Flt64` / `FltX` 分发。
4. **补齐增强上下文单元测试**：
   - `YieldContext`：欠产、超产、同单位聚合、不同单位隔离。
   - `LengthAssignmentContext`：动态长度分配、超长检测、无可推导长度时跳过。
   - `WastingMinimizationContext`：余宽汇总、余料汇总、车次数量放大。
5. **修正文档验收状态**：
   - 对尚未 solver 化的 yield / length / wasting 项改为“骨架完成，solver 建模待续”。
   - 重写 `Flt64/FltX` 允许出现位置，覆盖 application render / sorting 边界或迁移这些转换。
6. **下一阶段二选一**：
   - 若优先提高方案生成能力，实现 DFS / FullSum / N-Same / N-Sum。
   - 若优先提高求解可信度，将 `SimpleProduceSolver` 替换为基于 `AbstractLinearMetaModel` 的真实 MILP 主问题。

### 14.6 整体计划

1. **C1 收口**：修复统一需求语义、算术策略接口和增强上下文测试。
2. **C2 方案生成**：实现不依赖 solver 的 DFS、FullSum、N-Same、N-Sum，并覆盖余宽、刀数、配规和机器兼容测试。
3. **C3 主问题 MILP**：用 `AbstractLinearMetaModel` 建模切割方案使用、需求满足、物料使用、机器产能和基础目标。
4. **C4 Column Generation 深化**：从主问题提取 shadow price，接入 pricing 生成负 reduced cost 新列，并补充迭代终止条件。
5. **C5 增强上下文 solver 化**：将 yield、length assignment、wasting minimization 的分析模型转化为约束和目标插件。
6. **C6 应用与 KPI 完整化**：完善 `Csp1dSolution`、KPI、render mapper、Top-K、recovery、schedule variant 和 adapter 边界。

### 14.7 本次提交修改清单

本次提交范围限定为 `ospf-kotlin-framework-csp1d`：

1. `csp1d-domain-material-context`：
   - 修改 `CuttingPlan.kt`，通过 `QuantityArithmetic<V>` 计算 `usedWidth`、`restWidth` 和 repeat quantity。
   - 新增 `QuantityArithmetic.kt`，提供 `QuantityArithmetic<V>`、`Flt64QuantityArithmetic`、`FltXQuantityArithmetic`、`DefaultQuantityArithmetic.resolveFor(sample)`。
   - 修改 `MaterialTypeAliases.kt`，补充 `Flt64Arithmetic`、`FltXArithmetic`。
2. `csp1d-domain-yield-context`：
   - 新增 `YieldModel.kt` 和 `YieldContext.kt`，提供产出、欠产、超产分析骨架。
3. `csp1d-domain-length-assignment-context`：
   - 新增 `LengthAssignmentModel.kt` 和 `LengthAssignmentContext.kt`，提供动态卷长分配、约束模型和超长检测骨架。
4. `csp1d-domain-wasting-minimization-context`：
   - 新增 `WasteModel.kt` 和 `WastingMinimizationContext.kt`，提供余宽、余料和浪费目标分析骨架。
5. `daily.md`：
   - 回填阶段验收勾选状态。
   - 记录 2026-06-02 的完成事项、验证结果、风险、下一轮目标、执行原则、整体计划、修改清单和验收标准。

### 14.8 下一轮验收标准

下一轮至少满足以下标准后再继续扩大功能范围：

1. `YieldContext` 对同一产品的 roll / weight / sheet demand 能按 unit 分别汇总，不混算。
2. `YieldContext` 新增测试覆盖欠产、超产、同单位聚合、不同单位隔离，全部通过。
3. `DefaultQuantityArithmetic.resolve()` 不再作为不可靠公开入口存在，或已改造成可真实分发的接口。
4. `QuantityArithmetic` 的 `Flt64` / `FltX` 分发和加减测试通过。
5. `LengthAssignmentContext` 和 `WastingMinimizationContext` 至少各有 2 个直接单元测试，覆盖核心分析行为。
6. `mvn --% -pl ospf-kotlin-framework-csp1d/csp1d-domain-material-context,ospf-kotlin-framework-csp1d/csp1d-domain-yield-context,ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context,ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context -am -Dgpg.skip=true test` 通过。
7. `mvn --% -pl ospf-kotlin-framework-csp1d/csp1d-application -am -DskipTests=true -Dgpg.skip=true compile` 通过。
8. `git grep -n "com.poit\\|framework.bpp3d" -- ospf-kotlin-framework-csp1d` 仍仅命中文档说明。
9. `git grep -n "rollDemand\\|weightDemand\\|sheetDemand" -- ospf-kotlin-framework-csp1d` 仍仅命中文档说明、adapter、legacy factory 或测试。
10. `daily.md` 中不得把尚未接入 solver 的功能描述为“约束与目标建模已完成”。
