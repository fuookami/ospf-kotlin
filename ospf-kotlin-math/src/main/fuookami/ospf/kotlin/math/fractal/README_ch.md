# ospf-kotlin-math/fractal_operator

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 的分形生成算法。

## 算法

| 类 | 文件 | 描述 |
|----|------|------|
| `MandelbrotSet<V>` | `MandelbrotSet.kt` | Mandelbrot 集迭代函数 z -> z^2 + c，泛型参数为 `FloatingNumber<V>` |
| `MandelbrotSetGenerator` | `MandelbrotSet.kt` | Mandelbrot 集序列生成器，实现 `Generator<Point<Dim2, Flt64>>` |
| `JuliaSet<V>` | `JuliaSet.kt` | 固定复数 c 的 Julia 集迭代函数 |
| `MultiJuliaSet` | `JuliaSet.kt` | 使用 z^n + c 的推广 Julia 集 |
| `escapeTime(...)` | `EscapeTime.kt` | z -> z^2 + c 的逃逸时间计数，返回 `Ret<UInt64>` |
| `boxCountingDimension(...)` | `FractalDimension.kt` | 二维点集的盒计数维数，返回 `Ret<Flt64>` |
| `correlationDimension(...)` | `FractalDimension.kt` | 解集的 Grassberger–Procaccia 相关维数，返回 `Ret<Flt64>` |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.fractal.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.point2

// 创建 Mandelbrot 集，复数常量 c = -0.5 + 0.5i
val mandelbrot = MandelbrotSet(Flt64(-0.5), Flt64(0.5))

// 或使用 Point 直接构造
val mandelbrotFromPoint = MandelbrotSet(point2(Flt64(-0.5), Flt64(0.5)))

// 从原点迭代
var z = point2(Flt64.zero, Flt64.zero)
repeat(100) {
    z = mandelbrot(z)
}

// 使用 MandelbrotSetGenerator 进行迭代生成
val generator = MandelbrotSetGenerator(
    real = Flt64(-0.5),
    imag = Flt64(0.5),
    z = point2(Flt64.zero, Flt64.zero)
)
// 反复调用 invoke() 获取序列中的连续点
val points = List(50) { generator.invoke() }
```

## 相关链接

- [主 README](../../README.md)
- [Chaotic Operator 模块](../chaotic/README_ch.md)
- [Geometry 模块](../geometry/README_ch.md)
