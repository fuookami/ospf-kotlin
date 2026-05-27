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
5. 合并 item amount/weight demand 与 material amount/weight demand，统一为 `entity + Quantity<V>` demand，并为 unit 增加 `QuantityDomain`。
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

### 4.5 Demand 与 QuantityDomain

item amount demand 与 item weight demand、material amount demand 与 material weight demand 都应合并。它们不是不同领域概念，只是 demand 的 `quantity.unit` 与 entity key 不同。

建议语义：

1. `ItemDemand<V>` 表达为 `item + Quantity<V>`。
2. `MaterialDemand<V>` 表达为 `material + Quantity<V>`。
3. `PackingProgram.materials` 表达为 `Map<MaterialKey, Quantity<V>>` 或等价结构。
4. 离散/连续由 `PhysicalUnit.domain` 表达。
5. 数量、重量、体积等业务含义可由 unit dimension 描述，但不参与算法判定和运算。
6. 算法不再区分 item amount/weight、material amount/weight 多套建模入口，而是根据 entity key 与 unit metadata 归一化为统一 demand constraint。

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
5. `QuantityDimension` 只用于业务展示、校验提示或统计分组，不参与任何算法判定和运算。

兼容要求：

1. 现有 item amount 构造路径可保留为快捷入口，但应转换为 `Quantity<V>`，unit 使用 `QuantityDomain.Discrete`。
2. 现有 item weight 构造路径可保留为快捷入口，但应转换为 `Quantity<V>`，unit 使用 `QuantityDomain.Continuous`。
3. 现有 material amount 构造路径可保留为快捷入口，但应转换为 `Quantity<V>`，unit 使用 `QuantityDomain.Discrete`。
4. 现有 material weight 构造路径可保留为快捷入口，但应转换为 `Quantity<V>`，unit 使用 `QuantityDomain.Continuous`。
5. layer-generation、packing planner、reduced cost 都必须基于统一 demand entries。
6. material contribution 不能再隐含为 item weight，必须以 material composition 的 `Quantity<V>` 为准。

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
10. 合并 item amount/weight demand 与 material amount/weight demand 为统一 demand，并为 unit 增加 `QuantityDomain`。
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
- [x] unit metadata 支持 `QuantityDomain.Discrete`。
- [x] unit metadata 支持 `QuantityDomain.Continuous`。
- [x] demand 建模统一读取 `quantity.unit.domain`。
- [x] `QuantityDomain.Discrete` demand 进入离散约束。
- [x] `QuantityDomain.Continuous` demand 进入连续约束。
- [x] `QuantityDimension` 不参与任何算法判定和运算。
- [ ] BPP3D core model/service 已完成 `<V>` 泛型化。
- [ ] `Material<V>`、`PackageShape<V>`、`Package<V>`、`Item<V>`、`Bin<V>`、`Layer<V>` 不再依赖 `LegacyQuantity`。
- [ ] `Load<V>`、`Bpp3dDemandEntry<V>`、layer assignment constraint/objective 不再依赖 `LegacyQuantity`。
- [ ] layer generation request/result/generator 全链路不再依赖 `LegacyQuantity`。
- [ ] packing planner 与 `PackingProgram.materials` 主路径不再依赖 `LegacyQuantity`。
- [ ] `Flt64*` 仅作为兼容 facade/typealias 或测试实例化存在。
- [ ] `LegacyQuantity` 只允许出现在 `api/compat/**`、legacy facade、明确迁移桥接层或 legacy 测试中。
- [ ] `rg '\bLegacyQuantity\b' bpp3d-*/src/main | rg -v 'api/compat|LegacyScalars|compat|legacy'` 无命中。
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
