# ospf-kotlin-math/multiarray

:us: English | :cn: [简体中文](README_ch.md)

Multi-dimensional array operations including fast summation and Einstein summation (einsum) for OSPF Kotlin.

## Features

### Fast Sum Operations (`FastSum.kt`)

| Operation | Description |
|-----------|-------------|
| `sumAll` | Sum all elements in the array |
| `sumAxis` | Sum along a single axis |
| `sumAxes` | Sum along multiple axes |
| `cumsumAxis` | Cumulative sum along an axis |

### MultiArray Extensions (`MultiArrayExtensions.kt`)

Extension functions for the `AbstractMultiArray` type providing convenient access to fast sum operations with proper zero handling.

### Einstein Summation (`einsum/`)

Powerful tensor operations using Einstein notation for concise expression of contractions, matrix multiplications, and more.

## Usage

```kotlin
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

// Create a 2D array
val arr = MultiArray.newWith(Shape2(3, 4), Flt64(1.0))

// Sum all elements
val total = arr.sumAll(Flt64.zero)

// Sum along axis 0
val sumAxis0 = arr.sumAxis(0, Flt64.zero)  // Shape: [4]

// Sum along axis 1
val sumAxis1 = arr.sumAxis(1, Flt64.zero)  // Shape: [3]

// Cumulative sum along axis 1
val cumsum = arr.cumsumAxis(1, Flt64.zero)
```

### Einstein Summation

```kotlin
import fuookami.ospf.kotlin.multiarray.einsum.*

// Matrix multiplication
val c = einsumDouble(a, b, "ij,jk->ik")

// Tensor contraction
val contracted = einsumDouble(tensor, "ijk->ij")

// Outer product
val outer = einsumDouble(v1, v2, "i,j->ij")

// Batch matrix multiplication
val batchC = einsumDouble(batchA, batchB, "bij,bjk->bik")
```

## Related

- [Main README](../../README.md)
- [Functional Module](../utils/functional/README.md)
- [Algebra Module](../math/algebra/README.md)

### Contraction Preparation

`contract` decodes non-contracted output coordinates once per output rather than once
per contracted element. Products and additions retain their original order, including
floating-point cancellation and non-finite values. This optimization retains the existing
row-major linear-address interpretation of inputs, even for column-major shapes; it is
not a correction of that pre-existing storage-order limitation.

### Reduction Address Preparation

For built-in Shape1-4/DynShape with canonical public `offsets`, `sumAxisSafe` and
`sumAxesSafe` advance output addresses in source-linear order; `cumsumAxisSafe` uses
the axis stride to locate the previous prefix. This avoids per-element coordinate
arrays while retaining the original left-fold arithmetic and output layout, including
the existing column-major behavior. Custom shapes and externally modified offsets
retain the coordinate/Result path rather than assuming contiguous indexing.
