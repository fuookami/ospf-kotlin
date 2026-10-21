# ospf-kotlin-math/fractal_operator

:us: English | :cn: [简体中文](README_ch.md)

Fractal generation algorithms for OSPF Kotlin.

## Algorithms

| Class | File | Description |
|-------|------|-------------|
| `MandelbrotSet<V>` | `MandelbrotSet.kt` | Mandelbrot set iteration function z -> z^2 + c, generic over `FloatingNumber<V>` |
| `MandelbrotSetGenerator` | `MandelbrotSet.kt` | Mandelbrot set sequence generator, implements `Generator<Point<Dim2, Flt64>>` |
| `JuliaSet<V>` | `JuliaSet.kt` | Julia set iteration function for a fixed complex c |
| `MultiJuliaSet` | `JuliaSet.kt` | Generalised Julia set using z^n + c |
| `escapeTime(...)` | `EscapeTime.kt` | Escape-time counting for z -> z^2 + c, returns `Ret<UInt64>` |
| `boxCountingDimension(...)` | `FractalDimension.kt` | Box-counting dimension of a 2D point set, returns `Ret<Flt64>` |
| `correlationDimension(...)` | `FractalDimension.kt` | Grassberger–Procaccia correlation dimension of a solution set, returns `Ret<Flt64>` |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.fractal.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.point2

// Create Mandelbrot set with complex constant c = -0.5 + 0.5i
val mandelbrot = MandelbrotSet(Flt64(-0.5), Flt64(0.5))

// Or using Point directly
val mandelbrotFromPoint = MandelbrotSet(point2(Flt64(-0.5), Flt64(0.5)))

// Iterate from origin
var z = point2(Flt64.zero, Flt64.zero)
repeat(100) {
    z = mandelbrot(z)
}

// Using MandelbrotSetGenerator for iterative generation
val generator = MandelbrotSetGenerator(
    real = Flt64(-0.5),
    imag = Flt64(0.5),
    z = point2(Flt64.zero, Flt64.zero)
)
// Call invoke() repeatedly to get successive points in the sequence
val points = List(50) { generator.invoke() }
```

## Related

- [Main README](../../README.md)
- [Chaotic Operator Module](../chaotic/README.md)
- [Geometry Module](../geometry/README.md)
