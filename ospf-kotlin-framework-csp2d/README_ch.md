# ospf-kotlin-framework-csp2d（中文）

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-framework-csp2d` 是 OSPF Kotlin 的二维套裁框架。它沿用
[ospf-kotlin-framework-csp1d](../ospf-kotlin-framework-csp1d/README_ch.md) 的 CSP 语义（产品、需求、裁切图案、
裁切方案、良率、余料、板长分配、列生成），同时采用
[ospf-kotlin-framework-bpp2d](../ospf-kotlin-framework-bpp2d/README_ch.md) 的泛型分层纪律：领域层泛型于 `V`，
求解器数值类型只出现在建模边界与应用层。

二维几何与 bpp2d 共用 [ospf-kotlin-framework-geometry2d](../ospf-kotlin-framework-geometry2d/README_ch.md)。

## 范围

两种供给模式，一套模型：

| 模式 | 问题类别 | 物料模型 | 决策变量 |
| --- | --- | --- | --- |
| 卷材套裁 | 条带排样（幅宽固定，长度待定） | `width` 固定、`length = null` | 消耗长度与排样 |
| 定尺板材 | 二维装箱 | `width × height` 固定 | 板材张数与排样 |

裁切工艺可配置，且与模式正交：

| 工艺 | 含义 | 设备 |
| --- | --- | --- |
| `Freeform` | 任意排样，只需放置坐标 | 激光、水刀、CNC |
| `GuillotineTwoStage` | 先切条再切件，输出切分树 | 剪板机、开料锯 |
| `GuillotineThreeStage` | 切条 → 切块 → 切件，输出切分树 | 剪板机、开料锯 |

异形件（非矩形件）明确不在范围内。

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `csp2d-infrastructure` | 裁切区域、刀缝、断头台刀线与切分树、裁切工艺、错误类型与渲染 DTO |
| `csp2d-domain-material-context` | 产品、需求、物料、设备、副产物、裁切图案与裁切方案 |
| `csp2d-domain-pattern-generation-context` | 裁切图案生成器（任意排样与断头台）与检验数定价 |
| `csp2d-domain-produce-context` | 主问题变量、需求与产能管线、建模扩展 |
| `csp2d-domain-yield-context` | 欠产与超产分析、良率约束与目标 |
| `csp2d-domain-wasting-minimization-context` | 余料面积分析与余料惩罚目标 |
| `csp2d-domain-sheet-assignment-context` | 卷材消耗长度推导；定尺板材的张数与尺寸选择 |
| `csp2d-application` | 问题定义、MILP 与列生成求解、恢复、结果富化 |

## 当前状态

Phase C2-0 已完成：八模块骨架就绪，`csp2d-infrastructure` 提供裁切区域模型、带刀缝的区域切分、
k 级断头台切分树与校验、裁切工艺配置、错误类型与渲染 DTO。后续阶段补齐物料与图案模型、图案生成、
建模上下文、应用层以及数据集与 Gurobi 集成。

## 切分树语义

断头台方案不是一组坐标，而是一棵**切分树**：内部结点是贯穿刀，叶子是最终料块。刀线的级数是它的切割层号：
根刀线为第 1 级，其下的刀线必须恰为第 2 级，依此类推。因此 2 级工艺恰好等于「先切条、再切件」，
3 级工艺等于「切条 → 切块 → 切件」。`CutTree2.validate` 会拒绝这些不合法的树：区域切分不一致（含刀缝带）、
超出工艺级数上限、子刀线层号不正确。

## 泛型数值边界

1. 领域层泛型于 `V : FloatingNumber<V>`。
2. 坐标、尺寸与面积使用 `Quantity<V>`；无量纲值使用裸 `V`。
3. 求解器数值类型只出现在建模边界与应用层。
4. 泛型代码不得调用 `toDouble()`。

## 相关文档

- [daily.md](daily.md) 记录实施计划与进展。
- [ospf-kotlin-framework-csp1d](../ospf-kotlin-framework-csp1d/README_ch.md) 是成熟的 1D 套裁参考。
- [ospf-kotlin-framework-geometry2d](../ospf-kotlin-framework-geometry2d/README_ch.md) 提供共享二维几何。

## 本地验证

```powershell
mvn -B -ntp -f ospf-kotlin-framework-csp2d/pom.xml test
```
