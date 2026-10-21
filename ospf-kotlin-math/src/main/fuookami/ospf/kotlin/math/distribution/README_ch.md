# distribution — 概率分布采样

:us: [English](README.md) | :cn: 简体中文

## 简介

元启发式插件所用的连续概率分布集合。每个分布都通过**注入的熵源**采样（`Generator<Flt64>`，产出 `[0, 1)` 上的均匀值），因此随机性可替换：伪随机生成器、混沌序列或固定种子都能驱动同一套分布。

参数校验与采样均返回 `Ret`，全程不抛异常。`validate()` 报告参数问题，`sample()` 在参数已校验的前提下执行数值计算，`invoke()` 先校验后采样。

## 包结构

```
distribution/
├── Distribution.kt   # Distribution 接口、均匀熵工具、Ret 组合子
├── Uniform.kt        # Uniform(a, b)
├── Gaussian.kt       # Gaussian(mean, std) + Box–Muller 的 standardGaussian()
├── Cauchy.kt         # Cauchy(location, scale)
├── Exponential.kt    # Exponential(rate)
├── Gamma.kt          # Gamma(shape, scale)，Marsaglia–Tsang 方法
├── Beta.kt           # Beta(alpha, beta)，由两个伽马分布之比得到
├── StudentT.kt       # StudentT(degreesOfFreedom)
├── Levy.kt           # 对称莱维稳定分布，Chambers–Mallows–Stuck 方法
├── Truncated.kt      # Truncated(distribution, lower, upper)，拒绝采样
└── Direction.kt      # DirectionSampler(dimension) → 单位向量
```

## 用法

```kotlin
import fuookami.ospf.kotlin.math.distribution.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.utils.functional.*

val entropy = Generator { kotlin.random.Random.nextFlt64() }

when (val sampled = Gaussian(mean = Flt64.zero, standardDeviation = Flt64.one)(entropy)) {
    is Ok -> println(sampled.value)
    else -> println("采样失败: $sampled")
}

// 莱维飞行步长 = 重尾幅值 × 单位方向
val magnitude = Levy(Flt64(1.5))(entropy)
val direction = DirectionSampler(30)(entropy)
```

## 设计要点

- **熵源注入** —— 没有隐藏的全局随机源；固定种子即可让整个搜索可复现。
- **不抛异常** —— 非法参数返回 `Failed(ErrorCode.IllegalArgument, ...)`。
- **可组合** —— `Truncated` 可包装任意分布；`Levy` 与 `DirectionSampler` 组合即构成莱维飞行步长。
- **数值保护** —— 端点 `0` 与 `1` 会被夹紧，避免 `ln(0)` 与除零路径。

## 相关

- [主 README](../../../../../../../README_ch.md)
- [混沌模块](../chaotic/README_ch.md)
- [分形模块](../fractal/README_ch.md)
