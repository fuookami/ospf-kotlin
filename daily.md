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
4. ✅ 修复 utils 模块中不依赖 math 的文件:
   - `Code.kt`: UInt8/UByte 替换
   - `Map.kt`: UInt64 → ULong
   - `System.kt`: Flt64 → Double
   - `Indexed.kt`: UInt64 → ULong
   - `Common.kt`: UInt64 → ULong

**待修复问题** (1541 编译错误):

主要错误类型:
- `RealNumber` 未导入 (86 处)
- `errors` 未找到 (24 处)
- `neq` 未找到 (12 处)
- `error` 未找到 (12 处)
- `sumOf` 等函数问题

**根本原因**:
1. core 模块部分文件缺少必要的 import 语句
2. 部分扩展函数需要从 math.functional 导入
3. 部分类型推断需要显式导入

---

## 执行进度

1. ✅ 阶段 1: 创建模块结构
2. ✅ 阶段 2: operator → math
3. ✅ 阶段 3: multi_array → ospf-kotlin-multiarray
4. ✅ 阶段 4: math → ospf-kotlin-math
5. ✅ 阶段 5: physics → ospf-kotlin-quantities
6. 🔄 阶段 6: 清理验证 (core 模块编译错误待修复)

### 模块状态

| 模块 | 编译 | 测试 |
|------|------|------|
| ospf-kotlin-utils | ✅ | ✅ |
| ospf-kotlin-multiarray | ✅ | ✅ |
| ospf-kotlin-math | ✅ | ✅ |
| ospf-kotlin-quantities | ✅ | ✅ |
| ospf-kotlin-core | ❌ | - |
| ospf-kotlin-framework | ❌ | - |

### 本次工作记录 (2026-04-03)

#### 完成内容

1. **multiarray 模块重构**
   - 移除对 math 的依赖
   - 所有 UInt64 替换为 ULong
   - 更新 Shape.kt, MultiArray.kt, MultiArrayView.kt, Map.kt
   - 删除 FastSum.kt (移到 math 模块)

2. **math 模块扩展**
   - 创建 `multiarray/MultiArrayExtensions.kt`:
     - AbstractMultiArray 的 UInt64 get 扩展
     - MutableMultiArray 的 UInt64 set 扩展
     - MultiArrayView/MappedMultiArrayView 的 UInt64 get 扩展
     - Shape 的 UInt64 属性扩展
     - Shape 工厂方法扩展
     - Map 的 UInt64 扩展
   - 创建 `multiarray/FastSum.kt`
   - 从 utils.operator 复制 operator 文件到 math.operator

3. **core 模块导入更新**
   - 批量替换 `utils.math` → `math`
   - 批量替换 `utils.multi_array` → `multiarray`
   - 批量替换 `utils.operator` → `math.operator`

4. **utils 模块清理**
   - 移除 math/multi_array/physics/operator 目录
   - 修复不依赖 math 的文件使用标准 Kotlin 类型

#### 下一步工作

1. 修复 core 模块编译错误:
   - 添加缺失的 RealNumber 导入
   - 添加 math.functional 的 sum 等函数导入
   - 添加 Eq/neq 等扩展函数导入

2. 运行全量测试

3. 更新 framework 等其他模块

---

*创建时间: 2026-04-03*
*更新时间: 2026-04-03*