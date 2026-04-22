# P3-1 Example Import 迁移映射表

日期：2026-04-22（修订版，修正统计与插件路径）

---

## 1. frontend -> 当前包 映射

| 旧 import 路径 | 新 import 路径 | import 行数 |
|----------------|---------------|------------|
| `core.frontend.model.mechanism.*` | `core.intermediate_model.*` | 189 |
| `core.frontend.expression.polynomial.*` | `math.symbol.polynomial.*` | 103 |
| `core.frontend.inequality.*` | `math.symbol.inequality.*` | 77 |
| `core.frontend.expression.monomial.*` | `math.symbol.monomial.*` | 73 |
| `core.frontend.expression.symbol.*` | `core.intermediate_symbol.*` | 52 |
| `core.frontend.variable.*` | `core.variable.*` | 50 |
| `core.frontend.expression.symbol.linear_function.*` | `core.intermediate_symbol.function.*` | 46 |
| `core.frontend.model.callback.*` | `core.model.callback.*` | 2 |
| `core.frontend.model.*` | `core.model.*` | 2 |
| `core.frontend.expression.symbol.quadratic_function.*` | `core.intermediate_symbol.function.*` | 1 |
| `core.frontend.expression.symbol.LinearIntermediateSymbols1` | `core.intermediate_symbol.LinearIntermediateSymbols1` | 1 |

**合计**：596 条 import，191 文件

**注意**：`core.frontend.model.mechanism.*` 是最高频引用（189 行），迁移后需确认 `core.intermediate_model.*` 下的类型名是否与旧 frontend 一致。根据 P1 阶段工作，类型名已保持兼容。

## 2. backend -> 当前包 映射

| 旧 import 路径 | 新 import 路径 | import 行数 | 是否需要迁移 |
|----------------|---------------|------------|-------------|
| `core.backend.plugins.scip.*` | — | 46 | ❌ 无需迁移（当前仓库仍为 `core.backend.plugins.scip.*`） |
| `core.backend.solver.config.*` | `core.solver.config.*` | 7 | ✅ |
| `core.backend.plugins.gurobi.*` | — | 3 | ❌ 无需迁移（当前仓库仍为 `core.backend.plugins.gurobi.*`） |
| `core.backend.plugins.heuristic.pso.*` | — | 2 | ❌ 无需迁移（当前仓库仍为 `core.backend.plugins.heuristic.*`） |
| `core.backend.intermediate_model.*` | `core.intermediate_model.*` | 2 | ✅ |
| `core.backend.solver.*` | `core.solver.*` | 1 | ✅ |

**合计**：61 条 import，50 文件。其中 51 条（plugins）无需迁移，10 条需要迁移。

**关键确认**：当前仓库中插件包路径仍为 `core.backend.plugins.*`（Gurobi: `fuookami.ospf.kotlin.core.backend.plugins.gurobi.*`，SCIP: `fuookami.ospf.kotlin.core.backend.plugins.scip.*`，Heuristic: `fuookami.ospf.kotlin.core.backend.plugins.heuristic.*`），与 example 引用一致，**不需要迁移**。

## 3. framework 引用（无需迁移）

framework 的包路径未发生变化，example 对 framework 的引用无需修改。

## 4. 迁移执行脚本

迁移将在 P3-5（example 迁入）时执行。批量替换规则：

```bash
# frontend -> core/math（按从长到短顺序，避免子包被提前替换）
find . -name "*.kt" -exec sed -i 's/core\.frontend\.expression\.symbol\.linear_function\./core.intermediate_symbol.function./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.expression\.symbol\.quadratic_function\./core.intermediate_symbol.function./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.expression\.symbol\.LinearIntermediateSymbols1/core.intermediate_symbol.LinearIntermediateSymbols1/g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.expression\.symbol\./core.intermediate_symbol./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.expression\.polynomial\./math.symbol.polynomial./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.expression\.monomial\./math.symbol.monomial./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.model\.mechanism\./core.intermediate_model./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.model\.callback\./core.model.callback./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.inequality\./math.symbol.inequality./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.variable\./core.variable./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.frontend\.model\./core.model./g' {} +

# backend -> core（仅迁移 solver 和 intermediate_model，plugins 不动）
find . -name "*.kt" -exec sed -i 's/core\.backend\.intermediate_model\./core.intermediate_model./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.backend\.solver\.config\./core.solver.config./g' {} +
find . -name "*.kt" -exec sed -i 's/core\.backend\.solver\./core.solver./g' {} +
```

## 5. 已确认项

1. ~~`core.backend.plugins.gurobi.*` -> 当前仓库中 gurobi 插件的实际包路径~~ → **已确认**：当前仓库仍为 `core.backend.plugins.gurobi.*`，无需迁移
2. ~~`core.backend.plugins.scip.*` -> 当前仓库中 scip 插件的实际包路径~~ → **已确认**：当前仓库仍为 `core.backend.plugins.scip.*`，无需迁移
3. ~~`core.backend.plugins.heuristic.pso.*` -> 当前仓库中 PSO 的实际包路径~~ → **已确认**：当前仓库仍为 `core.backend.plugins.heuristic.*`，无需迁移
4. ~~`core.frontend.expression.symbol.LinearIntermediateSymbols1` -> 当前对应类型名~~ → **已确认**：`core.intermediate_symbol.LinearIntermediateSymbols1`（typealias in SymbolCombination.kt）
5. ~~example POM 中的 `ospf-kotlin-starter` 依赖是否在当前仓库中存在~~ → 待 P3-5 时确认
