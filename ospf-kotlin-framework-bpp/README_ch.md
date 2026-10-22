# 共享 BPP 离散装载

[English](README.md) | 简体中文

## 公共契约

`DiscreteLoadingProblem<V>` 显式提供货物/舱位 ID、合法组合、容量、固定装载、固定占用、
非传递冲突边及可选的恰选一种布局组。`DiscreteLoadingContext<V>` 只为合法组合注册变量，
通过 aggregation 按领域 ID 提供分配、件数、重量及装载量表达式。

同舱位可以装多件。实际货物、固定占用及任何严格正的附加重量都需要整体占位。
`linkAdditionalWeight` 使用舱位真实有限容量联动，不引入正数阈值。
允许无货预留；连续重量严格正性与二元变量的双向等价不是闭合 MILP 可行集。

经验证的团覆盖保留所有边，不做传递闭包。一个大舱位排斥多个小舱位，不代表这些小舱位互斥。
无显式布局组时允许无冲突区域混搭；共享舱位可以属于多个布局。
BPP1D/BPP2D 各自提供几何适配且互不依赖。贴边不重叠，物理空间 ID 隔离不同甲板。

## 求解与扩展

`DiscreteLoadingApplicationService<V>` 注入 MILP 执行器、可选 Benders 后端和完整领域模型工厂。
下游在工厂中注册 extra context/pipeline，追加目标、约束及变量，再返回 `RegisteredDiscreteLoading`。
附加重量提取参与独立复核。航空规则和商业求解器不进入公共内核。
回退必须通过同一工厂创建全新的变量、上下文与完整模型，禁止复用污染状态。

基础 framework 中的 `LinearBendersAlgorithm` 负责共享迭代。
自动 `LinearBendersPartition` 展开函数辅助变量，把所有整数及所有目标变量留在主问题，
子问题仅检查连续可行性，不添加虚构追索成本。此保守划分保持等价，但不承诺快于直接 MILP。
手工模型可提供真实连续追索目标，主问题必须包含对应 theta。

扩展可通过 `RegisteredDiscreteLoading.bendersStages` 声明 `LinearBendersStage.Master`
或 `ContinuousRecourse`。所有展开后的整数变量始终进入主问题，声明整数追索会返回输入错误。
显式连续追索目标从主目标中拆分，其变量原生边界必须提供可证明的有限下界，不以任意大常数代替。
目标常数保留在主问题中，只计一次。

值通过各模型自身 token 表和变量身份映射，不假设顺序一致。
最优性割使用最优点锚定截距，包含常数、固定决策直接成本及原生边界贡献；Farkas 割显式消去
追索变量边界，拒绝符号无效或不能分离当前点的证书，绝不随意翻转符号。
后端须返回原始行乘子，Farkas 采用 Gurobi 符号约定：`<=` 非负、`>=` 非正。
割在单次求解内去重，未绑定整数追索会被拒绝。

合并解按完整展开模型和离散契约双重复核，并独立重算完整目标以核对求解器报告。有可行 incumbent 但未收敛时返回 `Feasible`，
无 incumbent 返回结构化失败，不能一律标记 `Optimal`。
总预算在后端调用之间检查，还需为后端设置单次调用时限。
`LinearBendersResult.termination` 保留收敛、预算、停滞及后端非最优终止原因。
非最优连续追索中的可行解经复核后可保留，但不伪造最优性割，也不提升为 `Optimal`。

## 分区装载组合列生成

`PartitionColumnGenerationApplicationService<V>` 是独立于直接 MILP 和线性 Benders 的第三条路径，
BPP1D/BPP2D 的几何 context 均可使用。**不是**单舱位方案列，也不是通用装箱列生成，更不是联合 CG/Benders。

- `LoadingPartition` 显式且唯一地覆盖全部逻辑舱位，不按列表顺序、物理空间名或航空名称推断分区。
- 一列包含分区内的具体分配、整体占位、货物使用向量、件数/实际重量、布局兼容集合、附加局部决策；行资源系数由共享层按当前源模型计算。
  空载配置也必须满足固定装载；跨区货物唯一性、几何冲突及共享布局仍属于主问题。
- `PartitionLoadingContext` 仅适配领域，模型快照/版本由 core `LinearModelState` 提供，列映射/定价由共享 `ColumnProjection` 提供。下游 extra context/pipeline 注册的
  资源、力矩、目标及辅助变量自动参与列系数和对偶定价，不能只在最终整数模型补规则。
- `PartitionDecision` 显式声明预估、推荐重量等局部变量身份及归属；未归属的变量保留为全局主问题变量。
  `PartitionPricingOracle` 可替换局部 MILP 定价；默认 `MilpPartitionPricingOracle` 保留连续预估重量及整数推荐重量。
- 生命周期覆盖 `register`、`addColumns`、`removeColumns`、`refreshShadowPrice`、受限 LP、最终整数主问题和 `extractSolution`。
  每轮重新注册主问题，确保所有业务行、目标和 token 随增删列同步。可行性阶段最小化人工变量，不使用任意大 M。
- 最终可在**已有分配/占位轮廓**内补全下游资源，并将补全结果物化为新的完整列，再求恰选一列的整数主问题。
  这不是完整原始 MILP 回退，也不放开列池之外的分配/占位轮廓；所有原始行及资源整数类型保持有效。
  `columnLimit` 限制根节点列池，资源补全最多额外添加每分区一列。
- 根节点定价收敛不证明整数最优；当前实现没有 branch-and-price 或完整枚举证书，因此成功结果始终为 `Feasible`。
  有限列池找不到整数解只返回未收敛/受限池失败，不声称原问题不可行。连续资源补全也不提供全局整数最优证明。

`PartitionMasterRow`/`PartitionMasterRegistration`/`PartitionPricingRequest` 复用共享模型与分解类型。
`source.state.registerRow(ConstraintId("business:limit"), relation)` 可在已生成列后追加业务行；
下一次主问题重建与定价自动使用同一新行，不修改 application 主流程。
`pricingRequest`、主问题注册、加列及提取现在通过 suspend Ret/Try 验证当前状态；种子定价显式指定 `seed = true`。
对偶刷新必须提供产生该最优 LP 的 registration，缺失行不再默认为零；application 同轮定价后批量加列。
`PartitionMasterRow.sourceName` 保留下游显示名称；显式 ConstraintId 可跨重建绑定，`model-local-*` 仅限模型会话。
列的数值决策及行系数属于 solver adapter 边界；件数和实际货物重量分别保留 `UInt64` 与 `Quantity<V>`。

## 验证

默认测试无需商业许可证；覆盖固定种子扫描/穷举等价、稀疏注册、占位、布局和 Benders 证书及映射。
日志与报告写入系统临时目录。demo2 真实求解测试隔离在 `demo2-gurobi-e2e` profile。
