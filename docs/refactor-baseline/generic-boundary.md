# 泛型化边界清单 (C2 交付物)

**生成日期**: 2026-04-16
**版本**: C2 状态扫描

---

## 概览

本文档记录 Kotlin ospf-kotlin-core 当前泛型化状态，并与 Rust ospf-rust-core 架构对比。

---

## Rust 架构基线（目标）

### 1) 泛型层次

```
MetaModel → MechanismModel<V> → IntermediateModel(f64)
```

- **MechanismModel<V>**: 泛型值类型 V
- **IntermediateModel**: 求解器标准形式，直接使用 f64（如 `BasicLinearTriadModel` 使用 `Token<f64>`）
- **转换点**: `MechanismModel<V> → IntermediateModel` 时进行 V → f64 实例化

### 2) 关键设计原则

1. **主链路泛型化**: MetaModel 和 MechanismModel 保持泛型 V
2. **求解器边界固化**: IntermediateModel 作为求解器标准形式，使用 f64
3. **单一转换点**: V → f64 转换发生在 MechanismModel → IntermediateModel 边界

---

## Kotlin 当前状态

### 1) MetaModel 状态

**文件**: `MetaModel.kt`

| 类/接口 | 泛型参数 | 数值类型 | 是否泛型化 |
|---------|----------|----------|------------|
| `MetaModel` | 无 | - | ❌ 非泛型 |
| `LinearMetaModel` | 无 | `Flt64` | ❌ 非泛型 |
| `QuadraticMetaModel` | 无 | `Flt64` | ❌ 非泛型 |
| `SubObject` | 无 | `UtilsLinearPolynomial<Flt64>` | ❌ 已固化 |

**关键 API**:
- `addObject(category, polynomial: UtilsLinearPolynomial<Flt64>)` - 已固化
- `addConstraint(relation: MathLinearInequality)` - 已固化

### 2) MechanismModel 状态

**文件**: `MechanismModel.kt`

| 类/接口 | 泛型参数 | 数值类型 | 是否泛型化 |
|---------|----------|----------|------------|
| `MechanismModel` | 无 | - | ❌ 非泛型 |
| `LinearMechanismModel` | 无 | `Flt64` | ❌ 非泛型 |
| `QuadraticMechanismModel` | 无 | `Flt64` | ❌ 非泛型 |

**关键方法**:
- `generateOptimalCut(...)`: 返回 `List<MathLinearInequality>` - 已固化
- `generateFeasibleCut(...)`: 返回 `List<MathLinearInequality>` - 已固化

### 3) IntermediateModel 状态

**文件**: `LinearTriadModel.kt`, `QuadraticTetradModel.kt`

| 类/接口 | 泛型参数 | 数值类型 | 是否固化 |
|---------|----------|----------|----------|
| `LinearTriadModel` | 无 | `Flt64` | ✅ 正确固化 |
| `QuadraticTetradModel` | 无 | `Flt64` | ✅ 正确固化 |
| `LinearConstraintCell` | 无 | `Flt64` | ✅ 正确固化 |
| `QuadraticConstraintCell` | 无 | `Flt64` | ✅ 正确固化 |

**结论**: IntermediateModel 状态符合 Rust 设计（求解器标准形式使用 f64）

### 4) TokenTable / Cache 状态

**文件**: `TokenTable.kt`, `TokenCacheContext.kt`

| 类/接口 | 泛型参数 | 数值类型 | 是否泛型化 |
|---------|----------|----------|------------|
| `LinearFlattenData` | 无 | `Flt64` | ❌ 已固化 |
| `QuadraticFlattenData` | 无 | `Flt64` | ❌ 已固化 |
| `ValueCacheContext` | 无 | `Flt64` | ❌ 已固化 |
| `RangeCacheContext` | 无 | `ExpressionRange<Flt64>` | ❌ 已固化 |
| `TokenCacheContexts` | 无 | - | ❌ 非泛型 |

---

## 与 Rust 架构差异分析

### 差异项 1: MechanismModel 未泛型化

**Rust**: `MechanismModel<V>` 是泛型的，支持不同值类型

**Kotlin**: `LinearMechanismModel` 和 `QuadraticMechanismModel` 直接使用 `Flt64`

**影响**:
- 如果需要支持其他数值类型（如 Int64、Rational），需要重构
- 当前实现简化了代码，但牺牲了泛型灵活性

### 差异项 2: MetaModel 已固化

**Rust**: MetaModel 层应该支持泛型（通过符号类型）

**Kotlin**: MetaModel 的 `SubObject.polynomial` 已经是 `UtilsLinearPolynomial<Flt64>`

**影响**:
- 与 Rust 设计存在差异
- 用户层面已经是 Flt64，没有泛型灵活性

### 差异项 3: 缺少显式转换边界

**Rust**: `MechanismModel<V> → IntermediateModel` 有明确的 V → f64 转换点

**Kotlin**: 当前 MechanismModel 已经是 Flt64，没有显式转换边界

---

## 分类结论

### 必要固化点（符合 Rust 设计）

| 层级 | 类 | 固化状态 | 理由 |
|------|----|---------|------|
| IntermediateModel | `LinearTriadModel` | ✅ Flt64 | 求解器标准形式 |
| IntermediateModel | `QuadraticTetradModel` | ✅ Flt64 | 求解器标准形式 |
| Cell 类型 | `LinearConstraintCell` | ✅ Flt64 | 求解器内部表示 |
| Cell 类型 | `QuadraticConstraintCell` | ✅ Flt64 | 求解器内部表示 |

### 当前已固化（需评估是否需要泛型化）

| 层级 | 类 | 当前状态 | Rust 对应 |
|------|----|---------|-----------|
| MetaModel | `LinearMetaModel` | ❌ Flt64 固化 | 应支持泛型 |
| MetaModel | `QuadraticMetaModel` | ❌ Flt64 固化 | 应支持泛型 |
| MechanismModel | `LinearMechanismModel` | ❌ Flt64 固化 | `MechanismModel<V>` 泛型 |
| MechanismModel | `QuadraticMechanismModel` | ❌ Flt64 固化 | `MechanismModel<V>` 泛型 |

### 缓存体系

| 层级 | 类 | 当前状态 | 备注 |
|------|----|---------|------|
| FlattenContext | `LinearFlattenContext` | ❌ Flt64 固化 | 与符号类型绑定 |
| FlattenContext | `QuadraticFlattenContext` | ❌ Flt64 固化 | 与符号类型绑定 |
| ValueCache | `ValueCacheContext` | ❌ Flt64 固化 | 求值缓存 |
| RangeCache | `RangeCacheContext` | ❌ Flt64 固化 | 范围缓存 |

---

## C2 阶段结论

### 当前架构评估

**总体结论**: Kotlin 实现已经完全固化到 `Flt64`，与 Rust 的泛型设计有差异。

**两种可能方向**:

1. **保持当前固化状态**
   - 优点：代码简洁，无泛型复杂度
   - 缺点：与 Rust 架构不一致，无法支持其他数值类型
   - 适用场景：如果项目只使用 Flt64，可以选择此方案

2. **引入泛型化**
   - 优点：与 Rust 对齐，架构一致性
   - 缺点：重构工作量较大
   - 适用场景：如果项目需要与 Rust 保持架构一致性

### 建议

根据 daily.md 中架构约束第 7 点：

> "泛型化边界固定（对齐 Rust）：
> - MetaModel -> MechanismModel 必须保持泛型值类型 V 贯通（Rust: MechanismModel<V>）
> - IntermediateModel 作为求解器标准形式，直接使用 f64（Rust: BasicLinearTriadModel 直接用 Token<f64>）
> - 在 MechanismModel -> IntermediateModel 转换时进行 V -> f64 实例化"

**建议**: 按照架构约束，需要引入泛型化。但考虑到工作量，建议：

1. **优先确认 IntermediateModel 边界正确**: 已确认正确
2. **评估 MechanismModel 泛型化必要性**: 如果实际使用只涉及 Flt64，可以保持当前状态
3. **如果选择泛型化**: 需要在 MetaModel 和 MechanismModel 层引入泛型 `<V>`

---

## 下一步行动

### 等待用户确认

用户需确认以下决策：

1. **是否需要泛型化 MechanismModel**?
   - 选项 A: 保持 Flt64 固化（简化实现）
   - 选项 B: 引入泛型 `<V>`（与 Rust 对齐）

2. **如果选择泛型化，改造范围**:
   - MetaModel: 是否需要泛型化
   - MechanismModel: 是否需要泛型化
   - TokenTable/Cache: 是否需要泛型化

---

## 附录：Rust 关键代码参考

### Rust MechanismModel 签名

```rust
// ospf-rust-core/src/model/mechanism/mechanism_model.rs
pub struct MechanismModel<V: Value> {
    // ...
}
```

### Rust IntermediateModel 签名

```rust
// ospf-rust-core/src/model/intermediate/linear_triad_model.rs
pub struct BasicLinearTriadModel {
    variables: Vec<Variable>,
    constraints: LinearConstraintBatch,  // 使用 Token<f64>
    // ...
}
```

### Rust 转换边界

```rust
// MechanismModel<V> -> BasicLinearTriadModel
// 在转换时进行 V -> f64 实例化
impl<V: Value> MechanismModel<V> {
    pub fn to_triad_model(&self) -> BasicLinearTriadModel {
        // V -> f64 转换
    }
}
```