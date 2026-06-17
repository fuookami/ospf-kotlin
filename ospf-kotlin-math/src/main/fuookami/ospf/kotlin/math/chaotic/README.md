# ospf-kotlin-math/chaotic_operator

:us: English | :cn: [简体中文](README_ch.md)

Chaotic system attractors and iterative maps for OSPF Kotlin. Provides 88+ implementations of well-known chaotic systems for simulation, visualization, and research.

All operator classes are genericized as `data class Xxx<V : FloatingNumber<V>>`, supporting any floating-point type. Generator classes are `Flt64`-specific for convenience.

## Available Systems

### 3D Continuous Systems (Differential Equations)

| System                                  | Parameters                                  | Description                                       |
|-----------------------------------------|---------------------------------------------|---------------------------------------------------|
| `LorenzSystem`                          | a, b, c, h                                  | Classic Lorenz attractor (butterfly effect), 1963 |
| `LorenzAttractor`                       | sigma, rho, beta, h                         | Lorenz attractor (physics naming convention)      |
| `LorenzMod1Attractor`                   | alpha, beta, delta, zeta, h                 | Lorenz modified model 1                           |
| `LorenzMod2Attractor`                   | alpha, beta, delta, zeta, h                 | Lorenz modified model 2                           |
| `Lorenz84Model`                         | a, b, f, g, h                               | Lorenz 1984 atmospheric model                     |
| `ChenSystem`                            | a, b, c, h                                  | Chen attractor, dual of Lorenz                    |
| `ChenCelikovskyAttractor`               | alpha, beta, delta, h                       | Generalized Chen system                           |
| `ChenLeeAttractor`                      | alpha, beta, delta, h                       | Chen-Lee variant                                  |
| `CoulletAttractor`                      | alpha, beta, delta, zeta, h                 | Coullet system                                    |
| `BurkeShawAttractor`                    | zeta, nu, h                                 | Burke-Shaw system                                 |
| `BoualiAttractor`                       | alpha, zeta, h, c4, c15, c005               | Bouali system                                     |
| `AizawaAttractor`                       | alpha, beta, gamma, delta, epsilon, zeta, h | Aizawa attractor                                  |
| `AnishchenkoAstakhovAttractor`          | mu, eta, h                                  | Anishchenko-Astakhov system                       |
| `ArneodoAttractor`                      | alpha, beta, delta, h                       | Arneodo attractor                                 |
| `ChuaAttractor`                         | alpha, beta, delta, epsilon, zeta, h        | Chua's circuit attractor                          |
| `ChuasCircuit`                          | a, b, c, d, h                               | Chua's circuit (piecewise-linear)                 |
| `Brusselator`                           | a, b, h                                     | Brusselator chemical model                        |
| `RosslerAttractor`                      | alpha, beta, zeta, h                        | Rossler attractor, 1976                           |
| `ThomasAttractor`                       | beta, h                                     | Thomas attractor (sinusoidal)                     |
| `ThomasCyclicallySymmetricAttractor`    | b, h                                        | Thomas cyclically symmetric variant               |
| `NoseHooverAttractor`                   | alpha, h                                    | Nose-Hoover thermostat                            |
| `HalvorsenAttractor`                    | alpha, h                                    | Halvorsen attractor (rotational symmetry)         |
| `HadleyAttractor`                       | alpha, beta, delta, zeta, h                 | Hadley atmospheric model                          |
| `NewtonLeipnikAttractor`                | alpha, beta, h, c10, c5, c04                | Newton-Leipnik double scroll                      |
| `RucklidgeAttractor`                    | alpha, kappa, h                             | Rucklidge fluid convection                        |
| `ShimizuMoriokaAttractor`               | alpha, beta, h                              | Shimizu-Morioka laser model                       |
| `RabinovichFabrikantEquation`           | a, b, h                                     | Rabinovich-Fabrikant plasma waves                 |
| `DadrasAttractor`                       | gamma, epsilon, zeta, rho, sigma, h         | Dadras attractor                                  |
| `DequanLiAttractor`                     | alpha, beta, delta, epsilon, zeta, rho, h   | Dequan Li attractor                               |
| `FinanceAttractor`                      | alpha, beta, zeta, h                        | Finance market model                              |
| `GenesioTesiAttractor`                  | alpha, beta, delta, h                       | Genesio-Tesi system                               |
| `LiuChenAttractor`                      | alpha, beta, delta, epsilon, zeta, xi, rho, h | Liu-Chen attractor                             |
| `WangSunAttractor`                      | alpha, beta, delta, epsilon, zeta, xi, h    | Wang-Sun attractor                                |
| `SakaryaAttractor`                      | alpha, beta, h                              | Sakarya attractor                                 |
| `YuWangAttractor`                       | alpha, beta, delta, zeta, h                 | Yu-Wang attractor (exponential)                   |
| `WimolBanlueAttractor`                  | alpha, h                                    | Wimol-Banlue attractor (tangent)                  |
| `RayleighBenardAttractor`               | alpha, beta, gamma, h                       | Rayleigh-Benard convection                        |
| `QiChenAttractor`                       | alpha, beta, zeta, h                        | Qi-Chen attractor (enhanced Lorenz)               |
| `LuChenAttractor`                       | alpha, beta, zeta, h                        | Lu-Chen double scroll                             |
| `LuChenSystem`                          | a, b, c, d, h                               | Lu-Chen system (Lorenz-Chen transition)           |
| `DuffingEquation`                       | alpha, beta, gamma, delta, omega, h         | Duffing forced oscillator                         |
| `FourWingAttractor`                     | alpha, beta, delta, zeta, kappa, h          | Four-wing attractor                               |
| `HindmarshRoseModel`                    | a, b, c, d, s, r, xr, i, h                 | Hindmarsh-Rose neuron model                       |
| `BiologyChaoticModel`                   | a, b, c, r                                  | Biological chaotic model                          |
| `CapacitanceEquation`                   | a, b, c, d, e, h                            | Capacitance-based chaotic equation                |
| `CircuitChaotic`                        | a, b, c, d                                  | Electronic circuit chaos                          |
| `ChuaCircuit`                           | a, b, c, d, h                               | Chua's circuit model                              |
| `DoublePendulumSystem`                  | m, l, g, h                                  | Double pendulum                                   |
| `CoupledLorenzAttractor`                | beta, gamma1, gamma2, epsilon, omicron, h   | Coupled Lorenz systems                            |

### 4D Continuous Systems (Hyperchaotic)

| System                                  | Parameters                     | Description                                |
|-----------------------------------------|--------------------------------|--------------------------------------------|
| `QiAttractor`                           | alpha, beta, delta, zeta, h   | Qi hyperchaotic attractor (4D)             |
| `LorenzStenfloAttractor`                | alpha, beta, delta, zeta, h   | Lorenz-Stenflo system (4D)                 |
| `FourScrollHyperChaoticAttractor`       | a, b, c, d, h                 | Four-scroll hyperchaotic attractor (4D)    |

### N-Dimensional Systems

| System                                  | Parameters                     | Description                                |
|-----------------------------------------|--------------------------------|--------------------------------------------|
| `Lorenz96Model`                         | a, h                           | Lorenz 96 N-dimensional atmospheric model  |
| `NBodySystem`                           | m, G, h                        | N-body gravitational system (3D)           |
| `NBodySystemPlane`                      | m, G, h                        | N-body gravitational system (2D)           |
| `LotkaVolterraSystem`                   | a, b, c, d, h                  | Predator-prey model                        |

### 2D Discrete Maps

| System                       | Parameters              | Description                              |
|------------------------------|-------------------------|------------------------------------------|
| `ArnoldsCatMap`              | two                     | Arnold's cat map                         |
| `BakersMap`                  | -                       | Baker's map (Flt64, needs floor)         |
| `BogdanovMap`                | epsilon, kappa, mu      | Bogdanov map                             |
| `CircleMap`                  | alpha, beta             | Circle map                               |
| `ArnoldTongue`               | omega, kappa            | Arnold tongue visualization              |
| `ChebyshevMap`               | a                       | Chebyshev polynomial map                 |
| `GaussMap`                   | mu                      | Gauss map                                |
| `DuffingMap`                 | a, b                    | Duffing map                              |
| `HenonMap`                   | a, b                    | Henon map                                |
| `LoziMap`                    | a, b                    | Lozi map (piecewise-linear Henon)        |
| `TinkerbellMap`              | a, b, c, d              | Tinkerbell map                           |
| `IkedaMap`                   | u, t0, t1               | Ikeda map (optical chaos)                |
| `GingerbreadmanMap`          | one                     | Gingerbreadman map                       |
| `MartinIterate`              | a, b, c                 | Martin iterate                           |
| `NewtonIterate`              | three, four, twoThirds, two | Newton fractal iterate               |
| `KaplanYorkeMap`             | a, fourPi               | Kaplan-Yorke map                         |
| `ZaslavskiiMap`              | epsilon, upsilon, r, mu, twoPi | Zaslavskii map (plasma physics)    |
| `SymplecticMap`              | h                       | Symplectic (area-preserving) map         |
| `KickedRotator`              | k                       | Kicked rotator                           |
| `VanDerPolSystem`            | a, h                    | Van der Pol oscillator (2D)              |
| `ComplexQuadraticPolynomial` | c, d                    | Complex quadratic map (Flt64, org.kotlinmath) |
| `ComplexSquaringMap`         | -                       | Complex squaring map (Flt64, org.kotlinmath)  |

### 1D Maps

| System                       | Parameters              | Description                              |
|------------------------------|-------------------------|------------------------------------------|
| `LogisticMap`                | a                       | Logistic map x → ax(1-x)                 |
| `TentMap`                    | mu                      | Tent map (piecewise-linear)              |
| `SineMap`                    | mu                      | Sine map                                 |
| `ExponentialMap`             | c                       | Exponential map z → exp(z) + c           |
| `SineMap`                    | mu                      | Sine map x → mu*sin(pi*x)                |
| `SinusoidalMap`              | mu                      | Sinusoidal map                           |
| `SingerMap`                  | mu, c786, c2323, c2875, c1330 | Singer map (quartic polynomial)    |
| `GaussIteratedMap`           | a, b                    | Gauss iterated map                       |
| `ChebyshevMap`               | a                       | Chebyshev map                            |
| `DyadicTransformation`       | two, one                | Dyadic (Bernoulli) map x → 2x mod 1     |
| `IntervalExchangeTransformation` | lambda, pi          | Interval exchange transformation         |
| `SinusMap`                   | c23, c2                 | Sinus map x → 2.3*x^(2*sin(pi*x))       |

### Fractal Systems (in `fractal` package)

| System                       | Parameters              | Description                              |
|------------------------------|-------------------------|------------------------------------------|
| `MandelbrotSet`              | c                       | Mandelbrot set iteration z → z² + c      |
| `JuliaSet`                   | c                       | Julia set iteration z → z² + c           |
| `MultiJuliaSet`              | c, n                    | Multi Julia set z → zⁿ + c               |

### Three-Scroll Unified Chaotic System

| System                                        | Parameters                              | Description                    |
|-----------------------------------------------|-----------------------------------------|--------------------------------|
| `ThreeScrollUnifiedChaoticSystemTsucs1Attractor` | alpha, beta, delta, epsilon, zeta, rho, h | TSUCS1 variant              |
| `ThreeScrollUnifiedChaoticSystemTsucs2Attractor` | alpha, beta, delta, zeta, rho, h       | TSUCS2 variant              |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.chaotic.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.point3

// Lorenz attractor (generic)
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
val generator = LorenzSystemGenerator(
    a = Flt64(10.0), b = Flt64(28.0), c = Flt64(8.0 / 3.0), h = Flt64(0.01)
)
val first100 = List(100) { generator() }

// 4D hyperchaotic system
val qi = QiAttractor(alpha = Flt64(30.0), beta = Flt64(10.0), delta = Flt64(10.0), zeta = Flt64(1.0), h = Flt64(0.001))
var state4d = point4(Flt64(1.0), Flt64(1.0), Flt64(1.0), Flt64(1.0))
repeat(1000) { state4d = qi(state4d) }

// 1D map
val logistic = LogisticMap(a = Flt64(3.9))
var x = Flt64(0.5)
repeat(100) { x = logistic(x) }

// N-body simulation
val nbody = NBodySystem(m = listOf(Flt64(1.0), Flt64(1.0), Flt64(1.0)))
var bodyState = listOf(
    Pair(point3(Flt64(-1.0), Flt64(0.0), Flt64(0.0)), point3(Flt64(0.0), Flt64(0.5), Flt64(0.0))),
    Pair(point3(Flt64(1.0), Flt64(0.0), Flt64(0.0)), point3(Flt64(0.0), Flt64(-0.5), Flt64(0.0))),
    Pair(point3(Flt64(0.0), Flt64(1.0), Flt64(0.0)), point3(Flt64(0.5), Flt64(0.0), Flt64(0.0)))
)
repeat(1000) { bodyState = nbody(bodyState) }
```

## Architecture

- **Operator** (stateless): `data class Xxx<V : FloatingNumber<V>>(...) : Extractor<Output, Input>` — pure function, immutable
- **Generator** (stateful): `data class XxxGenerator(...) : Generator<Output>` — holds mutable state, Flt64-specific
- **4D systems** use `Point<Dim4, V>` (added to geometry module)
- **N-dimensional systems** use `List<Flt64>` for variable-length state vectors
- **Non-standard constants** are passed as constructor parameters; `companion object` provides Flt64 defaults

## Related

- [Main README](../../README.md)
- [Fractal Operator Module](../fractal/README.md)
- [Geometry Module](../geometry/README.md)
