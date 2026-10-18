# ospf-kotlin-framework-geometry2d（中文）

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-framework-geometry2d` 是 OSPF Kotlin 的共享二维几何内核。它被抽取出来，使装箱框架
[ospf-kotlin-framework-bpp2d](../ospf-kotlin-framework-bpp2d/README_ch.md) 与套裁框架
[ospf-kotlin-framework-csp2d](../ospf-kotlin-framework-csp2d/README_ch.md) 复用同一套量纲化二维几何，
而不是各自维护一份。

本模块与求解器无关：只依赖 `ospf-kotlin-utils`、`ospf-kotlin-math` 与 `ospf-kotlin-quantities`。

## 范围

| 领域 | 类型 |
| --- | --- |
| 量值算术 | `quantityPlusSafe`、`quantityMinusSafe`、`quantityMultiply`、安全比较（`quantityOrdSafe`、`quantityGreater` 等）、`quantityZeroOf`、`quantityOf`、`countFitting`、`scaledQuantity`、`quantitySum`、`sumOfQuantity`、排序辅助（`maxByQuantityOrNull`、`sortedByQuantity` 等），以及非空泛型运算符（`plus`、`minus`、`times`、`div`、`unaryMinus`） |
| 点与向量 | `QuantityPoint2`、`QuantityVector2`、`point2Of`、`vector2Of` |
| 形状 | `PackingShape2`（sealed）及 `RectanglePackingShape2` / `CirclePackingShape2`、`PackingShape2Type`、`Shape2Candidate`、`candidateShapes`、`rectangleShape2`、`circleShape2` |
| 放置与包含 | `Placement2`、`Box2Geometry` |
| 板材几何 | `Sheet2Geometry`、`Sheet2Shape`、`sheet2Shape`、`originBox` |
| 刀缝 | `inflatedBy`、`pitchOf`、`zeroSpacingOf` |
| 面积与边界扩展 | `areaOf`、`boundingWidth`、`boundingHeight`、`boundingArea`、`minX`、`minY`、`maxX`、`maxY`、`insideSheet` |

## 面积口径

`area` 是实际占用面积，`boundingArea` 是外接矩形面积：

1. 矩形件：`area = width × height`，`boundingArea = width × height`。
2. 圆形件：`area = πr²`，`boundingArea = 4r²`。

目标函数与 KPI 使用 `area`；网格生成使用包围尺寸。

## 重叠语义

重叠统一为**内部重叠**：相切**不算**重叠。矩形贴边、圆与圆相切、圆与矩形边界相切都视为不重叠。
这是网格排样的前提——若把相切判为重叠，任何规则网格都无法产出。量值库底层的 `QuantityBox2.overlapped`
把相切判为重叠，因此 `Box2Geometry.overlapped` 用严格不等式重新实现了分派。

## 刀缝

刀缝通过**膨胀参与重叠判定的形状**来表达：

1. `PackingShape2.inflatedBy(spacing)`：矩形宽高各加 `spacing`；圆形直径加 `spacing`。
2. 规则网格使用 `pitchOf(shape, spacing)`，即包围尺寸加间距，使相邻件的真实间隙恰为间距。
3. 几何校验必须**双向膨胀**：只有把任一侧膨胀后仍相交，才说明真实间隙小于刀缝。单向膨胀会漏掉另一侧的间隙不足。

## 泛型数值边界

1. 所有类型泛型于 `V : FloatingNumber<V>`。
2. 坐标、尺寸与面积使用 `Quantity<V>`；无量纲值使用裸 `V`。
3. 泛型代码不得调用 `toDouble()`；计数与缩放通过量值运算表达（`countFitting`、`scaledQuantity`）。

## 使用方

| 框架 | 用途 |
| --- | --- |
| `ospf-kotlin-framework-bpp2d` | 矩形与圆形件的二维装箱 |
| `ospf-kotlin-framework-csp2d` | 矩形件的二维套裁 |

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-geometry2d -am test
```
