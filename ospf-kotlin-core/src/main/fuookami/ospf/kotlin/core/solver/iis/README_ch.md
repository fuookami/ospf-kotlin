# solver/iis — 不可行子系统诊断包

:us: [English](README.md) | :cn: 简体中文

## 概述

`iis` 子包实现了 OSPF 框架的**不可约不可行子系统（Irreducible Infeasible Subsystem, IIS）**分析。当模型不可行时，IIS 识别导致不可行的最小约束和边界子集，帮助用户诊断和解决模型问题。

## 包结构

```
iis/
├── IISComputingStatus.kt  # IIS 计算状态枚举
├── IISConfig.kt           # IIS 配置
├── Linear.kt              # 线性 IIS 模型视图
└── Quadratic.kt           # 二次 IIS 模型视图
```

## 核心概念

### IISConfig (`IISConfig.kt`)

IIS 计算配置：
- 启用/禁用 IIS 分析
- 控制计算参数和超时

### IISComputingStatus (`IISComputingStatus.kt`)

IIS 计算状态枚举：
- 计算进度跟踪
- 成功/失败报告

### 线性 IIS (`Linear.kt`)

线性不可行子系统模型视图（`BasicLinearTriadModelView`），识别导致线性模型不可行的最小约束子集。

### 二次 IIS (`Quadratic.kt`)

二次不可行子系统模型视图（`QuadraticTetradModelView`），识别导致二次模型不可行的最小约束子集。

## 与其他包的关系

- **solver/output** — IIS 结果嵌入在 `LinearInfeasibleSolverOutput` 和 `QuadraticInfeasibleSolverOutput` 中
- **model/intermediate** — IIS 视图引用 `LinearTriadModelView` 和 `QuadraticTetradModelView`