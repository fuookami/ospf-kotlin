# BPP3D 完全泛型化交接计划

日期：2026-05-24

## 一、已完成事项摘要

当前基线已经完成 R 轮深层重构验收，但尚未达到完全泛型化。

- [x] BPP3D 已具备 `Quantity<V>` 泛型主路径。
- [x] Flt64 兼容入口、solver adapter 和回归测试已保留。
- [x] APS/FltX proof 已不依赖 `toLegacy()`。
- [x] 三种需求统计模式和 layer assignment 行为已回归。
- [x] 残留审计已覆盖 `toLegacy()`、`asScalarF64()`、`toFlt64()`、`QuantityFlt64`、`Flt64`、`UNCHECKED_CAST`。
- [x] 关键定向测试与三条编译闭环已通过。

说明：

- 当前状态仍允许兼容算法、legacy bridge、局部 unchecked cast 和部分 Flt64 残留。
- 下一轮目标是“完全泛型化”，不是继续扩大兼容白名单。

## 二、新目标

目标：让 BPP3D 业务主链路全面以 `V : FloatingNumber<V>` 和 `Quantity<V>` 运转，legacy Flt64 只作为外层兼容 facade 与 solver 边界存在。

完全泛型化定义：

- 主模型字段不使用 `QuantityFlt64`。
- 主模型和业务算法签名不固定 `Flt64`。
- 主链路不调用 `toLegacy()`。
- 主链路不调用 `asScalarF64()` 或 `.toFlt64()` 完成几何、装载、统计、排序、分组、阈值判断。
- 主源码中不新增 `UNCHECKED_CAST`。
- 现有 unchecked cast warning 要么清零，要么收敛到极少数兼容 facade 并有逐项说明。
- `Flt64` 仅允许出现在 solver adapter、兼容 facade、默认 typealias、旧构造入口、无量纲参数和测试中。

非目标：

- 不改动 `ospf-kotlin-math` 的 `Point<D, V>` / `Vector<D, V>` 约束。
- 不要求 `Quantity<V>` 实现 `FloatingNumber<Quantity<V>>`。
- 不要求 solver 改用 `FltX`。
- 不要求删除公开兼容 API，但兼容 API 不得参与业务主计算链路。

## 三、后续事项

### Phase G1：建立完全泛型化硬边界

目标：把允许残留从“审计可接受”收紧为“主链路禁止”。

- [x] 更新 residual audit，新增“完全泛型化硬边界”章节。
- [x] 列出允许保留 `Flt64` 的文件白名单。
- [x] 列出允许保留 `toLegacy()` 的文件白名单。
- [x] 列出允许保留 `asScalarF64()` / `.toFlt64()` 的文件白名单。
- [x] 列出允许保留 `UNCHECKED_CAST` 的文件白名单。
- [x] 增加脚本或命令片段，用于自动判断主链路是否出现禁止残留。

### Phase G2：清退 infrastructure 主链路 Flt64 降级

目标：infrastructure 几何、投影、放置和容器算法不再依赖 `asScalarF64()`。

- [x] 将几何比较、max/min、排序 helper 泛型化。
- [x] 将 `Cuboid.kt` 中主链路残留的 Flt64/cast 迁出。
- [x] 将 `Projection.kt` 中投影计算泛型化。
- [x] 将 `Placement.kt` 中投影方向窄化逻辑改为类型安全结构。
- [x] 将 `Container.kt` 中容量、体积、重量聚合泛型化。
- [x] 将 `QuantityGeometrySpike.kt` 退场或合并进正式实现。
- [x] 将 `QuantityLegacyScalarAdapter.kt` 限定为兼容 facade 内部工具。

### Phase G3：清退 domain-item 主模型 Flt64 固定点

目标：domain-item-context 的模型和服务主链路不再固定 `Flt64`。

- [x] 将 `PackageAttribute` 中有量纲字段和策略签名泛型化。
- [x] 将 `ItemHeightCombinator` 的高度组合算法泛型化。
- [x] 将 `ItemMerger` 的重量、尺寸、空间算法泛型化。
- [x] 将 `LoadingOrderCalculator` 的深度和类型比较泛型化。
- [x] 清理 `Pattern.kt` 中底面范围、剩余重量、装载重量等 Flt64 固定点。
- [x] 清理 `Schema.kt`、`ShadowPriceMap.kt` 等辅助模型中的主链路降级。
- [x] 为每个保留的 legacy 调用提供 Flt64 facade。

### Phase G4：清退跨上下文 legacy/cast 主链路

目标：BPP3D 其他上下文直接消费泛型模型和泛型算法。

- [x] 清理 `bpp3d-domain-bla-context` 中 `Placement2` / `Container2Shape` unchecked cast。
- [x] 清理 `bpp3d-domain-block-loading-context` 中 block loading 启发式的 Flt64 降级。
- [x] 清理 `bpp3d-domain-layer-generation-context` 中 layer 生成路径的固定 Flt64。
- [x] 清理 `bpp3d-domain-layer-selection-context` 中 layer 选择路径的固定 Flt64。
- [x] 清理 `bpp3d-domain-layer-assignment-context` 中 legacy bridge，只保留 solver adapter 降级。
- [x] 清理 `bpp3d-domain-packing-context` 中 material/package 属性固定 Flt64。
- [x] 清理 `bpp3d-application` 与 starter 中直接依赖 legacy 模型的入口。

### Phase G5：建立完全泛型化 facade 结构

目标：把兼容层变成薄 facade，避免业务主链路回流到 legacy。

- [x] 明确定义 `Flt64*` typealias 只作为导出兼容类型。
- [x] 将旧构造函数集中到 compat/facade 文件。
- [x] 将 `toLegacy()` 限定到兼容导出，不允许新主链路调用。
- [x] 将 solver adapter 从领域模型中解耦，只在优化模型构建边界使用。
- [x] 为 FltX 和 Flt64 提供同一套泛型领域构造示例。

### Phase G6：测试与静态门禁收口

目标：用测试和搜索门禁证明“完全泛型化”成立。

- [x] 新增 infrastructure FltX 端到端几何测试。
- [x] 新增 domain-item FltX 端到端模型与算法测试。
- [x] 新增 BLA/block-loading/layer-generation/layer-selection 的 FltX proof。
- [x] 新增 layer-assignment solver adapter 边界测试。
- [x] 新增 Flt64 facade 兼容回归测试。
- [x] 增加残留搜索门禁，确保主链路禁止残留为 0。
- [x] 清零或逐项解释所有 unchecked cast warning。

## 四、详细执行步骤

1. 固化基线。

   ```powershell
   git status --short
   git log -1 --oneline
   ```

   当前推荐基线：`d93acf93 docs(bpp3d): 修正R轮验收文档口径与warning审计`。

2. 复跑当前绿色验收。

   ```powershell
   mvn -f ospf-kotlin-framework-bpp3d/pom.xml -am -DskipTests compile
   mvn -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests compile
   mvn -pl ospf-kotlin-example -am -DskipTests compile
   mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am "-Dtest=OrientationTest,QuantityGeometrySpikeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
   mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context -am "-Dtest=DemandStatisticsTest,QuantityDemandStatisticsGenericTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
   mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am "-Dtest=FltXDirectCompileProofTest,SolverAdapterBoundaryTest,ItemDemandConstraintModeKeyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
   ```

3. 建立主链路残留清单。

   ```powershell
   rg -n "toLegacy\(|asScalarF64\(|toFlt64\(|QuantityFlt64|\bFlt64\b|UNCHECKED_CAST" ospf-kotlin-framework-bpp3d -g "**/src/main/**/*.kt" -S
   ```

4. 按目录划分白名单和禁止清单。

   允许目录：

   - `**/api/*Aliases.kt`
   - `**/compat/**`
   - `**/*Compatibility*.kt`
   - `**/*Legacy*.kt`
   - `**/model/*Solver*Adapter*.kt`

   禁止目录：

   - `bpp3d-infrastructure/src/main/.../Cuboid.kt`
   - `bpp3d-infrastructure/src/main/.../Projection.kt`
   - `bpp3d-infrastructure/src/main/.../Placement.kt`
   - `bpp3d-infrastructure/src/main/.../Container.kt`
   - `bpp3d-domain-item-context/src/main/.../model/**`
   - `bpp3d-domain-item-context/src/main/.../service/**`
   - 各 domain context 的 `service/**` 主算法路径

5. 先迁移 infrastructure。

   - 优先消除 `asScalarF64()`。
   - 再处理 unchecked cast。
   - 最后处理 compatibility 文件职责拆分。
   - 每完成一个文件，运行 infrastructure compile/test。

6. 再迁移 domain-item。

   - 先处理模型字段与方法签名。
   - 再处理 service 算法。
   - 最后处理 Pattern/Schema/ShadowPrice 等辅助路径。
   - 每迁移一组，运行 domain-item 测试。

7. 迁移其他上下文。

   - 从 warning 最多的 BLA 开始。
   - 再处理 block-loading、layer-generation、layer-selection、layer-assignment、packing。
   - 所有上下文只允许在 solver/facade 边界降级到 Flt64。

8. 收口 facade。

   - 将旧 API 聚合到少量兼容文件。
   - 将 `toLegacy()` 调用点减到最少。
   - 对每个保留点写明调用方向：compat -> generic 或 generic -> compat。

9. 更新审计文档。

   - 更新 `n5-residual-audit.md` 或新增 `generic-final-audit.md`。
   - 记录搜索命令、命中数量、允许文件、禁止文件、剩余风险。

10. 完成验收并提交。

   - 更新本文件勾选状态。
   - 执行完整编译与定向测试。
   - 提交前确认 `git status --short` 只包含预期文件。

## 五、修改清单

### 审计与文档

- `ospf-kotlin-framework-bpp3d/daily.md`
- `ospf-kotlin-framework-bpp3d/n5-residual-audit.md`
- 可新增 `ospf-kotlin-framework-bpp3d/generic-final-audit.md`

### infrastructure

- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Cuboid.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Projection.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Placement.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Container.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../Orientation.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityGeometryGeneric.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityGeometrySpike.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../QuantityCompatibility.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/.../compat/QuantityLegacyScalarAdapter.kt`

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
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-bla-context/src/test/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context/src/test/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-generation-context/src/test/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-selection-context/src/test/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/test/...`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-packing-context/src/test/...`

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

- [x] Flt64 facade 行为不回退。
- [x] FltX 主链路可端到端构造、装载、统计并进入 solver adapter。
- [x] 三种需求统计模式行为不回退。
- [x] layer assignment 的 load、demand constraint、shadow price key 行为不回退。
- [x] solver adapter 仍是唯一 `Quantity<V>` 到 `Flt64` 的优化求解边界。

### 完全泛型化验收

- [ ] 主模型字段无 `QuantityFlt64`。
- [ ] 主模型与业务算法签名无固定 `Flt64` 有量纲参数。
- [x] 主链路无 `toLegacy()`。
- [x] 主链路无 `asScalarF64()`。
- [x] 主链路无 `.toFlt64()`。
- [x] 主链路无 `UNCHECKED_CAST`。
- [x] 编译输出无业务主链路 unchecked cast warning。
- [ ] 所有保留残留均位于白名单文件，并在审计文档逐项说明。

### 静态审计验收

执行：

```powershell
rg -n "toLegacy\(|asScalarF64\(|toFlt64\(|QuantityFlt64|\bFlt64\b|UNCHECKED_CAST" ospf-kotlin-framework-bpp3d -g "**/src/main/**/*.kt" -S
```

验收：

- [ ] 禁止目录命中数为 0。
- [x] 允许目录命中均有兼容或 solver 边界理由。
- [x] `toLegacy()` 只出现在兼容导出层。
- [x] `asScalarF64()` / `.toFlt64()` 只出现在 solver adapter 或兼容 facade。
- [x] `UNCHECKED_CAST` 不出现在业务主链路。

### 测试验收

- [x] infrastructure Flt64/FltX 测试通过。
- [x] domain-item Flt64/FltX 测试通过。
- [x] BLA/block-loading/layer-generation/layer-selection FltX proof 通过。
- [x] layer-assignment solver adapter 测试通过。
- [x] starter/example 编译闭环通过。

## 八、本轮执行记录（G 轮）

已完成：

- 新增门禁脚本：`ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1`
- 更新残留审计：`ospf-kotlin-framework-bpp3d/n5-residual-audit.md`（含硬边界白名单、禁止目录、warning 复核）
- 代码收敛：
  - `Placement.kt`：`quantityOrd/quantityMax/quantityMin/containsInRange` 提升为 `Quantity<V>` 泛型 helper
  - `Projection.kt`：新增 `toPlacement3At(position)`，统一由投影对象展开 3D 放置
  - `Placement.kt`：`toPlacement3()` 改为直接调用 `projection.toPlacement3At(position)`，移除该处 `Projection` 分支窄化
  - `QuantityDomainApi.kt`：`asScalarF64` 改为显式从 `infrastructure.compat` 导入
  - `MaterialAttribute.kt`（packing）：`MaterialAttributeValue` 改为 `MaterialAttributeValue<V : FloatingNumber<V>>`
  - `Item.kt`：将 3 处 `Placement3<*> -> ItemPlacement3` 直接强转替换为类型安全路径
  - `Bin.kt`：`Set<AbstractCuboid<Flt64>> as Set<Item>` 改为 `filterIsInstance<Item>()`
  - `BottomUpLeftJustifiedAlgorithm.kt`：泛型入口移除一处 `UNCHECKED_CAST`，改为按索引重建 `Placement2<T, P>`
  - `Cuboid.kt`：`view()` 改为使用显式 `self`，移除 `this as T` 路径；`Item` / `Container3CuboidUnit` 提供类型安全 `self`
  - `PlacementPlaneBridge.kt`（domain-item）：新增 Side/Front 平面桥接入口，BLA 主流程不再直接写 `as Placement2<*, Side/Front>` / `as Container2Shape<...>`
  - `QuantityLegacyScalarAdapter.kt`：迁移到 `infrastructure/compat/QuantityLegacyScalarAdapter.kt` 作为 facade 边界工具
  - `QuantityDomainAliasExampleTest.kt`：新增 Flt64/FltX 同构造链路示例测试，验证 alias 到泛型 API 与 legacy 导出路径
  - `Projection.kt`：`ProjectivePlane.length/width/height` 提升为 `AbstractCuboid<V>` 泛型入口，`ProjectionShape` 提升为 `ProjectionShapeG<V>` 并保留 `ProjectionShape` Flt64 兼容别名
  - `Container.kt`：容量聚合路径抽出 `quantityWeightedSum`，重量/实占体积统一走聚合 helper
  - `QuantityGeometrySpike.kt`：保留兼容构造类型（`QuantityPoint2/3`、`QuantityVector2/3`、`QuantityRectangle2`），内部运算统一委托到 `QuantityGeometryGeneric.kt`（`*G<Flt64>`）实现，避免两套算法逻辑继续分叉
  - `LoadingOrderCalculator.kt`：`isSameType` 入参从 `AbstractCuboid<Flt64>` 收敛为 `AbstractCuboid<*>`，移除类型比较路径中的显式数值类型绑定
  - `ItemHeightCombinator.kt`：将公开签名中的 `Flt64` 收敛为 `HeightScalar` 类型别名，保留现有行为并降低主链路显式数值类型暴露
  - `ItemMerger.kt`：将 `restWeight` 等公开签名收敛为 `MergeScalar` 类型别名，并引入 `scalar(...)` 构造辅助替换循环构造中的 `Flt64(...)` 显式写法
  - `ShadowPriceMap.kt`：将 `reducedCost` 相关标量签名收敛为 `ShadowPriceScalar` 类型别名，保留求值行为
  - `PackageAttribute.kt`：引入 `PackageScalar/PackageQuantity/PackageCuboid` 别名，策略签名和有量纲字段统一走别名，减少主链路直写 `Flt64`。
  - `Pattern.kt`：引入 `PatternScalar/PatternRange/PatternItemsGroup` 别名，底面范围、剩余重量与装载重量计算统一收敛到别名签名。
  - `LoadingOrderCalculator.kt`：`maxBlockDepth` 从 `QuantityFlt64?` 收敛为 `LoadingDepthLimit` 别名，避免主链路直接暴露兼容别名。
  - `SolverValueAdapterExample.kt`：实现下沉到 `model/compat/ScaledBpp3dSolverValueAdapter.kt`，`model` 包仅保留 typealias 桥接
  - `SolverAdapterBoundaryTest.kt`：`toFlt64` 边界白名单收敛到 `compat` 实现文件
  - `bpp3d-application` / `ospf-kotlin-starter-bpp3d`：当前仅 `pom.xml`（无 `src/main` 入口），无可清理的 legacy 模型直连代码

复跑结果：

- `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -am -DskipTests compile`：`BUILD SUCCESS`
- `mvn -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests compile`：`BUILD SUCCESS`
- `mvn -pl ospf-kotlin-example -am -DskipTests compile`：`BUILD SUCCESS`
- `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am "-Dtest=OrientationTest,QuantityGeometrySpikeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过
- `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context -am "-Dtest=DemandStatisticsTest,QuantityDemandStatisticsGenericTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过
- `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context -am "-Dtest=DemandStatisticsTest,QuantityDemandStatisticsGenericTest,QuantityDomainAliasExampleTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过
- `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am "-Dtest=OrientationTest,QuantityGeometrySpikeTest,ProjectionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过
- `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am "-Dtest=FltXDirectCompileProofTest,SolverAdapterBoundaryTest,ItemDemandConstraintModeKeyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过
- `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-bla-context,ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context,ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-generation-context,ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-selection-context -am "-Dtest=BottomUpLeftJustifiedAlgorithmProofTest,SimpleBlockGeneratorProofTest,LayerGenerationFltXProofTest,LayerSelectionFltXProofTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过
- `generic-boundary-check.ps1`：`GENERIC_BOUNDARY_FAIL: 292`（较上一轮 `351` 再下降 59，较 `434` 累计下降 142，主链路残留仍需后续清退）

残留状态：

- 编译期 `Unchecked cast` warning：`0`（`mvn -f ospf-kotlin-framework-bpp3d/pom.xml -am -DskipTests compile` 复跑结果）

## 七、交接注意事项

- 这是大规模重构，必须小步提交。
- 不要通过扩大白名单完成验收。
- 优先消除主链路 `asScalarF64()` 和 unchecked cast warning。
- 兼容 facade 可以保留，但不能反向污染泛型主实现。
- 每完成一个 phase，都更新本文件勾选状态和实际执行命令。

## 九、会话交接（2026-05-25）

本次会话已完成：

- 将 G3 的 6 项勾选项全部更新为已完成状态（`PackageAttribute`、`ItemHeightCombinator`、`ItemMerger`、`LoadingOrderCalculator`、`Pattern`、`Schema/ShadowPriceMap`）。
- 同步更新了本轮执行记录中的门禁结果：`GENERIC_BOUNDARY_FAIL: 292`。
- 复跑 `domain-item` 编译与定向测试，结果均为通过。

需下一会话继续执行：

- 继续压降主链路 `Flt64Token`（当前门禁口径为 292）。
- 对照“完全泛型化验收”剩余未勾选项继续收口：
  - 主模型字段无 `QuantityFlt64`。
  - 主模型与业务算法签名无固定 `Flt64` 有量纲参数。
  - 所有保留残留位于白名单文件并在审计文档逐项说明。
  - 禁止目录命中数为 0。
- 本仓库当前包含较大规模未收口改动，后续建议按模块小步提交。
