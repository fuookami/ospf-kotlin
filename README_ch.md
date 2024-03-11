# ospf-kotlin

[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fuookami.ospf.kotlin/ospf-kotlin)](https://mvnrepository.com/artifact/io.github.fuookami.ospf.kotlin/ospf-kotlin)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-blue.svg?logo=kotlin)](http://kotlinlang.org)

## 介绍

ospf-kotlin 是 ospf 的 Kotlin 实现版本，更详细的介绍与文档可以参考主仓库与文档页面：

ospf：https://github.com/fuookami/ospf

文档：https://fuookami.github.io/ospf/

:us: [English](README.md) | :cn: 简体中文

## 安装

ospf-kotlin 已经发布到 maven 中心仓库，因此，如果你使用 maven 的话，只需要在 pom.xml 文件里面添加一个依赖即可：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter</artifactId>
    <version>1.0.3</version>
</dependency>
```

如果你需要使用 bpp3d 开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp3d</artifactId>
    <version>1.0.3</version>
</dependency>
```

如果你需要使用甘特图排程开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-gantt-scheduling</artifactId>
    <version>1.0.3</version>
</dependency>
```

## 开源协议

ospf-kotlin 是根据 Apache 许可证 2.0 版本的条款许可的。

更多信息请查看 [LICENSE](LICENSE)。
