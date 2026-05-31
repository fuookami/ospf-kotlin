# BPP3D 形状泛型化与竖直圆柱支持重构交接

日期：2026-05-31  
状态：`cylinder.md` 已合并到本文档，后续只维护 `refactor.md`。
最近更新：2026-05-31（阶段 1 完成，阶段 2 完成首版支撑几何收口）。

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

## 3. 当前未完成事项

当前状态不是“完全泛型化完成”，也不能把 strict scanner 或 shape scanner 通过等同于目标完成。

核心缺口：

1. `QuantityPlacement3<T : Cuboid<T>>` 仍是大量主链 API 的核心类型。
2. `QuantityPlacement2<T : Cuboid<T>, P : ProjectivePlane>` 仍依赖 `CuboidView` 和矩形投影。
3. `Item` 仍继承 `Cuboid<Item>`，`explicitPackingShape` 仍保留兼容，但主链已迁移到 `packingShape` 统一入口。
4. `PackageShape` 已支持 `shapeSpec`，但 CSV / application 输入协议与更完整业务入口仍未统一到该元数据。
5. `PackageAttribute`、`Pattern`、`ItemMerger`、`LoadingOrderCalculator` 仍大量使用长方体尺寸和矩形投影语义。
6. block、layer、BLA、DFS、MLHS、layer assignment 主链仍大量使用 `ItemPlacement3` / `QuantityPlacement3<Item>`。
7. 支撑面积已迁移到 shape-aware 几何策略（含圆-圆、圆-矩形），但 `PackageAttribute` 的堆叠业务规则仍有长方体尺寸语义残留。
8. 圆柱业务输入字段、单位、CSV 协议、可变半径、半径-重量函数仍未定型。
9. 圆密排候选已经初步存在，但还不是独立可替换算法服务，也没有完整 adapter 验收。
10. DFS / MLHS / 空间切分尚未证明不会把圆柱外接盒当最终可行性判定。
11. layer assignment 统计约束尚未系统验证圆柱 item 的原始需求统计语义。
12. 文档、示例和 README 尚未说明竖直圆柱真实几何语义。
13. 旧 three.js renderer 未适配圆柱新字段。

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

1. 检查 `DemandStatistics` 是否受 shape 类型影响。
2. 检查 layer assignment 统计约束是否仍按原始 item / material 统计。
3. 检查 `LoadingOrderCalculator` 对圆柱 placement 的语义。
4. 检查 `ItemMerger` 是否跳过圆柱，或只允许同轴同尺寸圆柱合并。
5. 为可变半径预留 radius candidates / radius range / radius weight function 的显式入口，但不进入连续优化。

完成标准：

1. 圆柱 item 的 amount demand 正确。
2. 圆柱 item 的 material weight demand 正确。
3. 可变半径不会隐式改变物料语义。
4. 可变半径 renderer 不输出未决策的半径范围作为最终半径。

### 阶段 5：文档、示例和门禁更新

目标：让实现、文档和自动化约束一致。

修改事项：

1. 更新 BPP3D README 或示例，说明竖直圆柱是真实几何，不是外接盒近似。
2. 添加固定半径竖直圆柱输入示例。
3. 添加圆柱 renderer DTO 输出示例。
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
