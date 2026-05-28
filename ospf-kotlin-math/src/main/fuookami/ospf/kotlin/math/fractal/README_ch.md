# ospf-kotlin-math/fractal_operator

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的分形生成算法。

## 算法

| 类 | 文件 | 描述 |
|----|------|------|
| `MandelbrotSet<V>` | `MandelbrotSet.kt` | Mandelbrot 集迭代函数 z -> z^2 + c，泛型参数为 `FloatingNumber<V>` |
| `MandelbrotSetGenerator` | `MandelbrotSet.kt` | Mandelbrot 集序列生成器，实现 `Generator<Point<Dim2, Flt64>>` |

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
