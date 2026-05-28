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

- [ ] root `pom.xml` 模块清单与本文件第 4 节一致。
- [ ] 子模块间无反向依赖。
- [ ] `git grep -n "com.poit\\|framework.bpp3d" -- ospf-kotlin-framework-csp1d` 没有领域代码命中。

### Phase C0-1：最小可用一维分切开发包

第一版不用一次性覆盖 `poit/csp1d` 的全部增强能力，但必须能完成标准流程：

1. 下游 DTO 映射为 `Csp1dProblem<V>`。
2. 根据产品、原料、设备和配规生成初始切割方案。
3. 在主问题中选择切割方案车次。
4. 输出 `Csp1dSolution<V>`、KPI 和 render 数据。
5. 支持静态 MILP 和列生成两条入口。

验收：

- [ ] 有一个不依赖 `com.poit` 的 example/test 覆盖“按卷数需求的一维分切”。
- [ ] 有一个 example/test 覆盖“配规 + 余宽 + 机器产能”。
- [ ] `Csp1dColumnGeneration` 可以从主问题 shadow price 调用 pricing 生成新列。

### Phase C1-0：定义数值与量纲策略

先确定 CSP1D 是否需要物理单位：

1. 若只需要统一高精度数值，使用 `V : RealNumber<V>`。
2. 若要表达宽度、长度、重量量纲，使用 `Quantity<V>`。

结论：领域物理量用 `Quantity<V>`，计算过程需要纯系数时使用裸 `V`。

验收：

- [ ] 宽度/长度/重量的单位约定写入 README 或本文件。
- [ ] 提供 Flt64/FltX 默认工厂。

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

- [ ] `Product<Flt64>` 编译通过。
- [ ] `Product<FltX>` 编译通过。
- [ ] `weight(width, length)` 返回单位正确的重量量纲。

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

- [ ] 区间上下界和步长单位一致。
- [ ] Flt64/FltX 两种特化测试通过。

### Phase C1-3：DTO 边界

`RenderDTO` 是序列化边界，不建议直接泛型化为 `RenderDTO<V>`。

改造方式：

1. 保留 `RenderCuttingPlanProductionDTO` 的 `FltX` 字段，作为稳定输出格式。
2. 新增 mapper：`fun <V : RealNumber<V>> Product<V>.toRenderDto(...)`。
3. mapper 内显式执行 `Quantity<V>.toFltX()`。

验收：

- [ ] DTO 输出兼容旧格式。
- [ ] 领域模型不再为了 DTO 固定为 `FltX`。

### Phase C1-4：三种需求方式合并

目标：

1. 定义 `ProductDemand<V>`，用 `Quantity<V>` 表达需求值。
2. 将卷数、重量、张数三种需求计算方式迁移为 adapter 层输入转换，并映射为 `DemandMode` 标签。
3. 定义 `CuttingPlanDemandContribution<V>` 或等价模型，使切割方案产出统一贡献 `Quantity<V>`。
4. 主问题 demand constraint、yield 欠产/超产、solution analyzer 均基于统一 demand/contribution 聚合。
5. 通过 `quantity.unit.domain` 决定建模变量和约束是离散还是连续。

验收：

- [ ] 领域主路径不存在 `rollDemand`、`weightDemand`、`sheetDemand` 三套平行字段。
- [ ] `DemandMode` 只作为 decision/constraint/analyzer/KPI 标签，不参与数值计算。
- [ ] 卷数需求可用离散单位建模并进入离散约束。
- [ ] 张数需求可用离散单位建模并进入离散约束。
- [ ] 重量需求可用连续单位建模并进入连续约束。
- [ ] 同一 solution analyzer 可以汇总三种 unit 的 `ProductDemand<V>` 与产出贡献。
- [ ] legacy/adapter 可从旧的卷数、重量、张数输入转换为统一 `ProductDemand<V>`。

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
