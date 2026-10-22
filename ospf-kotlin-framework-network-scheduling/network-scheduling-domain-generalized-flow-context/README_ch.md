# 广义流

[English](README.md) | [框架说明](../README_ch.md)

公共 API：`GeneralizedFlowProblem<V>`、`GeneralizedFlowContext<V>` 与 `GeneralizedFlowSolution<V>`。

每条有向服务弧只有一个起点投入量，并在终点产生到达产出量。模型约束为“节点流出投入量 - 流入产出量 = 节点净供给”，刻意不复用普通流守恒。弧容量和资费均按投入量计；先独立执行单位转换，再应用产率/损耗：`产出 = 投入量 × 投入到达转换系数 × 业务产率`。同量纲单位转换可自动推导；跨量纲转换必须显式给出单位为“终点单位 / 起点单位”的比例量。

每条弧的投入量都必须有有限上界。即使损耗/增益环路的产率乘积允许循环流量无限增长，有限弧容量仍会将其限制在有界域内。本版采用硬供需平衡，不涵盖缺货松弛、多输入 BOM 配方、MRP 或排程。

不可变问题与每模型注册态分离。每个 `AbstractLinearMetaModel<Flt64>` 应使用独立 context；失败后复用或跨模型复用都会被拒绝。领域 API 使用 `V : RealNumber<V>` 与 `Quantity<V>`，solver 数值转换集中由 `NetworkSchedulingSolverValueAdapter<V>` 承担。下游业务规则可通过接收已注册投入/产出表达式的 pipeline 工厂扩展。

`GeneralizedFlowSolution` 按弧报告投入量、单位换算后的产出、实际产出、有符号损耗/增益及投入资费；按节点报告声明净供给、流出投入、流入产出、实际净供给和守恒残差。默认测试使用手算见证和小模型检查，不依赖原生求解器。

`GeneralizedFlowContext` 可选接收 `GeneralizedFlowSolverTolerance`，用于结果提取校验。其绝对/相对容差作用于归一化后的 `Flt64` solver 值，而非领域物理量，因此应按 solver adapter 的数值尺度配置。若结果缺失、非有限、投入为负、投入超出容量界且超出容差，或节点平衡残差超出容差，提取会以结构化错误失败；solver 数值无法转换回 `V` 时也会失败。
