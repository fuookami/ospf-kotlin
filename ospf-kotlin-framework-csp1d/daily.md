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
5. QuantityArithmetic 实现（Flt64QuantityArithmetic / FltXQuantityArithmetic / DefaultQuantityArithmetic.resolveFor）。
6. application render mapper / sorting 边界（在 mapper 内显式转换，领域模型不应泄漏）。
7. QuantityArithmetic.zero() 实现内部。

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
5. **主问题 MILP 建模方式修正**：`ProduceSolver` / `SimpleProduceSolver` 是 C0-1 为跑通验收临时加入的占位设计，下一步应删除该领域层 solver 接口，并在 `Csp1dMilp` / `Csp1dColumnGeneration` 的 application 层直接构建 `LinearMetaModel`。

## 14. 当前交接与下一轮计划（2026-06-02）

### 14.1 已完成事项摘要

CSP1D 当前已完成阶段性基础建设，可作为后续继续推进的基础，但 C3 尚未完成验收。

已完成事项仅保留高层摘要：

1. 已完成 CSP1D 基础模块、领域模型和应用入口的第一阶段骨架。
2. 已完成核心物理量、需求口径和泛型算术的基础收口。
3. 已完成增强上下文的分析层基础能力与测试补充。
4. 已完成切割方案生成的基础算法与测试补充。
5. 已开始 application 层主问题 MILP 化改造，但仍需继续收口编译、测试和接口一致性。

### 14.2 下一轮目标

下一轮目标保持为 C3：消除 C0-1 骨架阶段留下的占位 solver 设计，在 application 层完成真实主问题 MILP 编排。

核心目标：

1. 删除或彻底脱离 `ProduceSolver` / `SimpleProduceSolver` 的领域层 solver 设计，不把它泛化成长期抽象。
2. 在 `Csp1dMilp` / `Csp1dColumnGeneration` 的 application 层直接构建 `LinearMetaModel`。
3. 在 application 层注册切割方案使用变量、需求约束、物料/设备约束和基础目标，并调用 `ColumnGenerationSolver.solveMILP()`。
4. 从 solver 输出回填模型解，提取 `Produce`，再经 `DefaultCsp1dSolutionAnalyzer` 组装 `Csp1dSolution`。
5. 保留 C1/C2 已建立的单位语义、泛型算术和方案生成基础，不在 C3 中回退这些边界。

### 14.3 下一轮原则

1. **不新增领域层 solver 接口**：主问题建模属于 application 编排，不再放回 domain。
2. **对齐 BPP3D 模式**：application 层直接建模、注册变量/约束/目标、调用 solver、提取结果。
3. **helper 只留在 application 内部**：若变量、约束、目标有复用需求，只抽 application 内部 helper。
4. **单位语义优先**：需求和贡献仍必须按 product + unit 口径匹配，不允许跨单位聚合。
5. **显式注入 solver**：应用入口应显式依赖 `ColumnGenerationSolver`，测试使用 fake solver。
6. **C3 不混入 C4/C5**：shadow price/pricing 和 yield/length/wasting solver 化等后续内容只保留接口空间，不在本轮扩展。
7. **小步验证**：先保证 application 编译和验收测试，再继续扩展列生成迭代。
8. **只提交 CSP1D 范围**：不触碰 `ospf-kotlin-framework-csp1d` 之外的改动。

### 14.4 本轮未完成事项

1. **C3 尚未验收完成**：application 主问题 MILP 化已有工作区改动，但还没有完整编译和测试通过记录。
2. **application 测试仍需同步**：旧验收测试仍按无参 `Csp1dMilp<Flt64>()` / `Csp1dColumnGeneration<Flt64>()` 调用，需要改成显式注入 fake `ColumnGenerationSolver`。
3. **列生成仍停留在主问题求解**：`solveWithTrace` 可保留追踪结果，但 C4 的 shadow price 提取和 pricing 新列生成尚未实现。
4. **方案生成仍有后续增强**：FullSum、缺陷/分段、unitBatch、单位长度、并行和时间限制仍未完成。
5. **增强上下文尚未 solver 化**：yield、length assignment、wasting minimization 仍是分析层能力，尚未转为主问题约束或目标插件。
6. **验证记录需要重跑**：最近一次完整 `-am` 编译曾到达 application；`Shape1` import 修复后，后续编译被中断，必须由下个会话重新执行。

### 14.5 下一轮详细计划

1. **收口 C3 编译**
   - 检查 `Csp1dAssignment` 是否使用 `fuookami.ospf.kotlin.multiarray.Shape1`。
   - 检查 `Csp1dMilpSolver` 的 imports、`LinearPolynomial` / `LinearInequality` 构造、`solveMILP` 返回处理和 `model.setSolution(...)` 回填。
   - 使用 `mvn --% -pl ospf-kotlin-framework-csp1d/csp1d-application -am -DskipTests=true -Dgpg.skip=true compile` 验证；不要用单模块无 `-am` 命令，因为本地未安装的 CSP1D 子模块会触发远端依赖解析失败。
2. **同步 application 验收测试**
   - 将 `Csp1dApplicationAcceptanceTest` 改为 `runBlocking` 调用 suspend API。
   - 添加固定解 fake `ColumnGenerationSolver`，按 `metaModel.tokens.tokensInSolver.size` 返回 solution。
   - 验证 `Csp1dMilp` 能从初始方案池构建主问题并输出非空 `Produce`。
   - 验证 `Csp1dColumnGeneration.solveWithTrace` 在 C3 阶段返回初始方案池与空 pricing trace，不声称 C4 已完成。
3. **清理占位 solver 设计**
   - 用 `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"` 检查 Kotlin 代码中无残留。
   - 保留 `ProduceInput` 作为主问题输入聚合模型；不要把它变成 solver 接口。
   - 确认 `Csp1dProblem` 不新增 `candidatePlans` 字段，候选方案由 application 调 generator 生成。
4. **稳定结果提取**
   - 用一个 `UIntVariable1("x", Shape1(planCount))` 表达方案使用量。
   - 从 solver solution 回填 token 后读取变量值，构造 `CuttingPlanUsage`、`MaterialUsage`、`MachineCapacityUsage` 和 `unmetDemands`。
   - 对连续/离散需求暂按现有贡献值语义处理，不能跨 unit 聚合。
5. **验证并更新文档**
   - 跑 application 编译和验收测试。
   - 选择性跑 C1/C2 涉及的 domain 测试，防止主问题改造破坏方案生成和聚合语义。
   - 更新 `daily.md` 的 C3 状态，不把未完成的 C4/C5 写成已完成。

### 14.6 下一轮修改清单

建议下个会话重点检查和修改以下文件：

1. `csp1d-application/src/main/.../application/model/Csp1dAssignment.kt`
   - 保持一维整数变量组合建模。
   - 确认变量注册返回 `Try` 并被调用方检查。
2. `csp1d-application/src/main/.../application/service/Csp1dMilpSolver.kt`
   - 完成 application 内部主问题 `LinearMetaModel` 建模。
   - 完成需求、物料、设备约束和目标函数注册。
   - 完成 solver 输出回填和 `Produce` 提取。
3. `csp1d-application/src/main/.../application/service/Csp1dMilp.kt`
   - 由 `Csp1dProblem` 生成初始方案池。
   - 显式注入 `ColumnGenerationSolver`。
   - 通过 analyzer 组装 `Csp1dSolution`。
4. `csp1d-application/src/main/.../application/service/Csp1dColumnGeneration.kt`
   - C3 阶段先复用主问题 MILP。
   - 保留 trace，但 pricing 数量应反映 C4 未接入。
5. `csp1d-application/src/main/.../application/service/Csp1dRecovery.kt` 和 `Csp1dSchedule.kt`
   - 同步 suspend API 和显式 solver 注入。
6. `csp1d-application/src/test/.../Csp1dApplicationAcceptanceTest.kt`
   - 改造旧无参入口测试。
   - 补 fake solver 和 C3 验收断言。
7. `daily.md`
   - 在 C3 通过编译和测试后更新验证记录。

### 14.7 下一轮验收标准

1. `mvn --% -pl ospf-kotlin-framework-csp1d/csp1d-application -am -DskipTests=true -Dgpg.skip=true compile` 通过。
2. `mvn --% -pl ospf-kotlin-framework-csp1d/csp1d-application -am -Dgpg.skip=true -Dtest=Csp1dApplicationAcceptanceTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
3. `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"` 无命中。
4. application 层不再引用 `problem.candidatePlans`。
5. `Csp1dMilp` 与 `Csp1dColumnGeneration` 都通过 application 层 `LinearMetaModel` 建模并调用 `ColumnGenerationSolver.solveMILP()`。
6. solver 输出通过 `model.setSolution(...)` 回填后再从 token 读取变量值。
7. `Csp1dSolution` 统一由 `DefaultCsp1dSolutionAnalyzer` 或等价 analyzer 组装。
8. C3 文档明确区分：主问题 MILP 已完成时才可进入 C4；shadow price/pricing 不得提前标记完成。
9. C1/C2 既有测试保持通过，至少覆盖 quantity arithmetic、yield 聚合和 cutting plan generation 的关键测试。
10. `daily.md` 的已完成事项只保留阶段级摘要，不恢复逐类逐测试的明细罗列。

### 14.8 当前提交说明

本次提交用于把中断会话的 CSP1D 工作区状态和交接文档固化，交给下个会话继续执行。

注意事项：

1. 本次提交包含 C1/C2 已落地改动和 C3 已开始但未验收完成的 application 改造。
2. 最新确认的阻断点是 C3 仍需重新跑完整 `-am` 编译与 application 验收测试。
3. 下个会话应优先完成 C3 编译、测试和 acceptance 同步，再推进 C4 pricing 或 C5 增强上下文 solver 化。
