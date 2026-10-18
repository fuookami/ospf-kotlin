# ospf-kotlin-framework-bpp2d

:us: English | :cn: [简体中文](README_ch.md)

## Sub-Modules

| Module | Description |
| --- | --- |
| [bpp2d-infrastructure](bpp2d-infrastructure/README.md) | Quantity geometry, packing shapes (rectangle / circle), sheet shape, placement, renderer DTOs and domain errors shared by all sub-modules. |
| [bpp2d-domain-item-context](bpp2d-domain-item-context/README.md) | Item, sheet, layer, packing program, demand statistics, solver value adapter and the legacy `*Need` compatibility bridge. |
| [bpp2d-domain-bla-context](bpp2d-domain-bla-context/README.md) | Bottom-Up Left-Justified (BLA) placement for rectangles on a sheet plane. |
| [bpp2d-domain-block-loading-context](bpp2d-domain-block-loading-context/README.md) | 2D space model, simple/complex block generators and DFS / multi-row space-splitting searches. |
| [bpp2d-domain-layer-generation-context](bpp2d-domain-layer-generation-context/README.md) | Candidate layer generation: block, space-split, BLA, pattern, circle grid (square / hexagonal) and mixed-shape strategies. |
| [bpp2d-domain-layer-assignment-context](bpp2d-domain-layer-assignment-context/README.md) | Column-generation model: assignment, load, capacity, demand constraint with shadow prices and objectives. |
| [bpp2d-domain-packing-context](bpp2d-domain-packing-context/README.md) | Final packing: geometry guard, packer, material summary and renderer adapter. |
| [bpp2d-application](bpp2d-application/README.md) | Column-generation orchestration, standard executors, application service and KPI analyzer. |

## Scope

`ospf-kotlin-framework-bpp2d` is the two-dimensional rectangular and circular packing framework of OSPF Kotlin. It
currently covers:

1. Rectangle items with optional 90-degree rotation.
2. Circular items, packed by square-grid and hexagonal-grid candidates.
3. Mixed shape layouts (rectangle row plus circle grid) validated pairwise against the underlying geometry.
4. A full column-generation loop: candidate layers → RMP (LP) with duals → **reduced-cost pricing** → column
   addition → final MILP → tail-sheet optimisation → packing → KPI.
5. Real Gurobi integration for the LP / MILP solves, including CSV dataset loading.
6. **Kerf (saw-blade spacing)** enforced through grid pitch and bidirectional guard validation.
7. **Remnant reporting**: guillotine free-space rectangles per sheet.

## Shape Support Matrix

| Path | Rectangle | Circle |
| --- | --- | --- |
| Layer generation: block loading (local / global) | Supported | Skipped (circles have no row-block semantics) |
| Layer generation: DFS / multi-row space splitting | Supported | Skipped |
| Layer generation: BLA (bottom-up left-justified) | Supported | Skipped with an explicit marker |
| Layer generation: pattern | Supported | Skipped |
| Layer generation: circle grid | Not applicable | Supported (square grid and hexagonal grid) |
| Layer generation: mixed shape | Supported | Supported (rectangle row + circle grid, pairwise validated) |
| Geometry guard / packing / renderer | Supported | Supported |
| Area accounting | `w × h` | `πr²` (actual area) / `4r²` (bounding area) |

## Overlap Semantics

Overlap is uniformly **interior overlap**: touching is not overlap. Rectangle edge contact, tangent circles and a
circle tangent to a rectangle edge are all disjoint. This is required by grid packing — treating tangency as overlap
would invalidate every regular grid.

## Kerf, Remnants and Datasets

| Capability | Entry point |
| --- | --- |
| Reduced-cost pricing | `layerReducedCost` / `isImprovingColumn` in `bpp2d-domain-layer-assignment-context`; wired by `ColumnGenerationStandardExecutors.reducedCostFilter()` |
| Tail-sheet optimisation | `TailSheetOptimizer` re-selects the tail column against the residual demand |
| Kerf | `Bpp2dLayerGenerationRequest.kerf` plus `inflatedBy` / `pitchOf` in infrastructure; `PackingGeometryGuard` validates bidirectionally |
| Remnant reporting | `FreeSpaceAnalyzer` produces `Remnant2` rectangles, exposed via `PackingResult.remnants` and KPI `remnantCount` / `remnantArea` |
| CSV datasets | `Bpp2dCsvDatasetLoader` with `grouped-layer` and `material-width-amount` schemas |
| Mixed sheet types | `Bpp2dLayerGenerationRequest.sheets` generates candidates per type; `SheetAreaMinimization` (primary, ≡ minimum waste) + `SheetAmountMinimization` + `SheetTypeCostMinimization` (weighted secondary terms) |

## Public API

| API | Responsibility | Stability |
| --- | --- | --- |
| `PackingShape2<V>` / `RectanglePackingShape2` / `CirclePackingShape2` | Shape abstraction with actual and bounding area. | stable |
| `Sheet2Shape<V>` / `Placement2<V>` / `Box2Geometry<V>` | Sheet geometry, placement and interior-overlap tests. | stable |
| `LayerGenerationContext<V>` | Composite candidate generator with shape dispatch. | stable |
| `LayerAssignmentContext` | Five-stage column-generation model lifecycle. | stable |
| `ColumnGenerationAlgorithm` | Column-generation orchestrator with failure stages. | stable |
| `ColumnGenerationStandardExecutors` | Standard RMP / final MILP / request-builder wiring. | stable |
| `ColumnGenerationApplicationService` | End-to-end entry point producing packing result and KPI. | stable |
| `PackingGeometryGuard` / `Packer` / `PackingRendererAdapter` | Final geometry validation, packing and renderer DTO output. | stable |

## Column-Generation Extension Points

1. `ColumnGenerationAlgorithm` accepts `rmpSolver`, `finalMilpSolver`, `layerRequestBuilder`,
   `initialColumnsWithResult`, `filterByReducedCostWithResult` and `failureAnalyzer`, so downstream code can replace
   assembly without copying the loop.
2. `LayerGenerationContext` takes an explicit generator list; new strategies only need to implement
   `Bpp2dLayerGenerator`.
3. `LayerAssignmentContext` exposes `registerRmp` / `addColumns` / `removeColumns` / `registerFinal` /
   `registerAreaMinimization` / `registerSheetAmountMinimization`.
4. `ColumnGenerationApplicationService` accepts an algorithm factory for full replacement of the assembly.

## Generic Numeric and Physical Quantity Boundaries

1. Domain models in infrastructure, item, BLA, block-loading, layer-generation and packing are generic over
   `V : FloatingNumber<V>`.
2. Lengths, coordinates and areas are `Quantity<V>`; dimensionless values (utilisation, ratios, scores) are plain `V`.
3. `FltX` appears only in `bpp2d-domain-layer-assignment-context` (the solver boundary) and in application wiring.
4. Generic code must not call `toDouble()`: counting and scaling are expressed through quantity arithmetic.

## Gurobi Integration

The Gurobi integration test lives in `bpp2d-application/src/gurobi-test` and only runs under the `gurobi-cg-test`
profile:

```powershell
$lib = "$env:GUROBI_HOME\bin"
mvn -s D:\environment\apache-maven-3.9.12\conf\settings.xml `
    -pl ospf-kotlin-framework-bpp2d/bpp2d-application -am test `
    -Pgurobi-cg-test -Dtest=GurobiColumnGeneration2DTest `
    -Dsurefire.failIfNoSpecifiedTests=false "-DargLine=-Djava.library.path=$lib"
```

Without a Gurobi installation, the profile is simply not activated and the default build is unaffected.

## Local Validation

```powershell
mvn -s D:\environment\apache-maven-3.9.12\conf\settings.xml -f ospf-kotlin-framework-bpp2d/pom.xml test
```

## Shared Geometry Module

The 2D geometry of this framework lives in the shared
[ospf-kotlin-framework-geometry2d](../ospf-kotlin-framework-geometry2d/README.md) module, so that packing (bpp2d) and
cutting stock (csp2d) reuse one implementation:

1. Quantity arithmetic (`QuantityCore`): non-null generic operators, safe ordering, counting and scaling.
2. Points and vectors: `QuantityPoint2` / `QuantityVector2`.
3. Shapes: `PackingShape2` with `RectanglePackingShape2` / `CirclePackingShape2`, plus `Shape2Candidate`.
4. Placement and geometry: `Placement2`, `Box2Geometry`, `Sheet2Geometry`, `Sheet2Shape`.
5. Kerf: `inflatedBy`, `pitchOf`, `zeroSpacingOf`.
6. Interior-overlap semantics: touching is not overlap.

`bpp2d-infrastructure` keeps only bpp2d-specific errors (`Bpp2dValidationError` and friends) and renderer DTOs.
The placement type was unified as `Placement2`; bpp2d code and docs use that name directly.
