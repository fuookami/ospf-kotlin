# rl — Reinforcement Learning Operators

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Q-learning enters a metaheuristic at three points, and this package keeps them separate:

| Concern | Contract | Why it is separate |
|---------|----------|--------------------|
| What is the situation? | `StateDiscretizer` | A Q-table is finite, the search situation is continuous |
| How good is each choice? | `QTable` | Pure bookkeeping; knows nothing about the algorithm |
| Which choice to make? | `ActionSelector` | Exploration–exploitation is one knob, not a rewrite |
| Was it a good idea? | `Reward` | The only coupling to the concrete algorithm |

`QlearningAgent` wires the four together. It deliberately does **not** know what an action is.

## Two uses, one kernel

| Wrapper | Action is… | Effect |
|---------|-----------|--------|
| `QlearningUpdate` | a candidate `SolutionUpdate` | **Operator selection** — switch rules per generation |
| `QlearningSource` | a candidate `ParameterSource` | **Parameter tuning** — adapt any `ParameterSource` slot |

Both are also `IterationObserver`s, so the skeleton hands them the reward after every round:

```kotlin
// AbstractRunner.run, after archive.update(...)
for (observer in observers) {
    observer.observe(iteration, members, improved, converter)
}
```

## State: diversity × stagnation

`DiversityStagnationDiscretizer` builds a `levels × levels` grid from two axes that need **no
objective value**:

- **diversity** — mean pairwise distance ÷ maximum pairwise distance, in `[0, 1]`
- **stagnation** — `notBetterIteration ÷ iteration`, in `[0, 1]`

Neither axis requires knowing which individual is best, so `ObjValue` never becomes a type parameter
and the discretizer stays a stateless `data class`.

## Correctness notes

The previous Q-learning code in this module had three defects; all three are addressed here and are
covered by tests in `QlearningTest.kt`:

1. **The action row had one element.** `mutableListOf(Flt64(9))` builds `[9.0]`, a single-element
   list, so the table degenerated into a scalar. `QTable(stateAmount, actionAmount)` allocates a
   full row; `QTableTest.shouldAllocateOneEntryPerAction` pins this down.
2. **No state transition.** The temporal-difference target used the maximum of the *current* state.
   `QTable.update` uses `nextState`:
   `Q(s,a) ← Q(s,a) + α·(r + γ·max_a' Q(s',a') − Q(s,a))`.
3. **The table never influenced decisions.** The old code picked parameters from
   `state / 3` and `state % 3` and never read the Q-values. Here the selector reads
   `table.valuesOf(state)` on every round.

Argument validation is explicit: `QTable.update` returns `Ret<Flt64>` and reports out-of-range
indices or hyper-parameters as `Failed`. The agent clamps `alpha` and `gamma` into `[0, 1]` at
construction so the loop itself has no failure branch.

## Example

```kotlin
// Operator selection over two swarm updates.
val solver = ComposedSolvers.qLearning<Flt64, Flt64>(
    size = 30,
    stopping = IterationLimit(UInt64(400)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    candidates = listOf(GreyWolfUpdate(), SineCosineUpdate())
)

// Parameter tuning for a PSO inertia weight.
val inertia = QlearningSource(
    candidates = listOf(ConstantSource(Flt64(0.4)), LinearSource(Flt64(0.9), Flt64(0.4))),
    randomGenerator = Generator { Random.nextFlt64() }
)
```

## Related

- [Operator families](../README.md)
- [Heuristic plugin README](../../../../../README.md)
- `runner/IterationObserver.kt` — the reward channel
