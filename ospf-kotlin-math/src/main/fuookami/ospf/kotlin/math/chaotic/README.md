# ospf-kotlin-math/chaotic_operator

:us: English | :cn: [简体中文](README_ch.md)

Chaotic system attractors and iterative maps for OSPF Kotlin. Provides 27+ implementations of well-known chaotic systems for simulation, visualization, and research.

## Available Systems

### 3D Continuous Systems (Differential Equations)

| System | Parameters | Description |
|--------|-----------|-------------|
| `LorenzSystem` | a, b, c, h | Classic Lorenz attractor (butterfly effect), 1963 |
| `ChenSystem` | a, b, c, h | Chen attractor, dual of Lorenz |
| `ChenCelikovskyAttractor` | alpha, beta, delta, h | Generalized Chen system |
| `ChenLeeAttractor` | alpha, beta, delta, h | Chen-Lee variant |
| `CoulletAttractor` | alpha, beta, delta, zeta, h | Coullet system |
| `BurkeShawAttractor` | zeta, nu, h | Burke-Shaw system |
| `BoualiAttractor` | alpha, zeta, h | Bouali system |
| `AizawaAttractor` | alpha, beta, gamma, delta, epsilon, zeta, h | Aizawa attractor |
| `AnishchenkoAstakhovAttractor` | mu, eta, h | Anishchenko-Astakhov system |
| `ArneodoAttractor` | alpha, beta, delta, h | Arneodo attractor |
| `ChuaAttractor` | alpha, beta, delta, epsilon, zeta, h | Chua's circuit attractor |
| `Brusselator` | a, b, h | Brusselator chemical model |

### 2D/3D Discrete Maps

| System | Parameters | Description |
|--------|-----------|-------------|
| `ArnoldsCatMap` | - | Arnold's cat map |
| `BakersMap` | - | Baker's map |
| `BogdanovMap` | epsilon, kappa, mu | Bogdanov map |
| `CircleMap` | alpha, beta | Circle map |
| `ArnoldTongue` | omega, kappa | Arnold tongue visualization |
| `ChebyshevMap` | a | Chebyshev polynomial map |
| `GaussMap` | mu | Gauss map |
| `ComplexQuadraticPolynomial` | c, d | Complex quadratic map (Mandelbrot/Julia) |
| `ComplexSquaringMap` | - | Complex squaring map |

### Physical and Biological Models

| System | Parameters | Description |
|--------|-----------|-------------|
| `BiologyChaoticModel` | a, b, c, r | Biological chaotic model |
| `CapacitanceEquation` | a, b, c, d, e, h | Capacitance-based chaotic equation |
| `CircuitChaotic` | a, b, c, d | Electronic circuit chaos |
| `ChuaCircuit` | a, b, c, d, h | Chua's circuit model |
| `DoublePendulumSystem` | m, l, g, h | Double pendulum |
| `CoupledLorenzAttractor` | beta, gamma1, gamma2, epsilon, omicron, h | Coupled Lorenz systems |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.chaotic.*
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
- [Fractal Operator Module](../fractal/README.md)
- [Geometry Module](../geometry/README.md)
