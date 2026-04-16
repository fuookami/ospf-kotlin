# C0-3: core 主计算路径 `.cells` 调用清单

**生成日期**: 2026-04-16
**扫描范围**: ospf-kotlin-core/src/main

---

## 概览

本清单记录主计算路径中 `.cells` 属性/方法调用位置，用于后续清退评估。

### 统计摘要

| 类别 | 数量 | 文件 |
|------|------|------|
| 主路径调用 | 6 | LinearTriadModel.kt, QuadraticTetradModel.kt |
| 字符串引用 | 2 | SolveValueValidation.kt |
| 扩展属性定义 | 1 | QuadraticMonomial.kt |

---

## 详细清单

### 1. 主计算路径调用 (需清退)

#### LinearTriadModel.kt (线性三元模型)

**路径**: `intermediate_model/LinearTriadModel.kt`

| 行号 | 代码 | 用途 |
|------|------|------|
| 694 | `for (cell in subObject.cells)` | 遍历子目标单元格 |
| 704 | `for (cell in subObject.cells)` | 遍历子目标单元格 |

**上下文**: `toDual()` / `toFarkasDual()` 实现，遍历子目标的单元格

**清退策略**: 改用 `MathLinearPolynomial.cells()` 或内部 API

---

#### QuadraticTetradModel.kt (二次四元模型)

**路径**: `intermediate_model/QuadraticTetradModel.kt`

| 行号 | 代码 | 用途 |
|------|------|------|
| 810 | `for (cell in subObject.cells)` | 遍历子目标单元格 |
| 832 | `for (cell in subObject.cells)` | 遍历子目标单元格 |

**上下文**: 模型转换实现，遍历子目标的单元格

**清退策略**: 改用 `MathQuadraticPolynomial.cells()` 或内部 API

---

### 2. 字符串引用 (不影响)

#### SolveValueValidation.kt (求解值验证)

**路径**: `solver/value/SolveValueValidation.kt`

| 行号 | 代码 | 用途 |
|------|------|------|
| 112 | `fieldName = "linear.objective.cells[$index].coefficient"` | 错误消息字符串 |
| 191 | `fieldName = "quadratic.objective.cells[$index].coefficient"` | 错误消息字符串 |

**判定**: ✅ **保留** - 这是字符串引用，非实际 `.cells` 调用

---

### 3. 扩展属性定义 (内部实现)

#### QuadraticMonomial.kt (二次单项式)

**路径**: `intermediate_model/monomial/QuadraticMonomial.kt`

| 行号 | 代码 | 用途 |
|------|------|------|
| 426 | `val QuadraticMonomialSymbolUnit.cells` | 扩展属性定义 |

**判定**: ⚠️ **评估** - 这是扩展属性定义，需评估是否保留

---

## 清退优先级

### P0: 主路径调用

1. **LinearTriadModel.kt** 行 694, 704
2. **QuadraticTetradModel.kt** 行 810, 832

这些调用位于主计算链路（dual/farkas 转换），需改为使用 math.symbol 类型或内部 API。

### P1: 评估保留

1. **QuadraticMonomial.kt** 扩展属性定义 - 需确认是否为内部实现

### P2: 无需处理

1. **SolveValueValidation.kt** 字符串引用 - 仅错误消息

---

## 清退策略

### 方案 A: 改用 math.symbol 类型

```kotlin
// 旧代码
for (cell in subObject.cells) { ... }

// 新代码
for (cell in subObject.mathPolynomial.cells()) { ... }
```

### 方案 B: 提供内部 API

在 `IntermediateModel` 提供内部访问方法，隐藏 `.cells` 直接调用。

---

## 验收命令

```bash
# 扫描主路径 .cells 调用
grep -r "\.cells" ospf-kotlin-core/src/main --include="*.kt" \
  | grep -v "fieldName" | grep -v "val.*cells"
```

---

## 下一步

进入 **P1-3**: `.cells` 主路径清退