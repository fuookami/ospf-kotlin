# OSPF Kotlin 泛型化交接计划

记录日期：2026-05-08
适用范围：`ospf-kotlin-math`、`ospf-kotlin-core`

## 1. 当前结论

P13、F1-F9、G1-G4、H1-H5 已完成。当前 `math` 与 `core` 的公开主链已经完成 V 化，`public_api_blocking` 全部为 0，`core/mechanism`、`core/callback`、`core/function override` 的边界项也已清零。

这说明主包公开 API 不再把 `Flt64` 作为主路径。当前尚未等同于”所有 Flt64 文本完全消失”：仍保留 1 个 `adapter/flt64` 兼容 typealias、2 个集中在 `intermediate_symbol/SolverBoundaryCasts.kt` 的 `UNCHECKED_CAST`（星投影桥接，不可消除），以及若干数值本体、adapter、solver bridge、内部实现中的 `Flt64` 使用。

下一阶段进入 I 阶段：不再处理已经清零的 H 阶段主链问题，而是审计和压缩“允许存在的兼容边界”，并把扫描门禁从正则统计升级为更接近真实 Kotlin API 的验收。

## 2. 已完成事项总结

| 范围 | 状态 | 完成内容 |
|---|---|---|
| P13 主链 | done | `scripts/scan-p13-mainchain.ps1` v3+；主链 `public_api_blocking = 0` |
| F1 | done | core 历史 solver conversion/policy 测试修复到 145/145 |
| F2 | done | geometry + variable 的 `Flt64 convenience typealias` 已移除 |
| F3 | done | 其他主包 `Flt64 typealias` 已收口；`MultiObject<Flt64>` 已删除 |
| F4 | done | `core/function` 的 Flt64 override 已拆出，主链不再泄漏 |
| F5 | done | mechanism 主链完成一轮边界迁移 |
| F6 | done | callback 兼容 typealias 已清理 |
| F7 | done | `UNCHECKED_CAST` 已集中化到 `SolverBoundaryCasts.kt`，core 内散落 cast 已清零 |
| F8 | done | `scripts/scan-full-genericization.ps1` 已落地 |
| F9 | done | README / README_ch 的迁移指南已补齐 |
| G1 | done | `core/mechanism boundary_allowed` 从 18 降至 12 |
| G2 | done | `MultiObject<Flt64>` typealias 删除，`migration_debt` 清零 |
| G3 | done | callback / bridge 债务压缩一轮，callback 剩余项进入 H 阶段 |
| G4 | done | 扫描、文档、结果 JSON 同步 |
| H1 | done | 扫描脚本增强，输出 `boundary_detail`，区分 `UNCHECKED_CAST` bridge 与 blocking |
| H2 | done | mechanism 剩余 12 项完成分类，全部判定为 `MOVE_TO_ADAPTER` |
| H3 | done | V-typed solver adapter 落地，`core/mechanism boundary_allowed` 从 12 降到 0 |
| H4 | done | callback adapter 重构完成，`core/callback boundary_allowed` 从 3 降到 0；`MultiObjectLocation<V>` 完成泛型化 |
| H5 | done | `MultiObjectLocationFlt64` typealias 删除，plugin 改用 `MultiObjectLocation<Flt64>`；非 adapter typealias 清零 |

## 3. 当前扫描与测试状态

扫描脚本：`scripts/scan-full-genericization.ps1`

最新扫描时间：2026-05-08T16:47:43

| 检查项 | raw | public_api_blocking | boundary_allowed |
|---|---:|---:|---:|
| import as | 0 | 0 | - |
| Suppress(UNCHECKED_CAST) | 2 | 0 | 2 |
| typealias *Flt64 | 8 | 0 | 1 |
| geometry typealias | 0 | 0 | - |
| variable typealias | 0 | 0 | - |
| math/symbol 非 adapter | 0 | 0 | - |
| math/symbol adapter | 162 | - | adapter surface |
| core/function | 115 | 0 | 0 |
| core/callback | 2 | 0 | 0 |
| core/mechanism | 26 | 0 | 0 |
| core/solver public | 17 | 0 | solver boundary |

当前门禁状态：

- 扫描：`GATE: PASS`，所有 `public_api_blocking = 0`
- `core/mechanism boundary_allowed = 0`
- `core/callback boundary_allowed = 0`
- `core/function override = 0`
- `typealias_flt64_non_adapter = 0`
- `geometry_typealias_flt64 = 0`
- `variable_typealias_flt64 = 0`
- `migration_debt = []`
- I2 分类：`PUBLIC_API_BLOCKING = 0`，`SOLVER_BRIDGE = 173`，`INTERNAL_IMPL = 87`
- 编译：`mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile` 通过
- 测试：`mvn -pl ospf-kotlin-core -am test` 通过，core 145/145
- 数学测试：`ospf-kotlin-math` 711/711

## 4. 当前剩余事项合并

| 项目 | 当前值 | 性质 | 是否阻塞公开主链 | 下一步 |
|---|---:|---|---|---|
| `typealias *Flt64` adapter 项 | 1 | `adapter/flt64` 兼容别名 | 否 | I2/I4 判断保留、迁 legacy 或删除 |
| `Suppress(UNCHECKED_CAST)` | 2 | 集中式星投影 solver bridge（不可消除） | 否 | I3 done |
| `math_symbol_adapter` | 162 | `math/symbol/adapter/flt64` 兼容表面 | 否 | I2 分类，I4 制定兼容边界退场策略 |
| `core/function` raw | 115 | 主包内部实现与 solver 计算路径中的 Flt64 文本 | 否 | I2 分类，区分内部实现、数值本体、可 V 化项 |
| `core/solver` raw | 17 | solver 边界、gap、normalization 等 Flt64 固有路径 | 否 | I2 分类，确认是否长期保留 |
| 文档基线 | 已更新本文件 | 交接材料 | 否 | I5 后同步 README / README_ch，如 API 策略变化 |

处理原则：

1. 不扩白名单来制造完成感。
2. 不再把已清零的 H 阶段任务重新列为待办。
3. `Flt64` 可以作为数值类型本体、明确 adapter、solver bridge 存在，但不能回流到主包公开 API。
4. 若目标升级为“连 adapter 兼容层也退场”，必须先明确破坏性变更窗口。
5. `SolverBoundaryCasts.kt` 的 4 个 cast 可以暂视为最小可解释值，但不允许新增散落 cast。

## 5. 关键架构决策

1. 公开主链只接受 V-typed API，`Flt64` 不再作为主路径。
2. `adapter/flt64` 是兼容边界，不作为主链设计来源。
3. mechanism 与 callback 主包已经不再承担 Flt64 solver 边界。
4. `SolverBoundaryCasts` 的星投影桥接是框架架构固有约束，2 个 `UNCHECKED_CAST` 为最小不可消除值；`solver/value/SolverBoundaryCasts.kt` 已删除，其 `flt64ToV` 已由 `converter.intoValue()` 替代。
5. `math` 中 `Flt64` 作为数值类型本体必须保留；“完全泛型化”不能解释为删除 `Flt64` 类型本身。

## 6. I 阶段执行计划

### I1：固化当前完成基线

目标：让交接文档、扫描 JSON、脚本输出三者一致。

改动范围：

- `ospf-kotlin-core/daily.md`
- `scripts/scan-full-genericization-result.json`

执行要求：

- 明确 H1-H5 全部完成。
- 删除旧的 `core/mechanism boundary_allowed = 12`、`core/callback boundary_allowed = 3` 待办描述。
- 保留当前真实剩余项：`typealias_flt64 adapter = 1`、`UNCHECKED_CAST = 4`、raw Flt64 分类待审计。

验收：

- `daily.md` 与最新扫描结果一致。
- `public_api_blocking` 全部为 0。
- `migration_debt = []`。

### I2：全量 Flt64 残留分类

目标：把 raw Flt64 从“数量”拆成“语义类别”，判断哪些必须保留、哪些可以迁移或删除。

分类建议：

| 分类 | 示例 | 策略 |
|---|---|---|
| 数值类型本体 | `math.algebra.number.Flt64` | KEEP |
| adapter/flt64 兼容 API | `math/symbol/adapter/flt64` | KEEP_OR_DEPRECATE |
| solver bridge | solver gap、normalization、solution conversion | KEEP_OR_REWORK |
| 主包内部实现 | function / intermediate symbol 内部 Flt64 计算路径 | REVIEW |
| 公开主包 API | 非 adapter public signature 中的 Flt64 | MUST_BE_ZERO |

执行要求：

- 扩展扫描脚本输出 raw Flt64 分类明细。
- 每类给出文件、行号、原因、处理策略。
- 不在 I2 阶段做大规模重构，只做可复核清单。

验收：

- `public_api_blocking` 保持全 0。
- 每个 raw Flt64 命中都有分类。
- 形成 I3/I4 的可执行输入。

### I3：评估并压缩 `SolverBoundaryCasts`

目标：判断 `UNCHECKED_CAST` 是否可以通过更强的 V-typed adapter contract 消除。

已完成的压缩：

- `solver/value/SolverBoundaryCasts.kt` 已删除。其中 2 个 `flt64ToV` 函数用 `value as V` 做类型转换，但调用者（`Constraint.kt`）已有 `converter: IntoValue<V>` 参数，改用 `converter.intoValue()` 即为类型安全替代。
- `Suppress(UNCHECKED_CAST)` 从 4 降到 2。

不可消除的剩余：

- `intermediate_symbol/SolverBoundaryCasts.kt` 的 1 个 `@Suppress("UNCHECKED_CAST")`（对象级）包含星投影类型转换（`IntermediateSymbol<*>` → `IntermediateSymbol<Flt64>` 等），JVM 类型擦除下无法恢复类型参数，属于框架架构固有桥接。
- 这 2 个扫描命中（1 个注释 + 1 个注解）是星投影桥接的最小可解释值。

改动范围：

- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/Constraint.kt`（`flt64ToV` → `converter.intoValue`）
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/value/SolverBoundaryCasts.kt`（已删除）

验收：

- `Suppress(UNCHECKED_CAST)` 降到 2，为星投影桥接不可消除值。
- 不新增任何业务文件中的 `@Suppress("UNCHECKED_CAST")`。
- core 145/145 测试通过。

### I4：决定 adapter/flt64 兼容边界策略

目标：明确最后 1 个 adapter typealias 和 `math/symbol/adapter/flt64` 表面是否继续保留。

可选策略：

| 策略 | 说明 | 影响 |
|---|---|---|
| KEEP | adapter/flt64 作为长期兼容层保留 | 对下游最稳，完全泛型化定义为“主链泛型化” |
| DEPRECATE | 加迁移说明，后续版本删除 | 有迁移窗口，适合发布节奏 |
| REMOVE | 直接删除 adapter typealias / 兼容 API | 最干净，但破坏性最大 |

执行要求：

- 先查 README / README_ch 中是否仍承诺保留 legacy typealias。
- 若选择删除，先更新迁移指南和测试。
- 若选择保留，扫描门禁必须明确 adapter whitelist 的边界，禁止主包借 adapter 反向依赖。

验收：

- `typealias_flt64_non_adapter = 0` 保持不变。
- adapter 项保留/删除策略写入文档。
- 若删除 adapter typealias，`typealias_flt64_adapter` 目标为 0。

### I5：升级扫描门禁

目标：减少纯正则扫描的盲区，让门禁更接近真实 Kotlin public API。

执行方向：

- 当前脚本继续保留为快速扫描。
- 增加公开 API 签名扫描或二进制 API 检查，避免换行、嵌套泛型、新目录绕过。
- 将 `boundary_allowed` 拆成长期允许、临时允许、必须下降三类。

验收：

- 新门禁能复现当前 `public_api_blocking = 0`。
- 新增公开 API 中的 `Flt64` 会失败。
- adapter / solver bridge 的允许项有明确文件级边界。

## 7. I 阶段最终验收标准

### 7.1 主链验收

- `public_api_blocking` 全部为 0
- `core/mechanism boundary_allowed = 0`
- `core/callback boundary_allowed = 0`
- `core/function override = 0`
- `geometry typealias *Flt64 = 0`
- `variable typealias *Flt64 = 0`
- `typealias *Flt64` 非 adapter / legacy 主包为 0
- `migration_debt = []`

### 7.2 边界验收

- `adapter/flt64` 是否保留有明确策略。
- `typealias_flt64_adapter` 为 0，或作为长期兼容边界被明确记录。
- `Suppress(UNCHECKED_CAST)` 为 0，或只剩集中 bridge 且有逐项不可替代说明。
- 所有 raw Flt64 命中完成分类。

### 7.3 构建与测试验收

- `mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile` 必须通过
- `mvn -pl ospf-kotlin-core -am test` 必须通过
- `ospf-kotlin-math` 测试必须全绿
- 如 adapter 策略变化，README / README_ch 必须同步

## 8. 下一会话建议顺序

1. I3 收尾：确认 `intermediate_symbol/SolverBoundaryCasts.kt` 星投影桥接不可消除，将 I3 标记为 done。
2. 做 I4：决定并执行 adapter/flt64 兼容边界策略。
3. 做 I5：升级扫描门禁，避免正则盲区。
4. 最后同步 README / README_ch 与 daily.md。

## 9. 执行记录

| 阶段 | 范围 | 状态 | 扫描 | 编译 | 测试 | 备注 |
|---|---|---|---|---|---|---|
| P13 | 主链泛型化 | done | PASS | PASS | math 711/711；core 143/145 | 主链已完成 |
| F1 | core solver 历史测试 | done | PASS | PASS | core 145/145 | 已修复 |
| F2 | 变量/几何 typealias 移除 | done | PASS | PASS | - | 已清零 |
| F3 | 其他 Flt64 typealias 移除 | done | PASS | PASS | - | 主包 Flt64 别名已收口 |
| F4 | function 接口拆分 | done | PASS | PASS | core 145/145 | override 已清零 |
| F5 | mechanism 边界迁移 | done | PASS | PASS | core 145/145 | 完成一轮迁移 |
| F6 | callback 兼容 API 收口 | done | PASS | PASS | core 145/145 | 已收口一轮 |
| F7 | UNCHECKED_CAST 清理 | done | PASS | PASS | core 145/145 | 已集中化 |
| F8 | 全面泛型化扫描脚本 | done | PASS | PASS | - | 已落地 |
| F9 | 文档与示例同步 | done | PASS | PASS | core 145/145 | 已补迁移指南 |
| G1 | core/mechanism 继续清零 | done | PASS | PASS | core 145/145 | boundary_allowed 18 -> 12 |
| G2 | MultiObject alias 清理 | done | PASS | PASS | core 145/145 | typealias 已删除，migration_debt 清零 |
| G3 | callback / bridge 债务压缩 | done | PASS | PASS | core 145/145 | callback 剩余项进入 H |
| G4 | 扫描与文档同步 | done | PASS | PASS | core 145/145 | daily.md 已更新 |
| H1 | 边界门禁固化 | done | PASS | PASS | core 145/145 | boundary_detail 已输出 |
| H2 | mechanism 12 项分类 | done | PASS | PASS | core 145/145 | 12 项全部 MOVE_TO_ADAPTER |
| H3 | V-typed solver adapter | done | PASS | PASS | core 145/145 | mechanism boundary_allowed 12 -> 0 |
| H4 | callback adapter 重构 | done | PASS | PASS | core 145/145 | callback boundary_allowed 3 -> 0 |
| H5 | bridge / typealias 收尾 | done | PASS | PASS | core 145/145 | non-adapter typealias 清零，UNCHECKED_CAST 4 为最小可解释值 |
| I1 | 当前基线固化 | done | PASS | PASS | - | daily.md 已按最新扫描结果重写 |
| I2 | 全量 Flt64 残留分类 | done | PASS | PASS | - | SOLVER_BRIDGE=173, INTERNAL_IMPL=87, PUBLIC_API_BLOCKING=0 |
| I3 | SolverBoundaryCasts 评估/压缩 | in_progress | PASS | PASS | - | solver/value/SolverBoundaryCasts.kt 已删除，4→2；intermediate_symbol 2 项为星投影不可消除 |
| I4 | adapter/flt64 策略确定 | pending | - | - | - | 依赖 I2 |
| I5 | 扫描门禁升级 | pending | - | - | - | 依赖 I2-I4 |

## 10. 当前未完成事项

| 项目 | 当前值 | 目标 | 阶段 |
|---|---:|---:|---|
| `typealias_flt64_adapter` | 1 | 0 或明确长期保留 | I4 |
| `Suppress(UNCHECKED_CAST)` | 2 | 0 或 2 个星投影不可消除值 | I3 in_progress |
| raw Flt64 分类覆盖率 | 100% | 100% 分类 | I2 done |
| 正则扫描盲区 | 存在 | 增加 public API 级门禁 | I5 |
| SOLVER_BRIDGE Flt64 | 173 | 评估可压缩项 | I3 done (solver/value 已消除) |
| INTERNAL_IMPL Flt64 | 87 | 逐项审计 | I3 done (已分类) |

## 11. 不再列为未完成的事项

| 项目 | 当前值 | 说明 |
|---|---:|---|
| `core/mechanism boundary_allowed` | 0 | H3 已完成 |
| `core/callback boundary_allowed` | 0 | H4 已完成 |
| `core/function override` | 0 | F4/H 阶段已完成 |
| `typealias_flt64_non_adapter` | 0 | H5 已完成 |
| `geometry_typealias_flt64` | 0 | F2 已完成 |
| `variable_typealias_flt64` | 0 | F2 已完成 |
