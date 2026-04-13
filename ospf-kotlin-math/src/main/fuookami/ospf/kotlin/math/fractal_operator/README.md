# ospf-kotlin-math/fractal_operator

[中文文档 (README_ch.md)](./README_ch.md)

Fractal generation algorithms for OSPF Kotlin.

## Algorithms

| Algorithm | File | Description |
|-----------|------|-------------|
| `MandelbrotSet` | `MandelbrotSet.kt` | Mandelbrot set iteration z -> z^2 + c |
| `MandelbrotSet.Generator` | `MandelbrotSet.kt` | Lazy Mandelbrot sequence generator |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.fractal_operator.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

// Create Mandelbrot set with complex constant c = -0.5 + 0.5i
val mandelbrot = MandelbrotSet(
    c = Point2(Flt64(-0.5), Flt64(0.5))
)

// Iterate from origin
var z = Point2(Flt64.zero, Flt64.zero)
repeat(100) {
    z = mandelbrot(z)
}

// Using Generator for lazy sequence
val generator = MandelbrotSet.Generator(
    c = Point2(Flt64(-0.5), Flt64(0.5)),
    initial = Point2(Flt64.zero, Flt64.zero)
)
val sequence = generator.asSequence().take(50).toList()
```

## Related

- [Main README](../../README.md)
- [Chaotic Operator Module](../chaotic_operator/README.md)
- [Geometry Module](../geometry/README.md)
