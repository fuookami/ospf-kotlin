# BPP2D 泛型化计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-bpp2d` 当前只有顶层 `pom.xml`，没有已跟踪 Kotlin 源文件，也没有子模块。

当前没有需要迁移的固定数值实现。后续实现 BPP2D 时，应直接参考 BPP3D 的泛型化经验，但避免复制 BPP3D 计划里 `Point<Dim*, Quantity<V>>` 的类型约束问题。

## 2. 泛型化目标

1. 宽、高、面积、重量等物理量使用 `Quantity<V>` 或专用几何物理量类型。
2. 坐标与矩形相交计算不要直接依赖 `Point<Dim2, Quantity<V>>`，除非 `Quantity<V>` 已实现完整 `FloatingNumber<Quantity<V>>`。
3. 成本、利用率、松弛量使用 `V`。
4. 件数、箱数、分组编号使用整数类型。

## 3. 物理量化硬规则

二维装箱/排样中的有量纲字段必须 `Quantity<V>` 化：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 坐标 | `x`, `y`, `left`, `bottom` | `Length` |
| 尺寸 | `width`, `height` | `Length` |
| 面积 | `area`, `usedArea`, `restArea` | `Area` |
| 重量/承重 | `weight`, `maxLoad` | `Mass` |
| 产能/处理能力 | `capacity`, `throughput` | `Amount / Time` 或业务定义单位 |

无量纲值才保留裸 `V`，例如利用率、旋转惩罚、评分、归一化目标值。

## 4. 建议路线

### Phase B2-0：基础几何量决策

在写业务模型前，先选择：

1. 新增 `QuantityPoint2<V>` / `QuantityRectangle<V>`。
2. 或让 `Quantity<V>` 完整实现 `FloatingNumber<Quantity<V>>` 后复用 math `Point`。

推荐第 1 种，边界更小。

验收：

- [ ] 可表达二维位置、宽高、面积。
- [ ] 矩形相交面积单位为面积量纲。
- [ ] Flt64 与 FltX 特化都能编译。

### Phase B2-1：领域模型

后续新增对象建议直接采用：

- `RectangleItem<V>`
- `Sheet<V>`
- `Placement2<V>`
- `Pattern2<V>`
- `PackingPlan2<V>`

验收：

- [ ] `RectangleItem<Flt64>` / `RectangleItem<FltX>` 编译通过。
- [ ] 面积、利用率、剩余空间计算不固定 `Flt64`。

### Phase B2-2：算法与 solver 边界

启发式算法保持泛型；MILP/LP 建模集中在 adapter：

- `Bpp2dModelAdapter<V>`
- `Bpp2dFlt64ModelAdapter`

验收：

- [ ] 核心领域层无 `LinearMetaModel<Flt64>`。
- [ ] solver 返回值写回 `Placement2<V>` 时走显式转换。

## 5. 门禁

```powershell
git grep -n "Flt64\\|FltX\\|Double\\|Point<Dim2, Quantity" -- ospf-kotlin-framework-bpp2d
```

允许 `Flt64` 只出现在兼容层、adapter 和测试里。

## 6. 真实消费者落盘（2026-05-25）

本轮按 geometry N17-N21 要求，先在 bpp2d 落地最小真实业务消费者：

1. 新增生产模型 `RectangularPackingDemand.kt`：
   - `RectangleItem2` / `Sheet2` / `PlannedRectangle2`
   - `Projection2Need` / `Placement2Need` / `Box2Need`
   - `PackingScene2`（边界校验与重叠对判）
2. 映射关系：
   - `Projection2Need` 对应未来 `Projection2` 的最小矩形表达。
   - `Placement2Need` 对应未来 `Placement2` 的放置表达。
   - `Box2Need` 对应未来 `Box2` 的边界与相交表达。
3. 说明：
   - 当前未引入 bpp3d-infrastructure 依赖，保持 bpp2d 生产代码独立。
   - 待 `quantity-geometry` 模块出现后，`*Need` 模型可迁移为模块外部 API 的直接消费者。

## 7. 稳定性迭代（2026-05-25）

本轮按 geometry N22-N26 要求，验证第二个迭代继续复用同一套 `*Need` API：

1. API 冻结范围（无破坏变更）：
   - `Projection2Need(width, height, area)`
   - `Placement2Need(x, y, projection, maxX, maxY, toBox2Need())`
   - `Box2Need(minX, minY, maxX, maxY, width, height, area, overlaps/intersect/inside)`
2. 第二个真实业务场景：
   - 在 `PackingScene2` 增加 `usedArea/remainingArea/utilization/illegalOverlaps`。
   - 场景聚焦“非法重叠报告 + 已用面积 + 剩余面积”，仍基于同一套 `*Need` 模型。
3. 契约测试：
   - 新增 `RectangularPackingContractTest`。
   - 覆盖未来 `Projection2/Placement2/Box2` 映射契约、单位转换和 `FltX` 路径。

## 8. 框架搭建与 Gurobi 接入（2026-10-17）

本轮把 bpp2d 从「单 jar 最小需求内核」升级为与 bpp3d 同构的 8 模块框架，并接入真实 Gurobi 打通列生成全链路。

### 8.1 模块拆分

| 模块 | 职责 |
|------|------|
| `bpp2d-infrastructure` | 量纲几何、装载形状（矩形/圆形）、板材形状、放置、渲染 DTO、领域错误 |
| `bpp2d-domain-item-context` | 物品/板材/层/排样程序/需求统计/求解器值适配器 + `*Need` 兼容桥接 |
| `bpp2d-domain-bla-context` | 二维底部向上左对齐排样（矩形件） |
| `bpp2d-domain-block-loading-context` | 二维空间、简单/复杂块生成、DFS 与多行空间切分 |
| `bpp2d-domain-layer-generation-context` | 候选层生成（块/切分/BLA/图样/圆形网格/混合） |
| `bpp2d-domain-layer-assignment-context` | 列生成模型（赋值/负载/容量/需求约束/目标），唯一接触 `FltX` |
| `bpp2d-domain-packing-context` | 终态几何校验、装箱器、物料汇总、渲染适配器 |
| `bpp2d-application` | 列生成编排、标准执行器、应用服务、KPI 分析器 |

父 pom 由 `jar` 改为 `pom`，子模块统一使用 `src/main` / `src/test`（与 bpp3d 一致）。

### 8.2 与既有 `*Need` 模型的关系

既有 `RectangularPackingDemand.kt` 与 `NeedGeometryMapping.kt` 保留在原包 `...bpp2d.domain`（下游零破坏），
并新增 `LegacyBridge.kt` 提供到新模型的双向转换：

| 既有 API | 新模型 |
|----------|--------|
| `RectangleItem2<V>` | `ActualItem<V>`（经 `toActualItem()`） |
| `Sheet2<V>` | `SheetType<V>`（经 `toSheetType()`） |
| `PlannedRectangle2<V>` | `LayerPlacement2<V>`（经 `toLayerPlacement()`） |
| `Box2Need<V>` | `Box2Geometry<V>`（经 `toBox2Geometry()`） |
| `PackingScene2<V>` | `Layer<V>`（经 `toLayer()` / `toLegacyScene()` 双向） |

### 8.3 面积量纲结论

`ospf-kotlin-quantities` 中 `Area = Length * Length`，`SquareMeter = DerivedPhysicalUnit(Meter * Meter)`，
因此 `width * height` 自动得到 `Area`，与 `Length²` 完全等价，**不引入任何面积包装类型**。

同时新增 `productUnit` / `quotientUnit`：`PhysicalUnit.*` 会把导出量纲的基础幂次再乘一次（`L × L` 得到 `L³`），
因此 bpp2d 在 `QuantityCore.kt` 中按基础量纲幂次自行合成单位，`Meter × Meter` 与 `SquareMeter` 使用的单位一致。

### 8.4 圆形件策略与限制

1. 圆形件只走两种规则网格：`circle-rect-grid`（按直径铺格）与 `circle-hex-grid`（行距 `26/30·d`，奇偶行错半格）。
   行距取 `26/30 ≈ 0.8667`，略大于 `√3/2 ≈ 0.8660`，偏大是保守的（行间更远，不可能重叠），同时仍比直角网格更密。
2. 选择 `26/30` 而非 `√3/2`，是因为泛型数值类型没有 `toDouble()` / `sqrt()`；用整数比可以保持纯泛型实现。
3. 混合形状层（矩形行 + 圆网格）生成后**逐对**用底层几何复核，凡重叠即丢弃，不做近似。
4. 圆形件不参与 BLA、块装载、空间切分与图样生成（这些路径依赖「整行/推挤」语义），并在结果中显式标记跳过。
5. 面积口径：圆形件实际面积 `πr²`，外接面积 `4r²`；KPI 与渲染都使用实际面积。

### 8.5 重叠语义变更

重叠统一为**内部重叠**：相切不算重叠。底层 `QuantityBox2.overlapped` 把相切判为重叠，若沿用该语义，
任何规则网格都无法成立，因此 `Box2Geometry.overlapped` 对矩形×矩形、圆×圆、圆×矩形统一采用严格不等式。

### 8.6 列生成模型

1. **RMP**：每列一个整数变量，`load_i = Σ_j coeff_ij · x_j`，目标为层面积最小化。
2. **最终 MILP**：二维中一个候选层就是一块板材的完整排布，因此变量是**每列的份数** `y_j ∈ ℤ⁺`，
   而不是「板材 × 层」矩阵；板材数量 = Σ y_j。使用矩阵时求解器会重复选择同一列导致超量放置。
3. **份数上界**：`maxCopiesPerColumn` 取「覆盖该列中任一物品需求所需份数」的最小值，从模型层阻断超量。
4. **需求约束**：需求必须被**覆盖**（`load ≥ demand`），可选业务上界（`load ≤ upperBound`）。
   若只注册上界，最小化目标会退化为「一块板材都不用」——这是本轮发现的建模缺陷，已修正。
5. **尾板裁剪**：列生成保证覆盖需求，最后一板通常超出需求量，`ColumnGenerationApplicationService.trimToDemand`
   按需求裁剪后再装箱，并在 KPI 中反映实际件数。
6. **种子列**：首次 LP 必须可行，因此先用候选生成器产出一批种子列，否则空变量模型的 RMP 直接不可行，
   Gurobi 也不会给出对偶解。

### 8.7 Gurobi 接入

1. `bpp2d-application/src/gurobi-test` 源集 + `gurobi-cg-test` profile（`build-helper-maven-plugin` 挂源集、
   surefire 注入 `bpp2d.gurobi.cg.test.enabled=true`），与 bpp3d 同构。
2. `core-plugin-gurobi` 的 `gurobi` 依赖是 `provided`，profile 中显式声明 `gurobi:gurobi:1.0.0`（test 作用域）。
3. 原生库通过 `-DargLine=-Djava.library.path=$env:GUROBI_HOME\bin` 注入（不硬编码绝对路径）。
4. 默认 `mvn test` 通过 surefire `excludes` 排除该测试；运行时需 `-Dtest=GurobiColumnGeneration2DTest` 覆盖排除。

已验证的真实求解结果：

| 用例 | 结果 |
|------|------|
| 纯矩形件（4×3 ×6、2×2 ×8） | LP + 最终 MILP 求解成功，需求全部覆盖，几何合法 |
| 纯圆形件（r=1 ×12、r=0.5 ×16） | 同上，仅出现圆形件 |
| 混合形状（6×4 ×4 + r=1 ×10） | 同上，两种形状都出现 |
| 单个圆形件 KPI | 实际面积 = π，外接面积 = 4 |

### 8.8 门禁与校验

```powershell
# 框架全量测试（不含 Gurobi）
mvn -s D:\environment\apache-maven-3.9.12\conf\settings.xml -f ospf-kotlin-framework-bpp2d/pom.xml test

# Gurobi 集成测试
mvn -s D:\environment\apache-maven-3.9.12\conf\settings.xml `
    -pl ospf-kotlin-framework-bpp2d/bpp2d-application -am test `
    -Pgurobi-cg-test -Dtest=GurobiColumnGeneration2DTest `
    -Dsurefire.failIfNoSpecifiedTests=false "-DargLine=-Djava.library.path=$env:GUROBI_HOME\bin"
```

泛型门禁（生产代码）：

```powershell
git grep -n "Flt64" -- ospf-kotlin-framework-bpp2d/bpp2d-infrastructure `
    ospf-kotlin-framework-bpp2d/bpp2d-domain-item-context `
    ospf-kotlin-framework-bpp2d/bpp2d-domain-bla-context `
    ospf-kotlin-framework-bpp2d/bpp2d-domain-block-loading-context `
    ospf-kotlin-framework-bpp2d/bpp2d-domain-layer-generation-context
```

`Flt64` 仅允许出现在 `bpp2d-domain-layer-assignment-context`、`bpp2d-application` 与测试中。
`RectangularPackingDemand.kt` 的 `.value!!` / `require` 属于既有兼容实现，为保持下游行为不变而保留在
`@Deprecated` 桥接层，新代码一律使用 `Ret` / `Try`。

### 8.9 检验数定价与列筛选（2026-10-18 补齐）

此前 `filterByReducedCostWithResult` 为空，等价于接受全部候选，迭代只能靠上限截断。现已实现真正的定价：

1. `ReducedCost.kt` 提供 `layerReducedCost` / `isImprovingColumn` / `reducedCostComparator`。
   模型为 `min Σ_j area_j·y_j  s.t.  Σ_j a_ij·y_j ≥ d_i`，对偶 `π_i ≥ 0`，故
   `rc_j = area_j·c_area − Σ_i π_i·a_ij`，只有 `rc_j < −tolerance` 的列才入模。
2. `ColumnGenerationStandardExecutors.reducedCostFilter()` 把 RMP 对偶还原为类型化键并完成筛选；
   `ColumnGenerationApplicationService` 已接线。
3. 效果：LP 目标单调不增、无改进列时自然终止，不再依赖迭代上限。

### 8.10 尾板优化（2026-10-18 补齐）

`TailSheetOptimizer` 取代原「先放先得」裁剪：对剩余需求枚举全部候选列，按「覆盖件数最多 → 超出件数最少 →
优先原列」重选尾板用列，再按剩余需求裁剪放置。测试覆盖了「换列后废弃件数从 6 降到 3」的场景。

### 8.11 刀缝 kerf（2026-10-18 补齐）

1. `infrastructure/Kerf.kt`：`inflatedBy` 按间距膨胀形状（矩形宽高各加 spacing；圆直径加 spacing）、
   `pitchOf` 给出「包围尺寸 + spacing」节距、`zeroSpacingOf` 提供零间距。
2. `Bpp2dLayerGenerationRequest.kerf` + `kerfSpacing(sample)`（不抛异常的零值回退），
   以下路径全部按节距排布：图样、圆形直角/六角网格、简单块、复杂块、DFS、多行、BLA。
3. `PackingGeometryGuard` 与 BLA 的可行性判定改为**双向膨胀**：只有把任一侧膨胀后仍相交，才算真实间隙小于 kerf。
   单向膨胀只能发现一侧的间隙不足，会让 BLA 产生「贴边但不算重叠」的非法结果。
4. `ColumnGenerationApplicationRequest.kerf` → 层生成请求与 `Packer` 的 guard 校验，端到端生效。

### 8.12 余料报告（2026-10-18 补齐）

`FreeSpaceAnalyzer` 按水平切片扣减已用区域，输出互不重叠的自由矩形 `Remnant2(sheetName, rectangle, area)`，
按面积降序。`PackingResult.remnants` 承载结果，KPI 新增 `remnantCount` / `remnantArea`。
**范围说明**：这是余料**报告**，不含跨工单余料库存与优先级复用（属仓储语义）。

### 8.13 CSV 数据集协议（2026-10-18 补齐）

`Bpp2dCsvDatasetLoader` 支持两种形态：

1. `grouped-layer`：`group_index` / `layer_index` / `item_id` / `amount`；
2. `material-width-amount`：`material` / `width` / `amount`。

两种形态都支持二维专属列 `height_meter`、`radius_meter`、`shape_type`（`rectangle` / `circle`）、
`allow_rotate`，以及可选的板材列 `sheet_width_meter` / `sheet_height_meter`。
圆形件可用 `radius_meter` 直接给半径，也可用 `width` 作为直径推导半径。
schema 门禁：重复列、未知列、必填列缺失、非法枚举、非正数、行内列数不匹配、同一物品几何不一致，
全部以 `Ret` 拒绝而不抛异常。样例数据集位于 `bpp2d-application/src/test/resources/gurobi/`。

### 8.14 仍未实现（明确非目标）

1. 异形件（不规则多边形）排样：bpp3d 亦未提供；需要 NFP/no-fit polygon、连续旋转角度与几何布尔运算，
   与现有「矩形 / 圆」抽象不是一个量级，作为独立能力另行设计。
2. 跨工单余料库存与余料优先消耗。
3. 多板材类型混合目标的额外权衡项（当前以板材数量为主目标）。
### 8.15 多板材类型与主板材/尾板材（2026-10-18 补齐）

对应「40GP / 40HQ / 20GP 混合」场景。整体目标仍是**余料最少**，板数与类型成本作为可配的次级权重。

**bpp3d 的对照结论**：bpp3d 有位置性的尾箱（`TailBinAssignmentConstraint` 把列表最后一个 bin 标为尾箱，
配合 `TailBinLoadingRateMinimization`），但**没有**多箱型差异化计价——`ColumnGenerationStandardExecutorConfig`
只有一个 `finalBinAmountCoefficient`，且候选 bin 必须由调用方逐个构造传入，框架不按类型展开。
另外 core 的多目标机制把同类别（`Minimum`）子目标的系数**逐项相加**，所以 bpp3d 的「板数 + 体积」实际是无权重相加。

**本框架的实现**：

1. **按类型生成候选列**：`Bpp2dLayerGenerationRequest.sheets: List<SheetType<V>>`，
   `LayerGenerationContext` 对每种板材分别生成，列通过 `Layer.sheet` 携带类型。
   `Layer.equals` / `hashCode` 已纳入板材类型，避免「同排布不同板材」被错误去重。
2. **主目标 `SheetAreaMinimization`**：`min sheetAreaWeight · Σ_j sheetArea_j · y_j`。
   之所以等价于余料最少：需求占用面积固定，`总余料 = Σ 板材面积 − 需求占用面积 = Σ 板材面积 − 常数`。
   **不能**直接对 `sheetArea_j − layerArea_j` 求和——列是整板排布，铺满的整板余料记为 0，
   会让「一块铺满的大板」优于「多块小板」，与裁剪后的真实余料相反（这是本轮验证时发现并修正的建模错误）。
   真实余料由 `layerWasteArea` / `layerWasteRatio` 在裁剪后统计，用于报告与 KPI。
3. **次目标 `SheetAmountMinimization`**：系数 = `sheetAmountWeight · finalSheetAmountCoefficient`，
   默认 `sheetAmountWeight = 1e-6`，只用于在余料相同的解之间打破平局，避免多选零余料整板。
4. **次目标 `SheetTypeCostMinimization`**：`min sheetTypeCostWeight · Σ_j typeCost(type_j) · y_j`，
   用于表达「同余料时优先 40GP 而非 40HQ」这类偏好；未配置类型按 0 处理。
5. **无限供给**：`maxCopiesPerColumn` 已按列封顶，因此不需要给无穷上界；当某类型不限量且为主板材时，
   「主板材铺满 + 尾板收尾」由 MILP 与 `TailSheetOptimizer` 共同自然产生，尾板余料已计入目标。

配置入口：`ColumnGenerationStandardExecutorConfig` 的 `sheetAreaWeight` / `sheetAmountWeight` /
`sheetTypeCostWeight` / `sheetTypeCosts` / `sheetTypeAmountCoefficients`。

验证（真实 Gurobi）：
- 小板 10×6 与大板 20×12 同时供给、需求 3 件 4×3：只用小板（板材面积消耗 60 < 240）。
- 给小板配置高类型成本后改用大板，证明类型成本项确实参与目标。
