# CSP2D 泛型化计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-csp2d` 当前只有顶层 `pom.xml`，没有已跟踪 Kotlin 源文件。

当前没有可迁移源码，泛型化计划作为后续实现准入约束。

## 2. 泛型化目标

1. 物料宽高、板材尺寸、裁切位置、面积使用 `Quantity<V>` 或专用二维量纲类型。
2. 损耗率、利用率、惩罚系数使用裸 `V`。
3. 件数、刀数、模式数量使用整数类型。
4. solver 边界集中转换，不在领域层暴露 `Flt64` 模型。

## 3. 物理量化硬规则

CSP2D 中有量纲字段必须 `Quantity<V>` 化：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 坐标/裁切位置 | `x`, `y`, `cutX`, `cutY` | `Length` |
| 尺寸 | `width`, `height`, `sheetWidth`, `sheetHeight` | `Length` |
| 面积 | `area`, `usedArea`, `wasteArea` | `Area` |
| 重量 | `weight` | `Mass` |
| 产能/加工能力 | `capacity`, `cuttingCapacity`, `cuttingRate` | `Area / Time`、`Amount / Time` 或业务定义单位 |

裸 `V` 只用于损耗率、利用率、系数、评分等无量纲值。

## 4. 建议实施步骤

### Phase C2-0：二维几何基础

参考 BPP2D，先建立：

- `QuantityPoint2<V>`
- `QuantityRect<V>`
- `CutLine<V>`
- `SheetArea<V>`

验收：

- [ ] 支持 Flt64/FltX。
- [ ] 面积单位正确。
- [ ] 不依赖 `Point<Dim2, Quantity<V>>`，除非 Quantity 已实现 FloatingNumber。

### Phase C2-1：领域模型

后续新增对象建议：

- `Material2<V>`
- `Product2<V>`
- `CuttingPattern2<V>`
- `CuttingPlan2<V>`

验收：

- [ ] 领域层无裸 `Double`。
- [ ] `Product2<Flt64>` / `Product2<FltX>` 编译通过。

### Phase C2-2：求解边界

建立：

- `Csp2dModelAdapter<V>`
- `Csp2dFlt64ModelAdapter`

验收：

- [ ] `LinearMetaModel<Flt64>` 只在 adapter 中出现。
- [ ] 结果写回领域模型有显式转换策略。

## 5. 门禁

```powershell
git grep -n "Flt64\\|FltX\\|Double\\|Point<Dim2, Quantity" -- ospf-kotlin-framework-csp2d
```

领域层不允许直接固定求解数值类型。
