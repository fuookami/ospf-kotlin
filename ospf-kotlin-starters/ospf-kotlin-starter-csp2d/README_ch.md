# ospf-kotlin-starter-csp2d（中文）

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter-csp2d` 是面向二维套裁使用场景的 starter artifact。它引入通用
[ospf-kotlin-starter](../ospf-kotlin-starter/README_ch.md)，以及
[ospf-kotlin-framework-csp2d](../../ospf-kotlin-framework-csp2d/README_ch.md) 的全部模块，
并传递引入共享的 [ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README_ch.md)：

1. `ospf-kotlin-framework-csp2d-infrastructure`
2. `ospf-kotlin-framework-csp2d-domain-material-context`
3. `ospf-kotlin-framework-csp2d-domain-pattern-generation-context`
4. `ospf-kotlin-framework-csp2d-domain-produce-context`
5. `ospf-kotlin-framework-csp2d-domain-yield-context`
6. `ospf-kotlin-framework-csp2d-domain-wasting-minimization-context`
7. `ospf-kotlin-framework-csp2d-domain-sheet-assignment-context`
8. `ospf-kotlin-framework-csp2d-application`

## 边界

本 starter 只负责聚合依赖，不包含任何代码。求解器插件（例如 `ospf-kotlin-core-plugin-gurobi`）**不在**其中，
因为求解器选择属于应用层决策。卷材套裁、定尺板材套裁、三种裁切工艺与切分树都在上述框架模块内。

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp2d</artifactId>
    <version>1.1.1</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-csp2d -am -DskipTests package
```
