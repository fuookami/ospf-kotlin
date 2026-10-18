# CSP2D 泛型化计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-csp2d` 当前只有顶层 `pom.xml`，没有已跟踪 Kotlin 源文件。

当前没有可迁移源码，泛型化计划作为后续实现准入约束。

## 2. 泛型化目标

1. 物料宽高、板材尺寸、裁切位置、面积使用 `Quantity<V>` 或专用二维量纲类型。
2. 损耗率、利用率、惩罚系数使用裸 `V`。
3. 件数、刀数、模式数量使用整数类型。
4. solver 边界集中转换，不在领域层暴露 `Flt64` 模型。

## 3. 物理量化硬规则

CSP2D 中有量纲字段必须 `Quantity<V>` 化：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 坐标/裁切位置 | `x`, `y`, `cutX`, `cutY` | `Length` |
| 尺寸 | `width`, `height`, `sheetWidth`, `sheetHeight` | `Length` |
| 面积 | `area`, `usedArea`, `wasteArea` | `Area` |
| 重量 | `weight` | `Mass` |
| 产能/加工能力 | `capacity`, `cuttingCapacity`, `cuttingRate` | `Area / Time`、`Amount / Time` 或业务定义单位 |

裸 `V` 只用于损耗率、利用率、系数、评分等无量纲值。

## 4. 建议实施步骤

### Phase C2-0：二维几何基础

参考 BPP2D，先建立：

- `QuantityPoint2<V>`
- `QuantityRect<V>`
- `CutLine<V>`
- `SheetArea<V>`

验收：

- [ ] 支持 Flt64/FltX。
- [ ] 面积单位正确。
- [ ] 不依赖 `Point<Dim2, Quantity<V>>`，除非 Quantity 已实现 FloatingNumber。

### Phase C2-1：领域模型

后续新增对象建议：

- `Material2<V>`
- `Product2<V>`
- `CuttingPattern2<V>`
- `CuttingPlan2<V>`

验收：

- [ ] 领域层无裸 `Double`。
- [ ] `Product2<Flt64>` / `Product2<FltX>` 编译通过。

### Phase C2-2：求解边界

建立：

- `Csp2dModelAdapter<V>`
- `Csp2dFlt64ModelAdapter`

验收：

- [ ] `LinearMetaModel<Flt64>` 只在 adapter 中出现。
- [ ] 结果写回领域模型有显式转换策略。

## 5. 门禁

```powershell
git grep -n "Flt64\\|FltX\\|Double\\|Point<Dim2, Quantity" -- ospf-kotlin-framework-csp2d
```

领域层不允许直接固定求解数值类型。

## 6. 实施记录（2026-10-18）

本文件第 2~5 节的泛型化计划已作为准入约束落实，并在其上补入两项此前未记录的设计决策：
**双供给模式**与**可配置裁切工艺**。

### 6.1 与 csp1d 的关系

沿用 csp1d 的 CSP 语义与模块划分（8 模块），但**不照抄其泛型化程度**。核实结果：csp1d 的实体与接口是泛型的
（`Csp1dAggregation<V : RealNumber<V>>`），但建模管线在**领域模块里**直接绑死 `LinearMetaModel<Flt64>`：

- `csp1d-domain-produce-context/Csp1dModelContext.kt:274` — `interface Csp1dIncrementalPipeline<V : RealNumber<V>> : Pipeline<LinearMetaModel<Flt64>>`
- 各模块 Flt64 命中数：produce 78、material 22、yield 18、length-assignment 22、waste 11、application 60，
  而 cutting-plan-generation 为 0（纯泛型）

csp2d 改为 bpp2d 的分层纪律：领域层全泛型，`FltX`/`Flt64` 只出现在建模边界与 application。

### 6.2 双供给模式（模式与 csp1d 的 length-assignment 对应）

| 模式 | 问题类别 | 物料模型 |
|------|----------|----------|
| 卷材套裁 | 条带排样（幅宽固定、长度待定） | `width` 固定、`length = null` → 由板长分配推导 |
| 定尺板材 | 二维装箱 | `width × height` 固定 |

这正是 csp1d `Material.length: Quantity<V>?` 语义在二维的推广，也是 csp2d 相对 bpp2d 的差异化能力
（bpp2d 只做定尺板材）。

### 6.3 可配置裁切工艺

工艺与模式正交，三种取值：

| 工艺 | 含义 | 设备 |
|------|------|------|
| `Freeform` | 任意排样，只给放置坐标 | 激光/水刀/CNC |
| `GuillotineTwoStage` | 先切条再切件，需切分树 | 剪板机/开料锯 |
| `GuillotineThreeStage` | 切条 → 切块 → 切件，需切分树 | 剪板机/开料锯 |

断头台模式下排样必须由**切分树**产生，且切分树就是给车间的下刀顺序，因此显式建模 `CutTree2`，
而不是只存坐标。异形件明确排除（圆件也排除：直线刀切不出圆）。

### 6.4 Phase C2-0 完成内容

八模块骨架 + `csp2d-infrastructure`：

| 文件 | 内容 |
|------|------|
| `Region2.kt` | 裁切区域（左下角+宽高），`cut` 按刀缝带切成两块，`contains`、`area`、`toBox` |
| `CutTree2.kt` | `CutOrientation2`、`CutLine2`、`CutTree2`（Leaf/Split）、`split2Of` 工厂、`validate` |
| `CuttingProcess2.kt` | 三种工艺，`maxStage`、`requiresCutTree`、`allowsStage`、`stageLabel` |
| `Csp2dErrors.kt` | Validation/Solving/Capability/Internal 四类错误 |
| `dto/RenderDTO.kt` | 刀线、裁切件、裁切图案、裁切方案、顶层 schema DTO |

### 6.5 级数语义（实现中修正过一次）

最初把「级数」实现为「不得回落」（子级 ≥ 父级），结果 2 级工艺下「父级 1、子级 1」被判为合法，
2 级工艺就退化成任意深度，与「先切条再切件」不符。改为经典 **k 级断头台**定义：

> 级数 = 切割层号；根刀线为第 1 级，其下刀线必须**恰为**第 2 级，依此类推。

这样 2 级工艺严格等于两级切割，3 级等于三级；`CutTree2.validate` 校验「子刀线层号恰为父层号 + 1」。
该错误由单元测试 `childCutMustBeExactlyOneLevelBelowParent` 锁定。

### 6.6 验证

| 验证 | 结果 |
|------|------|
| csp2d 反应堆（`csp2d-infrastructure`） | 17 个测试通过（Region2 8 个 + 切分树 9 个） |
| 全仓 `mvn test` | 100 个模块 SUCCESS、4306 个测试、0 失败 0 错误、19 跳过 |

### 6.7 后续阶段

C2-1 物料与图案模型（双模式 Material2、Pattern2、CuttingPlan2、Costar2、Machine2）→ C2-2 图案生成
（任意排样 → 断头台 2 级 → 3 级，可配置切换）→ C2-3 produce/yield/waste/sheet-assignment →
C2-4 application（MILP + 列生成 + 恢复）→ C2-5 双模式 × 三工艺 + 真实 Gurobi → C2-6 模块 README 与 starter 接线。

## 7. 实施记录补完（2026-10-18）

第 6 节之后又推进了四个阶段，本文档在此收尾记录。

### 7.1 C2-4 应用层

- `Csp2dProblem`：物料 + 产品 + 需求 + 工艺 + 刀缝 + 配置 + 良率配置 + 板长分配配置；
  `primaryObjective()` 按物料形态自动选择（卷材→消耗段高、定尺→板材张数），配置可覆盖
- 两条路径共用装配与恢复：`Csp2dMilp`（一次生成全部候选）与 `Csp2dColumnGeneration`
  （逐轮解松弛主问题 → 按剩余需求再生成 → 追加新列 → 最终整数主问题）
- **范围界定（重要）**：C2-4 的列追加由**剩余需求**驱动，不是检验数驱动。框架的 `LPResultOf`
  确实暴露 `dualSolution: Map<Constraint<Flt64, Linear>, Flt64>`，但把对偶映射回具体需求约束需要
  shadow price map 机制（bpp2d 用 `CGPipeline` + `AbstractShadowPriceMap`），本框架尚未实现，
  也未声称已实现。

被纠正的三个 API 事实：`solveLPAs`/`solveMILPAs` 是 suspend；`LPResultOf` 取值用 `solution` getter
（MILP 的 `SolveReport` 才有 `values`）；`FltX.value` 是 internal，测试比较数值要用 `toDouble()`。

### 7.2 C2-5 Gurobi 集成

与 bpp2d 同构：`src/gurobi-test` 源集 + `gurobi-cg-test` profile + `GurobiDelegatingCsp2dSolver`。
7 个用例覆盖 **双模式 × 三工艺** 加列生成路径：

| 模式 | 任意排样 | 断头台 2 级 | 断头台 3 级 |
|------|----------|-------------|-------------|
| 定尺板材 | ✓ | ✓ | ✓ |
| 卷材 | ✓ | ✓ | ✓ |

断言口径是「解可制造」而非仅「求解成功」：解非空、覆盖全部需求、张数为正、每个图案通过
`Pattern2.validate`、断头台方案必须带切分树且 `maxStage ≤ 工艺上限`、任意排样不带切分树、
卷材解给出消耗段高。

### 7.3 C2-6 文档与接线

- 8 个子模块各补中英 README
- starter-csp2d 接入全部 8 个模块，README 中英更新
- 根 README 与本文档更新

### 7.4 最终验收

见第 8 节。

## 8. 验收结果

| 验证 | 结果 |
|------|------|
| csp2d 单元/契约测试 | 76 个通过（infrastructure 17、material 20、pattern 13、produce 6、yield 3、waste 3、sheet-assignment 6、application 8） |
| csp2d Gurobi 集成 | 7 个用例通过（双模式 × 三工艺 + 列生成路径） |
| geometry2d | 16 个测试通过 |
| bpp2d | 117 个测试通过 + Gurobi 10 个 |
| 全仓 mvn test | 100 个模块 SUCCESS、4365 个测试、0 失败 0 错误、19 跳过 |

### 8.1 明确未实现（非目标）

1. **检验数驱动的定价**：当前列追加由剩余需求驱动。对偶已可从 `LPResultOf.dualSolution` 取到，
   但需要 shadow price map 机制才能映射到需求约束，属后续工作。
2. **余料库存**：只做余料可用性汇总（按最小可用余料阈值判定），不做跨工单库存与优先消耗。
3. **异形件与圆件**：直线刀切不出非矩形轮廓，二者均排除。
4. **k 级断头台的级数上限细化**：当前 `GuillotineTwoStage`/`GuillotineThreeStage` 通过
   方向联动的级数规则约束，未区分「同向多刀是否允许跨级重用」这类更细的工艺约定。
