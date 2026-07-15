# Network Scheduling 泛型化计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-network-scheduling` 当前只有顶层 `pom.xml`，没有已跟踪 Kotlin 源文件。

当前没有可迁移源码，泛型化计划作为后续实现准入约束。

## 2. 泛型化目标

后续实现网络调度时，应从第一版开始区分：

1. 时间、流量、容量、成本等有量纲领域值 `Quantity<V>`。
2. solver 建模数值 `Flt64`。
3. 图结构中的 ID、拓扑关系、离散计数。

## 3. 物理量化硬规则

网络调度中的有量纲字段必须使用 `Quantity<V>`：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 坐标/距离 | `x`, `y`, `distance`, `length` | `Length` |
| 时间 | `travelTime`, `serviceTime`, `duration` | `Time` |
| 流量 | `flow`, `demand`, `supply` | `Amount` 或业务单位 |
| 容量/产能 | `capacity`, `throughput`, `rate` | `Amount / Time` |
| 成本 | `cost`, `penalty` | `Currency` 或业务成本单位 |

裸 `V` 只用于比例、权重、评分和归一化目标值。

## 4. 建议设计

### Phase N0：基础类型

建议新增：

- `NetworkNode`
- `NetworkArc<V>`
- `Capacity<V>`，内部值为 `Quantity<V>`
- `Flow<V>`，内部值为 `Quantity<V>`
- `TravelTime<V>`，内部值为 `Quantity<V>`
- `NetworkCost<V>`，内部值为 `Quantity<V>`

验收：

- [ ] `NetworkArc<Flt64>` / `NetworkArc<FltX>` 编译通过。
- [ ] 容量与流量不使用裸 `Double`。

### Phase N1：算法与模型边界

路径搜索、流量分配、网络 LP/MIP 建模应分层：

- 纯图算法保持泛型或无数值。
- 成本/容量计算用 `V`。
- LP/MIP adapter 集中转换到 `Flt64`。

验收：

- [ ] 领域 API 不暴露 `LinearMetaModel<Flt64>`。
- [ ] solver 结果写回 `V` 有显式转换策略。

### Phase N2：门禁

```powershell
git grep -n "Flt64\\|FltX\\|Double\\|LinearMetaModel<Flt64>" -- ospf-kotlin-framework-network-scheduling
```

允许 `Flt64` 只出现在 adapter、typealias 和测试中。

## 5. 向后兼容

如果先实现 Flt64 特化，必须同时提供泛型主类型：

```kotlin
typealias Flt64NetworkArc = NetworkArc<Flt64>
```

不要只发布固定 `Flt64` 的领域模型。
