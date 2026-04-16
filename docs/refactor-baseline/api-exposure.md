# C0-1: core 对外 API 暴露符号类型清单

**生成日期**: 2026-04-16
**扫描范围**: ospf-kotlin-core/src/main

---

## 概览

本清单记录所有 `public` API 签名中使用 `AbstractLinearPolynomial` 或 `AbstractQuadraticPolynomial` 作为参数/返回类型的位置。

### 统计摘要

| 类型 | 出现次数 | 涉及文件数 |
|------|----------|------------|
| AbstractLinearPolynomial | 58 | 7 |
| AbstractQuadraticPolynomial | 18 | 5 |

---

## 详细清单

### 1. Model.kt (核心入口)

**路径**: `model/Model.kt`

| 函数名 | 类型 | 签名 | 行号 |
|--------|------|------|------|
| `addConstraint` | 参数 | `constraint: AbstractLinearPolynomial<*>` | 227 |
| `addObjective` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 343 |
| `addMipConstraint` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 434 |
| `setMipObjective` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 522 |
| `setMipObjectiveFromSum` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 539 |
| `addConstraint` | 参数 | `constraint: AbstractQuadraticPolynomial<*>` | 652 |
| `addObjective` | 参数 | `polynomial: AbstractQuadraticPolynomial<*>` | 755 |
| `addMipConstraint` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 846 |
| `addMipConstraint` | 参数 | `polynomial: AbstractQuadraticPolynomial<*>` | 908 |
| `setMipObjective` | 参数 | `polynomial: AbstractQuadraticPolynomial<*>` | 996 |
| `setMipObjectiveFromSum` | 参数 | `polynomial: AbstractQuadraticPolynomial<*>` | 1013 |

**影响**: ⚠️ **高** - 这是主入口，所有用户调用都经过这里

---

### 2. MetaModel.kt (元模型入口)

**路径**: `intermediate_model/MetaModel.kt`

| 函数名 | 类型 | 签名 | 行号 |
|--------|------|------|------|
| `addLinearObjective` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 1012 |
| `addQuadraticObjective` | 参数 | `polynomial: AbstractQuadraticPolynomial<*>` | 1271 |

**影响**: ⚠️ **高** - 元模型构建主入口

---

### 3. MetaConstraint.kt (约束构建)

**路径**: `intermediate_model/MetaConstraint.kt`

| 函数名 | 类型 | 签名 | 行号 |
|--------|------|------|------|
| `addLinearConstraint` | 参数 | `constraint: AbstractLinearPolynomial<*>` | 78 |
| `addLinearObjective` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 182 |
| `addQuadraticConstraint` | 参数 | `constraint: AbstractQuadraticPolynomial<*>` | 217 |
| `addQuadraticObjective` | 参数 | `polynomial: AbstractQuadraticPolynomial<*>` | 297 |

**影响**: ⚠️ **高** - 约束与目标构建入口

---

### 4. MathInequalityDsl.kt (DSL 扩展函数)

**路径**: `intermediate_model/MathInequalityDsl.kt`

#### AbstractLinearPolynomial DSL (行 573-619)

| 扩展函数 | 参数类型 | 行号范围 |
|----------|----------|----------|
| `eq/le/ge/lt/gt/ne` | `AbstractLinearPolynomial<*>.eq(rhs: Boolean)` | 575-585 |
| `leq/geq/neq/ls/gr` | `AbstractLinearPolynomial<*>.leq(rhs: Boolean)` | 587-591 |
| `eq/le/ge/lt/gt/ne` | `AbstractLinearPolynomial<*>.eq(rhs: Flt64)` | 593-603 |
| `leq/geq/neq` | `AbstractLinearPolynomial<*>.leq(rhs: Flt64)` | 605-607 |
| `eq/le/ge/ne` | `AbstractLinearPolynomial<*>.eq(rhs: UtilsLinearPolynomial<Flt64>)` | 609-619 |

#### AbstractQuadraticPolynomial DSL (行 621-665)

| 扩展函数 | 参数类型 | 行号范围 |
|----------|----------|----------|
| `eq/le/ge/lt/gt/ne` | `AbstractQuadraticPolynomial<*>.eq(rhs: Boolean)` | 623-633 |
| `leq/geq/neq` | `AbstractQuadraticPolynomial<*>.leq(rhs: Boolean)` | 635-637 |
| `eq/le/ge/lt/gt/ne` | `AbstractQuadraticPolynomial<*>.eq(rhs: Flt64)` | 639-649 |
| `leq/geq/neq` | `AbstractQuadraticPolynomial<*>.leq(rhs: Flt64)` | 651-653 |
| `eq/le/ge/ne` | `AbstractQuadraticPolynomial<*>.eq(rhs: UtilsQuadraticPolynomial<Flt64>)` | 655-665 |

**影响**: ⚠️ **中** - DSL 便利层，用户高频使用

**备注**: 这些是 **兼容层扩展函数**，内部委托给 `MathLinearInequality/MathQuadraticInequality`，可作为过渡保留。

---

### 5. IntermediateSymbol.kt (中间符号)

**路径**: `intermediate_symbol/IntermediateSymbol.kt`

| 函数名 | 类型 | 签名 | 行号 |
|--------|------|------|------|
| `LinearIntermediateSymbolImpl` 构造 | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 536, 689 |
| `QuadraticIntermediateSymbolImpl` 构造 | 参数 | `polynomial: AbstractQuadraticPolynomial<*>` | 839, 991 |

**影响**: ⚠️ **中** - 符号注册内部实现

---

### 6. function/Bridge.kt (桥接函数)

**路径**: `intermediate_symbol/function/Bridge.kt`

| 函数名 | 类型 | 签名 | 行号 |
|--------|------|------|------|
| `asMathLinearPolynomial` | 接收者 | `AbstractLinearPolynomial<*>.asMathLinearPolynomial()` | 18 |

**影响**: ✅ **低** - 这是转换函数，用于将 core 类型转换为 math 类型，属于兼容层

---

### 7. function/And.kt (逻辑函数)

**路径**: `intermediate_symbol/function/And.kt`

| 函数名 | 类型 | 签名 | 行号 |
|--------|------|------|------|
| `fromPolynomials` | 参数 | `polynomials: List<AbstractLinearPolynomial<*>>` | 100, 234 |

**影响**: ⚠️ **中** - 兼容工厂方法

---

### 8. function/Binaryzation.kt (二值化函数)

**路径**: `intermediate_symbol/function/Binaryzation.kt`

| 函数名 | 类型 | 签名 | 行号 |
|--------|------|------|------|
| `fromPolynomial` | 参数 | `x: AbstractLinearPolynomial<*>` | 175 |

**影响**: ⚠️ **中** - 兼容工厂方法

---

### 9. SymbolCombination.kt (符号组合)

**路径**: `intermediate_symbol/SymbolCombination.kt`

| 函数名 | 类型 | 签名 | 行号 |
|--------|------|------|------|
| 多个内部函数 | 参数类型推断 | `ctor: (T) -> AbstractLinearPolynomial<*>` | 365, 404, 453, 514, 544, 563 |

**影响**: ✅ **低** - 内部实现，lambda 类型推断

---

### 10. Polynomial.kt (多项式定义)

**路径**: `intermediate_model/Polynomial.kt`

| 类型/函数 | 类型 | 签名 | 行号 |
|-----------|------|------|------|
| `toLinearPolynomials` | 返回 | `List<AbstractLinearPolynomial<*>>` | 83 |
| `toQuadraticPolynomials` | 返回 | `List<AbstractQuadraticPolynomial<*>>` | 105 |
| `sumOf` | 参数 | `polynomials: List<AbstractLinearPolynomial<*>>` | 405 |
| `sumOf` | 参数 | `polynomials: List<AbstractQuadraticPolynomial<*>>` | 457 |
| `LinearPolynomial.invoke` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 631 |
| `MutableLinearPolynomial.invoke` | 参数 | `polynomial: AbstractLinearPolynomial<*>` | 795 |

**影响**: ⚠️ **中** - 类型定义与工厂方法

---

## 清退策略

### P0 优先清退 (主入口)

1. **Model.kt**: 所有 `addConstraint/addObjective` 签名需改为接收 `MathLinearPolynomial/MathQuadraticPolynomial`
2. **MetaModel.kt**: `addLinearObjective/addQuadraticObjective` 签名需改为 math 类型
3. **MetaConstraint.kt**: 所有约束/目标添加函数签名需改为 math 类型

### P1 保留为兼容层 (DSL + 桥接)

1. **MathInequalityDsl.kt**: 扩展函数保留，添加 `@Deprecated` + `ReplaceWith`
2. **Bridge.kt**: 转换函数保留作为过渡
3. **function/And.kt, Binaryzation.kt**: 兼容工厂方法保留

### P2 内部实现 (低优先)

1. **IntermediateSymbol.kt**: 内部构造参数
2. **SymbolCombination.kt**: 内部 lambda 类型推断
3. **Polynomial.kt**: 类型定义与工厂（最后删除）

---

## 验收命令

```bash
# 扫描公共 API 中的 Abstract*Polynomial
grep -r "public.*AbstractLinearPolynomial\|public.*AbstractQuadraticPolynomial" \
  ospf-kotlin-core/src/main --include="*.kt"
```

---

## 下一步

进入 **C1 阶段**: API 清退与主链路切流