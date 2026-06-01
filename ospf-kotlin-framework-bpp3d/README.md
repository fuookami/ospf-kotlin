# OSPF Kotlin Framework BPP3D

:us: English | :cn: [简体中文](README_ch.md)

## Scope

`ospf-kotlin-framework-bpp3d` is the BPP3D domain framework in OSPF Kotlin.
This repository currently focuses on:

1. Cuboid packing baseline.
2. Vertical cylinder MVP (`Axis3.Y`) with shape-aware geometry.

## Vertical Cylinder Geometry Semantics

For the current MVP:

1. Only vertical cylinders are supported (`Axis3.Y`).
2. Horizontal/side cylinder placement is rejected explicitly in guarded paths.
3. Bottom overlap/support checks use real footprint geometry (circle-circle, circle-rectangle, rectangle-rectangle).
4. Loading rate in renderer output uses `actualVolume` (not only bounding cuboid volume).

Unsupported or not fully generalized yet:

1. Arbitrary 3D cylinder rotation.
2. Fully shape-generic migration for all legacy cuboid algorithms.
3. Legacy three.js renderer adaptation for new cylinder metadata fields.

See detailed progress in [refactor.md](./refactor.md).

## CSV Input Protocol (Gurobi Dataset)

`GurobiColumnGenerationTest` currently supports two CSV shapes:

1. Grouped layer format (`group_index`, `layer_index` as scenario key).
2. Material-width-amount format (`material`, `width`, `amount` as scenario key).

### Shared Shape Columns

Optional shape metadata columns:

1. `shape_type`: `cuboid` or `vertical_cylinder` (aliases: `vertical-cylinder`, `verticalcylinder`, `cylinder`).
2. `radius_meter`: required when `shape_type` is cylinder.
3. `axis`: optional for cylinder, default `Y`, accepted values: `Y`, `AXIS3.Y`, `X`, `AXIS3.X`, `Z`, `AXIS3.Z`.

Schema guard rules:

1. If `radius_meter` or `axis` column exists, `shape_type` column must exist.
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
