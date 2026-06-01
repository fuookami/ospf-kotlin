# BPP3D 形状泛型化与竖直圆柱支持重构交接

日期：2026-05-31  
状态：`cylinder.md` 已合并到本文档，后续只维护 `refactor.md`。
最近更新：2026-05-31（阶段 1 完成，阶段 2 完成首版支撑几何收口；阶段 3 补充 DFS/MLHS 空间切分主链圆柱 unsupported 显式门禁与回归；阶段 4 补充 ItemMerger 圆柱 unsupported 门禁、LoadingOrderCalculator 三平面 shape-aware 判定并增加非 `Axis3.Y` 圆柱门禁、需求统计与 layer-assignment 圆柱语义回归，并新增可变半径元数据预留入口第一阶段；阶段 5 补充 README 与示例，完成泛型应用请求 shapeSpec 输入协议第一阶段打通，并补齐 CSV 协议收口与可执行验收记录）。

## 1. 总目标

把 BPP3D 从“长方体作为隐含前提”的主流程，推进为“数值泛型 + 形状泛型”的装载主链，并稳定支持第一阶段竖直圆柱真实几何 MVP。

最终目标包括：

1. 主流程能表达长方体和竖直圆柱，后续可扩展更多 shape。
2. `Cuboid` 只是 shape 的一种实现，不再是 placement、projection、support、container、renderer 的唯一几何真相。
3. 竖直圆柱固定轴向为 `Axis3.Y`，不支持横放、躺放或任意角度旋转。
4. 圆柱边界、碰撞、支撑、体积和 renderer 输出使用真实几何，不把外接盒近似作为最终判定。
5. 保持现有长方体用例、CSV 入口、Gurobi 流程和旧 renderer 兼容字段不回退。

## 2. 已完成摘要

当前已经完成以下方向的基础工作，后续不要重复铺垫：

1. strict generic boundary 门禁已建立并通过。
2. shape boundary 门禁已建立并通过。
3. infrastructure 已有 `PackingShape3`、`ShapeBoundingBox3`、`ShapeFootprint2`、`CuboidPackingShape3`、`CylinderPackingShape3`。
4. 长方体和圆柱已具备基础 shape adapter、真实体积、bounding box 和 footprint 能力。
5. `ShapePlacement3` 已存在，并能用 circle/rectangle footprint 做基础 overlap 判断。
6. container 已新增 shape-aware 边界判断入口，loading rate 可基于真实体积。
7. renderer DTO 已扩展 shape metadata，包含 shape type、algorithm shape type、axis、radius、diameter、bounding box、actualVolume。
8. packing renderer adapter 已能从 item shape metadata 输出长方体和固定半径竖直圆柱字段，并用 actualVolume 计算 loading rate。
9. `Item` 已有 `explicitPackingShape` 作为过渡入口。
10. layer generation 已有委托式 generator 框架，并新增 `CirclePackingLayerGenerator` 的初步矩形/六角圆密排候选。
11. block/layer/renderer/packing 侧已有圆柱轴向门禁，非 `Axis3.Y` 圆柱会被拒绝。
12. 已补充 infrastructure、renderer、packing、block loading、layer generation 的部分圆柱专项测试。
13. `PackageShape` 已新增稳定 `shapeSpec`（`Cuboid` / `VerticalCylinder`），`ActualItem` 已支持 `shapeSpecOverride`。
14. `Item` 已新增统一 `packingShape` 入口，主链 `Packer` / `PackingRendererAdapter` / `SimpleBlockGenerator` / `LayerGenerationContext` 已优先使用该入口。
15. `ShapePlacement3` 已新增 footprint overlap area 计算（圆-圆、圆-矩形、矩形-矩形），`bottomSupport` 已支持 shape resolver，并在 item 堆叠路径注入 `item.packingShape`。
16. `git diff --check` 与四个门禁脚本（generic/shape/geometry/dry-run）在上述改造后继续通过。
17. `ItemMerger` 的旧长方体合并入口已对圆柱 item 给出明确 unsupported，不再静默按长方体尺寸做 pile/block/hollow-square/pattern 合并。
18. `LoadingOrderCalculator` 的 `Bottom` 平面重叠判断已切换为 `ShapePlacement3.footprintOverlapArea`，`Side`/`Front` 平面也已切换为基于 shape bounding 的投影重叠判定，不再依赖 `QuantityPlacement2` 的 cuboid 视图重叠；并对非 `Axis3.Y` 圆柱给出显式 unsupported。
19. 已补圆柱 `DemandStatistics` 回归测试，覆盖 `ItemAmount` / `ItemMaterialAmount` / `ItemMaterialWeight` 与 layer 聚合统计。
20. 已补 layer-assignment `DemandConstraint.extractor` 圆柱语义回归测试，验证圆柱 item 在多模式下仍按原始 item/material 统计键与数值参与影子价格提取。
21. 已新增 `README.md` / `README_ch.md`（中英互链），并补固定半径竖直圆柱输入示例与 renderer DTO 圆柱字段输出示例。
22. `PackageShapeSpec.VerticalCylinder` 已预留可变半径显式入口（`radiusCandidates` / `radiusMin` / `radiusMax` / `radiusWeightFunctionKey`），并增加基本一致性校验；当前求解与渲染仍仅使用已决策 `radius`，不会把候选或范围当最终半径输出。
23. `DepthFirstSearchAlgorithm` / `MultiLayerHeuristicSearchAlgorithm` 已接入 `CylinderUnsupportedGuard`，对圆柱 item 在 DFS/MLHS 空间切分路径给出显式 unsupported，并新增回归测试，避免静默按外接盒进入 cuboid-only 搜索。
24. `GenericPackageShape` 已新增 `shapeSpec`（含 `GenericPackageShapeSpec.VerticalCylinder`）并映射到 `PackageShapeSpec`，`ColumnGenerationGenericApplicationRequest -> toModelRequest` 路径可显式携带圆柱 shape metadata；已补 domain/application 双侧回归测试。
25. `bpp3d-application` 的 Gurobi CSV 分组场景入口已支持 `shape_type` / `radius_meter` / `axis` 列并映射到 `PackageShapeSpec`（含 `VerticalCylinder`），同时兼容旧 6 列协议默认 `Cuboid`；已补 CSV 解析回归测试与数据集样例更新。
26. `bpp3d-application` 的 `material,width,amount` CSV 场景入口已支持 `shape_type` / `radius_meter` / `axis` 列并映射到 `PackageShapeSpec`（含 `VerticalCylinder`），同时兼容旧列协议默认 `Cuboid`；已补协议一致性校验（非法 axis、缺 shape_type 但提供 radius/axis）回归测试，并新增样例数据集文件。
27. 已补充中英 README 的应用级 CSV 输入协议说明（两类 CSV 形态、必填/可选列、shape 列规则、错误语义、样例路径），并在 CSV 解析入口新增 schema 先验校验：当存在 `radius_meter` / `axis` 列时必须存在 `shape_type` 列。
28. `bpp3d-application/src/test/resources/gurobi` 已补齐 mixed-shape 样例：分组分层形态 `grouped-layer-cylinder-mixed-sample.csv` 与物料宽度数量形态 `material-width-amount-cylinder-sample.csv`。
29. CSV suite 加载流程已接入 schema 预检报告：目录扫描与显式路径模式均会先执行表头识别、schema 校验与文件名声明类型校验（`grouped-layer` / `material-width-amount`），不一致时显式拒绝并输出 precheck 日志。
30. 已新增 CSV 协议 smoke 回归测试：样例分组 mixed-shape 文件与样例 material-width-amount mixed-shape 文件可直接完成场景构建（不依赖 Gurobi 求解）；并新增 declared-kind mismatch 拒绝用例。
31. 已完成本仓可执行验收：四个门禁脚本（generic/shape/geometry/dry-run）通过，`git diff --check` 通过（仅 LF/CRLF 警告），`mvn -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true` 通过。
32. 已补充阻塞记录：`mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dgpg.skip=true` 在 `ospf-kotlin-utils` 测试编译失败（`ParallelConcurrencyControlTest.kt` 的 `ULong -> UInt64` 参数类型不匹配）；Gurobi 回归与 CSV suite 受 `ospf-kotlin-core-plugin-gurobi:1.1.0` 私服依赖拉取失败（`poitech_public` connection reset）阻塞。

## 3. 当前未完成事项

当前状态不是“完全泛型化完成”，也不能把 strict scanner 或 shape scanner 通过等同于目标完成。

核心缺口：

1. `QuantityPlacement3<T : Cuboid<T>>` 仍是大量主链 API 的核心类型。
2. `QuantityPlacement2<T : Cuboid<T>, P : ProjectivePlane>` 仍依赖 `CuboidView` 和矩形投影。
3. `Item` 仍继承 `Cuboid<Item>`，`explicitPackingShape` 仍保留兼容，但主链已迁移到 `packingShape` 统一入口。
4. `PackageShape` 已支持 `shapeSpec`；泛型 application 请求路径与 Gurobi CSV 两类测试入口（分组场景、`material,width,amount` 场景）已完成第一阶段接入，并已补协议文档与 schema 门禁；但其余业务入口仍未统一到该元数据。
5. `PackageAttribute`、`Pattern`、`LoadingOrderCalculator` 仍大量使用长方体尺寸和矩形投影语义；`ItemMerger` 已明确拒绝圆柱，但尚未迁移为 shape-aware 合并。
6. block、layer、BLA、DFS、MLHS、layer assignment 主链仍大量使用 `ItemPlacement3` / `QuantityPlacement3<Item>`。
7. 支撑面积已迁移到 shape-aware 几何策略（含圆-圆、圆-矩形），但 `PackageAttribute` 的堆叠业务规则仍有长方体尺寸语义残留。
8. 圆柱业务输入字段中的 Gurobi CSV 两类入口（分组场景、`material,width,amount` 场景）与 README 协议说明已完成第一阶段统一；但其余业务入口、接口层契约及可变半径业务规则仍未定型。
9. 圆密排候选已经初步存在，但还不是独立可替换算法服务，也没有完整 adapter 验收。
10. DFS / MLHS / 空间切分已完成第一阶段门禁化（圆柱显式拒绝），但尚未实现可验证的圆柱真实几何搜索支持。
11. layer assignment 统计约束已补充多 bin / 多 layer / 混合需求模式的系统性回归，但仍未覆盖启用 Gurobi 插件后的完整求解链路回归。
12. 旧 three.js renderer 未适配圆柱新字段（当前仓内未发现 legacy three.js 代码，需外部仓或前端项目协同）。

## 4. 新一轮目标

下一轮会话的目标不是继续补零散字段，而是把“圆柱真实几何入口”从过渡实现推进到主链可验证闭环。

优先目标：

1. 建立稳定的 domain shape metadata：`Item`、`ActualItem`、`PackageShape` 能明确表达长方体和竖直圆柱。
2. 把 container、placement、support 的真实几何判断集中到 shape-aware policy，避免业务模块散落手写半径或矩形判断。
3. 让圆柱在 block、layer、packing 主链中要么走真实几何路径，要么明确 unsupported，不能静默退化为外接盒判定。
4. 固定第一阶段竖直圆柱 MVP 的端到端测试，包括混装、loading rate、renderer DTO、需求统计和 unsupported 路径。
5. 更新门禁 allowlist，让新代码不能继续扩大 `Cuboid`、`CuboidView`、`QuantityRectangle2`、`QuantityCuboid3` 的主链硬绑定。

非目标：

1. 不做横放圆柱。
2. 不做任意 shape 的完整通用装箱。
3. 不把 CSV 扩展作为第一阶段 blocker。
4. 不要求旧 three.js renderer 在本轮完成适配。
5. 不直接引入连续半径全局优化器。

## 5. 建议执行计划

### 阶段 0：接手确认

1. 读取 `.rules/chore.md`。
2. 执行 `git status --short --branch`。
3. 运行门禁脚本，确认当前静态基线。
4. 尝试运行 Maven 测试；如果依赖仓库不可用，记录阻塞，不要把测试通过写入文档。

完成标准：

1. 工作树状态已记录。
2. 门禁脚本结果已记录。
3. Maven 测试能运行或阻塞原因已记录。

### 阶段 1：Domain Shape Metadata 收口

目标：让 item/package 层有稳定 shape 入口。

修改事项：

1. 扩展 `PackageShape` 或新增等价 shape spec，使其能表达 `Cuboid` 和 `VerticalCylinder`。
2. 为 `ActualItem` 增加可选 shape spec 或 shape provider。
3. 保持 width、height、depth 为兼容 bounding box 字段。
4. 让长方体默认 shape metadata 自动生成。
5. 让固定半径竖直圆柱能通过测试 fixture 或 application 构造进入主链。

完成标准：

1. 现有长方体构造方式不破坏。
2. 圆柱 item 不需要匿名覆盖 `explicitPackingShape` 才能表达真实 shape。
3. renderer adapter 从稳定 domain metadata 读取 shape。
4. 固定半径圆柱字段完整：shape type、axis、radius、diameter、actualVolume、bounding box。

### 阶段 2：Shape Geometry Policy 收口

目标：把边界、碰撞、支撑的真实几何判断集中起来。

修改事项：

1. 抽出 containment policy。
2. 抽出 footprint overlap policy。
3. 抽出 support policy 或 support area result。
4. 让 `ShapePlacement3` 承载这些通用判断。
5. 给旧 `QuantityPlacement3` 增加明确 adapter，不在业务层重复写几何判断。

完成标准：

1. 圆柱与圆柱 overlap 使用圆形 footprint。
2. 圆柱与长方体 overlap 使用圆-矩形 footprint。
3. 圆柱与容器边界使用真实半径和高度。
4. 圆柱支撑不使用外接矩形面积冒充真实底面。
5. 旧长方体 placement 行为不回退。

### 阶段 3：Block / Layer / Packing 主链迁移

目标：圆柱路径在主链中可验证，不允许隐式外接盒最终判定。

修改事项：

1. 标记当前长方体专用算法边界。
2. 对圆柱不支持的旧算法返回明确 unsupported。
3. 将可复用 feasibility check 迁移到 shape-aware policy。
4. 完善 `CirclePackingLayerGenerator` 的委托边界和 result adapter。
5. 验证 block、layer、packing 输出不丢失圆柱 shape metadata。

完成标准：

1. 圆柱不会被误识别为真实长方体。
2. 圆柱 block/layer 结果通过真实几何二次校验。
3. 混装测试覆盖长方体 + 竖直圆柱。
4. DFS / MLHS / 空间切分路径要么真实支持圆柱，要么明确拒绝。

### 阶段 4：需求统计与可变半径铺垫

目标：明确圆柱对需求统计和后续可变半径的影响。

修改事项：

1. 检查 `DemandStatistics` 是否受 shape 类型影响。（已完成首轮回归：圆柱 item/layer 在 amount/material amount/material weight 三种模式下语义正确。）
2. 检查 layer assignment 统计约束是否仍按原始 item / material 统计。（已完成首轮回归：`DemandConstraint.extractor` 圆柱语义正确。）
3. 检查 `LoadingOrderCalculator` 对圆柱 placement 的语义。（已完成：三平面重叠判定均切换为 shape-aware 路径，并对非 `Axis3.Y` 圆柱显式拒绝。）
4. 检查 `ItemMerger` 是否跳过圆柱，或只允许同轴同尺寸圆柱合并。（已完成第一阶段：旧合并路径明确拒绝圆柱。）
5. 为可变半径预留 radius candidates / radius range / radius weight function 的显式入口，但不进入连续优化。（第一阶段完成：domain metadata 已提供入口与约束，求解链仍按 fixed-radius 语义运行。）

完成标准：

1. 圆柱 item 的 amount demand 正确。
2. 圆柱 item 的 material weight demand 正确。
3. 可变半径不会隐式改变物料语义。
4. 可变半径 renderer 不输出未决策的半径范围作为最终半径。

### 阶段 5：文档、示例和门禁更新

目标：让实现、文档和自动化约束一致。

修改事项：

1. 更新 BPP3D README 或示例，说明竖直圆柱是真实几何，不是外接盒近似。（已完成）
2. 添加固定半径竖直圆柱输入示例。（已完成）
3. 添加圆柱 renderer DTO 输出示例。（已完成）
4. 更新 `shape-boundary-check.ps1` allowlist。
5. 移除文档中已经过时的 `cylinder.md` 引用。

完成标准：

1. 新文档只以 `refactor.md` 追踪重构状态。
2. README / 示例没有宣称未实现能力。
3. 门禁能防止新增主链硬绑定。

## 6. 重点修改清单

优先阅读和修改以下文件。

Infrastructure：

1. `bpp3d-infrastructure/src/main/.../PackingShape.kt`
2. `bpp3d-infrastructure/src/main/.../Placement.kt`
3. `bpp3d-infrastructure/src/main/.../Container.kt`
4. `bpp3d-infrastructure/src/main/.../Projection.kt`
5. `bpp3d-infrastructure/src/main/.../GenericProjectionPlacementCore.kt`
6. `bpp3d-infrastructure/src/main/.../GenericContainerCore.kt`
7. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`

Domain item：

1. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
2. `bpp3d-domain-item-context/src/main/.../model/Package.kt`
3. `bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
4. `bpp3d-domain-item-context/src/main/.../model/Pattern.kt`
5. `bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`
6. `bpp3d-domain-item-context/src/main/.../service/LoadingOrderCalculator.kt`

Domain block / layer / packing：

1. `bpp3d-domain-block-loading-context/src/main/.../service/SimpleBlockGenerator.kt`
2. `bpp3d-domain-block-loading-context/src/main/.../model/Block.kt`
3. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
4. `bpp3d-domain-layer-assignment-context/src/main/...`
5. `bpp3d-domain-bla-context/src/main/...`
6. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
7. `bpp3d-application/src/main/...`

Scripts and docs：

1. `ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1`
2. `ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1`
3. `ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1`
4. `ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1`
5. `ospf-kotlin-framework-bpp3d/refactor.md`

## 7. 测试清单

基础门禁：

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1
git diff --check
```

默认测试：

```powershell
mvn -f ospf-kotlin-framework-bpp3d/pom.xml test "-Dgpg.skip=true"
```

应用链路测试：

```powershell
mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dgpg.skip=true
```

Gurobi 回归：

```powershell
mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
```

CSV suite：

```powershell
mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

圆柱专项至少覆盖：

1. 圆柱真实体积。
2. 竖直圆柱 bounding box。
3. 圆柱与圆柱真实 footprint overlap。
4. 圆柱与长方体真实 footprint overlap。
5. 圆柱与容器边界。
6. 圆柱支撑语义。
7. 圆柱 renderer DTO 输出。
8. 圆柱 loading rate 使用真实体积。
9. 长方体 + 圆柱混装。
10. 非 `Axis3.Y` 圆柱被拒绝。
11. 圆柱需求统计。
12. 圆柱 unsupported 路径不会静默退化。

## 8. 总体验收标准

只有同时满足以下条件，才可以把本重构标记为完成：

1. `Item` / `PackageShape` 有稳定 shape metadata，不再只能表达 width、height、depth。
2. placement / projection / container / support 主链存在 shape-aware API，并被圆柱路径实际使用。
3. 长方体通过 adapter 接入新抽象，现有公开行为不破坏。
4. 竖直圆柱固定 `Axis3.Y`，横放和躺放路径明确拒绝。
5. 圆柱边界、碰撞、支撑、loading rate 使用真实几何。
6. 圆柱 renderer DTO 输出完整 shape metadata。
7. 圆柱不会在 block、layer、packing、DFS、MLHS 中静默按外接盒作为最终判定。
8. 需求统计和 layer assignment 对圆柱 item 语义正确。
9. shape-hard-binding 门禁覆盖新增主链代码。
10. 默认长方体回归、圆柱专项测试、Gurobi 回归和 CSV suite 均通过，或环境阻塞被明确记录。
11. README / 示例 / `refactor.md` 与实现状态一致，没有把未完成能力写成已完成。

## 9. 提交前清单

1. 不要把 scanner 通过写成完全泛型化完成。
2. 不要把圆柱外接盒当最终几何判定。
3. 不要删除旧长方体 API 后再一次性修全仓库。
4. 不要把 renderer DTO 新字段设为非空必填。
5. 不要在 layer/block 旧算法里硬塞无法验证的圆柱特殊分支。
6. 新 Kotlin 注释必须中英双语。
7. 超过 2 个参数的函数调用使用多行命名参数。
8. `git diff --check` 必须通过。
9. 修改 shape / placement / support 时必须补圆柱几何测试。
10. 修改 renderer DTO 时必须补长方体兼容和圆柱输出测试。
11. Maven 测试跑不起来时，必须记录具体依赖或环境阻塞。
12. commit message 必须具体说明改动目的和关键变更点。

## 10. 未完成事项交接（下一会话入口）

以下事项截至 2026-06-01 仍未完成，下一会话请按顺序继续：

1. 旧 three.js renderer 的圆柱字段适配未完成（当前仓内未发现 legacy three.js 代码，需外部仓或前端项目协同）。
2. 主链仍存在 `Cuboid` / `QuantityPlacement*` 硬绑定，尚未完成“真实 shape 主链化”。
3. 应用层 `QuantityPlacement3` 直写构造已完成收口：主链与测试夹具已迁移到显式 adapter（含无门禁测试专用入口 `toLayerPlacementWithoutAxisGuard`），仅保留 adapter 内部统一构造。

### 10.0 本轮范围说明（2026-06-01）

1. 用户确认：旧 three.js renderer 不在本轮 `refactor.md` 执行范围，不作为本轮验收阻塞项。
2. 本轮重点继续执行第 2、3 项：应用层收口完成，主链硬绑定持续压缩（block-loading 子域新增统一 placement 工厂并替换散落构造；`domain-item-context` 的 `Item/PackageAttribute/Pattern` 已补齐 Bottom 平面重叠 helper 与 `placement2Of` 构造收口，减少 `QuantityPlacement2(..., Bottom)` 业务层散点；`LoadingOrderCalculator/ItemMerger` 已新增并接入 `placement3Of`，继续压缩业务层 `QuantityPlacement3(...)` 直写构造；`Bin/Block/Item/PlacementTyping/QuantityDomainModels` 与 `domain-bla-context` 的 `BottomUpLeftJustifiedAlgorithm` 已接入 `placement2Of/placement3Of`，进一步收口主链构造散点）。

### 10.1 当前可确认的阻塞

1. 当前环境未复现 `ospf-kotlin-utils` 编译阻塞与 Gurobi 私服依赖拉取阻塞：
   - `mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test "-Dgpg.skip=true"` 已通过。
   - 两条 Gurobi 验收命令均可执行；`CSV suite` 目录属性在相对路径场景下已由测试内路径解析兼容。
2. 仍有执行注意事项：
   - 在 PowerShell 中建议对 `-D...` 参数使用引号（例如 `"-Dbpp3d.gurobi.cg.test.enabled=true"`），避免被误解析为生命周期阶段。
   - 不要并行执行两条 Maven 全链路命令，避免 `target/` 目录互相清理导致的瞬时编译异常（如 `generated-sources/annotations` 缺失）。

### 10.2 下一会话建议执行顺序

1. 重跑以下四条命令并记录结论（PowerShell 下建议给每个 `-D` 参数加引号）：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`
   - `mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dgpg.skip=true`
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true`
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`
2. 在 `ospf-kotlin-framework-bpp3d` 范围内继续收敛剩余 `Cuboid` / `QuantityPlacement*` 硬绑定散点（优先 `domain-item-context` 其余 service 路径与 `domain-layer-generation-context`）。
3. 如果后续会话涉及 three.js，请按“本轮非阻塞项”策略单独立项，并补充外部仓路径/负责人/交付接口。

### 10.3 本轮门禁与剩余绑定快照（2026-06-01）

1. 四个门禁脚本在本轮收口后均通过：
   - `generic-boundary-check.ps1`：`STRICT_GENERIC_BOUNDARY_PASS`
   - `shape-boundary-check.ps1`：`SHAPE_BOUNDARY_PASS`
   - `geometry-boundary-check.ps1`：`GEOMETRY_BOUNDARY_PASS`
   - `geometry-module-dry-run.ps1`：`GEOMETRY_MODULE_DRY_RUN_PASS`（`WARNINGS=8`，`INTERNAL_BASELINE_OK=8`）
2. `shape-boundary-check.ps1` 已补充 allowlist，允许“集中构造入口”持有最小 `Cuboid` / `CuboidView` 绑定：
   - `bpp3d-domain-item-context/.../model/PlacementFactory.kt`
3. `bpp3d-domain-block-loading-context` 已删除本地 `BlockPlacementFactory`，并统一改为复用 `domain-item-context` 的 `placement3Of` 构造入口，减少重复 `Cuboid` 绑定面。
4. 当前 `src/main`（排除 `bpp3d-infrastructure`）内 `QuantityPlacement2/3(...)` 直写构造已收敛到统一工厂入口，不再在业务主链散落直写。
5. `BottomUpLeftJustifiedAlgorithm` 已移除仅用于类型回包的 `invokeT` 重载，保留星号泛型入口 `invoke(originProjections: List<Projection<*, P>>)`，进一步减少业务侧公开 `T : Cuboid<T>` 绑定面。
6. `DemandConstraint` / `VolumeMinimization` 已新增 item 专用构造入口（`forItem`），`bpp3d-application` 与 layer-assignment 回归测试调用已迁移，减少调用侧显式 `<..., Item>` 泛型噪声。
7. `PlacementPlaneMapping` 已移除文件内私有桥接函数的 `T : Cuboid<T>` 泛型显式约束，改为基于 `plane` 的受控类型收敛；相关 item-context 回归测试与门禁通过。
8. 当前第 2 项状态：`shape-aware policy` 已可用并持续接管主链调用，但“完全去除主链 `Cuboid` 泛型硬绑定”尚未完成；剩余绑定主要集中在 `Bin` 类定义、`DemandConstraint`/`VolumeMinimization` 类型定义本体，以及统一工厂 `PlacementFactory` 的必要边界。
9. `Bin` 调用面别名收口已完成第一阶段：`Bin.kt` 新增 `typealias BlockBin = Bin<Block>`，`layer-assignment` 与 `block-loading` 主链调用已从 `Bin<BinLayer>` / `Bin<Block>` 迁移至 `LayerBin` / `BlockBin`，减少调用侧显式 `Cuboid` 绑定噪声。
10. 本轮曾出现 `LayerBin` 未导入导致的编译失败（`Assignment.kt`、`Capacity.kt` 及 `service/limits/*` 多文件）；已补齐 import 并恢复编译通过，`bpp3d-domain-layer-assignment-context` 现可稳定通过 `compile`。
11. 本轮回归验证（仅 `ospf-kotlin-framework-bpp3d` 范围）已通过：
    - `mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am test "-Dtest=PreciseLoadMultiBinAggregationTest,ItemDemandConstraintModeKeyTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dgpg.skip=true"`
    - `mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context -am test "-Dtest=SearchAlgorithmCylinderGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dgpg.skip=true"`
    - `mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test "-Dtest=ColumnGenerationAlgorithmTest,ColumnGenerationPackingAnalyzerGenericEntryPointTest,MaterialPackingApplicationIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dgpg.skip=true"`
    - `generic-boundary-check.ps1` / `shape-boundary-check.ps1` 通过，`git diff --check -- ospf-kotlin-framework-bpp3d` 仅 CRLF 警告、无格式错误。
12. 测试调用面继续收敛：`bpp3d-application` 与 `bpp3d-domain-packing-context` 的测试辅助与断言类型已从 `Bin<BinLayer>` 迁移为 `LayerBin`；当前仓内剩余 `Bin<BinLayer>` 仅在 `Bin.kt` 的 `typealias LayerBin = Bin<BinLayer>` 定义与本交接文档说明中出现。
13. `QuantityPlacement3<BinLayer>` 调用面继续收敛：application 与 layer-assignment 的主链/测试调用已迁移到 `BinLayerPlacement`；当前仓内剩余仅在 `Layer.kt` 中 `typealias BinLayerPlacement = QuantityPlacement3<BinLayer>` 定义出现。
14. `QuantityPlacement3<Item>` 调用面继续收敛：`LayerPlacementAdapter`、`SimpleBlockGenerator`、`QuantityDomainModels` 已迁移为 `ItemPlacement3` 别名，减少业务侧显式泛型标注噪声。
15. 针对上述收敛的回归验证已通过：
    - `mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context -am test "-Dtest=SearchAlgorithmCylinderGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dgpg.skip=true"`
    - `mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test "-Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dgpg.skip=true"`
    - `generic-boundary-check.ps1` / `shape-boundary-check.ps1` 通过，`git diff --check -- ospf-kotlin-framework-bpp3d` 仅 CRLF 警告、无格式错误。
16. `QuantityPlacement2<*, P>` 调用面已在业务主链完成第二阶段收口：`Item.kt` 新增统一别名 `AnyPlacement2<P>`（以及 `AnySidePlacement2` / `AnyFrontPlacement2`），`DemandStatistics`、`ItemContainer`、`PlacementPlaneMapping`、`BottomUpLeftJustifiedAlgorithm` 已迁移到该别名体系，减少主链星号泛型直写噪声。
17. 本轮收口后，`rg --line-number "QuantityPlacement2<\\*," ospf-kotlin-framework-bpp3d --glob "*.kt" --glob "!**/bpp3d-infrastructure/**"` 扫描结果仅剩别名定义处（`Item.kt`），未再出现业务调用侧直写。
18. 针对第 16-17 项回归验证已通过：
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am test -Dtest=PreciseLoadMultiBinAggregationTest,ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context -am test -Dtest=SearchAlgorithmCylinderGuardTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `generic-boundary-check.ps1` / `shape-boundary-check.ps1` 通过，`git diff --check -- ospf-kotlin-framework-bpp3d` 仅 CRLF 警告、无格式错误。
19. `ItemContainer` 调用面继续收敛：新增 `ItemContainerPlacement2/3`、`ItemContainerSidePlacement2`、`ItemContainerFrontPlacement2` 别名，并迁移 `ItemContainer.kt` 内对应扩展函数签名，进一步压缩业务侧 `QuantityPlacement2<S, ...>` / `QuantityPlacement3<S>` 泛型噪声。
20. 针对第 19 项回归验证已通过：
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context -am test -Dtest=SearchAlgorithmCylinderGuardTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `generic-boundary-check.ps1` / `shape-boundary-check.ps1` 通过，`git diff --check -- ospf-kotlin-framework-bpp3d` 仅 CRLF 警告、无格式错误。
21. `QuantityPlacement3<*>` 调用面在 `domain-item-context` 完成第二阶段收口：`LoadingOrderCalculator`、`ItemMerger.dump`、`PlacementFactory`、`PlacementTyping`、`DemandStatistics`、`Bin`、`Layer`、`ItemContainer`、`Item` 的业务签名与扩展已迁移到 `AnyPlacement3` 别名，减少主链星号泛型直写噪声。
22. 本轮收口后，`rg --line-number "QuantityPlacement3<\\*>" ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item --glob "*.kt"` 扫描结果仅剩 `Item.kt` 的 `typealias AnyPlacement3 = QuantityPlacement3<*>` 定义。
23. 针对第 21-22 项回归验证已通过：
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context -am test -Dtest=SearchAlgorithmCylinderGuardTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am test -Dtest=PreciseLoadMultiBinAggregationTest,ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `generic-boundary-check.ps1` / `shape-boundary-check.ps1` 通过，`git diff --check -- ospf-kotlin-framework-bpp3d` 仅 CRLF 警告、无格式错误。
24. 测试侧 `QuantityPlacement3(...)` 调用面继续收口：`PreciseLoadMultiBinAggregationTest` 与 `PackerAndRendererAdapterTest` 已改为统一使用 `placement3Of(...)`，并补齐相应 import，消除测试编译期的 `Unresolved reference 'placement3Of'` 与连带类型推断噪声。
25. 针对第 24 项回归验证已通过：
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am test -Dtest=PreciseLoadMultiBinAggregationTest,ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context -am test -Dtest=SearchAlgorithmCylinderGuardTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-packing-context -am test -Dtest=PackerAndRendererAdapterTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `pwsh -File .\\ospf-kotlin-framework-bpp3d\\scripts\\generic-boundary-check.ps1`：`STRICT_GENERIC_BOUNDARY_PASS`
    - `pwsh -File .\\ospf-kotlin-framework-bpp3d\\scripts\\shape-boundary-check.ps1`：`SHAPE_BOUNDARY_PASS`
    - `git diff --check -- ospf-kotlin-framework-bpp3d` 仅 CRLF 警告、无格式错误。
26. layer-assignment / application 调用面继续收口：`DemandConstraint.forItem(...)` 返回类型已收口为 `ItemDemandConstraint<Args>`，并新增 `ItemVolumeMinimization<Args>` 别名；`ColumnGenerationStandardExecutors` 的 `RmpArtifacts.demandConstraint` 已迁移为 item 专用别名类型，减少应用层 `DemandConstraint<..., Item>` 泛型直写暴露。
27. 针对第 26 项回归验证已通过：
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am test -Dtest=ItemDemandConstraintModeKeyTest,PreciseLoadMultiBinAggregationTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am test -Dtest=ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `pwsh -File .\\ospf-kotlin-framework-bpp3d\\scripts\\generic-boundary-check.ps1`：`STRICT_GENERIC_BOUNDARY_PASS`
    - `pwsh -File .\\ospf-kotlin-framework-bpp3d\\scripts\\shape-boundary-check.ps1`：`SHAPE_BOUNDARY_PASS`
    - `git diff --check -- ospf-kotlin-framework-bpp3d` 通过（本轮无新增格式告警输出）。
26. `Bin` 调用面类型签名继续收口：`Bin.kt` 中 `override val units` 已从 `List<QuantityPlacement3<T>>` 迁移为 `List<ItemContainerPlacement3<T>>`，与既有别名体系对齐，进一步压缩 `QuantityPlacement3` 在业务类定义层的直接暴露。
27. 针对第 26 项回归验证已通过：
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am test -Dtest=PreciseLoadMultiBinAggregationTest,ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context -am test -Dtest=SearchAlgorithmCylinderGuardTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `pwsh -File .\\ospf-kotlin-framework-bpp3d\\scripts\\generic-boundary-check.ps1`：`STRICT_GENERIC_BOUNDARY_PASS`
    - `pwsh -File .\\ospf-kotlin-framework-bpp3d\\scripts\\shape-boundary-check.ps1`：`SHAPE_BOUNDARY_PASS`
    - `git diff --check -- ospf-kotlin-framework-bpp3d` 仅 CRLF 警告、无格式错误。
28. Item 专用约束/目标类型别名继续收口并用于主链显式类型：`DemandConstraint.forItem` 返回类型改为 `ItemDemandConstraint<Args>`，`VolumeMinimization.forItem` 返回类型改为 `ItemVolumeMinimization<Args>`；`ColumnGenerationStandardExecutors.RmpArtifacts` 的 `demandConstraint` 字段已迁移为 `ItemDemandConstraint<BPP3DShadowPriceArguments>`，进一步降低 application 侧对 `DemandConstraint<..., Item>` 的显式泛型暴露。
29. 为避免别名回包触发弃用告警，`ItemDemandConstraint<Args>` 已从“弃用兼容别名”调整为正式别名（保留 `ItemDemandShadowPriceKey` 的弃用兼容声明），`bpp3d-domain-layer-assignment-context` 定向回归可稳定通过。
30. 针对第 28-29 项回归验证已通过：
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am test -Dtest=ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
    - `mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true`
31. 下会话可继续项（不改变 10.2 原顺序）：在 `ospf-kotlin-framework-bpp3d` 范围内继续收敛剩余 `Cuboid` / `QuantityPlacement*` 硬绑定，优先 `DemandConstraint` / `VolumeMinimization` 定义本体与 `PlacementFactory` 必要边界；three.js 仍按“本轮非阻塞项”处理。
