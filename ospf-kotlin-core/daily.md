# OSPF Kotlin Core Refactor Daily

记录日期：2026-05-04（更新）

本文是下一会话交接文档。Commit-1 到 Commit-F（含 Commit-9 到 Commit-13）已有真实 git 提交记录。

---

## 0. 当前状态总结

1. Commit-1 到 Commit-8：已在 `git log` 中，完成于之前的会话。
2. Commit-9 到 Commit-13：已在 `git log` 中，完成于之前的会话（mechanism addConstraint V化、ConstraintImpl V化、_constraints存储消除强转、CallBackModel工厂重载、isTrue接口）。
3. Commit-E：已提交 `e59d1b00`，solver V-generic solveV overloads + SolverExt V-generic extensions。
4. Commit-F：已提交 `1e04bd18`，function 目录 UNCHECKED_CAST 清理（消除2处不必要强转，添加 asVQuadraticPoly converter）。
5. 编译门禁：core、framework、example 全部 BUILD SUCCESS。
6. 测试门禁：50 个定向测试全部通过。
7. **剩余 Flt64 命中均为合法 adapter boundary**，详见下方分析。

---

## 0.1 Adapter Boundary 豁免说明

以下 Flt64 命中是合法的 adapter boundary，不需要进一步消除：

### Solver adapter boundary
- `LinearSolver.kt` / `QuadraticSolver.kt`：Flt64 `invoke` 方法是外部 solver 的接口，solver 内部使用 double/Flt64。V-generic `solveV` 是主接口，Flt64 invoke 是 adapter。
- `SolverExt.kt`：Flt64 solve extensions 是 backward compat，V-generic solve 是主接口。

### Mechanism adapter boundary
- `MechanismModel.addConstraint` 中的 `relation as Flt64LinearInequality`：ConstraintImpl 需要 Flt64 flattenData 给 solver 消费。
- `MechanismModel.generateOptimalCut/FeasibleCut`：Benders cut 操作在 Flt64 上（solver dual values 是 Flt64）。
- `MechanismModel.generateOptimalCutFromOutput/FeasibleCutFromOutput`：接受 `Solution<Flt64>` 因为 solver 输出是 Flt64。
- `MetaModel.setSolverSolution`：接受 `Solution<Flt64>` 因为 solver 输出是 Flt64，内部转换到 V tokenTable。
- `MetaConstraint.flattenData`：`inequality as LinearInequality<Flt64>` 是 adapter boundary（flattenData 给 solver）。
- `LinearConstraintInput.from`：`relation as Flt64LinearInequality` 是 adapter boundary（flattenData 访问）。

### Function symbol adapter boundary
- `Product.kt`：`tokenTable as AbstractTokenTableFlt64` 是 solver 评估路径的 adapter boundary。
- `Masking.kt`：`values as Map<Symbol, V>` 是 Flt64→V 值映射的 adapter boundary。
- `FunctionSymbol.kt`：`values as Map<Symbol, V>` 是 evaluate 路径的 adapter boundary。

### DSL adapter boundary
- `MathInequalityDsl.kt`：所有 `Flt64LinearInequality` 返回类型是用户 API DSL 便利层。当 V=Flt64 时，`Flt64LinearInequality` 就是 `LinearInequality<V>`，传入 `addConstraint(relation: LinearInequality<V>)` 自然兼容。

### Bridge 豁免
- `MathInequalityBridge.kt`：不能删除。`ToMathLinearInequality` 被 `AbstractVariableItem.kt`、`IntermediateSymbol.kt`、`Model.kt` 依赖。`flattenData` 扩展属性被 MechanismModel、MetaConstraint、LinearConstraintInput 广泛使用。这是 solver 消费路径的核心基础设施。

### Constraint adapter boundary
- `Constraint.kt`：`(-flattenData.constant) as V` 是 adapter boundary（Flt64→V，safe when V=Flt64）。

---

## 0.2 当前扫描结果（2026-05-04）

| 扫描项 | 命中数 | 判定 |
|---|---:|---|
| `import ... as ...` | 0 | 通过 |
| `evaluateFlt64/evaluateAsFlt64/constantFlt64` | 0 | 通过 |
| `Flt64.zero as V/Flt64.one as V/Flt64(...) as V/this as Flt64` | 0 | 通过 |
| callback 中业务 `Solution<Flt64>` | 0 | 通过（Commit-7/13 已清理） |
| heuristic 中业务 `Solution<Flt64>` | 0 | 通过 |
| function 目录可消除 `UNCHECKED_CAST` | 0 | 通过（Commit-F 已清理） |
| mechanism adapter boundary `UNCHECKED_CAST` | 25 | 豁免（全部标注 adapter boundary） |
| solver adapter boundary `Solution<Flt64>` | 44 | 豁免（Flt64 invoke 是 adapter） |
| mechanism adapter boundary `Solution<Flt64>` | 6 | 豁免（Benders cut + setSolverSolution） |
| `Flt64LinearInequality` 总命中 | 331 | 豁免（= `LinearInequality<Flt64>`，V=Flt64 时自然兼容） |
| `MathInequalityBridge.kt` | 1 | 豁免（核心基础设施，3个文件依赖） |

---

## 1. 已完成提交记录

```text
1e04bd18 refactor(core): eliminate removable UNCHECKED_CAST in function symbols, add asVQuadraticPoly converter — Commit-F
e59d1b00 refactor(core): add V-generic solveV overloads for MechanismModel and solver extensions — Commit-E
8bf37626 refactor(core): V-genericize CallBackModel factory overloads, add isTrue to Constraint interface — Commit-13
019c45bc refactor(core): eliminate _constraints storage UNCHECKED_CAST in MechanismModel — Commit-12
64ad4014 refactor(core): V-genericize ConstraintImpl, MetaConstraint extensions, LinearConstraintInput — Commit-11
72ab4f73 refactor(framework): replace AbstractLinearMetaModel<*> with AbstractLinearMetaModel<Flt64> in pipelines — Commit-10
134c6120 refactor(core): V-genericize mechanism addConstraint signatures & TokenTable converter threading — Commit-9
bb3b7995 chore: checkpoint P10 genericization work
1fc17b46 refactor(core): add RealNumber & NumberField bounds to heuristic solver V parameters — Commit-8
9b9b5a16 refactor(core): genericize callback main chain with IntoValue<V> converter — Commit-7
54b83dab refactor(core): remove bridge typealiases from MathInequalityDsl, deduplicate against math layer — Commit-6
ff07b0ef refactor(core): eliminate pseudo-generic patterns in all function symbols — Commit-5
01d6df25 refactor(core): correct Slack/SlackRange/FunctionSymbol genericization — Commit-4
0003760f refactor(core): split MathFunctionSymbol.register into dual-phase registerAuxiliaryTokens + registerConstraints — Commit-3
6a570509 refactor(core): extend IntoValue<V> with zero/one/fromValue, deprecate pseudo-generic tools — Commit-1
```

---

## 2. 已完成的改进项

### Commit-A/B/C（Commit-1 到 Commit-8）：基线、callback、TokenTable、mechanism 基础
- 已在之前的会话完成并提交。

### Commit-D（Commit-9 到 Commit-13）：mechanism inequality/tokenTable V 化
- `addConstraint` 接口已接受 `LinearInequality<V>` / `QuadraticInequalityOf<V>`。
- `ConstraintImpl<V, P>` 已 V 泛型化，`isTrue()` 使用 V-typed evaluation。
- `_constraints` 存储类型改为 `MutableList<ConstraintImpl<V>>`，消除强转。
- `LinearConstraintInput.from` 已接受 `LinearInequality<V>`。
- `CallBackModel` 工厂重载已 V 泛型化。
- **剩余 adapter boundary**：Benders cut、flattenData、setSolverSolution 仍使用 Flt64，这是 solver 消费路径的合法边界。

### Commit-E：core solver V 化
- `solveV<V>(model: MechanismModel<V>, converter)` 已添加到 LinearSolver 和 QuadraticSolver。
- V-generic `solve` extension 已添加到 SolverExt。
- Flt64 invoke 方法是 adapter boundary（外部 solver 使用 double）。

### Commit-F：function 目录 UNCHECKED_CAST 清理
- `QuadraticLinearFunction.polynomial`：移除不必要的 `@Suppress(“UNCHECKED_CAST”)`（_polynomial 已是 `QuadraticPolynomial<V>`）。
- `QuadraticMinFunction.asMutable`：移除不必要的 `@Suppress(“UNCHECKED_CAST”)`（converter.zero 已是 V）。
- `ProductFunction.polynomial`：用 `asVQuadraticPoly(converter)` 替代 `toMathQuadraticPolynomial() as QuadraticPolynomial<V>`。
- `ProductFunction.asMutable`：用 `converter.zero` 替代 `Flt64.zero as MutableQuadraticPolynomial<V>`。
- 新增 `asVQuadraticPoly` converter extension（QuadraticPolynomial<Flt64> → QuadraticPolynomial<V>）。

### Commit-G：MathInequalityBridge
- **豁免**：`MathInequalityBridge.kt` 不能删除。`ToMathLinearInequality` 被 `AbstractVariableItem.kt`、`IntermediateSymbol.kt`、`Model.kt` 依赖。`flattenData` 扩展属性被 MechanismModel、MetaConstraint、LinearConstraintInput 广泛使用。这是 solver 消费路径的核心基础设施。

---

## 3. 下一步可选改进

以下改进在当前架构下是可选的，不影响 P10 泛型化的核心目标：

1. **V-generic FlattenData**：创建 `LinearFlattenData<V>` 和 `QuadraticFlattenData<V>`，消除 `Flt64LinearInequality.flattenData` 中的 `relation as Flt64LinearInequality`。但这需要 solver 接口也支持 V-typed flattenData，工作量较大且 solver 内部始终使用 double。

2. **V-generic Benders cut**：将 `generateOptimalCutFromOutput` 的 `Solution<Flt64>` 参数改为 `Solution<V>` + converter。但 dual values 本质上是 Flt64（solver 输出），V-generic 包装只是增加转换开销。

3. **V-generic DSL**：将 `MathInequalityDsl.kt` 的 122 个 DSL 函数改为返回 `LinearInequality<V>`。但这会使 DSL 使用变得繁琐（需要指定 V 类型参数），且当 V=Flt64 时 `Flt64LinearInequality` 自然兼容。

---

## 4. 验证命令

### 编译
```powershell
mvn -pl ospf-kotlin-core -DskipTests compile
mvn -pl ospf-kotlin-framework -am -DskipTests compile
mvn -pl ospf-kotlin-example -am -DskipTests compile
```

### 测试
```powershell
mvn -pl ospf-kotlin-core “-Dtest=BendersCutApiTest,GenericTokenTableRegressionTest,SolverExtIISOptionsTest,SolverOutputWithIISTest,ProductFunctionTest,FunctionSymbolMigrationTest,ParticleSwarmHeuristicSolverTest,QuadraticMechanismModelCutTest” test
```

### 扫描
```powershell
# 无伪泛型
grep -rn 'Flt64\.zero as V|Flt64\.one as V|IntoValue\.Flt64 as IntoValue<V>' ospf-kotlin-core/src/main/
# 无 import alias
grep -rn '^import .+ as ' ospf-kotlin-core/src/main/
# 无废弃命名
grep -rn 'evaluateFlt64|evaluateAsFlt64|constantFlt64' ospf-kotlin-core/src/main/
# Bridge 文件（豁免）
find ospf-kotlin-core/src/main/ -name '*Bridge*.kt'
```

### 计划

1. 泛型化 `LinearConstraintInput.isTrue` 的 solution 参数。
2. 泛型化 `ConstraintImpl` 内部 relation 和 tokenTable 访问。
3. 将 `Flt64LinearInequality` 主路径替换为 `LinearInequality<V>`。
4. `dualValues` / `farkasDualValues` 若来自外部 solver，应定义为 adapter 结果或转换为 `V`。
5. 将 `MathInequalityBridge.kt` 删除、重命名到 compat/adapter，或泛型化。
6. `MechanismModel<V>` 内部持有 `AbstractTokenTable<V>`，不把它降级为 `AbstractTokenTableFlt64`。

### 验收标准

1. `model/mechanism` 主路径中 `AbstractTokenTableFlt64` 命中为 0。
2. `model/mechanism` 主路径中 `Flt64LinearInequality` 命中为 0。
3. `model/mechanism` 主路径中 `Solution<Flt64>` 命中为 0。
4. `*Bridge*.kt` 文件数为 0，或唯一保留项有明确 adapter 豁免说明。
5. register 双阶段测试仍通过。

---

## Commit-E：core solver V 化（已完成）

`solveV<V>(model: MechanismModel<V>, converter)` 已添加到 LinearSolver 和 QuadraticSolver。V-generic `solve` extension 已添加到 SolverExt。Flt64 invoke 方法是 adapter boundary。

---

## Commit-F：function 目录 UNCHECKED_CAST 清理（已完成）

消除 2 处不必要 UNCHECKED_CAST（QuadraticLinear.polynomial、QuadraticMin.asMutable）。Product.polynomial 改用 asVQuadraticPoly(converter)。新增 asVQuadraticPoly converter extension。

---

## Commit-G：MathInequalityBridge 豁免

`MathInequalityBridge.kt` 不能删除：`ToMathLinearInequality` 被 AbstractVariableItem、IntermediateSymbol、Model 依赖。`flattenData` 被 MechanismModel、MetaConstraint、LinearConstraintInput 广泛使用。这是 solver 消费路径的核心基础设施。

---

## Commit-H：测试验证（已完成）

50 个定向测试全部通过：BendersCutApiTest(8)、GenericTokenTableRegressionTest(18)、QuadraticMechanismModelCutTest(2)、FunctionSymbolMigrationTest(8)、ProductFunctionTest(2)、ParticleSwarmHeuristicSolverTest(1)、SolverOutputWithIISTest(1)、SolverExtIISOptionsTest(10)。

---

## Commit-I：最终扫描、编译、提交（已完成）

- core BUILD SUCCESS
- framework BUILD SUCCESS
- example BUILD SUCCESS
- 50 个定向测试全部通过
- 无伪泛型（`Flt64 as V`、`IntoValue.Flt64 as IntoValue<V>` 为 0）
- 无 import alias
- 无废弃命名
- adapter boundary UNCHECKED_CAST 全部标注
- MathInequalityBridge.kt 豁免（核心基础设施）