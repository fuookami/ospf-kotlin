# ospf-kotlin-starter-bpp2d

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter-bpp2d` 是面向二维装箱使用场景的 starter artifact。

## 边界

当前 starter 依赖通用 [ospf-kotlin-starter](../ospf-kotlin-starter/README_ch.md)。BPP2D framework 仍处于早期阶段；如果需要当前实验性 API，可直接依赖 `ospf-kotlin-framework-bpp2d`。

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp2d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp2d -am -DskipTests package
```
