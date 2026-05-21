# BPP2D 泛型化计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-bpp2d` 当前只有顶层 `pom.xml`，没有已跟踪 Kotlin 源文件，也没有子模块。

当前没有需要迁移的固定数值实现。后续实现 BPP2D 时，应直接参考 BPP3D 的泛型化经验，但避免复制 BPP3D 计划里 `Point<Dim*, Quantity<V>>` 的类型约束问题。

## 2. 泛型化目标

1. 宽、高、面积、重量等物理量使用 `Quantity<V>` 或专用几何物理量类型。
2. 坐标与矩形相交计算不要直接依赖 `Point<Dim2, Quantity<V>>`，除非 `Quantity<V>` 已实现完整 `FloatingNumber<Quantity<V>>`。
3. 成本、利用率、松弛量使用 `V`。
4. 件数、箱数、分组编号使用整数类型。

## 3. 物理量化硬规则

二维装箱/排样中的有量纲字段必须 `Quantity<V>` 化：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 坐标 | `x`, `y`, `left`, `bottom` | `Length` |
| 尺寸 | `width`, `height` | `Length` |
| 面积 | `area`, `usedArea`, `restArea` | `Area` |
| 重量/承重 | `weight`, `maxLoad` | `Mass` |
| 产能/处理能力 | `capacity`, `throughput` | `Amount / Time` 或业务定义单位 |

无量纲值才保留裸 `V`，例如利用率、旋转惩罚、评分、归一化目标值。

## 4. 建议路线

### Phase B2-0：基础几何量决策

在写业务模型前，先选择：

1. 新增 `QuantityPoint2<V>` / `QuantityRectangle<V>`。
2. 或让 `Quantity<V>` 完整实现 `FloatingNumber<Quantity<V>>` 后复用 math `Point`。

推荐第 1 种，边界更小。

验收：

- [ ] 可表达二维位置、宽高、面积。
- [ ] 矩形相交面积单位为面积量纲。
- [ ] Flt64 与 FltX 特化都能编译。

### Phase B2-1：领域模型

后续新增对象建议直接采用：

- `RectangleItem<V>`
- `Sheet<V>`
- `Placement2<V>`
- `Pattern2<V>`
- `PackingPlan2<V>`

验收：

- [ ] `RectangleItem<Flt64>` / `RectangleItem<FltX>` 编译通过。
- [ ] 面积、利用率、剩余空间计算不固定 `Flt64`。

### Phase B2-2：算法与 solver 边界

启发式算法保持泛型；MILP/LP 建模集中在 adapter：

- `Bpp2dModelAdapter<V>`
- `Bpp2dFlt64ModelAdapter`

验收：

- [ ] 核心领域层无 `LinearMetaModel<Flt64>`。
- [ ] solver 返回值写回 `Placement2<V>` 时走显式转换。

## 5. 门禁

```powershell
git grep -n "Flt64\\|FltX\\|Double\\|Point<Dim2, Quantity" -- ospf-kotlin-framework-bpp2d
```

允许 `Flt64` 只出现在兼容层、adapter 和测试里。
