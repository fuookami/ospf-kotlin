# API 迁移映射表 (C1 交付物)

**生成日期**: 2026-04-16
**版本**: E7 迁移指南

---

## 概览

本文档记录 `AbstractLinearPolynomial` / `AbstractQuadraticPolynomial` 相关 API 到 `math.symbol` 类型的新映射关系。

---

## 1. MetaModel.kt 迁移映射

### addObject (Linear)

| 旧 API | 新 API |
|--------|--------|
| `addObject(category, polynomial: AbstractLinearPolynomial<*>, name, displayName)` | `addObject(category, LinearFlattenData(polynomial.toLinearPolynomial().monomials, polynomial.toLinearPolynomial().constant), name ?: "", displayName)` |

**导入**:
```kotlin
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenData
```

### addObject (Quadratic)

| 旧 API | 新 API |
|--------|--------|
| `addObject(category, polynomial: AbstractQuadraticPolynomial<*>, name, displayName)` | `addObject(category, QuadraticFlattenData(polynomial.toQuadraticPolynomial().monomials, polynomial.toQuadraticPolynomial().constant), name ?: "", displayName)` |

**导入**:
```kotlin
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenData
```

---

## 2. MetaConstraint.kt 迁移映射

### addConstraint (Linear)

| 旧 API | 新 API |
|--------|--------|
| `addConstraint(constraint: AbstractLinearPolynomial<*>, lazy, name, displayName, args, withRangeSet)` | `addConstraint(constraint.toLinearPolynomial() eq Flt64.one, lazy, name, displayName, args, withRangeSet)` |

**导入**:
```kotlin
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.algebra.number.Flt64
```

### partition (Linear)

| 旧 API | 新 API |
|--------|--------|
| `partition(polynomial: AbstractLinearPolynomial<*>, lazy, name, displayName, args)` | `addConstraint(polynomial.toLinearPolynomial() eq Flt64.one, lazy, name, displayName, args)` |

**导入**:
```kotlin
import fuookami.ospf.kotlin.math.algebra.number.Flt64
```

### addConstraint (Quadratic)

| 旧 API | 新 API |
|--------|--------|
| `addConstraint(constraint: AbstractQuadraticPolynomial<*>, lazy, name, displayName, args, withRangeSet)` | `addConstraint(constraint.toQuadraticPolynomial() eq Flt64.one, lazy, name, displayName, args, withRangeSet)` |

**导入**:
```kotlin
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.algebra.number.Flt64
```

### partition (Quadratic)

| 旧 API | 新 API |
|--------|--------|
| `partition(polynomial: AbstractQuadraticPolynomial<*>, lazy, name, displayName, args)` | `addConstraint(polynomial.toQuadraticPolynomial() eq Flt64.one, lazy, name, displayName, args)` |

**导入**:
```kotlin
import fuookami.ospf.kotlin.math.algebra.number.Flt64
```

---

## 3. MathInequalityDsl.kt 迁移映射

### AbstractLinearPolynomial DSL

#### Flt64 RHS (有 ReplaceWith)

| 旧 API | 新 API |
|--------|--------|
| `polynomial eq rhs: Flt64` | `polynomial.toLinearPolynomial() eq rhs` |
| `polynomial le rhs: Flt64` | `polynomial.toLinearPolynomial() le rhs` |
| `polynomial ge rhs: Flt64` | `polynomial.toLinearPolynomial() ge rhs` |
| `polynomial lt rhs: Flt64` | `polynomial.toLinearPolynomial() lt rhs` |
| `polynomial gt rhs: Flt64` | `polynomial.toLinearPolynomial() gt rhs` |
| `polynomial ne rhs: Flt64` | `polynomial.toLinearPolynomial() ne rhs` |
| `polynomial leq rhs: Flt64` | `polynomial.toLinearPolynomial() leq rhs` |
| `polynomial geq rhs: Flt64` | `polynomial.toLinearPolynomial() geq rhs` |
| `polynomial neq rhs: Flt64` | `polynomial.toLinearPolynomial() neq rhs` |

#### Boolean RHS (无 ReplaceWith，需手动迁移)

| 旧 API | 新 API |
|--------|--------|
| `polynomial eq true` | `polynomial.toLinearPolynomial() eq Flt64.one` |
| `polynomial eq false` | `polynomial.toLinearPolynomial() eq Flt64.zero` |
| `polynomial le true` | `polynomial.toLinearPolynomial() le Flt64.one` |
| `polynomial le false` | `polynomial.toLinearPolynomial() le Flt64.zero` |
| ... | 同理，使用 `Flt64.one` / `Flt64.zero` |

**导入**:
```kotlin
import fuookami.ospf.kotlin.math.algebra.number.Flt64
```

#### UtilsLinearPolynomial RHS (有 ReplaceWith)

| 旧 API | 新 API |
|--------|--------|
| `polynomial eq rhs: UtilsLinearPolynomial<Flt64>` | `polynomial.toLinearPolynomial() eq rhs` |
| `polynomial le rhs: UtilsLinearPolynomial<Flt64>` | `polynomial.toLinearPolynomial() le rhs` |
| ... | 同理 |

---

### AbstractQuadraticPolynomial DSL

#### Flt64 RHS (有 ReplaceWith)

| 旧 API | 新 API |
|--------|--------|
| `polynomial eq rhs: Flt64` | `polynomial.toQuadraticPolynomial() eq rhs` |
| `polynomial le rhs: Flt64` | `polynomial.toQuadraticPolynomial() le rhs` |
| `polynomial ge rhs: Flt64` | `polynomial.toQuadraticPolynomial() ge rhs` |
| `polynomial lt rhs: Flt64` | `polynomial.toQuadraticPolynomial() lt rhs` |
| `polynomial gt rhs: Flt64` | `polynomial.toQuadraticPolynomial() gt rhs` |
| `polynomial ne rhs: Flt64` | `polynomial.toQuadraticPolynomial() ne rhs` |
| `polynomial leq rhs: Flt64` | `polynomial.toQuadraticPolynomial() leq rhs` |
| `polynomial geq rhs: Flt64` | `polynomial.toQuadraticPolynomial() geq rhs` |
| `polynomial neq rhs: Flt64` | `polynomial.toQuadraticPolynomial() neq rhs` |

#### Boolean RHS (无 ReplaceWith，需手动迁移)

| 旧 API | 新 API |
|--------|--------|
| `polynomial eq true` | `polynomial.toQuadraticPolynomial() eq Flt64.one` |
| `polynomial eq false` | `polynomial.toQuadraticPolynomial() eq Flt64.zero` |
| ... | 同理，使用 `Flt64.one` / `Flt64.zero` |

#### UtilsQuadraticPolynomial RHS (有 ReplaceWith)

| 旧 API | 新 API |
|--------|--------|
| `polynomial eq rhs: UtilsQuadraticPolynomial<Flt64>` | `polynomial.toQuadraticPolynomial() eq rhs` |
| ... | 同理 |

---

## 4. Model.kt 迁移映射

Model.kt 已在早期版本完成迁移，主要入口已支持 `MathLinearInequality` / `MathQuadraticPolynomial`。

参考已有 `@Deprecated` 注解中的 `ReplaceWith`。

---

## 5. 迁移示例

### 示例 1: 约束添加

```kotlin
// 旧代码
model.addConstraint(polynomial eq true)

// 新代码
model.addConstraint(polynomial.toLinearPolynomial() eq Flt64.one)
```

### 示例 2: 目标添加

```kotlin
// 旧代码
metaModel.addObject(ObjectCategory.Minimum, polynomial)

// 新代码
metaModel.addObject(
    ObjectCategory.Minimum,
    LinearFlattenData(polynomial.toLinearPolynomial().monomials, polynomial.toLinearPolynomial().constant),
    "objective",
    null
)
```

### 示例 3: DSL 不等式

```kotlin
// 旧代码
val constraint: AbstractLinearPolynomial<*> = ...
constraint eq Flt64(10.0)

// 新代码
constraint.toLinearPolynomial() eq Flt64(10.0)
```

---

## 6. 注意事项

1. **Boolean RHS**: math.symbol 不支持 Boolean 版本的 eq/le/ge 等操作符，需转换为 `Flt64.one` (true) 或 `Flt64.zero` (false)

2. **toLinearPolynomial()**: 所有 `AbstractLinearPolynomial` 实现了 `ToLinearPolynomial` 接口，可直接调用

3. **toQuadraticPolynomial()**: 所有 `AbstractQuadraticPolynomial` 实现了 `ToQuadraticPolynomial` 接口，可直接调用

4. **导入清单**:
   ```kotlin
   import fuookami.ospf.kotlin.math.algebra.number.Flt64
   import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
   import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
   import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenData
   import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenData
   ```

---

## 7. 统计

| 类别 | 数量 | 有 ReplaceWith | 无 ReplaceWith |
|------|------|----------------|----------------|
| MetaModel | 2 | 2 | 0 |
| MetaConstraint | 4 | 4 | 0 |
| MathInequalityDsl (Linear) | 27 | 17 (Flt64+Poly) | 10 (Boolean) |
| MathInequalityDsl (Quadratic) | 25 | 16 (Flt64+Poly) | 9 (Boolean) |
| **合计** | **58** | **39** | **19** |

---

## 下一步

完成 C1 后，进入 **C2 阶段**: 泛型化贯通 + plugin 边界下沉