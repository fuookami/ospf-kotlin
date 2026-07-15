# 层分配限界上下文 领域模型

:us: [English](domain-model.md) | :cn: 简体中文

[toc]

## 一、概述

层分配上下文负责将货物（Item）通过层（BinLayer）分配到箱子（Bin）中，采用列生成（Column Generation）两阶段求解架构：第一阶段通过不精确聚合生成候选层并求解松弛线性规划（RMP），第二阶段通过精确聚合将层分配到具体箱子并求解混合整数线性规划（MILP）。

### 1. 依赖上下文

1. **BPP3D 基础设施层**（bpp3d-infrastructure）— 几何模型、影子价格映射、求解器值适配器
2. **BPP3D 货物域**（bpp3d-domain-item-context）— 货物、物料、需求模型

---

## 二、概念/实体

### 1. 箱型（BinType）

箱子的类型定义，包含三维尺寸、承载能力和类型标识。

**$w(B)$** ：箱型 $B$ 的宽度，物理量，单位为长度。
**$h(B)$** ：箱型 $B$ 的高度，物理量，单位为长度。
**$d(B)$** ：箱型 $B$ 的深度，物理量，单位为长度。
**$cap(B)$** ：箱型 $B$ 的承载能力，物理量，单位为重量。
**$code(B)$** ：箱型 $B$ 的类型编码，字符串标识。
**$isMain(B)$** ：箱型 $B$ 是否为主箱型，布尔值。

### 2. 箱子（Bin）

箱子实例，由箱型和放置的层组成。

**$type_{i}$** ：箱子 $i$ 所属的箱型。
**$units_{i}$** ：箱子 $i$ 中已放置的层列表，每个元素为层在箱子中的三维放置信息。
**$batchNo_{i}$** ：箱子 $i$ 的批次号（可选）。
**$cap_{i}$** ：箱子 $i$ 的承载能力，继承自箱型，$cap_{i} = cap(type_{i})$。

### 3. 箱层（BinLayer）

一层货物的排列，是列生成中的基本列单元。每个层对应箱子横截面上的一种货物排列方式。

**$iteration_{l}$** ：层 $l$ 所属的迭代次数，整数。
**$from_{l}$** ：层 $l$ 的来源类型（如 PlaneLayer）。
**$bin_{l}$** ：层 $l$ 对应的箱型（可选），层仅能放入匹配的箱型中。
**$shape_{l}$** ：层 $l$ 的三维形状。
**$units_{l}$** ：层 $l$ 中包含的货物三维放置列表。
**$depth_{l}$** ：层 $l$ 的实际深度，由其中货物的最大 Z 坐标决定。
**$volume_{l}$** ：层 $l$ 的体积，物理量，单位为立方长度。
**$weight_{l}$** ：层 $l$ 的重量，物理量，单位为重量。

### 4. 货物（Item）

待装载的原子货物单元。

**$weight_{k}$** ：货物 $k$ 的重量，物理量，单位为重量。
**$materialAmounts_{k}$** ：货物 $k$ 的物料用量映射，键为 MaterialKey，值为数量。
**$materialWeights_{k}$** ：货物 $k$ 的物料重量映射，键为 MaterialKey，值为重量。

### 5. 需求条目（Bpp3dDemandEntry）

描述一条装载需求，包含需求模式、需求键、需求值和需求范围。

**$mode_{k}$** ：需求条目 $k$ 的需求模式，区分货物/物料及离散/连续域。
**$key_{k}$** ：需求条目 $k$ 的需求键，标识需求针对的货物或物料。
**$demand_{k}$** ：需求条目 $k$ 的需求值，求解器数值。
**$demandRange_{k}$** ：需求条目 $k$ 的需求值域，包含上下界。
**$quantityUnit_{k}$** ：需求条目 $k$ 的量纲单位。

### 6. 求解器值适配器（Bpp3dSolverValueAdapter）

将业务物理量转换为求解器数值类型（FltX）的适配器接口。

**amountToSolver($v$)** ：将数量 UInt64 转换为求解器值。
**lengthToSolver($v$)** ：将长度量转换为求解器值。
**volumeToSolver($v$)** ：将体积量转换为求解器值。
**weightToSolver($v$)** ：将重量量转换为求解器值。
**depthToSolver($v$)** ：将深度量转换为求解器值，默认委托给长度转换。

---

## 三、变量

### 1. 决策变量

**$x_{ij}$** ：层 $j$ 在箱子 $i$ 中的放置次数，非负整数变量，取值范围为 $[0, \lfloor d(B_i) / d(L_j) \rfloor]$，表示层 $j$ 在箱子 $i$ 中叠放的层数，$\forall i \in Bins$，$\forall j \in Layers$，仅当 $type(B_i) = bin(L_j)$ 且层 $j$ 在箱子 $i$ 中启用时取非零范围。

> 精确阶段（MILP）使用二维变量矩阵 $x$。

**$x_{j}^{(t)}$** ：第 $t$ 次迭代中层 $j$ 的使用次数，非负整数变量，取值范围为 $[0, \max_{k}(\lceil d_k / amount(L_j, k) \rceil)]$，表示在列生成第 $t$ 轮中该层被选中的次数，$\forall j \in Layers^{(t)}$。

> 不精确阶段（RMP）使用一维变量向量，每次迭代新增一组。

### 2. 辅助变量

**$u_{ij}$** ：二值化中间符号，表示箱子 $i$ 中是否使用了层 $j$，取值范围为 $\{0, 1\}$，$u_{ij} = \mathbb{1}(x_{ij} > 0)$，$\forall i \in Bins$，$\forall j \in Layers$。

**$v_{i}$** ：二值化中间符号，表示箱子 $i$ 是否被使用，取值范围为 $\{0, 1\}$，$v_{i} = \mathbb{1}(\sum_{j} x_{ij} > 0)$，$\forall i \in Bins$。

**$tail_{i}$** ：尾箱标记变量，表示箱子 $i$ 是否为尾箱（最后一个使用的箱子），取值范围为 $\{0, 1\}$，$\forall i \in Bins$。

---

## 四、谓词

### 1. 层-箱匹配谓词

**matchType(L, B)** ：层 $L$ 的箱型与箱子 $B$ 的箱型匹配，即 $bin(L) = type(B)$。
**enabled(L, B)** ：层 $L$ 在箱子 $B$ 中启用，即 matchType(L, B) 且层的几何形状在箱子中可行。

### 2. 需求模式谓词

**isItem(m)** ：需求模式 $m$ 为货物类型需求（Item / ItemAmount / ItemWeight）。
**isMaterial(m)** ：需求模式 $m$ 为物料类型需求（Material / ItemMaterialAmount / ItemMaterialWeight）。
**isDiscrete(u)** ：需求条目的量纲单位 $u$ 为离散域（如 count）。
**isContinuous(u)** ：需求条目的量纲单位 $u$ 为连续域（如 kg）。

### 3. 箱子状态谓词

**isUsed(B)** ：箱子 $B$ 被使用，即 $v_{i} = 1$。
**isTail(B)** ：箱子 $B$ 为尾箱，即 $tail_{i} = 1$。
**isSameType(B_{i}, B_{i-1})$** ：相邻箱子 $B_i$ 和 $B_{i-1}$ 为同一箱型。

---

## 五、集合

### 1. 箱子

**$B$** ：所有箱子的全集。

**$B^{used}$** ：满足谓词 isUsed 的子集，即被使用的箱子集合。
**$B^{tail}$** ：满足谓词 isTail 的子集，即尾箱集合。
**$B_{T}$** ：箱型为 $T$ 的箱子子集。

### 2. 层

**$L$** ：所有层的全集。

**$L^{(t)}$** ：第 $t$ 次迭代新增的层集合。
**$L^{rem}$** ：已移除的层集合（通过 removeColumn 移除）。
**$L_{B}$** ：可在箱子 $B$ 中使用的层子集，即满足 enabled(L, B) 的层。
**$L_{T}$** ：箱型为 $T$ 的层子集，即满足 $bin(L) = T$ 的层。

### 3. 需求条目

**$K$** ：所有需求条目的全集。

**$K^{item}$** ：满足谓词 isItem 的需求条目子集，货物类需求。
**$K^{mat}$** ：满足谓词 isMaterial 的需求条目子集，物料类需求。
**$K^{discrete}$** ：满足谓词 isDiscrete 的需求条目子集，离散域需求。
**$K^{continuous}$** ：满足谓词 isContinuous 的需求条目子集，连续域需求。

### 4. 箱子-层对

**$BL$** ：可行的箱子-层对集合，$\{(i, j) \mid matchType(L_j, B_i) \wedge enabled(L_j, B_i)\}$。

---

## 六、中间值

### 1. 层负载（Load）

**描述**：每个需求条目上，所有层的贡献之和构成该需求的总负载。在不精确阶段按层聚合，在精确阶段按箱子-层矩阵聚合。

不精确阶段：

$$
load_{k} = \sum_{j \in L} c(L_j, K_k) \cdot x_j, \; \forall k \in K
$$

精确阶段：

$$
load_{k} = \sum_{i \in B} \sum_{j \in L} c(L_j, K_k) \cdot x_{ij}, \; \forall k \in K
$$

其中 $c(L_j, K_k)$ 为层 $L_j$ 对需求条目 $K_k$ 的负载系数，由层的统计信息和需求模式决定。

### 2. 超载（OverLoad）

**描述**：负载超过需求上界的部分，用于软约束松弛。

$$
overLoad_{k} = load_{k} - demandRange_{k}^{ub}, \; \forall k \in K
$$

### 3. 欠载（LessLoad）

**描述**：负载低于需求下界的部分，用于软约束松弛。

$$
lessLoad_{k} = demandRange_{k}^{lb} - load_{k}, \; \forall k \in K
$$

### 4. 装载重量（LoadWeight）

**描述**：每个箱子中所有层的重量之和。

$$
loadWeight_{i} = \sum_{j \in L} weight(L_j) \cdot x_{ij}, \; \forall i \in B
$$

### 5. 装载体积（LoadVolume）

**描述**：每个箱子中所有层的体积之和。

$$
loadVolume_{i} = \sum_{j \in L} volume(L_j) \cdot x_{ij}, \; \forall i \in B
$$

### 6. 装载深度（LoadDepth）

**描述**：每个箱子中所有层的深度之和。

$$
loadDepth_{i} = \sum_{j \in L} depth(L_j) \cdot x_{ij}, \; \forall i \in B
$$

### 7. 装载率（LoadingRate）

**描述**：每个箱子的装载体积与箱子容积之比。

$$
loadingRate_{i} = \frac{loadVolume_{i}}{volume(B_i)}, \; \forall i \in B
$$

### 8. 尾箱装载率（TailLoadingRate）

**描述**：尾箱的装载率，通过遮罩函数仅对尾箱生效。

$$
tailLoadingRate_{i} = loadingRate_{i} \cdot tail_{i}, \; \forall i \in B
$$

### 9. 总体积（Volume）

**描述**：不精确阶段中所有层的总体积，用于列生成 RMP 的目标函数。

$$
volume = \sum_{j \in L} volume(L_j) \cdot x_j
$$

---

## 七、断言

### 1. 层-箱类型一致性

**描述**：层只能放入与其箱型匹配的箱子中。若层的箱型与箱子的箱型不匹配，则该层在该箱子中的放置次数必须为零。

$$
\forall i \in B, \forall j \in L \; (type(B_i) \neq bin(L_j) \rightarrow x_{ij} = 0)
$$

### 2. 层深度上限

**描述**：箱子中所有层的总深度不能超过箱子的深度。

$$
\forall i \in B \; (loadDepth_{i} \leq d(B_i))
$$

### 3. 箱子承载上限

**描述**：箱子中所有层的总重量不能超过箱子的承载能力。

$$
\forall i \in B \; (loadWeight_{i} \leq cap(B_i))
$$

---

## 八、约束

### 1. 需求上界约束

**[英]**：Demand Upper Bound Constraint
**描述**：每个需求条目的负载不得超过需求范围的上界。当启用超载松弛时，使用超载符号。

$$
s.t. \quad load_{k} \leq demandRange_{k}^{ub}, \; \forall k \in K
$$

### 2. 需求下界约束

**[英]**：Demand Lower Bound Constraint
**描述**：每个需求条目的负载不得低于需求范围的下界。当启用欠载松弛时，使用欠载符号。

$$
s.t. \quad load_{k} \geq demandRange_{k}^{lb}, \; \forall k \in K
$$

### 3. 箱子重量容量约束

**[英]**：Bin Weight Capacity Constraint
**描述**：每个箱子中层的总装载重量不得超过箱子的承载能力。

$$
s.t. \quad loadWeight_{i} \leq cap(B_i), \; \forall i \in B
$$

### 4. 箱子深度约束

**[英]**：Bin Depth Constraint
**描述**：每个箱子中层的总装载深度不得超过箱子的深度。

$$
s.t. \quad loadDepth_{i} \leq d(B_i), \; \forall i \in B
$$

### 5. 箱子装载顺序约束

**[英]**：Bin Loading Order Constraint
**描述**：对于相同箱型的相邻箱子，前一个箱子必须先被使用（$v_{i-1} \geq v_i$），且前一个箱子的装载体积不小于后一个箱子（$loadVolume_{i-1} \geq loadVolume_i$）。此约束消除同型箱子的对称性。

$$
s.t. \quad v_{i-1} \geq v_i, \; \forall i \in B \setminus \{0\} \mid type(B_i) = type(B_{i-1})
$$

$$
s.t. \quad loadVolume_{i-1} \geq loadVolume_i, \; \forall i \in B \setminus \{0\} \mid type(B_i) = type(B_{i-1})
$$

### 6. 尾箱分配约束

**[英]**：Tail Bin Assignment Constraint
**描述**：尾箱标记必须与箱子使用状态一致。对于最后一个箱子，$tail_i \leq v_i$；对于其他箱子，$tail_i \leq v_{i+1}$。确保只有最后一个被使用的箱子可被标记为尾箱。

$$
s.t. \quad tail_i \leq v_i, \; i = |B| - 1
$$

$$
s.t. \quad tail_i \leq v_{i+1}, \; \forall i \in B \setminus \{|B| - 1\}
$$

---

## 九、目标函数

### 1. 体积最小化（不精确阶段）

**描述**：在列生成 RMP 阶段，最小化所有层的总体积使用量，引导求解器选择体积更紧凑的层组合。

$$
\min \sum_{j \in L} volume(L_j) \cdot x_j
$$

### 2. 箱子数量最小化（精确阶段）

**描述**：在 MILP 阶段，最小化使用的箱子总数。

$$
\min \sum_{i \in B} c(B_i) \cdot v_i
$$

其中 $c(B_i)$ 为箱子 $B_i$ 的系数函数，通常为 1。

### 3. 更优层最大化（精确阶段）

**描述**：在 MILP 阶段，最大化层与箱子的匹配质量系数，优先选择装载率更高、支撑更好的层组合。

$$
\max \sum_{i \in B} \sum_{j \in L} coeff(L_j, B_i) \cdot x_{ij}
$$

其中 $coeff(L_j, B_i)$ 为层 $L_j$ 与箱子 $B_i$ 的匹配系数函数。

### 4. 尾箱装载率最小化（精确阶段）

**描述**：在 MILP 阶段，最小化尾箱的装载率，避免尾箱残留过多未利用空间。

$$
\min \sum_{i \in B} c(B_i) \cdot tailLoadingRate_{i}
$$

---

## 十、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 列生成（Column Generation） | 框架内置 | 第三章、第六章、第九章 | 不精确阶段的 RMP 求解与列迭代机制 |
| 二值化函数（Binaryzation） | 框架内置 | 第三章辅助变量 | 将整数变量转换为二值指示变量 |
| 遮罩函数（Masking） | 框架内置 | 第六章尾箱装载率 | 通过布尔变量遮罩控制中间符号生效范围 |

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 箱型 | $B$ / BinType | Bin Type | 箱子的类型定义，包含尺寸和承载能力 |
| 箱子 | $B_i$ / Bin | Bin | 箱子实例，由箱型和层组成 |
| 箱层 | $L_j$ / BinLayer | Bin Layer | 一层货物的排列，列生成的基本列单元 |
| 货物 | $k$ / Item | Item | 待装载的原子货物单元 |
| 需求条目 | $K_k$ / Bpp3dDemandEntry | Demand Entry | 一条装载需求，含模式、键、值、范围 |
| 需求模式 | $m$ / Bpp3dDemandMode | Demand Mode | 区分货物/物料及离散/连续域 |
| 需求键 | $key$ / Bpp3dDemandKey | Demand Key | 标识需求针对的货物或物料 |
| 赋值 | $x$ / Assignment | Assignment | 决策变量，层在箱子中的放置次数 |
| 负载 | $load$ / Load | Load | 需求条目上的总装载量 |
| 容量 | $cap$ / Capacity | Capacity | 箱子的装载重量、体积、深度指标 |
| 层聚合 | $LA$ / LayerAggregation | Layer Aggregation | 管理列生成过程中的层集合与去重 |
| 影子价格 | $sp$ / Shadow Price | Shadow Price | 对偶变量，反映约束的边际价值 |
| 装载率 | $lr$ / Loading Rate | Loading Rate | 箱子装载体积与容积之比 |
| 尾箱 | $tail$ / Tail Bin | Tail Bin | 最后一个被使用的箱子 |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 两阶段求解 | 单阶段 MILP / 列生成+MILP | 列生成可处理大规模候选层空间，MILP 保证精确解 | 2024 |
| 不精确/精确分离 | 统一赋值模型 | 两阶段变量结构不同（一维向量 vs 二维矩阵），分离更清晰 | 2024 |
| 求解器值适配器 | 直接使用物理量 / 适配器模式 | 适配器支持单位转换和缩放，解耦业务域与求解器 | 2024 |
| 需求条目统一 | 按货物/物料分别建模 | 统一 Bpp3dDemandEntry 支持多种需求模式，减少重复代码 | 2024 |
| 层去重策略 | 基于 units 内容比较 | 基于货物放置列表的结构相等性去重，避免语义相同的层重复生成 | 2024 |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始层分配模型 | 基础列生成框架 |
| v1.1 | 新增精确阶段赋值模型 | 支持 MILP 精确求解 |
| v1.2 | 需求条目统一为 Bpp3dDemandEntry | 支持货物/物料多种需求模式 |
| v1.3 | 新增求解器值适配器与缩放支持 | 支持单位转换和数值缩放 |
| v1.4 | 新增尾箱标记与装载率优化 | 改善尾箱空间利用率 |
