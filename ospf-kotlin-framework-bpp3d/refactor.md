# BPP3D 形状泛型化与圆柱支持重构交接

日期：2026-05-31
最近更新：2026-06-05

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的交接状态。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。当前已完成主链 shape-aware 收敛，但尚未完成 BPP3D 完全泛型化。

## 1. 已完成事项摘要

1. BPP3D 主流程已支持长方体、竖直圆柱，以及已知坐标终态路径下的 X/Z 横向圆柱表达。
2. 圆柱终态碰撞、边界、支撑和 renderer 输出已从外接盒口径收敛到真实几何口径。
3. 旧长方体路径、旧 DTO 字段、旧 CSV 入口、application 普通链路、Gurobi 普通回归和 CSV dataset suite 保持兼容并已复核通过。
4. `PackingShape3` 已成为 domain 层 shape capability 入口，`Cuboid` 已收敛为 placement/projection 兼容基础设施，而非业务唯一几何真相。
5. 业务层部分 `Cuboid<*>`、`CuboidView<*>` 和 `QuantityPlacement*` 暴露已收窄，并由边界脚本保护。
6. renderer DTO、fixture、README、自动构建/类型检查/Rust 检查、DTO 反序列化测试和人工视觉确认已完成阶段性验收。
7. 当前未开放横向圆柱候选生成、block loading、stacking、hanging、circle packing 默认能力，也未开放连续半径优化或 depth boundary MILP 原生下沉。

## 2. 下一轮目标

1. 最大化收窄 application、domain service 和测试辅助入口对 `Cuboid`、`CuboidView`、`AbstractCuboid`、`QuantityPlacement2/3` 等底层兼容类型的感知。
2. 建立清晰的 shape-domain 公开语义，让业务层优先表达 `PackingShape3`、`ItemView`、`ItemCuboid`、domain-specific placement alias 和 shape capability，而不是直接暴露几何基础设施泛型。
3. 将当前不可迁移的 cuboid/placement/projection 结构性绑定分类固化到门禁、KDoc 和 unsupported 语义中，并尽量缩小 allowlist。
4. 系统评估 X/Z 横向圆柱从终态已知坐标路径向低风险路径扩展的可能性；只有真实几何、支撑、碰撞、边界和测试全部闭环时才开放。
5. 统一 application、CSV、Gurobi、depth boundary、renderer 和 README 的 shape contract，避免文档、DTO、测试与实际能力分裂。
6. 一次性扩大回归范围，减少后续迭代次数；任何修改 solver、CSV、shape spec、renderer DTO 或 packing 终态语义的提交都必须触发对应完整验收。

## 3. 下一轮事项

### 3.1 全量边界基线与 allowlist 收敛

**事项**

重新生成 BPP3D shape/generic/geometry 边界基线，按“基础设施结构性绑定、兼容适配、可迁移业务暴露、测试辅助暴露”四类梳理 allowlist。下一轮目标不是简单维持通过，而是推动 allowlist 减少、分类更清楚、违规提示更接近业务意图。

**计划**

1. 复跑 `generic-boundary-check.ps1`、`shape-boundary-check.ps1`、`geometry-boundary-check.ps1` 和 `geometry-module-dry-run.ps1`，保存当前边界基线。
2. 对所有 `Cuboid`、`CuboidView`、`AbstractCuboid`、`QuantityPlacement2/3`、`QuantityCuboid3`、`QuantityRectangle2` 命中项分类。
3. 将可迁移的业务暴露改为 shape-domain 或 item-domain API；只允许基础设施保留必要泛型。
4. 为暂不能迁移的项补充 KDoc、unsupported message 或脚本分类名称，说明为什么必须保留。
5. 增强脚本输出，使新增业务层暴露能直接定位到“应替换为哪个 domain 语义”。
6. 每完成一组迁移后跑相关最小测试，最后跑 BPP3D reactor 全量测试。

**修改清单**

1. `scripts/generic-boundary-check.ps1`
2. `scripts/shape-boundary-check.ps1`
3. `scripts/geometry-boundary-check.ps1`
4. `scripts/geometry-module-dry-run.ps1`
5. `bpp3d-infrastructure/src/main/.../Cuboid.kt`
6. `bpp3d-infrastructure/src/main/.../Placement.kt`
7. `bpp3d-infrastructure/src/main/.../Projection.kt`
8. `bpp3d-infrastructure/src/main/.../GenericProjectionPlacementCore.kt`
9. `bpp3d-infrastructure/src/main/.../GenericContainerCore.kt`
10. `bpp3d-domain-item-context/src/main/.../model/*`
11. `bpp3d-domain-*/src/test/.../*Boundary*Test.kt`

**验收标准**

1. 不新增未分类的 `Cuboid<*>`、`CuboidView<*>`、`AbstractCuboid<...>`、`QuantityPlacement*` 业务层暴露。
2. allowlist 数量不增加；若确需增加，必须有明确分类、KDoc 或 unsupported 语义说明。
3. 边界脚本能区分基础设施绑定和业务暴露，失败信息可直接指导修复方向。
4. 四个边界/几何脚本全部通过。
5. `git diff --check -- ospf-kotlin-framework-bpp3d` 通过。

### 3.2 item-domain 与 shape-domain API 收敛

**事项**

把业务服务、domain model 和测试辅助入口中仍以底层 cuboid/placement 表达的 API，尽量迁移到 `ItemView`、`ItemCuboid`、`ItemPlacement2/3`、`PackingShape3`、`PackageShapeSpec`、shape capability 或更窄的 domain alias。底层 placement/projection 实现不强行重写，但业务边界不得继续以基础设施泛型作为公开语义。

**计划**

1. 梳理 `Item.kt`、`Package.kt`、`PackageAttribute.kt`、`ItemContainer.kt`、`Bin.kt`、`Layer.kt`、`Block.kt` 的公开属性、扩展函数、typealias 和构造入口。
2. 将面向业务调用方的 shape 判断、尺寸读取、top/bottom/stacking/hanging 能力判断集中到 item-domain API。
3. 收窄 `PlacementFactory.kt` 对 `QuantityPlacement2/3` 构造器的外泄，鼓励统一工厂入口。
4. 复核 `ItemMerger`、`LoadingOrderCalculator`、`DemandStatistics`、`MaterialDemandReducedCost` 等服务是否仍依赖长方体假设。
5. 用 focused tests 覆盖 cuboid、竖直圆柱、横向圆柱终态对象在 item-domain API 下的行为一致性。
6. 清理重复注释、过期 KDoc 和与当前能力不一致的 README 示例。

**修改清单**

1. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
2. `bpp3d-domain-item-context/src/main/.../model/ItemModelAliases.kt`
3. `bpp3d-domain-item-context/src/main/.../model/Package.kt`
4. `bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
5. `bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
6. `bpp3d-domain-item-context/src/main/.../model/Bin.kt`
7. `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
8. `bpp3d-domain-item-context/src/main/.../model/Block.kt`
9. `bpp3d-domain-item-context/src/main/.../model/PlacementFactory.kt`
10. `bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`
11. `bpp3d-domain-item-context/src/main/.../service/LoadingOrderCalculator.kt`
12. `bpp3d-domain-item-context/src/test/.../*`

**验收标准**

1. 业务调用方优先看到 item-domain/shape-domain API，而不是底层几何泛型。
2. `ItemView`、`ItemPlacement2/3`、`PackageAttribute` 的 shape-sensitive 行为不回退到通用 `CuboidView<*>` 兼容扩展。
3. cuboid、竖直圆柱、横向圆柱终态路径的尺寸、体积、支撑、top/bottom 能力语义有测试覆盖。
4. 旧长方体测试和现有 application 普通测试全部通过。
5. KDoc 与 unsupported message 准确说明当前 shape 能力边界。

### 3.3 layer assignment、packing 与 application shape contract 统一

**事项**

统一 layer assignment、packing、application、CSV 和 Gurobi 测试中的 shape contract，减少各层各自解析、各自假设 `Axis3.Y` 或长方体尺寸语义的情况。目标是在不默认开放新能力的前提下，让所有入口对“支持、拒绝、降级、后验校验”的语义一致。

**计划**

1. 梳理 `DemandConstraint`、`VolumeMinimization`、`LayerAggregation`、`LayerPlacementAdapter`、`ColumnGenerationAlgorithm` 中的 item shape 读取与模式键生成。
2. 将 CSV/request mapping 中的 `shape_type`、`radius`、`diameter`、`axis`、depth boundary 字段统一映射到 shape spec/capability。
3. 复核 Gurobi column generation 对动态半径/直径、axis metadata、depth boundary 的读取与回归数据。
4. 将 application 层禁止横向圆柱进入默认候选/层生成的错误信息与 domain unsupported 语义对齐。
5. 保持 depth boundary 作为 application 后验硬校验；除非另有完整建模和测试，不下沉为默认 MILP 原生约束。
6. 补充 contract tests，覆盖旧 CSV、无 shape 字段 CSV、竖直圆柱 CSV、横向圆柱被拒绝路径和 depth boundary 字段兼容。

**修改清单**

1. `bpp3d-domain-layer-assignment-context/src/main/.../model/LayerAggregation.kt`
2. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
3. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/VolumeMinimization.kt`
4. `bpp3d-domain-packing-context/src/main/.../service/Packer.kt`
5. `bpp3d-domain-packing-context/src/main/.../service/PackingGeometryGuard.kt`
6. `bpp3d-domain-packing-context/src/main/.../service/MaterialPacker.kt`
7. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
8. `bpp3d-application/src/main/.../service/ColumnGenerationAlgorithm.kt`
9. `bpp3d-application/src/main/.../request-or-csv-mapping/*`
10. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
11. `bpp3d-application/src/test/resources/gurobi/*`

**验收标准**

1. application、domain、CSV 和 Gurobi 测试对 shape 字段的解释一致。
2. 旧 CSV 和旧 DTO 继续兼容；缺失 shape 字段时仍按旧 cuboid 路径处理。
3. 横向圆柱进入未开放默认路径时返回明确 unsupported，而不是隐式按外接盒求解。
4. Gurobi 普通回归和 CSV dataset suite 在相关改动后实际执行并通过。
5. depth boundary 行为不被误写成默认 MILP 原生能力。

### 3.4 横向圆柱能力扩展评估与最小开放

**事项**

对 X/Z 横向圆柱从“已知坐标终态 guard/rendering”向更前置路径扩展进行一次系统评估。下一轮允许做小范围开放，但必须以真实几何、支撑、边界、候选生成和测试完整闭环为前提；不能为了减少 unsupported 而复用 Y 轴 circle packing 假设。

**计划**

1. 列出所有 `Axis3.Y` 限制点、upright-only 限制点和 `requireNoCylinderItemsForCuboidSearch` 路径。
2. 按最终校验、候选生成、block loading、stacking、hanging、circle packing、renderer 输出分类每个限制点。
3. 对每个候选扩展点给出“开放、保持 unsupported、只做错误信息/KDoc 改进”的决策。
4. 若开放任何 X/Z 路径，先补真实几何 guard、同 layer 圆柱轴向限制、full-length support 或等价支撑证明。
5. 默认生产链路仍不自动生成 X/Z 横向圆柱候选，除非候选生成、支撑和测试全部闭环。
6. 对不开放路径新增或补强 negative tests，防止后续误开放。

**修改清单**

1. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
2. `bpp3d-domain-block-loading-context/src/main/.../SimpleBlockGenerator.kt`
3. `bpp3d-domain-block-loading-context/src/main/.../CylinderUnsupportedGuard.kt`
4. `bpp3d-domain-block-loading-context/src/main/.../DepthFirstSearchAlgorithm.kt`
5. `bpp3d-domain-block-loading-context/src/main/.../MultiLayerHeuristicSearchAlgorithm.kt`
6. `bpp3d-domain-packing-context/src/main/.../service/PackingGeometryGuard.kt`
7. `bpp3d-domain-packing-context/src/main/.../service/Packer.kt`
8. `bpp3d-domain-item-context/src/main/.../model/Package.kt`
9. `bpp3d-domain-layer-generation-context/src/test/.../*`
10. `bpp3d-domain-block-loading-context/src/test/.../*`
11. `bpp3d-domain-packing-context/src/test/.../*`

**验收标准**

1. X/Z 横向圆柱只在碰撞、边界、支撑、候选来源和 renderer 语义完整的路径开放。
2. 缺少完整语义的路径继续显式 unsupported，并有测试覆盖。
3. 同一 `BinLayer` 内仍只允许一种圆柱轴向；不同 layer 可按既定策略使用不同轴向。
4. 不复用 Y 轴 circle packing 平面假设作为 X/Z 横向圆柱最终判定。
5. 默认生产链路不因评估发生行为回退或误开放。

### 3.5 renderer、DTO 与文档契约维护

**事项**

renderer 当前自动和人工验收已完成。下一轮不以 renderer 为主要开发目标，但任何 DTO、axis、shape metadata、外接盒语义或实际求解输出格式变更，都必须同步本仓 fixture、外部 renderer README/DTO、自动测试和人工视觉确认记录。

**计划**

1. 保持本仓 renderer fixture 与外部 renderer DTO 枚举一致。
2. 若新增或修改 `algorithmShapeType`、`renderShapeType`、`axis`、`bounding*` 字段，先补仓内 `RendererDTOTest` 和外部 renderer Rust/TS 类型。
3. 使用混装样例、原始三轴样例和一次实际求解输出进行回归。
4. 外部 renderer 固定执行 `npm run build`、`npx vue-tsc --noEmit`、`cargo check`、`cargo test`。
5. 人工视觉确认必须明确记录样例名、覆盖内容和未执行原因；未实际确认不得写成通过。
6. README、README_ch、refactor.md 保持同一能力口径。

**修改清单**

1. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`
2. `bpp3d-infrastructure/src/test/.../RendererDTOTest.kt`
3. `bpp3d-infrastructure/src/test/resources/renderer/*`
4. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
5. `README.md`
6. `README_ch.md`
7. `refactor.md`
8. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`

**验收标准**

1. 仓内 renderer DTO 契约测试通过。
2. 外部 renderer build、typecheck、Rust check、Rust test 通过。
3. 人工视觉确认覆盖混装、X/Y/Z 三轴、横向圆柱贴地、外接盒语义和实际求解输出。
4. renderer 文档不引用不存在或未支持的 DTO 枚举。
5. 未执行或失败的 renderer 项不得写成通过。

### 3.6 验证、文档和提交隔离

**事项**

下一轮范围扩大后，必须把验证执行和提交隔离作为交付内容的一部分。BPP3D 改动不得混入非 BPP3D 提交，历史验证不得覆盖新增代码后的本轮验证。

**计划**

1. 修改前执行 `git status --short --branch`，记录 BPP3D、本仓其他模块和外部 renderer 的改动范围。
2. 每个子任务结束后执行最小相关测试；全部完成后执行必跑门禁。
3. 涉及 application、CSV、solver、depth boundary 或 shape spec 时执行 Gurobi 普通回归和 CSV dataset suite。
4. 涉及 renderer DTO 或显示语义时执行外部 renderer 自动验收和人工视觉确认。
5. README、README_ch、refactor.md 同步更新，不保留过期路径、过期能力或过期失败记录。
6. 提交前仅 stage BPP3D 相关文件；外部 renderer 改动单独提交或单独记录。

**修改清单**

1. `README.md`
2. `README_ch.md`
3. `refactor.md`
4. `scripts/*.ps1`
5. `bpp3d-*/src/test/**/*`
6. 外部 renderer 文档、DTO 和测试文件

**验收标准**

1. 必跑门禁全部通过。
2. 建议完整验收中被当前改动触发的项目全部实际执行并记录。
3. 文档只记录真实执行结果；失败、跳过、历史复核和环境阻断分开写。
4. 非 BPP3D 改动不混入 BPP3D 提交。
5. 提交信息具体说明重构目标、关键边界、未开放能力和验证结果。

## 4. 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

## 5. 触发式完整验收

修改 application、CSV、shape spec、depth boundary 或 solver 相关代码时执行：

```powershell
mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

修改 renderer DTO、renderer fixture、packing renderer adapter 或显示语义时执行：

```powershell
cd E:\workspace\ospf\framework\bpp3d-interface-renderer
npm run build
npx vue-tsc --noEmit
cargo check
cargo test
```

## 6. 当前保留边界

1. BPP3D 完全泛型化尚未完成；底层 placement/projection 体系仍可保留必要 cuboid 结构性绑定。
2. 默认候选生成、block loading、stacking、hanging、circle packing 仍不开放 X/Z 横向圆柱。
3. 连续半径优化不进入默认生产链路。
4. depth boundary 仍为 application 后验硬校验，未下沉为默认 MILP 原生约束。
5. 后续新增代码后必须重新执行相关验证；未执行或失败的命令不得沿用历史通过记录。
