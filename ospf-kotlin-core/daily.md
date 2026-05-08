# OSPF Kotlin 泛型化交接计划

记录日期：2026-05-08
适用范围：`ospf-kotlin-math`、`ospf-kotlin-core`

## 1. 当前结论

P13、F1-F9、G1-G4 已完成。当前 `math` 与 `core` 的公开主链已经 V 化，扫描、编译、测试门禁均通过。

这不等于边界接口已经彻底泛型化。当前剩余工作集中在三类：`core/mechanism boundary_allowed = 12`、`core/callback boundary_allowed = 3`、`Suppress(UNCHECKED_CAST) = 4`。这些不是当前 `public_api_blocking`，但如果目标是继续压缩 adapter / solver-boundary 表面，就需要进入 H 阶段的边界接口重构。

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
| G3 | done | callback / bridge 债务压缩一轮，callback 保持 3 |
| G4 | done | 扫描、文档、结果 JSON 同步 |

## 3. 当前扫描与测试状态

扫描脚本：`scripts/scan-full-genericization.ps1`

| 检查项 | raw | public_api_blocking | boundary_allowed |
|---|---:|---:|---:|
| import as | 0 | 0 | - |
| Suppress(UNCHECKED_CAST) | 4 | 0 | 4 |
| typealias *Flt64 | 8 | 0 | 1 |
| geometry typealias | 0 | 0 | - |
| variable typealias | 0 | 0 | - |
| math/symbol 非 adapter | 0 | 0 | - |
| core/function override | 0 | 0 | 0 |
| core/callback | 5 | 0 | 3 |
| core/mechanism | 26 | 0 | 12 |
| core/solver public | 17 | 0 | - |

当前门禁状态：
- 扫描：`GATE: PASS`，所有 `public_api_blocking = 0`
- 编译：`mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile` 通过
- 测试：`mvn -pl ospf-kotlin-core -am test` 通过，core 145/145
- 数学测试：`ospf-kotlin-math` 711/711
- 迁移债务：`migration_debt = []`

## 4. 当前未完成事项合并

| 项目 | 当前值 | 性质 | 下一步 |
|---|---:|---|---|
| `core/mechanism boundary_allowed` | 12 | solver / mechanism adapter 边界 | H2 分类，H3 迁入 V-typed solver adapter |
| `core/callback boundary_allowed` | 3 | callback 接口契约边界 | H4 重构 callback adapter |
| `Suppress(UNCHECKED_CAST)` | 4 | 集中式 type-erased bridge | H5 随 adapter 重构后继续压缩 |
| `typealias *Flt64` | 1 | `adapter/flt64` 合法兼容别名 | 暂保留，H5 评估删除窗口 |

这些项目当前不阻塞主链门禁。后续是否继续下降，取决于是否落地 V-typed solver adapter 层和 callback adapter 层。

## 5. 关键架构决策

1. 公开主链只接受 V-typed API，`Flt64` 不再作为主路径。
2. `adapter/flt64` 只作为兼容边界，不作为主链设计来源。
3. 机制层不再继续靠零散 `internal` 修饰收口，下一步需要边界接口重构。
4. `SolverBoundaryCasts` 允许短期集中存在，但不能再扩散到业务文件。
5. `core/mechanism` 与 `core/callback` 的剩余项必须通过接口形状变化来下降，不能只靠白名单扩容。

## 6. H 阶段执行计划

### H1：固化更严格的边界门禁

目标：让下一阶段扫描口径从“public API 不泄漏”升级到“边界表面继续下降”。

改动范围：
- `scripts/scan-full-genericization.ps1`
- `scripts/scan-full-genericization-result.json`
- `ospf-kotlin-core/daily.md`

执行要求：
- 扫描脚本输出 `core/mechanism boundary_allowed = 12` 的完整命中清单。
- 扫描脚本输出 `core/callback boundary_allowed = 3` 的完整命中清单。
- 将 `UNCHECKED_CAST` 分成 `bridge_allowed` 与 `blocking` 两类。

验收：
- 当前基线可重复复现。
- 每个剩余边界项都有文件、行号、原因、下一步分类。

### H2：分类 `core/mechanism` 剩余 12 项

目标：逐项判断 mechanism 剩余边界该 V 化、迁 adapter，还是等待接口重构。

分类建议：
- solver solution ingestion
- flatten / convert
- constraint / meta data carrier
- DSL / infix residue
- solver plugin / adapter contract

执行要求：
- 不直接改大接口，先完成清单分类。
- 每项必须标记处理策略：`V_TYPED`、`MOVE_TO_ADAPTER`、`INTERFACE_REWORK`、`KEEP_ADAPTER_TEMPORARILY`。

验收：
- `daily.md` 增加 12 项分类表。
- 后续改动可以按分类独立推进，不再依赖人工猜测。

### H3：设计并落地 V-typed solver adapter 层

目标：让 mechanism 不再直接面向 `Flt64` solver 数据结构。

改动范围：
- `core/model/mechanism`
- `core/solver`
- 可能新增 solver adapter / boundary 包

执行要求：
- 定义 V-typed solver adapter 接口，承接 solver 输入输出转换。
- 将 `convertMechanismModelToFlt64`、flatten data、solution ingestion 等行为从 mechanism 主包剥离。
- 旧 Flt64 adapter 入口只保留在明确边界包。

验收：
- `core/mechanism boundary_allowed` 明显下降，目标 12 -> 0。
- `core/mechanism public_api_blocking` 保持 0。
- core 145/145 测试通过。

### H4：重构 callback 边界

目标：处理 `core/callback boundary_allowed = 3`。

改动范围：
- `core/model/callback`
- solver callback adapter 相关调用方

执行要求：
- 明确 callback 的 V-typed 主接口与 Flt64 adapter 接口。
- 不再让 callback 主包承担 Flt64 solver 边界。
- Kotlin 接口成员无法 `internal` 的限制，需要通过接口拆分解决。

验收：
- `core/callback boundary_allowed` 从 3 降到 0，或只剩明确 adapter 包项。
- callback 测试继续通过。

### H5：收尾 `SolverBoundaryCasts` 与 adapter typealias

目标：H3/H4 完成后再压缩集中 bridge。

改动范围：
- `SolverBoundaryCasts.kt`
- `adapter/flt64`
- `scan-full-genericization.ps1`

执行要求：
- 移除不再需要的 star-projection cast。
- 如果 adapter typealias 仍存在，明确是否保留一个兼容版本周期。
- 不允许新增业务文件中的 `@Suppress("UNCHECKED_CAST")`。

验收：
- `Suppress(UNCHECKED_CAST)` 从 4 继续下降，目标 0 或最小可解释值。
- `typealias *Flt64 = 1` 明确保留/删除策略。
- 扫描、编译、core test、math test 全部通过。

## 7. H 阶段最终验收标准

### 7.1 扫描门禁

- `public_api_blocking` 全部为 0
- `core/mechanism boundary_allowed = 0`
- `core/callback boundary_allowed = 0`
- `core/function override = 0`
- `geometry typealias *Flt64 = 0`
- `variable typealias *Flt64 = 0`
- `typealias *Flt64` 非 adapter / legacy 主包为 0
- `Suppress(UNCHECKED_CAST)` 为 0，或仅剩集中 bridge 且有不可替代说明

### 7.2 构建与测试门禁

- `mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile` 必须通过
- `mvn -pl ospf-kotlin-core -am test` 必须通过
- `ospf-kotlin-math` 测试必须全绿

### 7.3 API 验收

- 用户可用 V-typed API 完成变量声明、表达式构造、约束添加、模型构建、求解结果读取。
- Flt64 solver 适配只存在于 adapter / boundary 层。
- adapter / legacy 中的 Flt64 API 不会被主包隐式带入。

## 8. 下一会话建议顺序

1. 先做 H1，让扫描结果列出 mechanism 12 项和 callback 3 项的完整清单。
2. 做 H2，只分类，不大改接口。
3. 分类稳定后做 H3，落地 V-typed solver adapter 层。
4. 做 H4，拆 callback 主接口和 adapter 接口。
5. 最后做 H5，收 `SolverBoundaryCasts` 和 adapter typealias。

## 9. 执行记录

| 阶段 | 范围 | 状态 | 扫描 | 编译 | 测试 | 备注 |
|---|---|---|---|---|---|---|
| P13 | 主链泛型化 | done | PASS | PASS | math 711/711；core 143/145 | 主链已完成 |
| F1 | core solver 历史测试 | done | PASS | PASS | core 145/145 | 已修复 |
| F2 | 变量/几何 typealias 移除 | done | PASS | PASS | - | 已清零 |
| F3 | 其他 Flt64 typealias 移除 | done | PASS | PASS | - | 主包 Flt64 别名已收口，保留项仅在 adapter 边界 |
| F4 | function 接口拆分 | done | PASS | PASS | core 145/145 | override 已清零 |
| F5 | mechanism 边界迁移 | done | PASS | PASS | core 145/145 | 完成一轮迁移，剩余 12 项进入 H 阶段 |
| F6 | callback 兼容 API 收口 | done | PASS | PASS | core 145/145 | 已收口一轮 |
| F7 | UNCHECKED_CAST 清理 | done | PASS | PASS | core 145/145 | 已集中化 |
| F8 | 全面泛型化扫描脚本 | done | PASS | PASS | - | 已落地 |
| F9 | 文档与示例同步 | done | PASS | PASS | core 145/145 | 已补迁移指南 |
| G1 | core/mechanism 继续清零 | done | PASS | PASS | core 145/145 | boundary_allowed 18 -> 12 |
| G2 | MultiObject alias 清理 | done | PASS | PASS | core 145/145 | typealias 已删除，migration_debt 清零 |
| G3 | callback / bridge 债务压缩 | done | PASS | PASS | core 145/145 | callback 保持 3，bridge 集中化 |
| G4 | 扫描与文档同步 | done | PASS | PASS | core 145/145 | daily.md 已更新 |
| H1 | 边界门禁固化 | pending |  |  |  | 输出剩余项完整清单 |
| H2 | mechanism 12 项分类 | pending |  |  |  | 先分类再重构 |
| H3 | V-typed solver adapter | pending |  |  |  | 目标 mechanism 12 -> 0 |
| H4 | callback adapter 重构 | pending |  |  |  | 目标 callback 3 -> 0 |
| H5 | bridge / typealias 收尾 | pending |  |  |  | 目标 cast 和 adapter alias 最小化 |

## 10. 当前未完成事项

| 项目 | 当前值 | 目标 | 处理阶段 |
|---|---:|---:|---|
| `core/mechanism boundary_allowed` | 12 | 0 | H2/H3 |
| `core/callback boundary_allowed` | 3 | 0 | H4 |
| `Suppress(UNCHECKED_CAST)` | 4 | 0 或最小可解释值 | H5 |
| `typealias *Flt64` adapter 项 | 1 | 保留/删除决策明确 | H5 |

处理原则：
- 不扩白名单来制造完成感。
- 不再靠零散 `internal` 修饰解决接口形状问题。
- 先清单、再分类、再重构。
