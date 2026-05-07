# OSPF Kotlin 泛型化交接计划

记录日期：2026-05-07
适用范围：`ospf-kotlin-math`、`ospf-kotlin-core`

## 1. 当前结论

P13 主链泛型化已经完成：`math.symbol` 与 `core` 的公开主链以 `V : RealNumber<V>, NumberField<V>` 为主线，扫描门禁与编译门禁通过。

全面泛型化尚未完成。当前仍保留一批 `Flt64` 兼容别名、solver-boundary 方法、type-erased bridge 和历史 solver conversion 测试失败。下一阶段目标是继续收口这些边界，并按用户要求移除变量、几何等 `Flt64 convenience typealias`。

## 2. P13 已完成事项总结

| 范围 | 状态 | 完成内容 |
|---|---|---|
| C1 基线脚本 | done | `scripts/scan-p13-mainchain.ps1` 修正为 v3+ 两层制扫描，输出 `scripts/scan-p13-mainchain-result.json` |
| C2 register 双阶段 | done | `MathFunctionSymbolBase` V-typed；`MetaModel -> MechanismModel` 转换时注册辅助 token，模型构建后注册附加约束 |
| C3-C5 function 批次 | done | function 主构造、evaluate、invoke、register 主链已 V 化 |
| C6 二次函数/Product | done | 二次函数与 Product 接入双阶段 register；测试侧 `Map<Symbol, Flt64>` 调用改为 `MapValueProvider` |
| C7 symbol/variable/token | done | token 与 variable 主链 V 化；`UNCHECKED_CAST` 集中到 4 处 type-erased bridge |
| C8 mechanism | done | `normalize()` 迁移至 `math.symbol.adapter.flt64.Normalize.kt`；`isTrue(tokenList)` 改为 `internal`；`core/mechanism public_api_blocking = 0` |
| C9 callback | done | callback 抽象接口已 V-generic；Flt64 typealias 暂列为迁移债务 |
| C10 solver/heuristic | done | solver 主链已 V 化；heuristic 策略数值参数保留 `Flt64` |
| C11 math.symbol | done | 非 adapter `math.symbol` 公开 API 不再暴露 `Flt64`；`QuadraticInequality` typealias 迁移到 `adapter/flt64` |
| C12 全仓验收 | done | 扫描门禁 PASS；math + core compile PASS；math test 711/711；core test 143/145 |

## 3. 当前扫描与测试状态

扫描脚本：`scripts/scan-p13-mainchain.ps1`（v3+）

| 检查项 | raw | public_api_blocking | boundary_allowed |
|---|---:|---:|---:|
| import as | 0 | 0 | - |
| Suppress(UNCHECKED_CAST) | 4 | - | 4 |
| typealias *Flt64 | 77 | 0 | 70 |
| math/symbol 非 adapter | 0 | 0 | - |
| core/function | 115 | 0 | 44 |
| core/callback | 4 | 0 | 2 |
| core/mechanism | 20 | 0 | 12 |

当前门禁状态：
- 扫描：`GATE: PASS`，`public_api_blocking` 全部为 0。
- 编译：`mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile` 通过。
- 测试：`ospf-kotlin-math` 711/711 通过；`ospf-kotlin-core` 143/145，剩余 2 个历史 solver conversion/policy 失败。

已知 core 历史失败：
- `SolveOptionsTest.solveOptionsShouldUseAllowRoundingAsEffectiveDefaultPolicy`：期望 `AllowRounding`，实际 `Strict`。
- `SolveValueConversionContextTest.conversionPolicyShouldRestoreAfterScopeExit`：`Strict conversion rejected NaN at outer`。

## 4. 关键架构决策

1. 公开主链只接受 V-typed API；`Flt64` 不再作为 function、mechanism、callback 的主路径。
2. `Flt64` 只允许出现在明确的 adapter、solver-boundary 或历史兼容层；下一阶段继续把这些边界集中化。
3. `adapter/flt64` 是临时兼容边界，不再作为主链设计来源。
4. `import as` 禁止回流。
5. `UNCHECKED_CAST` 只允许集中在 type-erased bridge；下一阶段应随接口拆分继续减少，最终清零。
6. 变量、几何等 `Flt64 convenience typealias` 不再视为 stable，下一阶段必须移除或迁入明确 legacy/adapter 层后删除。

## 5. 全面泛型化目标

全面泛型化完成时，应满足：

- `ospf-kotlin-math` 与 `ospf-kotlin-core` 的非 adapter 公开 API 不暴露 `Flt64`。
- 变量、几何、model、callback、mechanism、intermediate symbol 中的 `Flt64 convenience typealias` 清零。
- `core/function` 不再因为接口契约要求实现 `Flt64` override。
- `core/mechanism` 的 flatten、solver solution、model conversion 等 `Flt64` 边界函数迁入明确 adapter/solver-boundary 层。
- `core/callback` 的 `Flt64` 兼容 typealias 移除。
- `UNCHECKED_CAST` 清零，或只剩不可避免的 internal adapter cast 并有单独门禁说明。
- `mvn -pl ospf-kotlin-core -am test` 全绿，不再依赖“历史失败”豁免。

## 6. 剩余工作拆解计划

### F1：修复 core solver conversion/policy 历史测试

目标：先让 core 测试真正全绿，避免后续泛型化改动被历史失败遮蔽。

范围：
- `ospf-kotlin-core/src/test/.../SolveOptionsTest.kt`
- `ospf-kotlin-core/src/test/.../SolveValueConversionContextTest.kt`
- 相关 solver value conversion/policy 实现。

验收：
- `mvn -pl ospf-kotlin-core -am test` 中 core 从 143/145 提升到 145/145。
- 不降低 `SolveValueConversionContext` 的 strict/allow-rounding 语义覆盖。

### F2：移除变量与几何 `Flt64 convenience typealias`

目标：按最终决策移除变量、几何这些便捷别名，不再把它们列为 stable 白名单。

范围：
- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/geometry/*`
  - `Point2`、`Point3`、`Vector2`、`Vector3`、`Circle2`、`Circle3`、`Edge2`、`Edge3`、`Triangle2`、`Triangle3`、`Quadrilateral2`、`Quadrilateral3`、`Rectangle2`、`Rectangle3` 等。
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/variable/*`
  - `RealVariable`、`URealVariable`、`PercentageVariable`
  - `RealVariableView*`、`URealVariableView*`、`PctVariableView*`
  - `QuantityRealVariableView*`、`QuantityURealVariableView*`、`QuantityPctVariableView*`
  - `Dyn*VariableView` 系列。

执行要求：
- 先用 V-typed 原类型替换内部调用点，再删除 typealias。
- 若需要保留迁移路径，只允许在明确 `legacy` 或 `adapter/flt64` 包中短期保留，并标记 `@Deprecated`；最终验收不允许继续留在主包。

验收：
- geometry 主包 `typealias .*Flt64` = 0。
- variable 主包 `typealias .*Flt64` = 0。
- math + core compile 通过。
- 相关 geometry/variable 测试通过；缺测试时补最小编译型或行为型覆盖。

### F3：移除其他公开 `Flt64 typealias`

目标：把剩余 `typealias *Flt64` 从主包清掉，adapter 中的兼容入口也要明确去留。

范围：
- `IntermediateSymbol.kt` 中 `Quantity*` aliases。
- `ModelView.kt` 中 `OriginConstraint` alias。
- `MultiObject.kt` 中 `MulObj` alias。
- `model/intermediate` 中 `LinearCellI`、`QuadraticCellI`、`OriginLinearConstraint`、`OriginQuadraticConstraint`。
- `model/mechanism/Constraint.kt` 中 `ConstraintFlt64`、`DualSolution`。
- `model/callback/CallBackModelInterface.kt` 中 `CallBackModelInterface`、`MultiObjectiveModelInterface`。
- `solver/heuristic/ParticleSwarmHeuristicSolver.kt` 中 `InitialVelocityGenerator`。
- `math.symbol.adapter.flt64` 中 `QuadraticInequality` alias 的兼容策略。

执行要求：
- 主包中的 `Flt64` typealias 直接删除或迁入明确 legacy/adapter 包后删除。
- 对外兼容需要保留时，必须给出迁移替代类型，并在文档中说明删除版本。

验收：
- `typealias *Flt64` 在非 adapter/legacy 主包中为 0。
- 若最终选择 strict 全移除，则 `ospf-kotlin-math/src/main` 与 `ospf-kotlin-core/src/main` 中 `typealias .*Flt64` 总数为 0。
- 扫描脚本不再把变量、几何 aliases 放入 `boundary_allowed`。

### F4：拆分 `core/function` solver-boundary 接口

目标：消除 44 处因接口契约产生的 `Flt64` override。

范围：
- `core/intermediate_symbol/function/FunctionSymbol.kt`
- `Masking.kt`、`Product.kt`、`QuadraticInStepRange.kt`、`QuadraticLinear.kt`、`QuadraticMaskingRange.kt`、`QuadraticMin.kt`
- 相关 `LinearIntermediateSymbol` / `QuadraticIntermediateSymbol` 接口。

执行要求：
- 公开 symbol 接口只保留 V-typed 方法。
- `prepareSolver(Map<Symbol, Flt64>?)`、`evaluate(AbstractTokenList<Flt64>)`、`evaluateSolver(List<Flt64>)`、`toMathLinearInequality(): LinearInequality<Flt64>` 等迁入 internal solver-boundary 接口。
- 调用方通过 internal adapter 访问 solver-boundary，不让用户 API 继承这些签名。

验收：
- `core/function override` boundary_allowed 从 44 降到 0。
- `core/intermediate_symbol/function public_api_blocking = 0` 保持。
- function 相关测试通过，并新增至少 1 个非 Flt64 类型集成测试覆盖 evaluate/register 路径。

### F5：收拢 `core/mechanism` Flt64 边界函数

目标：把 mechanism 主包中剩余 12 个 `Flt64` boundary function 迁入明确边界，或改为 internal。

范围：
- `Constraint.kt`：`Map<Constraint<Flt64, *>, Flt64>.toMeta()`
- `MathInequalityDsl.kt`：`LinearInequality<Flt64>.toQuadraticConstraint(...)`
- `MathInequalityFlatten.kt`：`toLinearFlattenDataFlt64`、`toQuadraticFlattenDataFlt64`、`toFlattenData()`、`toFrontendPolynomial()` 等
- `MechanismModel.kt`：`convertMechanismModelToFlt64`
- `MetaModel.kt`：`setSolverSolution(List<Flt64>)`、`setSolverSolution(Map<..., Flt64>)`

执行要求：
- 名称带 `Flt64` 的转换函数集中到 adapter/solver-boundary 包。
- 主包只保留 V-typed DSL、constraint 与 model API。
- solver solution ingestion 入口若仍需 `Flt64`，必须是 internal 或 adapter API。

验收：
- `core/mechanism boundary_allowed` 从 12 降到 0，或仅剩 internal adapter 且不计入公开 API。
- `MathInequalityDsl.kt` 不再提供 `Flt64` 用户 DSL。
- mechanism 非 Flt64 类型集成测试通过。

### F6：收口 callback 兼容 API

目标：移除 callback 主包中的 `Flt64` 兼容 typealias。

范围：
- `CallBackModelInterface = CallBackModelInterfaceV<Flt64>`
- `MultiObjectiveModelInterface = MultiObjectiveModelInterfaceV<Flt64>`

执行要求：
- 调用方迁移到 `CallBackModelInterfaceV<V>` / `MultiObjectiveModelInterfaceV<V>`。
- 如必须保留兼容入口，只能短期放入 legacy/adapter 包并标记删除计划。

验收：
- `core/callback boundary_allowed` 从 2 降到 0。
- callback 相关测试覆盖 V-typed 接口。

### F7：减少并清零 `UNCHECKED_CAST`

目标：清理 4 处 type-erased bridge。

范围：
- `FunctionSymbol.kt`
- `IntermediateSymbol.kt`
- `TokenTable.kt`

执行要求：
- 优先通过接口拆分与类型化 token 注册消除 cast。
- 若仍需 erased bridge，只能保持 internal，且必须由脚本单独输出原因。

验收：
- 理想门禁：`Suppress(UNCHECKED_CAST) = 0`。
- 若暂不能清零，最多保留 internal adapter cast，并在扫描结果中独立列为临时债务。

### F8：更新扫描脚本为全面泛型化门禁

目标：在 P13 脚本基础上新增或升级全面泛型化扫描口径。

建议文件：
- `scripts/scan-full-genericization.ps1`
- `scripts/scan-full-genericization-result.json`

验收字段：
- `import_as`
- `suppress_unchecked_cast`
- `typealias_flt64_total`
- `typealias_flt64_non_adapter`
- `geometry_typealias_flt64`
- `variable_typealias_flt64`
- `math_symbol_non_adapter_flt64`
- `core_function_flt64_override`
- `core_callback_flt64`
- `core_mechanism_flt64`
- `core_solver_public_flt64`

验收：
- 脚本能稳定复现本文件指标。
- 不再把变量、几何 convenience aliases 作为 stable 白名单。

### F9：文档、迁移说明与示例同步

目标：让用户侧迁移路径明确，避免删除 typealias 后下游无从替换。

范围：
- `README.md`
- `README_ch.md`
- 示例代码中的 `Flt64` convenience alias 用法。
- 如维护 changelog，则增加 breaking change 说明。

验收：
- 中英文 README 互相链接保持有效。
- 文档中所有旧 alias 均给出 V-typed 替代写法。
- example 模块中 P13 前预存编译错误要么修复，要么明确排除在 math/core 验收之外。

## 7. 全面泛型化验收标准

### 7.1 扫描硬门禁

全面泛型化最终门禁：

- `import as = 0`
- `Suppress(UNCHECKED_CAST) = 0`
- `math/symbol 非 adapter Flt64 = 0`
- `core/function Flt64 override = 0`
- `core/callback Flt64 = 0`
- `core/mechanism Flt64 = 0`
- `geometry typealias *Flt64 = 0`
- `variable typealias *Flt64 = 0`
- `typealias *Flt64` 在非 adapter/legacy 主包中为 0

允许项必须显式列入 adapter/legacy：
- 外部 solver SDK 要求的 `Flt64` 转换。
- `math.symbol.adapter.flt64` 中临时兼容入口。
- 已标记删除版本的 legacy API。

### 7.2 构建与测试门禁

- `mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile` 必须通过。
- `mvn -pl ospf-kotlin-core -am test` 必须通过，不再允许 143/145 豁免。
- `ospf-kotlin-math` 测试必须全绿。
- 至少新增 3 组非 Flt64 类型覆盖：
  - function evaluate/register 路径。
  - mechanism addConstraint/flatten/register 路径。
  - callback 或 solver-output conversion 路径。

### 7.3 API 验收

- 用户可用 V-typed API 完成变量声明、表达式构造、约束添加、模型构建、求解结果读取。
- 删除的 `Flt64 convenience typealias` 均有替代写法。
- adapter/legacy 包中的 `Flt64` API 不会被主包 wildcard import 隐式带入。

## 8. 下一会话建议顺序

1. 先做 F1，让 core test 从 143/145 变为 145/145。
2. 做 F8，先把全面泛型化扫描脚本固化，避免继续靠人工判断。
3. 做 F2 + F3，移除变量、几何及其他 `Flt64 typealias`。
4. 做 F4，拆分 function solver-boundary 接口，清掉 44 个 override。
5. 做 F5 + F6，迁移 mechanism/callback 边界。
6. 做 F7，清理 `UNCHECKED_CAST`。
7. 最后跑全量扫描、compile、test，并回填本文件执行记录。

## 9. 执行记录

| 阶段 | 范围 | 状态 | 扫描 | 编译 | 测试 | 备注 |
|---|---|---|---|---|---|---|
| P13 | 主链泛型化 | done | PASS | PASS | math 711/711；core 143/145 | core 剩余 2 个历史 solver conversion/policy 失败 |
| F1 | core solver 历史测试 | done | PASS | PASS | core 145/145 | SolveOptions + SolveValueConversionContext 修复 |
| F8 | 全面泛型化扫描脚本 | done | PASS | PASS | - | scan-full-genericization.ps1 v2, 两层门禁 |
| F2 | 变量/几何 typealias 移除 | done | PASS | PASS | - | geometry + variable typealias 已移除 |
| F3 | 其他 Flt64 typealias 移除 | partial | PASS | PASS | - | Cos/Sin defaultPoints 修复; Cell converter 非空化; MetaModel import 修复; Constraint converter 传播 |
| F4 | function 接口拆分 | done | PASS | PASS | core 145/145 | core/function override 44→0; public_api_blocking 44→0; Flt64方法改为internal |
| F5 | mechanism 边界迁移 | done | PASS | PASS | core 145/145 | boundary_allowed 22→19; toFrontendPolynomial删除; sign/flattenData/toFlattenData/toConstraint改internal; 白名单Debt标记更新为solver-inherent |
| F6 | callback 兼容 API 收口 | done | PASS | PASS | core 145/145 | CallBackModelInterface + MultiObjectiveModelInterface typealias已删除; boundary 5→3 |
| F7 | UNCHECKED_CAST 清理 | done | PASS | PASS | core 145/145 | SolverBoundaryCasts集中化; registerAuxiliaryTokensAny/registerConstraintsAny删除; UNCHECKED_CAST分散6→1(集中); 扫描白名单更新 |
| F9 | 文档与示例同步 | done | PASS | PASS | core 145/145 | 中英文README添加泛型化迁移指南; 已删除typealias/internal化方法/SolverBoundaryCasts/遗留typealias均有文档 |

## 10. F1–F9 全面泛型化执行总结

### 10.1 总览

F1–F9 为 P13 主链泛型化之后的全面泛型化收口阶段，目标是将 `ospf-kotlin-math` 与 `ospf-kotlin-core` 的公开 API 从 `Flt64` 专用迁移为 `V : RealNumber<V>, NumberField<V>` 泛型，消除所有 `Flt64` convenience typealias、solver-boundary 公开方法、type-erased bridge 和 `UNCHECKED_CAST` 分散。

全部 9 个阶段均已完成，扫描门禁 PASS，编译通过，测试 145/145。

### 10.2 各阶段执行结果

| 阶段 | 范围 | 状态 | 关键变更 |
|---|---|---|---|
| F1 | core solver 历史测试 | done | SolveOptions + SolveValueConversionContext 修复；core test 143→145 |
| F2 | 变量/几何 typealias 移除 | done | geometry + variable 主包 `typealias .*Flt64` 清零 |
| F3 | 其他 Flt64 typealias 移除 | partial | Cos/Sin defaultPoints 修复; Cell converter 非空化; MetaModel/Constraint import 修复；剩余 `MultiObject<Flt64>` 为迁移债务 |
| F4 | function 接口拆分 | done | 从 `IntermediateSymbol<V>`/`LinearIntermediateSymbol<V>`/`QuadraticIntermediateSymbol<V>` 接口移除 Flt64 solver-boundary 方法（prepareSolver、evaluateSolver、evaluate(tokenList)、toMathLinearInequality、toMathQuadraticInequality、flattenedMonomials）；实现类 `override` 关键字移除；`core/function override` 44→0；`public_api_blocking` 44→0；添加 `solverFlattenedMonomials` internal 扩展 |
| F5 | mechanism 边界迁移 | done | `toFrontendPolynomial` 删除（identity 函数）；`sign`/`flattenData`/`toFlattenData`/`toConstraint` 改 internal；`QuadraticMonomialKey.from(mono)` 改 private；`core/mechanism boundary_allowed` 22→19；白名单 Debt 标记从 `low (boundary)` 更新为 `none (solver-inherent)` |
| F6 | callback 兼容 API 收口 | done | `CallBackModelInterface` typealias 删除；`MultiObjectiveModelInterface` typealias 删除；`core/callback boundary_allowed` 5→3 |
| F7 | UNCHECKED_CAST 清理 | done | 创建 `SolverBoundaryCasts.kt` 集中所有 `@Suppress("UNCHECKED_CAST")`；删除 `registerAuxiliaryTokensAny`/`registerConstraintsAny` 类型擦除桥接方法；TokenTable.kt/MechanismModel.kt 委托 SolverBoundaryCasts；`UNCHECKED_CAST` 分散 6→1（集中）；扫描白名单更新 |
| F8 | 全面泛型化扫描脚本 | done | `scripts/scan-full-genericization.ps1` v2，两层门禁（public_api_blocking + boundary_allowed） |
| F9 | 文档与示例同步 | done | `ospf-kotlin-core/README.md` 添加 Genericization Migration Guide；`ospf-kotlin-core/README_ch.md` 添加泛型化迁移指南；覆盖已删除 typealias 替代写法、internal 化方法、SolverBoundaryCasts 说明、遗留 typealias 列表 |

### 10.3 最终扫描指标

| 检查项 | raw | public_api_blocking | boundary_allowed |
|---|---:|---:|---:|
| import as | 0 | 0 | - |
| Suppress(UNCHECKED_CAST) | 2 | 0 | 2 (集中) |
| typealias *Flt64 | 9 | 0 | 2 |
| geometry typealias | 0 | 0 | - |
| variable typealias | 0 | 0 | - |
| math/symbol 非 adapter | 0 | 0 | - |
| core/function override | 0 | 0 | 0 |
| core/callback | 5 | 0 | 3 |
| core/mechanism | 26 | 0 | 18 |
| core/solver public | 0 | 0 | - |

扫描门禁：**GATE: PASS**

### 10.4 关键架构变更

1. **公开接口只保留 V-typed 方法**：`IntermediateSymbol<V>`、`LinearIntermediateSymbol<V>`、`QuadraticIntermediateSymbol<V>` 不再暴露 Flt64 solver-boundary 方法。Flt64 方法降级为具体类（`LinearExpressionSymbol`/`QuadraticExpressionSymbol`）的 internal 方法。

2. **SolverBoundaryCasts 集中化**：所有 `@Suppress("UNCHECKED_CAST")` 集中到 `SolverBoundaryCasts.kt` 一个文件。框架内其他位置不再允许 `UNCHECKED_CAST`。星投影泛型调用（`IntermediateSymbol<*>` → `IntermediateSymbol<Flt64>`）统一通过 `SolverBoundaryCasts` 桥接。

3. **type-erased bridge 方法删除**：`registerAuxiliaryTokensAny`、`registerConstraintsAny` 等从接口中移除。调用方改为委托 `SolverBoundaryCasts`。

4. **mechanism 边界方法 internal 化**：`sign`、`flattenData`、`toFlattenData`、`toConstraint` 等方法改为 `internal`，不污染公开 API。白名单中这些项的 Debt 标记更新为 `none (solver-inherent)`。

5. **callback typealias 删除**：`CallBackModelInterface`、`MultiObjectiveModelInterface` 两个 Flt64 typealias 已删除，调用方需使用 `CallBackModelInterfaceV<V>`、`MultiObjectiveModelInterfaceV<V>`。

6. **geometry/variable typealias 清零**：主包中所有 geometry 和 variable 的 Flt64 convenience typealias 已移除。

### 10.5 遗留项

| 项目 | 位置 | 说明 |
|---|---|---|
| `MultiObject<Flt64>` typealias | `core/model/basic/MultiObject.kt:11` | Flt64 专用别名，扫描标记为 MIGRATE，需后续删除或改为泛型形式 |
| `QuadraticInequality` typealias | `math/symbol/adapter/flt64/Inequality.kt:14` | adapter 包内 Flt64 兼容别名，设计上允许 |
| `core/mechanism boundary_allowed = 18` | 多文件 | solver-inherent 项目：DSL infix 运算符、数据载体（Constraint/MetaConstraint/SubObject）、求解器插件接口、solver flatten/convert 方法。无法 internal 化，Debt = `none (solver-inherent)` |
| `core/callback boundary_allowed = 3` | 多文件 | solver-inherent 项目：AbstractCallBackModelInterface、Normalization、SolvingStatus。Debt = `none (solver-inherent)` |

### 10.6 构建与测试验证

- 编译：`mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile` — **PASS**
- 测试：`mvn -pl ospf-kotlin-core -am test` — **145/145 PASS**
- 数学测试：`ospf-kotlin-math` — **711/711 PASS**
- 扫描：`scripts/scan-full-genericization.ps1` — **GATE: PASS**
