# gantt-scheduling-domain-capacity-scheduling-context

`IterativeCapacityCompilation` 在装配调用内建立 action/slot 首匹配索引和执行器分组。
每次符号刷新只构造一次当前列-变量映射，并按执行器/时隙分桶，避免每条选列表达式都扫描完整列映射；每次刷新都会重新读取可变输入，不跨调用缓存。

列变量仍沿用既有替换生命周期：`CombinationVariableItem` 的身份依赖 parent，矩形 shape 扩展还可能改变扁平坐标。因此新增列仍会替换该执行器的变量 parent，并重绑其历史项；本兼容优化不声称变量可直接追加。边界复制以及 token 移除/重新注册仍遍历旧、新矩形变量数组；操作时间刷新仍扫描全部历史 allocation，capacity 仍重建 action-slot 网格，全局 cost 则保持原有扁平项顺序并扫描全部历史列。因此剩余工作至少与被替换变量矩形和受影响历史规模成正比。选列表达式索引单项从重复的 `O(执行器数 × 时隙数 × 列数)` 扫描降为预期 `O(列数 + allocation项数 + 执行器-时隙行数)`；`CapacityColumn` 键的哈希还需访问其 allocation 映射，不能视为常数工作。

:us: [English](README.md) | :cn: 简体中文

基于时间槽的产能调度 — 建模产能列、编译调度约束，并定义主问题的产能相关限制。

## 关键类型

| 类型 | 描述 |
|------|------|
| `Capacity` | 调度实体的产能定义 |
| `CapacityColumn` | 表示时间槽中产能分配的列 |
| `CapacityColumnAggregation` | 产能列的聚合 |
| `CapacityCompilation` | 将产能数据编译到调度模型 |
| `CapacityOrderCompilation` | 编译订单特定的产能约束 |
| `IterativeCapacityCompilation` | 产能精化的迭代编译 |
| `CapacitySchedulingSolution` | 产能调度的解输出 |
| `ProductionAction` | 表示生产决策的动作 |
| `CapacitySolverValue` | 产能变量的求解器层值 |
| `CapacitySchedulingContext` | 产能调度的领域上下文 |
| `CapacitySchedulingAggregation` | 产能调度结果的聚合根 |
| `CapacityCostMinimization` | 限制：最小化总产能成本 |
| `ExecutorCapacityConstraint` | 限制：约束执行者产能使用 |
| `OrderConstraint` | 限制：强制订单满足约束 |

## 依赖

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`
