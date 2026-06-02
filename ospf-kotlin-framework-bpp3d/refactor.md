# BPP3D 形状泛型化与竖直圆柱支持重构交接

日期：2026-05-31
最近更新：2026-06-02
状态：`cylinder.md` 已合并到本文档，后续只维护 `refactor.md`。

## 1. 总目标

把 BPP3D 从“长方体作为隐含前提”的主流程，推进为“数值泛型 + 形状泛型”的装载主链，并稳定支持第一阶段竖直圆柱真实几何 MVP。

最终目标包括：

1. 主流程能表达长方体和竖直圆柱，后续可扩展更多 shape。
2. `Cuboid` 只是 shape 的一种实现，不再是 placement、projection、support、container、renderer 的唯一几何真相。
3. 竖直圆柱固定轴向为 `Axis3.Y`，不支持横放、躺放或任意角度旋转。
4. 圆柱边界、碰撞、支撑、体积和 renderer 输出使用真实几何，不把外接盒近似作为最终判定。
5. 保持现有长方体用例、CSV 入口、Gurobi 流程和旧 renderer 兼容字段不回退。

## 2. 已完成摘要

以下只保留结论，不保留逐项实现细节；需要查细节时请查看 git 历史。

1. 竖直圆柱第一阶段 MVP 已建立，真实几何基础、shape-aware 主入口、renderer metadata 和 loading rate 语义已完成阶段性接入。
2. domain item/package 层已具备长方体与竖直圆柱的稳定 shape metadata 入口，主链已优先使用统一 shape 入口。
3. block、layer、packing、loading order、demand、renderer、CSV/application 入口已完成第一阶段圆柱接入，暂不支持的旧算法路径已显式 unsupported。
4. Gurobi CSV 两类入口、README / README_ch、样例数据、协议 smoke 回归和 schema 预检已完成第一阶段收口。
5. 主链硬绑定调用面已阶段性收敛，`QuantityPlacement2/3(...)` 直写构造集中到 `PlacementFactory`，调用侧显式泛型噪声明显减少。
6. domain 层对 `Cuboid` / `CuboidView` 的显式绑定已进一步收敛，Item 专用封装模式已建立；底层结构性绑定仍保留。
7. 四个门禁脚本、BPP3D 全量测试、根 POM application 链路、Gurobi 回归和 CSV suite 在当前环境均可执行通过。

## 3. 当前未完成事项

当前状态不是“完全泛型化完成”，也不能把门禁通过等同于总目标完成。

核心缺口：

1. `Item` 仍继承 `Cuboid<Item>`，这是最大结构性耦合。
2. `QuantityPlacement3<T : Cuboid<T>>`、`QuantityPlacement2<T : Cuboid<T>, P : ProjectivePlane>` 和 `CuboidView` 仍是 placement / projection 基础类型。
3. `AbstractBPP3DShadowPriceArguments<T : Cuboid<T>>`、`DemandConstraint<Args, T : Cuboid<T>>`、`VolumeMinimization<Args, T : Cuboid<T>>` 仍保留基础设施类型链约束。
4. `Bin<T : Cuboid<T>>`、`Container3`、`ItemContainer`、`Layer`、`Block` 仍有以 `Cuboid` 为核心的继承或兼容语义。
5. `PackageAttribute`、`Pattern`、`ItemMerger`、部分 `CuboidView<*>` 扩展仍保留 cuboid-only 业务桥接。
6. DFS / MLHS / 空间切分对圆柱仍是显式 unsupported，不是真实圆柱几何搜索支持。
7. 圆密排候选仍是初步候选生成器，不是完整可替换圆柱算法服务。
8. 可变半径只完成 metadata 预留，不参与连续优化，也不应作为最终半径输出。
9. 其余业务入口、接口层契约和 legacy three.js renderer 未统一适配圆柱新字段；three.js 当前不在本仓内。

## 4. 下一轮目标

下一轮要尽可能减少迭代次数，优先处理能显著推进总目标的结构性问题。目标不是继续清理零散 import 或只做别名包装。

优先目标：

1. 制定并落地 `Item : Cuboid<Item>` 解耦的第一阶段可运行方案。
2. 建立 domain 层 shape capability 抽象，用于替代业务层直接依赖 `Cuboid` / `CuboidView` 的场景。
3. 将 item、container、demand、shadow price、bin/layer/block 的业务能力拆成 shape-aware 或 cuboid-only 明确边界。
4. 推进 placement / projection 适配层，让业务主链尽量依赖 `packingShape`、bounding box、footprint、support policy，而不是直接依赖 `CuboidView`。
5. 继续保持圆柱路径真实几何或显式 unsupported，严禁退回外接盒最终判定。
6. 同步收紧门禁 allowlist，防止新增主链硬绑定。

非目标：

1. 不一次性重写所有 `QuantityPlacement2/3` 基础设施。
2. 不强行把 DFS / MLHS 改成圆柱真实几何搜索，除非能补完整回归。
3. 不引入任意 shape 完整通用装箱框架。
4. 不实现连续半径全局优化器。
5. 不在缺少外部前端上下文时适配 legacy three.js renderer。

## 5. 下一轮原则

1. 先设计能力边界，再改继承结构。
2. 大步推进，但每一步必须可编译、可测试、可回退。
3. 新增抽象必须服务于真实迁移，不能只包装旧类型。
4. 别名只能作为过渡，不得用来宣称结构性解耦完成。
5. cuboid-only 路径必须显式命名、显式 KDoc、显式门禁或显式 unsupported。
6. 圆柱路径必须使用真实 shape geometry 或明确拒绝。
7. 不做跨模块无关格式化，不改非 BPP3D 模块。
8. 文档只记录实际完成和实际验证结果，未执行的测试不能写成通过。

## 6. 下一轮事项

### 6.1 建立基线

1. 读取 `.rules/chore.md`。
2. 执行 `git status --short --branch`，确认是否存在非 BPP3D 改动。
3. 跑四个门禁脚本、`git diff --check -- ospf-kotlin-framework-bpp3d`、BPP3D 全量测试。
4. 扫描当前硬绑定快照：

```powershell
rg --line-number "\b(Cuboid|AbstractCuboid|CuboidView|QuantityPlacement2|QuantityPlacement3|QuantityRectangle2|QuantityCuboid3)\b" ospf-kotlin-framework-bpp3d --glob "*.kt" --glob "!**/bpp3d-infrastructure/**"
rg --line-number "QuantityPlacement[23]\(" ospf-kotlin-framework-bpp3d --glob "*.kt" --glob "!**/bpp3d-infrastructure/**"
rg --line-number "ItemDemandConstraint<|ItemVolumeMinimization<|forItem<" ospf-kotlin-framework-bpp3d --glob "*.kt"
```

### 6.2 设计 domain shape capability

目标：让业务层有比 `Cuboid<T>` 更小、更稳定的能力边界。

建议评估以下能力接口，名称以最终代码语义为准：

1. `PackingShapeProvider`：暴露 `packingShape`。
2. `BoundingBoxProvider`：暴露兼容 bounding box 尺寸。
3. `FootprintProvider`：暴露 bottom footprint 或 shape placement 所需信息。
4. `DemandStatisticsProvider`：暴露 demand statistics。
5. `MaterialIdentityProvider`：暴露 material / package identity。
6. `CuboidCompatibleUnit`：仅给旧 placement/projection 兼容层使用，避免业务层直接扩散 `Cuboid`。

完成标准：

1. 新抽象至少能解释 `Item`、`ItemContainer`、`BinLayer`、`Block` 当前真实需求。
2. 不要求一步替换所有基础设施泛型，但必须减少业务层直接引用 `Cuboid` 的必要性。
3. 抽象命名不能暗示任意 shape 已完整支持。

### 6.3 推进 `Item : Cuboid<Item>` 第一阶段解耦

目标：为最终去除 `Item` 对 `Cuboid` 的继承建立可运行迁移路径。

建议步骤：

1. 梳理 `Item` 当前从 `Cuboid` 继承获得的能力：尺寸、体积、view、orientation、package 属性桥接、placement 构造。
2. 将纯业务属性访问迁移到 `Item` 自身或 shape metadata。
3. 将 placement 兼容能力集中到 adapter / factory，不在业务服务中直接要求 `Item : Cuboid<Item>`。
4. 若无法直接移除继承，先新增明确的 compatibility adapter，并将业务调用迁移到 adapter。
5. 补测试证明长方体行为不回退，圆柱路径不退回外接盒。

完成标准：

1. `Item` 业务调用面不再因为需求统计、shadow price、renderer、loading order 等逻辑而要求 `Cuboid<Item>`。
2. 剩余继承如果还存在，必须只服务于 placement/projection 基础设施兼容。
3. 文档明确记录继承是否仍存在，不能宣称完全完成。

### 6.4 收敛 `CuboidView` 和 cuboid-only 扩展

目标：减少 `CuboidView<*>` 在业务层承担 item 属性桥接。

建议步骤：

1. 处理 `Item.kt` 中 `Cuboid<*>` / `CuboidView<*>` 的 `packageType`、`packageCategory`、`bottomOnly`、`topFlat`。
2. 处理 `ItemContainer.kt` 中 `CuboidView<S>` 的属性和 dump 兼容路径。
3. 能改成 `Item` / `ItemContainer` / `packingShape` 的直接语义就迁移。
4. 不能迁移的扩展必须放在明确 compat 文件或 compat section 中。
5. 门禁 allowlist 应区分“compat adapter”与“业务主链”。

完成标准：

1. 业务服务不新增 `CuboidView<*>` 属性桥接。
2. cuboid-only 扩展集中、命名清楚、KDoc 中明确兼容语义。
3. 圆柱路径不会调用 cuboid-only dump / merge 逻辑。

### 6.5 推进 `Bin` / `Layer` / `Block` 封装

目标：减少调用侧对 `Bin<T : Cuboid<T>>`、`QuantityPlacement3<BinLayer>`、`QuantityPlacement3<Block>` 的显式感知。

建议步骤：

1. 评估 `ItemBin` 是否需要从 typealias 升级为独立类；如果影响面过大，先新增 facade。
2. 检查 `LayerBin`、`BlockBin`、`BinLayerPlacement`、`BlockPlacement3` 调用面。
3. 将 public API 尽量改为业务别名、facade 或 adapter。
4. 保留基础设施层泛型，但避免 application / service 直接暴露。

完成标准：

1. application、layer-assignment、block-loading、packing 主链不新增显式 `Bin<...>` 泛型。
2. `QuantityPlacement3<BinLayer>` / `QuantityPlacement3<Block>` 显式标注继续减少或集中。
3. 所有迁移都有定向回归。

### 6.6 Shadow price / demand / volume 类型链再评估

目标：判断是否能在不重写 CGPipeline 的前提下继续缩小 `T : Cuboid<T>` 暴露面。

建议步骤：

1. 复查 `AbstractBPP3DShadowPriceArguments<T : Cuboid<T>>` 的真实能力需求。
2. 复查 `DemandConstraint` / `VolumeMinimization` 是否还有公开 API 暴露 `T : Cuboid<T>` 给 domain 或 application。
3. 若无法移除泛型基类，保持 `ItemDemandConstraint` / `ItemVolumeMinimization` 独立类封装，并收紧所有调用方。
4. 若可以抽象能力边界，先做 item 专用路径，再考虑泛型路径。

完成标准：

1. 不再出现 `ItemDemandConstraint<...>` / `ItemVolumeMinimization<...>`。
2. application 不直接调用 `DemandConstraint<..., Item>` / `VolumeMinimization<..., Item>`。
3. 若基础设施泛型仍保留，文档明确说明原因。

### 6.7 圆柱真实几何主链闭环

目标：减少“只显式 unsupported”的区域，让可支持路径走真实几何。

建议步骤：

1. 复查 block / layer / packing 主链当前哪些圆柱路径真实支持，哪些显式 unsupported。
2. 为 `CirclePackingLayerGenerator` 增加 adapter 验收，确保候选结果通过真实几何二次校验。
3. 检查 `PackageAttribute`、`Pattern`、`ItemMerger` 是否仍存在圆柱静默退化风险。
4. 可支持的 feasibility check 迁移到 shape-aware policy；不可支持的路径保持显式 unsupported。

完成标准：

1. 混装长方体 + 竖直圆柱测试覆盖 layer / packing / renderer。
2. 圆柱 block/layer 结果通过真实几何校验。
3. DFS / MLHS 若仍 unsupported，测试覆盖不回退。

### 6.8 门禁和文档

目标：让自动化约束反映新的边界。

建议步骤：

1. 更新 `generic-boundary-check.ps1` 和 `shape-boundary-check.ps1` allowlist。
2. allowlist 只允许 infrastructure、compat adapter、集中构造入口持有结构性绑定。
3. 对已迁移文件删除 allowlist。
4. 更新 `refactor.md`，只记录实际结果。

完成标准：

1. 门禁通过且没有扩大业务主链硬绑定。
2. `refactor.md` 的“已完成”仍只保留高层结论，不恢复细节流水账。
3. 下一轮未完成事项继续保留真实缺口。

## 7. 下一轮整体计划

### 阶段 0：接手确认

1. 读取规则与本文档。
2. 记录工作树状态。
3. 跑门禁和 BPP3D 全量测试。
4. 生成硬绑定扫描快照。

### 阶段 1：能力边界设计

1. 梳理 `Item`、`ItemContainer`、`Bin`、`Layer`、`Block` 当前依赖 `Cuboid` 的能力。
2. 梳理 shadow price / demand / volume 类型链。
3. 定义最小 domain capability 或 compat adapter 方案。

### 阶段 2：Item 解耦第一刀

1. 迁移 `Item` 的纯业务能力调用。
2. 将 placement 兼容能力集中到 adapter。
3. 跑 item-context 与 layer-assignment 定向测试。

### 阶段 3：业务桥接收口

1. 迁移或集中 `CuboidView` 扩展。
2. 收敛 `ItemMerger`、`PackageAttribute`、`Pattern` 的 cuboid-only 语义。
3. 跑 block-loading / packing / application 定向测试。

### 阶段 4：Bin / Layer / Block facade

1. 评估 `ItemBin` / `LayerBin` / `BlockBin` 是否升级为 facade 或独立类。
2. 减少 application / service public API 中的显式泛型。
3. 跑 layer-assignment、block-loading、packing 回归。

### 阶段 5：圆柱闭环和门禁

1. 补 circle layer adapter 验收。
2. 补混装真实几何回归。
3. 更新门禁 allowlist。
4. 跑完整验收并更新文档。

## 8. 重点修改清单

优先文件：

1. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
2. `bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
3. `bpp3d-domain-item-context/src/main/.../model/ItemModelAliases.kt`
4. `bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
5. `bpp3d-domain-item-context/src/main/.../model/Pattern.kt`
6. `bpp3d-domain-item-context/src/main/.../model/Bin.kt`
7. `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
8. `bpp3d-domain-item-context/src/main/.../model/Block.kt`
9. `bpp3d-domain-item-context/src/main/.../model/PlacementFactory.kt`
10. `bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`
11. `bpp3d-domain-item-context/src/main/.../service/LoadingOrderCalculator.kt`
12. `bpp3d-domain-item-context/src/main/.../model/DemandReducedCost.kt`
13. `bpp3d-domain-item-context/src/main/.../model/ShadowPriceMap.kt`
14. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
15. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/VolumeMinimization.kt`
16. `bpp3d-domain-layer-generation-context/src/main/.../CirclePackingLayerGenerator.kt`
17. `bpp3d-domain-block-loading-context/src/main/.../service/SimpleBlockGenerator.kt`
18. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
19. `bpp3d-application/src/main/...`
20. `ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1`
21. `ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1`
22. `ospf-kotlin-framework-bpp3d/refactor.md`

验证相关文件：

1. `bpp3d-domain-item-context/src/test/.../DemandStatisticsTest.kt`
2. `bpp3d-domain-item-context/src/test/.../GenericDemandReducedCostTest.kt`
3. `bpp3d-domain-item-context/src/test/.../MaterialDemandReducedCostTest.kt`
4. `bpp3d-domain-item-context/src/test/.../ItemMergerCylinderTest.kt`
5. `bpp3d-domain-item-context/src/test/.../LoadingOrderCalculatorCylinderTest.kt`
6. `bpp3d-domain-layer-assignment-context/src/test/.../ItemDemandConstraintModeKeyTest.kt`
7. `bpp3d-domain-layer-assignment-context/src/test/.../PreciseLoadMultiBinAggregationTest.kt`
8. `bpp3d-domain-block-loading-context/src/test/.../SearchAlgorithmCylinderGuardTest.kt`
9. `bpp3d-domain-layer-generation-context/src/test/.../LayerGenerationProgramCandidateAdaptersTest.kt`
10. `bpp3d-domain-packing-context/src/test/.../PackerAndRendererAdapterTest.kt`
11. `bpp3d-application/src/test/.../ColumnGenerationAlgorithmTest.kt`
12. `bpp3d-application/src/test/.../MaterialPackingApplicationIntegrationTest.kt`
13. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`

## 9. 验收标准

### 9.1 阶段性验收

1. 新增或迁移代码不新增散落 `QuantityPlacement2/3(...)` 直写构造。
2. application / service public API 不新增显式 `Cuboid` / `CuboidView` / `QuantityPlacement*` 泛型暴露。
3. 若 `Item : Cuboid<Item>` 仍存在，必须证明其只服务于基础设施兼容，并记录剩余迁移点。
4. 若 `DemandConstraint` / `VolumeMinimization` 仍保留 `T : Cuboid<T>`，必须记录原因；不能宣称完全结构性解耦。
5. 圆柱路径不能因类型迁移退回外接盒判定。
6. cuboid-only 路径必须显式命名、显式 KDoc 或显式 unsupported。
7. 门禁 allowlist 不得扩大业务主链硬绑定。
8. README / 示例 / `refactor.md` 不得宣称未实现能力。

### 9.2 必跑命令

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1
git diff --check -- ospf-kotlin-framework-bpp3d
mvn -f ospf-kotlin-framework-bpp3d/pom.xml test "-Dgpg.skip=true"
```

### 9.3 建议完整验收

```powershell
mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test "-Dgpg.skip=true"
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

### 9.4 文档验收

1. `refactor.md` 必须同步记录实际执行命令与结果。
2. 未执行的测试不能写成通过。
3. 已完成事项只保留高层摘要，不恢复逐项实现细节。
4. 仍未完成的结构性绑定必须保留在“当前未完成事项”中。
5. 如果遇到依赖仓库、Gurobi license、JVM code cache 等环境问题，只记录为环境阻塞，不修改业务结论。
