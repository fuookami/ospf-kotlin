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
