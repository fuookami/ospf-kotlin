# ospf-kotlin-starter-csp1d

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter-csp1d` 是面向 CSP1D framework 栈的 starter artifact。

## 包含模块

本 starter 依赖通用 [ospf-kotlin-starter](../ospf-kotlin-starter/README_ch.md) 以及 CSP1D framework 模块：

| 模块 | 用途 |
| --- | --- |
| `ospf-kotlin-framework-csp1d-infrastructure` | 共享渲染 DTO 和基础设施类型 |
| `ospf-kotlin-framework-csp1d-domain-material-context` | 产品、物料、机器、需求和切割方案 |
| `ospf-kotlin-framework-csp1d-domain-produce-context` | 主问题变量和生产建模 |
| `ospf-kotlin-framework-csp1d-domain-cutting-plan-generation-context` | 切割方案生成与定价 |
| `ospf-kotlin-framework-csp1d-domain-yield-context` | 产量约束与目标 |
| `ospf-kotlin-framework-csp1d-domain-length-assignment-context` | 动态长度分配 |
| `ospf-kotlin-framework-csp1d-domain-wasting-minimization-context` | 损耗分析与惩罚 |
| `ospf-kotlin-framework-csp1d-application` | application 层求解流程 |

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp1d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-csp1d -am -DskipTests package
```
