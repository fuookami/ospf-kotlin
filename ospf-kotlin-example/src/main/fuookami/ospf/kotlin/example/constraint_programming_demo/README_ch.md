# 约束规划示例

:us: [English](README.md) | :cn: 简体中文

本目录包含仓库中最小的端到端 CP 示例：

- `DirectConstraintProgrammingDemo` 直接构造独立的 `ConstraintProgrammingModel`，使用确定性的 `FakeConstraintProgrammingSolver` 求解。
- `CpDemoContext` 持有 aggregation 和 `ConstraintProgrammingPipeline`，下游只需新增管线即可扩展模型，不需要修改求解引擎。
- `LogicBasedBendersDemo` 将二进制主问题赋值绑定到 CP 子问题，并通过已验证的冲突 no-good cut 收敛。

可运行 smoke test 使用 Fake solver，因此不要求本机安装 SCIP 或商业求解器。生产调用方可以通过同一个 `ConstraintProgrammingSolver` 接口替换为 `ScipConstraintProgrammingSolver`。

公共契约见 [`ospf-kotlin-core`](../../../../../../../../ospf-kotlin-core/README_ch.md) 和 [`ospf-kotlin-framework`](../../../../../../../../ospf-kotlin-framework/README_ch.md)。
