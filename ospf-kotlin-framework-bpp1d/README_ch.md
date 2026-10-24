# ospf-kotlin-framework-bpp1d

[English](README.md) | 简体中文

## 分层与入口

七个模块分别负责 infrastructure、item、pattern-generation、pattern-assignment、
discrete-loading、packing 和 application。

- `Bpp1dMilp`：基于组合列的普通整数装箱。
- `Bpp1dColumnGeneration`：FFD/BFD 初始列、受限 LP、影子价格、负检验数加列、全新最终 MILP。
  有界组合枚举提供定价候选，不等同于未截断的 branch-and-price 最优性证明。
- `TailBinOptimizer`：保留前部箱体，按剩余库存重排尾箱。全局业务约束通过完整结果接收策略复核，
  不应错误地应用于局部子问题。
- `Bpp1dCsvDatasetLoader`：严格 CSV，物品表头 `id,length,weight,amount`；箱型表头
  `id,length,max_weight,cost,inventory`。必须显式给出单位和数字解析器，库存空值表示不限。
- `PackingContext`：独立复核精确需求、库存、长度、间距和重量，输出放置、尾部自由区间及物料汇总。
- `PackingRendererAdapter`、`PackingResult.kpi(unit)`：渲染适配及保留物理量口径的 KPI。
- `Bpp1dDiscreteLoading.context`：把显式纵向区间接到[共享离散装载内核](../ospf-kotlin-framework-bpp/README_ch.md)。
- 固定舱位还可使用共享 `PartitionColumnGenerationApplicationService`，以显式分区的完整装载组合为列；
  与本模块普通装箱的 `Bpp1dColumnGeneration` 不同，且不与 Benders 联合。受限整数列池成功时仅报告 `Feasible`。

## 领域与扩展契约

`Item<V>`、`BinType<V>`、`Bin<V>`、`Placement<V>`、`PackingPattern<V>`、`PackingPlan<V>`
以 `V : FloatingNumber<V>` 保留领域精度；长度、坐标、重量、间距及余量使用 `Quantity<V>`，
需求和库存使用 `UInt64`。间距只存在于相邻物品间，不额外收取尾间距。

候选通过 `PatternPolicy` 扩展；`PatternModelingExtension` 支持模型注册、增删列刷新、
定价附加项以及最终结果提取。普通 MILP、LP、最终 MILP 共用注册路径，`stage` 明确阶段。
扩展的模型私有状态必须按 context 隔离，不能把 LP 变量带入最终新模型。
`Flt64PackingValueAdapter`、`FltXPackingValueAdapter` 集中处理求解器边界转换。

内置数值 adapter 保留未变历史基础表达式项，仅追加新列。公开 map 没有修改通知，因此每轮仍核验
历史方案件数、变量绑定和成本；历史内容或基础表达式变化时重建，删除列也始终重建。扩展 refresh
保留原顺序和失败行为，其对基础表达式的写入会在下一轮检测到。任意自定义 adapter 可能有副作用，
仍保持原完整刷新阶段和读取行为。两条路径的重建均一次构造项列表，不再反复拼接增长的不可变
多项式。这不代表 addColumns 总成本为常量：输入核验、缓存失效和实际输出系数仍有成本。

`PatternAssignmentContext.reducedCosts(candidates)` 在一次只读定价批次内共享稀疏对偶索引，
保留候选顺序、对偶行累加顺序和扩展贡献；调用期间调用方和扩展不得修改模型或对偶 map。
独立调用仍可使用单项 `reducedCost` 重载；每个新批次使用 `refreshShadowPrice` 提供的当前价格。
内置求解器仅在没有建模扩展时自动使用批量快路径；既有扩展仍保留单项定价及逐候选状态读取行为。

只有候选完备且最终整数求解证明最优，才返回 `Optimal`。候选或节点预算截断时，
`candidateComplete=false`，受限池的整数最优也只返回 `Feasible`；LP 定价收敛本身不证明整数最优。
失败统一使用 `Ret`/`Try`。

## 验证

默认测试不需要商业许可证。命令参见[英文说明](README.md#validation)，完整日志输出到 `%TEMP%`。
