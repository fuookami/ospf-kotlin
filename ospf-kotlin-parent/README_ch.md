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

## 测试 jar 发布开关

`maven-jar-plugin` 的 `test-jar` 目标产出 `classifier=tests` 的测试 jar，便于本地 `install`
后跨模块复用测试产物。Maven 中央仓库并不要求该附件（只强制要求 `-sources.jar` 与
`-javadoc.jar`），且仓库内没有任何模块依赖兄弟模块的 `tests.jar`，因此默认不发布。

属性 `ospf.test-jar.phase` 控制该执行的绑定阶段，默认为 `none`：`package` 与 `deploy` 都不会
生成或上传 `tests.jar`，除非显式要求。本地需要该产物时传 `package` 即可；测试的编译与执行
两种情况都不受影响：

```powershell
# 发布：裸跑 deploy 即可，不产出也不上传 tests.jar（默认行为）
mvn -B -ntp deploy

# 本地：额外产出 tests.jar 以便跨模块复用
mvn -B -ntp install "-Dospf.test-jar.phase=package"
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-parent validate
```
