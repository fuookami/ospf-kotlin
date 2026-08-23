# ospf-kotlin-starter-network-scheduling

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter-network-scheduling` 是网络调度框架的 starter artifact。它引入全部六个 network-scheduling 模块以及基础 starter：

- `ospf-kotlin-framework-network-scheduling-infrastructure` — 泛型图、弧、流量原语、值适配器
- `ospf-kotlin-framework-network-scheduling-domain-vrp-context` — VRPTW 实例、路线、校验器、成本策略、BranchMask 与 VRP 时间值对象
- `ospf-kotlin-framework-network-scheduling-domain-flow-context` — 单商品/多商品流、容量上下界、流守恒与最小费用目标
- `ospf-kotlin-framework-network-scheduling-domain-route-generation-context` — ESPPRC 定价器、定价图
- `ospf-kotlin-framework-network-scheduling-domain-route-compilation-context` — 列池、列生成生命周期、影子价格
- `ospf-kotlin-framework-network-scheduling-application` — 分支定价算法、应用服务

求解器插件（如 `ospf-kotlin-core-plugin-gurobi`）**不**包含在内，需单独添加。

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-network-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>
```

若需使用 Gurobi 后端的 VRPTW 分支定价求解器，还需添加：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-core-plugin-gurobi</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-network-scheduling -am -DskipTests package
```
