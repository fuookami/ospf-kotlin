# ospf-kotlin-math/chaotic_operator

[中文文档 (README_ch.md)](./README_ch.md)

Chaotic system attractors and iterative maps for OSPF Kotlin. Provides 27+ implementations of well-known chaotic systems for simulation, visualization, and research.

## Available Systems

### 3D Continuous Systems (Differential Equations)

| System | Parameters | Description |
|--------|-----------|-------------|
| `LorenzSystem` | a, b, c, h | Classic Lorenz attractor (butterfly effect), 1963 |
| `ChenSystem` | a, b, c, h | Chen attractor, dual of Lorenz |
| `RosslerAttractor` | a, b, c, h | Rossler attractor, simpler than Lorenz |
| `ChenCelikovskyAttractor` | a, b, c, d, h | Generalized Chen system |
| `ChenLeeAttractor` | a, b, c, h | Chen-Lee variant |
| `CoulletAttractor` | a, b, c, h | Coullet system |
| `BurkeShawAttractor` | a, b, c, h | Burke-Shaw system |
| `BoualiAttractor` | a, b, c, d, h | Bouali system |
| `AizawaAttractor` | a, b, c, d, e, f, h | Aizawa attractor |
| `AnishchenkoAstakhovAttractor` | a, b, c, d, h | Anishchenko-Astakhov system |
| `ArneodoAttractor` | a, b, c, d, h | Arneodo attractor |
| `ChuaAttractor` | a, b, c, d, e, h | Chua's circuit attractor |
| `Brusselator` | a, b, h | Brusselator chemical model |

### 2D/3D Discrete Maps

| System | Parameters | Description |
|--------|-----------|-------------|
| `ArnoldsCatMap` | h | Arnold's cat map |
| `BakersMap` | - | Baker's map |
| `BogdanovMap` | a, b, c, d | Bogdanov map |
| `CircleMap` | K, Omega | Circle map |
| `ArnoldTongue` | - | Arnold tongue visualization |
| `ChebyshevMap` | n | Chebyshev polynomial map |
| `GaussMap` | a, q | Gauss map |
| `ComplexQuadraticPolynomial` | c | Complex quadratic map (Mandelbrot/Julia) |
| `ComplexSquaringMap` | - | Complex squaring map |

### Physical and Biological Models

| System | Parameters | Description |
|--------|-----------|-------------|
| `BiologyChaoticModel` | a, b, c, d, h | Biological chaotic model |
| `CapacitanceEquation` | a, b, c, d, h | Capacitance-based chaotic equation |
| `CircuitChaotic` | a, b, c, h | Electronic circuit chaos |
| `ChuaCircuit` | alpha, beta, m0, m1, h | Chua's circuit model |
| `DoublePendulumSystem` | g, l1, l2, m1, m2, h | Double pendulum |
| `CoupledLorenzAttractor` | a, b, c, k, h | Coupled Lorenz systems |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.chaotic_operator.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point3
import fuookami.ospf.kotlin.math.geometry.point3

// Lorenz attractor
val lorenz = LorenzSystem(
    a = Flt64(10.0),
    b = Flt64(28.0),
    c = Flt64(8.0 / 3.0),
    h = Flt64(0.01)
)

// Generate trajectory
var state = point3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
val trajectory = mutableListOf(state)
repeat(10000) {
    state = lorenz(state)
    trajectory.add(state)
}

// Using Generator for lazy iteration
val generator = LorenzSystem.Generator(
    a = Flt64(10.0),
    b = Flt64(28.0),
    c = Flt64(8.0 / 3.0),
    h = Flt64(0.01),
    initial = point3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
)
val first100 = generator.asSequence().take(100).toList()
```

## Related

- [Main README](../../README.md)
- [Fractal Operator Module](../fractal_operator/README.md)
- [Geometry Module](../geometry/README.md)
