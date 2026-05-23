# BPP3D Quantity<V> 全链路交接计划

日期：2026-05-23

## 一、已完成事项摘要

当前代码已经完成路线 A 的阶段性收口，但尚未完成原始理想目标。

- [x] BPP3D 主链路已完成 `Quantity<Flt64>` 阶段迁移。
- [x] `Orientation` 已完成 sealed class 改造并保留兼容入口。
- [x] item 领域已补齐三种需求统计模式。
- [x] layer assignment 已接入三种统计模式和 shadow price key 区分。
- [x] solver 数值转换边界已初步集中到 adapter。
- [x] 已提供 `FltX` / APS 最小直连 proof。
- [x] public/domain API 已能构造最小 `Material<FltX>`、`PackageShape<FltX>`、`Item<FltX>`、`BinLayer<FltX>` 等对象。
- [x] starter 与 example 当前编译闭环已通过。

说明：

- 以上只代表阶段态可用，不代表 `Quantity<V>` 全链路完成。
- 当前仍存在 `QuantityFlt64` / 裸 `Flt64` 领域字段残留。
- 剩余工作需要按跨模块泛型化任务重新执行。

## 二、新目标

目标：把 BPP3D 从阶段态 `Quantity<Flt64>` 推进到正式 `Quantity<V>` 全链路能力。

最终状态：

- public/domain API 支持 `V : FloatingNumber<V>`。
- 所有有物理量纲的领域字段使用 `Quantity<V>`。
- `Flt64` 只允许保留在兼容入口、默认 typealias、测试回归样例和 solver adapter 边界。
- APS 可以直接以 `Quantity<FltX>` 复用 BPP3D 类型。
- 旧 `Flt64` 调用路径继续可编译、行为不回退。

路线约束：

- 继续采用路线 A：保留 BPP3D 专用几何类型，并将其泛型化为 `Quantity<V>`。
- 不让 `Quantity<V>` 实现 `FloatingNumber<Quantity<V>>`。
- 不改动 `ospf-kotlin-math` 的 `Point<D, V>` / `Vector<D, V>` 核心约束。
- solver 仍以 `Flt64` 为数值边界，所有单位归一和数值转换集中在 adapter。

## 三、剩余事项

### Phase N1：泛型化几何与兼容层

目标：把 BPP3D 专用几何类型从 `QuantityFlt64` 推进到 `Quantity<V>`。

- [ ] 将 `QuantityPoint2` / `QuantityPoint3` 泛型化。
- [ ] 将 `QuantityVector2` / `QuantityVector3` 泛型化。
- [ ] 将几何加减、比较、投影、相交面积、体积计算泛型化。
- [ ] 保留 `QuantityFlt64` typealias 作为兼容入口。
- [ ] 保留裸 `Flt64` 工厂函数，但返回值必须进入 `Quantity<Flt64>` 兼容路径。

### Phase N2：泛型化 infrastructure 主模型

目标：`bpp3d-infrastructure` 的有量纲主模型支持 `Quantity<V>`。

- [ ] 将 `AbstractCuboid` 泛型化为 `AbstractCuboid<V>`。
- [ ] 将 `Cuboid<T>` 泛型化为 `Cuboid<T, V>`。
- [ ] 将 `CuboidView`、`BottomSupport`、`Container2`、`Container3`、`Projection`、`Placement` 等类型泛型化。
- [ ] `width`、`height`、`depth`、`weight`、`area`、`volume`、`actualVolume`、`linearDensity` 均返回 `Quantity<V>`。
- [ ] `Orientation` 保持非泛型 sealed class，但其计算方法支持泛型 cuboid。

### Phase N3：泛型化 domain-item-context

目标：item 领域主模型支持 `Quantity<V>`，并保留 `Flt64` 兼容入口。

- [ ] 将 `Material.weight` 推进到 `Quantity<V>`。
- [ ] 将 `PackageShape`、`PackageAttribute`、`Package` 泛型化。
- [ ] 将 `Item`、`ActualItem`、`PatternedItem`、`ItemView` 泛型化。
- [ ] 将 `BinLayer`、`Block`、`Bin`、`ItemContainer`、`Pattern`、`Schema` 泛型化。
- [ ] 将 `DemandStatistics`、`Bpp3dDemandValue.Weight` 推进到 `Quantity<V>`。
- [ ] 保留 `Flt64Item`、`Flt64PackageShape` 等兼容 typealias。

### Phase N4：泛型化其他 BPP3D 上下文

目标：所有 BPP3D 上下文能接受 `V : FloatingNumber<V>` 主路径。

- [ ] 适配 `bpp3d-domain-bla-context`。
- [ ] 适配 `bpp3d-domain-block-loading-context`。
- [ ] 适配 `bpp3d-domain-layer-generation-context`。
- [ ] 适配 `bpp3d-domain-layer-selection-context`。
- [ ] 适配 `bpp3d-domain-layer-assignment-context`。
- [ ] 适配 `bpp3d-domain-packing-context`。
- [ ] 适配 `bpp3d-application`。
- [ ] 适配 `ospf-kotlin-starter-bpp3d`。

### Phase N5：收紧残留边界

目标：清理 `QuantityFlt64` / 裸 `Flt64` 的领域残留，只保留明确允许的位置。

- [ ] 审计 `QuantityFlt64` 残留，并为每个残留标注兼容理由。
- [ ] 审计裸 `Flt64` 残留，并确认其只属于 solver、兼容入口、默认值工厂或测试。
- [ ] 清退领域物理量字段中的裸 `Flt64`。
- [ ] 清退领域主模型字段中的 `QuantityFlt64`。
- [ ] 将新增代码中的 `.toFlt64()` 限定到 solver adapter 或兼容转换层。

## 四、详细执行步骤

1. 确认当前工作区和基线。

   ```powershell
   git status --short
   mvn -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests compile
   mvn -pl ospf-kotlin-example -am -DskipTests compile
   ```

2. 建立残留清单。

   ```powershell
   git grep -n "QuantityFlt64\|Flt64\|toFlt64()" -- ospf-kotlin-framework-bpp3d
   ```

   分类规则：

   - 允许：solver adapter、旧 API 兼容入口、默认 typealias、Flt64 回归测试。
   - 不允许：领域物理量字段、几何主模型字段、需求统计主模型、物料重量主模型。

3. 先迁移几何层。

   - 从 `QuantityGeometrySpike.kt`、`QuantityCompatibility.kt` 开始。
   - 先让 `QuantityPoint*` / `QuantityVector*` 接收 `V : FloatingNumber<V>`。
   - 保持 `QuantityFlt64` 兼容别名和旧工厂函数可用。
   - 每完成一组几何类型就运行 infrastructure 相关测试。

4. 再迁移 infrastructure 主模型。

   - 从 `AbstractCuboid<V>` 和 `Cuboid<T, V>` 开始。
   - 继续推进 view、projection、placement、container。
   - `Orientation` 不泛型化，只让它的计算入口支持泛型 cuboid。
   - 保持旧 `Cuboid<T>` 或等价 `Flt64` typealias 可编译。

5. 迁移 domain-item-context。

   - 优先迁移 `Material`、`PackageShape`、`Package`、`Item`。
   - 再迁移 `BinLayer`、`Block`、`Bin`、`ItemContainer`、`Pattern`、`Schema`。
   - 最后迁移 `DemandStatistics` 和需求值模型。
   - 每个公开类型迁移后补一个 `Flt64` 兼容用例和一个 `FltX` 泛型用例。

6. 迁移 layer assignment。

   - 保留 solver adapter 作为 `Quantity<V>` 到 `Flt64` 的唯一数值边界。
   - 检查 `Load`、`Capacity`、限制器、目标函数、shadow price 读取路径。
   - 禁止在业务模型中新增 `.toFlt64()`。

7. 迁移其余上下文。

   - 按依赖顺序逐个处理 BLA、block loading、layer generation、layer selection、packing、application、starter。
   - 每迁移一个模块就运行该模块 compile，避免最后集中爆炸。

8. 强化 APS/FltX proof。

   - 保留现有最小 proof。
   - 增加一条不经过 `toLegacy()` 的正式 API proof。
   - 若暂时仍需要 `toLegacy()`，必须在测试名或注释中明确其为过渡桥接。

9. 做最终残留审计。

   - 对所有 `QuantityFlt64`、裸 `Flt64`、`.toFlt64()` 残留逐条归类。
   - 对无法清理的残留写入兼容理由。
   - 不允许用注释掩盖新的领域字段残留。

10. 执行完整验收命令并更新本文件勾选状态。

## 五、修改清单

### 重点主代码

- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityGeometrySpike.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityCompatibility.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Cuboid.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Container.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Projection.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Placement.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Orientation.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../api/QuantityDomainApi.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Material.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Package.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Item.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Bin.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Block.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Layer.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/DemandStatistics.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/.../model/Load.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/.../model/Capacity.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/.../model/Assignment.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/.../model/SolverValueAdapterExample.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/.../service/limits/ItemDemandConstraint.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/.../service/limits/*.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-bla-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-generation-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-selection-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-packing-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-application/src/main/...`
- `ospf-kotlin-starters/ospf-kotlin-starter-bpp3d/src/main/...`
- `ospf-kotlin-example/src/main/...`

### 重点测试

- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/.../QuantityGeometrySpikeTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/.../CuboidCoreTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/.../ContainerShapeTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/.../ProjectionTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/.../PlacementTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/.../OrientationTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/test/.../DemandStatisticsTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/test/.../FltXDirectCompileProofTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/test/.../SolverAdapterBoundaryTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/test/.../ItemDemandConstraintModeKeyTest.kt`

## 六、验收标准

### 编译验收

- [ ] `bpp3d-infrastructure` 编译通过。
- [ ] `bpp3d-domain-item-context` 编译通过。
- [ ] `bpp3d-domain-layer-assignment-context` 编译通过。
- [ ] BPP3D 全模块编译通过。
- [ ] `ospf-kotlin-starter-bpp3d` 编译通过。
- [ ] `ospf-kotlin-example` 编译通过。

建议命令：

```powershell
mvn -f ospf-kotlin-framework-bpp3d/pom.xml -am -DskipTests compile
mvn -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests compile
mvn -pl ospf-kotlin-example -am -DskipTests compile
```

### 行为验收

- [ ] Flt64 旧主路径行为不回退。
- [ ] `Orientation` 兼容调用和序列化行为不回退。
- [ ] 三种需求统计模式行为不回退。
- [ ] layer assignment 的 load、demand constraint、shadow price 在三种统计模式下仍可区分 key。
- [ ] solver adapter 负责所有 `Quantity<V>` 到 `Flt64` 的数值转换。
- [ ] 新增 `FltX` proof 不依赖手写 `Flt64` 中间 DTO。
- [ ] 正式 public/domain API 可直接构造 `Quantity<FltX>` 主链路对象。

建议命令：

```powershell
mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am "-Dtest=FltXDirectCompileProofTest,SolverAdapterBoundaryTest,ItemDemandConstraintModeKeyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### 原始目标验收

- [ ] public/domain API 能直接构造 `Material<FltX>`。
- [ ] public/domain API 能直接构造 `PackageShape<FltX>` / `Package<FltX>`。
- [ ] public/domain API 能直接构造 `Item<FltX>`。
- [ ] public/domain API 能直接构造 `BinLayer<FltX>`、`Block<FltX>`、`Bin<FltX>` 或等价正式装载对象。
- [ ] APS/FltX proof 不依赖 `toLegacy()` 才能表达领域对象。
- [ ] `QuantityFlt64` 不再出现在领域主模型字段中，只保留在兼容层、默认 typealias 或测试中。
- [ ] 裸 `Flt64` 不再出现在有量纲领域字段中，只保留在 solver adapter、兼容入口或测试中。

### 残留审计

执行：

```powershell
git grep -n "QuantityFlt64\|Flt64\|toFlt64()" -- ospf-kotlin-framework-bpp3d
```

验收：

- [ ] 每个 `QuantityFlt64` 残留都有明确兼容理由。
- [ ] 每个裸 `Flt64` 残留都属于 solver、兼容入口、默认值工厂或测试。
- [ ] 每个 `.toFlt64()` 残留都属于 solver adapter、兼容转换层或测试。
- [ ] 不存在新的领域物理量字段使用裸 `Flt64`。
- [ ] 不存在新的领域主模型字段使用 `QuantityFlt64`。

## 七、交接注意事项

- 当前任务规模较大，不建议一次性跨所有模块改完；应按 Phase N1 到 N5 顺序推进。
- 每完成一个 phase，都需要更新本文件勾选状态和实际验证命令结果。
- 若必须保留 `Flt64` / `QuantityFlt64`，需要在代码或测试命名中体现兼容边界。
- 不要把 APS/FltX proof 停留在 `toLegacy()` 桥接层；它只能作为过渡验收。
