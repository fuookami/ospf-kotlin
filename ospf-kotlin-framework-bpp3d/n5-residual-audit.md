# N5 残留审计（R 轮深层重构版）

日期：2026-05-24

## 1. 审计口径

- 范围：`ospf-kotlin-framework-bpp3d/**/src/main/**/*.kt`
- 不含测试目录
- 关键字：
  - `toLegacy(`
  - `asScalarF64(`
  - `toFlt64(`
  - `QuantityFlt64`
  - `\bFlt64\b`
  - `UNCHECKED_CAST`

命令：

```powershell
rg -n "toLegacy\(|asScalarF64\(|toFlt64\(|QuantityFlt64|\bFlt64\b|UNCHECKED_CAST" ospf-kotlin-framework-bpp3d -g "**/src/main/**/*.kt" -S
```

## 2. 统计结果（当前基线）

- `toLegacy(`：17
- `asScalarF64(`：130
- `toFlt64(`：13
- `QuantityFlt64`：86
- `Flt64`（token）：599
- `UNCHECKED_CAST`：5

说明：

- 以上计数是“残留计数”，不是“问题计数”。
- 口径允许兼容层、solver 边界、已登记兼容算法保留。

## 3. toLegacy 残留分类

### 3.1 兼容 API 层（允许）

- `bpp3d-domain-item-context/.../api/QuantityDomainApi.kt`（13）
  - 用于 Quantity 主模型到 Legacy 主模型的兼容导出。

### 3.2 layer-assignment 兼容桥（允许）

- `bpp3d-domain-layer-assignment-context/.../model/Load.kt`（4）
  - 用于对接 legacy layer assignment 负载路径，属于兼容桥而非新主链路。

结论：`toLegacy(` 残留全部位于允许分类。

## 4. 数值降级边界分类

### 4.1 toFlt64（允许）

- `bpp3d-infrastructure/.../QuantityLegacyScalarAdapter.kt`
- `bpp3d-infrastructure/.../QuantityCompatibility.kt`
- `bpp3d-domain-layer-assignment-context/.../model/Load.kt`
- `bpp3d-domain-layer-assignment-context/.../model/SolverValueAdapterExample.kt`

结论：`.toFlt64()` 已限定在兼容转换层与 solver adapter。

### 4.2 asScalarF64（允许）

主要分布：

- `bpp3d-infrastructure`：几何/投影/容器的 Flt64 兼容计算路径
- `bpp3d-domain-item-context`：兼容算法与历史启发式（如 ItemMerger/Pattern/ItemHeightCombinator）
- `bpp3d-domain-block-loading-context`：legacy block loading 启发式计算
- `bpp3d-domain-item-context/api/QuantityDomainApi.kt`：兼容导出路径

结论：当前 `asScalarF64(` 残留均属于“兼容层 + 历史算法保留”范围，未发现新增“泛型新主链路直降级”路径。

## 5. UNCHECKED_CAST 审计（与 daily 勾选对齐）

### 5.1 代码中显式 `@Suppress("UNCHECKED_CAST")`（5处，均为局部 suppress）

- `bpp3d-infrastructure/.../api/QuantityInfrastructureApi.kt:22`
- `bpp3d-infrastructure/.../QuantityGeometryGeneric.kt:13`
- `bpp3d-infrastructure/.../Cuboid.kt:54`
- `bpp3d-domain-item-context/.../api/QuantityDemandStatistics.kt:36`
- `bpp3d-domain-item-context/.../api/QuantityDemandStatistics.kt:48`

说明：

- 均为“最小作用域 suppress”，未使用文件级大范围压制。

### 5.2 编译 warning 残留（`mvn -f ospf-kotlin-framework-bpp3d/pom.xml -am -DskipTests compile`）

本次复验提取到 `Unchecked cast` warning 共 27 条，分布：

- `bpp3d-domain-item-context`：14 条
  - 主要在 `Bin.kt`、`Item.kt`、`ItemContainer.kt`、`PackageAttribute.kt`、`ItemMerger.kt`、`LoadingOrderCalculator.kt`
  - 类型多为 `Placement3<*>` / `List<Placement3<*>>` 到具体实体类型的窄化
- `bpp3d-domain-bla-context`：13 条
  - 主要在 `BottomUpLeftJustifiedAlgorithm.kt`
  - 类型多为 `Placement2<*, P>`、`Container2Shape<P>` 到 `Side/Front` 具体投影类型窄化

说明：

- 这些 warning 来自历史泛型桥接和投影方向特化，不是本轮新增语义回退。
- 本轮已在 infrastructure 新增最小范围 suppress（如 `Cuboid.view`），避免 warning 向外扩散。

## 6. 行为口径复核

- `daily.md`：`- [ ]` 为 0（无未勾选）
- `layer-assignment` 测试目录：`toLegacy(` 为 0
- 关键定向测试通过：
  - infrastructure：13/13
  - domain-item：6/6
  - layer-assignment：6/6
- 三条编译闭环均 `BUILD SUCCESS`

## 7. 结论

N5 审计口径与 R 轮验收状态已一致：

- 数值降级边界（`toFlt64`/`asScalarF64`）均在允许范围。
- `toLegacy` 残留仅在兼容 API 与兼容桥。
- `UNCHECKED_CAST` 已补充“代码 suppress + 编译 warning”双维说明，并与 `daily.md` 勾选口径一致。
