# N5 残留审计（2026-05-24，三次更新）

## 统计口径
- 范围：`ospf-kotlin-framework-bpp3d/**/src/main/**/*.kt`
- 关键字：`QuantityFlt64`、`Flt64`、`toFlt64()`
- 不含测试目录

## 统计结果
- `QuantityFlt64` 命中：`86`
- `toFlt64()` 命中：`13`
- `Flt64`（token）命中：`599`

## `toFlt64()` 归类（逐项）
### 兼容转换层（允许）
- `bpp3d-infrastructure/.../QuantityLegacyScalarAdapter.kt`
- `bpp3d-infrastructure/.../QuantityCompatibility.kt`

### solver adapter（允许）
- `bpp3d-domain-layer-assignment-context/.../model/Load.kt`
- `bpp3d-domain-layer-assignment-context/.../model/SolverValueAdapterExample.kt`

### 业务主链路（不再出现）
- 复核结果：`0`。
- 说明：`Cuboid.kt`、`Projection.kt`、`Container.kt` 的历史 `.toFlt64()` 已替换为兼容层 `asScalarF64()`。

## 裸 `Flt64` 有量纲字段复核
使用以下规则复核字段签名：

```powershell
rg -n "\b(val|var)\s+(width|height|depth|weight|area|volume|actualVolume|linearDensity|maxHeight|minDepth|maxDepth|maxWeight|maxVolume|maxArea|maxLength|maxWidth|restWeight|loadedWeight)\w*\s*:\s*Flt64\b" ospf-kotlin-framework-bpp3d -g "**/src/main/**/*.kt" -S
```

结果：无命中。

说明：
- `bpp3d-domain-packing-context/.../MaterialAttribute.kt` 中 `maxHeight: Flt64` 已提升为 `Quantity<Flt64>`。

## 结论
N5 收口验收通过（按当前口径）：
- `.toFlt64()` 已限定在 solver adapter 或兼容转换层。
- 未发现新的“有量纲领域字段使用裸 `Flt64`”。
- `QuantityFlt64` 未出现在新增领域主模型字段中（残留在兼容层/别名路径）。
