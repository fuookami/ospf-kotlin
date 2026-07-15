# BPP1D 泛型化计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-bpp1d` 当前只有顶层 `pom.xml`，没有已跟踪 Kotlin 源文件，也没有子模块。

这意味着当前没有可迁移的 `Flt64` / `FltX` 硬编码实现。泛型化工作应作为后续实现的准入约束，而不是源码改造。

## 2. 泛型化目标

后续补充 BPP1D 实现时，所有业务数值域必须从第一版开始泛型化：

1. 宽度、长度、重量等物理量使用 `Quantity<V>` 或模块内统一的量纲包装类型。
2. 成本、目标值、松弛量、惩罚项使用 `V : RealNumber<V>` 或 `V : FloatingNumber<V>`。
3. 计数、件数、层数等离散数量继续使用 `UInt64` / `Int64`。
4. solver/model 边界允许保留 `Flt64` 特化 wrapper，但核心领域模型不得直接固定为 `Flt64`。

## 3. 物理量化硬规则

只要字段有物理量纲，就必须使用 `Quantity<V>`，不能只写成裸 `V`：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 坐标/偏移 | `x`, `start`, `end`, `offset` | `Length` |
| 尺寸 | `width`, `length` | `Length` |
| 重量 | `weight`, `load` | `Mass` |
| 面积/体积 | 若后续扩展到二维/三维 | `Area` / `Volume` |
| 产能/处理能力 | `capacity`, `throughput`, `processingRate` | `Amount / Time` 或业务定义单位 |

裸 `V` 只用于无量纲值：利用率、比例、惩罚系数、归一化目标值等。

## 4. 建议模块设计

### Phase B1-0：建立数值上下文

新增统一数值上下文：

```kotlin
interface Bpp1dNumberContext<V> {
    val zero: V
    val one: V
    fun fromUInt(value: UInt64): V
}
```

验收：

- [ ] 领域模型构造不直接引用 `Flt64.zero` / `Flt64.one`。
- [ ] 默认提供 `Flt64Bpp1dNumberContext` 兼容当前生态。

### Phase B1-1：领域对象泛型化

后续新增对象建议直接采用：

- `Item<V>`
- `Bin<V>`
- `BinType<V>`
- `Placement<V>`
- `PackingPlan<V>`

验收：

- [ ] `Item<Flt64>` 编译通过。
- [ ] `Item<FltX>` 编译通过。
- [ ] 物理量字段不使用裸 `Double` / `Flt64`。

### Phase B1-2：算法与模型边界

算法内部可以按 solver 需要转换到 `Flt64`，但转换必须集中在 adapter：

- `Bpp1dSolverAdapter<V>`
- `Bpp1dFlt64SolverAdapter`

验收：

- [ ] 领域层无 `AbstractLinearMetaModel<Flt64>` 泄漏。
- [ ] Flt64 求解结果写回 `V` 时有显式转换策略。

## 5. 门禁

在新增任何 Kotlin 源文件后执行：

```powershell
git grep -n "Flt64\\|FltX\\|Double" -- ospf-kotlin-framework-bpp1d
```

允许出现的位置：

1. Flt64 兼容 typealias。
2. solver adapter。
3. 测试中的特化用例。

其余位置都应改为泛型数值域。
