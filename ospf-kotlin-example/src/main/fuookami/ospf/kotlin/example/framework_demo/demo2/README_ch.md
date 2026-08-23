# demo2 — 飞机货物装载优化

:us: [English](README.md) | :cn: 简体中文

## 简介

`demo2` 是一个用于**飞机货物装载优化**的框架示例。它确定如何将货物装入飞机，同时满足适航性、结构和操作约束。三个公开应用模式均支持直接 MILP 和自适应 Benders 分解，并稳定返回响应 DTO 及可选渲染 DTO。

## 作用范围

- 建模飞机物理属性（机身、甲板、燃油、舱门）
- 定义货物项目（位置、装载、预约）
- 施加适航约束（CLIM、包络线、纵/横向平衡）
- 优化 MAC（平均气动弦）以控制重心
- 应用软安全约束（压舱物、空载）
- 在满足结构限制的前提下最大化载荷
- 支持快递货物优先级和装载顺序优化

## 模块结构

| 领域上下文 | 职责 |
| --- | --- |
| `aircraft` | 飞机模型：机身、甲板、燃油、舱门、装载顺序 |
| `stowage` | 货物装载：项目、装载、位置、预约 |
| `mac` | 平均气动弦计算 |
| `airworthiness_security` | 结构限制：CLIM、包络线、区域重量、累积载荷 |
| `soft_security` | 压舱物和空载约束 |
| `mac_optimization` | 重心优化（纵/横向平衡） |
| `payload_maximization` | 最大化货物载荷 |
| `express_effectiveness` | 快递货物优先级排序 |
| `loading_effectiveness` | 装载顺序、拖车管理、顺序装载 |
| `recommended_weight_equalization` | 重量均衡和优先预约 |
| `redundancy` | 冗余约束和实验性平衡 |
| `infrastructure` | 求解器配置、DTO、Benders 策略、诊断 |

## 架构

每个领域上下文遵循 DDD 模式：

```
domain/<context>/
  <Context>Context.kt     -- 上下文类：init(), register(), construct(), analyze()
  Aggregation.kt          -- 聚合根：持有领域模型状态
  model/                  -- 领域实体和值对象
  service/
    AggregationInitializer.kt  -- 从输入初始化聚合
    PipelineListGenerator.kt   -- 将约束注册到模型
    limits/                    -- 约束定义（业务规则）
    SolutionAnalyzer.kt        -- 提取该上下文的解
```

## 应用入口

| 文件 | 说明 |
| --- | --- |
| `FullLoadApplication.kt` | 满载优化 |
| `LoadingOrderApplication.kt` | 装载顺序生成 |
| `PredistributionApplication.kt` | 预分配优化 |
| `WeightRecommendationApplication.kt` | 重量推荐 |

## 求解模式与响应

`FullLoadAlgorithm`、`PredistributionApplication` 和 `WeightRecommendationApplication` 接收 `RequestDTO`，并返回 `Pair<ResponseDTO, RenderDTO?>`。

| 请求配置 | 实际路径 | 响应行为 |
| --- | --- | --- |
| `preferBenders = false` | 直接 MILP | 成功响应包含 `solver_path=milp_direct`。 |
| `preferBenders = true` 且达到二元变量阈值 | Gurobi Benders | 成功响应包含 `solver_path=benders`。 |
| Benders 失败且 `bendersFallbackToMilp = true` | 回退到直接 MILP | 成功响应包含 `solver_path=milp_fallback_after_benders`。 |
| Benders 失败且禁用回退 | 无解 | 响应状态为 `BendersFailed`。 |

有解时 `ResponseDTO.status` 为 `Optimal`；预求解发现硬不可行时为 `NoSolution`；B767、B747 或未知机型输入为 `UnsupportedAircraft`；其他结构化失败为 `Error`。`notes` 和 `diagnostics` 会保留求解路径和可行性细节。Benders 仅适用于 B737、B757，且需要可用的 Gurobi 安装和许可证。

## 模式边界

| 模式 | 附加行为 |
| --- | --- |
| `FullLoad` | 注册标准的配载、适航、MAC、软安全、快件和装载效率链。高优先级货物必须装载。 |
| `Predistribution` | 追加冗余和装载顺序行为，不注册 must-ship 和来源提前约束。 |
| `WeightRecommendation` | 追加推荐重量均衡和业载最大化。高优先级货物必须装载；不包含 MAC 优化和软安全目标。 |

领域扩展应落在 context、aggregation、model component、pipeline 或 limit 中。Application 只负责选择求解路径、映射结构化失败，以及组装 `ResponseDTO` 和可选 `RenderDTO`。

## 请求控制与验证 profile

`RequestDTO` 暴露业载比例、相邻载荷差、累积载荷、包络线、目标纵向力矩、纵向偏差和横向不平衡参数。它们在所有支持的模式中都会注册为模型硬约束，而不是只用于诊断。重量推荐的 `balancePriority` 和 `payloadPriority` 会转换到 `Parameter`，并实际改变推荐目标的系数。

普通 `demo2-only` Maven profile 编译 Demo2 应用和迁移测试，不依赖商业求解器。需要许可证的应用级 E2E 隔离在 `demo2-gurobi-e2e` 中；该 profile 只编译 `Demo2ApplicationE2EIntegration`，需要可用的 Gurobi 安装和许可证。直接 MILP 与 Benders 路径共用相同的模式专用 context 和 pipeline 注册边界。

精简版 `RequestDTO` 没有水平安定面查找点或最大配平输入，因此 MAC initializer 保持空的安定面集合，与 Rust 示例一致；不会用人为的配平常量冒充硬约束。

适航 initializer 会创建默认的线密度、面密度区域、请求包络、目标纵向力矩/横向不平衡边界，以及宽体机的 CLIM 点。`MaxUnsymmetricalLinearDensity` 和水平安定面限制只有在显式提供领域数据时才会注册，不会从精简 DTO 人工推导。默认纵向包络只通过 `EnvelopeLimit` 注册一次，目标力矩范围则由独立 limit 提供。

## 用法

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo2.FullLoadAlgorithm
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.RequestDTO

suspend fun main() {
    val (response, render) = FullLoadAlgorithm(
        request = RequestDTO.sample(),
        withRender = true
    )
    if (response.succeed) {
        println(response.assignments)
        println(render?.positions)
    } else {
        println("${response.status}: ${response.notes}")
    }
}
```

## Limit 对齐与容量约束决策

四个与 Rust 对齐的 limit 已注册到对应领域 pipeline：

| Limit | Pipeline | 语义 |
| --- | --- | --- |
| `AdjacentGapLimit` | `airworthiness_security` | 为每一对相邻位置增加载荷间隙绝对值的两个硬不等式。 |
| `MustShipLimit` | `express_effectiveness` | 为每个选定货物增加恰好装载一次的硬等式；仅在 `FullLoad` 和 `WeightRecommendation` 启用。 |
| `PriorityOrderLimit` | `loading_effectiveness` | 为每一对严格高优先级货物增加大 M 硬不等式，其中大 M 为位置数量。 |
| `SourceEarlyLimit` | `loading_effectiveness` | 要求包含多个货物的来源至少有一个货物进入早期位置的硬下界；在 `Predistribution` 中禁用。 |

`LoadWeightLimit` 仍由 `stowage` pipeline 注册，负责每个位置的最大载重约束。不再新增独立的适航性 `CapacityLimit`，以避免职责重复。已对照 `E:/workspace/ospf/ospf-rust/ospf-rust-example/src/framework/demo2` 的模式选择器和派生数据：must-ship 使用优先级 `>= 8`，货物按来源分组，`earlyEnd` 为 `(positionCount - 1) / 2`，大 M 为位置数量。

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pdemo2-only test
```
