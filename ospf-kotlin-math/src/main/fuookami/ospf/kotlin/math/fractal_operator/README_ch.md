# ospf-kotlin-math/fractal_operator

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的分形生成算法。

## 算法

| 算法 | 文件 | 描述 |
|------|------|------|
| `MandelbrotSet` | `MandelbrotSet.kt` | Mandelbrot 集迭代 z -> z^2 + c |
| `MandelbrotSet.Generator` | `MandelbrotSet.kt` | 惰性 Mandelbrot 序列生成器 |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.fractal_operator.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

// 创建 Mandelbrot 集，复数常量 c = -0.5 + 0.5i
val mandelbrot = MandelbrotSet(
    c = Point2(Flt64(-0.5), Flt64(0.5))
)

// 从原点迭代
var z = Point2(Flt64.zero, Flt64.zero)
repeat(100) {
    z = mandelbrot(z)
}

// 使用 Generator 惰性序列
val generator = MandelbrotSet.Generator(
    c = Point2(Flt64(-0.5), Flt64(0.5)),
    initial = Point2(Flt64.zero, Flt64.zero)
)
val sequence = generator.asSequence().take(50).toList()
```

## 相关链接

- [主 README](../../README.md)
- [Chaotic Operator 模块](../chaotic_operator/README_ch.md)
- [Geometry 模块](../geometry/README_ch.md)
