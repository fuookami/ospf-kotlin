# OSPF Kotlin Framework BPP3D

:us: English | :cn: [简体中文](README_ch.md)

## Scope

`ospf-kotlin-framework-bpp3d` is the BPP3D domain framework in OSPF Kotlin.
This repository currently focuses on:

1. Cuboid packing baseline.
2. Vertical cylinder MVP (`Axis3.Y`) with shape-aware geometry.
3. Guarded horizontal cylinder known-coordinate packing (`Axis3.X` / `Axis3.Z`) when final coordinates are already known.

## Cylinder Geometry Semantics

For the current MVP:

1. Candidate generation, block loading, stacking, and hanging support remain vertical-cylinder-only (`Axis3.Y`) unless a path says otherwise.
2. Final packing conversion/rendering and generic known-coordinate analysis can accept `Axis3.X` / `Axis3.Z` horizontal cylinders only when coordinates are already fixed and `PackingGeometryGuard` validates real axis-aligned cylinder geometry.
3. Horizontal cylinders must be on the bin floor or have full-length cuboid support underneath; unsupported or partially supported horizontal cylinders are rejected.
4. A single `BinLayer` cannot mix multiple cylinder axes; different layers in the same bin may use different axes.
5. Bottom overlap/support checks use real footprint geometry for supported vertical-cylinder paths.
6. Loading rate in renderer output uses `actualVolume` (not only bounding cuboid volume).

Unsupported or not fully generalized yet:

1. Arbitrary 3D cylinder rotation.
2. Fully shape-generic migration for all legacy cuboid algorithms.
3. Renderer source code is not part of this repository; this module emits shape metadata for external renderer validation.

See detailed progress in [refactor.md](./refactor.md).

## Cylinder Axis Support Matrix

| Axis | Meaning | Current status |
| --- | --- | --- |
| `Axis3.Y` | Vertical cylinder; circular footprint on the loading plane. | Supported in guarded vertical-cylinder paths with real footprint checks. |
| `Axis3.X` | Horizontal cylinder along X; circular cross-section on the YZ plane. | Supported only in known-coordinate final packing/rendering paths guarded by real 3D geometry and floor/full-length cuboid support checks; generation/stacking/hanging paths remain unsupported. |
| `Axis3.Z` | Horizontal cylinder along Z; circular cross-section on the XY plane. | Supported only in known-coordinate final packing/rendering paths guarded by real 3D geometry and floor/full-length cuboid support checks; generation/stacking/hanging paths remain unsupported. |

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
5. `axis`: optional for cylinder, default `Y`, accepted values: `Y`, `AXIS3.Y`, `X`, `AXIS3.X`, `Z`, `AXIS3.Z`.

For cylinder rows, at least one of `radius_meter`, `radius_min`, or `diameter_min` must be available. `axis = X` / `Z` can be parsed for metadata and policy validation; known-coordinate final packing/rendering accepts them only after real 3D geometry validation, while candidate generation/support/stacking paths remain guarded.

Dynamic radius/diameter support is discrete: interval columns expand to fixed candidate radii, and circle-packing outputs a concrete radius, concrete placement, and concrete `actualVolume`. Continuous radius optimization is not a production capability yet; keep it as a design/prototype topic outside the default solving path.

### Depth Boundary Layer Policy Columns

Optional scenario-level policy columns:

1. `first_layer_allowed_cylinder_axes`
2. `last_layer_allowed_cylinder_axes`
3. `first_layer_allowed_cuboid_orientations`
4. `last_layer_allowed_cuboid_orientations`

These fields constrain only the first and last depth layers after final placement collection. They are application-level hard validation after final MILP solving, not native MILP constraints and not candidate-generation filters.

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
4. In dataset suite mode, file name can declare scenario kind:
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

## Example: Fixed-Radius Vertical Cylinder Input

```kotlin
val cylinderItem = ActualItem(
    id = "cyl-1",
    name = "cyl-1",
    pack = Package.innerPackage(
        shape = PackageShape(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.2) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(1.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y
            )
        ),
        materials = mapOf(material to UInt64.one)
    ),
    enabledOrientations = listOf(Orientation.Upright),
    batchNo = BatchNo("B-cyl-1"),
    packageAttribute = packageAttribute,
    shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
        radius = infraScalar(0.5) * Meter,
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

External renderer source code is kept outside this repository. Build/type-check or visual validation results for that project should be recorded only after running the external renderer commands and, for visual consistency, opening one of the fixtures or an actual solver output.
