# distribution — Probability Distribution Sampling

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Continuous probability distributions used by the metaheuristic plugin. Every distribution samples through an **injected entropy source** (`Generator<Flt64>` producing uniform values in `[0, 1)`), so randomness stays replaceable: pseudo-random generators, chaotic sequences, or fixed seeds can all drive the same distribution.

Parameter validation and sampling both return `Ret`; nothing throws. `validate()` reports parameter problems, `sample()` performs the numeric work assuming validated parameters, and `invoke()` performs validation followed by sampling.

## Package Structure

```
distribution/
├── Distribution.kt   # Distribution interface, uniform entropy helper, Ret combinators
├── Uniform.kt        # Uniform(a, b)
├── Gaussian.kt       # Gaussian(mean, std) + Box–Muller standardGaussian()
├── Cauchy.kt         # Cauchy(location, scale)
├── Exponential.kt    # Exponential(rate)
├── Gamma.kt          # Gamma(shape, scale) via Marsaglia–Tsang
├── Beta.kt           # Beta(alpha, beta) from two gammas
├── StudentT.kt       # StudentT(degreesOfFreedom)
├── Levy.kt           # Symmetric Lévy stable via Chambers–Mallows–Stuck
├── Truncated.kt      # Truncated(distribution, lower, upper) via rejection sampling
└── Direction.kt      # DirectionSampler(dimension) → unit vector
```

## Usage

```kotlin
import fuookami.ospf.kotlin.math.distribution.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.utils.functional.*

val entropy = Generator { kotlin.random.Random.nextFlt64() }

when (val sampled = Gaussian(mean = Flt64.zero, standardDeviation = Flt64.one)(entropy)) {
    is Ok -> println(sampled.value)
    else -> println("sampling failed: $sampled")
}

// Lévy flight step = heavy-tailed magnitude × unit direction
val magnitude = Levy(Flt64(1.5))(entropy)
val direction = DirectionSampler(30)(entropy)
```

## Design Notes

- **Entropy injection** — no hidden global randomness; a fixed seed makes the whole search reproducible.
- **No exceptions** — invalid parameters return `Failed(ErrorCode.IllegalArgument, ...)`.
- **Composable** — `Truncated` wraps any distribution; `Levy` + `DirectionSampler` together form a Lévy flight step.
- **Numerically guarded** — endpoints `0` and `1` are clamped to avoid `ln(0)` / division-by-zero paths.

## Related

- [Main README](../../../../../../../README.md)
- [Chaotic Module](../chaotic/README.md)
- [Fractal Module](../fractal/README.md)
