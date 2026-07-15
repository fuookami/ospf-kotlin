# Layer Assignment Bounded Context — Domain Model

:us: English | :cn: [简体中文](domain-model_ch.md)

[toc]

## 1. Overview

The Layer Assignment context is responsible for assigning items (Item) to bins (Bin) through layers (BinLayer), using a two-phase column generation architecture: the first phase generates candidate layers via imprecise aggregation and solves a relaxed linear program (RMP), and the second phase assigns layers to specific bins via precise aggregation and solves a mixed-integer linear program (MILP).

### 1. Dependent Contexts

1. **BPP3D Infrastructure** (bpp3d-infrastructure) — geometry models, shadow price maps, solver value adapters
2. **BPP3D Item Domain** (bpp3d-domain-item-context) — item, material, and demand models

---

## 2. Concepts / Entities

### 1. BinType

The type definition of a bin, containing three-dimensional dimensions, load capacity, and type identifier.

**$w(B)$** : Width of bin type $B$, physical quantity in length units.
**$h(B)$** : Height of bin type $B$, physical quantity in length units.
**$d(B)$** : Depth of bin type $B$, physical quantity in length units.
**$cap(B)$** : Load capacity of bin type $B$, physical quantity in weight units.
**$code(B)$** : Type code of bin type $B$, string identifier.
**$isMain(B)$** : Whether bin type $B$ is the primary bin type, boolean.

### 2. Bin

A bin instance, composed of a bin type and placed layers.

**$type_{i}$** : The bin type of bin $i$.
**$units_{i}$** : List of layers placed in bin $i$, each element being a 3D placement of a layer within the bin.
**$batchNo_{i}$** : Batch number of bin $i$ (optional).
**$cap_{i}$** : Load capacity of bin $i$, inherited from the bin type, $cap_{i} = cap(type_{i})$.

### 3. BinLayer

An arrangement of items forming a single layer, serving as the fundamental column unit in column generation. Each layer corresponds to one arrangement of items on a bin's cross-section.

**$iteration_{l}$** : Iteration number when layer $l$ was generated, integer.
**$from_{l}$** : Source type of layer $l$ (e.g., PlaneLayer).
**$bin_{l}$** : Bin type associated with layer $l$ (optional); a layer can only be placed in a matching bin type.
**$shape_{l}$** : 3D shape of layer $l$.
**$units_{l}$** : List of item 3D placements contained in layer $l$.
**$depth_{l}$** : Actual depth of layer $l$, determined by the maximum Z-coordinate of its items.
**$volume_{l}$** : Volume of layer $l$, physical quantity in cubic length units.
**$weight_{l}$** : Weight of layer $l$, physical quantity in weight units.

### 4. Item

The atomic cargo unit to be packed.

**$weight_{k}$** : Weight of item $k$, physical quantity in weight units.
**$materialAmounts_{k}$** : Material usage mapping of item $k$, keyed by MaterialKey with quantity values.
**$materialWeights_{k}$** : Material weight mapping of item $k$, keyed by MaterialKey with weight values.

### 5. Bpp3dDemandEntry

Describes a single loading demand, containing demand mode, demand key, demand value, and demand range.

**$mode_{k}$** : Demand mode of entry $k$, distinguishing item/material and discrete/continuous domain.
**$key_{k}$** : Demand key of entry $k$, identifying the item or material targeted by the demand.
**$demand_{k}$** : Demand value of entry $k$, solver numeric value.
**$demandRange_{k}$** : Demand value range of entry $k$, containing upper and lower bounds.
**$quantityUnit_{k}$** : Quantity unit of entry $k$.

### 6. Bpp3dSolverValueAdapter

An adapter interface that converts business physical quantities to solver numeric types (FltX).

**amountToSolver($v$)** : Converts UInt64 amount to solver value.
**lengthToSolver($v$)** : Converts length quantity to solver value.
**volumeToSolver($v$)** : Converts volume quantity to solver value.
**weightToSolver($v$)** : Converts weight quantity to solver value.
**depthToSolver($v$)** : Converts depth quantity to solver value, defaults to length conversion.

---

## 3. Variables

### 1. Decision Variables

**$x_{ij}$** : Number of times layer $j$ is placed in bin $i$, non-negative integer variable, domain is $[0, \lfloor d(B_i) / d(L_j) \rfloor]$, representing the stacking count of layer $j$ in bin $i$, $\forall i \in Bins$, $\forall j \in Layers$. Non-zero range only when $type(B_i) = bin(L_j)$ and layer $j$ is enabled in bin $i$.

> The precise phase (MILP) uses a 2D variable matrix $x$.

**$x_{j}^{(t)}$** : Usage count of layer $j$ in iteration $t$, non-negative integer variable, domain is $[0, \max_{k}(\lceil d_k / amount(L_j, k) \rceil)]$, representing how many times this layer is selected in column generation round $t$, $\forall j \in Layers^{(t)}$.

> The imprecise phase (RMP) uses a 1D variable vector, with a new group added per iteration.

### 2. Auxiliary Variables

**$u_{ij}$** : Binary intermediate symbol indicating whether layer $j$ is used in bin $i$, domain is $\{0, 1\}$, $u_{ij} = \mathbb{1}(x_{ij} > 0)$, $\forall i \in Bins$, $\forall j \in Layers$.

**$v_{i}$** : Binary intermediate symbol indicating whether bin $i$ is used, domain is $\{0, 1\}$, $v_{i} = \mathbb{1}(\sum_{j} x_{ij} > 0)$, $\forall i \in Bins$.

**$tail_{i}$** : Tail bin marker variable indicating whether bin $i$ is the tail bin (the last bin used), domain is $\{0, 1\}$, $\forall i \in Bins$.

---

## 4. Predicates

### 1. Layer-Bin Matching Predicates

**matchType(L, B)** : Layer $L$'s bin type matches bin $B$'s bin type, i.e., $bin(L) = type(B)$.
**enabled(L, B)** : Layer $L$ is enabled in bin $B$, i.e., matchType(L, B) and the layer's geometry is feasible within the bin.

### 2. Demand Mode Predicates

**isItem(m)** : Demand mode $m$ is of item type (Item / ItemAmount / ItemWeight).
**isMaterial(m)** : Demand mode $m$ is of material type (Material / ItemMaterialAmount / ItemMaterialWeight).
**isDiscrete(u)** : The quantity unit $u$ of a demand entry is in the discrete domain (e.g., count).
**isContinuous(u)** : The quantity unit $u$ of a demand entry is in the continuous domain (e.g., kg).

### 3. Bin Status Predicates

**isUsed(B)** : Bin $B$ is used, i.e., $v_{i} = 1$.
**isTail(B)** : Bin $B$ is the tail bin, i.e., $tail_{i} = 1$.
**isSameType(B_{i}, B_{i-1})$** : Adjacent bins $B_i$ and $B_{i-1}$ are of the same bin type.

---

## 5. Sets

### 1. Bins

**$B$** : Universal set of all bins.

**$B^{used}$** : Subset satisfying predicate isUsed, i.e., the set of bins that are used.
**$B^{tail}$** : Subset satisfying predicate isTail, i.e., the set of tail bins.
**$B_{T}$** : Subset of bins with bin type $T$.

### 2. Layers

**$L$** : Universal set of all layers.

**$L^{(t)}$** : Set of layers added in iteration $t$.
**$L^{rem}$** : Set of removed layers (removed via removeColumn).
**$L_{B}$** : Subset of layers usable in bin $B$, i.e., layers satisfying enabled(L, B).
**$L_{T}$** : Subset of layers with bin type $T$, i.e., layers with $bin(L) = T$.

### 3. Demand Entries

**$K$** : Universal set of all demand entries.

**$K^{item}$** : Subset satisfying predicate isItem, item-type demands.
**$K^{mat}$** : Subset satisfying predicate isMaterial, material-type demands.
**$K^{discrete}$** : Subset satisfying predicate isDiscrete, discrete domain demands.
**$K^{continuous}$** : Subset satisfying predicate isContinuous, continuous domain demands.

### 4. Bin-Layer Pairs

**$BL$** : Set of feasible bin-layer pairs, $\{(i, j) \mid matchType(L_j, B_i) \wedge enabled(L_j, B_i)\}$.

---

## 6. Intermediate Values

### 1. Load

**Description**: For each demand entry, the sum of contributions from all layers forms the total load. In the imprecise phase, aggregation is per layer; in the precise phase, it uses the bin-layer matrix.

Imprecise phase:

$$
load_{k} = \sum_{j \in L} c(L_j, K_k) \cdot x_j, \; \forall k \in K
$$

Precise phase:

$$
load_{k} = \sum_{i \in B} \sum_{j \in L} c(L_j, K_k) \cdot x_{ij}, \; \forall k \in K
$$

where $c(L_j, K_k)$ is the load coefficient of layer $L_j$ for demand entry $K_k$, determined by the layer's statistics and demand mode.

### 2. OverLoad

**Description**: The amount by which the load exceeds the upper bound of the demand range, used for soft constraint relaxation.

$$
overLoad_{k} = load_{k} - demandRange_{k}^{ub}, \; \forall k \in K
$$

### 3. LessLoad

**Description**: The amount by which the load falls below the lower bound of the demand range, used for soft constraint relaxation.

$$
lessLoad_{k} = demandRange_{k}^{lb} - load_{k}, \; \forall k \in K
$$

### 4. LoadWeight

**Description**: Total weight of all layers in each bin.

$$
loadWeight_{i} = \sum_{j \in L} weight(L_j) \cdot x_{ij}, \; \forall i \in B
$$

### 5. LoadVolume

**Description**: Total volume of all layers in each bin.

$$
loadVolume_{i} = \sum_{j \in L} volume(L_j) \cdot x_{ij}, \; \forall i \in B
$$

### 6. LoadDepth

**Description**: Total depth of all layers in each bin.

$$
loadDepth_{i} = \sum_{j \in L} depth(L_j) \cdot x_{ij}, \; \forall i \in B
$$

### 7. LoadingRate

**Description**: Ratio of loaded volume to bin volume for each bin.

$$
loadingRate_{i} = \frac{loadVolume_{i}}{volume(B_i)}, \; \forall i \in B
$$

### 8. TailLoadingRate

**Description**: Loading rate of the tail bin, activated only for the tail bin via a masking function.

$$
tailLoadingRate_{i} = loadingRate_{i} \cdot tail_{i}, \; \forall i \in B
$$

### 9. Volume

**Description**: Total volume of all layers in the imprecise phase, used as the objective function for the column generation RMP.

$$
volume = \sum_{j \in L} volume(L_j) \cdot x_j
$$

---

## 7. Assertions

### 1. Layer-Bin Type Consistency

**Description**: A layer can only be placed in a bin whose bin type matches the layer's bin type. If the bin type does not match, the placement count must be zero.

$$
\forall i \in B, \forall j \in L \; (type(B_i) \neq bin(L_j) \rightarrow x_{ij} = 0)
$$

### 2. Layer Depth Upper Bound

**Description**: The total depth of all layers in a bin must not exceed the bin's depth.

$$
\forall i \in B \; (loadDepth_{i} \leq d(B_i))
$$

### 3. Bin Capacity Upper Bound

**Description**: The total weight of all layers in a bin must not exceed the bin's load capacity.

$$
\forall i \in B \; (loadWeight_{i} \leq cap(B_i))
$$

---

## 8. Constraints

### 1. Demand Upper Bound Constraint

**[CN]**: 需求上界约束
**Description**: The load for each demand entry must not exceed the upper bound of its demand range. When overload relaxation is enabled, the overload symbol is used.

$$
s.t. \quad load_{k} \leq demandRange_{k}^{ub}, \; \forall k \in K
$$

### 2. Demand Lower Bound Constraint

**[CN]**: 需求下界约束
**Description**: The load for each demand entry must not fall below the lower bound of its demand range. When less-load relaxation is enabled, the less-load symbol is used.

$$
s.t. \quad load_{k} \geq demandRange_{k}^{lb}, \; \forall k \in K
$$

### 3. Bin Weight Capacity Constraint

**[CN]**: 箱子重量容量约束
**Description**: The total load weight of layers in each bin must not exceed the bin's load capacity.

$$
s.t. \quad loadWeight_{i} \leq cap(B_i), \; \forall i \in B
$$

### 4. Bin Depth Constraint

**[CN]**: 箱子深度约束
**Description**: The total load depth of layers in each bin must not exceed the bin's depth.

$$
s.t. \quad loadDepth_{i} \leq d(B_i), \; \forall i \in B
$$

### 5. Bin Loading Order Constraint

**[CN]**: 箱子装载顺序约束
**Description**: For adjacent bins of the same type, the preceding bin must be used first ($v_{i-1} \geq v_i$), and the preceding bin's loaded volume must be at least that of the following bin ($loadVolume_{i-1} \geq loadVolume_i$). This constraint eliminates symmetry among same-type bins.

$$
s.t. \quad v_{i-1} \geq v_i, \; \forall i \in B \setminus \{0\} \mid type(B_i) = type(B_{i-1})
$$

$$
s.t. \quad loadVolume_{i-1} \geq loadVolume_i, \; \forall i \in B \setminus \{0\} \mid type(B_i) = type(B_{i-1})
$$

### 6. Tail Bin Assignment Constraint

**[CN]**: 尾箱分配约束
**Description**: The tail bin marker must be consistent with the bin usage status. For the last bin, $tail_i \leq v_i$; for other bins, $tail_i \leq v_{i+1}$. This ensures only the last used bin can be marked as the tail bin.

$$
s.t. \quad tail_i \leq v_i, \; i = |B| - 1
$$

$$
s.t. \quad tail_i \leq v_{i+1}, \; \forall i \in B \setminus \{|B| - 1\}
$$

---

## 9. Objective Function

### 1. Volume Minimization (Imprecise Phase)

**Description**: In the column generation RMP phase, minimize the total volume usage of all layers, guiding the solver toward more compact layer combinations.

$$
\min \sum_{j \in L} volume(L_j) \cdot x_j
$$

### 2. Bin Amount Minimization (Precise Phase)

**Description**: In the MILP phase, minimize the total number of bins used.

$$
\min \sum_{i \in B} c(B_i) \cdot v_i
$$

where $c(B_i)$ is the coefficient function for bin $B_i$, typically 1.

### 3. Better Layer Maximization (Precise Phase)

**Description**: In the MILP phase, maximize the layer-bin matching quality coefficient, preferring layer combinations with higher loading rates and better support.

$$
\max \sum_{i \in B} \sum_{j \in L} coeff(L_j, B_i) \cdot x_{ij}
$$

where $coeff(L_j, B_i)$ is the matching coefficient function for layer $L_j$ and bin $B_i$.

### 4. Tail Bin Loading Rate Minimization (Precise Phase)

**Description**: In the MILP phase, minimize the tail bin's loading rate to avoid excessive unused space in the tail bin.

$$
\min \sum_{i \in B} c(B_i) \cdot tailLoadingRate_{i}
$$

---

## 10. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Column Generation | Built-in framework | Sections 3, 6, 9 | RMP solving and column iteration mechanism for the imprecise phase |
| Binaryzation Function | Built-in framework | Section 3 (auxiliary variables) | Converts integer variables to binary indicator variables |
| Masking Function | Built-in framework | Section 6 (tail loading rate) | Controls intermediate symbol activation scope via boolean variable masking |

---

## 11. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 箱型 | $B$ / BinType | Bin Type | Bin type definition with dimensions and capacity |
| 箱子 | $B_i$ / Bin | Bin | Bin instance composed of a bin type and layers |
| 箱层 | $L_j$ / BinLayer | Bin Layer | An arrangement of items forming a single layer; the fundamental column unit in column generation |
| 货物 | $k$ / Item | Item | Atomic cargo unit to be packed |
| 需求条目 | $K_k$ / Bpp3dDemandEntry | Demand Entry | A single loading demand with mode, key, value, and range |
| 需求模式 | $m$ / Bpp3dDemandMode | Demand Mode | Distinguishes item/material and discrete/continuous domain |
| 需求键 | $key$ / Bpp3dDemandKey | Demand Key | Identifies the item or material targeted by the demand |
| 赋值 | $x$ / Assignment | Assignment | Decision variable: number of times a layer is placed in a bin |
| 负载 | $load$ / Load | Load | Total loading quantity for a demand entry |
| 容量 | $cap$ / Capacity | Capacity | Bin's load weight, volume, and depth metrics |
| 层聚合 | $LA$ / LayerAggregation | Layer Aggregation | Manages layer sets and deduplication during column generation |
| 影子价格 | $sp$ / Shadow Price | Shadow Price | Dual variable reflecting the marginal value of a constraint |
| 装载率 | $lr$ / Loading Rate | Loading Rate | Ratio of loaded volume to bin volume |
| 尾箱 | $tail$ / Tail Bin | Tail Bin | The last bin that is used |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Two-phase solving | Single-phase MILP / Column generation + MILP | Column generation handles large candidate layer spaces; MILP guarantees exact solutions | 2024 |
| Imprecise/Precise separation | Unified assignment model | Different variable structures (1D vector vs 2D matrix) per phase; separation is cleaner | 2024 |
| Solver value adapter | Direct use of physical quantities / Adapter pattern | Adapter supports unit conversion and scaling, decoupling business domain from solver | 2024 |
| Unified demand entry | Separate item/material models | Unified Bpp3dDemandEntry supports multiple demand modes, reducing code duplication | 2024 |
| Layer deduplication strategy | Based on units content comparison | Structural equality of item placement lists avoids generating semantically identical layers | 2024 |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial layer assignment model | Basic column generation framework |
| v1.1 | Added precise phase assignment model | Support for MILP exact solving |
| v1.2 | Unified demand entries as Bpp3dDemandEntry | Support for multiple item/material demand modes |
| v1.3 | Added solver value adapter with scaling support | Support for unit conversion and numeric scaling |
| v1.4 | Added tail bin marker and loading rate optimization | Improved tail bin space utilization |
