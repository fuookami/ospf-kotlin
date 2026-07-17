# OSPF Kotlin Framework BPP3D

:us: English | :cn: [简体中文](README_ch.md)

## Sub-Modules

| Module | Description |
| --- | --- |
| [bpp3d-infrastructure](bpp3d-infrastructure/README.md) | Foundational geometry primitives, type aliases, and utility functions shared across all sub-modules. |
| [bpp3d-domain-item-context](bpp3d-domain-item-context/README.md) | Item domain context: models for items, packages, materials, bins, layers, patterns, and column-generation model components. |
| [bpp3d-domain-bla-context](bpp3d-domain-bla-context/README.md) | Bottom-Up Left-Justified Assignment (BLA) placement algorithm context. |
| [bpp3d-domain-block-loading-context](bpp3d-domain-block-loading-context/README.md) | Block loading context: block generation algorithms and search strategies for packing candidate construction. |
| [bpp3d-domain-layer-assignment-context](bpp3d-domain-layer-assignment-context/README.md) | Layer assignment context: column-generation model for assigning items to bin layers with constraint pipelines. |
| [bpp3d-domain-layer-generation-context](bpp3d-domain-layer-generation-context/README.md) | Layer generation context: candidate bin layer generation (circle packing, fallback, pile). |
| [bpp3d-domain-packing-context](bpp3d-domain-packing-context/README.md) | Packing context: final packing process, geometry validation, renderer output, and solution assembly. |
| [bpp3d-application](bpp3d-application/README.md) | Application layer: column-generation workflow orchestration combining all domain contexts. |

## Scope

`ospf-kotlin-framework-bpp3d` is the BPP3D domain framework in OSPF Kotlin.
This repository currently focuses on:

1. Cuboid packing baseline.
2. Vertical cylinder MVP (`Axis3.Y`) with shape-aware geometry.
3. Guarded fixed/discrete-radius horizontal cylinder candidates (`Axis3.X` / `Axis3.Z`) through axis-aware circle-packing grids, conservative single/repeated/heterogeneous cuboid supported-stack candidates, verified single/repeated narrow-support hanging candidates, and column generation.
4. Guarded horizontal cylinder known-coordinate packing (`Axis3.X` / `Axis3.Z`) when final coordinates are already known.
5. Conservative 3D stacking/hanging support for horizontal cylinders only when they are on the floor or on cuboid support intervals that cover the full cylinder axis and the bottom support line.

## Column-Generation Extension Points

The application layer supports downstream dynamic constraints without copying the standard RMP/final model assembly:

1. `ColumnGenerationApplicationAlgorithmFactory` can replace algorithm assembly while retaining material-demand preparation and packing orchestration.
2. `ColumnGenerationStandardExecutors.rmpSolver(...)` accepts RMP model and dual-solution extensions. Extensions receive `ColumnGenerationRmpContext`, can register additional model rows before LP solving, and can return typed `additionalShadowPrices` plus audit `info` after dual extraction.
3. `ColumnGenerationStandardExecutors.finalSolver(...)` accepts final-MILP model extensions through `ColumnGenerationFinalModelContext`.
4. `ColumnGenerationAlgorithm` provides fallible `initialColumnsWithResult` and `filterByReducedCostWithResult` hooks, propagates additional shadow prices through `ColumnGenerationState`, and reports failed lifecycle stages through `ColumnGenerationFailureAnalyzer`.
5. `finalSolved` is true only when an actual final solver callback was configured and invoked.

## Cylinder Geometry Semantics

For the current MVP:

1. Fixed-radius and discrete-radius `Axis3.X` / `Axis3.Z` horizontal cylinders can enter the axis-aware circle-packing generated grid path and the conservative cuboid supported-stack/hanging path, including single full-length support, repeated same-shape multi-support intervals, heterogeneous support intervals, and single or repeated narrow cuboid support-line hanging that cover the cylinder axis. Other candidate generation, block loading, coordinate-less hanging, and pile support remain vertical-cylinder-only (`Axis3.Y`) unless a path says otherwise.
2. Final packing conversion/rendering and shape-neutral known-coordinate analysis can accept `Axis3.X` / `Axis3.Z` horizontal cylinders when generated candidates or known coordinates pass `PackingGeometryGuard` real axis-aligned cylinder geometry validation.
3. Horizontal cylinders must be on the bin floor or have cuboid support intervals underneath that cover the full cylinder axis and the bottom support line; unsupported or partially supported horizontal cylinders are rejected in final validation and in the 3D stacking support checker.
4. A single `BinLayer` cannot mix multiple cylinder axes; different layers in the same bin may use different axes.
5. Bottom overlap/support checks use real footprint geometry for supported vertical-cylinder paths.
6. `radiusWeightFunctionKey` can enter production only with a selected-radius result and a concrete selected `radius` that satisfies declared radius/diameter bounds and is carried through final validation and renderer `actualVolume`; interval-only continuous radius variables that qualify for the PWL approximation path (`isPWLRegisterable`) are registered with a continuous `r` variable, radius bound constraints, and a core `UnivariateLinearPiecewiseFunction` symbol via `model.add(pwlSymbol)`. The function symbol should be expanded by the core MetaModel -> MechanismModel lifecycle; BPP3D should not mirror core PWL Big-M logic in application code. They use `rMax` conservative envelope for geometry safety and write solver-selected `r` back with `PWLRadiusSelectionMetadata` diagnostics; other interval-only variables are reported through a gap contract. Fixed and discrete radius candidates remain supported.
7. Loading rate in renderer output uses `actualVolume` (not bounding cuboid volume); `BoundingCuboid` compat mapping has been removed — horizontal cylinders use native `HorizontalCylinderX`/`HorizontalCylinderZ` algorithm shape types.

Explicit non-goals and remaining work:

1. Arbitrary 3D cylinder rotation is not a target.
2. Some cuboid-oriented search algorithms are still being migrated to fully shape-polymorphic implementations.
3. Full solver-native continuous radius optimization is still in progress; the current column-generation model selects concrete generated `BinLayer` columns. Continuous-radius metadata has a solver-variable prototype carried through `ColumnGenerationState`, RMP/final solve info, packing snapshot KPI, and a shared solver registration plan with variable bounds, selected-radius bound validation, and blocked model-registration diagnostics. Interval-only continuous radius variables that qualify for the PWL approximation path are registered into the solver model with continuous `r`, radius bound constraints, and a core `UnivariateLinearPiecewiseFunction` symbol via `model.add(pwlSymbol)`, then expanded by the core mechanism-model lifecycle. They are wired into footprint (via `rMax` conservative envelope), volume (via `PWLRadiusSelectionMetadata.actualVolume`/`pwlVolume`), final MILP selection, and renderer `actualVolume`; other interval-only symbolic radius variables remain unregistered and are reported through the gap contract.
4. Renderer source code is not part of this repository; this module emits shape metadata consumed by the external renderer, which now supports native X/Y/Z cylinders and `actualVolume` display semantics.

See detailed progress in [daily.md](./daily.md).

## Cylinder Axis Support Matrix

| Axis | Meaning | Current status |
| --- | --- | --- |
| `Axis3.Y` | Vertical cylinder; circular footprint on the loading plane. | Supported in guarded vertical-cylinder paths with real footprint checks. |
| `Axis3.X` | Horizontal cylinder along X; circular cross-section on the YZ plane. | Supported for fixed/discrete-radius axis-aware circle-packing grid candidates, conservative generated cuboid supported-stack/hanging candidates with single, repeated narrow-line hanging, repeated same-shape support, or heterogeneous support axis coverage, known-coordinate final packing/rendering paths, and 3D stacking checks with floor/cuboid support coverage; coordinate-less hanging and cuboid-only generated paths remain unsupported. |
| `Axis3.Z` | Horizontal cylinder along Z; circular cross-section on the XY plane. | Supported for fixed/discrete-radius axis-aware circle-packing grid candidates, conservative generated cuboid supported-stack/hanging candidates with single, repeated narrow-line hanging, repeated same-shape support, or heterogeneous support axis coverage, known-coordinate final packing/rendering paths, and 3D stacking checks with floor/cuboid support coverage; coordinate-less hanging and cuboid-only generated paths remain unsupported. |

## Shape Path Support Matrix

The production path labels and unsupported messages are centralized in `CylinderCapabilityPath` / `CylinderShapeContract`.
Candidate-generation, cuboid-only search/merge, support checks, known-coordinate final validation, renderer final validation, and depth-boundary final validation must use that shared contract instead of duplicating path strings. Horizontal-cylinder cuboid support coverage is centralized in infrastructure and reused by generated stacking checks and final packing/rendering geometry guards.
Strict quantity boundary checks reject stale fixed-number aliases, material-packing number aliases, deleted layer/depth-limit compatibility aliases, and deleted compatibility shortcuts so new code uses shape-polymorphic or solver-polymorphic APIs directly.

| Path | Cuboid | `Axis3.Y` cylinder | `Axis3.X` / `Axis3.Z` cylinder |
| --- | --- | --- | --- |
| Explicit final bins / known-coordinate packing | Supported | Supported with real geometry guard | Supported with real geometry guard, floor or cuboid support axis coverage required |
| Shape-aware known-coordinate analysis | Supported; optional depth boundary final validation | Supported with real geometry guard and optional depth boundary final validation | Supported with real geometry guard, floor or cuboid support axis coverage required, and optional depth boundary final validation |
| Default layer placement adapter for generated candidates | Supported | Supported | Supported only for verified axis-aware generated candidates; manual impostors are rejected |
| Layer generation fallback / circle packing / pile | Supported | Supported as vertical-cylinder candidates; pile support is limited to upright `Axis3.Y` cylinders | Circle packing supports fixed/discrete-radius axis-aware horizontal grid candidates and conservative cuboid supported-stack/hanging candidates with single/repeated narrow-line hanging/repeated same-shape/heterogeneous support axis coverage; fallback and pile remain unsupported |
| BLA placement | Supported for current generated layers | Supported only through verified vertical-cylinder generated layers | Supported only through verified axis-aware circle-packing generated layers |
| Simple block generation | Supported | Supported only for upright `Axis3.Y` cylinders | Unsupported |
| DFS / MLHS space splitting | Supported cuboid-only path | Unsupported | Unsupported |
| Stacking / hanging support semantics | Supported cuboid semantics | Limited to upright `Axis3.Y` support checks where explicitly guarded | 3D stacking/hanging supports floor or cuboid support intervals that cover the cylinder axis and bottom support line; coordinate-less hanging, radial partial support without the support line, and bottom-cylinder support remain unsupported |
| Packing program / material packing | Supported and preserves `PackingProgram.shape.shapeSpec` when emitting items | Preserved as item shape metadata; downstream support follows item/generated/final-path guards | Preserved as item shape metadata; only axis-aware generated or known-coordinate final paths may open it |
| Depth boundary policy | Application-level final validation | Application-level final validation | Application-level final validation only after known coordinates exist |

## CSV Input Protocol (Gurobi Dataset)

`GurobiColumnGenerationTest` currently supports two CSV shapes:

1. Grouped layer format (`group_index`, `layer_index` as scenario key).
2. Material-width-amount format (`material`, `width`, `amount` as scenario key).

### Shared Shape Columns

Optional shape metadata columns:

1. `shape_type`: `cuboid` or `vertical_cylinder` (aliases: `vertical-cylinder`, `verticalcylinder`, `cylinder`).
2. `radius_meter`: fixed cylinder radius.
3. `radius_min` / `radius_min_meter`, `radius_max` / `radius_max_meter`, `radius_step` / `radius_step_meter`: dynamic radius interval.
4. `diameter_min` / `diameter_min_meter`, `diameter_max` / `diameter_max_meter`, `diameter_step` / `diameter_step_meter`: dynamic diameter interval.
5. `radius_weight_function_key`: continuous-radius selection key; accepted only when `radius_meter` provides the concrete selected radius, and rejected with discrete radius/diameter steps.
6. `axis`: optional for cylinder, default `Y`, accepted values: `Y`, `AXIS3.Y`, `X`, `AXIS3.X`, `Z`, `AXIS3.Z`.

For cylinder rows, at least one of `radius_meter`, `radius_min`, or `diameter_min` must be available. `axis = X` / `Z` is a production input for fixed/discrete-radius axis-aware circle-packing candidate generation and for known-coordinate final packing/rendering after real 3D geometry validation. In the material-width-amount CSV, `width` is interpreted as the cylinder axis length for X/Z rows. Support paths remain guarded except for the 3D floor/cuboid support coverage checker.

Grouped-layer Gurobi test datasets may use `width_meter`, `height_meter`, and `depth_meter` to express explicit item dimensions, so horizontal cylinder supported-stack and hanging seed layers can validate single, repeated narrow-line, repeated same-shape, or heterogeneous cuboid support coverage without changing the material-width-amount `width` axis-length contract.

Dynamic radius/diameter support is discrete: interval columns expand to fixed candidate radii, and circle-packing outputs a concrete radius, concrete placement, and concrete `actualVolume`. `radiusWeightFunctionKey` is a selected-radius production marker only when a concrete `radius_meter` / `radius` is present and inside declared radius/diameter bounds, and the selected radius is represented by a result contract before production shape emission. Interval-only continuous radius variables have a `ContinuousCylinderRadiusSolverPrototype` in diagnostics, selected-radius prototypes are carried through column-generation solver context, and RMP/final solve info plus packing snapshot KPI expose the shared registration plan and blocked model-registration reason. Interval-only inputs are rejected through `ContinuousCylinderRadiusOptimizationGapReport` unless they qualify for the PWL approximation path (isPWLRegisterable, which requires `radiusWeightFunctionKey` for production writeback), which registers a continuous `r` variable, radius bound constraints, and a core `UnivariateLinearPiecewiseFunction` symbol via `model.add(pwlSymbol)`. The PWL function constraints belong to the core mechanism-model lifecycle rather than application-level model assembly. The PWL path uses `rMax` conservative envelope for geometry safety and writes solver-selected `r` back with `PWLRadiusSelectionMetadata` diagnostics including PWL error, actual volume (`π·r²·h`), PWL volume (`π·q·h`), and envelope validation. The PWL modeling is encapsulated in `ContinuousRadiusModelComponent` (domain-item-context), which exposes `register()`, `extractNativeResults()`, `extractPWLResults()`, `extractPWLResultsList()`, `info()`, and `modelScaleInfo()` as the stable extension point for adding new continuous-radius approximation strategies without modifying the application solver layer.

### Depth Boundary Layer Policy Columns

Optional scenario-level policy columns:

1. `first_layer_allowed_cylinder_axes`
2. `last_layer_allowed_cylinder_axes`
3. `first_layer_allowed_cuboid_orientations`
4. `last_layer_allowed_cuboid_orientations`

These fields constrain only the first and last depth layers after final placement collection. They are application-level hard validation after final MILP solving, or after shape-neutral known-coordinate final bins have been built; they are not native MILP constraints and not candidate-generation filters.

Column semantics:

1. Missing column means no restriction for that boundary/type.
2. Present but empty value is a configuration error, not "unrestricted".
3. Multiple values can be separated by `|` or `;`.
4. Axis values use the same parser as `axis`.
5. Cuboid orientation values use existing `Orientation` labels, such as `Upright`, `Side`, or `Lie`.
6. Values are scenario-level; if the same column has inconsistent values across CSV rows, the loader rejects the file.

Schema guard rules:

1. If shape metadata columns exist, `shape_type` column must exist.
2. If `shape_type` is empty, the row is treated as `cuboid`.
3. Invalid `axis` value for cylinder is rejected explicitly.
4. Invalid depth boundary cylinder-axis or cuboid-orientation policy values are rejected explicitly.
5. Duplicate CSV columns are rejected explicitly.
6. Unsupported CSV columns outside the declared grouped-layer or material-width-amount schema are rejected explicitly.
7. Variable radius/diameter intervals must be discrete. `*_min` + `*_max` without `*_step` is rejected unless `radius_meter` provides a fixed concrete radius.
8. `radius_weight_function_key` requires `radius_meter` as the concrete selected radius, the selected radius must satisfy declared radius/diameter bounds, and it cannot be combined with `radius_step` or `diameter_step`; solver-native interval-only continuous radius variables that are isSolverRegisterable are registered as `RealVar` solver variables with constraint-based bounds; interval-only continuous radius variables that are isPWLRegisterable (but not isSolverRegisterable) are registered via the PWL approximation path with continuous `r`, radius bound constraints, and a core `UnivariateLinearPiecewiseFunction` symbol via `model.add(pwlSymbol)`, together with the `rMax` conservative envelope; production-ready continuous radius variables (with a concrete selected radius and no gaps) are registered as `RealVar` solver variables with constraint-based bounds and a target equality constraint, their solver-selected values are exposed in RMP/final info as `continuous_radius_solver_selected_*` keys, and the renderer adapter applies solver-selected radius to `actualVolume` and `radius`/`diameter` DTO fields when available; PWL path `actualVolume` uses true `π·r²·h` (not approximate `π·q·h`), PWL diagnostics (pwl_volume, pwl_error, pwl_segments, pwl_within_envelope) are written to renderer item info; the registration plan exposes mutual exclusion classification (native, pwl, productionReady, blocked) and PWL diagnostics (segments, maxRelError) in RMP/final info.
9. In dataset suite mode, file name can declare scenario kind:
   `grouped-layer` / `grouped_layer` => grouped layer CSV,
   `material-width-amount` / `material_width_amount` => material-width-amount CSV.
   Declared kind mismatch with header detection is rejected explicitly.

### Grouped Layer CSV

Required columns:

1. `group_index`
2. `layer_index`
3. `item_id`
4. `material_no`
5. `material_name`
6. `material_weight_kg`

Sample files:

1. `bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv`
2. `bpp3d-application/src/test/resources/gurobi/grouped-layer-cylinder-mixed-sample.csv`
3. `bpp3d-application/src/test/resources/gurobi/grouped-layer-depth-boundary-sample.csv`
4. `bpp3d-application/src/test/resources/gurobi/grouped-layer-horizontal-multisupport-sample.csv`
5. `bpp3d-application/src/test/resources/gurobi/grouped-layer-horizontal-z-multisupport-sample.csv`
6. `bpp3d-application/src/test/resources/gurobi/grouped-layer-horizontal-hanging-multisupport-sample.csv`
7. `bpp3d-application/src/test/resources/gurobi/grouped-layer-horizontal-z-hanging-multisupport-sample.csv`
8. `bpp3d-application/src/test/resources/gurobi/grouped-layer-continuous-radius-sample.csv`

### Material-Width-Amount CSV

Required columns:

1. `material`
2. `width`
3. `amount`

Optional columns:

1. `material_no`
2. `material_name`
3. `material_weight_kg`
4. shared shape columns above

Sample file:

1. `bpp3d-application/src/test/resources/gurobi/material-width-amount-cylinder-sample.csv`
2. `bpp3d-application/src/test/resources/gurobi/material-width-amount-dynamic-diameter-sample.csv`
3. `bpp3d-application/src/test/resources/gurobi/material-width-amount-continuous-radius-sample.csv`

## Example: Fixed-Radius Vertical Cylinder Input

```kotlin
val cylinderItem = ActualItem(
    id = "cyl-1",
    name = "cyl-1",
    pack = Package.innerPackage(
        shape = PackageShape(
            width = fltX(1.0) * Meter,
            height = fltX(1.2) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(1.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = fltX(0.5) * Meter,
                axis = Axis3.Y
            )
        ),
        materials = mapOf(material to UInt64.one)
    ),
    enabledOrientations = listOf(Orientation.Upright),
    batchNo = BatchNo("B-cyl-1"),
    packageAttribute = packageAttribute,
    shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
        radius = fltX(0.5) * Meter,
        axis = Axis3.Y
    )
)
```

## Example: Renderer DTO Output for Cylinder

`PackingRendererAdapter` emits cylinder metadata in `RenderLoadingPlanItemDTO`:

```json
{
  "name": "cyl-1",
  "shapeType": "Cylinder",
  "renderShapeType": "Cylinder",
  "algorithmShapeType": "VerticalCylinder",
  "axis": "Y",
  "radius": 0.5,
  "diameter": 1.0,
  "boundingWidth": 1.0,
  "boundingHeight": 1.2,
  "boundingDepth": 1.0,
  "actualVolume": 0.942477796
}
```

Field definitions are in:
`bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`.

A minimal mixed `Cuboid + Axis3.Y Cylinder` renderer fixture is available at:
`bpp3d-infrastructure/src/test/resources/renderer/mixed-shape-renderer-schema.json`.

A renderer coordinate fixture covering `Axis3.X`, `Axis3.Y`, and `Axis3.Z` cylinders is available at:
`bpp3d-infrastructure/src/test/resources/renderer/cylinder-axis-renderer-schema.json`.
It follows the external renderer coordinate guide: `x` / `y` / `z` are the bounding-box minimum corner, X/Z horizontal cylinders use `boundingHeight = diameter`, and `y = 0` means floor contact.

External renderer source code is kept outside this repository. The renderer project has passed `npm run build`, `npx vue-tsc --noEmit`, `cargo check`, and `cargo test` for native X/Y/Z cylinder rendering; visual consistency should still be checked with one of the fixtures or an actual solver output when changing geometry semantics.
