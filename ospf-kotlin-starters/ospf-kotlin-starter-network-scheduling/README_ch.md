# ospf-kotlin-starter-network-scheduling

[English](README.md)

包含运输分配与枢纽、整条路径二元选择、根树/Steiner 中转与设施归属，以及显式损耗/产率或单输入转换的有限容量广义流。嵌入业务若需要更小依赖面，可直接引用所需 domain，而非完整 starter。有限候选集最优不代表全图最优。

独立 Network 入口：拓扑、流、资源路径、网络设计、显式场景韧性与 VRPTW 分支定价应用。

单位、模型注册、搜索完成度及证明边界见 [Network 框架](../../ospf-kotlin-framework-network-scheduling/README_ch.md)。全局排程主体仍为 Gantt，Network 只贡献适配后的子问题。
