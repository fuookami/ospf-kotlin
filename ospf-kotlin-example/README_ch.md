# ospf-kotlin-example

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-example` 包含 OSPF Kotlin 建模 API 的可运行示例和兼容性测试。它不是面向发布的库 artifact。

## 作用范围

本模块演示：

1. core 优化模型构建和函数符号。
2. 泛型数值示例。
3. framework 建模模式和 DTO 边界。
4. starter 兼容性检查。
5. 当本地求解器库可用时，运行带求解器的集成示例。

## 结构

| 区域 | 说明 |
| --- | --- |
| `core_demo` | core 模型示例和泛型数值演示 |
| `linear_function` | 线性函数符号的 build-only 与求解示例 |
| `quadratic_function` | 二次函数的 build-only 与求解示例 |
| `framework_demo` | 面向 framework 的示例应用和 DTO |
| `business_compat` | 面向业务集成路径的源码兼容性检查 |

## 子模块

| 子模块 | 说明 | 详情 |
| --- | --- | --- |
| [`core_demo`](src/main/fuookami/ospf/kotlin/example/core_demo/README_ch.md) | 核心优化模型构建演示（Demo1-17 + GenericNumberDemo），展示线性/二次模型构建、中间符号和 SCIP 求解，覆盖经典运筹学问题类型 | [README](src/main/fuookami/ospf/kotlin/example/core_demo/README_ch.md) |
| [`framework_demo`](src/main/fuookami/ospf/kotlin/example/framework_demo/README_ch.md) | DDD 风格框架示例：网络路由、飞机货物装载、一维下料（列生成）和甘特排程泛型数量示例 | [README](src/main/fuookami/ospf/kotlin/example/framework_demo/README_ch.md) |
| [`heuristic_demo`](src/main/fuookami/ospf/kotlin/example/heuristic_demo/README_ch.md) | 使用粒子群优化 (PSO) 的启发式求解器演示，配合 `CallBackModel` 和 `LinearMetaModel` | [README](src/main/fuookami/ospf/kotlin/example/heuristic_demo/README_ch.md) |

## 快速开始

使用 `Demo1`（资本投资选择）的最小示例：

```kotlin
import fuookami.ospf.kotlin.example.core_demo.Demo1
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal

suspend fun main() {
    val result = Demo1()
    when (result) {
        is Ok -> println("求解成功")
        is Failed -> println("失败: ${result.error}")
        is Fatal -> println("致命错误: ${result.errors}")
    }
}
```

完整演示列表及其问题类型请参阅 [`core_demo/README_ch.md`](src/main/fuookami/ospf/kotlin/example/core_demo/README_ch.md)。

## Maven Profiles

| Profile | 用途 |
| --- | --- |
| `build-only-function-tests` | 编译并运行线性/二次函数结构 build-only 测试 |
| `core-demo-only` | 编译并运行 core demo 测试 |
| `business-source-compat` | 检查业务源码兼容性 |
| `framework-starter-compat` | 检查 framework starter 兼容性 |
| `solver-integration-tests` | 运行带求解器的示例，需要本地求解器库 |

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
mvn -B -ntp -pl ospf-kotlin-example -Pbuild-only-function-tests test
```

求解器集成测试依赖本机环境，只应在对应求解器库已安装时运行。
