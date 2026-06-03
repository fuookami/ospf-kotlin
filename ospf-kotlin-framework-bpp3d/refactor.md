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
5. 动态直径/半径分两个版本推进：先实现离散动态半径候选生成，再评估连续半径优化原型。
6. 下一阶段开放 `Axis3.X` / `Axis3.Z` 横向圆柱摆放，仍不支持任意角度圆柱。
7. 支持可选的 depth 边界层硬约束：可分别限制第一个 layer 和最后一个 layer 的圆柱轴向或长方体朝向。
8. 长期目标才考虑去除 `Item : Cuboid<Item>`、重写 `QuantityPlacement2/3` 和 `CuboidView` 体系。

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
9. 离散动态半径输入语义已在 `PackageShapeSpec.VerticalCylinder` 闭合，支持显式半径候选、半径区间和直径区间生成 `resolvedRadiusCandidates`，已接入 `CirclePackingLayerGenerator` 候选层生成，并已补齐 Gurobi CSV 场景加载器的动态半径/直径字段解析。
10. 深度边界层轴向/朝向策略已接入 application 最终已放置结果硬校验，支持按 bin 内 depth 坐标识别 first / last layer，并分别校验圆柱 axis 与长方体 orientation。
11. 四个门禁脚本、BPP3D 全量测试和 renderer 构建在最近复核中通过；Gurobi / CSV suite 最近一轮未执行，仍列为下一轮完整验收项。

### 2.1 本轮新增完成事项

本轮完成日期：2026-06-03。

1. Gurobi CSV 场景加载器已支持动态半径/直径字段解析：
   - 保持旧字段 `shape_type`、`radius_meter`、`axis` 兼容。
   - 新增支持 `radius_min` / `radius_min_meter`、`radius_max` / `radius_max_meter`、`radius_step` / `radius_step_meter`。
   - 新增支持 `diameter_min` / `diameter_min_meter`、`diameter_max` / `diameter_max_meter`、`diameter_step` / `diameter_step_meter`。
   - schema 校验已收紧：任一 shape metadata 字段出现但缺 `shape_type` 时拒绝。
   - `radius_meter` 缺失时，可从 `radius_min` 或 `diameter_min / 2` 解析确定半径，并将区间字段透传到 `PackageShapeSpec.VerticalCylinder`。
2. 深度边界层轴向/朝向策略已完成 application 层最终结果硬校验：
   - 新增 `DepthBoundaryLayerOrientationPolicy`。
   - `ColumnGenerationStandardExecutorConfig` 已新增 `depthBoundaryLayerOrientationPolicy`。
   - `ColumnGenerationStandardExecutors.finalSolver()` 已在最终 MILP 求解后对 `collectSelectedBins(...)` 得到的已放置 `LayerBin` 执行校验。
   - 校验按 layer 的 `z` 坐标排序识别 first / last，不按生成顺序判断。
   - 圆柱校验 `resolvedPackingShape().axis`，长方体校验 `unitPlacement.orientation`。
   - `null` 表示不限制；空集合在策略构造阶段报配置错误。
   - 单 layer bin 会同时应用 first 和 last 约束；两侧冲突时不可行。
   - 拒绝信息能区分 `boundary=first/last`，并包含 `layer_z`、`item`、`cylinder_axis` 或 `cuboid_orientation`。
3. 新增并通过 `DepthBoundaryLayerOrientationPolicyTest`：
   - 覆盖未配置不限制、按 `z` 排序识别 first / last、first / last 长方体朝向拒绝、单 layer first/last 同时应用、空集合配置错误、圆柱 axis 通过与拒绝。

### 2.2 下一会话接手提示

1. 不要继续把深度边界层策略描述为 MILP 原生约束；当前只是 application 最终已放置结果硬校验。
2. 深度边界层的 application request / CSV / Gurobi dataset 输入字段尚未实现。下一会话若继续 5.5，应优先设计字段名、解析规则和空集合错误语义，再补定向测试。
3. Gurobi 求解回归 / CSV dataset suite 最近复核未执行，不能写成通过。
4. 当前工作区仍有 CSP1D 无关改动；提交时只 stage `ospf-kotlin-framework-bpp3d`，不要混入 CSP1D。

## 3. 当前未完成事项

1. `Item` 仍继承 `Cuboid<Item>`，原因是 placement/projection 基础设施仍依赖 `view()` / `toQuantity()` / `CuboidView` 体系。
2. `QuantityPlacement2<T : Cuboid<T>, P : ProjectivePlane>`、`QuantityPlacement3<T : Cuboid<T>>`、`CuboidView` 仍是基础设施核心类型。
3. `Bin<T : Cuboid<T>>`、`Container3`、`Layer`、`Block` 等结构仍以 cuboid 体系作为 placement 容器基础。
4. `DemandConstraint<Args, T : Cuboid<T>>`、`VolumeMinimization<Args, T : Cuboid<T>>`、shadow price 相关底层泛型约束仍保留。
5. DFS / MLHS / 空间切分等搜索路径对圆柱仍是显式 unsupported，不是真实圆柱几何搜索支持。
6. `CirclePackingLayerGenerator` 仍是初步候选生成器，不是完整可替代的圆柱算法服务。
7. 圆柱 block / layer / packing 结果仍需要更强的真实几何二次校验。
8. 动态直径/半径目标尚未完全完成：离散候选半径已进入 `CirclePackingLayerGenerator` 的 layer candidate 生成，并能随 placement 输出确定半径/直径；Gurobi CSV 场景加载器已支持动态半径/直径字段解析；连续优化和完整 Gurobi / CSV suite 验收仍未完成。
9. `Axis3.X` / `Axis3.Z` 横向圆柱摆放尚未开放，当前主链多处仍有 `only Axis3.Y` 门禁。
10. 深度方向第一个 layer / 最后一个 layer 的轴向或朝向约束已接入最终已放置 `LayerBin` 结果硬校验；application request / CSV / Gurobi dataset 输入字段和 MILP 原生约束尚未实现。
11. CSV / Gurobi suite 最近一轮未执行，不能写成本轮通过。
12. 当前工作树存在非 BPP3D 改动，下一轮提交前必须隔离。

## 4. 下一轮目标

下一轮要尽可能多处理结构性缺口，减少后续迭代次数。目标不是完全重写 placement/projection，而是在不推翻现有体系的前提下，把圆柱真实几何闭环、cuboid-only 边界和文档门禁一次性收紧。

优先目标：

1. 修正文档与门禁口径，删除不存在 API 或不可验证完成项。
2. 补齐 circle packing adapter 的真实几何验收。
3. 复查 block / layer / packing / support / merger 的圆柱退化风险。
4. 将 cuboid-only 能力边界显式命名、显式 KDoc、显式 unsupported。
5. 继续降低业务层对 `Cuboid` / `CuboidView` / `QuantityPlacement*` 的直接感知。
6. 实现离散动态半径版本：从直径/半径区间和间隔生成候选直径，再生成圆密排候选层。
7. 增加连续半径优化版本的模型设计与原型评估计划，不把它误写成已完成能力。
8. 开放 `Axis3.X` / `Axis3.Z` 横向圆柱摆放：移除已完成校验路径上的 Y-only 门禁，并为未完成路径保留显式 unsupported。
9. 深度边界层约束配置已接入 application 最终已放置结果校验；后续仍需设计 application request / CSV / Gurobi dataset 输入字段，并评估是否进入 MILP 原生建模。
10. 补齐 renderer DTO、外部 renderer 示例和 schema 契约验收。
11. 重新执行 Gurobi / CSV suite 或明确记录环境阻塞。
12. 在提交前隔离无关模块改动，只提交 BPP3D 相关变更。

非目标：

1. 不在下一轮去除 `Item : Cuboid<Item>` 继承。
2. 不重写 `QuantityPlacement2/3`、`CuboidView`、projection 基础设施。
3. 不支持任意角度圆柱或任意 shape；横向圆柱只限 `Axis3.X` / `Axis3.Z` 两种轴向。
4. 不把连续半径全局优化器作为下一轮生产能力；下一轮只做模型设计、边界评估和可选原型。
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
5. 将非 `Axis3.Y` 圆柱拆分处理：`Axis3.X` / `Axis3.Z` 进入横向圆柱专项，其他任意角度继续显式 unsupported。
6. 对无法验证真实几何的 block/layer 结果明确拒绝。
7. 复查 `PackingRendererAdapter` 输出的 shape metadata 是否覆盖外部 renderer 所需字段。

### 5.4 X/Z 横向圆柱摆放

目标：在保持 `Axis3.Y` 竖直圆柱稳定的前提下，开放 `Axis3.X` 与 `Axis3.Z` 横向圆柱摆放。该目标只覆盖轴向与坐标轴平行的圆柱，不覆盖任意角度旋转。

能力边界：

1. `Axis3.Y`：竖直圆柱，平面投影为圆，继续走 circle packing / vertical cylinder 路径。
2. `Axis3.X`：横向圆柱，圆柱轴沿 X 方向，长度占用 X，截面圆位于 YZ 平面。
3. `Axis3.Z`：横向圆柱，圆柱轴沿 Z 方向，长度占用 Z，截面圆位于 XY 平面。
4. 同一个 `BinLayer` 内只允许一种圆柱轴向/朝向，不允许 X/Y/Z 混放在同一 layer。
5. 同一个 bin 内可以包含多个 layer，不同 layer 可以选择不同轴向/朝向。
6. 横向圆柱不能复用竖直圆柱的 XY 平面密排假设，必须单独校验投影、支撑、碰撞和 renderer 朝向。
7. 在 XY / ZY 投影平面完成圆密排后沿投影方向拓展为 3D 圆柱时，必须重新校验全长支撑；圆柱不允许任何形式的悬空。

实现事项：

1. 梳理并分类当前 `only Axis3.Y` 门禁：哪些可以开放 X/Z，哪些必须保留 unsupported。
2. 扩展 `PackingAlgorithmShapeType` 或算法能力标记，区分 `VerticalCylinder`、`HorizontalCylinderX`、`HorizontalCylinderZ`，或明确使用 `BoundingCuboid` + 真实几何校验策略。
3. 完善 `CylinderPackingShape3.boundingBox`、`footprint()`、`actualVolume` 对 X/Y/Z 三轴的语义文档和测试。
4. 增加 X/Z 轴圆柱的 placement 几何校验：容器内、圆柱-圆柱、圆柱-长方体、圆柱-层边界。
5. 复查 `Container3.enabled` / `ShapePlacement3` / footprint overlap 是否能正确表达横向圆柱，不足时新增 shape-aware helper。
6. `LayerGenerationContext` / layer candidate adapter 必须显式记录 layer 轴向，生成候选时按单一轴向成层。
7. layer merge / assignment / packing 只能组合多个 layer，不能把不同轴向的圆柱合并到同一 `BinLayer`。
8. 对 XY / ZY 投影密排后沿轴向拓展得到的圆柱，增加 3D 无悬空校验：圆柱轴向全长都必须有有效支撑或位于底面。
9. 无悬空校验不能只看外接盒，也不能只看投影圆心；必须以真实圆柱 footprint / contact geometry 判定。
10. `SimpleBlockGenerator`、`Packer`、`LayerPlacementAdapter`、`PackingRendererAdapter`、`LoadingOrderCalculator` 逐项拆门禁：完成真实几何校验的路径允许 X/Z，未完成路径继续显式拒绝。
11. support / stacking / hanging 默认不得直接开放 X/Z；必须先定义横向圆柱支撑语义，否则保持 unsupported。
12. renderer DTO 必须输出 `axis = X` / `axis = Z`，外部 renderer 需要按轴向旋转 `CylinderGeometry`。
13. CSV / application / Gurobi 入口允许解析 `X`、`Y`、`Z`，但只有通过真实几何校验的路径才允许生成方案。
14. README / README_ch 明确列出三轴支持矩阵：Y 已支持，X/Z 分模块支持或 unsupported，不得笼统宣称“圆柱任意摆放”。

测试事项：

1. `Axis3.X` bounding box、footprint、actualVolume 测试。
2. `Axis3.Z` bounding box、footprint、actualVolume 测试。
3. X/Z 横向圆柱容器边界测试。
4. X/Z 横向圆柱与长方体不重叠测试。
5. X/Z 横向圆柱与竖直圆柱混装测试。
6. 同一 `BinLayer` 内混入多个圆柱轴向时必须显式拒绝或拆分成多个 layer。
7. 同一 bin 内多个 layer 使用不同轴向时允许通过，并保持 layer 间堆叠/顺序语义正确。
8. XY / ZY 投影密排后沿轴向拓展导致局部悬空的案例必须被拒绝。
9. 圆柱全长完全有支撑或位于底面时允许通过。
10. X/Z support / stacking / hanging 未定义时显式 unsupported 测试。
11. renderer DTO round-trip 覆盖 `axis = X` / `axis = Z`。
12. CSV axis 字段解析覆盖 `X` / `Y` / `Z` 与非法值。

验收边界：

1. X/Z 只能在已有真实几何校验覆盖的路径中开放。
2. 同一 layer 内必须单一轴向；不同轴向只能出现在同一 bin 的不同 layer 中。
3. 圆柱不允许任何形式的悬空；投影平面密排结果沿轴向拓展后必须通过 3D 全长支撑校验。
4. 若某条主链仍缺少真实几何校验或无悬空校验，必须继续报 unsupported，不能退回外接盒最终判定。
5. 外部 renderer 未适配 X/Z 前，本仓只能输出 metadata，不能宣称端到端渲染完成。

### 5.5 深度边界层轴向/朝向约束

目标：提供一个可选配置，用于约束 depth 方向第一个 layer 和最后一个 layer 的允许轴向/朝向。配置存在时必须满足；字段缺失或为 `null` 时不施加额外限制；字段存在但允许集合为空时视为配置错误。

当前进展：

1. 已完成：新增 `DepthBoundaryLayerOrientationPolicy`，支持分别配置 first / last layer 的圆柱允许轴向集合与长方体允许朝向集合。
2. 已完成：`ColumnGenerationStandardExecutors.finalSolver()` 在最终 MILP 求解后，对收集出的已放置 `LayerBin` 按 `z` 坐标识别 first / last layer 并执行硬校验。
3. 已完成：拒绝原因包含 `bin`、`boundary=first/last`、`layer_z`、`item`，以及 `cylinder_axis` 或 `cuboid_orientation`。
4. 未完成：application request / CSV / Gurobi dataset suite 输入字段尚未设计；当前不是 MILP 原生约束建模，也不会在候选生成阶段提前丢弃候选。

配置语义：

1. 约束对象是同一个 bin 内按 depth 方向排序后的第一个 `BinLayer` 和最后一个 `BinLayer`。
2. 圆柱使用轴向约束，允许值为 `Axis3.X`、`Axis3.Y`、`Axis3.Z` 的集合。
3. 长方体使用朝向约束，允许值为现有 `Orientation` 集合，可以配置多个允许朝向。
4. 第一个 layer 和最后一个 layer 分别配置，二者可以不同。
5. 只配置第一个 layer 时，只约束第一个 layer；只配置最后一个 layer 时，只约束最后一个 layer。
6. 字段缺失或为 `null` 时，表示该边界不限制轴向/朝向；字段存在但允许集合为空时，视为配置错误，不能等价为“不限制”。
7. 若同一个边界 layer 同时包含圆柱和长方体，则圆柱检查 axis，长方体检查 orientation；对应允许集合存在时，各自必须落在允许集合中。
8. 该配置不改变“同一 layer 内只允许一种圆柱轴向/朝向”的规则。

实现事项：

1. 已完成：增加配置模型 `DepthBoundaryLayerOrientationPolicy`，包含 `firstLayerAllowedCylinderAxes`、`lastLayerAllowedCylinderAxes`、`firstLayerAllowedCuboidOrientations`、`lastLayerAllowedCuboidOrientations`。
2. 未完成：在 application request / CSV / Gurobi dataset suite 中设计对应输入字段，字段缺失时保持现有行为；字段存在但集合为空时必须给出明确配置错误或不可行结果。
3. 已完成：在 application 最终求解后增加已放置结果硬校验，按 bin 内 depth 坐标排序识别第一个和最后一个 layer，不能按生成顺序判断。
4. 已完成：对第一个/最后一个 layer 执行轴向/朝向集合检查；不满足时返回明确错误原因。
5. 对只有一个 layer 的 bin，同时应用 first 与 last 配置；若两侧配置冲突，则该 layer 必须同时满足两侧，否则不可行。
6. 文档和 README 明确：该约束只作用于 depth 边界层，不约束中间 layer。

测试事项：

1. 未配置时不限制第一个/最后一个 layer。
2. 只配置第一个 layer 时，最后一个 layer 不受限制。
3. 只配置最后一个 layer 时，第一个 layer 不受限制。
4. first/last 配置不同，bin 内多个 layer 分别满足各自约束。
5. 单 layer bin 同时满足 first 和 last 配置时可行。
6. 单 layer bin 无法同时满足 first 和 last 配置时不可行。
7. 圆柱 axis 不在允许集合时拒绝。
8. 长方体 orientation 不在允许集合时拒绝。
9. 允许集合包含多个轴向/朝向时，任一命中即可通过。
10. 字段缺失或为 `null` 时保持原结果；字段存在但集合为空时返回配置错误或不可行。

验收边界：

1. 配置存在时必须满足，不能作为软偏好。
2. 字段缺失或为 `null` 时不能改变原有候选生成和求解结果。
3. 空集合不能被解释为“不限制”；若输入层允许空集合，必须在校验阶段转成明确错误或不可行。
4. 拒绝原因必须可诊断，至少能区分 first layer / last layer、圆柱 axis / 长方体 orientation。

### 5.6 动态直径/半径版本

动态直径/半径必须明确分为两个版本，避免把 metadata 预留误认为已完成能力。

当前进展：

1. 已完成离散动态半径的输入模型与候选生成：`PackageShapeSpec.VerticalCylinder` 支持 `radiusStep`、`diameterMin`、`diameterMax`、`diameterStep`，并通过 `resolvedRadiusCandidates` 输出去重、排序、含上下界的半径候选。
2. `GenericPackageShapeSpec.VerticalCylinder` 已透传上述字段，application generic request 映射到 model 后会保留动态半径配置。
3. 已完成候选半径接入 `CirclePackingLayerGenerator` 的 layer candidate 生成：每个候选半径生成独立 placement-level shape，保持原 `Item` identity，不破坏 demand key 与 shadow price key。
4. 已完成 Gurobi CSV 场景加载器的动态字段解析：支持 `radius_min` / `radius_max` / `radius_step` 与 `diameter_min` / `diameter_max` / `diameter_step`，并保留 `radius_meter`、`shape_type`、`axis` 旧字段兼容。

#### 5.6.1 离散动态半径版本

目标：把直径/半径区间离散化成有限候选，再复用现有 layer candidate 生成、真实几何校验和评分链路。

输入语义：

1. 支持按直径输入区间和间隔，例如：`diameterMin = 300`、`diameterMax = 360`、`diameterStep = 10`。
2. 上述输入必须生成候选直径：`300, 310, 320, 330, 340, 350, 360`。
3. domain 内部可统一换算为半径候选：`150, 155, 160, 165, 170, 175, 180`。
4. 若输入为半径区间，则同理生成半径候选，并按需要输出对应直径。
5. 间隔必须为正数；区间端点必须为正数；`min <= max`；候选生成必须包含上下边界。
6. 若已有显式 `radiusCandidates`，需要明确它和区间生成候选的优先级：建议显式候选优先，区间用于补全或作为另一种输入模式，不能混用出不可解释结果。

实现事项：

1. 已完成：扩展 `PackageShapeSpec.VerticalCylinder`，表达 `diameterMin`、`diameterMax`、`diameterStep` / `radiusMin`、`radiusMax`、`radiusStep`。
2. 已完成：增加候选生成 helper，负责区间合法性校验、候选生成、去重、排序和单位换算。
3. 已完成：`CirclePackingLayerGenerator` 对每个候选半径生成对应 item view / shape / layer candidate。
4. 已完成：动态候选层的 `source` 记录候选半径，方便调试和回归断言。
5. 已完成：每个候选半径生成的层都经过真实几何二次校验。
6. 阶段完成：ranking 已考虑装入数量、真实体积和 shadow score；独立候选半径偏好策略尚未配置化。
7. 已完成：Gurobi CSV 场景加载器支持直径区间和间隔字段，覆盖 `diameter_min`、`diameter_max`、`diameter_step`，同时覆盖半径区间字段 `radius_min`、`radius_max`、`radius_step`；完整 Gurobi / CSV dataset suite 仍未执行。
8. 已完成：renderer DTO 输出最终选中的实际 `radius`、`diameter`、外接尺寸和 `actualVolume`，不只输出区间。

验收样例：

1. 输入直径 `300..360`、间隔 `10`，生成 7 个候选直径。
2. 每个候选直径生成的圆柱实际体积与外接盒体积不同，且 loading rate 使用实际体积。
3. 候选直径越界、间隔为 0、负数半径、空候选都必须显式报错。
4. 混装长方体 + 动态圆柱候选时，最终层通过真实几何校验。

#### 5.6.2 连续半径优化版本

目标：评估是否把半径作为连续变量参与优化。该版本属于独立模型设计与原型，不作为下一轮生产能力默认完成项。

设计事项：

1. 明确连续半径优化的数学模型：变量包括半径、圆心坐标、装入数量或选择变量。
2. 明确约束类型：容器边界、圆-圆不重叠、圆-矩形不重叠、半径上下界、物料需求、体积/重量约束。
3. 评估模型类别：连续非线性、MINLP、二阶锥近似、离散化 MIP 或启发式局部搜索。
4. 评估和现有 CG / layer candidate 体系的耦合方式：作为候选生成器、pricing 子问题，还是独立 heuristic adapter。
5. 明确求解器要求：Gurobi 是否支持目标模型，是否需要非凸参数，是否可接受性能风险。
6. 先做小规模原型：单 bin、单物料、固定数量或最大数量，再扩展混装。
7. 原型输出仍必须转回确定半径和确定 placement，再进入真实几何校验。

验收边界：

1. 连续优化原型不得替代离散动态半径生产路径。
2. 若连续求解不可用，必须回退到离散候选半径路径。
3. 文档必须明确连续优化是否只是原型、是否执行、是否通过。

### 5.7 support / stacking / hanging 语义复查

1. 梳理 `PackageAttribute.enabledStackingOn(...)` 的真实输入能力：`ItemView`、`ItemPlacement3`、底部支撑、layer、height。
2. 判断 stacking / hanging 当前是否只能安全用于 cuboid footprint。
3. 对任意轴向圆柱叠放增加硬约束：只有半径/直径相同的圆柱才允许叠放。
4. 半径/直径比较必须基于归一化后的 quantity 数值，不允许用原始字符串或未换算单位比较。
5. 任意轴向圆柱半径/直径不同但发生叠放时，必须显式拒绝或返回 unsupported，不能按外接盒或更大圆柱 footprint 静默通过。
6. 对圆柱 top/bottom support 若尚无真实策略，保持显式 unsupported 或明确 cuboid-only。
7. 能迁移的判断改用 `shapeFootprint`、`shapeBoundingBox`、`shapeVolume`。
8. 不能迁移的判断补 KDoc 与测试，禁止静默按外接盒通过。
9. 增加混装支撑测试：圆柱在盒体上、盒体在圆柱上、圆柱在圆柱上。

### 5.8 Item / container / compat 边界收口

1. 继续把业务几何访问迁移到 `Item.shapeBoundingBox` / `shapeFootprint` / `shapeVolume`。
2. 检查 `Item`、`ItemContainer`、`Bin`、`Layer`、`Block` 是否还有业务逻辑直接读 `Cuboid.volume` / `width` / `height` / `depth` 当作真实形状。
3. 将确实只为 placement 兼容存在的扩展集中到 compat 区域。
4. 所有 compat 扩展补齐中英双语 KDoc。
5. 移除文档中不存在的 compat 扩展描述。
6. 不新增额外 `ShapeProvider` / `BoundingBoxProvider` 等包装接口，除非能替代真实重复依赖。

### 5.9 ItemMerger / Pattern / cuboid-only 路径

1. 全量复查 `ItemMerger` 的所有入口。
2. 确保混入圆柱时所有 cuboid-only merge 路径都显式拒绝。
3. 复查 `Pattern` 是否隐含长方体 footprint、排列或堆叠假设。
4. 对 cuboid-only API 命名、KDoc、异常信息和测试进行统一。
5. 补充混装负例：圆柱参与 merge、pattern block、hollow square、pile 等路径。
6. 若发现可支持的圆柱子路径，必须加真实几何校验后再开放。

### 5.10 Demand / shadow price / volume 类型链

1. 保持 application 使用 `itemDemandConstraint` / `itemVolumeMinimization`。
2. 检查测试、样例和文档不再推荐业务调用 `DemandConstraint.forItem` / `VolumeMinimization.forItem`。
3. 评估 shadow price key、demand statistics、reduced cost 是否还有几何类型不必要依赖。
4. 若底层 `T : Cuboid<T>` 保留，文档必须说明它是 solver unit 结构约束，不是 shape capability。
5. 增加门禁保护，防止 application 重新引入泛型基类 factory。
6. 保持 Item 专用封装的测试覆盖。

### 5.11 Renderer DTO 与外部 renderer 契约

1. 固化 renderer DTO 字段契约：`shapeType`、`renderShapeType`、`algorithmShapeType`、`radius`、`diameter`、`axis`、`boundingWidth/Height/Depth`、`actualVolume`。
2. 为长方体旧字段兼容、竖直圆柱 metadata、混装 loading rate 增加或保留测试。
3. 输出一份最小混装 JSON fixture，供外部 renderer 使用。
4. 外部 renderer 验收记录只写命令与结果，不把外部源码改动写成本仓修改项。
5. 对 unsupported renderer shape 明确约定：用户可见提示优先，至少不能静默显示为普通长方体。

### 5.12 CSV / Gurobi / application 完整验收

1. 重新执行 application 普通测试链路。
2. 在有 license 和数据集条件时执行 Gurobi 回归。
3. 执行 CSV dataset suite，覆盖旧 CSV、长方体新字段、圆柱字段、动态直径区间字段、非法 shape、非法 axis。
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
3. 梳理 `Axis3.X` / `Axis3.Z` 横向圆柱真实几何能力和 Y-only 门禁。
4. 实现离散动态半径候选生成：支持直径区间、半径区间和固定间隔。
5. 建立 layer 轴向约束：同一 layer 单一朝向，同一 bin 可包含不同朝向 layer。
6. 深度边界层轴向/朝向约束配置已接入最终已放置结果硬校验：按 bin 内 depth 坐标识别 first / last layer，圆柱检查 axis，长方体检查 orientation；字段缺失或为 `null` 时不限制，空集合视为配置错误。application request / CSV / Gurobi dataset 输入字段与 MILP 原生约束仍未完成。
7. 补混装正例与 unsupported 负例。
8. 确认 renderer DTO 输出轴向、最终选中半径/直径和真实体积。
9. 形成连续半径优化模型设计记录，决定是否进入原型。

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
4. `bpp3d-domain-layer-assignment-context/src/main/.../model/LayerAggregation.kt`
5. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
6. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/VolumeMinimization.kt`
7. `bpp3d-domain-block-loading-context/src/main/.../SimpleBlockGenerator.kt`
8. `bpp3d-domain-block-loading-context/src/main/.../CylinderUnsupportedGuard.kt`
9. `bpp3d-domain-block-loading-context/src/test/.../SearchAlgorithmCylinderGuardTest.kt`
10. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
11. `bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
12. `bpp3d-domain-item-context/src/main/.../model/Package.kt`
13. `bpp3d-domain-item-context/src/main/.../model/QuantityDomainModels.kt`
14. `bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
15. `bpp3d-domain-item-context/src/main/.../model/Pattern.kt`
16. `bpp3d-domain-item-context/src/main/.../model/Bin.kt`
17. `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
18. `bpp3d-domain-item-context/src/main/.../model/Block.kt`
19. `bpp3d-domain-item-context/src/main/.../model/PlacementFactory.kt`
20. `bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`
21. `bpp3d-domain-item-context/src/main/.../service/LoadingOrderCalculator.kt`
22. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
23. `bpp3d-domain-packing-context/src/main/.../service/CylinderAxisGuard.kt`
24. `bpp3d-application/src/main/.../service/ColumnGenerationApplicationService.kt`
25. `bpp3d-application/src/main/.../service/ColumnGenerationAlgorithm.kt`
26. `bpp3d-application/src/main/.../service/ColumnGenerationStandardExecutors.kt`
27. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
28. `bpp3d-application/src/main/.../request-or-csv-mapping/*`
29. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
30. `bpp3d-infrastructure/src/main/.../PackingShape.kt`
31. `bpp3d-infrastructure/src/main/.../Placement.kt`
32. `bpp3d-infrastructure/src/main/.../Container.kt`
33. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`
34. `scripts/generic-boundary-check.ps1`
35. `scripts/shape-boundary-check.ps1`
36. `scripts/geometry-boundary-check.ps1`
37. `scripts/geometry-module-dry-run.ps1`
38. `README.md`
39. `README_ch.md`
40. `refactor.md`

建议新增或扩展测试：

1. circle packing adapter 混装验收测试。
2. 圆柱真实几何二次校验测试。
3. `Axis3.X` / `Axis3.Z` bounding box、footprint、actualVolume 测试。
4. X/Z 横向圆柱容器边界、圆柱-长方体、圆柱-圆柱真实几何测试。
5. X/Z 横向圆柱与 Y 轴竖直圆柱混装测试。
6. 同一 `BinLayer` 内多轴向圆柱混放拒绝或拆分测试。
7. 同一 bin 内多个 layer 使用不同轴向的可行性测试。
8. XY / ZY 投影密排后沿轴向拓展导致局部悬空的拒绝测试。
9. 圆柱全长完全有支撑或位于底面时允许通过测试。
10. X/Z support / stacking / hanging 未定义时显式 unsupported 测试。
11. renderer DTO fixture round-trip 测试，覆盖 `axis = X` / `axis = Y` / `axis = Z`。
12. 离散动态直径候选生成测试：`300..360 step 10` 生成 `300, 310, 320, 330, 340, 350, 360`。
13. 离散动态半径候选生成测试：半径区间、边界包含、去重排序、非法间隔和空候选。
14. 动态半径 circle packing 测试：不同候选半径生成不同 layer candidate，并通过真实几何校验。
15. 任意轴向圆柱相同半径/直径叠放允许测试。
16. 任意轴向圆柱不同半径/直径叠放显式拒绝测试。
17. support / stacking / hanging 混装负例测试。
18. `Pattern` 圆柱显式拒绝测试。
19. `ItemMerger` 全入口圆柱拒绝测试。
20. CSV shape 字段解析、动态直径区间字段、X/Y/Z axis 字段和非法输入测试。
21. Gurobi CSV dataset suite 圆柱字段测试。
22. 连续半径优化原型测试或设计验证记录。
23. 深度边界层约束未配置时不改变 first / last layer 选择结果测试。
24. 只配置 first layer 时，last layer 不受额外限制测试。
25. 只配置 last layer 时，first layer 不受额外限制测试。
26. first / last 配置不同且多 layer 分别满足测试。
27. 单 layer bin 同时应用 first / last 配置，满足时通过、冲突时不可行测试。
28. 圆柱允许多个 axis、长方体允许多个 orientation，任一命中通过测试。
29. 圆柱 axis 或长方体 orientation 不在允许集合时拒绝测试。
30. 深度边界层配置字段存在但集合为空时返回配置错误或不可行测试。
31. 门禁脚本文档误报测试。

## 8. 验收标准

### 8.1 代码验收

1. `Item : Cuboid<Item>` 若仍保留，文档必须说明它只服务 placement/projection 兼容。
2. application / domain service 不新增散落 `Cuboid` / `CuboidView` / `QuantityPlacement*` 泛型暴露。
3. `QuantityPlacement2/3(...)` 直写构造仍只允许在 `PlacementFactory`。
4. application 不直接调用 `DemandConstraint.forItem` / `VolumeMinimization.forItem`。
5. 圆柱可支持路径必须使用真实几何校验。
6. 不可支持圆柱路径必须显式 unsupported。
7. cuboid-only API 命名、KDoc、异常与测试一致。
8. `Axis3.X` / `Axis3.Z` 只能在真实几何校验覆盖的路径中开放，缺校验路径必须继续 unsupported。
9. X/Z 横向圆柱不得复用 Y 轴 circle packing 平面假设作为最终判定。
10. 同一 `BinLayer` 内只允许一种圆柱轴向/朝向；不同轴向不能在同一 layer 内混放。
11. 同一 bin 内允许多个 layer，不同 layer 可以使用不同圆柱轴向/朝向。
12. 圆柱不允许任何形式的悬空；投影平面密排后沿轴向拓展必须通过 3D 全长支撑校验。
13. 任意轴向圆柱叠放只允许相同半径/直径；不同半径/直径必须显式拒绝或 unsupported。
14. 半径/直径一致性比较必须基于归一化 quantity 数值。
15. renderer DTO 必须能表达 `axis = X` / `axis = Y` / `axis = Z`。
16. 离散动态半径支持直径/半径区间和固定间隔，候选生成包含上下边界。
17. 输入直径 `300..360`、间隔 `10` 时必须生成 7 个候选直径：`300, 310, 320, 330, 340, 350, 360`。
18. 动态半径候选进入 circle packing 后，最终输出必须是确定半径/直径、确定 placement 和确定 actual volume。
19. 连续半径优化若未实现生产能力，必须明确保留为设计/原型，不得写成已完成。
20. 深度边界层约束必须按 bin 内 depth 坐标识别 first / last layer，不能按生成顺序判断。
21. 深度边界层约束配置存在时必须满足，字段缺失或为 `null` 时不增加限制。
22. 深度边界层约束字段存在但允许集合为空时，必须返回配置错误或不可行，不能解释为“不限制”。
23. 单 layer bin 必须同时应用 first / last 配置；两侧冲突时不可行。
24. 深度边界层约束中，圆柱检查 axis，长方体检查 orientation，允许集合中任一命中即可通过。
25. renderer DTO 对长方体旧字段和圆柱新字段均兼容。
26. 不新增与 BPP3D 无关的改动。

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
6. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`：200 tests，0 failures；保留 JVM code heap warning。
7. 外部 renderer `npm run build` / `npx vue-tsc --noEmit` / `cargo check`：通过；保留 Rust static mut warning。
8. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am -Dtest=LayerGenerationFltXProofTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：19 tests，0 failures；覆盖 circle packing 真实几何二次校验、混装候选、半径不同、动态半径候选层生成、placement-level shape、renderer DTO 最终半径/直径输出、边界贴合、超出容器与非 Y 轴拒绝。
9. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=PackageShapeSpecTest,GenericDomainAliasExampleTest,MaterialDemandReducedCostTest,DemandStatisticsTest,MixedShapeGeometryTest,ItemMergerCylinderTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：35 tests，0 failures；覆盖动态半径输入语义、generic shape spec 字段透传、圆柱 reduced cost、demand statistics、support / stacking / hanging 真实 footprint，以及 `ItemMerger` / `Pattern` cuboid-only 显式拒绝。
10. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am -Dtest=PackerAndRendererAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：5 tests，0 failures；覆盖 packer / renderer shape metadata、混装真实体积 loading rate 和非 Y 轴拒绝。
11. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-infrastructure -Dtest=PlacementTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：8 tests，0 failures；覆盖圆形 footprint 与矩形 footprint 的 overlap 基础行为。
12. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationGenericShapeSpecEntryPointTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：1 test，0 failures；覆盖 application generic request 到 model 后保留 `radiusStep` / `diameterMin` / `diameterMax` / `diameterStep`。
13. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#groupedLayerCsvShouldMapDynamicDiameterColumnsToVerticalCylinderShapeSpec+materialWidthAmountCsvShouldMapDynamicRadiusColumnsToVerticalCylinderShapeSpec -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：2 tests，0 failures；覆盖 Gurobi CSV 场景加载器的动态直径字段和动态半径字段解析。
14. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#groupedLayerCsvShouldMapVerticalCylinderShapeSpecFromCsvColumns+groupedLayerCsvShouldMapDynamicDiameterColumnsToVerticalCylinderShapeSpec+groupedLayerCsvShouldRemainCompatibleWithLegacySixColumns+materialWidthAmountCsvShouldMapVerticalCylinderShapeSpecFromCsvColumns+materialWidthAmountCsvShouldMapDynamicRadiusColumnsToVerticalCylinderShapeSpec+materialWidthAmountCsvShouldRemainCompatibleWithLegacyColumns+materialWidthAmountCsvShouldRejectInvalidCylinderAxis+materialWidthAmountCsvShouldRejectShapeColumnsWithoutShapeType+groupedLayerCsvShouldRejectSchemaWhenShapeTypeColumnMissingButShapeMetadataColumnsExist+materialWidthAmountCsvShouldRejectSchemaWhenShapeTypeColumnMissingButAxisColumnExists+declaredGroupedLayerScenarioKindShouldRejectMaterialWidthAmountHeader+groupedLayerMixedShapeSampleFileShouldBeParsable+materialWidthAmountMixedShapeSampleFileShouldBeParsable -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：13 tests，0 failures；覆盖旧 CSV 兼容、固定圆柱字段、动态半径/直径字段、非法 axis、缺失 `shape_type`、scenario kind 校验与样例 CSV 可解析性。
15. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=DepthBoundaryLayerOrientationPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：8 tests，0 failures；覆盖深度边界层策略未配置、按 `z` 坐标识别 first / last、first / last 长方体朝向拒绝、单 layer 同时应用 first / last、空集合配置错误，以及圆柱 axis 通过/拒绝。
16. Gurobi 求解回归 / CSV dataset suite：最近复核未执行。

## 9. 提交前检查

1. `git status --short --branch` 中只能包含本轮 BPP3D 相关改动。
2. 新增测试文件必须纳入提交。
3. 无关 CSP1D 或其他模块改动不得混入 BPP3D 提交。
4. 提交信息必须具体说明重构目的、关键边界和验证结果。
5. 若 Gurobi / CSV 未执行，提交信息和文档都必须明确说明。
