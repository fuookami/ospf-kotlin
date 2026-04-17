# C7/C8 Regression and Guard Entry

## C7 回归脚本

脚本路径：

- `ospf-kotlin-core/scripts/run-c7-regression.ps1`

默认执行顺序：

1. `mvn -pl ospf-kotlin-core -am -q test`
2. `mvn -pl ospf-kotlin-framework -am -q -DskipTests compile`
3. `mvn -pl ospf-kotlin-core-plugin -am -q -DskipTests compile`

可选参数：

- `-SkipFramework`
- `-SkipPlugin`

示例：

```powershell
pwsh ./ospf-kotlin-core/scripts/run-c7-regression.ps1
pwsh ./ospf-kotlin-core/scripts/run-c7-regression.ps1 -SkipPlugin
```

## C8 门禁脚本

脚本路径：

- `ospf-kotlin-core/scripts/check-c8-guards.ps1`

当前门禁规则（针对增量 diff）：

1. 禁止在 API 主入口文件新增 `AbstractLinearPolynomial` / `AbstractQuadraticPolynomial` 暴露。
2. 禁止在 `Polynomial.kt` 之外新增 `.cells` 主路径调用。
3. 禁止在 core 主路径新增 `Double` 固化。

示例：

```powershell
pwsh ./ospf-kotlin-core/scripts/check-c8-guards.ps1 -BaseRef HEAD~1
```

## CI 工作流

工作流文件：

- `.github/workflows/core-refactor-guards.yml`

行为：

1. 先跑 C7 回归脚本。
2. 再跑 C8 增量门禁脚本（PR 使用 base sha，push 使用 `HEAD~1`）。
