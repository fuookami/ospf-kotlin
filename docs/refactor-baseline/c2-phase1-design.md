# C2 第一段设计方案：边界声明泛型 + 兼容别名

**生成日期**: 2026-04-16

---

## 目标

- 在接口签名层面声明泛型 `<V>`
- 数值内核保持 `Flt64`（内部实现不变）
- 提供 typealias 保证现有代码兼容
- 为第二段数值内核泛型化预留接口

---

## 设计原则

### Rust 参考

```rust
// Rust: 完全泛型
pub struct MetaModel<V = f64>
where
    V: Clone + Debug + Send + Sync + 'static,
{
    basic: BasicModel<V>,
    objective: Objective<V>,
    ...
}
```

### Kotlin 第一段策略

```kotlin
// Kotlin: 声明式泛型（内部仍用 Flt64）
sealed interface MetaModelOf<V : Number> {
    // 签名泛型，但内部实现保持 Flt64
    val tokens: AbstractMutableTokenTable  // 保持 Flt64
}

class LinearMetaModelOf<V : Number>(
    override var name: String,
    override val objectCategory: ObjectCategory,
    configuration: MetaModelConfiguration
) : MetaModelOf<V>, AbstractLinearMetaModel {
    // 内部实现保持 Flt64
    internal val _relationConstraints: MutableList<LinearInequalityConstraint>
    ...
}

// 兼容别名
typealias LinearMetaModel = LinearMetaModelOf<Flt64>
typealias QuadraticMetaModel = QuadraticMetaModelOf<Flt64>
typealias MetaModel = MetaModelOf<Flt64>

// 兼容构造器（保持现有调用方式）
fun LinearMetaModel(
    name: String = "",
    objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration()
): LinearMetaModelOf<Flt64> = LinearMetaModelOf(name, objectCategory, configuration)
```

---

## 具体改动

### 1. MetaModel 层

| 当前 | 新增 | typealias |
|------|------|-----------|
| `sealed interface MetaModel` | `sealed interface MetaModelOf<V : Number>` | `typealias MetaModel = MetaModelOf<Flt64>` |
| `class LinearMetaModel` | `class LinearMetaModelOf<V : Number>` | `typealias LinearMetaModel = LinearMetaModelOf<Flt64>` |
| `class QuadraticMetaModel` | `class QuadraticMetaModelOf<V : Number>` | `typealias QuadraticMetaModel = QuadraticMetaModelOf<Flt64>` |
| `interface AbstractLinearMetaModel` | `interface AbstractLinearMetaModelOf<V : Number>` | `typealias AbstractLinearMetaModel = AbstractLinearMetaModelOf<Flt64>` |
| `interface AbstractQuadraticMetaModel` | `interface AbstractQuadraticMetaModelOf<V : Number>` | `typealias AbstractQuadraticMetaModel = AbstractQuadraticMetaModelOf<Flt64>` |

### 2. MechanismModel 层

| 当前 | 新增 | typealias |
|------|------|-----------|
| `sealed interface MechanismModel` | `sealed interface MechanismModelOf<V : Number>` | `typealias MechanismModel = MechanismModelOf<Flt64>` |
| `interface AbstractLinearMechanismModel` | `interface AbstractLinearMechanismModelOf<V : Number>` | `typealias AbstractLinearMechanismModel = AbstractLinearMechanismModelOf<Flt64>` |
| `interface AbstractQuadraticMechanismModel` | `interface AbstractQuadraticMechanismModelOf<V : Number>` | `typealias AbstractQuadraticMechanismModel = AbstractQuadraticMechanismModelOf<Flt64>` |
| `class LinearMechanismModel` | `class LinearMechanismModelOf<V : Number>` | `typealias LinearMechanismModel = LinearMechanismModelOf<Flt64>` |
| `class QuadraticMechanismModel` | `class QuadraticMechanismModelOf<V : Number>` | `typealias QuadraticMechanismModel = QuadraticMechanismModelOf<Flt64>` |

### 3. 兼容构造器

为每个泛型类添加顶层构造函数：

```kotlin
// MetaModel.kt
fun LinearMetaModel(
    name: String = "",
    objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration()
): LinearMetaModelOf<Flt64> = LinearMetaModelOf(name, objectCategory, configuration)

fun QuadraticMetaModel(
    name: String = "",
    objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration()
): QuadraticMetaModelOf<Flt64> = QuadraticMetaModelOf(name, objectCategory, configuration)

// MechanismModel.kt
// (suspend 构造需要特殊处理)
```

---

## 内部实现策略

**关键点**: 第一段不改动数值内核，内部仍用 `Flt64`

示例：
```kotlin
class LinearMetaModelOf<V : Number>(
    override var name: String,
    override val objectCategory: ObjectCategory,
    configuration: MetaModelConfiguration
) : AbstractMetaModel(Linear, configuration), AbstractLinearMetaModelOf<V> {
    
    // 内部仍用 Flt64 类型（第一段不改动）
    internal val _relationConstraints: MutableList<LinearInequalityConstraint> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    
    internal val _subObjects: MutableList<MetaModel.SubObject> = ArrayList()
    override val subObjects: List<MetaModel.SubObject> by ::_subObjects
    
    // 方法签名保持现有（第一段不改动）
    fun addObject(
        category: ObjectCategory,
        polynomial: UtilsLinearPolynomial<Flt64>,
        name: String,
        displayName: String?
    ): Try { ... }
}
```

---

## 兼容性验证

### 现有调用方式

```kotlin
// 外部模块调用（如 BranchAndPriceAlgorithm.kt）
val model = LinearMetaModel("test")  // 必须继续工作
model.addConstraint(...)
```

### 兼容后

```kotlin
// typealias 让现有调用继续工作
val model: LinearMetaModel = LinearMetaModel("test")  // 实际是 LinearMetaModelOf<Flt64>

// 或显式使用泛型版本
val model = LinearMetaModelOf<Flt64>("test")
```

---

## 文件改动清单

| 文件 | 改动内容 |
|------|----------|
| `MetaModel.kt` | 添加泛型接口、泛型类、typealias、兼容构造器 |
| `MechanismModel.kt` | 添加泛型接口、泛型类、typealias |
| `MetaConstraint.kt` | 添加泛型接口（如需要） |
| 其他引用文件 | 更新 import（如有必要） |

---

## 测试验证

1. core 编译通过
2. framework 编译通过
3. 现有测试通过
4. 外部模块调用验证

---

## 第一段退出条件

| 条件 | 验证方式 |
|------|----------|
| 泛型接口声明完成 | `MetaModelOf<V>` 签名存在 |
| typealias 生效 | `LinearMetaModel` 别名可用 |
| 兼容构造器工作 | `LinearMetaModel("test")` 可调用 |
| 现有调用不破坏 | framework 编译通过 |
| 测试通过 | `mvn -pl ospf-kotlin-core -am test` |