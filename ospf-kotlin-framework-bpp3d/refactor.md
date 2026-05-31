# BPP3D 完全泛型化与圆柱铺垫重构交接

日期：2026-05-31  
当前基线提交：`ed3ed221 fix(bpp3d): 修复主流程收口后的中文注释乱码`
目标文档：`cylinder.md`

## 1. 总目标

本交接文档只服务一个目标：把 BPP3D 从“以长方体为隐含前提的主链”重构为“数值泛型 + 形状泛型”的主链，并为 `cylinder.md` 中第一阶段竖直圆柱真实几何 MVP 做好铺垫。

完全泛型化必须同时满足两层含义：

1. 数值泛型化：核心抽象不把数值类型写死为单一实现；业务兼容层可以继续使用 `InfraNumber`。
2. 形状泛型化：主流程能表达不同三维形状；`Cuboid` 是一个 shape 实现，而不是 placement、projection、support、container、renderer 的唯一几何真相。

圆柱铺垫的第一阶段目标：

1. 支持竖直圆柱，默认轴向为 `Axis3.Y`。
2. 圆柱使用真实 footprint 做边界、碰撞、支撑判断，不接受外接盒近似作为最终判定。
3. 圆柱使用真实体积参与 loading rate、KPI 和 renderer 输出。
4. renderer DTO 能输出 shape type、algorithm shape type、axis、radius、diameter、actualVolume、bounding box。
5. 保持现有长方体行为、CSV 数据入口、默认测试、Gurobi 测试兼容。

## 2. 当前状态

当前源码状态不是“完全泛型化已完成”。上一轮已完成 strict generic boundary 命名收口、中文注释乱码修复和回归验证，但 strict scanner 只能证明旧命名、旧别名和部分数值边界问题已经清零，不能证明主流程摆脱了长方体硬绑定。

已完成事项：

1. `generic-boundary-check.ps1` 当前输出 `STRICT_GENERIC_BOUNDARY_PASS`。
2. `RendererDTO.kt` 已新增 `RenderShapeTypeDTO`、`RenderAxis3DTO`、`RenderAlgorithmShapeTypeDTO`。
3. `RenderLoadingPlanItemDTO` 已新增兼容的 shape metadata 字段。
4. `PackingRendererAdapter` 已对现有长方体输出 `Cuboid` shape metadata、bounding box 和 `actualVolume`。
5. renderer loading rate 已优先使用 `actualVolume`，缺失时回退到 width、height、depth 相乘。
6. `cylinder.md` 的 Renderer DTO 部分已勾选已完成子项，圆柱 item 输出子项仍未完成。

仍未完成事项：

1. `Cylinder` 尚未接入 item、placement、projection、support、container 主链。
2. `QuantityPlacement3<T : Cuboid<T>>` 仍要求实体实现 `Cuboid<T>`。
3. `Item` 仍继承 `Cuboid<Item>`。
4. `PackageShape` 仍以 width、height、depth 为唯一几何输入。
5. 支撑、投影、碰撞、边界判断仍主要依赖 `QuantityRectangle2`、`QuantityCuboid3` 和矩形 overlap。
6. layer、block loading、packing 相关模型仍大量使用 `ItemPlacement3` 和 `QuantityPlacement3<Item>`。
7. 缺少 shape-hard-binding 门禁脚本。
8. 缺少圆柱真实几何专项测试。

## 3. 源码硬绑定清单

交接后的执行会话应优先阅读以下文件，并以这些文件作为重构入口。

Infrastructure：

1. `bpp3d-infrastructure/src/main/.../Cylinder.kt`：已有圆柱基础结构，但未接入主链。
2. `bpp3d-infrastructure/src/main/.../Cuboid.kt`：`AbstractCuboid`、`Cuboid`、`CuboidView` 是当前主几何入口。
3. `bpp3d-infrastructure/src/main/.../Placement.kt`：`QuantityPlacement2<T : Cuboid<T>, P : ProjectivePlane>` 和 `QuantityPlacement3<T : Cuboid<T>>` 是 shape 泛型化的主要阻塞。
4. `bpp3d-infrastructure/src/main/.../GenericProjectionPlacementCore.kt`：已做数值泛型，但边界仍是 `GenericCuboid<T, V>`。
5. `bpp3d-infrastructure/src/main/.../Container.kt`：`Container3<S : Container3<S>> : AbstractCuboid<InfraNumber>`，容器和内部 units 仍绑定长方体 placement。
6. `bpp3d-infrastructure/src/main/.../GenericContainerCore.kt`：已做数值泛型，但仍围绕长方体 geometry view。
7. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`：shape metadata 已开始铺垫，后续圆柱输出仍要继续接入。

Domain item：

1. `bpp3d-domain-item-context/src/main/.../model/Item.kt`：`interface Item : Cuboid<Item>` 是 item 形状泛型化核心阻塞。
2. `bpp3d-domain-item-context/src/main/.../model/Package.kt`：`PackageShape<V>` 仍只表达长方体尺寸。
3. `bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`：额外堆叠规则仍使用 `ItemPlacement3`。
4. `bpp3d-domain-item-context/src/main/.../model/Pattern.kt`：pattern 仍依赖 `ItemPlacement2`、`ItemPlacement3`。
5. `bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`：合并逻辑仍以长方体尺寸为核心。

Domain packing / block / layer：

1. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`：长方体 renderer 已兼容 shape metadata，圆柱输出待接入。
2. `bpp3d-domain-block-loading-context/src/main/.../service/SimpleBlockGenerator.kt`：大量构造 `QuantityPlacement3<Item>`。
3. `bpp3d-domain-block-loading-context/src/main/.../model/Block.kt`：block units 仍使用长方体 placement。
4. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`：layer placement 生成仍以 `ItemPlacement3` 为结果。
5. `bpp3d-domain-layer-assignment-context/src/main/...`：layer/bin/pallet 结构中仍保留 `QuantityPlacement3<*>`。
6. `LoadingOrderCalculator.kt`：装载顺序计算仍使用长方体 placement 空间关系。

## 4. 总体验收标准

另一个会话执行完成后，只有满足以下标准，才可以把 refactor 目标标记为完成：

1. 主 placement/projection/container/support 链路存在 shape-aware 抽象，例如 `PackingShape3`、`ShapePlacement3`、`ShapeFootprint2` 或等价本地命名。
2. `Cuboid` 通过 adapter 或具体 shape 实现接入新抽象，现有公开行为不破坏。
3. `Item` 或 item shape metadata 不再只能表达 width、height、depth。
4. 圆柱第一阶段所需的 shape type、axis、radius、diameter、actualVolume 有稳定 domain 入口。
5. 圆柱 footprint 边界、碰撞、支撑判断从矩形硬编码中抽出，不能只靠外接盒近似。
6. renderer DTO 能完整输出长方体和圆柱的 shape metadata。
7. 新增 shape-hard-binding 门禁，防止新代码继续在主链 API 中硬绑定长方体。
8. 默认长方体回归、Gurobi 回归、CSV suite、圆柱几何专项测试全部通过。
9. `refactor.md` 和 `cylinder.md` 状态同步，不能把未完成事项勾选为完成。

## 5. 阶段 0：接手前确认

目标：确认当前工作树、文档和验证基线，避免在错误状态上继续开发。

执行步骤：

1. 读取 `.rules/chore.md`，确认 Kotlin 注释、参数换行、commit message 等规则。
2. 执行 `git status --short --branch`，记录是否存在未提交改动。
3. 阅读 `cylinder.md` 的阶段目标，尤其是 Renderer DTO、geometry、layer generation、验收清单。
4. 阅读本文件第 1 到第 4 节，确认目标不是 strict scanner 收口，而是 shape-aware 泛型化。
5. 运行当前基线验证命令，确认本地环境可用。

建议命令：

```powershell
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File scripts/generic-boundary-check.ps1 -ProjectRoot .
mvn -f pom.xml -pl bpp3d-application -am test -Dgpg.skip=true
```

完成标准：

1. 工作树状态已记录。
2. strict scanner 可运行。
3. 默认测试可运行，或者已明确记录环境阻塞。
4. 接手会话明确知道当前仍未完成完全泛型化。

## 6. 阶段 1：Shape Boundary 设计与落地

目标：建立 shape-aware 抽象，让后续 placement、projection、support 可以不再直接依赖 `Cuboid`。

建议新增或调整的抽象：

1. `PackingShape3<V>`：三维真实形状抽象。
2. `PackingShapeType`：至少包含 `Cuboid`、`Cylinder`。
3. `PackingAxis3` 或复用现有轴向类型：表达 X、Y、Z。
4. `ShapeBoundingBox3<V>`：兼容旧 width、height、depth。
5. `ShapeFootprint2<V>`：表达底面 footprint。
6. `ShapeGeometryPolicy<V>`：封装 containment、overlap、support 等判定。

最小字段和能力：

1. `shapeType`
2. `boundingWidth`
3. `boundingHeight`
4. `boundingDepth`
5. `actualVolume`
6. `weight`
7. `footprint(plane or axis)`
8. `contains(point)`
9. `overlaps(other)`
10. `supportAreaWith(other)`

实施步骤：

1. 在 infrastructure 层新增 shape 抽象文件，先不要改穿业务主链。
2. 为现有 `Cuboid` 提供 adapter 或默认实现，使长方体可以作为 `PackingShape3` 使用。
3. 为现有 `Cylinder` 提供竖直圆柱 shape 实现或 adapter，先只支持 `Axis3.Y`。
4. 为 shape 抽象新增单元测试，覆盖长方体 bounding box、actualVolume、圆柱 actualVolume、圆柱 footprint 半径。
5. 保持 `Cuboid` 旧 API 不删除，让旧测试继续通过。

完成标准：

1. 新 shape 抽象可编译。
2. 长方体 adapter 与旧 `Cuboid` 行为一致。
3. 圆柱 shape 能表达 radius、diameter、height、axis、actualVolume。
4. 没有修改 solver 行为。
5. infrastructure 测试通过。

## 7. 阶段 2：Placement 与 Projection 泛型化

目标：让 placement/projection 能承载 shape，而不是只承载 `CuboidView<T>`。

建议事项：

1. 新增 `ShapePlacement3<S : PackingShape3<*>>` 或等价类型。
2. 新增 `ShapePlacement2` 或 `ShapeProjection2`，用 `ShapeFootprint2` 表达二维 footprint。
3. 保留 `QuantityPlacement3<T : Cuboid<T>>` 作为兼容层，内部可委托到 shape placement。
4. 保留 `QuantityPlacement2<T : Cuboid<T>, P : ProjectivePlane>` 作为兼容层，避免一次性改穿全部调用方。
5. 将 `contains`、`overlapped`、`absolutePosition`、`maxAbsolutePosition` 等通用能力迁移到 shape placement。
6. 将 `QuantityCuboid3`、`QuantityRectangle2` 的直接使用限制在 cuboid adapter 或 legacy wrapper 内。

实施步骤：

1. 在不删除旧类的前提下新增 shape placement。
2. 给 `QuantityPlacement3` 增加到 shape placement 的转换。
3. 先让长方体 placement 的 `contains`、`overlapped` 通过新策略跑通。
4. 增加圆柱 placement 几何测试：圆柱与圆柱不相交、相切、重叠；圆柱与容器边界；圆柱 actualVolume。
5. 逐步把 domain 层只需要几何判定的地方改用 shape placement 接口。

完成标准：

1. 旧 `QuantityPlacement3` 行为不回归。
2. 新 shape placement 支持长方体和竖直圆柱。
3. 圆柱 overlap 不等于 bounding box overlap。
4. 新测试覆盖圆柱真实 footprint。
5. 默认测试通过。

## 8. 阶段 3：Container 与 Support 泛型化

目标：让容器边界、支撑、堆叠判断使用 shape geometry policy，而不是默认矩形投影。

建议事项：

1. 抽出 container containment policy。
2. 抽出 support policy。
3. 抽出 top/bottom contact policy。
4. 保留现有 `Container3` 长方体容器模型，但内部判断允许 shape placement。
5. `loadingRate` 和 `actualVolume` 统一使用真实体积。

实施步骤：

1. 识别 `Container.kt` 中使用 width、height、depth 判断 unit 是否可放置的入口。
2. 将长方体 unit 的判断迁移为 cuboid shape policy。
3. 增加竖直圆柱在矩形容器内的边界测试：圆心到边界距离必须大于等于 radius。
4. 增加圆柱支撑测试：圆柱底面 footprint 与下层 footprint 的交叠面积或策略结果可计算。
5. 保持旧 `Container3.units: List<QuantityPlacement3<*>>` 兼容，新增 shape units 通道或 adapter。

完成标准：

1. 长方体容器旧测试通过。
2. 圆柱 containment 使用真实半径判断。
3. 圆柱支撑不使用外接矩形面积冒充真实底面。
4. `loadingRate` 可基于圆柱 actualVolume。

## 9. 阶段 4：Item 与 Package Shape Metadata

目标：让 domain item 能表达真实 shape，为后续求解、渲染和 CSV 扩展提供稳定入口。

建议事项：

1. 为 `Item` 增加 shape metadata 或 shape provider。
2. 为 `ActualItem` 增加可选圆柱字段，或引入新的 item shape 数据类。
3. 为 `PackageShape<V>` 增加 shape type、radius、diameter、axis、actualVolume 通道。
4. 保持 width、height、depth 兼容字段，作为 bounding box 或旧 renderer 兼容尺寸。
5. 明确 CSV 暂不强制扩展，第一阶段可以通过 fixture 或 application 构造圆柱 item。

实施步骤：

1. 先新增 shape metadata，不立刻移除 `Item : Cuboid<Item>`。
2. 让长方体 item 默认 metadata 为 `Cuboid`。
3. 增加圆柱 item fixture 或测试用构造器。
4. 将 renderer adapter 从 item metadata 读取 shape 信息，而不是永远写死 `Cuboid`。
5. 增加 serialization 或 DTO 输出测试，确认长方体旧字段不变，圆柱新字段完整。

完成标准：

1. 现有 item 构造方式仍可编译。
2. 长方体 item 输出仍保持旧字段。
3. 圆柱 item 可输出 shapeType、renderShapeType、algorithmShapeType、axis、radius、diameter、actualVolume。
4. 不要求第一阶段完成所有 CSV 协议扩展。

## 10. 阶段 5：Layer、Block、Packing 主链迁移

目标：把主链中只依赖几何关系的地方逐步迁移到 shape-aware API，同时保留长方体优化路径。

高风险文件：

1. `SimpleBlockGenerator.kt`
2. `Block.kt`
3. `LayerGenerationContext.kt`
4. `Layer.kt`
5. `Pattern.kt`
6. `ItemMerger.kt`
7. `LoadingOrderCalculator.kt`
8. `MaterialPacker.kt`

迁移策略：

1. 不要一次性删除 `ItemPlacement3`。
2. 先新增 `ShapeItemPlacement3` 或等价 adapter。
3. 对只支持长方体的算法明确命名，例如 `CuboidBlockGenerator`，避免伪泛型。
4. 对圆柱暂不支持的算法返回明确 unsupported 或走专用 generator，不要静默按外接盒处理。
5. layer generation 预留圆柱委托接口，例如 `CylinderLayerCandidateGenerator`。

实施步骤：

1. 给 block/layer 入口增加 shape capability 判断。
2. 将当前长方体生成器标记为 cuboid-only 实现。
3. 为圆柱第一阶段增加最小可用 generator 或 adapter，只覆盖 `cylinder.md` 要求的 MVP。
4. 在 packing 主链中禁止圆柱走长方体外接盒最终判定。
5. 对 unsupported 路径增加测试，确保不会悄悄退化。

完成标准：

1. 长方体 block/layer 流程不回归。
2. 圆柱不会被误识别为真实长方体。
3. 圆柱路径要么执行真实几何策略，要么明确失败。
4. 第一阶段圆柱 MVP 有完整端到端测试。

## 11. 阶段 6：Renderer 输出与端到端圆柱 MVP

目标：让 packing 结果能输出可渲染的圆柱 shape metadata，并让 loading rate 使用真实体积。

已完成基础：

1. DTO enum 已存在。
2. DTO shape metadata 字段已存在。
3. 长方体输出已保持兼容。

待完成事项：

1. 圆柱 item 输出 `shapeType = Cylinder`。
2. 圆柱 item 输出 `renderShapeType = Cylinder`。
3. 圆柱 item 输出 `algorithmShapeType = VerticalCylinder`。
4. 圆柱 item 输出 `radius`、`diameter`、`axis = Y`。
5. 圆柱 item 输出 `actualVolume`。
6. 圆柱 item 保持 width、height、depth 为旧 renderer 兼容尺寸，即 bounding box。
7. 可变半径圆柱按最终求得的半径逐 item 输出，不输出未决策的半径范围。

实施步骤：

1. 从 item shape metadata 获取 renderer 字段。
2. 增加长方体 renderer adapter 测试，锁定旧字段兼容。
3. 增加圆柱 renderer adapter 测试，锁定新字段。
4. 确认 `RenderLoadingPlanDTO.volume` 和 `loadingRate` 使用真实体积。
5. 同步更新 `cylinder.md` 的 Renderer DTO 勾选项。

完成标准：

1. 长方体 JSON 仍包含旧 renderer 必需字段。
2. 圆柱 JSON 包含所有 shape metadata。
3. 圆柱 loading rate 不再按 bounding box 计算。
4. renderer DTO 测试覆盖长方体与圆柱。

## 12. 阶段 7：门禁脚本与防回归

目标：让“完全泛型化”具备自动化门禁，而不是只靠人工 code review。

建议新增脚本：

```powershell
scripts/shape-boundary-check.ps1
```

建议检查项：

1. 禁止在新增 shape-aware 主链文件中出现 `T : Cuboid<T>`。
2. 禁止在非 adapter 文件中新增 `CuboidView`。
3. 禁止在非 adapter 文件中新增 `QuantityCuboid3`、`QuantityRectangle2` 的核心判定调用。
4. 禁止圆柱路径出现 `BoundingCuboid` 作为最终几何判定。
5. 允许旧兼容文件保留 legacy API，但必须通过 allowlist 管理。

建议 allowlist：

1. `Cuboid.kt`
2. `Placement.kt` 中明确标记的兼容 wrapper。
3. cuboid adapter 文件。
4. 长方体专用 generator。
5. 旧测试中锁定兼容行为的文件。

完成标准：

1. 新脚本能在当前仓库运行。
2. 脚本输出清晰指出违规文件和行号。
3. 默认 CI 或本地验证清单包含该脚本。
4. 新增圆柱相关主链代码不能绕过脚本。

## 13. 测试矩阵

每个阶段至少运行：

```powershell
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File scripts/generic-boundary-check.ps1 -ProjectRoot .
mvn -f pom.xml -pl bpp3d-application -am test -Dgpg.skip=true
```

完成 shape-hard-binding 脚本后增加：

```powershell
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File scripts/shape-boundary-check.ps1 -ProjectRoot .
```

Gurobi 相关阶段运行：

```powershell
mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
```

CSV suite 运行：

```powershell
mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=E:/workspace/ospf-kotlin/ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

圆柱专项测试至少覆盖：

1. 圆柱 actualVolume = `pi * radius * radius * height`。
2. 竖直圆柱 bounding box = `diameter x height x diameter`。
3. 圆柱与圆柱相离、相切、相交。
4. 圆柱与容器边界。
5. 圆柱与长方体 footprint 真实碰撞。
6. 圆柱底部支撑面积或支撑策略结果。
7. 圆柱 renderer DTO 输出。
8. 圆柱 loading rate 使用真实体积。

## 14. 每次提交前清单

提交前必须确认：

1. 没有把未完成事项在 `refactor.md` 或 `cylinder.md` 中标记为完成。
2. 没有把圆柱按 bounding box 当作真实几何判定。
3. 新 Kotlin 注释符合中英双语要求。
4. 函数调用参数超过 2 个时使用多行命名参数。
5. `git diff --check` 通过。
6. 至少运行与改动范围匹配的 Maven 测试。
7. 如果修改了 renderer DTO，要同步测试长方体兼容输出。
8. 如果修改了 shape/placement/support，要新增或更新圆柱几何测试。
9. commit message 要具体说明改动目的和关键变更点。

## 15. 不应做的事

1. 不要把 strict scanner 通过写成完全泛型化完成。
2. 不要删除旧长方体 API 后再尝试一次性修全仓库。
3. 不要让圆柱静默走长方体外接盒最终判定。
4. 不要把 renderer DTO 新字段设为非空必填后破坏旧消费者。
5. 不要把 CSV 协议扩展作为第一阶段 blocker。
6. 不要在 layer/block 旧算法中硬塞圆柱特殊分支，优先建立委托接口。
7. 不要只写文档不加测试就勾选实现完成。

## 16. 建议执行顺序

推荐另一个会话按以下顺序推进：

1. 阶段 0：确认当前工作树与验证基线。
2. 阶段 1：新增 shape boundary 和 cuboid/cylinder adapter。
3. 阶段 2：新增 shape placement/projection，并让旧 placement 委托或可转换。
4. 阶段 3：抽出 container/support policy。
5. 阶段 4：为 item/package 增加 shape metadata。
6. 阶段 6：先完成 renderer 圆柱输出测试，让可观测结果稳定。
7. 阶段 5：再迁移 layer/block/packing 主链，避免前面没有稳定 shape 数据入口。
8. 阶段 7：补上 shape-hard-binding 门禁。
9. 最后运行完整测试矩阵，并同步 `refactor.md`、`cylinder.md` 状态。

阶段 5 和阶段 6 的顺序可以根据实际阻塞互换，但不要在没有真实 shape metadata 的情况下开始大规模迁移 block/layer 算法。
