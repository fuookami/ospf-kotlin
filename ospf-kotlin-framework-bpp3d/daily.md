# BPP3D Quantity<V> 深层重构交接计划

日期：2026-05-24

## 一、已完成事项摘要

当前代码已经完成路线 A 的全链路收口，可作为下一轮深层重构的基线。

- [x] BPP3D 已具备 `Quantity<V>` 泛型 API 主路径。
- [x] `Flt64` 旧路径、兼容入口和 starter/example 编译闭环已保留。
- [x] APS/FltX proof 已能不依赖 `toLegacy()` 表达领域对象。
- [x] solver 数值转换已通过 adapter 收口。
- [x] 需求统计、layer assignment 和残留审计已完成当前口径验收。
- [x] 关键定向测试与三条编译闭环已通过。

说明：

- 当前状态是“泛型 API 主路径 + legacy 兼容层并存”。
- 下一轮目标不是继续补外层 API，而是让内部主实现也逐步泛型化。
- 未提交文件较多，开始前必须先确认当前工作区是否已作为基线提交。

## 二、新目标

目标：把 BPP3D 从“泛型 API 包装层”推进到“泛型内部主实现”，让 legacy `Flt64` 模型退到薄兼容层。

最终状态：

- infrastructure 内部核心模型以 `Quantity<V>` / `V : FloatingNumber<V>` 为主实现。
- domain-item-context 内部主模型以泛型类型承载物理量字段。
- BPP3D 各上下文不再通过 `toLegacy()`、`asScalarF64()` 或 `QuantityFlt64` 完成主链路逻辑。
- `Flt64` 只保留在兼容 typealias、旧构造入口、solver adapter 和测试回归样例中。
- unchecked cast warning 大幅减少，剩余 warning 有明确设计理由。
- APS/FltX 能走一条正式端到端领域链路，而不是只在 API 层 proof 中成立。

非目标：

- 不要求删除所有旧 API。
- 不要求改动 `ospf-kotlin-math` 的 `Point<D, V>` / `Vector<D, V>` 约束。
- 不要求让 `Quantity<V>` 实现 `FloatingNumber<Quantity<V>>`。
- 不要求 solver 改用 `FltX`；solver 边界仍然保持 `Flt64`。

## 三、后续事项

### Phase R1：冻结兼容边界与重构基线

目标：先把“主实现”和“兼容层”的边界写清楚，避免下一轮重构继续扩大 legacy 使用面。

- [x] 确认当前工作区已提交，或记录本轮基线 commit。
- [x] 将 `toLegacy()`、`asScalarF64()`、`QuantityFlt64`、裸 `Flt64` 残留重新分类。
- [x] 标记允许保留的兼容入口和 solver adapter 文件。
- [x] 标记必须迁出的主链路文件。
- [x] 更新 `n5-residual-audit.md` 为深层重构版审计文档。

### Phase R2：下沉 infrastructure 泛型主实现

目标：让 `bpp3d-infrastructure/src/main/...` 中的核心类型本身成为泛型主实现，而不是只在 `infrastructure/api` 下提供泛型镜像。

- [x] 将 `AbstractCuboid<Flt64>` 使用点推进为 `AbstractCuboid<V>` 或等价泛型边界。
- [x] 将 `Cuboid`、`CuboidView`、`Projection`、`Placement`、`Container` 的内部主链路泛型化。
- [x] 将 `QuantityGeometrySpike.kt` / `QuantityGeometryGeneric.kt` 收敛为正式几何实现。
- [x] 将 `QuantityCompatibility.kt` 退化为兼容入口，不再承载主链路逻辑。
- [x] 将 `QuantityLegacyScalarAdapter.kt` 限定为 legacy/solver 边界工具。
- [x] 修复或消除 infrastructure 中由泛型迁移引入的 unchecked cast warning。

### Phase R3：下沉 domain-item-context 泛型主实现

目标：让 item 领域模型内部主实现泛型化，API 层不再只是外壳。

- [x] 将 `Material`、`PackageShape`、`Package` 的内部主模型泛型化。
- [x] 将 `Item`、`ActualItem`、`PatternedItem`、`ItemView` 的内部主模型泛型化。
- [x] 将 `BinLayer`、`Block`、`Bin`、`ItemContainer`、`Pattern`、`Schema` 的主链路泛型化。
- [x] 将 `PackageAttribute`、hanging/stacking/deformation 策略中的有量纲值从 `Flt64` 推进到 `Quantity<V>`。
- [x] 将 `ItemHeightCombinator`、`ItemMerger`、`LoadingOrderCalculator` 中的有量纲算法参数泛型化。
- [x] 保留 `Flt64*` typealias 和旧构造入口作为兼容薄层。

### Phase R4：迁移跨上下文算法主链路

目标：让 BPP3D 其他上下文直接消费泛型领域模型，减少 legacy 模型中转。

- [x] 迁移 `bpp3d-domain-bla-context`。
- [x] 迁移 `bpp3d-domain-block-loading-context`。
- [x] 迁移 `bpp3d-domain-layer-generation-context`。
- [x] 迁移 `bpp3d-domain-layer-selection-context`。
- [x] 迁移 `bpp3d-domain-layer-assignment-context`。
- [x] 迁移 `bpp3d-domain-packing-context`。
- [x] 迁移 `bpp3d-application` 和 starter。
- [x] 确认所有上下文的 Flt64 兼容路径只通过 typealias 或 adapter 暴露。

### Phase R5：收口 solver 与数值转换边界

目标：把所有数值降级和单位归一集中到明确边界，业务算法不直接调用 `asScalarF64()`。

- [x] 为几何比较、排序、分组、阈值判断补充泛型 helper。
- [x] 将 `asScalarF64()` 从领域算法主链路迁出。
- [x] 将 `.toFlt64()` 限定到 solver adapter、legacy 转换层和测试。
- [x] 为 FltX 场景提供正式 solver adapter 用例。
- [x] 为所有保留的数值降级点记录原因。

### Phase R6：测试、warning 与残留审计收口

目标：用测试和静态审计证明内部主链路已泛型化。

- [x] 补充 infrastructure 泛型测试，覆盖 `Flt64` 与 `FltX`。
- [x] 补充 item/domain 泛型测试，覆盖 `Flt64` 与 `FltX`。
- [x] 补充跨上下文 FltX 端到端 proof。
- [x] 补充 legacy Flt64 兼容回归测试。
- [x] 清理或解释 unchecked cast warning。
- [x] 更新 residual audit，列出所有允许残留。

## 四、详细执行步骤

1. 固化当前基线。

   ```powershell
   git status --short
   git rev-parse --abbrev-ref HEAD
   git log -1 --oneline
   ```

   如果当前大批文件尚未提交，先完成提交或明确基线，否则后续重构难以审查。

2. 复跑当前验收命令，确认从已知绿色状态开始。

   ```powershell
   mvn -f ospf-kotlin-framework-bpp3d/pom.xml -am -DskipTests compile
   mvn -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests compile
   mvn -pl ospf-kotlin-example -am -DskipTests compile
   mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am "-Dtest=FltXDirectCompileProofTest,SolverAdapterBoundaryTest,ItemDemandConstraintModeKeyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
   ```

3. 建立深层残留清单。

   ```powershell
   rg -n "toLegacy\(|asScalarF64\(|QuantityFlt64|\bFlt64\b|UNCHECKED_CAST" ospf-kotlin-framework-bpp3d -g "**/src/main/**/*.kt" -S
   ```

   分类规则：

   - 允许：兼容 typealias、旧构造入口、solver adapter、legacy 转换层、测试。
   - 待迁移：领域算法、几何算法、上下文服务、主模型字段、主模型方法签名。
   - 禁止新增：新主链路中的 `toLegacy()`、直接 `.toFlt64()`、无解释的 unchecked cast。

4. 先做 infrastructure 下沉。

   - 以现有 `infrastructure/api/QuantityInfrastructureApi.kt` 为目标形态。
   - 将正式实现逐步迁入 `Cuboid.kt`、`Projection.kt`、`Placement.kt`、`Container.kt`。
   - 每迁移一个核心类型，保留 Flt64 typealias 并运行 infrastructure test-compile。
   - 避免同时迁移 domain-item-context，降低泛型错误扩散。

5. 再做 domain-item-context 下沉。

   - 先迁移低依赖模型：`Material`、`PackageShape`、`Package`。
   - 再迁移 item 和 view：`Item`、`ActualItem`、`PatternedItem`、`ItemView`。
   - 最后迁移容器和组合模型：`BinLayer`、`Block`、`Bin`、`ItemContainer`、`Pattern`、`Schema`。
   - 每完成一组模型，补一个 Flt64 兼容测试和一个 FltX 泛型测试。

6. 迁移领域算法服务。

   - 优先处理 `PackageAttribute`、`ItemHeightCombinator`、`ItemMerger`、`LoadingOrderCalculator`。
   - 将有量纲参数改为 `Quantity<V>`。
   - 将无量纲比例、排序权重、计数继续保持裸数值。
   - 对必须降级到 `Flt64` 的算法点抽取 adapter 或 comparator。

7. 逐个迁移其他上下文。

   - 按依赖顺序处理 BLA、block-loading、layer-generation、layer-selection、layer-assignment、packing。
   - 每个模块完成后运行该模块 compile 或相关测试。
   - 不要在多个上下文中重复创建桥接逻辑。

8. 收口 solver 边界。

   - 保持 solver 输入为 `Flt64`。
   - 所有 `Quantity<V>` 到 solver 数值转换都必须经过 `Bpp3dSolverValueAdapter` 或等价 adapter。
   - 禁止领域模型自行做单位归一。

9. 清理 warning。

   - 先处理因泛型迁移新增的 unchecked cast。
   - 对无法消除的 cast 加最小范围 `@Suppress`，并在审计文档说明原因。
   - 不接受文件级大范围 suppress 掩盖新问题。

10. 更新文档和验收状态。

   - 每完成一个 phase，更新本文件勾选状态。
   - 更新 `n5-residual-audit.md` 或新增深层审计章节。
   - 最后执行完整验收命令。

## 五、修改清单

### 基线与审计

- `ospf-kotlin-framework-bpp3d/daily.md`
- `ospf-kotlin-framework-bpp3d/n5-residual-audit.md`

### infrastructure

- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../api/QuantityInfrastructureApi.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityGeometryGeneric.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityGeometrySpike.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityCompatibility.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityLegacyScalarAdapter.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Cuboid.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Container.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Projection.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Placement.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Orientation.kt`

### domain-item-context

- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../api/QuantityDomainApi.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../api/QuantityDomainAliases.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../api/QuantityDemandStatistics.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Material.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Package.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Item.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Bin.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Block.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Layer.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Pattern.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/Schema.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/DemandStatistics.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../model/ShadowPriceMap.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../service/ItemHeightCombinator.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/.../service/LoadingOrderCalculator.kt`

### other contexts

- `ospf-kotlin-framework-bpp3d/bpp3d-domain-bla-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-generation-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-selection-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-packing-context/src/main/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-application/src/main/...`
- `ospf-kotlin-starters/ospf-kotlin-starter-bpp3d/src/main/...`

### tests

- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/test/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/test/.../FltXDirectCompileProofTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/test/.../SolverAdapterBoundaryTest.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/test/.../ItemDemandConstraintModeKeyTest.kt`

## 六、验收标准

### 编译验收

- [x] `bpp3d-infrastructure` 编译通过。
- [x] `bpp3d-domain-item-context` 编译通过。
- [x] `bpp3d-domain-layer-assignment-context` 编译通过。
- [x] BPP3D 全模块编译通过。
- [x] `ospf-kotlin-starter-bpp3d` 编译通过。
- [x] `ospf-kotlin-example` 编译通过。

建议命令：

```powershell
mvn -f ospf-kotlin-framework-bpp3d/pom.xml -am -DskipTests compile
mvn -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests compile
mvn -pl ospf-kotlin-example -am -DskipTests compile
```

### 行为验收

- [x] Flt64 兼容路径行为不回退。
- [x] FltX 泛型路径可直接通过内部主模型构造领域对象。
- [x] APS/FltX proof 不依赖 `toLegacy()`。
- [x] 三种需求统计模式行为不回退。
- [x] layer assignment 的 load、demand constraint、shadow price key 行为不回退。
- [x] solver adapter 仍是唯一 solver 数值转换边界。

建议命令：

```powershell
mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am "-Dtest=FltXDirectCompileProofTest,SolverAdapterBoundaryTest,ItemDemandConstraintModeKeyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### 深层重构验收

- [x] `infrastructure/api` 下的泛型镜像已合并进正式 infrastructure 主实现，或仅保留为薄导出层。
- [x] `domain/item/api` 下的泛型镜像已合并进正式 domain-item 主实现，或仅保留为薄导出层。
- [x] 主链路不再通过 `toLegacy()` 表达领域对象。
- [x] 主链路不再通过 `asScalarF64()` 完成几何或领域算法。
- [x] `QuantityFlt64` 只保留在兼容 typealias、旧构造入口或测试。
- [x] 裸 `Flt64` 只保留在 solver adapter、兼容入口、无量纲参数或测试。
- [x] unchecked cast warning 较基线减少，剩余项有审计说明。

### 残留审计

执行：

```powershell
rg -n "toLegacy\(|asScalarF64\(|QuantityFlt64|\bFlt64\b|UNCHECKED_CAST" ospf-kotlin-framework-bpp3d -g "**/src/main/**/*.kt" -S
```

验收：

- [x] 每个 `toLegacy()` 残留都属于旧 API 兼容入口。
- [x] 每个 `asScalarF64()` 残留都属于 solver adapter、legacy 转换层或已记录的兼容算法。
- [x] 每个 `QuantityFlt64` 残留都有明确兼容理由。
- [x] 每个裸 `Flt64` 残留都属于允许分类。
- [x] 每个 `UNCHECKED_CAST` 都有局部 suppress 或审计说明。

## 七、交接注意事项

- 这轮重构会触及核心类型，必须按 phase 小步提交。
- 不要在未提交基线上继续扩大修改范围。
- 优先下沉已有泛型 API，不要再新增平行包装层。
- 如果某个 legacy 算法短期无法泛型化，必须把它标为兼容算法并写入残留审计。
- 每完成一个 phase，都更新本文件勾选项和实际执行过的命令。

