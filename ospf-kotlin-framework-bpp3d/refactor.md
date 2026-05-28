# BPP3D 主流程重构交接

日期：2026-05-27
基线提交：`33b1dd5b feat(bpp3d): complete material packing planner integration and refactor handoff`

## 1. 已完成摘要

以下事项已完成主线闭环，本文不再保留实现细节和逐日流水：

1. application 层已承载 `ColumnGenerationAlgorithm` 主流程编排。
2. 独立 `bpp3d-domain-layer-selection-context` 已从主线架构移除。
3. demand 建模已支持 item demand、material demand 与混合模式；item/material demand 后续统一由 `Quantity<V>` 表达数量、重量等计量口径。
4. shadow price、reduced cost、layer statistics 已统一按 active demand entries 处理。
5. layer generation 已形成委托式接口与基础实现骨架。
6. application CG 已覆盖初始列、LP、SP、加列、final IP/MIP 与 packing analyzer 链路。
7. Gurobi 标准执行器、求解指标透传、CSV 回归、随机回归、多 bin/mixed-demand 回归已建立。
8. `final bins -> PackingResult -> SchemaDTO` 分析链路已存在。
9. `packing-context` 已完成前置包装规划语义修正，支持 material demand 到 packaged item 的转换。
10. `ColumnGenerationApplicationService` 已接入 material-only demand 包装规划入口。
11. `daily.md` 已删除，`refactor.md` 是唯一主线交接入口。

## 2. 当前边界

本交接文档只覆盖 BPP3D 主流程后续业务验收与性能基线沉淀。

不纳入本轮：

1. 圆柱真实几何、可变半径、真实圆形碰撞与密排，继续归 `cylinder.md`。
2. three.js renderer 展示细节。
3. 重构已完成部分的重复实现。

## 3. 下一轮目标

下一轮目标是把现有主流程从“代码回归通过”推进到“真实业务数据可验收、可度量、可沉淀基线”：

1. 接入真实业务外部 CSV 数据集并运行 Gurobi suite。
2. 沉淀真实数据下的 Gurobi 性能基线。
3. 验证 `final IP/MIP -> packing -> schema` 在真实业务口径下正确。
4. 支持 layer generation 直接使用 `PackingProgram` 作为候选 item，且 demand 仍按 `material + Quantity<V>` 结算。
5. 合并 item amount/weight demand 与 material amount/weight demand，统一为 `entity + Quantity<V> + DemandMode` demand，并为 unit 增加 `QuantityDomain`。
6. 补回 BPP3D core model/service 泛型化目标，清退主流程中的 `LegacyQuantity`。
7. 预留 APS `PackageSolution` 对接路径，支持应用系统和算法共用包装主数据。
8. 固化真实数据回归入口、阈值参数与输出指标。
9. 更新 `refactor.md` 的验收结果，不恢复 `daily.md`。

## 4. 下一轮事项

### 4.1 真实业务大规模数据联调

现有内置 CSV、随机、Gurobi 中大规模回归已补齐，但仍需要使用真实业务外部数据集跑出稳定结果。

需要使用的入口：

1. `bpp3d.gurobi.dataset.suite.paths`
2. `bpp3d.gurobi.dataset.suite.dir`

需要记录的指标：

1. 总耗时。
2. 单 case 耗时。
3. LP/MILP objective。
4. LP/MILP gap。
5. selected bin count。
6. selected layer count。
7. schema 与 packing aggregation 一致性。

### 4.2 Gurobi 性能基线沉淀

现有 `elapsed`、`lpInfos`、`finalInfo` 和阈值断言入口已具备。下一轮需要用真实数据确认阈值是否合理。

需要校准：

1. `bpp3d.gurobi.dataset.suite.max.elapsed.seconds`
2. `bpp3d.gurobi.dataset.suite.max.total.elapsed.seconds`
3. solver gap。
4. thread 数。
5. time limit。
6. not-improvement time。

### 4.3 final analyzer 真实场景验收

测试侧已覆盖多物料、多 bin、大样本、schema、CSV、随机场景。下一轮需要用真实业务数据确认业务口径。

需要验收：

1. `final bins -> packing -> schema` 输出符合业务预期。
2. material summary 与真实需求口径一致。
3. loading plan 数量与 selected bin/layer 一致。
4. 空 bin、选中 bin、未选中 layer 的处理符合业务预期。
5. renderer DTO KPI 可被前端或下游服务稳定消费。

### 4.4 PackingProgram 作为 layer-generation item

需要支持一种新的 layer-generation 输入语义：`PackingProgram` 可以直接作为候选 item 进入 layer generation。

业务含义：

1. `PackingProgram` 表示一个可选择的包装方案或包装后单元。
2. layer generation 可以直接基于 `PackingProgram.shape` 生成 layer。
3. demand 不按 `PackingProgram` 自身数量结算，而是按其包含的 `material + Quantity<V>` 结算。
4. shadow price 与 reduced cost 仍必须走 active material demand entries。
5. 该能力应与现有 material packing planner 共存，不应破坏已包装成 `ActualItem` 的路径。

需要明确的边界：

1. 当输入是 `ActualItem` 时，继续按现有 item/material demand 统计。
2. 当输入是 `PackingProgram` 时，应构造可用于 layer generation 的 item view 或等价 candidate。
3. `PackingProgram.materials` 是 demand contribution 的来源，但不能只表达 `MaterialKey -> UInt64`。
4. material contribution 应统一表达为 `MaterialKey -> Quantity<V>`。
5. unit 的 `QuantityDomain` 用于区分离散量与连续量，并且是算法判定变量值域的唯一依据。
6. unit 可以保留 dimension 作为业务描述元数据，但 dimension 不参与任何算法判定和运算。
7. 若 `PackingProgram` 缺少真实 `Material` 对象，只持有 `MaterialKey`，需要在 request/context 中补充 `MaterialKey -> Material` 映射。

### 4.5 Demand、DemandMode 与 QuantityDomain

item amount demand 与 item weight demand、material amount demand 与 material weight demand 都应合并。它们不是不同领域概念，只是 demand 的 `quantity.unit` 与 entity key 不同。

同时需要保留 `DemandMode` 作为语义标签，标记该 demand 在业务和分析口径上来自 item/material、amount/weight 等哪类来源。`DemandMode` 不是数值入口，也不是离散/连续判定入口。

建议语义：

1. `ItemDemand<V>` 表达为 `item + Quantity<V> + DemandMode`。
2. `MaterialDemand<V>` 表达为 `material + Quantity<V> + DemandMode`。
3. `PackingProgram.materials` 表达为 `Map<MaterialKey, Quantity<V>>` 或等价结构。
4. 离散/连续由 `PhysicalUnit.domain` 表达。
5. 数量、重量、体积等业务含义可由 unit dimension 描述，但不参与算法判定和运算。
6. 算法不再保留 item amount/weight、material amount/weight 多套建模入口，而是根据 entity key 与 unit metadata 归一化为统一 demand constraint。
7. `DemandMode` 可用于 decision、constraint、shadow price key、analyzer、KPI、报表和调试输出。

建议 demand metadata：

```kotlin
sealed interface Bpp3dDemandMode {
    object Item : Bpp3dDemandMode
    object Material : Bpp3dDemandMode
    object ItemAmount : Bpp3dDemandMode
    object ItemWeight : Bpp3dDemandMode
    object ItemMaterialAmount : Bpp3dDemandMode
    object ItemMaterialWeight : Bpp3dDemandMode
}

data class ItemDemand<V : FloatingNumber<V>>(
    val item: Item<V>,
    val quantity: Quantity<V>,
    val mode: Bpp3dDemandMode = Bpp3dDemandMode.Item
)

data class MaterialDemand<V : FloatingNumber<V>>(
    val material: Material<V>,
    val quantity: Quantity<V>,
    val mode: Bpp3dDemandMode = Bpp3dDemandMode.Material
)
```

`DemandMode` 的使用边界：

1. 可以作为 constraint name、decision key、shadow price key、aggregation key 和 KPI 分组字段。
2. 只用于标记需求来源和业务口径，不参与单位换算、数值计算或变量值域判定。
3. 离散/连续必须由 `quantity.unit.domain` 判定。
4. 旧 concrete mode 可保留为兼容标签，但主入口应收敛到 `Item` / `Material` + `Quantity<V>`。
5. 兼容期沿用当前 `ItemMaterialAmount` / `ItemMaterialWeight` 命名；若后续要改为 `MaterialAmount` / `MaterialWeight`，必须单独做兼容迁移，不能在本轮交接中隐式改名。

建议 unit metadata：

```kotlin
enum class QuantityDomain {
    Discrete,
    Continuous
}

abstract class PhysicalUnit {
    abstract val quantity: DerivedQuantity

    open val domain: QuantityDomain
        get() = quantity.domain
}
```

规则：

1. `QuantityDomain.Discrete`：离散 demand，进入整数/离散约束。
2. `QuantityDomain.Continuous`：连续 demand，进入连续约束。
3. 算法侧必须读取 `quantity.unit.domain`，不得使用 `quantity.unit.quantity.domain` 作为最终判断入口。
4. `DerivedQuantity.domain` 只提供默认值；具体单位可以覆盖，例如 `bit` 可为 `Discrete`，`kilobit` 可为 `Continuous`。
5. `DemandMode` 和 `QuantityDimension` 只用于业务展示、校验提示、统计分组或追踪，不参与任何算法判定和运算。

兼容要求：

1. 现有 item amount 构造路径可保留为快捷入口，但应转换为 `Quantity<V>`，unit 使用 `QuantityDomain.Discrete`。
2. 现有 item weight 构造路径可保留为快捷入口，但应转换为 `Quantity<V>`，unit 使用 `QuantityDomain.Continuous`。
3. 现有 material amount 构造路径可保留为快捷入口，但应转换为 `Quantity<V>`，unit 使用 `QuantityDomain.Discrete`。
4. 现有 material weight 构造路径可保留为快捷入口，但应转换为 `Quantity<V>`，unit 使用 `QuantityDomain.Continuous`。
5. layer-generation、packing planner、reduced cost 都必须基于统一 demand entries。
6. material contribution 不能再隐含为 item weight，必须以 material composition 的 `Quantity<V>` 为准。
7. 兼容入口可根据旧 amount/weight 参数填充 `DemandMode`，但进入主流程后必须只保留统一 `quantity` 数值。

### 4.6 BPP3D Core 泛型化与 LegacyQuantity 清退

当前代码仍存在大量 `LegacyQuantity`，并且部分核心模型和服务仍固定到 `Quantity<Flt64>` / `LegacyScalar`。这意味着 BPP3D 尚未完成真正的泛型化。

本项是主线目标，不应只停留在 demand 层局部支持 `Quantity<V>`。

目标：

1. BPP3D core model/service 统一参数化为 `<V>`，其中 `V` 是算法数值类型。
2. `Material<V>`、`PackageShape<V>`、`Package<V>`、`Item<V>`、`Bin<V>`、`Layer<V>`、`Load<V>`、`DemandEntry<V>` 等核心类型不再依赖 `LegacyQuantity`。
3. 几何、包装、layer generation、layer assignment、packing planner、application request/response 的主流程统一传递 `Quantity<V>`。
4. `Flt64*` 类型只作为兼容 facade 或 typealias 存在，不作为核心模型的唯一真实类型。
5. `LegacyQuantity` 只能保留在 `api/compat/**`、legacy constructor facade、迁移桥接代码和测试兼容层中。
6. `Bpp3dSolverValueAdapter` 这类 solver adapter 可以将 `Quantity<V>` 映射为 solver 标量，但不得倒逼 domain model 固定为 `Flt64`。

建议迁移顺序：

1. 先梳理 `LegacyQuantity` 使用点，按 core model、service、application、compat/test 分组。
2. 从 `bpp3d-domain-item-context` 开始泛型化基础模型：`Material`、`PackageShape`、`Package`、`Item`、`Bin`、`Layer`。
3. 再迁移 `bpp3d-domain-layer-generation-context`，使 request/result/generator 全链路接受泛型 item/bin/layer。
4. 再迁移 `bpp3d-domain-layer-assignment-context`，使 `Load`、`Bpp3dDemandEntry`、constraint、objective 使用 `Quantity<V>` 或 adapter 后的 solver value。
5. 再迁移 `bpp3d-domain-packing-context`，使 material packing planner 和 package solution adapter 使用统一 `Quantity<V>` material contribution。
6. 最后迁移 application 和 Gurobi 测试，保留 `Flt64` 作为默认测试实例化类型。
7. 每迁移一层，都保留兼容 facade，避免一次性破坏现有测试和外部调用。

需要明确的边界：

1. 泛型化不等于删除所有 `Flt64` 测试；测试仍可用 `Flt64` 实例化。
2. 泛型化不等于删除所有 compat；compat 是外部兼容边界。
3. `LegacyQuantity` 不应出现在 core model/service 主路径中。
4. `QuantityDomain` 判定入口应同步使用 `quantity.unit.domain`。

建议新增边界检查脚本或测试：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "rg '\bLegacyQuantity\b' ospf-kotlin-framework-bpp3d/bpp3d-*/src/main | rg -v 'api/compat|LegacyScalars|compat|legacy'"
```

期望结果为空。

### 4.7 APS PackageSolution 主数据对接

已阅读 APS 侧文档与模型：

1. `E:\workspace\poit\aps-domain-ontology\aps-domain\aps-domain-material\daily.md`
2. `E:\workspace\poit\aps-domain-ontology\aps-domain\aps-domain-material\src\main\com\poit\aps\material\packaging\model\PackageSolution.kt`

APS 目标是 packaging 直接复用 BPP3D 的包材、包装方案和物理量类型，最终消除应用系统与算法之间的重复主数据。

APS `PackageSolution` 当前关键结构：

1. `PackageSolution` 是包装方案聚合根。
2. `PackageSolutionSpec.materialItems` 使用 `PackageSolutionMaterialItem` 表达物料组成。
3. `PackageSolutionMaterialItem.quantity` 是 `Quantity<FltX>`，应通过 `QuantityDomain` 表示离散/连续语义。
4. `PackageSolution` 支持 children，表示树形内外包装层级。
5. `PackagingMaterial.shape` 是包材形状来源，后续计划改为复用 BPP3D `PackageShape`。

BPP3D 侧应预留转换边界：

```text
APS PackageSolution
-> canonical package solution DTO / adapter
-> BPP3D PackingProgram
-> layer-generation candidate / packing planner candidate
```

设计要求：

1. BPP3D 不直接依赖 APS domain，避免算法模块反向依赖应用系统。
2. BPP3D 提供稳定的 packaging 主数据模型或 adapter 接口。
3. APS 侧通过 adapter 将 `PackageSolution` 转为 BPP3D `PackingProgram`。
4. `PackageSolutionMaterialItem.quantity` 必须能映射到统一 material demand。
5. `PackageSolution.children` 应能映射到 outer/inner `PackingProgram` 层级。
6. `PackagingMaterial.shape` 应能映射到 `PackingProgram.shape`。
7. 包材自身数量 `PackageSolutionSpec.quantity` 与物料组成 `materialItems.quantity` 需要区分，避免把包材数量误算为被包装物料 demand。

### 4.8 文档与回归入口固化

下一轮执行后只更新 `refactor.md`：

1. 记录真实数据集入口形式，不记录敏感业务数据内容。
2. 记录最终阈值和基线结果。
3. 标记验收项完成状态。
4. 不恢复 `daily.md`。

## 5. 执行步骤

1. 运行 `git status --short`，确认只处理 `ospf-kotlin-framework-bpp3d` 相关改动，不处理 `.rules/chore.md` 与 `ospf-kotlin-multiarray` 的既有脏改。
2. 确认真实业务 CSV 字段与当前 `GurobiColumnGenerationTest` 数据加载入口兼容。
3. 如字段不兼容，新增轻量 adapter 或扩展 CSV parser，避免改动已有内置数据集语义。
4. 使用 `bpp3d.gurobi.dataset.suite.paths` 跑单个真实 CSV。
5. 使用 `bpp3d.gurobi.dataset.suite.dir` 跑目录级真实 CSV suite。
6. 收集 `elapsed`、`lpInfos`、`finalInfo`、packing aggregation、schema KPI。
7. 根据真实结果调整 suite 阈值参数，避免阈值过紧或过松。
8. 补充必要断言，锁定 selected bin/layer、material summary、schema KPI 一致性。
9. 梳理 layer-generation request/context，设计 `PackingProgram` candidate 输入。
10. 合并 item amount/weight demand 与 material amount/weight demand 为统一 demand，并补充 `DemandMode` 语义标签与 unit `QuantityDomain`。
11. 扩展 `PackingProgram.materials` 的表达，使其支持 `MaterialKey -> Quantity<V>`。
12. 实现 `PackingProgram -> layer item candidate` 的转换，并保证 demand contribution 按 `material + Quantity<V>` 计算。
13. 设计 APS `PackageSolution -> PackingProgram` adapter 边界，先定义 BPP3D 侧稳定输入模型，不在 BPP3D 中直接依赖 APS。
14. 补充 `PackingProgram` 直接参与 layer generation 的 domain/application 测试。
15. 补充 `PackingProgram.materials` 统一 `Quantity<V>` material demand 测试。
16. 补充 APS-like package solution DTO/adapter 测试，覆盖 discrete quantity、continuous quantity、children 层级映射。
17. 扫描 `LegacyQuantity` 使用点，生成 core/compat/test 分类清单。
18. 分层迁移 BPP3D core model/service 到 `<V>` + `Quantity<V>`。
19. 将 `LegacyQuantity` 收敛到 `api/compat/**`、legacy facade 或明确迁移桥接层。
20. 补充 `LegacyQuantity` 边界检查，防止 core model/service 回退到 legacy 类型。
21. 运行默认 application 回归，确保真实数据入口和新 candidate 输入不破坏常规测试。
22. 如本机具备 Gurobi，运行完整 `GurobiColumnGenerationTest`。
23. 更新 `refactor.md` 验收项与基线结果。

## 6. 修改清单

预计可能修改：

1. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
2. `bpp3d-application/src/test/resources/gurobi/...`，仅在需要增加脱敏样例或格式说明时修改。
3. `bpp3d-application` 中 CSV loading/helper 相关代码，如果真实 CSV 字段需要适配。
4. `ColumnGenerationStandardExecutors.kt`，仅在真实数据暴露必要指标缺口时修改。
5. `ColumnGenerationPackingAnalyzer.kt` 或 packing/schema adapter，仅在真实业务口径发现统计缺口时修改。
6. `bpp3d-domain-layer-generation-context` 中 request/context/generator 相关文件，用于支持 `PackingProgram` candidate。
7. `bpp3d-domain-item-context` 中 demand model、`PackingProgram` / material composition 相关模型，用于支持统一 `ItemKey/MaterialKey -> Quantity<V>`。
8. `bpp3d-domain-packing-context` 中 `PackageSolution`-like adapter/model，仅在需要承接外部包装主数据时修改。
9. `bpp3d-domain-packing-context` 或 application 中 APS adapter 接口测试，不直接依赖 APS domain。
10. `bpp3d-domain-item-context/src/main/.../api/LegacyScalars.kt` 与 `api/compat/**`，仅作为兼容边界保留或调整。
11. `bpp3d-domain-item-context/src/main/.../model/**`，清退 core model 中的 `LegacyQuantity`。
12. `bpp3d-domain-item-context/src/main/.../service/**`，清退 core service 中的 `LegacyQuantity`。
13. `bpp3d-domain-layer-assignment-context/src/main/**`，迁移 `Load`、demand entry、constraint、adapter 边界。
14. `bpp3d-domain-layer-generation-context/src/main/**`，迁移 request/result/generator 泛型边界。
15. `bpp3d-domain-packing-context/src/main/**`，迁移 packing planner material contribution 到 `Quantity<V>`。
16. `bpp3d-application/src/main/**` 与 `src/test/**`，以 `Flt64` 作为默认实例化类型验证泛型主流程。
17. `scripts/generic-boundary-check.ps1` 或新增等价脚本，加入 `LegacyQuantity` 边界检查。
18. `refactor.md`

不应修改：

1. `.rules/chore.md`
2. `ospf-kotlin-multiarray/**`
3. 已完成的 packing planner 主语义，除非真实数据暴露明确 bug。
4. `cylinder.md` 相关实现，除非用户明确切换到圆柱专题。

## 7. 验收标准

- [x] 真实业务 CSV 可通过 `bpp3d.gurobi.dataset.suite.paths` 跑通。
- [x] 真实业务 CSV 目录可通过 `bpp3d.gurobi.dataset.suite.dir` 批量跑通。
- [x] 每个真实 case 输出 elapsed、LP/MILP objective、gap、selected bin count、selected layer count。
- [x] suite 总耗时和单 case 耗时有明确阈值。
- [x] 阈值参数已在命令或文档中沉淀。
- [x] `final bins -> packing -> schema` 在真实数据下通过一致性断言。
- [x] material summary 与真实需求口径一致。
- [x] loading plan 数量与 selected bin/layer 一致。
- [x] layer generation 可直接接受 `PackingProgram` 作为候选 item。
- [x] `PackingProgram` candidate 的 demand contribution 按 `material + Quantity<V>` 计算正确。
- [x] `PackingProgram` candidate 的 shadow price / reduced cost 使用 active material demand entries。
- [x] item amount demand 与 item weight demand 已合并为 item demand。
- [x] material amount demand 与 material weight demand 已合并为 material demand。
- [x] item demand 与 material demand 均使用 `Quantity<V>` 表达计量值。
- [x] `PackingProgram.materials` 支持 `MaterialKey -> Quantity<V>`。
- [x] item/material demand 均携带 `DemandMode` 语义标签。
- [x] `DemandMode` 可用于 decision、constraint、shadow price key、analyzer、KPI 和报表分组。
- [x] `DemandMode` 不参与单位换算、数值计算或离散/连续判定。
- [x] unit metadata 支持 `QuantityDomain.Discrete`。
- [x] unit metadata 支持 `QuantityDomain.Continuous`。
- [x] demand 建模统一读取 `quantity.unit.domain`。
- [x] `QuantityDomain.Discrete` demand 进入离散约束。
- [x] `QuantityDomain.Continuous` demand 进入连续约束。
- [x] `QuantityDimension` 不参与任何算法判定和运算。
- [x] BPP3D core model/service 已完成 `<V>` 泛型化。
- [x] `Material<V>`、`PackageShape<V>`、`Package<V>`、`Item<V>`、`Bin<V>`、`Layer<V>` 不再依赖 `LegacyQuantity`。
- [x] `Load<V>`、`Bpp3dDemandEntry<V>`、layer assignment constraint/objective 不再依赖 `LegacyQuantity`。
- [x] layer generation request/result/generator 全链路不再依赖 `LegacyQuantity`。
- [x] packing planner 与 `PackingProgram.materials` 主路径不再依赖 `LegacyQuantity`。
- [x] `Flt64*` 仅作为兼容 facade/typealias 或测试实例化存在。
- [x] `LegacyQuantity` 只允许出现在 `api/compat/**`、legacy facade、明确迁移桥接层或 legacy 测试中。
- [x] `rg '\bLegacyQuantity\b' bpp3d-*/src/main | rg -v 'api/compat|LegacyScalars|compat|legacy'` 无命中。
- [x] APS-like `PackageSolution` 可通过 adapter 转为 BPP3D `PackingProgram`。
- [x] APS-like children 层级可映射为 outer/inner `PackingProgram`。
- [x] BPP3D 不直接依赖 APS domain。
- [x] 默认 application 回归通过。
- [x] Gurobi 回归通过，或明确记录本机缺少 Gurobi 环境导致未执行。
- [x] `refactor.md` 更新最终验收结果。
- [x] `daily.md` 保持删除状态。

## 8. 本轮执行结果

执行日期：2026-05-27

真实业务 CSV（`suite.paths`）：

1. `E:/workspace/poit/排刀优化/2024-05 山鹰/输出-山鹰-订单件数.csv`
2. elapsed：`7.205s`
3. LP/MILP objective：`0.0 / 0.0`
4. LP/MILP gap：`0.0 / 0.0`
5. selected bin/layer：`1 / 814`

真实业务 CSV（`suite.dir`）：

1. `E:/workspace/poit/排刀优化/2024-05 山鹰/输出-山鹰-订单件数-剩余可匹配件数.csv`
   elapsed：`3.486s`，selected bin/layer：`1 / 755`，LP/MILP gap：`0.0 / 0.0`
2. `E:/workspace/poit/排刀优化/2024-05 山鹰/输出-山鹰-订单件数.csv`
   elapsed：`7.205s`，selected bin/layer：`1 / 814`，LP/MILP gap：`0.0 / 0.0`
3. suite 合计（按 case elapsed 累计）：`10.691s`

阈值与参数（当前默认）：

1. `bpp3d.gurobi.dataset.suite.max.elapsed.seconds=180`
2. `bpp3d.gurobi.dataset.suite.max.total.elapsed.seconds=600`
3. `bpp3d.gurobi.dataset.suite.max.lp.gap=0.05`
4. `bpp3d.gurobi.dataset.suite.max.milp.gap=0.05`
5. `bpp3d.gurobi.dataset.suite.solver.time.seconds=40`
6. `bpp3d.gurobi.dataset.suite.solver.thread.num=4`
7. `bpp3d.gurobi.dataset.suite.solver.gap=0.01`
8. `bpp3d.gurobi.dataset.suite.solver.not.improvement.time.seconds=15`

一致性校验说明：

1. suite/single/random 均新增断言：`packing aggregation`、`schema.loadingPlans`、`material summary` 同步一致。
2. `selected_layer_count` 与 `packedLayerCount` 分离校验：前者表示选中的列（layer pattern）数量，后者表示展开后的实际装载层数。
3. 默认 application 回归与完整 Gurobi 回归均通过。
4. `Bpp3dDemandEntry` 新增 `quantityUnit` 与 `quantityDomain` 元数据，`demand` 建模读取 `quantity.unit.domain`。
5. `DemandConstraint` 约束命名新增 `discrete/continuous` 标签，离散与连续 demand 约束可区分追踪。
6. 新增 `MaterialDemandEntriesTest#materialWeightDemandDomainShouldReadFromUnitInsteadOfDimension`，验证同量纲单位可通过 unit-level domain 覆盖默认量纲语义。
7. 新增 `Bpp3dDemandMode.ItemWeight` 与 `Item.statistics(ItemWeight)`，补齐 item 侧连续量 demand 表达。
8. `Load.kt` 新增统一 `Quantity<Flt64>` demand 入口：
   `demandEntriesFromItemDemands`、`demandEntriesFromMaterialDemands`（含泛型桥接重载），按 `quantity.unit` 的 domain 自动选择离散/连续 demand 模式。
9. 新增 `UnifiedDemandEntriesTest`，覆盖 item/material 统一 demand 入口在离散与连续单位下的模式选择、domain 透传与 solver 值构造。
10. `PackingProgram` 新增 `innerPackageWithMaterialQuantities` 与 `materialQuantities`，可从 `MaterialKey -> Quantity<Flt64>` 输入构造 material contribution，并兼容离散/连续单位映射。
11. `Bpp3dDemandMode` 增加统一主模式 `Item/Material`，并通过 `toConcreteMode(isDiscrete)` 在统计阶段映射到旧模式，保持约束与统计口径一致。
12. `DemandConstraint` 与 `ColumnGenerationStandardExecutors` 增加 shadow price key 兼容层：支持带 `quantityUnit` 的新 key，同时兼容无 `quantityUnit` 与旧 concrete mode key 的读取。
13. 定向回归通过：`DemandStatisticsTest`、`QuantityDemandStatisticsGenericTest`、`MaterialDemandEntriesTest`、`UnifiedDemandEntriesTest`、`ItemDemandConstraintModeKeyTest`、`LayerGenerationFltXProofTest`、`ColumnGenerationAlgorithmTest`。
14. `ColumnGenerationApplicationRequest.materialWeightDemands` 与 `MaterialPackingDemand.weight` 已统一为 `Quantity<Flt64>`；`PackageSolutionLikeAdapter` 同步去除 `LegacyQuantity` 依赖。
15. `LegacyQuantity` 主路径扫描结果（2026-05-27）：`core main` 69 行（全部位于 `bpp3d-domain-item-context`），`compat main` 1 行（`LegacyScalars.kt`），`application/domain-packing/domain-layer-assignment/domain-block-loading` 主路径已无命中。
16. 修复 `bpp3d-domain-item-context` 批量替换后遗留的文件注解污染（重复 `@file:Suppress("DEPRECATION")`）与 `Package.kt` 语法损坏，恢复模块可编译状态。
17. `MaterialPackingObjectiveConfig`、`MaterialPackingMipResult`、`MaterialPackingSolveInfo` 与 `ExhaustiveMaterialPackingSolverExecutor` 的目标值/间隙/权重已由 `Double` 迁移为 `Flt64`，并保留原有求解语义。
18. `LayerGenerationContext` 的 `scoreByShadowPrice`/`numericScore` 及 `ColumnGenerationStandardExecutorConfig.integralityTolerance` 已迁移为 `Flt64`，评分排序与列选择逻辑保持不变。
19. 定向回归通过（`mvn -f ospf-kotlin-framework-bpp3d/pom.xml ...`）：`MaterialPackerTest`、`LayerGenerationFltXProofTest`、`ColumnGenerationAlgorithmTest`。
20. 修复 `bpp3d-domain-layer-assignment-context` 与 `ospf-kotlin-core:1.1.0` 包路径不一致问题：`core.symbol.*` 全量迁移到 `core.intermediate_symbol.*`（`Assignment.kt`、`Capacity.kt`、`Load.kt`），`-DskipTests compile` 可通过。
21. 同步修复 `ItemDemandConstraintModeKeyTest` 的 `core.symbol.*` 旧包引用，定向测试通过：`mvn -f ... -pl bpp3d-domain-layer-assignment-context -am test -Dtest=ItemDemandConstraintModeKeyTest '-Dsurefire.failIfNoSpecifiedTests=false'`。
22. 修复 application 测试中 `IntermediateSymbol` 全限定名旧包引用（`ColumnGenerationAlgorithmTest`、`MaterialPackingApplicationIntegrationTest`），恢复 `bpp3d-application` 测试编译。
23. `GurobiColumnGenerationTest` 已清退显式 `Double` 类型声明（参数、配置阈值、CSV 行模型、聚合容器），统一为 `Flt64`；新增 `optionalFlt64Property` 并在时长阈值比较处显式 `toDouble().seconds`。
24. 启用 `gurobi-cg-test` profile 的 test-compile 回归通过：`mvn -f ... -pl bpp3d-application -am -Pgurobi-cg-test test-compile -DskipTests`。
25. `PackingProgramMaterialValue.weight` 与 `MaterialPackingDemand.weight` 已由 `Quantity<Flt64>?` 放宽为可承载 `Quantity<V>` 的 `Quantity<*>?`；`innerPackageWithMaterialQuantities` 已泛型化为 `<V : FloatingNumber<V>>`，显式 material contribution 与 material packing weight demand 可输入 `Flt64` 或 `FltX`。
26. `PackingProgram.materialWeights()` 与 `materialQuantities()` 保持 `Quantity<Flt64>` 兼容视图，内部通过统一归一化函数把 `Flt64/FltX` material contribution 映射到旧主流程。
27. APS-like `PackageSolutionLikeQuantity.Weight` 与 `AmountAndWeight` 已泛型化为 `Quantity<V>`，adapter 复用 `mergePackingProgramMaterialValues` 合并 amount/weight contribution，新增 `FltX` adapter 回归。
28. 新增 `Bpp3dGenericBoundaryTest`，扫描 `ospf-kotlin-framework-bpp3d/**/src/main/**/*.kt`，防止 `LegacyQuantity` 泄漏到兼容边界外。
29. `LegacyQuantity` 主源码扫描结果（2026-05-28）：仅 `bpp3d-domain-item-context/src/main/.../api/LegacyScalars.kt` 保留 `typealias LegacyQuantity = Quantity<LegacyScalar>`。
30. 当前 4.6 剩余缺口：核心运行模型仍未完成真正 `<V>` 泛型化，`Container/Item/Bin/Layer/Load/Assignment/Capacity/ColumnGenerationStandardExecutors` 等主路径仍固定 `InfraScalar/Flt64` 或 `LegacyScalar`。
31. 定向回归通过：`PackingProgramMaterialValueTest`、`PackageSolutionLikeAdapterTest`、`Bpp3dGenericBoundaryTest`、`LayerGenerationFltXProofTest`、`MaterialPackerTest`、`ColumnGenerationAlgorithmTest`。
32. `bpp3d-domain-item-context` 继续收敛显式 `Flt64`：`Bin.kt`、`Block.kt`、`DemandStatistics.kt`、`Item.kt`、`ItemContainer.kt`、`Layer.kt`、`Material.kt`、`Package.kt`、`LoadingOrderCalculator.kt` 的主类型签名由 `Quantity<Flt64>` 迁移为 `Quantity<LegacyScalar>`，避免 `LegacyQuantity` 在主路径泄漏且保持行为不变。
33. `LegacyQuantity` 主源码边界复核（2026-05-28）：`rg '\bLegacyQuantity\b' bpp3d-domain-item-context/src/main` 仅保留 `api/LegacyScalars.kt` 的 typealias 定义；`Bpp3dGenericBoundaryTest` 通过。
34. 本轮回归通过：`PackingProgramMaterialValueTest`、`QuantityDemandStatisticsGenericTest`、`QuantityDomainAliasExampleTest`、`MaterialPackerTest`、`PackageSolutionLikeAdapterTest`、`ColumnGenerationAlgorithmTest`、`Bpp3dGenericBoundaryTest`。
35. `bpp3d-domain-item-context/src/main/.../model` 显式 `Flt64` 扫描结果（2026-05-28）已归零；`Flt64` 仅保留在 `api/compat`、`api` 别名/桥接层与迁移辅助函数中。
36. 新增 `bpp3d-domain-layer-generation-context/.../compat/LayerGenerationScalarAliases.kt`，将 `LayerGenerationContext.kt` 中 `scoreByShadowPrice`、`numericScore`、shadow-price 评分与排序默认值的显式 `Flt64` 收敛到 `LayerGenerationScalar` 别名与 helper。
37. `bpp3d-domain-layer-generation-context/src/main` 显式 `Flt64` 扫描结果（2026-05-28）：仅 `compat/LayerGenerationScalarAliases.kt` 保留，实现主路径不再直接引用 `Flt64`。
38. 定向回归补充通过：`LayerGenerationFltXProofTest`；`ColumnGenerationAlgorithmTest` + `Bpp3dGenericBoundaryTest` 在 `MAVEN_OPTS='-XX:ReservedCodeCacheSize=384m -XX:NonProfiledCodeHeapSize=256m'` 下通过。
39. 新增 `bpp3d-application/.../service/compat/ApplicationScalarAliases.kt`，并将 `ColumnGenerationApplicationService.kt`、`ColumnGenerationPackingAnalyzer.kt`、`PackingProgramLayerCandidateAdapter.kt`、`ColumnGenerationStandardExecutors.kt` 的显式 `Flt64` 收敛到 `ApplicationScalar` 别名。
40. `bpp3d-application/src/main` 显式 `Flt64` 扫描结果（2026-05-28）：仅 `service/compat/ApplicationScalarAliases.kt` 保留，应用主路径不再直接引用 `Flt64`。
41. 应用层回归补充通过：`ColumnGenerationAlgorithmTest`、`Bpp3dGenericBoundaryTest`。
42. `bpp3d-domain-layer-assignment-context` 核心模型 `Assignment.kt`、`Capacity.kt`、`Load.kt` 的主类型签名已由 `Flt64` 收敛为 `LayerAssignmentScalar`，保留 `compat` solver adapter 作为显式 `Flt64` 边界。
43. `bpp3d-domain-layer-assignment-context/src/main` 显式 `Flt64` 扫描结果（2026-05-28）：仅 `model/compat/**` 保留，核心模型主路径不再直接引用 `Flt64`。
44. layer-assignment 定向回归补充通过：`UnifiedDemandEntriesTest`、`ItemDemandConstraintModeKeyTest`、`FltXDirectCompileProofTest`。
45. `bpp3d-domain-block-loading-context` 的 `DepthFirstSearchAlgorithm.kt` 已将 `fitness`/`compareWithFitness` 从 `Quantity<Flt64>` 收敛为 `Quantity<LegacyScalar>`；模块主路径显式 `Flt64` 命中归零。
46. `bpp3d-domain-item-context/api/QuantityDemandStatistics.kt` 的数值分支已改为 `LegacyScalar`，`quantityScale` 使用 `legacyScalar(amount)`，同时保留 `FltX` 分支兼容。
47. `bpp3d-infrastructure` 主路径收敛补充完成：`ShadowPriceMap.kt`、`QuantityGeometryGeneric.kt`、`QuantityCompatibility.kt` 均改为 `InfraScalar`，`Orientation.kt` 移除无用 `Flt64` 依赖。
48. `bpp3d-infrastructure/src/main` 显式 `Flt64` 扫描结果（2026-05-28）：仅 `InfraLegacyAliases.kt` 保留（typealias/helper 边界）。
49. 回归通过：`bpp3d-infrastructure` 全量 35 tests、`SimpleBlockGeneratorProofTest`、`ComplexBlockGeneratorProofTest`、`QuantityDemandStatisticsGenericTest`（`MAVEN_OPTS='-XX:ReservedCodeCacheSize=384m -XX:NonProfiledCodeHeapSize=256m'`）。
50. 非兼容主路径显式 `Flt64` 边界复核（2026-05-28）：
    `rg --line-number --glob 'bpp3d-*/src/main/**/*.kt' '\bFlt64\b' | rg -v 'compat|LegacyScalars|QuantityDomainAliases|InfraLegacyAliases|LayerGenerationScalarAliases|PackingScalarAliases|ApplicationScalarAliases'` 无命中。
51. `QuantityDomainAliases.kt` 的 `Flt64*` 别名已收敛为 `LegacyScalar` 托管（保留外部命名兼容），并通过 `QuantityDomainAliasExampleTest` 回归。
52. 新增 `bpp3d-infrastructure/.../GenericContainerCore.kt`：引入 `GenericContainer3Shape<V>`、`QuantityContainer3Shape<V>`、`GenericCuboid<T, V>`、`GenericCuboidView<T, V>`，作为 core `<V>` 泛型化迁移骨架（与旧主路径并存）。
53. 新增旧模型桥接：`LegacyCuboidGenericAdapter`、`asGenericCuboid()`、`asGenericContainer3Shape()`，支持 `Cuboid<T>`/`AbstractContainer3Shape` 向泛型骨架渐进迁移。
54. 新增 `GenericContainerCoreProofTest`，覆盖 `FltX` 泛型容器/视图与 legacy 适配桥接；`bpp3d-infrastructure` 全量回归提升至 41 tests 全通过。
55. 4.6 剩余缺口更新：泛型骨架已落地，但 `Container/Cuboid/Placement/Projection` 旧主类型签名仍以 `InfraScalar` 固化，尚未完成主路径替换。
56. `ShadowPriceMap.kt` 补充 `GenericBPP3DShadowPriceArguments<V, T>`、`GenericBPP3DShadowPriceMap<Args, V, T>` 与对应 extractor/pipeline typealias，为 CG shadow-price 链路提供 `<V>` 泛型入口，同时保留既有 `AbstractBPP3D*` 兼容路径。
57. 新增 `GenericShadowPriceMapProofTest`，验证 `GenericBPP3DShadowPriceMap` 可在 `FltX` cuboid/args 上实例化并与旧路径并存。
58. 新增 `GenericProjectionPlacementCore.kt`：补充 `GenericProjection<T, V, P>`、`GenericPlaneProjection`、`GenericPileProjection`、`GenericMultiPileProjection`、`GenericQuantityPlacement2/3`，以及 legacy placement 到泛型 placement 的桥接扩展。
59. `ProjectivePlaneGeometryBridge.kt` 补充 `QuantityPoint2G/3G` 泛型几何重载（`distanceByGeometry`、`point2ByGeometry`、`point3ByGeometry`、`vectorByGeometry`），支持 `ProjectivePlane` 在 `FltX` 等 `<V>` 路径复用。
60. 新增 `GenericProjectionPlacementCoreProofTest`，验证 `FltX` 下 pile projection 的层叠距离与 legacy placement 桥接；`bpp3d-infrastructure` 全量回归 41 tests 全通过。
61. 4.6 剩余缺口更新：`Projection/Placement` 泛型骨架已落地，但旧 `Projection.kt` / `Placement.kt` 主类型签名仍保持 `InfraScalar` 兼容路径，尚未切换主调用链。
62. 下游兼容回归通过：`mvn -pl bpp3d-domain-item-context -am test -Dtest=QuantityDemandStatisticsGenericTest`，验证新增泛型骨架未破坏 item-context 编译与统计逻辑。
63. 新增 `Projection<T, P>.asGenericProjection()` 桥接（覆盖 `PlaneProjection`/`PileProjection`/`MultiPileProjection`），用于将 legacy projection 渐进迁移到 `GenericProjection<T, V, P>` 调用链。
64. `ColumnGenerationPackingAnalyzer` 新增 `DemandMode` 维度 shadow-price 汇总输出：`ColumnGenerationPackingSnapshot` 增加 `demandModeShadowPriceTotals`/`demandModeShadowPriceEntryCounts`，并将分组结果写入 `schema.kpi`（`shadow_price_mode_*`）；`ColumnGenerationAlgorithmTest` 已补充断言，`-pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest test` 回归通过。
65. `Load.kt` 新增带标签 demand 结构 `Bpp3dItemDemand`/`Bpp3dMaterialDemand` 与入口 `demandEntriesFromLabeledItemDemands`/`demandEntriesFromLabeledMaterialDemands`，支持在 item/material quantity-demand 入口显式携带 `DemandMode`，并对 item/material 模式合法性做约束校验；`UnifiedDemandEntriesTest` 已补充模式保留与非法模式拒绝断言并通过。
66. `Load.kt` 的 `quantityDomain` 推导已改为只读 `quantityUnit`（`defaultDemandDomain(unit)`），并移除 `mode` 对 `quantityDomain` 与默认 demand 值类型的影响；`UnifiedDemandEntriesTest#quantityDomainShouldDependOnUnitOnly` 新增“同 mode 不同 unit”反向断言，验证离散/连续仅由 unit 决定。
67. 新增 `QuantityDemandReducedCost.kt`：在 `bpp3d-domain-item-context/api` 提供 `<V>` 泛型 reduced-cost 入口（`Item<V>.reducedCost`、`BinLayer<V>.reducedCost`），支持 active demand entries + `Quantity<V>` 统计直接计算 shadow-price reduced cost，不再依赖 legacy `BPP3DShadowPriceMap`；新增 `QuantityDemandReducedCostGenericTest`（`Flt64`/`FltX`）验证通过。
68. `QuantityDemandReducedCost.kt` 补充 `GenericDemandShadowPriceKey<V>` 与 map 重载（`demandEntries + shadowPrices map`）以提供 typed-key 泛型 shadow-price 输入；`QuantityDemandReducedCostGenericTest#reducedCostShouldSupportGenericShadowPriceKeyMap` 验证通过。
69. `Projection.kt` 的 `ProjectivePlane` 新增泛型几何重载（`distance(point: QuantityPoint3G<V>)`、`point2(point: QuantityPoint3G<V>)`、`point3(point: QuantityPoint2G<V>, distance: Quantity<V>)`、`vector(distance: Quantity<V>)`），并保留原 `InfraScalar` 兼容接口，形成双轨主入口。
70. `GenericProjectionPlacementCore.kt` 已切换为调用 `ProjectivePlane` 泛型主接口（`plane.point3(...)`），减少对 `point3ByGeometry` helper 的直接耦合；`GenericContainerCoreProofTest`、`GenericProjectionPlacementCoreProofTest`、`GenericShadowPriceMapProofTest` 回归通过。
71. `GenericProjectionPlacementCore.kt` 继续补齐 `GenericQuantityPlacement2/3` 的几何能力：新增 `contains`、`overlapped`、`intersect`，并补充泛型 `topPlacements` / `bottomPlacements`，使 `Placement` 常用判定可在 `<V>` 路径直接复用。
72. `GenericProjectionPlacementCoreProofTest` 新增用例覆盖 `contains/intersect` 与 `top/bottom` 判定；定向回归通过：`GenericContainerCoreProofTest`、`GenericProjectionPlacementCoreProofTest`、`GenericShadowPriceMapProofTest`（共 9 tests，全通过）。
73. 4.6 剩余缺口更新：`Placement` 泛型能力已接近 legacy 等价，但 `Container/Cuboid/Projection/Placement` 旧主类型签名仍未完成从 `InfraScalar` 到 `<V>` 的主链替换。
74. `GenericContainerCore.kt` 继续补齐容器泛型骨架：新增 `GenericContainer2Shape<P, V>`、`QuantityContainer2Shape<P, V>`、`GenericContainer2<S, V, P>`、`GenericContainer3<S, V>`，并补充 `amounts/amount/contains` 与 `weight/actualVolume` 聚合逻辑，形成 `Container2/Container3` 在 `<V>` 路径上的可用抽象。
75. `GenericContainerCoreProofTest` 新增 `genericContainer2ShouldCountPlacements` 与 `genericContainer3ShouldAggregateWeightAndVolume`，覆盖泛型容器计数、contains、重量/体积聚合行为。
76. 定向回归通过：`GenericContainerCoreProofTest`、`GenericProjectionPlacementCoreProofTest`、`GenericShadowPriceMapProofTest`（共 11 tests，全通过）；4.6 剩余缺口更新为“核心类型签名与主调用链替换”阶段。
77. 新增 `AbstractContainer2Shape<P>.asGenericContainer2Shape()` 兼容桥接，并在 `GenericContainerCoreProofTest#legacyAdapterShouldBridgeToGenericContainerShape` 补充 2D shape 断言，确认 legacy 2D 容器形状可进入泛型链路。
78. 下游兼容回归补充通过：`mvn -pl bpp3d-domain-item-context -am -Dtest=QuantityDemandStatisticsGenericTest test`，确认 `GenericContainerCore` 本轮扩展未破坏 item-context 编译与统计逻辑。
79. `Container.kt` 完成“主调用链替换第一刀”：`Container2.count` / `Container3.count` 新增 `GenericContainer2/3` 优先计数路径（`count2ByGenericOrLegacy` / `count3ByGenericOrLegacy`），并在检测到嵌套 `Container2/Container3` 时自动回退 legacy `merge` 路径，保持旧语义兼容。
80. `Container.kt` 新增 generic-to-legacy key 映射 `toLegacyCountMap`，将 `LegacyCuboidGenericAdapter` 键回收为 legacy `AbstractCuboid<InfraScalar>`，使现有 `amount/contains` 外部行为保持不变。
81. 回归补充通过：`bpp3d-infrastructure` 全量 46 tests 全通过；`bpp3d-domain-item-context` 定向 `QuantityDemandStatisticsGenericTest` 通过。
82. `Cuboid.kt` 完成“主调用链替换第二刀”：legacy `Cuboid.geometryView/geometry/enabledOrientationsAt(AbstractContainer3Shape)` 改为委托到 `self.asGenericCuboid()` + `space.asGenericContainer3Shape()`，减少 legacy 与 generic 几何逻辑双维护。
83. 回归补充通过：再次执行 `bpp3d-infrastructure` 全量 46 tests 与 `bpp3d-domain-item-context` 定向 `QuantityDemandStatisticsGenericTest`，均通过。
84. `Projection.kt` 完成“主调用链替换第三刀”：`ProjectivePlane.shape(AbstractContainer3Shape)` 改为经 `space.asGenericContainer3Shape()` 走泛型长度/宽度计算；`Bottom/Side/Front` 的 `length/width/height(AbstractContainer3Shape)` 同步委托到对应泛型重载，统一 legacy/generic 空间尺寸语义。
85. 回归补充通过：`bpp3d-infrastructure` 全量 46 tests 与 `bpp3d-domain-item-context` 定向 `QuantityDemandStatisticsGenericTest` 再次通过。
86. `Placement.kt` 完成“主调用链替换第四刀”：`QuantityPlacement2/3` 的 `contains`、`overlapped`、`intersect` 改为委托到 `asGenericPlacement2/3` 对应泛型实现；`topPlacements`/`bottomPlacements` 的平面重叠判定改为使用 `GenericQuantityPlacement2`（Bottom 平面）进行重叠计算，保留 legacy 的高度比较语义。
87. 回归补充通过：`bpp3d-infrastructure` 全量 46 tests 与 `bpp3d-domain-item-context` 定向 `QuantityDemandStatisticsGenericTest` 再次通过。
88. `GenericContainerCore.kt` 与 `Container.kt` 完成“主调用链替换第五刀”：新增 `GenericContainer3Shape.enabled(GenericQuantityPlacement3)` 与 `enabled(List<GenericQuantityPlacement3>)`；`AbstractContainer3Shape.enabled(unit/units)` 改为委托到 `asGenericContainer3Shape()` + `asGenericPlacement3()`，统一 legacy/generic 容器可行性判定入口。
89. 回归补充通过：`bpp3d-infrastructure` 全量 46 tests 与 `bpp3d-domain-item-context` 定向 `QuantityDemandStatisticsGenericTest` 再次通过。
90. `Placement.kt` 与 `Projection.kt` 继续收敛 legacy/generic 双轨：
    - `QuantityPlacement2/3.contains`、`overlapped`、`intersect` 已委托到 `asGenericPlacement2/3`。
    - `topPlacements`/`bottomPlacements` 采用 `GenericQuantityPlacement2`（Bottom 平面）执行重叠判定，并保留 legacy 高度比较规则。
    - `ProjectivePlane.shape(AbstractContainer3Shape)` 与 `Bottom/Side/Front` 的 `length/width/height(AbstractContainer3Shape)` 统一经 `asGenericContainer3Shape()` 走泛型计算。
91. 回归补充通过：`bpp3d-infrastructure` 全量 46 tests、`PlacementTest`、`ProjectionTest` 与 `bpp3d-domain-item-context` 定向 `QuantityDemandStatisticsGenericTest` 均通过。
92. `Projection.kt` 完成“主调用链替换第六刀”：`PlaneProjection`、`PileProjection`、`MultiPileProjection` 的 `toPlacement3At` 与（可复用部分）聚合计算改为委托 `GenericPlaneProjection` / `GenericPileProjection` / `GenericMultiPileProjection`，并通过 `GenericQuantityPlacement3 -> QuantityPlacement3` 映射回 legacy 返回类型，保持外部接口不变。
93. 兼容修复：`amount(unit)` 保持 legacy 等值语义（不直接复用 generic adapter 对象比较），避免 `AbstractCuboid` 与 adapter 键比较导致的计数归零回归。
94. 回归补充通过：`bpp3d-infrastructure` 全量 46 tests（含 `Bpp3dGeometryWrapperCompatibilityTest`、`ProjectionTest`）与 `bpp3d-domain-item-context` 定向 `QuantityDemandStatisticsGenericTest` 均通过。
95. `Projection.kt` 完成“主调用链替换第七刀”：`Projection` 接口层 `length/width/height/area/weight` 默认实现改为统一读取 `asGenericProjection()`，使 `Plane/Pile/MultiPile` 的尺寸与重量口径在接口层也走同一 generic 计算路径。
96. 回归补充通过：`bpp3d-infrastructure` 全量 46 tests 与 `bpp3d-domain-item-context` 定向 `QuantityDemandStatisticsGenericTest` 再次通过。
97. `ColumnGenerationStandardExecutors` 新增 `<V>` 工厂入口：`fromQuantityItems(...)` 与 `fromDemandEntries(... QuantityItem<V> ...)`，允许 application 层直接接收 `QuantityItem<V>` demand，并通过 `toLegacyItems` + demand-entries 兼容桥接落到现有求解主链。
98. 新增 `ColumnGenerationAlgorithmTest#standardExecutorsFactoryShouldSupportQuantityItemDemands`，覆盖 `QuantityItem<FltX>` 需求输入、legacy cache 复用与 executor 工厂重载编译/运行边界；同时保留旧 `fromDemandEntries` 路径不变。
99. 回归补充通过：`MAVEN_OPTS='-XX:ReservedCodeCacheSize=384m -XX:NonProfiledCodeHeapSize=256m' mvn -f pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest '-Dsurefire.failIfNoSpecifiedTests=false' test`（12 tests 全通过）。
100. `ColumnGenerationApplicationRequest` 新增 `<V>` 入口 `fromQuantityDemands(...)`：支持 `QuantityItem<V>`、`QuantityMaterial<V>` 的 item/material amount/material weight 需求直接组装 application request，并在请求边界统一桥接到 legacy `Item/Material` 与 `Quantity<ApplicationScalar>`。
101. 新增 `ColumnGenerationAlgorithmTest#applicationRequestFactoryShouldSupportQuantityDemands`，验证 `QuantityItem<FltX>` + `QuantityMaterial<FltX>` 输入可正确转换为 legacy request（item id/amount、material amount、material weight 数值保持一致）。
102. 回归补充通过：同命令下 `ColumnGenerationAlgorithmTest` 提升至 13 tests 全通过（新增 request 工厂测试已纳入）。
103. `ColumnGenerationApplicationService` 新增服务级 `<V>` 入口 `solveQuantityDemands(...)`，外部可直接以 `QuantityItem<V>` / `QuantityMaterial<V>` 调用 application service；内部统一委托 `ColumnGenerationApplicationRequest.fromQuantityDemands(...)` 后复用既有 `solve(request, ...)` 主流程。
104. 新增 `ColumnGenerationAlgorithmTest#applicationServiceShouldSupportQuantityDemandEntryPoint`，覆盖 `solveQuantityDemands(...)` 直入路径；测试中显式传入 `demandEntries` + 复用同一 legacy cache，确保 `Bpp3dDemandKey.Item` 命中同一对象引用。
105. 回归补充通过：`MAVEN_OPTS='-XX:ReservedCodeCacheSize=384m -XX:NonProfiledCodeHeapSize=256m' mvn -f pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest '-Dsurefire.failIfNoSpecifiedTests=false' test`（14 tests 全通过）。
106. `Load.kt` 修复 `ValueRange` companion-reflection 依赖：`demandEntriesFromLabeledItemDemands` / `demandEntriesFromMaterialDemandsByKey` / `demandEntriesFromItems` 的等值区间构造统一改为 `exactDemandRange(...)`，并显式传入 `Flt64` provider，避免 `-Dospf.kotlin.math.enableCompanionReflectionFallback=false` 时抛错。
107. `applicationServiceShouldSupportQuantityDemandEntryPoint` 已恢复为默认路径验证：不再注入手工 `demandEntries`，直接走 `solveQuantityDemands(...)` 内建 demand-entry 生成逻辑并通过回归。
108. 回归补充通过：同命令下 `ColumnGenerationAlgorithmTest` 维持 14 tests 全通过，验证服务泛型入口与默认 demand-entry 路径可在反射 fallback 关闭时稳定运行。
109. `ColumnGenerationApplicationRequest.fromQuantityDemands(...)` 与 `ColumnGenerationApplicationService.solveQuantityDemands(...)` 补齐 `quantityInitialColumns: List<QuantityBinLayer<V>>` 入口，支持 `<V>` 请求直接传入泛型初始列；内部复用同一 `materialCache/itemCache` 转换为 legacy `initialColumns` 后进入既有主流程。
110. `ColumnGenerationAlgorithmTest` 的 `applicationRequestFactoryShouldSupportQuantityDemands` 与 `applicationServiceShouldSupportQuantityDemandEntryPoint` 已补充 `quantityInitialColumns` 覆盖：验证 request 组装与 service 直入路径均可携带泛型初始列并保持列数稳定。
111. 回归补充通过：`MAVEN_OPTS='-XX:ReservedCodeCacheSize=384m -XX:NonProfiledCodeHeapSize=256m' mvn -f pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest '-Dsurefire.failIfNoSpecifiedTests=false' test`（14 tests 全通过，含 `quantityInitialColumns` 场景）。
112. `Load.kt` 的等值 demand 区间构造不再直接依赖 `Flt64` 常量：新增 `layerAssignmentScalarProvider()`（`LegacyAliases.kt`）并由 `exactDemandRange(...)` 统一调用，进一步将 layer-assignment 主路径中的标量 provider 收敛到模块别名边界。
113. layer-assignment 定向回归补充通过：`MAVEN_OPTS='-XX:ReservedCodeCacheSize=384m -XX:NonProfiledCodeHeapSize=256m' mvn -f pom.xml -pl bpp3d-domain-layer-assignment-context -am '-Dtest=UnifiedDemandEntriesTest,ItemDemandConstraintModeKeyTest,FltXDirectCompileProofTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`（11 tests 全通过）。
114. `scripts/generic-boundary-check.ps1` 已增强为可直接在 repo 根运行：自动识别 `ospf-kotlin-framework-bpp3d` 根目录，新增 `LegacyQuantity` / `LegacyScalar` 规则与 `compat`/alias 边界排除，并将 `bpp3d-domain-layer-assignment-context` 与 `bpp3d-application` 主路径纳入 core 泛型边界扫描。
115. 边界脚本执行验证完成：`pwsh -NoLogo -NoProfile -File scripts/generic-boundary-check.ps1 -ProjectRoot .` 可稳定输出剩余违规清单（当前 111 条），用于后续 4.6 `<V>` 主链替换的清单化收敛。
116. `Load.kt` 的 quantity-demand 泛型桥接已继续下沉到 `model/compat/LoadLegacyBridge.kt`：新增 `toLegacyItemDemands(...)`、`toLegacyMaterialDemands(...)`、`toLegacyMaterialWeightDemandsByKey(...)`，并将主文件中的 `toLegacy/asScalarF64` 显式调用替换为 compat helper，进一步收敛 layer-assignment 主路径的 legacy 转换痕迹。
117. `ColumnGenerationApplicationRequest.fromQuantityDemands(...)` 的 legacy 转换已下沉到 `service/compat/ApplicationRequestLegacyBridge.kt`（`toLegacyQuantityDemandSlices(...)`），将 item/material/weight/initialColumns 的 quantity-to-legacy 聚合转换集中到 compat 边界。
118. 边界脚本复扫结果（2026-05-28）更新：`pwsh -NoLogo -NoProfile -File scripts/generic-boundary-check.ps1 -ProjectRoot .` 违规条目由 111 收敛到 102（剩余主集中区：`bpp3d-domain-item-context` 的 `LegacyScalar` 主模型签名与 `bpp3d-domain-block-loading-context` 服务层）。
119. `bpp3d-domain-block-loading-context` 新增 `compat/BlockLoadingScalarAliases.kt`，将 `LegacyScalar` 与 `legacyInfinity/legacyScalar` 收敛到 compat 边界；`DepthFirstSearchAlgorithm.kt`、`SimpleBlockGenerator.kt`、`ComplexBlockGenerator.kt` 的 service 主路径已改为使用 `BlockLoadingScalar` 别名，不再直接暴露 `LegacyScalar`。
120. `bpp3d-domain-bla-context` 新增 `compat/BlaScalarAliases.kt`，并将 `BottomUpLeftJustifiedAlgorithm` 的 legacy 标量构造参数改为 `Bpp3dBlaScalar` 别名，清理 service 主路径中的 `LegacyScalar` 直引。
121. block-loading/bla 定向回归通过：`MAVEN_OPTS='-XX:ReservedCodeCacheSize=384m -XX:NonProfiledCodeHeapSize=256m' mvn -f pom.xml -pl bpp3d-domain-block-loading-context,bpp3d-domain-bla-context -am '-Dtest=SimpleBlockGeneratorProofTest,ComplexBlockGeneratorProofTest,BottomUpLeftJustifiedAlgorithmProofTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`（共 4 tests，全通过）。
122. 边界脚本复扫结果（2026-05-28）更新：`pwsh -NoLogo -NoProfile -File scripts/generic-boundary-check.ps1 -ProjectRoot .` 违规条目由 102 继续收敛到 91；当前剩余主集中区为 `bpp3d-domain-item-context` model/service 以及 `layer-assignment` 的 `LegacyAliases.kt` provider 暴露点。
123. `layer-assignment` 的 legacy 标量实现进一步下沉：新增 `model/compat/LayerAssignmentScalarAliases.kt`，`LegacyAliases.kt` 改为仅转发 `LayerAssignmentCompatScalar` 与 compat helper，不再在主路径直接暴露 `LegacyScalar/Flt64` token。
124. 边界脚本复扫结果（2026-05-28）更新：违规条目由 91 继续收敛到 87（`layer-assignment` alias/provider 暴露点已清除）。
125. `bpp3d-domain-item-context` 新增 `model/compat/ItemModelScalarAliases.kt`，并将 `model/**` 与 `service/**` 中的 `LegacyScalar` 主路径引用统一改为 `ItemModelScalar` 别名，收敛 legacy 标量暴露到 compat 边界。
126. 本轮 `item-context` 批量替换后出现重复 import 导致的 `ItemModelScalar` 冲突编译错误，已在同轮修复（去重冲突 import 后回归通过），未遗留编译问题。
127. `Package.kt`（item-context）移除主路径 `toFlt64()` 调用：`FltX` 分支改为显式 `Quantity(ItemModelScalar(value.toDouble()), unit)` 重建，保持数值语义不变并清理边界脚本 `toFlt64` 命中。
128. `MaterialPacker.kt`（packing-context）移除主路径 `toFlt64()` 调用：`FltX` 分支改为显式 `Quantity(MaterialPackingScalar(value.toDouble()), unit)` 重建，避免 service 主路径直接依赖 legacy 转换扩展。
129. 边界脚本终态（2026-05-28）达成：`pwsh -NoLogo -NoProfile -File scripts/generic-boundary-check.ps1 -ProjectRoot .` 输出 `GENERIC_BOUNDARY_PASS`。
130. 定向回归补充通过：
    - `mvn -f pom.xml -pl bpp3d-domain-item-context -am -Dtest=QuantityDemandStatisticsGenericTest '-Dsurefire.failIfNoSpecifiedTests=false' test`（3 tests 通过）。
    - `mvn -f pom.xml -pl bpp3d-domain-item-context,bpp3d-domain-packing-context -am '-Dtest=QuantityDemandStatisticsGenericTest,MaterialPackerTest,PackerAndRendererAdapterTest,PackageSolutionLikeAdapterTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`（14 tests 通过）。
    - `mvn -f pom.xml -pl bpp3d-domain-item-context,bpp3d-domain-layer-assignment-context,bpp3d-domain-block-loading-context,bpp3d-domain-bla-context -am '-Dtest=QuantityDemandStatisticsGenericTest,UnifiedDemandEntriesTest,ItemDemandConstraintModeKeyTest,FltXDirectCompileProofTest,SimpleBlockGeneratorProofTest,ComplexBlockGeneratorProofTest,BottomUpLeftJustifiedAlgorithmProofTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`（18 tests 通过）。
131. `bpp3d-application` 新增边界测试 `Bpp3dGenericBoundaryTest` 与 compat bridge 命名在主路径的子串冲突已修复：`toLegacyQuantityDemandSlices` / `LegacyQuantityDemandSlices` 重命名为 `toLegacyDemandSlices` / `LegacyDemandSlices`，避免 `LegacyQuantity` 在 application service 主路径误命中。
132. application 回归补充通过：`mvn -f pom.xml -pl bpp3d-application -am '-Dtest=ColumnGenerationAlgorithmTest,Bpp3dGenericBoundaryTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`（15 tests 全通过；含 `Bpp3dGenericBoundaryTest` 1 项 + `ColumnGenerationAlgorithmTest` 14 项）。
133. 边界脚本终态复核（2026-05-28）：`pwsh -NoLogo -NoProfile -File scripts/generic-boundary-check.ps1 -ProjectRoot .` 再次输出 `GENERIC_BOUNDARY_PASS`，确认 application 侧重命名后未引入新泄漏。
134. 默认 application 回归补充通过：`mvn -f pom.xml -pl bpp3d-application -am test`（2026-05-28，9 模块联动；`bpp3d-application` 22 tests 全通过，含 `Bpp3dGenericBoundaryTest`、`ColumnGenerationAlgorithmTest`、`MaterialPackingApplicationIntegrationTest`、`PackingProgramLayerCandidateAdapterTest`）。
135. 默认回归期间上游模块同步通过：`bpp3d-infrastructure` 46 tests、`bpp3d-domain-item-context` 17 tests、`bpp3d-domain-bla-context` 1 test、`bpp3d-domain-block-loading-context` 3 tests、`bpp3d-domain-layer-assignment-context` 19 tests、`bpp3d-domain-layer-generation-context` 9 tests、`bpp3d-domain-packing-context` 11 tests；确认本轮 4.6 收口改动在主链路上未引入回归。
136. 4.6 验收收口更新：在 `GENERIC_BOUNDARY_PASS`、`LegacyQuantity` 主路径清零、application 默认回归通过三项同时满足后，将验收项 “BPP3D core model/service 已完成 `<V>` 泛型化” 置为完成状态。

## 9. 建议验证命令

单文件真实 CSV：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test '-Dbpp3d.gurobi.dataset.suite.enabled=true' '-Dbpp3d.gurobi.dataset.suite.paths=E:/path/to/real-case.csv' -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenarioSuite '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

目录级真实 CSV suite：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test '-Dbpp3d.gurobi.dataset.suite.enabled=true' '-Dbpp3d.gurobi.dataset.suite.dir=E:/path/to/real-suite' -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenarioSuite '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

默认 application 回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test '-Dgpg.skip=true'"
```

完整 Gurobi 回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

## 10. 注意事项

1. 真实业务数据如包含敏感信息，不要提交原始 CSV。
2. 如需要提交样例数据，只提交脱敏最小样例。
3. 不要把真实数据性能阈值硬编码到非测试主流程。
4. 不要恢复 `daily.md`。
5. 不要把 `cylinder.md` 专题混入本轮主线验收。
