# CSP1D CuttingPlan 训练计划

日期：2026-05-21

## 1. 目标

CSP1D 的列生成主框架中，`CuttingPlan` 是主问题可选择的列。实际业务会保留最终人工调整后的 `CuttingPlan`，这些结果可以作为训练数据，用于生成或排序更接近人工经验的切割方案。

训练模块目标：

1. 从历史人工调整结果中提取可复用的 `CuttingPlan` 样本。
2. 在初始切割方案生成和 pricing 阶段提供候选 `CuttingPlan` 来源。
3. 在不破坏现有幅宽、刀数、配规、缺陷、动态卷长和产出约束的前提下，提高列质量和求解效率。

非目标：

1. 不用模型替代切割方案可行性判断。
2. 不用模型替代主问题 MILP / column generation。
3. 不把训练逻辑抽成跨 BPP3D/CSP1D 的公共模块；CSP1D 单独维护。

## 2. 核心原则

1. 模型只生成候选 `CuttingPlan`，所有候选方案必须经过 `Material.enabled(...)` 和 cutting plan validation。
2. 精确定价、DFS、FullSum、N-Same、N-Sum、MILP pricing 仍作为兜底。
3. 默认不改变现有流程；启用训练来源后，作为额外 `CuttingPlanSource` 注入。
4. 训练样本必须保留物理量单位，宽度、长度、重量使用 `Quantity<V>`。

## 3. 参数训练边界

如果人工最终采用的 `CuttingPlan` 本来就能由现有算法生成，那么优先怀疑的是参数不一致，而不是列生成能力不足。此时应训练 generation 参数、objective weights、soft constraint penalty、排序/换产参数，而不是直接训练生成式 `CuttingPlan`。

CSP1D framework 不提供完整参数训练平台。完整参数训练需要参数存储、样本管理、实验调度、评估函数、版本管理和部署流转，这些能力依赖具体业务系统，必须由使用者自行具备。

framework 只提供参数训练所需的最小抽象：

1. 可观察性：输出 problem snapshot、候选 `CuttingPlan`、主问题结果、人工调整结果、KPI、shadow price 和列统计。
2. 可调性：把 cutting plan generation、pricing、master objective、soft constraint、sorter 参数显式暴露为输入。
3. 可评估性：提供统一 evaluator 接口，允许使用者用自己的参数集重复求解并计算指标。
4. 可接入性：允许使用者把训练出的参数集重新作为 configuration 输入 framework。

建议抽象：

```kotlin
interface Csp1dTunableParameterSet

data class Csp1dParameterTrial<V>(
    val problem: Csp1dProblem<V>,
    val parameters: Csp1dTunableParameterSet,
    val solution: Csp1dSolution<V>?,
    val metrics: Csp1dTrainingMetrics<V>
)

interface Csp1dParameterEvaluator<V> {
    suspend fun evaluate(
        problem: Csp1dProblem<V>,
        parameters: Csp1dTunableParameterSet
    ): Csp1dParameterTrial<V>
}
```

不进入 framework 的内容：

1. 训练样本数据库。
2. 参数实验平台。
3. 自动调参任务调度。
4. 深度学习框架。
5. 业务参数推荐服务。

使用者必须自行负责：

1. 保存每次求解使用的参数版本。
2. 保存人工调整后的最终结果。
3. 定义“接近人工”的 loss。
4. 执行多参数实验和训练。
5. 决定线上使用哪个参数集。

参数/列来源诊断闭环：

```text
人工 CuttingPlan
  -> 是否可行？
  -> 当前生成器能否生成？
  -> 是否进入候选列池？
  -> 加入列池后 master 是否选择？
  -> 当前 objective 下 reduced cost 是否合理？
```

诊断结论：

| 结果 | 优先处理 |
|------|----------|
| 不可行但人工使用 | 校正约束参数或数据映射 |
| 可行但生成器没生成 | 训练 generation 参数或增加历史列来源 |
| 已生成但没被选中 | 训练 master objective / penalty 参数 |
| 被选中但数量不同 | 训练需求范围、slack、软约束 penalty |
| 方案相同但顺序不同 | 训练 sorter / 换产参数 |

## 4. 数据采集

需要采集：

1. problem snapshot：products、costars、materials、machines、width range、cut range、defects、configuration。
2. demand snapshot：产品需求、配规需求、动态卷长需求、订单/日期维度需求。
3. master problem 信息：iteration、shadow price、reduced cost、column usage amount、objective。
4. 算法输出：初始列、pricing 生成列、最终选择列。
5. 人工调整结果：最终 `CuttingPlan`、最终使用车次、排序后的计划序列。

建议事件：

```kotlin
data class Csp1dCuttingPlanTrainingRecord<V>(
    val problemId: String,
    val iteration: UInt64?,
    val materialId: String,
    val machineId: String?,
    val restDemand: Map<String, String>,
    val shadowPrices: Map<String, Flt64>,
    val cuttingPlan: CuttingPlan<V>,
    val planStatistics: Csp1dCuttingPlanStatistics<V>,
    val source: Csp1dCuttingPlanSourceType,
    val selected: Boolean,
    val usageAmount: UInt64,
    val manuallyAdjusted: Boolean
)
```

具体 PO/DTO 由下游决定，framework 只提供抽取接口和字段语义。

## 5. 样本提取

### 5.1 CuttingPlan 标准化

历史 `CuttingPlan` 入库前必须执行：

1. canonicalize：统一产品/配规顺序、宽度精度、切割序列和 segment 表达。
2. feasibility check：幅宽范围、刀数范围、同单位长度边/中约束、单位批次、最大超长、缺陷/子切。
3. deduplicate：按 material、products、costars、segments、length 生成 canonical key 去重。

### 5.2 统计标签

每个样本需要记录：

1. 产品数量贡献：`Map<Product, UInt64>`。
2. 产品重量贡献：`Map<Product, Quantity<V>>`。
3. 配规数量贡献：`Map<Costar, UInt64>`。
4. 配规重量贡献：`Map<Costar, Quantity<V>>`。
5. 余宽、余长、满幅率、余料率、刀数、是否违反软规则。

## 6. 模型入口

建议在 CSP1D 内新增专属接口：

```kotlin
interface Csp1dCuttingPlanSource<V> {
    suspend fun generate(
        problem: Csp1dProblem<V>,
        material: Material<V>,
        shadowPrices: Csp1dShadowPrices<V>? = null,
        limit: UInt64
    ): List<CuttingPlan<V>>
}
```

实现类型：

1. `HistoricalCuttingPlanSource`：历史相似问题检索。
2. `RankedHeuristicCuttingPlanSource`：对 DFS/FullSum/N-Same/N-Sum/MILP 候选列排序。
3. `NeuralCuttingPlanSource`：由模型生成产品/配规组合、宽度序列和 segment。

## 7. 接入流程

### 7.1 初始列

在初始 cutting plan generation 阶段：

1. 按 material width、product width、demand profile、costar profile 检索历史方案。
2. 映射到当前产品/配规/物料。
3. 校验可行性。
4. 加入初始列池。

### 7.2 Column Generation 迭代

每轮主问题求解后：

1. 读取产品、配规、物料和软约束相关 shadow price。
2. 调用 `Csp1dCuttingPlanSource.generate(...)`。
3. 校验 `CuttingPlan`。
4. 计算 reduced cost。
5. 将有效负 reduced cost 或高价值方案加入列池。

### 7.3 兜底

ML 来源不能作为收敛证明。只有既有 pricing 逻辑确认没有可加入列时，主流程才能进入收敛判断。

## 8. 训练路线

优先级应先做参数学习，再做候选列学习：

1. 参数学习：拟合 `CSP1DConfiguration`、generation pruning/top-k/阈值、objective weights、soft constraint penalty、sorter/换产参数。
2. 检索式列来源：按相似物料/产品宽度/需求结构召回历史 `CuttingPlan`。
3. 排序式列来源：训练模型对已有候选 `CuttingPlan` 打分，用于 top-k 选择。
4. 生成式列来源：模型直接生成 cut sequence / product-costar composition，再由 OR 校验。

第一期重点是建立历史列复用和数据闭环，不急于上深度生成模型。

## 9. 特征建议

输入特征：

1. material：宽度范围、步长、长度、单位重量、刀数范围、缺陷、机器。
2. product：可选宽度、长度、单位重量、需求范围、优先级、动态长度。
3. costar：可选宽度、长度、最大超长、兼容性。
4. demand：剩余卷数、剩余重量、剩余张数、订单日期。
5. dual：产品需求 shadow price、配规 shadow price、物料/机器/软约束 shadow price。

输出标签：

1. 切割序列。
2. 产品/配规和宽度组合。
3. segment / defect-costar 布局。
4. 使用车次。
5. reduced cost、余宽、软约束违反量。

## 10. 验收指标

1. 可行率：模型输出 `CuttingPlan` 通过 validation 的比例。
2. 命中率：最终被使用 `CuttingPlan` 在模型 top-k 中的比例。
3. 负 reduced cost 命中率：模型输出有效负 reduced cost 列的比例。
4. 重复率：模型输出与已有列重复的比例。
5. 求解时间：启用训练来源后总耗时变化。
6. 目标值：最终 objective 不劣化。
7. 人工调整距离：最终方案与人工调整方案的差异下降。
8. 参数复现率：使用训练参数后，无 ML 列来源时也能更接近人工方案。

## 11. 实施步骤

1. 定义 `Csp1dTunableParameterSet` 和 `Csp1dParameterEvaluator`。
2. 定义 `Csp1dCuttingPlanTrainingRecord` 和 `Csp1dCuttingPlanSource`。
3. 实现 `CuttingPlan` canonical key 和 validation wrapper。
4. 实现从最终人工方案采样的 extractor。
5. 实现参数试验记录与 evaluator adapter。
6. 实现 `HistoricalCuttingPlanSource`。
7. 在初始列生成阶段接入 historical source。
8. 在 CG pricing 阶段接入 shadow-price aware source。
9. 补齐 Flt64/FltX/Quantity 场景下的训练样本回归测试。

## 12. 门禁

- [ ] 训练来源默认关闭。
- [ ] 启用训练来源后，所有候选 `CuttingPlan` 必须经过可行性校验。
- [ ] DFS/FullSum/N-Same/N-Sum/MILP pricing 兜底仍可运行。
- [ ] 旧 CSP1D 流程在无训练数据时行为不变。
- [ ] 宽度、长度、重量等训练字段保留 `Quantity<V>` 单位信息。
- [ ] framework 不内置参数存储、训练调度和参数推荐服务。
- [ ] 文档提示使用者必须自行具备参数训练和部署能力。
