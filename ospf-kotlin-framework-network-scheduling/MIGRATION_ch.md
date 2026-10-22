# Network / Gantt 迁移说明

[English](MIGRATION.md)

1. `NetworkGraph<V>` 改为 `NetworkGraph<N, A>` 与显式 `NetworkGraphAccess`。内置物理量节点/弧工厂保留。平行弧必须提供不同 `NetworkArcId`，不能用端点对充当服务身份。视图保持 ID；反向图访问端点应使用 `graph.from/to`，不是业务载荷端点。
2. 删除 `FlowGraph` 及其持有注册态的重复内核。构造不可变 `FlowProblem`，每模型新建 `FlowContext`。守恒/容量/费用 pipeline 改为小粒度表达式组件，由 `FlowAggregation` 调用，不再自行注册另一套变量。整数步长同时缩放守恒、容量和费用；异量纲共享容量必须显式给用量映射。
3. 资源路径规则适配 `PathProblem`，业务仍掌握状态、转移和终点校验。搜索预算不是业务长度上限。即使结果为空也必须读取完成度与诊断；输出数量限制本身不截断搜索。
4. Demo4 保留 Gantt 编排、航空资源/成本/恢复规则，Network 负责拓扑与标签遍历。VRPTW 保留独立入口并复用相同路径机制。多解时不保证列顺序相同。
5. 列批次区分完整搜索、启发式与预算/中断终态。证据必须匹配模型、对偶、分支和定价范围。不能从返回候选列的 reduced cost 推导全局下界。旧生成器无证据即为启发式；精确定价不自动证明全局整数最优。
6. 设计等级是声明有限弧包络内的总容量；场景降级上界绝对限制建成容量。设施处理能力用显式共享资源行表达。故障节点需求不会自动取消；韧性结论只覆盖给定场景。
7. 领域量保留泛型物理量，solver 报告按既有边界合同使用 `Flt64`。正常不可行/限时无 incumbent 返回结构化结果；错误定义及技术失败返回 `Ret` 失败。

不新增任务、日历或全局时空排程框架。需要 Gantt 桥接时显式引入 `ospf-kotlin-starter-gantt-network`。

## 明确的行为修正

- VRPTW 不再根据单条直达弧的时间窗失败永久排除客户；非 metric 网络或平行服务可能仍有可行替代路径。容量单调排除仍保留。
- VRPTW 和 Demo4 在终点按业务策略重算完整成本，再筛选、排序和复核 reduced cost。非可加 VRPTW 成本默认禁用支配，除非策略明确认证与内置固定费加逐弧成本分解一致。
- Phase II 删除路线列时同步清除增量目标项，避免已删除变量残留在最终编译目标中。
- Gantt 不再把返回候选列的最小 reduced cost 当作证明性下界；未知下界以空值表示，空定价轮允许以未认证状态正常停止。当前启发式编排不提供全局整数最优认证。
- 受设施或通道启停控制的商品流必须具有正的弧容量消耗；不能从零消耗推导有限联动上界时返回错误，不用任意大 M 补救。

## 验证入口

默认测试不需要原生求解器；小图路径使用穷举 oracle，结构选择使用小变量域枚举，场景流分别复核。真实求解必须显式选择 profile，运行时不可用导致的跳过不能算验证通过。

```powershell
mvn -f ospf-kotlin-framework-network-scheduling/pom.xml test -T 0.75C > "$env:TEMP\ospf-network-unit.log" 2>&1
mvn -pl :ospf-kotlin-framework-network-scheduling-application -am -Pscip-integration-tests test -T 0.75C > "$env:TEMP\ospf-network-scip.log" 2>&1
mvn -pl :ospf-kotlin-example -am -Pdemo4-only test -T 0.75C > "$env:TEMP\ospf-demo4-unit.log" 2>&1
mvn -pl :ospf-kotlin-example -am -Pdemo4-gurobi-cg test -T 0.75C > "$env:TEMP\ospf-demo4-gurobi.log" 2>&1
mvn -pl :ospf-kotlin-example -am -Pdemo5-scip-bp test -T 0.75C > "$env:TEMP\ospf-demo5-scip.log" 2>&1
```

Network 父 reactor 的定向命令要求本机已有外部基础模块依赖；从新检出的仓库验证时优先使用根 reactor 的 `-pl ... -am` 命令。SCIP/JSCIP 和 Gurobi 的运行库及许可证由环境提供，默认构建不会下载或捆绑原生运行库。
