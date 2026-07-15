# ospf-kotlin-starters

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starters` 是 starter artifact 的 Maven 聚合模块。starter 模块为用户提供便捷依赖包，使用户不需要手工挑选每一个 core、framework 和 plugin 模块，就可以引入一套可用的 OSPF Kotlin 栈。

## 边界

starter 模块只应聚合依赖，不应包含领域逻辑、求解编排或业务 API。

## Starter 模块

| 模块 | 用途 |
| --- | --- |
| `ospf-kotlin-starter` | 通用 OSPF Kotlin starter |
| `ospf-kotlin-starter-bpp1d` | 一维装箱 starter |
| `ospf-kotlin-starter-bpp2d` | 二维装箱 starter |
| `ospf-kotlin-starter-bpp3d` | 三维装箱 starter |
| `ospf-kotlin-starter-csp1d` | 一维下料 starter |
| `ospf-kotlin-starter-csp2d` | 二维下料 starter |
| `ospf-kotlin-starter-gantt-scheduling` | 甘特调度 starter |
| `ospf-kotlin-starter-network-scheduling` | 网络调度 starter |

## 使用方式

多数用户应选择与领域匹配的一个 starter，再按需要添加具体求解器插件。

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp1d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters -am -DskipTests package
```
