# ospf-kotlin-dependencies

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-dependencies` 是 OSPF Kotlin 仓库的依赖管理 BOM 模块。它集中维护第三方依赖版本和内部模块坐标，使下游 Maven 模块继承统一的依赖图。

## 边界

本模块只承担构建与发布职责，不包含 Kotlin 源码、运行时 public API 或领域建模逻辑。

## 职责

| 范围 | 说明 |
| --- | --- |
| 第三方版本 | Kotlin、kotlinx、日志、序列化、数据库、求解器和工具库依赖 |
| 内部坐标 | OSPF Kotlin core、framework、plugin、领域框架、starter 和 example artifact |
| 依赖收敛 | 为继承 `ospf-kotlin-parent` 的模块提供统一 dependency-management 基线 |

## 相关模块

- [ospf-kotlin-parent](../ospf-kotlin-parent/README_ch.md)
- [根项目 README](../README_ch.md)

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-dependencies validate
```
