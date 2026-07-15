# Model Package

:us: English | :cn: [简体中文](README_ch.md)

Solving pipeline and shadow price abstractions, providing extensible interfaces for constraint registration, dual information management, and heuristic analysis in domain frameworks.

## Pipeline Hierarchy

```
MetaConstraintGroup
  └── Pipeline<in M : Model<*>>
        ├── register(model: M)           Register constraint group to meta model
        ├── invoke(model: M): Try        Execute pipeline
        ├── infeasibleReasons(iis)       Get infeasible reasons (linear/quadratic)
        │
        ├── CGPipeline<Args, Model, Map>
        │     ├── extractor()            Get shadow price extractor
        │     └── refresh(map, model, dualSolution)  Refresh shadow prices
        │
        └── HAPipeline<in M : Model<*>>
              ├── calculate(model, solution)  Calculate heuristic objective value
              └── check(model, solution)      Check solution validity
```

### Pipeline<M>

Constraint pipeline interface. Implementations register a group of constraints to a `MetaModel` and execute them during solving.

Typical usage:

```kotlin
class DemandPipeline : Pipeline<LinearMetaModel<Flt64>> {
    override fun register(model: LinearMetaModel<Flt64>) {
        // Register constraint group
    }

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        // Add constraints and objectives to model
        return ok
    }
}
```

### CGPipeline<Args, Model, Map>

Column generation pipeline interface. Extends `Pipeline` with shadow price extraction and refresh capabilities for the column generation pricing phase.

- `extractor()` — Returns `ShadowPriceExtractor<Args, Map>`, computing shadow prices via argument.
- `refresh(shadowPriceMap, model, shadowPrices)` — Refreshes shadow price map from dual solution.
- Companion object provides `refreshByKeyAsArgs()` method, extracting shadow prices by key from constraint arguments.

### HAPipeline<M>

Heuristic analysis pipeline interface. Extends `Pipeline` with heuristic objective calculation and solution validity checking.

- `calculate(model, solution)` — Calculates heuristic objective value, returns `Ret<Flt64?>`.
- `check(model, solution)` — Checks whether the solution satisfies heuristic conditions.
- `invoke(model, solution)` — Executes analysis and returns `Ret<Obj>`, where `Obj` contains tag and value.

## Pipeline Lists

Type aliases and batch execution extensions:

| Type Alias | Description |
| --- | --- |
| `PipelineList<M>` | `List<Pipeline<M>>` |
| `CGPipelineList<Args, Model, Map>` | `List<CGPipeline<Args, Model, Map>>` |
| `HAPipelineList<M>` | `List<HAPipeline<M>>` |

`PipelineList<M>.invoke(model: M): Try` registers and executes all pipelines sequentially, short-circuiting on the first failure.

## Shadow Price System

```
ShadowPriceKey(limit: KClass<*>)
  ↓ Shadow price key, identifies constraint source type
ShadowPrice(key: ShadowPriceKey, price: Flt64)
  ↓ Shadow price, key-value pair
AbstractShadowPriceMap<in Args, in M>
  ├── map: Map<ShadowPriceKey, ShadowPrice>    Shadow price map
  ├── invoke(arg: Args): Flt64                 Compute shadow price sum via argument
  ├── get(key): ShadowPrice?                   Get by key
  ├── set(key, value)                          Set by key
  ├── put(price)                               Put shadow price
  ├── putOrAdd(price)                          Put or add shadow price
  ├── put(extractor)                           Register extractor
  ├── remove(key)                              Remove by key
  └── shrink()                                 Shrink (remove zero-value entries)
  ↓
ShadowPriceExtractor<Args, M> = (AbstractShadowPriceMap<Args, M>, Args) -> Flt64
```

### Core Functions

- `extractShadowPrice(shadowPriceMap, pipelineList, model, shadowPrices)` — Extracts shadow prices from a `CGPipelineList`: iterates the pipeline list, refreshes shadow prices one by one, and registers extractors.
- `IntermediateSymbol<*>.refresh(shadowPriceMap, shadowPrices)` — Refreshes shadow prices of intermediate symbols: if the symbol argument is a `ShadowPriceKey`, extracts the value from the dual solution and puts it into the map.

### Usage Pattern

```kotlin
// Define custom shadow price map
class MyShadowPriceMap : AbstractShadowPriceMap<MyArg, MyShadowPriceMap>()

// Extract shadow prices from pipeline list
val ret = extractShadowPrice(
    shadowPriceMap = shadowPriceMap,
    pipelineList = cgPipelines,
    model = metaModel,
    shadowPrices = lpResult.dualSolution
)

// Query shadow price via argument
val price = shadowPriceMap(myArg)
```

## Integration with Domain Frameworks

Domain frameworks (CSP1D, BPP3D, etc.) use the pipeline system as follows:

1. **Constraint Pipeline** — During domain modeling, register constraint groups to `MetaModel` via `Pipeline`.
2. **Column Generation Pipeline** — During CG iterations, extract shadow prices from LP dual solutions via `CGPipeline` to drive the pricing problem.
3. **Heuristic Analysis Pipeline** — During post-solve analysis, evaluate solution quality and heuristic metrics via `HAPipeline`.
4. **Extension Pipeline** — Downstream businesses inject custom constraints and objectives via `extra pipeline` without modifying the framework core.
