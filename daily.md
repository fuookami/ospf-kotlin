# demo4 Branch-and-Price 完整化计划

## 背景

`docs/ddd.md` 暂不修改 demo4 的完整性描述。当前 demo4 仍存在 Branch-and-Price 编排与列生成 master 生命周期不完整的问题，本计划供后续会话按阶段补齐实现、测试和文档。

## 当前证据

1. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/bunch_selection/service/BranchAndPriceAlgorithm.kt` 仍为空类，尚未承接列生成迭代编排。
2. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/bunch_compilation/BunchCompilationContext.kt` 的 `selectFreeExecutors` 仍为 `TODO("Not yet implemented")`。
3. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/bunch_compilation/service/PipelineListGenerator.kt` 中 fleet、flight、link 相关 coefficient 仍有多个 `TODO("not implemented yet")`。
4. `bunch_compilation` 已有 `addColumns` 局部生命周期，但需要确认与 master model、shadow price、final MILP 的完整闭环是否一致。

## 目标

1. 接入或复用 `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-application` 中的 bunch `BranchAndPriceAlgorithm`，必要时实现 demo4 适配器。
2. 补齐 `bunch_compilation` 的约束系数、影子价格提取、`addColumns` / `removeColumns` 生命周期和 free executor 选择逻辑。
3. 完成 `bunch_selection` 编排入口，串联 `BunchGenerationContext`、`BunchCompilationContext`、solver builder、初始列、迭代求解和最终 MILP。
4. 补齐失败路径与诊断输出，避免算法中途失败时只返回未解释的底层错误。
5. 增加覆盖 pricing 生成、初始列、加列、影子价格、最终 MILP 与失败路径的测试。

## 阶段计划

### 阶段 1：对齐框架实现

1. 阅读 `gantt-scheduling-application` 中 Branch-and-Price 的应用层实现，确认可复用的泛型参数、上下文接口和 solver 依赖。
2. 对照 demo4 的 `BunchGenerationContext`、`BunchCompilationContext` 和 `ShadowPriceMap`，列出缺失适配点。
3. 明确 demo4 是否直接复用框架算法，还是保留 demo4 专属薄适配层。

验收：

```powershell
rg -n "class BranchAndPriceAlgorithm|BranchAndPrice" ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4
```

### 阶段 2：补齐 master 约束生命周期

1. 实现 `BunchCompilationContext.selectFreeExecutors`，确保 free executor 选择与 demo4 的 Aircraft / FlightTaskAssignment 语义一致。
2. 补齐 `PipelineListGenerator` 中 fleet balance、flight capacity、flight link 等 coefficient 的业务计算。
3. 复核 `Aggregation.addColumns` 与各 model component 的 `addColumns` 顺序，必要时补齐 `removeColumns` 或同步刷新逻辑。
4. 验证 shadow price extractor 能从 master 约束中提取到 pricing 所需 key。

验收：

```powershell
rg -n "TODO\\(\"not implemented yet\"\\)|TODO\\(\"Not yet implemented\"\\)" ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/bunch_compilation
```

### 阶段 3：完成 Branch-and-Price 编排

1. 在 `bunch_selection` 中完成算法入口，包含初始列注册、LP master 求解、shadow price 刷新、pricing 生成、加列和收敛判断。
2. 增加 final MILP 求解路径，将 LP 阶段生成的列转为最终整数选择结果。
3. 对接 solver builder，不在领域层直接绑定具体 solver。
4. 输出迭代 trace：迭代次数、新列数量、best reduced cost、LP 目标、final MILP 状态。

验收：

```powershell
rg -n "TODO|class BranchAndPriceAlgorithm" ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/bunch_selection
```

### 阶段 4：测试与回归

1. 添加 pricing 单元测试：给定 shadow price 后能生成 reduced cost 为负的 bunch。
2. 添加 master 生命周期测试：初始列注册、`addColumns` 后约束项刷新、shadow price key 可提取。
3. 添加端到端最小样例：可从输入生成初始列，完成至少一次列生成迭代，并进入 final MILP。
4. 添加失败路径测试：无初始列、pricing 无可行列、solver 失败、shadow price 缺失。

验收命令占位：

```powershell
mvn -pl ospf-kotlin-example -Dtest=*Demo4* test
mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-application test
```

## 完成标准

1. demo4 不再存在阻断 Branch-and-Price 主流程的 `TODO("not implemented yet")`。
2. `BranchAndPriceAlgorithm` 能完成 LP 列生成迭代和 final MILP。
3. `bunch_generation` 与 `bunch_compilation` 通过 `ShadowPriceMap` 和 `addColumns` 形成闭环。
4. 最小端到端样例可稳定运行，并能暴露可解释的失败原因。
