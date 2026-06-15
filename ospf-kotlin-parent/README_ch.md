# ospf-kotlin-parent

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-parent` 是 OSPF Kotlin 各模块共享的 Maven 父 POM。它导入仓库依赖 BOM，配置通用编译与打包插件，并定义仓库级构建默认值。

## 边界

本模块只承担构建父 POM 职责，不承载运行时代码、领域 API 或模块专属行为。

## 职责

| 范围 | 说明 |
| --- | --- |
| 构建默认值 | Kotlin/JVM target、编译参数、源码目录和插件继承 |
| 插件管理 | Kotlin、compiler、surefire、failsafe、source jar、dokka、deploy 和 enforcer 插件 |
| 依赖基线 | 导入 [ospf-kotlin-dependencies](../ospf-kotlin-dependencies/README_ch.md) |

## 使用方

仓库内大多数模块直接继承本父 POM。模块自己的 POM 应只保留局部坐标、打包类型、依赖、profile 和必要的插件例外配置。

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-parent validate
```
