# Demo 5：VRPTW 分支定价

[English](README.md)

本示例将 network-scheduling framework 接到可选择的 Gurobi 或 SCIP 列生成求解器，支持 Demo17 和 Solomon 风格输入、明确的时间与节点上限，并返回 framework 求解状态及路线级时间、负载、距离和成本信息。

运行 SCIP 集成 profile：

```powershell
mvn -B -ntp -pl ospf-kotlin-example -am -Pdemo5-scip-bp test -T 0.75C
```

SCIP profile 需要仓库约定的 `jscip` artifact，并通过 `JSCIP_HOME` 或 `SCIP_HOME` 提供本地运行库。
