# ospf-kotlin-framework-bpp2d（中文）

:us: [English](README.md) | :cn: 简体中文

## 子模块

| 模块 | 描述 |
| --- | --- |
| [bpp2d-infrastructure](bpp2d-infrastructure/README_ch.md) | 量纲几何、装载形状（矩形/圆形）、板材形状、放置、渲染 DTO 与领域错误，供所有子模块共享。 |
| [bpp2d-domain-item-context](bpp2d-domain-item-context/README_ch.md) | 物品、板材、层、排样程序、需求统计、求解器值适配器，以及既有 `*Need` 兼容桥接。 |
| [bpp2d-domain-bla-context](bpp2d-domain-bla-context/README_ch.md) | 矩形件在板材平面上的底部向上左对齐（BLA）排样。 |
| [bpp2d-domain-block-loading-context](bpp2d-domain-block-loading-context/README_ch.md) | 二维空间模型、简单/复杂块生成器，以及 DFS / 多行空间切分搜索。 |
| [bpp2d-domain-layer-generation-context](bpp2d-domain-layer-generation-context/README_ch.md) | 候选层生成：块装载、空间切分、BLA、图样、圆形网格（直角/六角）与混合形状策略。 |
| [bpp2d-domain-layer-assignment-context](bpp2d-domain-layer-assignment-context/README_ch.md) | 列生成模型：赋值、负载、容量、带影子价格的需求约束与目标函数。 |
| [bpp2d-domain-packing-context](bpp2d-domain-packing-context/README_ch.md) | 终态装箱：几何校验、装箱器、物料汇总与渲染适配器。 |
| [bpp2d-application](bpp2d-application/README_ch.md) | 列生成编排、标准执行器、应用服务与 KPI 分析器。 |

## 模块范围

`ospf-kotlin-framework-bpp2d` 是 OSPF Kotlin 的二维矩形与圆形装箱框架。当前能力：

1. 矩形件，支持可选的 90° 旋转。
2. 圆形件，通过直角网格与六角网格候选排布。
3. 混合形状排布（矩形行 + 圆形网格），并逐对通过底层几何校验。
4. 完整列生成闭环：候选层 → RMP(LP) 求对偶 → 加列 → 最终 MILP → 装箱 → KPI。
5. LP / MILP 接入真实 Gurobi 求解器。

## 形状支持矩阵

| 路径 | 矩形件 | 圆形件 |
| --- | --- | --- |
| 层生成：块装载（局部/全局） | 支持 | 跳过（圆形件无整行块语义） |
| 层生成：DFS / 多行空间切分 | 支持 | 跳过 |
| 层生成：BLA（底部向上左对齐） | 支持 | 跳过并显式标记 |
| 层生成：图样 | 支持 | 跳过 |
| 层生成：圆形网格 | 不适用 | 支持（直角网格与六角网格） |
| 层生成：混合形状 | 支持 | 支持（矩形行 + 圆形网格，逐对校验） |
| 几何校验 / 装箱 / 渲染 | 支持 | 支持 |
| 面积口径 | `w × h` | `πr²`（实际面积）/ `4r²`（外接面积） |

## 重叠语义

重叠统一为**内部重叠**：相切不算重叠。矩形贴边、圆与圆相切、圆与矩形边界相切都视为不重叠。这是网格排样的
前提——若把相切判为重叠，任何规则网格都无法成立。

## 公开 API

| API | 职责 | 稳定性 |
| --- | --- | --- |
| `PackingShape2<V>` / `RectanglePackingShape2` / `CirclePackingShape2` | 形状抽象，提供实际面积与外接面积。 | stable |
| `Sheet2Shape<V>` / `LayerPlacement2<V>` / `Box2Geometry<V>` | 板材几何、放置与内部重叠判定。 | stable |
| `LayerGenerationContext<V>` | 带形状分派的组合式候选生成器。 | stable |
| `LayerAssignmentContext` | 列生成模型的五阶段生命周期。 | stable |
| `ColumnGenerationAlgorithm` | 列生成编排器，带失败阶段。 | stable |
| `ColumnGenerationStandardExecutors` | 标准 RMP / 最终 MILP / 请求构建器装配。 | stable |
| `ColumnGenerationApplicationService` | 端到端入口，输出装箱结果与 KPI。 | stable |
| `PackingGeometryGuard` / `Packer` / `PackingRendererAdapter` | 终态几何校验、装箱与渲染 DTO 输出。 | stable |

## 列生成扩展点

1. `ColumnGenerationAlgorithm` 接受 `rmpSolver`、`finalMilpSolver`、`layerRequestBuilder`、
   `initialColumnsWithResult`、`filterByReducedCostWithResult` 与 `failureAnalyzer`，下游无需复制循环即可替换装配。
2. `LayerGenerationContext` 接受显式生成器列表；新策略只需实现 `Bpp2dLayerGenerator`。
3. `LayerAssignmentContext` 暴露 `registerRmp` / `addColumns` / `removeColumns` / `registerFinal` /
   `registerAreaMinimization` / `registerSheetAmountMinimization`。
4. `ColumnGenerationApplicationService` 接受算法工厂，可整体替换装配。

## 泛型数值与物理量边界

1. infrastructure、item、BLA、block-loading、layer-generation、packing 的领域模型泛型于
   `V : FloatingNumber<V>`。
2. 长度、坐标、面积使用 `Quantity<V>`；无量纲值（利用率、比值、评分）使用裸 `V`。
3. `FltX` 只出现在 `bpp2d-domain-layer-assignment-context`（求解器边界）与应用层接线。
4. 泛型代码不得调用 `toDouble()`：计数与缩放通过量值运算表达。

## Gurobi 接入

Gurobi 集成测试位于 `bpp2d-application/src/gurobi-test`，仅在 `gurobi-cg-test` profile 下运行：

```powershell
$lib = "$env:GUROBI_HOME\bin"
mvn -s D:\environment\apache-maven-3.9.12\conf\settings.xml `
    -pl ospf-kotlin-framework-bpp2d/bpp2d-application -am test `
    -Pgurobi-cg-test -Dtest=GurobiColumnGeneration2DTest `
    -Dsurefire.failIfNoSpecifiedTests=false "-DargLine=-Djava.library.path=$lib"
```

未安装 Gurobi 时该 profile 不激活，默认构建不受影响。

## 本地校验

```powershell
mvn -s D:\environment\apache-maven-3.9.12\conf\settings.xml -f ospf-kotlin-framework-bpp2d/pom.xml test
```
