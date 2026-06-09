# Gantt Scheduling 泛型化计划

日期：2026-06-09（下一轮：G18 兼容层与边界收口）

## 1. 总目标

`ospf-kotlin-framework-gantt-scheduling` 的长期目标是完成领域数值泛型化与物理量化，并将 `Flt64` 收敛为 solver/model boundary、adapter boundary、算法内部或明确的迁移期兼容入口。

核心目标：

1. 领域层的时间、数量、成本、资源用量、产出消耗等有量纲字段支持 `Quantity<V>`。
2. solver/model 层可以继续使用 `Flt64`，但 `Flt64` 必须集中在 adapter、model boundary 或算法边界。
3. `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>` 等建模类型不再从 application/service API 向领域对象泄漏。
4. `Cost<V>`、`ShadowPriceMap<V>`、`CapacityColumn<V>`、`SlotBasedCapacityResult<V>`、solution summary、application snapshot 等领域和应用结果模型支持 `Flt64` 与 `FltX`。
5. 迁移期保留必要的 `Flt64` typealias、wrapper 和旧入口，确保现有应用可按阶段迁移；兼容入口应逐步 deprecated 并最终退出。
6. 裸 `V` 只用于无量纲量，例如相对改善率、利用率、折扣、权重、归一化 reduced cost、排序评分。
7. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4` 作为 gantt-scheduling 示例使用方，必须随框架 API 泛型化同步更新并持续通过 reactor 编译。

## 2. 已完成事项摘要

已完成 G5-G17 主迁移链路，并启动 G18 第一批收口：

1. 已建立泛型领域模型、泛型时间窗口、共享 solver adapter、Flt64 scan gate 与分类门禁。
2. 已完成 task、bunch、capacity、resource、produce、application 主链路的泛型兼容与 Quantity 主字段铺设。
3. 已完成 solution summary、iteration snapshot、slot/pre-solver result、capacity/resource/produce solved quantity 等泛型物理量出口。
4. 已完成主要旧裸值构造函数、属性、helper、typealias、日历兼容入口的 deprecation 与替代路径标注。
5. 已将重复的 solver 数值转换集中到 task/capacity/produce/resource/time 等命名 adapter 或 helper。
6. 已完成 solver 时间边界 facade、application 算法标量边界和 shadow price 边界归类。
7. 已同步 demo4 示例并压缩测试侧非必要 legacy 覆盖，普通测试主路径优先使用 Quantity/FltX。
8. scan gate 已达到 `Domain DTO Pending=0` 与 `unclassified=0`，G17 可验收；G18 目标转为削减兼容层与进一步压缩边界噪音，而不是盲目清零所有 `Flt64`。

当前 scan gate 基线：

```text
main=1,365
test=158
total=1,523
unclassified=0

Domain DTO Pending=0
Compat Wrapper=125
Adapter Conversion=40
Solver Boundary=771
```

## 3. 当前保留边界

以下 `Flt64` 使用不作为当前阻塞项，但必须保持边界清晰：

1. solver 建模对象、变量、符号、求解结果、pre-solver 模型和 solver facade。
2. framework shadow price 基础 API，通过 adapter/helper 隔离到泛型结果模型。
3. branch-and-price 内部 objective、reduced cost、optimal rate、heartbeat、剪枝阈值等算法标量。
4. WorkingCalendar、TimeWindow 与生产力日历内部的时间数值计算边界。
5. 迁移期公开 typealias、旧 wrapper、legacy constructor/property/helper 和最小兼容测试。

公开 `Flt64...` typealias 即使仓库内暂无引用，也不能在没有兼容策略时直接删除；优先通过 `@Deprecated`、`ReplaceWith`、文档和替代 API 引导退出。

## 4. 下一轮事项

下一轮以 G18 收口为主，尽可能覆盖兼容层、adapter conversion、测试噪音与应用边界，减少后续小迭代。

1. 削减 `Compat Wrapper`：优先处理仓库内无引用、已有泛型/Quantity 替代者、非公开或低风险的 legacy constructor/property/helper。
2. 收敛 `Adapter Conversion`：审计剩余 40 行转换点，能迁入现有 shared solver helper、factory 或 adapter facade 的继续迁移；无法迁移的补分类和边界说明。
3. 强化 public facade：在 resource、produce、capacity、task compilation、application result/shadow price 周边补齐泛型主入口，减少调用方触达 solver `Flt64` 的机会。
4. 压缩测试侧 legacy 噪音：保留边界与兼容断言，删除普通 FltX/Quantity 测试中重复的 `Flt64` 命名、构造和属性覆盖。
5. 审计旧裸值 property/constructor/helper：优先让内部实现委托 Quantity 主字段，外部旧入口只保留迁移提示。
6. 扩展 scan gate 分类：新增边界或保留点必须进入明确分类，继续保持 `Domain DTO Pending=0` 和 `unclassified=0`。
7. 同步 example：demo4 必须持续使用当前推荐 API 编译通过，必要时同步替换 deprecated 调用。

## 5. 下一轮计划

1. 基线确认：运行 scan gate，列出 `Compat Wrapper`、`Adapter Conversion`、测试侧 `Flt64` 的文件级热点。
2. 第一批低风险清理：处理 produce/resource/capacity 中仓库内无引用且已有 Quantity 替代的旧 helper 和裸值属性。
3. 第二批边界收口：处理 task compilation 与 bunch compilation 中 solver time/result facade 周边重复转换和旧 overload。
4. 第三批 application 收口：隔离 branch-and-price snapshot/result 与算法内部标量，减少结果模型对 solver `Flt64` 的直接暴露。
5. 第四批测试与示例：压缩普通测试 legacy 噪音，同步 demo4，保留最小兼容覆盖。
6. 门禁校验：运行局部模块测试、gantt 全量测试、example reactor 编译、scan gate 与 diff check。
7. 文档回写：用下一轮实际结果更新本文件，只保留结论、基线、剩余工作和验收状态。

## 6. 下一轮修改清单

重点模块与方向：

1. `gantt-scheduling-domain-resource-context`
   - 审计 `Resource`、`ExecutionResource`、`ConnectionResource`、`StorageResource` 的 Quantity 主方法下沉情况。
   - 继续压缩 capacity、usage、storage、slack、cost 相关 legacy getter/helper。
   - 保留必要公开兼容 typealias，不直接破坏外部 API。

2. `gantt-scheduling-domain-produce-context`
   - 继续收敛 `Produce`、`Consumption`、`ProductionTask`、capacity scheduling produce 的旧裸值 constructor/property/helper。
   - 让内部业务判断、range、bunch 系数和 solved quantity 继续走共享 helper。

3. `gantt-scheduling-domain-capacity-scheduling-context`
   - 审计 `CapacityColumn`、`CapacityCompilation`、`CapacityOrderCompilation`、`IterativeCapacityCompilation` 的 solver helper 覆盖面。
   - 处理 column produce/consumption、cost、duration、amount 旧入口和测试覆盖。

4. `gantt-scheduling-domain-task-compilation-context`
   - 收口 `TaskTime`、`Switch`、`Compilation`、`Makespan` 与 limit overload 的 solver time boundary 使用。
   - 审计 threshold、delay/advance、makespan、switch、aggregation 的旧裸值入口。

5. `gantt-scheduling-domain-bunch-compilation-context`
   - 审计 slot result、pre-solver result、task reverse、task time、capacity result 的旧裸值属性退出计划。
   - 保持 solver result boundary 与 algorithm scalar 分类清晰。

6. `gantt-scheduling-application`
   - 梳理 task/bunch branch-and-price snapshot、iteration、solution result 的泛型 facade。
   - 将 algorithm internal 标量与对外结果模型边界继续拆开。

7. `gantt-scheduling-infrastructure`
   - 审计 `TimeRange`、`TimeSlot`、`TimeWindow`、`WorkingCalendar` 中 time/calendar adapter 的公开面。
   - 保持日历内部转换集中，避免领域层重新散落 `Flt64`。

8. `ospf-kotlin-example`
   - 仅同步 demo4 受 gantt API 影响的调用点。
   - 不纳入非 gantt 示例的无关重构。

## 7. 下一轮验收标准

1. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
2. scan gate 保持 `Domain DTO Pending=0`、`unclassified=0`。
3. `Compat Wrapper` 明确下降；如果未下降，必须在本文件记录阻塞原因和保留依据。
4. `Adapter Conversion` 不上升；新增转换点必须有命名 helper/facade 或明确分类。
5. 普通 FltX/Quantity 测试不再新增 legacy-only 覆盖；兼容测试只保留迁移期必要断言。
6. 相关局部模块测试通过，至少覆盖 produce、resource、capacity、task compilation 或本轮实际触达模块。
7. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
8. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
9. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 无 whitespace error。
10. 本文件更新为下一轮实际完成摘要、最新基线和剩余工作，不再恢复过时迁移文档。
