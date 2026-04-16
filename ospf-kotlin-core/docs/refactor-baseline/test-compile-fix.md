# C2 测试编译遗留修复方案（2026-04-17 修订）

> 基于 mvn test-compile 实际输出，补全56条完整错误清单

---

## 1. 错误统计总览

| 文件 | 错误数 | 错误类型分布 |
|------|--------|--------------|
| MonomialCoefficientPreservationTest.kt | 22 | 14×泛型参数错误 + 6×evaluate歧义 + 2×候选不匹配 |
| LinearPolynomialBaselineTest.kt | 12 | 3×tokenList类型 + 4×evaluate候选 + 4×类型推断 + 1×引用错误 |
| QuadraticPolynomialBaselineTest.kt | 12 | 3×tokenList类型 + 4×evaluate候选 + 4×类型推断 + 1×引用错误 |
| FlattenUtilityTest.kt | 8 | 8×typealias类型不匹配 |
| SubObjectTest.kt | 2 | 2×typealias类型不匹配 |
| **总计** | **56** | |

---

## 2. 每个文件的完整错误清单

### 2.1 MonomialCoefficientPreservationTest.kt (22 条)

#### 错误位置明细

| 行号 | 错误类型 | 当前代码 | 修复方法 |
|------|----------|----------|----------|
| 33 | No type arguments expected | `LinearMonomialCell.invoke<Flt64>(Flt64(3.0), variable)` | 移除 `<Flt64>` |
| 38 | Overload resolution ambiguity | `cell.evaluate(values, null, false)` | 添加类型转换: `null as AbstractTokenList?` |
| 47 | No type arguments expected | `LinearMonomialCell.invoke<Flt64>(Flt64(-2.0), variable)` | 移除 `<Flt64>` |
| 51 | Overload resolution ambiguity | `cell.evaluate(values, null, false)` | 添加类型转换 |
| 63 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(7.0), variable, null)` | 移除 `<Flt64>` |
| 67 | Overload resolution ambiguity | `cell.evaluate(values, null, false)` | 添加类型转换 |
| 77 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(3.0), x, y)` | 移除 `<Flt64>` |
| 84 | Overload resolution ambiguity | `cell.evaluate(values, null, false)` | 添加类型转换 |
| 94 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(-1.5), x, y)` | 移除 `<Flt64>` |
| 101 | Overload resolution ambiguity | `cell.evaluate(values, null, false)` | 添加类型转换 |
| 111 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(4.0), x, x)` | 移除 `<Flt64>` |
| 115 | Overload resolution ambiguity | `cell.evaluate(values, null, false)` | 添加类型转换 |
| 126 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(2.0), x, null)` | 移除 `<Flt64>` |
| 127 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(2.0), x, null)` | 移除 `<Flt64>` |
| 135 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(2.0), x, null)` | 移除 `<Flt64>` |
| 136 | No type arguments expected | `LinearMonomialCell.invoke<Flt64>(Flt64(2.0), x)` | 移除 `<Flt64>` |
| 146 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(2.0), x, null)` | 移除 `<Flt64>` |
| 147 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(3.0), x, null)` | 移除 `<Flt64>` |
| 156 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(2.0), x, y)` | 移除 `<Flt64>` |
| 157 | No type arguments expected | `QuadraticMonomialCell.invoke<Flt64>(Flt64(2.0), x, y)` | 移除 `<Flt64>` |
| 172 | None of candidates applicable | `monomial.evaluate(values, null as AbstractTokenList?, false)` | 改为 `null as AbstractTokenListOf<Flt64>?` |
| 192 | None of candidates applicable | `monomial.evaluate(values, null as AbstractTokenList?, false)` | 改为 `null as AbstractTokenListOf<Flt64>?` |

#### 修复模板

```kotlin
// 修复1: 移除泛型参数 (14处)
- LinearMonomialCell.invoke<Flt64>(coefficient, variable)
+ LinearMonomialCell.invoke(coefficient, variable)

- QuadraticMonomialCell.invoke<Flt64>(coefficient, variable1, variable2)
+ QuadraticMonomialCell.invoke(coefficient, variable1, variable2)

// 修复2: evaluate 调用消除歧义 (6处)
- cell.evaluate(values, null, false)
+ cell.evaluate(values, null as AbstractTokenList?, false)

// 修复3: monomial.evaluate 类型转换 (2处)
- monomial.evaluate(values, null as AbstractTokenList?, false)
+ monomial.evaluate(values, null as AbstractTokenListOf<Flt64>?, false)
```

---

### 2.2 LinearPolynomialBaselineTest.kt (12 条)

#### 错误位置明细

| 行号 | 错误类型 | 当前代码 | 修复方法 |
|------|----------|----------|----------|
| 96 | Argument type mismatch | `tokenList` (AutoTokenList) | 转换为 `AbstractTokenListOf<Flt64>?` |
| 163 | None of candidates applicable | `polynomial.evaluate(...)` | 使用命名参数或类型转换 |
| 166 | Cannot infer type parameter T | 返回值类型推断失败 | 添加显式类型 |
| 168 | Unresolved reference 'eq' | 链式错误（由166引发） | 修复166后自动解决 |
| 192 | None of candidates applicable | `polynomial.evaluate(...)` | 使用命名参数或类型转换 |
| 194 | None of candidates applicable | `polynomial.evaluate(...)` | 使用命名参数或类型转换 |
| 197 | Cannot infer type parameter T | 返回值类型推断失败 | 添加显式类型 |
| 199 | Cannot infer type parameter T | 返回值类型推断失败 | 添加显式类型 |
| 201 | Unresolved reference 'eq' | 链式错误 | 修复197后自动解决 |
| 202 | Unresolved reference 'eq' | 铔式错误 | 修复199后自动解决 |
| 315 | Argument type mismatch | `tokenList` (AutoTokenList) | 转换为 `AbstractTokenListOf<Flt64>?` |
| 323 | Argument type mismatch | `tokenList` (AutoTokenList) | 转换为 `AbstractTokenListOf<Flt64>?` |

#### 修复模板

```kotlin
// 修复1: tokenList 类型转换 (3处: 96, 315, 323)
- polynomial.evaluate(..., tokenList = tokenList, ...)
+ polynomial.evaluate(..., tokenList = tokenList as AbstractTokenListOf<Flt64>?, ...)

// 或使用显式 cast
- val tokenList = AutoTokenList(...)
+ val tokenList = AutoTokenList(...) as AbstractTokenListOf<Flt64>

// 修复2: evaluate 调用候选不匹配 (行 163, 192, 194)
// 需读取具体代码确定修复方式

// 修复3: 类型推断失败 (行 166, 197, 199)
- val result = polynomial.evaluate(...)
+ val result: Flt64? = polynomial.evaluate(...)
```

---

### 2.3 QuadraticPolynomialBaselineTest.kt (12 条)

#### 错误位置明细

| 行号 | 错误类型 | 当前代码 | 修复方法 |
|------|----------|----------|----------|
| 98 | Argument type mismatch | `tokenList` (AutoTokenList) | 转换为 `AbstractTokenListOf<Flt64>?` |
| 166 | None of candidates applicable | `polynomial.evaluate(...)` | 同 Linear 版本 |
| 169 | Cannot infer type parameter T | 返回值类型推断失败 | 添加显式类型 |
| 171 | Unresolved reference 'eq' | 链式错误 | 修复169后自动解决 |
| 196 | None of candidates applicable | `polynomial.evaluate(...)` | 同 Linear 版本 |
| 198 | None of candidates applicable | `polynomial.evaluate(...)` | 同 Linear 版本 |
| 201 | Cannot infer type parameter T | 返回值类型推断失败 | 添加显式类型 |
| 203 | Cannot infer type parameter T | 返回值类型推断失败 | 添加显式类型 |
| 205 | Unresolved reference 'eq' | 链式错误 | 修复201后自动解决 |
| 206 | Unresolved reference 'eq' | 链式错误 | 修复203后自动解决 |
| 307 | Argument type mismatch | `tokenList` (AutoTokenList) | 转换为 `AbstractTokenListOf<Flt64>?` |
| 315 | Argument type mismatch | `tokenList` (AutoTokenList) | 转换为 `AbstractTokenListOf<Flt64>?` |

**修复模式**: 与 LinearPolynomialBaselineTest.kt 相同

---

### 2.4 FlattenUtilityTest.kt (8 条)

#### 错误位置明细

| 行号 | 错误类型 | 当前代码 | 修复方法 |
|------|----------|----------|----------|
| 78 | Argument type mismatch | `List<LinearFlattenData>` | 改为 `List<LinearFlattenDataOf<Flt64>>` |
| 136 | Argument type mismatch | `List<QuadraticFlattenData>` | 改为 `List<QuadraticFlattenDataOf<Flt64>>` |
| 159 | Argument type mismatch (×2) | `LinearFlattenData` 参数 | 改为 `LinearFlattenDataOf<Flt64>` |
| 180 | Argument type mismatch (×2) | `LinearFlattenData` 参数 | 改为 `LinearFlattenDataOf<Flt64>` |
| 204 | Argument type mismatch | `LinearFlattenData` 参数 | 改为 `LinearFlattenDataOf<Flt64>` |
| 223 | Argument type mismatch | `QuadraticFlattenData` 参数 | 改为 `QuadraticFlattenDataOf<Flt64>` |

#### 修复模板

```kotlin
// 修改 import
- import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenData
- import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenData
+ import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataOf
+ import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenDataOf

// 所有类型替换
- LinearFlattenData(...) -> LinearFlattenDataOf<Flt64>(...)
- QuadraticFlattenData(...) -> QuadraticFlattenDataOf<Flt64>(...)
```

---

### 2.5 SubObjectTest.kt (2 条)

#### 错误位置明细

| 行号 | 错误类型 | 当前代码 | 修复方法 |
|------|----------|----------|----------|
| 47 | Argument type mismatch | `LinearFlattenData` | 改为 `LinearFlattenDataOf<Flt64>` |
| 75 | Argument type mismatch | `QuadraticFlattenData` | 改为 `QuadraticFlattenDataOf<Flt64>` |

**修复模式**: 与 FlattenUtilityTest.kt 相同的 typealias 替换

---

## 3. 修复执行优先级

| 优先级 | 文件 | 错误数 | 预估工时 | 理由 |
|--------|------|--------|----------|------|
| P0 | MonomialCoefficientPreservationTest.kt | 22 | 20 min | 错误最多，修复模式统一（批量替换） |
| P1 | LinearPolynomialBaselineTest.kt | 12 | 15 min | 多种错误类型，需逐个处理 |
| P1 | QuadraticPolynomialBaselineTest.kt | 12 | 15 min | 与 Linear 模式相同 |
| P2 | FlattenUtilityTest.kt | 8 | 10 min | 简单的 typealias 替换 |
| P2 | SubObjectTest.kt | 2 | 5 min | 仅 2 处修改 |

**总预估工时**: 65 分钟

---

## 4. 根因分析

### Kotlin 2.2.20 类型推断变化

**核心问题**: typealias 身份识别变更

| 变化点 | 影响 | 示例 |
|--------|------|------|
| typealias 构造器调用 | 泛型参数推断失败 | `LinearFlattenData(...)` 不等价于 `LinearFlattenDataOf<Flt64>(...)` |
| 非泛型 typealias | 不接受泛型参数 | `LinearMonomialCell.invoke<Flt64>` 错误 |
| List<typealias> vs List<泛型> | 类型不匹配 | `List<LinearFlattenData>` ≠ `List<LinearFlattenDataOf<Flt64>>` |
| evaluate 重载 | AbstractTokenList vs AbstractTokenListOf<Flt64> 歧义 | `null` 参数需显式类型转换 |

---

## 5. 执行步骤

| 步骤 | 内容 | 状态 |
|------|------|------|
| C2-分析补全 | 补全56条错误清单 | ✅ 完成 |
| C2-P0修复 | MonomialCoefficientPreservationTest.kt (22条) | ✅ 完成 |
| C2-P1修复 | LinearPolynomialBaselineTest.kt (12条) | ✅ 完成 |
| C2-P1修复 | QuadraticPolynomialBaselineTest.kt (12条) | ✅ 完成 |
| C2-P2修复 | FlattenUtilityTest.kt (8条) | ✅ 完成 |
| C2-P2修复 | SubObjectTest.kt (2条) | ✅ 完成 |
| C2-验证 | mvn test-compile 验证 | ✅ BUILD SUCCESS |

---

## 6. 文档签署

| 角色 | 签署 |
|------|------|
| **C2 执行人** | Claude Code |
| **审核人** | 用户 |
| **日期** | 2026-04-17 |