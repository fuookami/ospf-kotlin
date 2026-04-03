# 模块迁移重构实施计划

## 需求概述

将 ospf-kotlin-utils 中的四个包拆分为独立模块：

1. `operator` → 合并到 `math` 包
2. `math` → 独立 `ospf-kotlin-math` 模块
3. `multi_array` → 独立 `ospf-kotlin-multiarray` 模块
4. `physics` → 独立 `ospf-kotlin-quantities` 模块

## 目标模块依赖结构

```
ospf-kotlin-utils (基础模块，含 concept、functional、error 等)
    ↑
ospf-kotlin-multiarray (依赖 utils，使用 ULong、Indexed 等，不依赖 math)
    ↑
ospf-kotlin-math (依赖 multiarray 和 utils，含 MultiArrayExtensions.kt 扩展)
    ↑
ospf-kotlin-quantities (依赖 math)
    ↑
ospf-kotlin-core/framework (依赖上述模块)
```

## 关键设计决策

### 依赖关系说明

- **ospf-kotlin-utils**: 保留 concept、functional、error、config、context、parallel、meta_programming、serialization 等基础包
- **ospf-kotlin-multiarray**: 依赖 utils，使用标准 Kotlin ULong 替代 UInt64，不依赖 math
- **ospf-kotlin-math**: 依赖 multiarray 和 utils，提供数学代数和符号系统，含 UInt64 扩展方法
- **ospf-kotlin-quantities**: 依赖 math，提供物理量系统

### Operator 包拆分策略

**关键决策**: Operator 接口按功能拆分到两个包：

| 包位置 | 接口 | 说明 |
|--------|------|------|
| `utils.math.operator` | `Eq`, `Neg`, `Ord`, `Order` | 比较相关，utils.functional 依赖 |
| `math.operator` | `Plus`, `Minus`, `Times`, `Div`, `Abs`, `Pow`, `Exp`, `Log`, `RangeTo`, `Reciprocal`, `Rem`, `Trigonometry`, `Cross`, `Dec`, `IntDiv` | 数学运算 |

**原因**: `Predicate.kt` 中的 `ThreeWayComparator` 依赖 `Order` 类型，若 Order 在 math 模块会产生循环依赖。

---

## 实施阶段

### 阶段 1: 创建新模块目录结构 ✅

**状态**: 已完成

---

### 阶段 2: operator → math 包内迁移 ✅

**状态**: 已完成

---

### 阶段 3: multi_array → ospf-kotlin-multiarray ✅

**状态**: 已完成

**关键变更**:
- 所有 `UInt64` 替换为标准 Kotlin `ULong`
- 所有 `UInt64` 扩展方法移到 `ospf-kotlin-math` 的 `MultiArrayExtensions.kt`
- FastSum.kt 移到 `ospf-kotlin-math/multiarray/` 目录
- Map.kt 保留在 multiarray，使用 ULong

---

### 阶段 4: math → ospf-kotlin-math ✅

**状态**: 已完成

**关键变更**:
- operator 包从 utils.operator 复制到 math.operator
- 创建 `math/multiarray/MultiArrayExtensions.kt` 提供 UInt64 扩展
- 创建 `math/multiarray/FastSum.kt` 提供高性能求和

---

### 阶段 5: physics → ospf-kotlin-quantities ✅

**状态**: 已完成

---

### 阶段 6: 清理与验证 🔄

**状态**: 进行中

**已完成工作**:
1. ✅ 更新 ospf-kotlin-dependencies (BOM)
2. ✅ 更新 ospf-kotlin-starters
3. ✅ 批量更新 ospf-kotlin-core 导入路径:
   - `fuookami.ospf.kotlin.utils.math` → `fuookami.ospf.kotlin.math`
   - `fuookami.ospf.kotlin.utils.multi_array` → `fuookami.ospf.kotlin.multiarray`
   - `fuookami.ospf.kotlin.utils.operator` → `fuookami.ospf.kotlin.math.operator`
4. ✅ 修复 utils 模块中不依赖 math 的文件
5. ✅ Operator 包拆分：比较类保留在 utils.math.operator，数学类迁移到 math.operator
6. ✅ 修复 math 模块编译错误 (Numbers.kt, Bound.kt, ValueRange.kt, ValueWrapper.kt 等)
7. ✅ 修复 quantities 模块编译错误 (Quantity.kt)
8. ✅ 修复 core 模块大部分编译错误 (LinearPolynomial.kt, QuadraticPolynomial.kt, Monomial.kt 等)

**剩余问题** (core 模块 22 个编译错误):

仅剩 `MetaModel.kt` 文件有错误，位于 `exportOpm` 函数：

```
E:\workspace\ospf-kotlin\ospf-kotlin-core\src\main\fuookami\ospf\kotlin\core\frontend\model\mechanism\MetaModel.kt
- 行 485: 'when' expression must be exhaustive
- 行 485: Overload resolution ambiguity for register function
- 行 486-495: Ok/Failed/Fatal 类型参数问题
- 行 503: Return type mismatch
- 行 503: Overload resolution ambiguity for register function
- 行 504-513: Ok/Failed/Fatal 类型参数问题
```

**问题分析**:
1. `AbstractMutableTokenTable` 是 sealed interface，有两个实现：`MutableTokenTable` 和 `ConcurrentMutableTokenTable`
2. `when` 表达式缺少 `else` 分支
3. `register` 函数有两个重载（同步和 suspend），编译器无法选择
4. `Ok`, `Failed`, `Fatal` 需要显式类型参数

---

## 执行进度

1. ✅ 阶段 1: 创建模块结构
2. ✅ 阶段 2: operator → math
3. ✅ 阶段 3: multi_array → ospf-kotlin-multiarray
4. ✅ 阶段 4: math → ospf-kotlin-math
5. ✅ 阶段 5: physics → ospf-kotlin-quantities
6. 🔄 阶段 6: 清理验证 (core 模块 MetaModel.kt 剩余 22 个错误)

### 模块状态

| 模块 | 编译 | 测试 |
|------|------|------|
| ospf-kotlin-utils | ✅ | 待验证 |
| ospf-kotlin-multiarray | ✅ | 待验证 |
| ospf-kotlin-math | ✅ | 待验证 |
| ospf-kotlin-quantities | ✅ | 待验证 |
| ospf-kotlin-core | ❌ (22 errors) | - |
| ospf-kotlin-framework | ❌ | - |

### 本次工作记录 (2026-04-04)

#### 完成内容

1. **Operator 包拆分**
   - 保留 `utils.math.operator`: Eq, Neg, Ord, Order (比较相关)
   - 迁移到 `math.operator`: Plus, Minus, Times, Div, Abs, Pow, Exp, Log, RangeTo, Reciprocal, Rem, Trigonometry, Cross, Dec, IntDiv
   - 删除 `math.operator` 中的 Order.kt 避免冲突
   - 更新 `Predicate.kt` 导入 `utils.math.operator.Order`

2. **math 模块修复**
   - Numbers.kt: 添加所有 operator 接口导入
   - NumericUInteger.kt: 添加 Neg, Minus, Abs 导入
   - Bound.kt, ValueRange.kt, ValueWrapper.kt: 添加 Plus, Minus, Times, Div 导入

3. **quantities 模块修复**
   - Quantity.kt: 添加 Times, Div 导入

4. **core 模块修复**
   - LinearPolynomial.kt: 添加 Plus, Minus, Times, Div, Eq 导入
   - QuadraticPolynomial.kt: 添加 Plus, Minus, Times, Div, Eq 导入
   - Monomial.kt: 添加 operator.* 导入
   - Polynomial.kt: 添加 operator.* 导入
   - LinearMonomial.kt: 添加 operator.* 导入
   - QuadraticMonomial.kt: 添加 operator.* 导入，修复 Eq 导入

#### 下一步工作

1. **修复 MetaModel.kt 编译错误**:
   - 添加 `else` 分支到 `when` 表达式
   - 解决 `register` 函数重载歧义（使用显式调用或类型转换）
   - 为 `Ok`, `Failed`, `Fatal` 添加显式类型参数

2. **编译通过后**:
   - 运行全量测试
   - 更新 framework 模块

#### 修复指南

**MetaModel.kt 修复示例**:

```kotlin
// 原代码 (行 482-517)
when (val result = when (tokens) {
    is MutableTokenTable -> { ... }
    is ConcurrentMutableTokenTable -> { ... }
    // 缺少 else 分支
}) { ... }

// 修复方案
when (val result = when (tokens) {
    is MutableTokenTable -> { ... }
    is ConcurrentMutableTokenTable -> { ... }
    else -> throw IllegalStateException("Unknown token table type")
}) { ... }

// Ok/Failed/Fatal 类型参数修复
// 原: Ok(temp)
// 改: Ok<AbstractTokenTable, Error>(temp)

// register 函数歧义修复
// 需要显式指定调用哪个重载，或将 suspend 版本改名
```

---

*创建时间: 2026-04-03*
*更新时间: 2026-04-04*