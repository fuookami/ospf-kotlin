# OSPF Kotlin Core Refactor Daily

记录日期：2026-05-04

## P10 泛型化已完成

所有 P10 提交级改进已执行完毕，验收标准全部通过。

---

## 0. P10 验收结果

### 扫描结果（全部通过）

| 扫描项 | 结果 |
|--------|------|
| `*Bridge*.kt` 文件 | 0 |
| `ToMathLinearInequality` / `ToMathQuadraticInequality` 命中 | 0 |
| `relation as Flt64LinearInequality` 在 mechanism | 0 |
| `IntoValue.Flt64 as IntoValue<V>` 在 heuristic plugin | 0 |
| core 编译 | BUILD SUCCESS |
| 50 定向测试 | 全部通过 |

### 保留的 UNCHECKED_CAST（均为合法 adapter boundary）

| 位置 | 类型 | 说明 |
|------|------|------|
| Constraint.kt:160,204 | `flattenData.constant as V` | ConstraintImpl invoke: flattenData 是 Flt64，V=Flt64 时安全 |
| MechanismModel.kt:73 | `convertMechanismModelToFlt64` | 类型转换工具函数 |
| MechanismModel.kt:523,566,622,649 | `tokens as AbstractTokenTableFlt64` | solver 消费需要 Flt64 tokens |
| MechanismModel.kt:997,1085,1173,1200 | Benders cut solver boundary | solver 返回 Flt64 dual values |
| MetaConstraint.kt:259,289 | `IntoValue.Flt64 as IntoValue<V>` | flattenData 给 solver 消费 |
| MetaModel.kt:687,731 | tokens cast | solver 消费 |
| MetaModel.kt:940,951,1011,1122 | converter/tokens default | adapter boundary |

---

## 1. P10 提交记录

```text
fc2021ed refactor(core): eliminate remaining inequality unsafe casts in MechanismModel and MetaModel (P10-8)
9b4a323e refactor(heuristic): remove UNCHECKED_CAST converter defaults from heuristic solvers (P10-7)
95a9281d refactor(core): annotate Benders cut solver boundary, remove unused AbstractTokenTableFlt64 import (P10-6)
0c2d6f79 refactor(core): add V-generic companion factory overloads for SlackRange and SatisfiedAmountInequality (P10-5)
67fca2ba refactor(core): replace MathInequalityBridge with explicit flatten adapter, remove ToMathLinearInequality/ToMathQuadraticInequality (P10-3/P10-4)
```

### 各提交摘要

**P10-3/P10-4** (67fca2ba): 删除 `MathInequalityBridge.kt`，新增 `toLinearFlattenDataFlt64(converter)` / `toQuadraticFlattenDataFlt64(converter)` 显式 adapter 函数。`LinearConstraintInput.from()` 改为要求显式 `converter` 参数。移除 `ToMathLinearInequality` / `ToMathQuadraticInequality` 接口。

**P10-5** (0c2d6f79): 为 `SlackRangeFunction` 和 `SatisfiedAmountInequalityFunction` 系列（6 个类）增加 V-generic companion factory。`epsilon: Flt64` 判定为策略数值参数，保留 Flt64 签名，内部通过 `converter.intoValue(epsilon)` 转换。

**P10-6** (95a9281d): 为 Benders cut 4 个方法添加 solver boundary KDoc。移除 Constraint.kt 中未使用的 `AbstractTokenTableFlt64` import。

**P10-7** (9b4a323e): 移除 heuristic plugin 中所有 `IntoValue.Flt64 as IntoValue<V>` 默认值。`converter` 改为必填构造参数。GA/PSO/Particle/SAA/MVO/GWO/SCA 共 8 处。

**P10-8** (fc2021ed): 消除 MechanismModel 和 MetaModel 中最后的 `relation as Flt64LinearInequality` cast。`QuadraticMechanismModel.addConstraint(LinearInequality<V>)` 改用 `toLinearFlattenDataFlt64(parent.converter)` + 手动 quadratic promote。`QuadraticMetaModel.addConstraint(LinearInequality<V>)` 改用 V-typed `QuadraticInequalityOf<V>` 构造。

---

## 2. 下一阶段工作建议

P10 泛型化主路径改造完成。后续可考虑：

1. **P11: solver 插件 V 化** — 外部 solver 插件（Gurobi/CPLEX/SCIP 等）的 `solveV` / `Solution<V>` 主入口。
2. **P11: framework pipeline V 化** — `ospf-kotlin-framework` 中的 pipeline 类目前仍使用 `AbstractLinearMetaModel<Flt64>`。
3. **P11: example V 化** — 示例项目中的 Flt64 硬编码。
4. **补充测试** — 非 Flt64 V 的集成测试（当前只有 Flt64 回归测试）。
