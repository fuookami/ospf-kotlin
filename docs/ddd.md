# DDD 运筹组件模式 vs 传统运筹开发模式：结构对比说明

> 本文档基于 `ospf-kotlin-example` 中的 `framework_demo/demo1`、`framework_demo/demo2`、`framework_demo/demo4`（DDD 框架模式）、`core_demo`（OSPF core 脚本式模式）、Gurobi Java examples、COPT Java examples（solver SDK 原生模式），以及 `ospf-kotlin-framework-*` 框架模块的代码对比分析。

---

## 1. 总体架构概览

### 1.1 传统模式与 core 脚本式：线性建模流程

solver SDK 原生模式以 solver API 为中心，模型构建、求解、解分析在一个 `main()` 方法内线性完成。

```
main()
  ├── 创建 solver 环境
  ├── 声明变量（model.addVar）
  ├── 构造目标（model.setObjective）
  ├── 添加约束（model.addConstr）
  ├── 求解（model.optimize）
  └── 读取结果（var.get(DoubleAttr.X)）
```

代表：Gurobi `Mip1.java`、`Facility.java`，COPT `Lp_ex1.java`。`ospf core_demo/Demo1.kt` 也是单文件脚本式示例，但已经使用 OSPF core 的 `MetaModel`、变量和表达式符号抽象，不等同于 solver SDK 原生 API。

### 1.2 DDD 框架模式：分层领域驱动

DDD 框架模式以业务领域为中心，按领域边界拆分子域，每个子域自包含变量、中间值、约束、解分析逻辑，通过 Application 层编排组合。

```
Application Layer
  ├── init()      → 按依赖顺序初始化各子域 Context
  ├── register()  → 各子域将变量和中间值注册到 MetaModel
  ├── construct() → 各子域通过 Pipeline 注入约束
  ├── solve()     → 基础设施层统一求解
  └── analyze()   → 各子域独立提取领域解

Domain Layer（每个子域）
  ├── Context       — 子域入口，管理生命周期
  ├── Aggregation   — 聚合根，组合子域内的领域模型
  ├── Model         — 领域模型，持有变量和中间值
  └── Service       — 领域服务，生成约束 Pipeline 和解分析器

Infrastructure Layer
  ├── Solver 构建器（运行时切换 Gurobi/SCIP）
  ├── DTO（请求/响应数据传输对象）
  ├── Benders 策略、诊断、降级逻辑
```

代表：`framework_demo/demo1`（SSP 最短服务路径），`framework_demo/demo2`（飞机装载优化，11 个子域）。

---

## 2. 核心差异：变量与中间值的独立生命周期

这是两种模式最根本的架构分歧。

### 2.1 solver SDK 原生模式与 core 脚本式用法

```java
// Gurobi Mip1.java
GRBModel model = new GRBModel(env);
GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
// x 由 model 创建，x 的生命周期绑定在 model 上
// x 离开 model 没有意义，无法被其他 model 引用
```

```kotlin
// ospf core_demo/Demo1.kt
private val metaModel = LinearMetaModel<Flt64>("demo1", converter = flt64Converter)

private suspend fun initVariable(): Try {
    x = BinVariable1("x", Shape1(companies.size))
    metaModel.add(x)  // 变量直接绑定到单一 metaModel
    return ok
}
```

在 solver SDK 原生模式中，变量是 `model.addVar()` 的产物，生命周期绑定在具体 model 上。solver SDK 本身不提供 DDD 框架中的领域中间值生命周期，表达式通常以内联方式参与约束或目标；如果需要命名、复用或跨模块共享，需要开发者自行封装。

`ospf core_demo/Demo1.kt` 相比 solver SDK 原生模式已经更进一步：变量由 `BinVariable1(...)` 创建后注册到 `LinearMetaModel`，并且可使用 `LinearExpressionSymbol` 等具名符号。它仍属于脚本式 core 用法，是因为变量、表达式、约束、求解和解分析集中在单一示例对象和单一 `metaModel` 流程中，缺少 DDD 框架模式中的 Context / Aggregation / Pipeline 分层边界。

### 2.2 DDD 框架模式：变量和中间值是领域对象的属性

```kotlin
// framework_demo/demo1/route_context/model/Assignment.kt
class Assignment(
    private val nodes: List<Node>,
    private val services: List<Service>
) {
    lateinit var x: BinVariable2                                    // 决策变量
    lateinit var nodeAssignment: LinearIntermediateSymbols1<Flt64>  // 算术中间值
    lateinit var serviceAssignment: LinearIntermediateSymbols1<Flt64> // 算术中间值

    fun register(model: LinearMetaModel<Flt64>): Try {
        if (!::x.isInitialized) {
            // 变量的创建、命名、范围约束 —— 全部在领域层完成
            x = BinVariable2("x", Shape2(nodes.size, services.size))
            for (service in services) {
                for (node in nodes.filter(normal)) {
                    x[node, service].name = "${x.name}_${node}_$service"
                }
                for (node in nodes.filter(client)) {
                    val variable = x[node, service]
                    variable.name = "${x.name}_${node}_$service"
                    variable.range.eq(false)
                }
            }
        }
        model.add(x)  // 仅仅是"注册"——将已有变量挂载到 model

        if (!::nodeAssignment.isInitialized) {
            nodeAssignment = flatMap("node_assignment", nodes, { n ->
                if (n is NormalNode) sum(x[n, _a]) else LinearPolynomial()
            }, { (_, n) -> "$n" })
        }
        model.add(nodeAssignment)  // 中间值同样只是"注册"

        if (!::serviceAssignment.isInitialized) {
            serviceAssignment = flatMap("service_assignment", services, { s ->
                sumVars(nodes.filter(normal)) { n -> x[n, s] }
            }, { (_, s) -> "$s" })
        }
        model.add(serviceAssignment)

        return ok
    }
}
```

**变量和中间值的生命周期**：

1. **诞生**：在领域 Model 的 `register()` 中首次初始化（`if (!::x.isInitialized)` 守卫）
2. **独立存在**：变量和中间值作为领域对象的 `lateinit var` 属性持续存活，不依赖任何 model
3. **按需注册**：`model.add(x)` / `model.add(nodeAssignment)` 只是将已有的变量/中间值"挂载"到某个 model 实例
4. **可重复注册**：同一个变量/中间值可以注册到不同 model（如 Benders 主问题/子问题）

### 2.3 对比

| | solver SDK 原生模式 / core 脚本式用法 | DDD 框架模式 |
|---|---------|-------------|
| **变量创建者** | solver SDK 原生模式由 `model.addVar()` 创建；OSPF core 脚本式示例可先创建变量再注册到 `metaModel` | `BinVariable2(...)` — 领域对象创建变量 |
| **变量所有者** | solver SDK 原生模式由 model 持有；core 脚本式用法仍围绕单一示例流程组织 | 领域 Model 拥有变量 |
| **变量生命周期** | solver SDK 原生变量与 model 绑定；core 脚本式示例缺少跨子域生命周期管理 | 独立于 model，领域对象存活则变量存活 |
| **中间值** | solver SDK 原生 API 不提供 DDD 中间值抽象；OSPF core 可使用具名表达式符号 | 算术中间值 + 函数中间值，命名、可复用 |
| **变量跨 model 复用** | solver SDK 原生变量不能直接跨 model；如自行封装需维护映射一致性 | 通过领域对象和注册路径复用 |
| **model 的角色** | solver SDK 原生 model 是变量工厂；OSPF core `metaModel` 是脚本式注册和编译轴心 | 接收各子域注册的变量和中间值 |

---

## 3. 子域组合：中间值作为组合的桥梁

DDD 模式中，子域之间的组合依赖于 **中间值**（Intermediate Value）机制。中间值是 ospf 框架的核心概念，它使得一个子域定义的运算结果可以被另一个子域像变量一样使用，而无需了解其内部实现。

### 3.1 中间值的定义与特性

中间值在数学模型中用于表示运算的中间结果，它具有以下特性：

- **指代一个被存储起来的具名的表达式**：`nodeAssignment` 是 `sum(x[n, _a])` 这个多项式的具名引用
- **语义上等价于匿名的表达式**：使用 `nodeAssignment[node]` 与直接写 `sum(x[n, _a])` 效果完全相同
- **文法上等价于变量**：拥有全局作用域以及静态生命周期，可以在约束、目标、其他中间值中自由使用

### 3.2 两类中间值

#### 算术中间值（Arithmetic Intermediate Value）

将多项式封装为命名符号，ospf 在翻译到求解器接口时自动将其替换为具体的多项式。

```kotlin
// Assignment 中定义的算术中间值
serviceAssignment = flatMap("service_assignment", services, { s ->
    sumVars(nodes.filter(normal)) { n -> x[n, s] }  // 对每个 service，求所有 normal node 的 x 之和
}, { (_, s) -> "$s" })

// 使用时，serviceAssignment[service] 语义上等价于 sumVars(nodes.filter(normal)) { n -> x[n, s] }
// 但使用者无需知道这个中间值是由哪些变量通过什么运算构成的
```

数学表示：

$$
serviceAssignment_{s} = \sum_{n \in NormalNodes} x_{n,s}
$$

$$
min \quad \sum_{s} cost_{s} \cdot serviceAssignment_{s}
$$

$$
s.t. \quad serviceAssignment_{s} \leq 1, \quad \forall s
$$

ospf 在求解翻译时自动将 `serviceAssignment_{s}` 展开为 $\sum_{n} x_{n,s}$，使用者无需感知。

#### 函数中间值（Function Intermediate Value）

将逻辑运算等非算术表达式封装到中间值中，ospf 在翻译时自动添加所需的中间变量和约束。

$$
FuncSymbol = \bigvee_{i} x_{i} = Or(x_{1}, x_{2}, .., x_{i})
$$

$$
s.t. \quad FuncSymbol = 1
$$

概念上可以把它理解为引入辅助变量和约束，例如下面这个简化示意：

$$
s.t. \quad y = 1
$$

$$
\begin{cases}
  y \geq \frac{x_{i}}{\sup_{\leq}(x_{i})}, & \sup_{\leq}(x_{i}) > 1 \\
  y \geq x_{i}, & else
\end{cases}
$$

$$
y \leq \sum_{i} x_{i}
$$

$$
y \in \{ 0, 1 \}
$$

实际实现不必等同于上述公式。以当前 `OrFunction` 为例，框架会根据函数类型引入 `resultVar`、`indicatorVars`、`sideVars` 以及 indicator 约束等更细的内部结构。使用者只需要知道 `FuncSymbol` 表示"或"运算，不需要知道它内部引入了哪些辅助变量和约束。

### 3.3 中间值实现的角色分离

中间值机制将数学模型的维护者划分为两个角色：

- **中间值维护者**：定义并实现中间值（领域 Model 层），负责将业务概念封装为变量和中间符号
- **模型构建者**：使用中间值维护数学模型（Service 层），只关注中间值的定义与行为，使用它们描述业务逻辑

这个工程实践与 OOD 中"定义一个类把相同语义的变量、函数封装起来，使用者只需关注其行为，无需关注其实现"是一致的。

### 3.4 demo1 的跨域组合链

```
route_context                           bandwidth_context
┌─────────────────┐                    ┌─────────────────────┐
│ Assignment      │◄── 被引用 ────────│ EdgeBandwidth       │
│   .x (BinVar2)  │                    │   .y (UIntVariable2)│
│   .serviceAs- ──┼──── 中间值 ──────▶│   .bandwidth        │
│   signment      │                    │                     │
│                 │                    │ ServiceBandwidth    │
│ Graph           │◄── 被引用 ────────│   .inDegree         │
│   .nodes        │                    │   .outDegree        │
│   .edges        │                    │   .outFlow          │
│                 │                    │                     │
│ Service         │◄── 被引用 ────────│ NodeBandwidth       │
└─────────────────┘                    └─────────────────────┘
```

`bandwidth_context` 的约束中直接使用 `route_context` 定义的中间值 `assignment.serviceAssignment`，无需了解它内部是由哪些变量如何运算构成的：

```kotlin
// EdgeBandwidthConstraint — bandwidth 域的约束，引用了 route 域的中间值
class EdgeBandwidthConstraint(
    private val edges: List<Edge>,
    private val services: List<Service>,
    private val assignment: Assignment,        // route 域的领域对象
    private val edgeBandwidth: EdgeBandwidth,  // bandwidth 域的领域对象
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        val y = edgeBandwidth.y                        // bandwidth 域的变量
        val assignment = assignment.serviceAssignment  // route 域的中间值
        for (edge in edges) {
            for (service in services) {
                model.addConstraint(
                    // 中间值 assignment[service] 与变量 y[edge, service] 自由组合
                    (UInt64.one - assignment[service]) * edge.maxBandwidth.toFlt64()
                        + LinearPolynomial(y[edge, service])
                        leq edge.maxBandwidth.toFlt64(),
                )
            }
        }
        return ok
    }
}
```

**关键**：`assignment.serviceAssignment` 是 route 域维护的中间值，`edgeBandwidth.y` 是 bandwidth 域维护的变量。bandwidth 域的约束构造者只需要知道 `serviceAssignment[service]` 的语义（"该 service 在所有 normal node 上的分配之和"），不需要知道它内部的实现细节。这就是中间值实现的"关注点分离"。

### 3.5 传统模式缺少内建的子域组合抽象

```java
// Gurobi: 两个 model 的变量无法交叉使用
GRBModel modelA = new GRBModel(env);
GRBVar xA = modelA.addVar(...);  // 属于 modelA

GRBModel modelB = new GRBModel(env);
GRBVar yB = modelB.addVar(...);  // 属于 modelB

// xA 和 yB 无法出现在同一个约束表达式中
// 没有中间值概念 —— 所有表达式必须内联，无法被命名和跨模块共享
// 要组合两个问题的变量和表达式，只能把所有逻辑写在一个 main() 里
```

solver SDK 原生模式中，变量是 model 的派生物，表达式通常是一次性构造值。要组合不同"子问题"的变量和表达式，需要自行建立领域对象、表达式封装和变量映射；原生 API 本身不提供 DDD 框架这种 Context / Aggregation / Pipeline 级别的模块边界。

---

## 4. Pipeline：约束作为可组合的一等公民

### 4.1 传统模式：约束是不可管理的副作用

```java
// Gurobi Facility.java
GRBLinExpr ptot = new GRBLinExpr();
for (int w = 0; w < nWarehouses; ++w) {
    ptot.addTerm(1.0, transport[w][p]);
}
GRBLinExpr limit = new GRBLinExpr();
limit.addTerm(Capacity[p], open[p]);
model.addConstr(ptot, GRB.LESS_EQUAL, limit, "Capacity" + p);
```

约束通过 `addConstr()` 调用直接注入 model，是一个不可逆的副作用。约束注入后无法被排序、过滤、移除或重新组合。所有约束在同一个函数中按代码顺序一次性注入，无法被独立管理。

### 4.2 DDD 框架模式：约束是 Pipeline 对象

ospf 框架将每个约束（或目标）封装为一个实现了 `Pipeline<LinearMetaModel<Flt64>>` 接口的独立类：

```kotlin
// 每个约束是一个独立的、可命名的、可组合的对象
class NodeAssignmentConstraint(
    private val nodes: List<Node>,
    private val assignment: Assignment,
    override val name: String = "node_assignment"  // 约束有独立的名称
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        for (node in nodes.filter(normal)) {
            model.addConstraint(
                assignment.nodeAssignment[node] leq 1,
                name = "${name}_$node"
            )
        }
        return ok
    }
}
```

Pipeline 的生命周期与 model 完全解耦：

1. **创建**：在 `PipelineListGenerator` 中实例化，此时约束尚未注入任何 model
2. **组装**：多个 Pipeline 被组装为 `PipelineList`（一个有序列表）
3. **延迟执行**：只有当 `invoke(model)` 被调用时，约束才被注入到具体的 model
4. **可重复执行**：同一个 Pipeline 可以对不同的 model 执行（如 Benders 主问题/子问题）

### 4.3 PipelineListGenerator：按子域组装约束管线

每个子域的 Service 层包含一个 `PipelineListGenerator`，负责将该子域的所有约束和目标组装为有序的 Pipeline 列表：

```kotlin
// route_context 的 PipelineListGenerator
class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(): Ret<PipelineList<LinearMetaModel<Flt64>>> {
        val list = ArrayList<Pipeline<LinearMetaModel<Flt64>>>()
        list.add(NodeAssignmentConstraint(aggregation.graph.nodes, aggregation.assignment))
        list.add(ServiceAssignmentConstraint(aggregation.services, aggregation.assignment))
        list.add(ServiceCostObjective(aggregation.services, aggregation.assignment))
        return Ok(list)
    }
}

// bandwidth_context 的 PipelineListGenerator — 引入了 route 域的中间值
class PipelineListGenerator(
    private val aggregation: Aggregation,
    private val graph: Graph,              // 来自 route 域
    private val services: List<Service>,   // 来自 route 域
    private val assignment: Assignment,    // 来自 route 域（持有中间值）
) {
    operator fun invoke(): Ret<PipelineList<LinearMetaModel<Flt64>>> {
        val list = ArrayList<Pipeline<LinearMetaModel<Flt64>>>()
        list.add(EdgeBandwidthConstraint(graph.edges, services, assignment, aggregation.edgeBandwidth))
        list.add(DemandConstraint(graph.nodes, aggregation.nodeBandwidth))
        list.add(ServiceCapacityConstraint(graph.nodes, services, assignment, aggregation.serviceBandwidth))
        list.add(BandwidthCostObjective(graph.edges, aggregation.edgeBandwidth))
        return Ok(list)
    }
}
```

### 4.4 Application 层的编排

```kotlin
// framework_demo/demo1/Application.kt — SSP 类
private fun construct(model: LinearMetaModel<Flt64>): Try {
    // 第一步：各子域将变量和中间值注册到 model
    // 顺序无关，因为变量和中间值已存在于领域对象中
    routeContext.register(model)
    bandwidthContext.register(model)

    // 第二步：各子域通过 PipelineListGenerator 生成约束管线并执行
    // 顺序决定约束在 model 中的排列
    routeContext.construct(model)
    bandwidthContext.construct(model)
    return ok
}
```

### 4.5 Pipeline 的优势

| | 传统模式 | Pipeline 模式 |
|---|---------|-------------|
| **约束的本质** | 副作用（addConstr 调用） | 一等公民（Pipeline 对象） |
| **可命名** | 仅字符串标签 | 独立的 `name` 属性 |
| **可组合** | 原生 API 不提供 Pipeline 级组合，需要自行封装 | `PipelineList` 有序列表 |
| **可独立测试** | 通常通过整段建模/求解验证 | 每个 Pipeline 可单独实例化并验证 |
| **延迟执行** | 不支持 | Pipeline 创建后不立即注入 model |
| **可重复执行** | 不支持 | 同一 Pipeline 可对不同 model 执行 |
| **可跨域引用** | 不适用 | 构造函数接收任意子域的领域对象（含中间值） |
| **可排序/过滤** | 不支持 | `PipelineList` 是普通列表，支持所有集合操作 |

---

## 5. 求解器抽象

### 5.1 传统模式：直接绑定

```java
// Gurobi
import gurobi.*;
GRBModel model = new GRBModel(env);
model.optimize();

// COPT
import copt.*;
Model model = env.createModel("lp_ex1");
model.solve();
```

更换求解器 = 更换 import + 重写所有 API 调用。

### 5.2 DDD 框架模式：运行时抽象

```kotlin
// infrastructure/Solver.kt — LinearSolverBuilder
data object LinearSolverBuilder {
    operator fun invoke(
        solver: String? = null,       // "gurobi" / "scip" / null(自动选择)
        config: SolverConfig = SolverConfig(),
        // ...
    ): AbstractLinearSolver {
        // 根据参数或环境自动选择求解器
        // 支持 SerialCombinatorialLinearSolver — 串行降级
    }
}
```

在普通 MILP / CG 路径中，Application 可以通过基础设施层的构建器选择 solver，领域层不感知具体 solver：

```kotlin
// 普通路径可将求解器选择收敛到基础设施构建器
val solver = LinearSolverBuilder(config = SolverConfig(time = 1.minutes))
val result = solveLinearMetaModel(solver, metaModel)
```

当前示例仍有部分教学或算法路径直接选择具体 solver，例如 demo1 的 Application 直接使用 `ScipLinearSolver`，demo2 的 Benders 路径直接实例化 `GurobiLinearBendersDecompositionSolver`。因此更准确的边界是：领域层应保持 solver 无关；Application 层可以选择 solver，但生产框架应把具体 solver 绑定收敛到 Infrastructure 边界或少量编排入口。

### 5.3 对比

| | 传统模式 | DDD 框架模式 |
|---|---------|-------------|
| **切换求解器** | 更换 import + 重写 API | `LinearSolverBuilder(solver="gurobi")` |
| **组合求解器** | 不支持 | `SerialCombinatorialLinearSolver([Gurobi, SCIP])` 自动降级 |
| **求解器感知范围** | 全部代码 | 理想边界为 Infrastructure；当前示例中部分 Application / Benders 路径仍直接选择具体 solver |

---

## 6. 解分析

### 6.1 传统模式：手动遍历原始数值

```java
// Gurobi Facility.java
for (int p = 0; p < nPlants; ++p) {
    if (open[p].get(GRB.DoubleAttr.X) > 0.99) {
        System.out.println("Plant " + p + " open:");
        // ...
    }
}
```

```kotlin
// core_demo/Demo1.kt
private suspend fun analyzeSolution(): Try {
    for (token in metaModel.tokens.tokens) {
        if (token.result!! eq Flt64.one) {
            ret.add(companies[token.variable.index])
        }
    }
    return ok
}
```

解分析与模型构建耦合在同一上下文中，无法独立复用。

### 6.2 DDD 框架模式：领域化解分析服务

```kotlin
// 每个子域拥有独立的 SolutionAnalyzer
class BandwidthContext(...) {
    fun analyze(model: LinearMetaModel<Flt64>, result: List<Flt64>): Ret<List<List<Node>>> {
        val analyzer = SolutionAnalyzer(
            routeAggregation.graph,
            routeAggregation.services,
            routeAggregation.assignment,
            aggregation
        )
        return analyzer(model, result)  // 返回领域对象，不是原始数值
    }
}
```

demo2 中更复杂——stowageContext 的 `analyze()` 返回 `Solution` 领域对象，包含装载方案、渲染信息、诊断日志：

```kotlin
val output = stowageContext.analyze(solution = solution, input = request)
// 返回 Ret<Solution>，其中 Solution 是包含完整领域语义的对象
```

---

## 7. Benders 分解：独立生命周期与中间值的终极体现

### 7.1 为什么 Benders 分解需要独立生命周期

Benders 分解将一个大规模 MILP 问题拆分为主问题（Master Problem）和子问题（Sub Problem），两者共享部分变量但包含不同的约束。这要求：

- **主问题变量到子问题固定值的映射**：例如 stowage 域变量需要通过 `fixedVariables` 衔接主问题解和子问题约束
- **不同子域按角色选择性注册**：某些子域只注册到主问题，某些只注册到子问题
- **共享领域对象保持一致语义**：主问题和子问题可以复用同一领域对象，但并不要求所有变量和中间值都完整注册到两个 model

如果变量和表达式完全归具体 solver model 所有，这种"共享领域对象 + 选择性注册 + 固定变量映射"的模式需要手动维护多份变量定义和映射关系，复杂度会迅速上升。

### 7.2 demo2 的 Benders 分解架构

demo2（飞机装载优化）展示了 Benders 分解在 DDD 框架模式中的完整实现：

```
┌─────────────────────────────────────────────────────────────┐
│  Application Layer (FullLoadApplication.kt)                 │
│                                                             │
│  buildBendersModels()                                       │
│  ├── masterModel = LinearMetaModel("master")                │
│  ├── subModel = LinearMetaModel("sub")                      │
│  │                                                          │
│  │  // 主问题：装载 + MAC + 软安全 + MAC优化 + 快递效率     │
│  │  stowageContext.registerForBendersMP(masterModel)         │
│  │  macContext.registerForBendersMP(masterModel)             │
│  │  softSecurityContext.registerForBendersMP(masterModel)    │
│  │  macOptimizationContext.registerForBendersMP(masterModel) │
│  │  expressEffectivenessContext.registerForBendersMP(...)    │
│  │  loadingEffectivenessContext.registerForBendersMP(...)    │
│  │                                                          │
│  │  // 子问题：装载（共享变量）+ 适航安全约束               │
│  │  stowageContext.registerForBendersSP(subModel, ...)       │
│  │  airworthinessSecurityContext.registerForBendersSP(...)   │
│  │                                                          │
│  │  // 共享领域对象与固定变量映射（stowage.x 由 fixedVariables 衔接）│
│  │  fixedVariables = stowage.x[i,j] → Flt64.zero            │
│  │                                                          │
│  solveWithBendersAlgorithm()                                │
│  ├── BendersSolver.solve(solver, masterModel, subModel, ...)│
│  ├── 质量守卫检查（gap、time、progress、cut efficiency）    │
│  ├── 降级到 MILP（如果质量不达标且策略允许）               │
│  └── stowageContext.analyze(solution, model) → 领域解       │
└─────────────────────────────────────────────────────────────┘
```

### 7.3 子域选择性注册的实现

每个 Context 提供两组注册方法，分别对应 Benders 主问题和子问题：

```kotlin
// MacContext
class MacContext {
    // 注册到主问题：完整的 MAC 计算约束
    fun registerForBendersMP(model: AbstractLinearMetaModel<Flt64>): Try {
        return aggregation.register(stowageMode = StowageMode.FullLoad, model = model)
    }

    // 注册到子问题：MAC 域在子问题中不需要额外约束
    fun registerForBendersSP(model: AbstractLinearMetaModel<Flt64>): Try {
        return ok  // 空实现
    }
}

// AirworthinessSecurityContext
class AirworthinessSecurityContext {
    // 注册到主问题：不需要
    fun registerForBendersMP(model: AbstractLinearMetaModel<Flt64>): Try {
        return ok  // 空实现
    }

    // 注册到子问题：适航安全约束只在子问题中
    fun registerForBendersSP(model: AbstractLinearMetaModel<Flt64>): Try {
        return aggregation.register(stowageMode = StowageMode.FullLoad, model = model)
    }
}

// StowageContext — 按 Benders 主/子问题选择性注册
class StowageContext {
    fun registerForBendersMP(model: AbstractLinearMetaModel<Flt64>): Try {
        return aggregation.registerForBendersMP(stowageMode = StowageMode.FullLoad, model = model)
    }

    fun registerForBendersSP(model: AbstractLinearMetaModel<Flt64>, solution: List<Flt64>): Try {
        return aggregation.registerForBendersSP(stowageMode = StowageMode.FullLoad, model = model, solution = solution)
    }
}
```

**关键点**：`stowageContext` 通过共享领域对象、Benders 专用注册路径和 `fixedVariables` 映射衔接主问题与子问题。当前实现是选择性注册：主问题注册装载决策及其相关主问题约束，子问题注册子问题需要的载重、总重、最大载重等中间值和适航约束。它不是简单地把同一组变量和中间值完整挂到两个 model 上。

### 7.4 不同应用的不同子域编排

demo2 展示了同一个业务领域（飞机装载）下，不同应用场景通过选择不同的子域组合来构建完全不同的优化问题——同一套子域积木，不同的拼装方式。

| Application | 子域组合 | 业务目标 |
|------------|---------|---------|
| **FullLoadApplication** | Aircraft + Stowage + MAC + AirworthinessSecurity + SoftSecurity + MacOptimization + ExpressEffectiveness + LoadingEffectiveness | 满载优化：最小化装载成本 |
| **WeightRecommendationApplication** | Aircraft + Stowage + MAC + AirworthinessSecurity + RecommendedWeightEqualization + PayloadMaximization | 建议打板：最小化重量偏差，最大化载重 |
| **PredistributionApplication** | Aircraft + Stowage + MAC + AirworthinessSecurity + SoftSecurity + MacOptimization + ExpressEffectiveness + LoadingEffectiveness + Redundancy | 预配载：最小化偏差 + 冗余约束 |
| **LoadingOrderApplication** | Aircraft（仅一个） | 装载顺序：纯数据导出，无优化 |

```kotlin
// FullLoadApplication — 编排 8 个子域
private val aircraftContext = AircraftContext()
private val stowageContext = StowageContext()
private val macContext = MacContext()
private val airworthinessSecurityContext = AirworthinessSecurityContext()
private val softSecurityContext = SoftSecurityContext()
private val macOptimizationContext = MacOptimizationContext()
private val expressEffectivenessContext = ExpressEffectivenessContext()
private val loadingEffectivenessContext = LoadingEffectivenessContext()
// register: stowage + mac + airworthiness + softSecurity + macOptimization + express + loading

// WeightRecommendationApplication — 编排 6 个子域（不同的子域组合）
private val aircraftContext = AircraftContext()
private val stowageContext = StowageContext()
private val macContext = MacContext()
private val airworthinessSecurityContext = AirworthinessSecurityContext()
private val recommendedWeightEqualizationContext = RecommendedWeightEqualizationContext()
private val payloadMaximizationContext = PayloadMaximizationContext()
// register: stowage + mac + airworthiness + recommendedWeightEqualization + payloadMaximization

// PredistributionApplication — 编排 9 个子域（比 FullLoad 多一个 Redundancy）
private val aircraftContext = AircraftContext()
private val stowageContext = StowageContext()
private val macContext = MacContext()
private val airworthinessSecurityContext = AirworthinessSecurityContext()
private val softSecurityContext = SoftSecurityContext()
private val macOptimizationContext = MacOptimizationContext()
private val expressEffectivenessContext = ExpressEffectivenessContext()
private val loadingEffectivenessContext = LoadingEffectivenessContext()
private val redundancyContext = RedundancyContext()
// register: stowage + mac + airworthiness + softSecurity + macOptimization + express + loading + redundancy

// LoadingOrderApplication — 仅 1 个子域，无优化
private val aircraftContext = AircraftContext()
// 无 register/solve，直接导出数据
```

**关键点**：每个 Application 是一个独立的编排单元，它决定：
1. 使用哪些子域（哪些 Context 参与）
2. 子域的初始化顺序（按依赖拓扑排序）
3. 子域的注册顺序（决定约束在 model 中的排列）
4. 求解策略（MILP / Benders / 纯数据导出）

子域本身是无状态的可复用组件——同一个 `MacContext` 在 FullLoad、WeightRecommendation、Predistribution 三个应用中都被使用，但它不需要知道谁在编排它。

### 7.5 中间值的多态实现：同一语义，不同变量实现

同一个 Context 的 `register()` 方法接收一个 `StowageMode` 参数，根据不同的模式产生不同的中间值和约束，这是中间值层面的多态。更准确地说，它包含两层变化：

1. **注册集合变化**：不同 `StowageMode` 会注册不同的中间值、变量、约束和目标。
2. **同名中间值的实现变化**：同一个中间值语义保持不变，但底层可以由不同决策变量、常量或派生中间值实现。

```kotlin
// StowageMode - 通过枚举属性控制中间值和约束的生成 / Controls generated symbols and constraints.
enum class StowageMode {
    Predistribution,
    FullLoad,
    WeightRecommendation {
        override val withMacOptimization: Boolean = false  // 不生成 MAC 优化中间值
        override val withSoftSecurity: Boolean = false      // 不生成软安全约束
        override val withPayloadMaximization: Boolean = true // 生成载重最大化中间值
    };

    open val withMacOptimization: Boolean = true   // 默认生成
    open val withSoftSecurity: Boolean = true       // 默认生成
    open val withPayloadMaximization: Boolean = false // 默认不生成
}
```

例如，`Payload.estimatePayload` 对外始终表示“预计载荷”。使用者只依赖这个语义，不需要知道它的公式来自哪里：

```kotlin
class Payload(
    private val aircraftModel: AircraftModel,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val load: Load,
    private val plannedPayload: Quantity<Flt64>,
    private val computedPayload: Quantity<Flt64>?
) {
    lateinit var estimatePayload: QuantityLinearIntermediateSymbol<Flt64>

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::estimatePayload.isInitialized) {
            estimatePayload = Quantity(
                when (stowageMode) {
                    StowageMode.FullLoad -> {
                        // 全配载：预计载荷由货物集合直接给出 / Full load: derived from all cargo items.
                        LinearExpressionSymbol(
                            LinearPolynomial(
                                items.fold(Flt64.zero) { acc, item ->
                                    acc + item.weight.to(aircraftModel.weightUnit)!!.value
                                }
                            ),
                            name = "estimate_payload"
                        )
                    }

                    StowageMode.Predistribution -> {
                        // 预配载：预计载荷使用既有计划或计算结果 / Predistribution: uses planned or computed payload.
                        LinearExpressionSymbol(
                            (computedPayload ?: plannedPayload).to(aircraftModel.weightUnit)!!.value,
                            name = "estimate_payload"
                        )
                    }

                    StowageMode.WeightRecommendation -> {
                        // 建议打板：预计载荷来自各舱位推荐载重变量的聚合 / Recommendation: sums recommended-position weights.
                        LinearExpressionSymbol(
                            sum(positions.indices.map { j ->
                                load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value
                            }),
                            name = "estimate_payload"
                        )
                    }
                },
                aircraftModel.weightUnit
            )
        }

        return model.add(estimatePayload)
    }
}
```

再往下一层看，`load.estimateLoadWeight[j]` 本身也可以保持同一个中间值名称和语义，但在不同模式下由不同决策变量实现。下面是抽象后的示意代码：

```kotlin
class Load(
    private val aircraftModel: AircraftModel,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage
) {
    lateinit var y: QuantityURealVariable1     // 预配载重量变量 / Predistribution weight variable.
    lateinit var z: QuantityUIntVariable1      // 建议重量变量 / Recommendation weight variable.
    lateinit var estimateLoadWeight: QuantityLinearIntermediateSymbols1<Flt64>

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::estimateLoadWeight.isInitialized) {
            estimateLoadWeight = QuantityLinearIntermediateSymbols1(
                name = "load_weight",
                shape = Shape1(positions.size)
            ) { j, _ ->
                QuantityLinearIntermediateSymbol(
                    LinearExpressionSymbol(
                        loadWeightPolynomial(
                            stowageMode = stowageMode,
                            positionIndex = j
                        ),
                        name = "load_weight_${positions[j]}"
                    ),
                    aircraftModel.weightUnit
                )
            }
        }

        return model.add(estimateLoadWeight)
    }

    private fun loadWeightPolynomial(
        stowageMode: StowageMode,
        positionIndex: Int
    ): LinearPolynomial<Flt64> {
        return when (stowageMode) {
            StowageMode.FullLoad -> {
                // 全配载：由货物-舱位装载决策 x/u 派生 / Full load: derived from assignment decisions x/u.
                sum(items.mapIndexed { itemIndex, item ->
                    item.weight.to(aircraftModel.weightUnit)!!.value *
                        stowage.stowage[itemIndex, positionIndex]
                })
            }

            StowageMode.Predistribution -> {
                // 预配载：由预配重量决策 y[j] 实现 / Predistribution: implemented by y[j].
                LinearPolynomial(y[positionIndex].to(aircraftModel.weightUnit)!!.value)
            }

            StowageMode.WeightRecommendation -> {
                // 建议打板：由推荐重量决策 z[j] 实现 / Recommendation: implemented by z[j].
                LinearPolynomial(z[positionIndex].to(aircraftModel.weightUnit)!!.value)
            }
        }
    }
}
```

因此，`AirworthinessSecurity`、`MAC`、`PayloadMaximization` 等下游子域可以始终引用 `payload.estimatePayload` 或 `load.estimateLoadWeight[j]`。它们不需要知道当前模式下这个中间值到底是常量、装载决策 `x/u` 的线性组合、预配重量变量 `y[j]`，还是推荐重量变量 `z[j]`。

Context 的 `register()` 方法再根据 `StowageMode` 的属性决定注册哪些中间值和约束：

```kotlin
// StowageContext.register() — 同一个接口，根据 stowageMode 产生不同的模型
fun register(stowageMode: StowageMode, model: AbstractLinearMetaModel<Flt64>): Try {
    // 1. 注册基础变量和中间值（所有模式共享）
    aggregation.register(stowageMode, model)

    // 2. 根据 stowageMode 生成不同的约束管线
    val generator = PipelineListGenerator(aggregation)
    val pipelines = generator.invoke(stowageMode)  // PipelineList 根据模式动态生成
    for (pipeline in pipelines) {
        pipeline(model)
    }
    return ok
}
```

Application 层在调用时指定模式：

```kotlin
// FullLoadApplication — 使用 FullLoad 模式
stowageContext.register(stowageMode = StowageMode.FullLoad, model = model)
macContext.register(stowageMode = StowageMode.FullLoad, model = model)
softSecurityContext.register(stowageMode = StowageMode.FullLoad, model = model)

// WeightRecommendationApplication — 使用 WeightRecommendation 模式
// 同一个 stowageContext，但产生不同的中间值和约束
stowageContext.register(stowageMode = StowageMode.WeightRecommendation, model = model)
macContext.register(stowageMode = StowageMode.WeightRecommendation, model = model)
// softSecurityContext 不参与（WeightRecommendation.withSoftSecurity = false）
```

**这与 OOP 多态的类比**：

| OOP 概念 | 中间值多态对应 |
|---------|-------------|
| 接口（Interface） | 稳定的中间值语义，如 `payload.estimatePayload`、`load.estimateLoadWeight[j]` |
| 实现类 | `StowageMode` 下对应的公式、决策变量集合和注册路径 |
| 多态调用 | 下游子域引用同一个中间值，Context 根据模式绑定不同实现 |
| 策略模式 | `StowageMode` 的属性（`withMacOptimization`、`withSoftSecurity` 等）控制行为分支 |

这种设计使得：
- **同一个 Context 可以服务于不同的业务场景**，无需为每个场景创建子类
- **同一个中间值语义可以有不同实现**——例如同一个预计载荷可以由常量、装载决策、预配重量变量或推荐重量变量计算得到
- **中间值的实现细节对使用者透明**——Application 层只需要选择模式，不需要知道模式内部生成了哪些中间变量、变量来源和约束
- **新增模式的入口较稳定，但不是完全对修改关闭**——新增 `StowageMode` 枚举值可以复用同一注册入口，但仍可能需要扩展枚举属性、`when` / 策略分支或 `PipelineListGenerator` 的组合逻辑

### 7.6 共享变量与 Benders 切割

```kotlin
private fun buildBendersModels(): BendersModels {
    val masterModel = LinearMetaModel<Flt64>(name = "demo2_full_load_master", ...)
    val subModel = LinearMetaModel<Flt64>(name = "demo2_full_load_sub", ...)

    // 各子域选择性注册
    stowageContext.registerForBendersMP(masterModel)
    macContext.registerForBendersMP(masterModel)
    softSecurityContext.registerForBendersMP(masterModel)
    macOptimizationContext.registerForBendersMP(masterModel)
    expressEffectivenessContext.registerForBendersMP(masterModel)
    loadingEffectivenessContext.registerForBendersMP(masterModel)

    stowageContext.registerForBendersSP(subModel, emptyList())
    airworthinessSecurityContext.registerForBendersSP(subModel)

    // Benders 切割变量
    val thetaVar = URealVar("benders_theta")
    masterModel.add(thetaVar)

    // 共享变量映射：stowage 的 x 变量在两个 model 中的对应关系
    val stowageAgg = stowageContext.aggregation
    val fixedVariables = mutableMapOf<AbstractVariableItem<*, *>, Flt64>()
    for (i in stowageAgg.items.indices) {
        for (j in stowageAgg.positions.indices) {
            fixedVariables[stowageAgg.stowage.x[i, j]] = Flt64.zero
        }
    }

    return BendersModels(masterModel, subModel, thetaVar, fixedVariables)
}
```

`fixedVariables` 建立了主问题变量到子问题固定值的映射——这正是 Benders 分解中"将主问题的解固定到子问题"的核心步骤。由于变量由领域对象持有，Application 可以用同一组 `stowageAgg.stowage.x[i, j]` 构造映射，并在主/子问题注册路径之间保持语义一致；当前实现依赖该映射和选择性注册，而不是把所有 stowage 中间值完整复制到两个 model 后自动对应。

### 7.7 质量守卫与降级策略

Benders 求解完成后，Application 层执行质量守卫检查：

```kotlin
private suspend fun solveWithBendersAlgorithm(
    request: RequestDTO,
    notes: MutableList<String>
): Ret<Solution> {
    // 1. 构建 Benders 模型
    val bendersModels = buildBendersModels()

    // 2. 自适应配置调优
    val bendersConfig = BendersStrategy.tuneAdaptiveConfig(
        request.bendersAdaptive,
        request.cargos.size * request.positions.size
    )

    // 3. 求解
    val solver = GurobiLinearBendersDecompositionSolver()
    val bendersResult = BendersSolver.solve(
        solver = solver,
        masterModel = bendersModels.masterModel,
        subModel = bendersModels.subModel,
        fixedVariables = bendersModels.fixedVariables,
        objectVariable = bendersModels.objectVariable,
        config = bendersConfig,
        notes = notes
    )

    // 4. 质量守卫检查
    val qualityGuard = BendersStrategy.resolveQualityGuardConfig(request.bendersQualityOverrides)
    val qualityReason = BendersStrategy.resolveQualityReason(
        adaptive = bendersConfig,
        qualityGuard = qualityGuard,
        bendersIterations = bendersResult.bendersIterations,
        bendersGap = bendersResult.gap,
        bendersTimeMs = bendersResult.timeMs,
        executedIterations = bendersResult.runtimeMetrics?.executedIterations,
        totalCuts = bendersResult.runtimeMetrics?.totalCuts,
        iterationSnapshots = bendersResult.runtimeMetrics?.iterationSnapshots?.map { it.masterObj }
    )

    // 5. 质量不达标时的降级决策
    if (qualityReason != null) {
        // 记录诊断信息
        val qualityCode = when (qualityReason) {
            "gap_guard_exceeded" -> Diagnostics.CODE_BENDERS_GAP_GUARD_EXCEEDED
            "time_guard_exceeded" -> Diagnostics.CODE_BENDERS_TIME_GUARD_EXCEEDED
            "progress_guard_triggered" -> Diagnostics.CODE_BENDERS_PROGRESS_GUARD_TRIGGERED
            "cut_efficiency_low" -> Diagnostics.CODE_BENDERS_CUT_EFFICIENCY_LOW
            "trajectory_weak" -> Diagnostics.CODE_BENDERS_TRAJECTORY_WEAK
            else -> Diagnostics.CODE_BENDERS_FAILED
        }

        // 根据策略决定降级行为
        if (request.solvePolicy.bendersFallbackToMilp) {
            // 降级到 MILP
            return Failed(Err(ErrorCode.ApplicationError, "Benders quality insufficient: $qualityReason"))
        }
    }

    // 6. 将 Benders 解转换为领域解
    val solutionList = bendersResult.solution.map { Flt64(it) }
    return stowageContext.analyze(solution = solutionList, model = bendersModels.masterModel)
}
```

质量守卫检查的维度：

| 检查项 | 含义 |
|--------|------|
| `gap_guard_exceeded` | Benders gap 超过阈值 |
| `time_guard_exceeded` | 求解时间超过阈值 |
| `progress_guard_triggered` | 目标值长时间无改善 |
| `cut_efficiency_low` | 割平面效率低下 |
| `trajectory_weak` | 目标值收敛轨迹不佳 |

降级策略的决策流：

```
Benders 求解
  ├── 成功
  │     ├── 质量达标 → 返回 Benders 解
  │     └── 质量不达标
  │           ├── 策略允许降级 → 回退到 MILP 求解
  │           └── 策略不允许 → 返回失败
  ├── Failed
  │     ├── 策略允许降级 → 回退到 MILP 求解
  │     └── 策略不允许 → 返回失败
  └── Fatal
        ├── 策略允许降级 → 回退到 MILP 求解
        └── 策略不允许 → 返回失败
```

### 7.8 传统模式的 Benders 对比

传统模式中实现 Benders 分解面临以下困难：

- **solver SDK 原生变量无法跨 model 复用**：需要手动在两个 model 中分别声明相同的变量，维护两份变量定义和映射的一致性
- **无中间值机制**：主问题和子问题之间的表达式共享需要手动展开，无法通过命名中间值实现语义一致
- **无 Pipeline 机制**：约束的拆分（哪些属于主问题、哪些属于子问题）需要手动在代码中区分，无法通过子域的 `registerForBendersMP` / `registerForBendersSP` 自然表达
- **无质量守卫框架**：降级策略需要从零实现

---

## 8. 列生成（Branch-and-Price）：中间值驱动的迭代分解

除了 Benders 分解，DDD 框架模式还支持另一种经典分解算法——**列生成**（Column Generation），也称为 Branch-and-Price。demo4（机组排班）展示了这一模式在 DDD 框架中的完整实现。

### 8.1 列生成的核心思想

列生成将大规模 MILP 问题拆分为：

- **主问题（Master Problem）**：从已有的列（bunch）中选择最优组合，满足覆盖约束
- **定价子问题（Pricing Subproblem）**：利用主问题的对偶值（shadow price），生成 reduced cost 为负的新列

两者的通信机制是 **Shadow Price Map**——主问题求解后，将约束的对偶值传递给定价子问题，子问题据此生成有价值的列并添加到主问题中，迭代直到没有新的负 reduced cost 列。

### 8.2 demo4 的子域角色划分

```
demo4/
  domain/
    ├── crew/                 ← 数据子域：机组人员
    ├── task/                 ← 数据子域：飞行任务
    ├── rule/                 ← 数据子域：排班规则
    ├── cargo/                ← 数据子域：货物
    ├── passenger/            ← 数据子域：旅客
    │
    ├── bunch_generation/     ← Pricing 子问题：生成新列
    │     ├── model/Graph.kt        — 飞行图（Node, Edge, Graph）
    │     ├── model/FlightTaskReverse.kt — 可逆任务对
    │     └── service/
    │           ├── FlightTaskBunchGenerator.kt — Label Setting 算法
    │           ├── InitialFlightTaskBunchGenerator.kt — 初始列生成
    │           └── FlightTaskFeasibilityJudger.kt — 10 步可行性检查
    │
    ├── bunch_compilation/    ← Master 约束：机队平衡、航班链接
    │     ├── model/FlightLink.kt    — 航班链接约束
    │     ├── model/FleetBalance.kt  — 机队平衡约束
    │     └── service/limits/        — 约束 Pipeline
    │
    └── bunch_selection/      ← Branch-and-Price 编排
          └── service/BranchAndPriceAlgorithm.kt
```

三个核心子域的职责边界：

| 子域 | 角色 | 职责 | 不承担 |
|------|------|------|--------|
| `bunch_generation` | Pricing 子问题 | 飞行图构建、初始列生成、Label Setting 定价 | Master 约束、fleet balance、solution 解析 |
| `bunch_compilation` | Master 约束 | 注册航班链接约束、机队平衡约束、航班容量约束 | Label Setting、route graph、reduced cost |
| `bunch_selection` | 编排器 | Branch-and-price 迭代、shadow price 提取、add columns | 具体 pricing 逻辑、具体约束注册 |

### 8.3 BunchGenerationContext：Pricing 子问题的实现

`BunchGenerationContext` 管理定价子问题的完整生命周期：

```kotlin
class BunchGenerationContext {
    private lateinit var aggregation: Aggregation
    private lateinit var generators: Map<Aircraft, FlightTaskBunchGenerator>

    fun init(
        aircrafts: List<Aircraft>,
        aircraftUsability: Map<Aircraft, AircraftUsability>,
        flightTasks: List<FlightTask>,
        // ... 规则检查器、成本计算器等依赖
    ): Try {
        // 1. 构建可行性判断器
        feasibilityJudger = FlightTaskFeasibilityJudger(...)

        // 2. 生成初始列（每架飞机的初始可行 bunch）
        aggregation = AggregationInitializer()(
            aircrafts, aircraftUsability, flightTasks, originBunches, lock,
            feasibilityJudger, initialFlightTaskBunchGenerator, withOrderChange
        )

        // 3. 为每架飞机创建定价生成器
        generators = aircrafts.associateWith { aircraft ->
            FlightTaskBunchGenerator(
                aircraft = aircraft,
                graph = aggregation.graphs[aircraft]!!,  // 飞行图
                costCalculator = costCalculator,
                configuration = BunchGenerationConfiguration(...)
            )
        }
        return ok
    }

    // 定价入口：接收 shadow price，输出新列
    fun generateFlightTaskBunch(
        aircrafts: List<Aircraft>,
        iteration: Int64,
        shadowPriceMap: ShadowPriceMap  // ← 从主问题传入的对偶值
    ): Ret<List<FlightTaskBunch>> {
        val bunches = ArrayList<FlightTaskBunch>()
        for (aircraft in aircrafts) {
            val thisBunches = generators[aircraft]!!(iteration, shadowPriceMap)
            bunches.addAll(thisBunches)
        }
        return Ok(bunches)
    }
}
```

### 8.4 Label Setting 算法：定价子问题的核心

`FlightTaskBunchGenerator` 实现了经典的 Label Setting 算法，在飞行图上搜索负 reduced cost 的路径：

```kotlin
class FlightTaskBunchGenerator(
    private val aircraft: Aircraft,
    private val graph: Graph,                    // 飞行图
    private val costCalculator: CostCalculator,
    private val configuration: BunchGenerationConfiguration
) {
    operator fun invoke(iteration: Int64, shadowPriceMap: ShadowPriceMap): Ret<List<FlightTaskBunch>> {
        val labels: LabelMap = HashMap()
        initRootLabel(labels, shadowPriceMap)  // 初始化根标签

        // 按拓扑序遍历图节点
        for (prevNode in nodes) {
            for (prevLabel in getLabels(labels, prevNode)) {
                for (edge in graph[prevNode]) {
                    val succNode = edge.to
                    if (succNode is EndNode) {
                        // 到达终点：累加 shadow price，记录标签
                        val builder = LabelBuilder(succNode, prevLabel)
                        builder.shadowPrice += shadowPriceMap(prevLabel.flightTask!!, null)
                        insertLabel(succLabels, Label(builder))
                    } else if (!prevLabel.visited(succNode)) {
                        // 扩展标签：计算成本、shadow price、延迟
                        val succLabel = generateFlightTaskLabel(prevLabel, succNode, shadowPriceMap)
                        if (succLabel != null) insertLabel(succLabels, succLabel)
                    }
                }
            }
        }
        // 选择 reduced cost < 0 的 bunch
        return Ok(selectBunches(iteration, labels[EndNode]!!))
    }
}
```

标签的核心属性：

```kotlin
private data class Label(
    val cost: Cost<FltX>,        // 原始成本
    val shadowPrice: Flt64,       // 累计 shadow price（从主问题对偶值）
    val delay: Duration,          // 延迟
    val prevLabel: Label?,        // 前驱标签（用于回溯路径）
    val node: Node,               // 当前节点
    val flightTask: FlightTask?   // 关联的飞行任务
) {
    val reducedCost: Flt64 get() = cost.solverCostOrNull(Flt64.zero)!! - shadowPrice
    val isBetterBunch get() = reducedCost < Flt64.zero

    // 支配规则：同末端节点下，reduced cost 更小、延迟更短、飞机变更更少的标签支配其他标签
    infix fun ls(rhs: Label): Boolean {
        return (reducedCost - rhs.reducedCost) < Flt64.zero
                && delay <= rhs.delay
                && ((node is EndNode) || (aircraftChange >= rhs.aircraftChange))
    }
}
```

### 8.5 Shadow Price：主问题与子问题的通信桥梁

`ShadowPriceMap` 是列生成中主问题与定价子问题的通信机制——它封装了主问题约束的对偶值，定价子问题通过查询它来计算 reduced cost：

```kotlin
// 定价子问题查询 shadow price
val shadowPrice = shadowPriceMap(aircraft)                      // 飞机可用性约束的对偶值
val shadowPrice = shadowPriceMap(prevFlightTask, recoveryTask)  // 航班链接约束的对偶值

// 标签的 reduced cost = 原始成本 - 累计 shadow price
val reducedCost = cost.solverCostOrNull(Flt64.zero)!! - shadowPrice

// 只有 reduced cost < 0 的列才有价值（能改善主问题目标）
val isBetterBunch = reducedCost < Flt64.zero
```

这与 Benders 分解中的 `fixedVariables` 类似——都是子域之间通过共享数据结构进行通信的机制，但通信方向不同：

| 分解算法 | 通信方向 | 通信内容 |
|---------|---------|---------|
| Benders | 主问题 → 子问题 | `fixedVariables`：主问题的变量值固定到子问题 |
| 列生成 | 主问题 → 子问题 | `ShadowPriceMap`：主问题的对偶值传递到定价子问题 |
| 列生成 | 子问题 → 主问题 | `FlightTaskBunch`：新列添加到主问题 |

### 8.6 BunchCompilationContext：Master 约束的注册

`BunchCompilationContext` 负责将主问题的约束（航班链接、机队平衡、航班容量）注册到 model：

```kotlin
class BunchCompilationContext : BunchCompilationContext<
    ShadowPriceArguments, FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment
> {
    override lateinit var aggregation: Aggregation
    override lateinit var pipelineList: CGPipelineList

    override fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        // 生成约束管线（FlightLinkLimit, FleetBalanceLimit 等）
        pipelineList = PipelineListGenerator(aggregation)()
        // 调用框架基类的 register，将管线注入 model
        return super.register(model)
    }
}
```

### 8.7 与传统列生成实现的对比

| | 传统模式 | DDD 框架模式 |
|---|---------|-------------|
| **列的表示** | 原始数组/列表，手动管理 | `FlightTaskBunch` 领域对象，包含飞机、任务序列、成本 |
| **定价算法** | 直接编写 Label Setting 循环 | `FlightTaskBunchGenerator` 封装在 `bunch_generation` 子域中 |
| **Shadow price 传递** | 手动从 solver 提取对偶值，传递给定价函数 | `ShadowPriceMap` 作为子域间的通信协议 |
| **Master 约束** | 在主循环中手动添加 | `BunchCompilationContext` 通过 Pipeline 机制注册 |
| **迭代编排** | 手动 while 循环 + 收敛判断 | `BranchAndPriceAlgorithm` 框架级编排 |
| **与 Benders 的复用** | 完全独立的代码 | 共享相同的子域基础设施（Context、Aggregation、Pipeline） |

---

## 9. 框架级应用：DDD 模式在 ospf-kotlin-framework 中的实践

demo1–demo4 是示例代码，而 ospf-kotlin 的框架层（framework）将 DDD 模式固化为可复用的领域框架。以下是三个核心框架的子域结构，它们共享相同的架构范式：Context + Aggregation + Model + Service + Pipeline。

### 9.1 gantt-scheduling（甘特排程框架）

适用于生产排程（APS）、批次生产（LSP）、机组排班等调度/规划问题。采用列生成（Branch-and-Price）分解。

```
ospf-kotlin-framework-gantt-scheduling/
  ├── gantt-scheduling-domain-task-context/                 ← 任务子域：任务定义、时间、切换
  ├── gantt-scheduling-domain-task-generation-context/      ← 任务生成占位模块（当前与束生成集成）
  ├── gantt-scheduling-domain-task-compilation-context/     ← 任务编译子域
  ├── gantt-scheduling-domain-bunch-generation-context/     ← 束生成子域（pricing，列生成）
  ├── gantt-scheduling-domain-bunch-compilation-context/    ← 束编译子域（master 约束，列生成）
  ├── gantt-scheduling-domain-resource-context/             ← 资源子域：资源容量、使用量
  ├── gantt-scheduling-domain-capacity-scheduling-context/  ← 容量调度子域：时间槽分配
  ├── gantt-scheduling-domain-produce-context/              ← 生产子域：物料、产出、消耗
  ├── gantt-scheduling-application/                         ← Application 层编排
  └── gantt-scheduling-infrastructure/                      ← 基础设施：求解器、DTO、时间工具
```

**列生成结构**：gantt-scheduling 的可落地主线是束层列生成，即 `gantt-scheduling-domain-bunch-generation-context` 作为 pricing 子问题，`gantt-scheduling-domain-bunch-compilation-context` 作为 master 约束。`gantt-scheduling-domain-task-compilation-context` 是实际任务编译子域；`gantt-scheduling-domain-task-generation-context` 目前是占位模块，任务生成能力已集成到束生成模块中。

**子域依赖图**：

```
gantt-scheduling-domain-task-context
  ├── gantt-scheduling-domain-task-compilation-context
  ├── gantt-scheduling-domain-resource-context ──→ gantt-scheduling-domain-capacity-scheduling-context
  └── gantt-scheduling-domain-produce-context

gantt-scheduling-domain-bunch-generation-context (pricing) ──→ gantt-scheduling-domain-bunch-compilation-context (master)
gantt-scheduling-domain-task-generation-context (placeholder)

gantt-scheduling-application → 编排所有子域
```

**泛型数量系统**：框架通过 `TimeRange`、`Cost<V>`、`ResourceCapacity<V>`、`TaskTime<V>` 等泛型类型，将时间、成本、资源容量等业务概念统一为可组合的中间值，支持不同数值类型（`Flt64`、`FltX` 等）。

### 9.2 bpp3d（三维装箱框架）

适用于集装箱装载、托盘堆垛等三维空间优化问题。采用列生成分解。

```
ospf-kotlin-framework-bpp3d/
  ├── bpp3d-domain-item-context/                 ← 物品子域：物品定义、尺寸、重量
  ├── bpp3d-domain-packing-context/              ← 装箱子域：装箱约束、稳定性
  ├── bpp3d-domain-layer-generation-context/     ← 层生成子域（pricing）：生成可行层
  ├── bpp3d-domain-layer-assignment-context/     ← 层分配 / master 子域：列生成模型、约束、影子价格、解提取
  ├── bpp3d-domain-block-loading-context/        ← 块装载子域：块级装载策略
  ├── bpp3d-domain-bla-context/                  ← BLA 子域：Bottom-Left-Algorithm
  ├── bpp3d-application/                         ← Application 层编排
  └── bpp3d-infrastructure/                      ← 基础设施
```

**列生成结构**：`bpp3d-domain-layer-generation-context` 作为 pricing 子问题生成可行的层（layer），`bpp3d-domain-layer-assignment-context` 作为 master / assignment 问题维护约束、影子价格和解提取。这与 gantt-scheduling 的 bunch-generation / bunch-compilation 主线同构。

**子域依赖图**：

```
bpp3d-domain-item-context
  ├── bpp3d-domain-packing-context
  ├── bpp3d-domain-layer-generation-context (pricing) ──→ bpp3d-domain-layer-assignment-context (master / assignment)
  ├── bpp3d-domain-block-loading-context
  └── bpp3d-domain-bla-context

bpp3d-application → 编排所有子域
```

### 9.3 csp1d（一维下料框架）

适用于钢管切割、纸张裁切等一维下料问题。采用列生成分解。

```
ospf-kotlin-framework-csp1d/
  ├── material-context/                ← 原材料子域：原材料定义、长度、成本
  ├── produce-context/                 ← 生产子域：生产约束、需求
  ├── cutting-plan-generation-context/ ← 切割计划生成子域（pricing）：生成可行切割方案
  ├── length-assignment-context/       ← 长度分配子域：将长度分配到原材料
  ├── yield-context/                   ← 产出率子域：利用率优化
  ├── wasting-minimization-context/    ← 废料最小子域：废料最小化目标
  ├── application/                     ← Application 层编排
  └── infrastructure/                  ← 基础设施
```

**列生成结构**：`cutting-plan-generation-context` 作为 pricing 子问题生成可行的切割方案（cutting pattern），master 问题选择最优的方案组合以满足需求并最小化废料。

**子域依赖图**：

```
material-context
  ├── produce-context
  ├── cutting-plan-generation-context (pricing) ──→ master 问题
  ├── length-assignment-context
  ├── yield-context
  └── wasting-minimization-context

application → 编排所有子域
```

### 9.4 三个框架的共同模式

| 模式 | gantt-scheduling | bpp3d | csp1d |
|------|-----------------|-------|-------|
| **领域对象** | Task, Bunch, Crew, Resource | Item, Layer, Block, Container | Material, CuttingPlan, Length |
| **Pricing 子问题** | bunch-generation（Label Setting） | layer-generation | cutting-plan-generation |
| **Master 约束** | bunch-compilation（航班链接、机队平衡） | layer-assignment | 需求满足、原材料容量 |
| **列生成迭代** | ShadowPriceMap → 新 Bunch | ShadowPriceMap → 新 Layer | ShadowPriceMap → 新 CuttingPlan |
| **分解算法** | 列生成（Branch-and-Price） | 列生成 | 列生成 |
| **子域模块数** | 8 个 domain context（其中 task-generation 当前为占位） | 6 个 domain context | 6 个 domain context |
| **共享基础设施** | gantt-scheduling-infrastructure | bpp3d-infrastructure | csp1d-infrastructure |

**关键观察**：三个框架虽然领域完全不同（排程、装箱、下料），但共享相同的架构骨架：

1. **数据子域**（material/item/task）定义领域实体
2. **Pricing 子域**（*-generation）封装列生成算法
3. **编译/分配子域**（*-compilation/*-assignment 等）封装 master 约束
4. **Application 层**编排子域的 init → register → solve → analyze
5. **Infrastructure 层**封装求解器和 DTO

这种一致性使得开发者掌握一个框架后，可以快速理解其他框架的结构——DDD 模式不仅是代码组织方式，更是运筹优化领域的**通用架构语言**。

---

## 10. 适用场景分析

### 10.1 传统模式适用场景

- **教学示例**：展示 solver API 的基本用法
- **快速原型**：一次性验证数学模型的正确性
- **单一模型**：问题结构简单，无子域拆分需求
- **脚本式调用**：不需要长期维护和扩展

### 10.2 DDD 框架模式适用场景

- **生产系统**：需要长期维护、持续迭代
- **多约束来源**：不同业务规则由不同团队或不同业务领域维护
- **求解器无关**：需要在 Gurobi/SCIP/COPT 之间切换或降级
- **分解算法**：需要 Benders 分解或列生成（Branch-and-Price / Dantzig-Wolfe）
- **复杂编排**：需要心跳回调、诊断日志、质量守卫等生产级特性
- **子域复用**：同一套领域模型用于不同的求解路径（MILP/Benders/列生成）
- **多应用场景**：同一业务领域需要多个不同的优化应用（如 demo2 的 FullLoad / WeightRecommendation / Predistribution）
- **框架级复用**：需要将领域知识沉淀为可复用框架（如 gantt-scheduling / bpp3d / csp1d）

---

## 11. 总结对比表

| 维度 | 传统模式 | DDD 框架模式 |
|------|---------|-------------|
| **代码组织** | 单文件线性排列 | 按领域分层（domain/infrastructure/application） |
| **变量所有权** | solver SDK 原生变量归 model；core 脚本式用法围绕单一 `metaModel` 流程 | 归领域对象所有 |
| **变量生命周期** | solver SDK 原生变量绑定 model；core 脚本式示例缺少跨子域生命周期管理 | 独立于 model |
| **中间值** | solver SDK 原生 API 不提供 DDD 中间值抽象；OSPF core 可使用具名表达式符号 | 算术中间值 + 函数中间值，具名、可复用、全局作用域 |
| **子域组合** | 不适用 | 通过中间值实现跨域组合，使用者无需了解中间值的内部实现 |
| **约束管理** | 副作用（addConstr） | 一等公民（Pipeline 对象），可命名、可组合、可延迟执行 |
| **多应用场景** | 每个场景独立实现 | 同一套子域积木，不同 Application 编排不同子域组合 |
| **中间值多态** | 不适用 | 同一 Context 根据 StowageMode 等参数为同一中间值语义绑定不同公式、变量集合、约束和目标 |
| **求解器绑定** | 直接 import | 可通过运行时抽象收敛到基础设施层 |
| **解分析** | 手动遍历原始数值 | 领域对象化返回 |
| **Benders 分解** | 需手动实现，solver SDK 原生变量无法跨 model | 通过选择性注册、共享领域对象和固定变量映射支持 |
| **列生成** | 需手动实现定价循环和 shadow price 传递 | 框架级支持，Label Setting 封装在子域中，ShadowPriceMap 作为通信协议 |
| **框架级复用** | 不适用 | 相同架构骨架应用于不同领域（甘特排程、三维装箱、一维下料） |
| **可测试性** | 仅端到端 | 每个 Context/Model/Pipeline 可独立测试 |
| **可扩展性** | 扩展通常需要改动主流程 | 通过新增 Context、Pipeline 或策略入口扩展 |
| **初始成本** | 低 | 高（需理解框架分层、中间值和 Context 编排） |
| **维护成本** | 复杂度容易集中到主流程 | 复杂度由子域边界分摊 |
| **适用规模** | 小型/教学 | 中大型/生产 |

---

## 12. 一句话总结

solver SDK 原生模式通常以具体 model 为建模中心，变量、表达式和约束围绕单一求解器对象展开；DDD 框架模式则把变量和中间值提升为领域对象，model 主要承担注册、编译和求解载体的职责。中间值机制使得子域之间可以实现"关注点分离"——一个子域维护的运算结果通过中间值暴露给其他子域，使用者只关注语义，不关注实现。而中间值的多态实现（同一 Context 根据 `StowageMode` 等参数，为同一中间值语义绑定不同公式、决策变量集合、约束和目标）使得同一个子域可以服务于不同的业务场景，无需为每个场景创建子类。这种独立生命周期、中间值组合和中间值多态的三重机制，使得 Benders 分解和列生成（Branch-and-Price / Dantzig-Wolfe）等复杂分解算法可以沿着既有子域边界扩展。同一个架构骨架——数据子域 → pricing 子域 → master 约束 → Application 编排 ——在甘特排程、三维装箱、一维下料等不同领域中重复出现，说明 DDD 模式不仅是代码组织方式，也是一套适合运筹优化领域沉淀复用能力的架构语言。这是从"用 solver API 写数学公式"到"用领域语言描述业务规则"的范式跃迁。
