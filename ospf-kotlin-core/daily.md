# 泛型化交接（2026-05-12）

## 目标

将 `ospf-kotlin-math`、`ospf-kotlin-core`、`ospf-kotlin-framework` 从“以 `Flt64` 为主、外层局部泛型”的状态推进到“建模、表达式、中间模型、框架算法全链路支持泛型数值类型”。

外部求解器若只能接收 double，可继续以 `Flt64` 作为 solver adapter 原生值类型；但 `Flt64` 不应泄漏到泛型建模 API、框架算法 API、约束/目标/解输出主类型签名中。

## 总体原则

1. 泛型主类型统一记为 `V`，约束为 `where V : RealNumber<V>, V : NumberField<V>`。
2. `Flt64` 仅允许出现在兼容层、求解器适配层、`adapter.flt64` 包、测试基准与数值转换边界。
3. 建模层、机制模型层、框架算法层主 API 返回 `V` 类型结果（如 `FeasibleSolverOutput<V>`、`LinearInequality<V>`、`QuadraticInequalityOf<V>`）。
4. `IntoValue<V>` 仅作为边界转换能力，不能替代内部泛型数据结构。
5. 旧 `Flt64` API 可保留为 deprecated/compat wrapper，但不再承载唯一业务实现。

## 本轮总结

本轮已把 `ospf-kotlin-core/src/main/.../intermediate_symbol/function` 目录中的 `.toDouble()` / `fromValue(...).toDouble()` 路径清零，并补齐了对应的四类型回归与门禁测试。

关键结果：

1. `function` 目录运行时评估路径已统一到 `V` 或 `Flt64` 原生算术，不再经 `Double` 桥接。
2. `SatisfiedAmount`、`SatisfiedAmountInequality` 的计数构造已改为 `V` 类型累加（`repeatAdd`）。
3. 新增并强化门禁测试：`FunctionSymbolToDoubleBridgeGuardTest`（当前规则为禁止 `function` 目录出现 `.toDouble()`）。
4. 分支当前工作区干净，最近改动已提交（见下方“归档提交”）。

## 已完成事项（清空）

已完成事项列表已清空，不在本文件继续累积。

历史完成记录请直接以 `git log --oneline` 为准。

## 归档提交（供下个会话快速定位）

1. `48a74668` refactor: remove toDouble bridges from piecewise function builders
2. `f3c3f5f6` refactor: build satisfied-amount counts with V-typed arithmetic
3. `40237605` test: guard against fromValue toDouble bridge in function symbols
4. `8d230d87` refactor: remove remaining evaluate toDouble bridges in function symbols
5. `d081edbe` refactor: keep quadratic in-step-range evaluation V-typed
6. `e54344b2` refactor: keep univariate piecewise evaluation V-typed
7. `7eb2aea7` refactor: remove quadratic min and masking toDouble evaluate
8. `47c53a2f` refactor: keep max and masking evaluation V-typed
9. `b324a32c` refactor: keep slack function evaluation V-typed

## 未完成事项

1. `core` 非 `function` 目录仍有 `.toDouble()` 热点，需分层处理（保留边界 vs 继续泛型化）：
   - `core/model/mechanism/MathInequalityDsl.kt`
   - `core/solver/value/SolveValueConversionContext.kt`
   - `core/solver/value/SolveValueValidation.kt`
   - `core/token/Token.kt`
2. `function` 目录虽已清零，但门禁目前只覆盖该目录；尚未扩展到 `core` 其他高风险目录。
3. 尚未进行本轮“全口径慢测回归”确认（`with-slow-tests`、`with-all-slow-tests`）。
4. `framework` 主链路泛型化（列生成/Benders 组合求解器）尚未开始收口。

## 下一步计划（交接给下个会话）

1. 先做边界分层决策（半天内完成）：
   - 明确 `MathInequalityDsl`、`SolveValue*`、`Token` 中每个 `.toDouble()` 是“允许边界”还是“需迁移主链路”。
   - 输出 allowlist 草案（文件+行号级别）。

2. 扩展门禁测试（优先）：
   - 新增 `core` 级别 `.toDouble()` 扫描门禁，支持 allowlist。
   - 保持 `function` 目录为“零容忍”（不允许新增 `.toDouble()`）。

3. 逐步迁移非边界热点（如果被判定为主链路）：
   - 先 `MathInequalityDsl` 中可泛型化部分。
   - 再处理 `Token` 与 `SolveValue` 中非适配层桥接。

4. 回归与提交策略：
   - 每完成一组迁移即跑：
     - `FunctionSymbolToDoubleBridgeGuardTest`
     - 受影响模块的定向测试
   - 每次提交只包含单一主题，避免混改。

## 建议下个会话起手命令

1. `git status -sb`
2. `Get-ChildItem -Path ospf-kotlin-core/src/main -Recurse -Filter *.kt | ForEach-Object { Select-String -Path $_.FullName -Pattern '\.toDouble\(\)' }`
3. `mvn -pl ospf-kotlin-core "-Dtest=FunctionSymbolToDoubleBridgeGuardTest" test`

## 当前状态

- 分支：`rewrite-bigbang`
- 工作区：干净
- 进度：`origin/rewrite-bigbang` 之上 `ahead 52`
