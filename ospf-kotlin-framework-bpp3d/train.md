# BPP3D Layer 训练计划

日期：2026-05-21

## 1. 目标

BPP3D 的列生成主框架中，`Layer` / `BinLayer` 是主问题可选择的列。实际业务会保留最终人工调整后的 `Bin`，这些结果可以作为训练数据，用于生成或排序更接近人工经验的 layer。

训练模块目标：

1. 从历史人工调整结果中提取可复用的 `Layer` / `BinLayer` 样本。
2. 在 layer generation / layer selection 阶段提供候选 layer 来源。
3. 在不破坏现有 OR 可行性和收敛逻辑的前提下，提高初始列质量、减少无效列、降低人工调整量。

非目标：

1. 不用模型替代约束校验。
2. 不用模型替代主问题求解。
3. 不把训练逻辑抽成跨 BPP3D/CSP1D 的公共模块；BPP3D 单独维护。

## 2. 核心原则

1. 模型只生成候选列，所有候选列必须经过 BPP3D 既有 feasibility check。
2. 精确定价或启发式定价仍作为兜底，不能用 ML 结果判断列生成收敛。
3. 默认不改变现有流程；启用训练来源后，作为额外 `LayerSource` 注入。
4. 训练样本应记录物理量单位和统计模式，支持 item 数量、item-material 数量、item-material 重量三种口径。

## 3. 参数训练边界

BPP3D framework 不提供完整参数训练平台。完整参数训练需要参数存储、样本管理、实验调度、评估函数、版本管理和部署流转，这些能力依赖具体业务系统，必须由使用者自行具备。

framework 只提供参数训练所需的最小抽象：

1. 可观察性：输出 problem snapshot、候选 layer、最终方案、人工调整结果、KPI、shadow price 和统计向量。
2. 可调性：把 layer generation、layer selection、packing、stacking/support、loading preference 等参数显式暴露为输入。
3. 可评估性：提供统一 evaluator 接口，允许使用者用自己的参数集重复求解并计算指标。
4. 可接入性：允许使用者把训练出的参数集重新作为 configuration 输入 framework。

建议抽象：

```kotlin
interface Bpp3dTunableParameterSet

data class Bpp3dParameterTrial<V>(
    val problem: Bpp3dLayerProblem<V>,
    val parameters: Bpp3dTunableParameterSet,
    val solution: List<Bin>?,
    val metrics: Bpp3dTrainingMetrics<V>
)

interface Bpp3dParameterEvaluator<V> {
    suspend fun evaluate(
        problem: Bpp3dLayerProblem<V>,
        parameters: Bpp3dTunableParameterSet
    ): Bpp3dParameterTrial<V>
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

## 4. 数据采集

需要从最终人工调整后的结果中采集：

1. 原始 problem snapshot：bin、item、package、material、packageAttribute、统计模式。
2. 原始算法输出：生成过的 layer、被主问题选择的 layer、最终 bin。
3. 人工调整结果：最终 `Bin`、最终 placements、可还原的 `BinLayer`。
4. 求解过程信息：iteration、shadow price、reduced cost、loading rate、objective、列来源。
5. 统计向量：item amount、material amount、material weight。

建议事件：

```kotlin
data class Bpp3dLayerTrainingRecord<V>(
    val problemId: String,
    val iteration: UInt64?,
    val binType: String,
    val demandMode: Bpp3dDemandMode,
    val restDemand: Map<String, String>,
    val shadowPrices: Map<String, Flt64>,
    val layer: BinLayer,
    val layerStatistics: Map<String, String>,
    val source: Bpp3dLayerSourceType,
    val selected: Boolean,
    val manuallyAdjusted: Boolean
)
```

`restDemand` 和 `layerStatistics` 可由下游 PO/DTO 决定具体持久化结构，framework 只提供抽取接口和稳定字段语义。

## 5. 样本提取

### 5.1 从 Bin 提取 Layer

优先级：

1. 如果最终 `Bin` 保留了 `BinLayerPlacement`，直接使用其中的 `BinLayer`。
2. 如果只有 item placements，则按 z/depth 连续区间切分 layer。
3. 如果无法稳定切分，记录为 bin-level 样本，只用于训练排序/评估，不直接作为候选列。

提取后必须执行：

1. canonicalize：排序 placements、统一 orientation 表达、归一化坐标。
2. feasibility check：bin 容量、尺寸、承重、堆叠、悬挂、装载顺序、统计需求。
3. deduplicate：与已有 layer 用当前 `equals`/hash 逻辑或 canonical key 去重。

### 5.2 统计标签

每个样本都要计算：

1. `ItemAmount`: `Map<Item, UInt64>`。
2. `ItemMaterialAmount`: `Map<MaterialKey, UInt64>`。
3. `ItemMaterialWeight`: `Map<MaterialKey, Quantity<V>>`。

训练时按当前 demand mode 选择标签，但样本应尽量保存三种统计结果，方便后续切换统计模式。

## 6. 模型入口

建议在 BPP3D 内新增专属接口：

```kotlin
interface Bpp3dLayerSource<V> {
    suspend fun generate(
        problem: Bpp3dLayerProblem<V>,
        shadowPrices: Bpp3dLayerShadowPrices? = null,
        limit: UInt64
    ): List<BinLayer>
}
```

实现类型：

1. `HistoricalLayerSource`：历史相似问题检索。
2. `RankedHeuristicLayerSource`：对既有启发式生成 layer 排序。
3. `NeuralLayerSource`：由深度模型生成 placements 或 pattern。

## 7. 接入流程

### 7.1 初始列

在 layer generation 之前或之后追加历史候选 layer：

1. 根据 bin type、item/material demand、尺寸分布检索历史样本。
2. 映射到当前 item/material。
3. 校验可行性。
4. 加入初始列池。

### 7.2 Column Generation 迭代

每轮主问题求解后：

1. 读取 shadow price。
2. 调用 `Bpp3dLayerSource.generate(...)`。
3. 校验 layer。
4. 计算 reduced cost 或由现有 layer selection 评估。
5. 只把有价值的 layer 加入列池。

### 7.3 兜底

ML 来源失败、返回空、或长期没有有效列时，必须回退到既有 layer generation / exact pricing 逻辑。

## 8. 训练路线

优先级应先做参数学习，再做候选列学习：

1. 参数学习：拟合 layer generation、layer selection objective weights、stacking/support penalty、bin/layer 排序参数。
2. 检索式列来源：按 problem 相似度召回历史 layer，最快落地。
3. 排序式列来源：训练模型对候选 layer 打分，用于 top-k 截断。
4. 生成式列来源：模型直接生成 layer pattern / placements，再由 OR 校验。

第一期不需要深度学习，优先建立数据闭环。

诊断规则：

| 现象 | 优先处理 |
|------|----------|
| 人工 layer 可由现有算法生成，但没被选中 | 训练 objective/penalty/selection 参数 |
| 人工 layer 可生成且被选中，但数量或 bin 组合不同 | 训练 demand slack、bin penalty、排序参数 |
| 人工 layer 现有算法生成不到 | 增加历史列来源或生成式 layer source |
| 人工只调整装箱顺序 | 训练 bin/layer 排序和后处理参数 |

## 9. 验收指标

1. 可行率：模型输出 layer 通过 feasibility check 的比例。
2. 命中率：最终被选中 layer 在模型 top-k 中的比例。
3. 去重率：模型输出与已有列重复的比例。
4. reduced cost 命中率：模型输出负 reduced cost layer 的比例。
5. 求解时间：启用训练来源后主流程耗时变化。
6. 目标值：最终 objective 不劣化。
7. 人工调整距离：最终方案与人工调整方案的差异下降。
8. 参数复现率：使用训练参数后，无 ML 列来源时也能更接近人工方案。

## 10. 实施步骤

1. 定义 `Bpp3dTunableParameterSet` 和 `Bpp3dParameterEvaluator`。
2. 定义 `Bpp3dLayerTrainingRecord` 和 `Bpp3dLayerSource`。
3. 实现从最终 `Bin` 提取 `BinLayer` 的 sample extractor。
4. 实现 layer canonical key 和 feasibility wrapper。
5. 实现参数试验记录与 evaluator adapter。
6. 实现 `HistoricalLayerSource`。
7. 在初始列生成阶段接入 historical source。
8. 在 column generation 迭代中接入 shadow-price aware source。
9. 补齐三种统计模式下的训练样本和回归测试。

## 11. 门禁

- [ ] 训练来源默认关闭。
- [ ] 启用训练来源后，所有候选 layer 必须经过可行性校验。
- [ ] 精确定价/启发式兜底仍可运行。
- [ ] 旧 BPP3D 流程在无训练数据时行为不变。
- [ ] item amount、item-material amount、item-material weight 三种统计模式均可采样。
- [ ] framework 不内置参数存储、训练调度和参数推荐服务。
- [ ] 文档提示使用者必须自行具备参数训练和部署能力。
