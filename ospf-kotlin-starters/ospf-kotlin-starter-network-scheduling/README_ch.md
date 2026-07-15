# ospf-kotlin-starter-network-scheduling

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter-network-scheduling` 是预留给网络调度使用场景的 starter artifact。

## 边界

当前 starter 依赖通用 [ospf-kotlin-starter](../ospf-kotlin-starter/README_ch.md)。network scheduling framework 模块仍处于规划阶段，因此本 starter 暂未额外加入独立的网络调度运行时依赖。

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-network-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-network-scheduling -am -DskipTests package
```
