# 机组 领域模型

:us: [English](domain-model.md) | :cn: 简体中文

[toc]

## 一、概述

管理航班恢复调度系统中机组成员、排班和中转时间的领域模型，为批次生成提供可行性和成本计算所需的机组数据。

### 1. 依赖上下文

1. **task**（航班任务）
2. **infrastructure**（基础设施）

---

## 二、概念/实体

### 1. 机组成员（CrewMember）

具有身份信息的机组人员，分为飞行员和非飞行员。

**$\text{type}_{m}$** ：机组成员 $m$ 的类型（Operator、Attendant、Other）。
**$\text{workerNo}_{m}$** ：机组成员 $m$ 的工号。
**$\text{name}_{m}$** ：机组成员 $m$ 的姓名。
**$\text{nationality}_{m}$** ：机组成员 $m$ 的国籍。

### 2. 飞行员（CrewPilotMember）

作为飞行员的机组成员，将身份字段委托给底层 `Pilot`。

**$\text{rank}_{p}$** ：飞行员 $p$ 的职级（`PilotRank`）。
**$\text{pilot}_{p}$** ：飞行员 $p$ 的底层 `Pilot` 对象。

### 3. 机组（Crew）

分配给航班任务的机组，由飞行员和非飞行员成员组成。

**$\text{flight}_{c}$** ：机组 $c$ 分配的航班任务。
**$\text{members}_{c}$** ：机组 $c$ 的成员列表。

### 4. 机组排班（CrewSchedule）

机组成员的排班，将航班任务映射到其分配的职级。

**$\text{crewMan}_{s}$** ：排班 $s$ 的机组成员。
**$\text{schedules}_{s}$** ：排班 $s$ 的航班任务到职级的映射。

### 5. 中转时间（TransitTime）

基于飞机和机场关系的中转时间场景及其所需时长。

**$\text{scene}_{t}$** ：中转时间 $t$ 的场景。
**$\text{duration}_{t}$** ：中转时间 $t$ 的所需时长。

---

## 三、变量

> 机组上下文不直接定义优化变量，其数据作为批次生成的输入参数。

---

## 四、谓词

### 1. 机组成员类型

**isPilot** ：机组成员 $m$ 是飞行员（`CrewPilotMember`）。
**isNotPilot** ：机组成员 $m$ 是非飞行员（`CrewNotPilotMember`）。

### 2. 中转时间场景

**isSameAircraft** ：前后任务使用同一飞机。
**isDomesticNotSameAircraft** ：前后任务不同飞机但机场为国内类型。
**isInternationalNotSameAircraft** ：前后任务不同飞机且机场为国际类型。

---

## 五、集合

### 1. 机组

**$C$** ：所有机组全集。

**$C_{i}$** ：分配给航班任务 $i$ 的机组子集，$\forall i \in I$ 。

### 2. 机组成员

**$M$** ：所有机组成员全集。

**$M^{P}$** ：飞行员成员子集。
**$M^{N}$** ：非飞行员成员子集。
**$M_{c}$** ：机组 $c$ 的成员子集，$\forall c \in C$ 。

### 3. 排班

**$S$** ：所有机组排班全集。

**$S_{m}$** ：机组成员 $m$ 的排班子集，$\forall m \in M$ 。

### 4. 中转时间

**$T$** ：所有中转时间条目全集。

---

## 六、中间值

### 1. 中转时间查找

**描述**：给定连续航班任务的中转时间。

$$
\text{transitTime}(i_{prev}, i_{succ}) = \begin{cases}
T_{\text{SameAircraft}},& \text{same aircraft} \\ \\
T_{\text{Domestic}},& \text{different aircraft, domestic airport} \\ \\
T_{\text{International}},& \text{different aircraft, international airport} \\ \\
\text{null},& \text{otherwise}
\end{cases}
$$

---

## 七、断言

### 1. 机组成员完整性

**描述**：每个机组必须包含至少一名成员。

$$
\forall c \in C \; (|M_{c}| \geq 1)
$$

### 2. 排班连续性

**描述**：机组成员的排班中，连续航班任务的到达机场必须与下一任务的出发机场一致。

$$
\forall s \in S, \forall (i_{prev}, i_{succ}) \in \text{schedules}_{s} \; (i_{prev}.\text{arr} = i_{succ}.\text{dep})
$$

---

## 八、约束

> 机组上下文不直接定义优化约束，其数据通过 `ConnectionTimeCalculator` 和 `RuleChecker` 间接影响批次生成的可行性判断。

---

## 九、目标函数

> 机组上下文不直接定义目标函数。

---

## 十、算法引用

> 当前上下文无独立算法引用。

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 机组 | $C$ | Crew | 分配给航班任务的机组人员集合 |
| 机组成员 | $M$ | Crew Member | 具有身份信息的机组人员 |
| 飞行员 | $M^{P}$ | Pilot | 操作飞机的机组成员 |
| 排班 | $S$ | Schedule | 机组成员的航班任务分配 |
| 中转时间 | $T$ | Transit Time | 连续航班间的必要间隔时间 |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 飞行员/非飞行员分离建模 | 统一 CrewMember 类型 | 职级体系不同，分离更清晰 | - |
| 中转时间按场景枚举 | 连续函数建模 | 场景有限，枚举更直观 | - |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1 | 初始实现 | 基础机组域建模 |
