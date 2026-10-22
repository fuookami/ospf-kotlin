# 流

[English](README.md) | [框架说明](../README_ch.md)

公共接口：`FlowProblem<V> / FlowContext<V> / FlowAggregation<V>`。

商品单位与显式资源消耗；默认硬供需，有界松弛必须配置。

嵌入现有模型时只注册 context 到 `AbstractLinearMetaModel<Flt64>`；独立运行使用对应 Network application service。领域值采用 `V : RealNumber<V>` 与 `Quantity<V>`，数值转换集中在 solver 边界。context/aggregation 仅属于一个模型；同一不可变问题可创建多个独立 context。注册失败后不重试污染的部分模型。

下游通过已注册表达式及标准 pipeline 追加约束或目标。默认测试使用独立见证和小规模枚举，不依赖原生求解器。场景、单位及证明边界详见框架说明。
