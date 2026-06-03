# BPP3D 形状泛型化与竖直圆柱支持重构交接

日期：2026-05-31
最近更新：2026-06-03

本文档用于交接 BPP3D “形状泛型化 + 竖直圆柱支持”重构。当前状态不是总目标完成，而是长方体主链向 shape-aware 主链迁移后的阶段性收敛。

## 1. 总目标

把 BPP3D 从“长方体作为隐含前提”的主流程，推进为能稳定表达长方体与竖直圆柱的 shape-aware 装载链路。

目标边界：

1. 支持 `Cuboid` 与固定 `Axis3.Y` 的竖直圆柱。
2. 圆柱路径使用真实几何或显式 unsupported，不把外接盒当作最终几何判定。
3. 长方体路径、旧 DTO 字段、旧 CSV 入口和现有应用流程不回退。
4. `Cuboid` 逐步退回为 placement/projection 基础设施兼容类型，不再作为业务层唯一几何真相。
5. 长期目标才考虑去除 `Item : Cuboid<Item>`、重写 `QuantityPlacement2/3` 和 `CuboidView` 体系。

## 2. 已完成摘要

以下只保留结论，不保留逐项实现细节；需要查细节时查看 git 历史和对应测试。

1. 竖直圆柱第一阶段 MVP 已接入主链，shape metadata、真实体积、renderer DTO、loading rate 与 application 入口具备阶段性支持。
2. `PackingShape3` 已成为 domain 层 shape capability 入口，`Item` 已提供从 `packingShape` 派生的形状能力访问方式。
3. 长方体与竖直圆柱的 shape metadata、真实体积、外接盒、底面轮廓和 renderer metadata 已有回归覆盖。
4. 不支持圆柱真实几何的旧算法路径已逐步改为显式 unsupported，避免静默退回外接盒判定。
5. `QuantityPlacement2/3(...)` 直写构造已集中到 `PlacementFactory`，业务调用侧显式 placement 泛型暴露已收敛。
6. `DemandConstraint` / `VolumeMinimization` 已提供 Item 专用顶层入口，application 调用侧不再直接依赖泛型基类 factory。
7. `Cuboid<*>` / `CuboidView<*>` 相关 compat 扩展已开始集中标注，底层结构性绑定仍保留。
8. 外部 `bpp3d-interface-renderer` 已完成 `Cuboid + axis=Y Cylinder` 渲染适配；本仓负责输出 renderer DTO 元数据。
9. 四个门禁脚本、BPP3D 全量测试和 renderer 构建在最近复核中通过；Gurobi / CSV suite 最近一轮未执行，仍列为下一轮完整验收项。

## 3. 当前未完成事项

1. `Item` 仍继承 `Cuboid<Item>`，原因是 placement/projection 基础设施仍依赖 `view()` / `toQuantity()` / `CuboidView` 体系。
2. `QuantityPlacement2<T : Cuboid<T>, P : ProjectivePlane>`、`QuantityPlacement3<T : Cuboid<T>>`、`CuboidView` 仍是基础设施核心类型。
3. `Bin<T : Cuboid<T>>`、`Container3`、`Layer`、`Block` 等结构仍以 cuboid 体系作为 placement 容器基础。
4. `DemandConstraint<Args, T : Cuboid<T>>`、`VolumeMinimization<Args, T : Cuboid<T>>`、shadow price 相关底层泛型约束仍保留。
5. DFS / MLHS / 空间切分等搜索路径对圆柱仍是显式 unsupported，不是真实圆柱几何搜索支持。
6. `CirclePackingLayerGenerator` 仍是初步候选生成器，不是完整可替代的圆柱算法服务。
7. 圆柱 block / layer / packing 结果仍需要更强的真实几何二次校验。
8. 可变半径仍只是 metadata 预留，不参与连续优化或最终半径求解。
9. CSV / Gurobi suite 最近一轮未执行，不能写成本轮通过。
10. 当前工作树存在非 BPP3D 改动，下一轮提交前必须隔离。

## 4. 下一轮目标

下一轮要尽可能多处理结构性缺口，减少后续迭代次数。目标不是完全重写 placement/projection，而是在不推翻现有体系的前提下，把圆柱真实几何闭环、cuboid-only 边界和文档门禁一次性收紧。

优先目标：

1. 修正文档与门禁口径，删除不存在 API 或不可验证完成项。
2. 补齐 circle packing adapter 的真实几何验收。
3. 复查 block / layer / packing / support / merger 的圆柱退化风险。
4. 将 cuboid-only 能力边界显式命名、显式 KDoc、显式 unsupported。
5. 继续降低业务层对 `Cuboid` / `CuboidView` / `QuantityPlacement*` 的直接感知。
6. 补齐 renderer DTO、外部 renderer 示例和 schema 契约验收。
7. 重新执行 Gurobi / CSV suite 或明确记录环境阻塞。
8. 在提交前隔离无关模块改动，只提交 BPP3D 相关变更。

非目标：

1. 不在下一轮去除 `Item : Cuboid<Item>` 继承。
2. 不重写 `QuantityPlacement2/3`、`CuboidView`、projection 基础设施。
3. 不支持横向圆柱、任意角度圆柱或任意 shape。
4. 不实现连续半径全局优化器。
5. 不把外部 renderer 源码纳入本仓。

## 5. 下一轮事项

### 5.1 接手与文档修正

1. 读取 `.rules/chore.md` 和本文档。
2. 执行 `git status --short --branch`，记录 BPP3D 与非 BPP3D 改动。
3. 修正 `refactor.md` 中所有不存在或不可验证的 API 名称。
4. 将历史验证、本轮验证、建议验证分开记录。
5. 删除或改写任何“未执行但写成通过”的测试描述。
6. 检查 README / README_ch / 示例数据与本文档是否口径一致。

### 5.2 硬绑定基线与门禁

1. 重新生成 `Cuboid` / `CuboidView` / `QuantityPlacement2/3` / `AbstractCuboid` 硬绑定快照。
2. 将允许存在的绑定分成基础设施、compat adapter、测试、文档四类。
3. 收紧门禁 allowlist，禁止 application / domain service 新增散落硬绑定。
4. 对 `QuantityPlacement2/3(...)` 直写构造保持只允许 `PlacementFactory`。
5. 增加或更新扫描，禁止文档继续引用不存在 API 作为完成项。
6. 对 `DemandConstraint.forItem` / `VolumeMinimization.forItem` application 调用侧保持禁用。

建议扫描：

```powershell
rg --line-number "\b(Cuboid|AbstractCuboid|CuboidView|QuantityPlacement2|QuantityPlacement3|QuantityRectangle2|QuantityCuboid3)\b" ospf-kotlin-framework-bpp3d --glob "*.kt" --glob "!**/bpp3d-infrastructure/**"
rg --line-number "QuantityPlacement[23]\(" ospf-kotlin-framework-bpp3d --glob "*.kt" --glob "!**/bpp3d-infrastructure/**"
rg --line-number "DemandConstraint\.forItem|VolumeMinimization\.forItem|ItemDemandConstraint<|ItemVolumeMinimization<" ospf-kotlin-framework-bpp3d --glob "*.kt"
```

### 5.3 圆柱真实几何闭环

1. 为 `CirclePackingLayerGenerator` 增加 adapter 级验收。
2. 对 circle packing 输出增加真实几何二次校验：容器内、圆柱间不重叠、与长方体不冲突。
3. 覆盖同层多圆柱、圆柱+长方体混装、边界贴合、半径不同、超出容器等场景。
4. 检查 loading rate、used volume、actual volume 在混装路径中是否始终使用真实体积。
5. 对非 `Axis3.Y` 圆柱保持显式 unsupported，并补测试。
6. 对无法验证真实几何的 block/layer 结果明确拒绝。
7. 复查 `PackingRendererAdapter` 输出的 shape metadata 是否覆盖外部 renderer 所需字段。

### 5.4 support / stacking / hanging 语义复查

1. 梳理 `PackageAttribute.enabledStackingOn(...)` 的真实输入能力：`ItemView`、`ItemPlacement3`、底部支撑、layer、height。
2. 判断 stacking / hanging 当前是否只能安全用于 cuboid footprint。
3. 对圆柱 top/bottom support 若尚无真实策略，保持显式 unsupported 或明确 cuboid-only。
4. 能迁移的判断改用 `shapeFootprint`、`shapeBoundingBox`、`shapeVolume`。
5. 不能迁移的判断补 KDoc 与测试，禁止静默按外接盒通过。
6. 增加混装支撑测试：圆柱在盒体上、盒体在圆柱上、圆柱在圆柱上。

### 5.5 Item / container / compat 边界收口

1. 继续把业务几何访问迁移到 `Item.shapeBoundingBox` / `shapeFootprint` / `shapeVolume`。
2. 检查 `Item`、`ItemContainer`、`Bin`、`Layer`、`Block` 是否还有业务逻辑直接读 `Cuboid.volume` / `width` / `height` / `depth` 当作真实形状。
3. 将确实只为 placement 兼容存在的扩展集中到 compat 区域。
4. 所有 compat 扩展补齐中英双语 KDoc。
5. 移除文档中不存在的 compat 扩展描述。
6. 不新增额外 `ShapeProvider` / `BoundingBoxProvider` 等包装接口，除非能替代真实重复依赖。

### 5.6 ItemMerger / Pattern / cuboid-only 路径

1. 全量复查 `ItemMerger` 的所有入口。
2. 确保混入圆柱时所有 cuboid-only merge 路径都显式拒绝。
3. 复查 `Pattern` 是否隐含长方体 footprint、排列或堆叠假设。
4. 对 cuboid-only API 命名、KDoc、异常信息和测试进行统一。
5. 补充混装负例：圆柱参与 merge、pattern block、hollow square、pile 等路径。
6. 若发现可支持的圆柱子路径，必须加真实几何校验后再开放。

### 5.7 Demand / shadow price / volume 类型链

1. 保持 application 使用 `itemDemandConstraint` / `itemVolumeMinimization`。
2. 检查测试、样例和文档不再推荐业务调用 `DemandConstraint.forItem` / `VolumeMinimization.forItem`。
3. 评估 shadow price key、demand statistics、reduced cost 是否还有几何类型不必要依赖。
4. 若底层 `T : Cuboid<T>` 保留，文档必须说明它是 solver unit 结构约束，不是 shape capability。
5. 增加门禁保护，防止 application 重新引入泛型基类 factory。
6. 保持 Item 专用封装的测试覆盖。

### 5.8 Renderer DTO 与外部 renderer 契约

1. 固化 renderer DTO 字段契约：`shapeType`、`renderShapeType`、`algorithmShapeType`、`radius`、`diameter`、`axis`、`boundingWidth/Height/Depth`、`actualVolume`。
2. 为长方体旧字段兼容、竖直圆柱 metadata、混装 loading rate 增加或保留测试。
3. 输出一份最小混装 JSON fixture，供外部 renderer 使用。
4. 外部 renderer 验收记录只写命令与结果，不把外部源码改动写成本仓修改项。
5. 对 unsupported renderer shape 明确约定：用户可见提示优先，至少不能静默显示为普通长方体。

### 5.9 CSV / Gurobi / application 完整验收

1. 重新执行 application 普通测试链路。
2. 在有 license 和数据集条件时执行 Gurobi 回归。
3. 执行 CSV dataset suite，覆盖旧 CSV、长方体新字段、圆柱字段、非法 shape、非法 axis。
4. 若环境不具备 Gurobi 或 dataset，只记录环境阻塞，不写成通过。
5. 将实际命令、结果、日期写入文档。
6. 验证失败时优先修代码，不用文档掩盖。

## 6. 下一轮整体计划

### 阶段 0：基线确认

1. 记录工作树和分支状态。
2. 隔离非 BPP3D 改动。
3. 跑门禁、diff check、BPP3D 全量测试。
4. 记录当前未提交 BPP3D 文件清单。

### 阶段 1：文档与门禁修正

1. 修正本文档不准确 API 描述。
2. 更新 README / README_ch / 示例数据口径。
3. 收紧硬绑定扫描和 allowlist。
4. 建立本轮实际验证记录模板。

### 阶段 2：圆柱几何闭环

1. 补 circle packing adapter 验收。
2. 补真实几何二次校验。
3. 补混装正例与 unsupported 负例。
4. 确认 renderer DTO 输出完整。

### 阶段 3：cuboid-only 语义收口

1. 收口 `ItemMerger`、`Pattern`、support / stacking / hanging。
2. 给不可支持路径补显式异常与测试。
3. 能迁移的路径改用 shape capability。
4. 保持基础设施兼容，不重写 placement。

### 阶段 4：solver 与 application 调用面收口

1. 复查 demand / shadow price / volume 调用面。
2. 防止 application 重新依赖泛型基类 factory。
3. 补定向测试。
4. 跑 application 普通链路。

### 阶段 5：完整验收与提交

1. 跑必跑命令。
2. 视环境跑 Gurobi / CSV suite。
3. 更新本文档实际结果。
4. 确认只提交 BPP3D 相关改动。
5. 使用具体、完整的提交信息提交。

## 7. 下一轮修改清单

优先检查和修改：

1. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
2. `bpp3d-domain-layer-generation-context/src/test/.../LayerGenerationProgramCandidateAdaptersTest.kt`
3. `bpp3d-domain-layer-generation-context/src/test/.../LayerGenerationFltXProofTest.kt`
4. `bpp3d-domain-block-loading-context/src/main/.../SimpleBlockGenerator.kt`
5. `bpp3d-domain-block-loading-context/src/main/.../CylinderUnsupportedGuard.kt`
6. `bpp3d-domain-block-loading-context/src/test/.../SearchAlgorithmCylinderGuardTest.kt`
7. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
8. `bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
9. `bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
10. `bpp3d-domain-item-context/src/main/.../model/Pattern.kt`
11. `bpp3d-domain-item-context/src/main/.../model/Bin.kt`
12. `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
13. `bpp3d-domain-item-context/src/main/.../model/Block.kt`
14. `bpp3d-domain-item-context/src/main/.../model/PlacementFactory.kt`
15. `bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`
16. `bpp3d-domain-item-context/src/main/.../service/LoadingOrderCalculator.kt`
17. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
18. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/VolumeMinimization.kt`
19. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
20. `bpp3d-domain-packing-context/src/main/.../service/CylinderAxisGuard.kt`
21. `bpp3d-application/src/main/.../service/ColumnGenerationApplicationService.kt`
22. `bpp3d-application/src/main/.../service/ColumnGenerationStandardExecutors.kt`
23. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
24. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
25. `bpp3d-infrastructure/src/main/.../PackingShape.kt`
26. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`
27. `scripts/generic-boundary-check.ps1`
28. `scripts/shape-boundary-check.ps1`
29. `scripts/geometry-boundary-check.ps1`
30. `scripts/geometry-module-dry-run.ps1`
31. `README.md`
32. `README_ch.md`
33. `refactor.md`

建议新增或扩展测试：

1. circle packing adapter 混装验收测试。
2. 圆柱真实几何二次校验测试。
3. support / stacking / hanging 混装负例测试。
4. `Pattern` 圆柱显式拒绝测试。
5. `ItemMerger` 全入口圆柱拒绝测试。
6. renderer DTO fixture round-trip 测试。
7. CSV shape 字段解析和非法输入测试。
8. Gurobi CSV dataset suite 圆柱字段测试。
9. 门禁脚本文档误报测试。

## 8. 验收标准

### 8.1 代码验收

1. `Item : Cuboid<Item>` 若仍保留，文档必须说明它只服务 placement/projection 兼容。
2. application / domain service 不新增散落 `Cuboid` / `CuboidView` / `QuantityPlacement*` 泛型暴露。
3. `QuantityPlacement2/3(...)` 直写构造仍只允许在 `PlacementFactory`。
4. application 不直接调用 `DemandConstraint.forItem` / `VolumeMinimization.forItem`。
5. 圆柱可支持路径必须使用真实几何校验。
6. 不可支持圆柱路径必须显式 unsupported。
7. cuboid-only API 命名、KDoc、异常与测试一致。
8. renderer DTO 对长方体旧字段和圆柱新字段均兼容。
9. 不新增与 BPP3D 无关的改动。

### 8.2 文档验收

1. 已完成事项只保留高层摘要，不恢复实现流水账。
2. 文档不引用当前代码中不存在的 API。
3. 本轮未执行的测试不能写成通过。
4. 历史验证、最近复核、建议验收必须分开。
5. 未完成的结构性绑定必须继续保留。
6. 外部 renderer 状态与本仓 BPP3D 状态分开描述。

### 8.3 必跑命令

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

### 8.4 建议完整验收

```powershell
mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test "-Dgpg.skip=true"
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

外部 renderer 建议验收：

```powershell
cd E:\workspace\ospf\framework\bpp3d-interface-renderer
npm run build
npx vue-tsc --noEmit
cargo check
```

### 8.5 最近复核记录

最近复核日期：2026-06-03

1. `generic-boundary-check.ps1`：通过。
2. `shape-boundary-check.ps1`：通过。
3. `geometry-boundary-check.ps1`：通过。
4. `geometry-module-dry-run.ps1`：通过，保留 8 个内部基线 warning。
5. `git diff --check -- ospf-kotlin-framework-bpp3d`：通过，仅 CRLF 工作区提示。
6. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`：179 tests，0 failures；保留 JVM code heap warning。
7. 外部 renderer `npm run build` / `npx vue-tsc --noEmit` / `cargo check`：通过；保留 Rust static mut warning。
8. Gurobi / CSV suite：最近复核未执行。

## 9. 提交前检查

1. `git status --short --branch` 中只能包含本轮 BPP3D 相关改动。
2. 新增测试文件必须纳入提交。
3. 无关 CSP1D 或其他模块改动不得混入 BPP3D 提交。
4. 提交信息必须具体说明重构目的、关键边界和验证结果。
5. 若 Gurobi / CSV 未执行，提交信息和文档都必须明确说明。
