# ospf-kotlin-framework-geometry2d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-framework-geometry2d` is the shared two-dimensional geometry kernel of OSPF Kotlin. It is extracted so
that the packing framework [ospf-kotlin-framework-bpp2d](../ospf-kotlin-framework-bpp2d/README.md) and the cutting
stock framework [ospf-kotlin-framework-csp2d](../ospf-kotlin-framework-csp2d/README.md) share one implementation of
quantity-aware 2D geometry instead of duplicating it.

The module is independent of any solver: it depends only on `ospf-kotlin-utils`, `ospf-kotlin-math` and
`ospf-kotlin-quantities`.

## Scope

| Area | Types |
| --- | --- |
| Quantity arithmetic | `quantityPlusSafe`, `quantityMinusSafe`, `quantityMultiply`, safe ordering (`quantityOrdSafe`, `quantityGreater`, ...), `quantityZeroOf`, `quantityOf`, `countFitting`, `scaledQuantity`, `quantitySum`, `sumOfQuantity`, ordering helpers (`maxByQuantityOrNull`, `sortedByQuantity`, ...) and the non-null generic operators (`plus`, `minus`, `times`, `div`, `unaryMinus`) |
| Points and vectors | `QuantityPoint2`, `QuantityVector2`, `point2Of`, `vector2Of` |
| Shapes | `PackingShape2` (sealed) with `RectanglePackingShape2` / `CirclePackingShape2`, `PackingShape2Type`, `Shape2Candidate`, `candidateShapes`, `rectangleShape2`, `circleShape2` |
| Placement and containment | `Placement2`, `Box2Geometry` |
| Sheet geometry | `Sheet2Geometry`, `Sheet2Shape`, `sheet2Shape`, `originBox` |
| Kerf | `inflatedBy`, `pitchOf`, `zeroSpacingOf` |
| Area and bounds extensions | `areaOf`, `boundingWidth`, `boundingHeight`, `boundingArea`, `minX`, `minY`, `maxX`, `maxY`, `insideSheet` |

## Area Semantics

`area` is the actually occupied area and `boundingArea` is the enclosing rectangle area:

1. Rectangle: `area = width × height`, `boundingArea = width × height`.
2. Circle: `area = πr²`, `boundingArea = 4r²`.

Objectives and KPI use `area`; grid generation uses the bounding size.

## Overlap Semantics

Overlap is uniformly **interior overlap**: touching is **not** overlap. Rectangle edge contact, tangent circles and a
circle tangent to a rectangle edge are all disjoint. This is required by grid packing: if tangency counted as overlap,
no regular grid could ever be produced. The underlying `QuantityBox2.overlapped` of the quantity library treats
tangency as overlap, so `Box2Geometry.overlapped` re-implements the dispatch with strict comparisons.

## Kerf

Kerf (saw-blade spacing) is expressed by inflating the shape used in the overlap test:

1. `PackingShape2.inflatedBy(spacing)` grows a rectangle by `spacing` on both axes and a circle by `spacing` in
   diameter.
2. Regular grids use `pitchOf(shape, spacing)`, i.e. bounding size plus spacing, so the real gap between neighbours
   equals the spacing.
3. Guard validation must inflate **both directions**: only an intersection after inflating either side proves that the
   real gap is below the kerf. Inflating one side only misses gaps on the other side.

## Generic Numeric Boundaries

1. Every type is generic over `V : FloatingNumber<V>`.
2. Coordinates, dimensions and areas are `Quantity<V>`; dimensionless values are bare `V`.
3. Generic code must not call `toDouble()`; counting and scaling are expressed through quantity arithmetic
   (`countFitting`, `scaledQuantity`).

## Used By

| Framework | Usage |
| --- | --- |
| `ospf-kotlin-framework-bpp2d` | 2D bin packing of rectangles and circles |
| `ospf-kotlin-framework-csp2d` | 2D cutting stock of rectangles |

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-geometry2d -am test
```
