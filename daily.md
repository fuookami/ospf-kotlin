# Daily

## demo4 Branch-and-Price 完整化计划

### 背景

`docs/ddd.md` 暂不修改 demo4 的完整性描述。当前 demo4 仍存在 Branch-and-Price 编排与列生成 master 生命周期不完整的问题，本计划供后续会话按阶段补齐实现、测试和文档。

### 当前证据

1. `demo4/domain/bunch_selection/service/BranchAndPriceAlgorithm.kt` 仍为空类，尚未承接列生成迭代编排。
2. `demo4/domain/bunch_compilation/BunchCompilationContext.kt` 的 `selectFreeExecutors` 仍为 `TODO("Not yet implemented")`。
3. `demo4/domain/bunch_compilation/service/PipelineListGenerator.kt` 中 fleet、flight、link 相关 coefficient 仍有多个 `TODO("not implemented yet")`。
4. `bunch_compilation` 已有 `addColumns` 局部生命周期，但需确认与 master model、shadow price、final MILP 的完整闭环是否一致。

### 目标

1. 接入或复用 `gantt-scheduling-application` 中的 bunch `BranchAndPriceAlgorithm`，必要时实现 demo4 适配器。
2. 补齐 `bunch_compilation` 的约束系数、影子价格提取、`addColumns` / `removeColumns` 生命周期和 free executor 选择逻辑。
3. 完成 `bunch_selection` 编排入口，串联 BunchGeneration、BunchCompilation、solver builder、初始列、迭代求解和最终 MILP。
4. 补齐失败路径与诊断输出，避免算法中途失败时只返回未解释的底层错误。
5. 增加覆盖 pricing 生成、初始列、加列、影子价格、最终 MILP 与失败路径的测试。

### 阶段计划

#### 阶段 1：对齐框架实现
- 阅读 `gantt-scheduling-application` 中 Branch-and-Price 的应用层实现
- 对照 demo4 的 BunchGenerationContext、BunchCompilationContext 和 ShadowPriceMap
- 明确 demo4 是否直接复用框架算法，还是保留专属薄适配层

#### 阶段 2：补齐 master 约束生命周期
- 实现 `selectFreeExecutors`，确保与 Aircraft / FlightTaskAssignment 语义一致
- 补齐 fleet balance、flight capacity、flight link 等 coefficient 计算
- 复核 `addColumns` 与各 model component 顺序
- 验证 shadow price extractor

#### 阶段 3：完成 Branch-and-Price 编排
- 完成算法入口：初始列注册、LP master 求解、shadow price 刷新、pricing 生成、加列、收敛判断
- 增加 final MILP 求解路径
- 对接 solver builder，不在领域层直接绑定具体 solver
- 输出迭代 trace

#### 阶段 4：测试与回归
- pricing 单元测试
- master 生命周期测试
- 端到端最小样例
- 失败路径测试

验收命令：
```bash
mvn -pl ospf-kotlin-example -Dtest=*Demo4* test
mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-application test
```

### 完成标准
1. demo4 不再存在阻断主流程的 `TODO("not implemented yet")`
2. `BranchAndPriceAlgorithm` 能完成 LP 列生成迭代和 final MILP
3. `bunch_generation` 与 `bunch_compilation` 通过 `ShadowPriceMap` 和 `addColumns` 形成闭环
4. 最小端到端样例可稳定运行

---

## 全仓库 KDoc 中英文注释补全

### 目标

全仓库 `src/main/` 下所有类、方法（含 internal、private 作用域）补充中英文双语 KDoc，确保所有字段和参数都添加 `@property`、`@param`、`@return` 标签。

### 进度

| 阶段 | 文件数 | 问题数 | 说明 |
|------|--------|--------|------|
| 初始 | 577 files | 3641 issues | 扫描全仓库 |
| 当前 | 441 files | 2487 issues | 约 1/3 完成 |
| 已提交 | 354 files changed | commit: 2a586ff8f | 10752 insertions, 3080 deletions |

### 已修复模块

- `ospf-kotlin-utils/Variant.kt` -- 完整覆盖 338 issues
- `ospf-kotlin-core` -- 从 548 降到 102 issues
- `ospf-kotlin-math`（混沌映射系列）-- 大部分已处理
- `ospf-kotlin-framework` -- 部分完成
- `ospf-kotlin-framework-bpp3d` 各子模块 -- 部分完成
- `ospf-kotlin-core-plugin` 各求解器 -- 部分完成
- `ospf-kotlin-framework-plugin` -- 部分完成
- `ospf-kotlin-multiarray`、`quantities` -- 已处理
- 中文乱码修复 -- 15 个文件

### 详细待办清单

（以下为 `scan_kdoc.py` 自动生成，`class:N` 表示缺少 KDoc 的类数，`func:N` 表示缺少 KDoc 的函数数，`tags:` 后为缺少的标签数）

### ospf-kotlin-example (133 files, 382 KDoc + 6 tags)

- [ ] FlightTask.kt func:24
- [ ] Position.kt class:11 func:1 typealias:2
- [ ] BendersStrategy.kt class:6 func:8
- [ ] SemanticParameter.kt class:13
- [ ] FlightTaskBunchGenerator.kt func:11
- [ ] FlightTaskFeasibilityJudger.kt class:1 func:10
- [ ] Formula.kt func:10
- [ ] Aggregation.kt class:1 func:6
- [ ] HorizontalStabilizer.kt class:4 func:2 typealias:1
- [ ] SemanticParameter.kt class:7
- [ ] FreeAircraftSelector.kt func:7
- [ ] SoftSecurityContext.kt class:1 func:5
- [ ] BendersSolver.kt class:4 func:2
- [ ] AirworthinessSecurityContext.kt func:5
- [ ] Envelope.kt class:1 func:4
- [ ] MaxCumulativeLoadWeight.kt class:5
- [ ] ExpressEffectivenessContext.kt func:5
- [ ] MacContext.kt func:5
- [ ] MacOptimizationContext.kt func:5
- [ ] PayloadMaximizationContext.kt func:5
- [ ] RecommendedWeightEqualizationContext.kt func:5
- [ ] RedundancyContext.kt func:5
- [ ] Aggregation.kt class:1 func:4
- [ ] Aggregation.kt class:1 func:4
- [ ] Cargo.kt class:4 func:1
- [ ] RequestDTO.kt class:4 func:1
- [ ] AggregationInitializer.kt func:5
- [ ] BandwidthContext.kt func:4
- [ ] LoadingOrder.kt func:4
- [ ] NeighbourCalculator.kt func:4
- [ ] Aggregation.kt func:4
- [ ] MaxUnsymmetricalLinearDensity.kt class:4
- [ ] Aggregation.kt func:4
- [ ] Aggregation.kt func:4
- [ ] Aggregation.kt func:4
- [ ] Aggregation.kt func:4
- [ ] Stowage.kt class:1 func:3
- [ ] Diagnostics.kt class:1 func:3
- [ ] DomainPipeline.kt func:4
- [ ] Restriction.kt func:4
- [ ] FlightCycle.kt func:4
- [ ] Application.kt func:3
- [ ] LinearDensity.kt class:2 func:1
- [ ] MinLowPayload.kt class:1 func:2
- [ ] Load.kt class:1 func:2
- [ ] TotalWeight.kt class:1 func:2
- [ ] FeasibilityDiagnostics.kt class:1 func:2
- [ ] ResponseDTO.kt class:1 func:2
- [ ] MaxCLIM.kt class:1 func:1
- [ ] MaxZoneLoadWeight.kt class:2
- [ ] SurfaceDensity.kt class:1 func:1
- [ ] AdviceLoading.kt class:1 func:1
- [ ] SequentialLoading.kt class:1 func:1
- [ ] Trailer.kt class:2
- [ ] TrailerLoading.kt class:1 func:1
- [ ] TransferAdjacentLoading.kt class:1 func:1
- [ ] MACRange.kt class:2
- [ ] DivideEmptyLoading.kt class:1 func:1
- [ ] Ballast.kt class:1 func:1
- [ ] Flight.kt class:1 func:1
- [ ] MaxLoadWeight.kt class:1 func:1
- [ ] Solution.kt class:1 func:1
- [ ] LoadingOrderResponseDTO.kt class:1 func:1
- [ ] RunningHeartBeatDTO.kt class:2
- [ ] InitialFlightTaskBunchGenerator.kt func:2
- [ ] RouteGraphGenerator.kt func:2
- [ ] ShadowPriceMap.kt | tags: @param:1, @return:1
- [ ] ServiceBandwidth.kt func:1
- [ ] SolutionAnalyzer.kt class:1
- [ ] Fuel.kt class:1
- [ ] Neighbour.kt typealias:1
- [ ] RelativeOrder.kt func:1
- [ ] PipelineListGenerator.kt class:1
- [ ] AdviceLoadAmountLimit.kt class:1
- [ ] AdviceLoadWeightLimit.kt class:1
- [ ] ItemAheadLoadLimit.kt class:1
- [ ] ItemOrderReverseLimit.kt class:1
- [ ] ItemReserveLimit.kt class:1
- [ ] ItemReweighNeededLimit.kt class:1
- [ ] SameDestinationAdjacent.kt class:1
- [ ] SameSourceAdjacentLimit.kt class:1
- [ ] TrailerChangeLimit.kt class:1
- [ ] TrailerCirclingLimit.kt class:1
- [ ] MAC.kt func:1
- [ ] Torque.kt func:1
- [ ] LateralBalance.kt func:1
- [ ] LongitudinalBalance.kt func:1
- [ ] ExperimentalLongitudinalBalance.kt func:1
- [ ] Redundancy.kt func:1
- [ ] AggregationInitializer.kt class:1
- [ ] PipelineListGenerator.kt class:1
- [ ] AdviceBallastWeightLimit.kt class:1
- [ ] DivideEmptyLoadingLimit.kt class:1
- [ ] EmptyHatedLimit.kt class:1
- [ ] MainDeckDoorEmptyLimit.kt class:1
- [ ] Appointment.kt class:1
- [ ] BiologicalLimit.kt class:1
- [ ] Payload.kt class:1
- [ ] AggregationInitializer.kt class:1
- [ ] PipelineListGenerator.kt class:1
- [ ] SolutionAnalyzer.kt class:1
- [ ] AOGMATBulkConflictLimit.kt class:1
- [ ] BiologicalAdjacentLimit.kt class:1
- [ ] BiologicalBulkConflictLimit.kt class:1
- [ ] ELDAdjacentLimit.kt class:1
- [ ] EmptyForbiddenLimit.kt class:1
- [ ] ItemAdjustmentLimit.kt class:1
- [ ] ItemAssignmentLimit.kt class:1
- [ ] LoadAmountLimit.kt class:1
- [ ] LoadingOrderLimit.kt class:1
- [ ] LoadWeightLimit.kt class:1
- [ ] NormalBulkDestinationAssignmentLimit.kt class:1
- [ ] PredicateLoadWeightLimit.kt class:1
- [ ] RecommendLoadWeightLimit.kt class:1
- [ ] StowageLimit.kt class:1
- [ ] Parameter.kt class:1
- [ ] Solver.kt class:1
- [ ] TryHelpers.kt | tags: @param:1
- [ ] BendersAdaptiveConfig.kt class:1
- [ ] BendersQualityOverrideConfig.kt class:1
- [ ] DiagnosticNote.kt class:1
- [ ] KPIResponseDTO.kt class:1
- [ ] RenderDTO.kt class:1
- [ ] ReportResponseDTO.kt class:1
- [ ] SolvePolicy.kt class:1
- [ ] WeightRecommendationObjectiveConfig.kt class:1
- [ ] Main.kt class:1
- [ ] Demo4Application.kt class:1
- [ ] FlightTaskReverse.kt func:1
- [ ] Graph.kt | tags: @property:1
- [ ] CrewManRank.kt | tags: @property:1
- [ ] PilotRank.kt | tags: @property:1
- [ ] Passenger.kt func:1

### ospf-kotlin-math (71 files, 181 KDoc + 186 tags)

- [ ] EvaluateBoolean.kt func:1 | tags: @param:15, @return:16
- [ ] Parser.kt | tags: @param:7, @return:16
- [ ] ExpressionSerde.kt class:18 func:1 | tags: @return:4
- [ ] Rational.kt class:10 func:8 | tags: @param:1, @return:3
- [ ] Inequality.kt func:18 | tags: @return:2
- [ ] Serde.kt | tags: @param:6, @return:14
- [ ] Flt64QuickDsl.kt func:14
- [ ] Parse.kt class:4 | tags: @return:7
- [ ] PolynomialParser.kt class:4 | tags: @return:7
- [ ] SymbolIdentitySerde.kt class:7 | tags: @param:1, @return:3
- [ ] BooleanExpression.kt | tags: @param:1, @return:8
- [ ] Lexer.kt | tags: @property:1, @param:4, @return:4
- [ ] Triangulation.kt func:8
- [ ] PolynomialLexer.kt class:3 | tags: @return:5
- [ ] Latex.kt | tags: @param:1, @return:6
- [ ] EinsumParser.kt | tags: @param:1, @return:6
- [ ] ToPolynomial.kt func:6
- [ ] PolynomialSerde.kt class:6
- [ ] Floating.kt class:3 func:2
- [ ] Integer.kt class:5
- [ ] NumericInteger.kt class:5
- [ ] NumericUInteger.kt class:5
- [ ] UInteger.kt class:5
- [ ] ExpressionDsl.kt func:1 | tags: @param:2, @return:2
- [ ] QuickDsl.kt | tags: @property:1, @return:4
- [ ] IndexLabel.kt | tags: @param:2, @return:3
- [ ] ScalarExpression.kt | tags: @param:1, @return:3
- [ ] Normalize.kt | tags: @param:2, @return:2
- [ ] NumericOps.kt class:4
- [ ] CompileOps.kt class:4
- [ ] InequalityDsl.kt | tags: @property:1, @return:3
- [ ] LatexOps.kt class:1 | tags: @param:1, @return:2
- [ ] LinearQuadraticOps.kt class:1 func:1 | tags: @param:1, @return:1
- [ ] MatrixForm.kt | tags: @param:2, @return:2
- [ ] PropertyPath.kt class:1 | tags: @return:2
- [ ] InequalitySerde.kt class:3
- [ ] ChebyshevMap.kt func:2
- [ ] Circle.kt func:2
- [ ] Triangle.kt func:2
- [ ] FltXPowerStrategy.kt | tags: @param:1, @return:1
- [ ] Convert.kt | tags: @param:1, @return:1
- [ ] Evaluate.kt | tags: @param:1, @return:1
- [ ] PowerVectorKey.kt | tags: @param:1, @return:1
- [ ] LinearSpaces.kt func:1
- [ ] Numbers.kt func:1
- [ ] ValueTraits.kt func:1
- [ ] Interval.kt class:1
- [ ] ValueRange.kt func:1
- [ ] ArnoldTongue.kt func:1
- [ ] CircleMap.kt func:1
- [ ] HindmarshRoseModel.kt class:1
- [ ] IntervalExchangeTransformation.kt class:1
- [ ] LiuChenAttractor.kt class:1
- [ ] LorenzMod1Attractor.kt class:1
- [ ] LorenzMod2Attractor.kt class:1
- [ ] LorenzStenfloAttractor.kt class:1
- [ ] LotkaVolterraSystem.kt class:1
- [ ] QiAttractor.kt class:1
- [ ] QiChenAttractor.kt class:1
- [ ] RabinovichFabrikantEquation.kt class:1
- [ ] RucklidgeAttractor.kt class:1
- [ ] SakaryaAttractor.kt class:1
- [ ] SingerMap.kt class:1
- [ ] Edge.kt func:1
- [ ] Projection2.kt | tags: @return:1
- [ ] Quadrilateral.kt func:1
- [ ] Contains.kt class:1
- [ ] SymbolIdentity.kt class:1
- [ ] QuickOps.kt | tags: @property:1
- [ ] TensorExpr.kt class:1
- [ ] Fold.kt func:1

### ospf-kotlin-utils (9 files, 21 KDoc + 337 tags)

- [ ] Variant.kt | tags: @property:1, @param:168, @return:168
- [ ] Time.kt func:5
- [ ] Common.kt class:2 func:3
- [ ] Context.kt func:4
- [ ] Fold.kt func:2
- [ ] DateTimeSerializer.kt class:2
- [ ] UUIDv7.kt func:1
- [ ] Quadruple.kt class:1
- [ ] Json.kt class:1

### ospf-kotlin-framework (20 files, 99 KDoc + 45 tags)

- [ ] ValueTypes.kt class:16 | tags: @param:16, @return:16
- [ ] SerializedModels.kt class:15 func:1
- [ ] SortBy.kt func:10 | tags: @param:1, @return:3
- [ ] RemoteSolverHttpClient.kt class:7 func:3
- [ ] TaskModels.kt class:8
- [ ] UpdateAssignment.kt func:5 | tags: @return:2
- [ ] PredicateFieldFilter.kt class:1 func:5
- [ ] UnsupportedPredicatePolicy.kt | tags: @param:2, @return:3
- [ ] LocalFileObjectStoragePort.kt func:5
- [ ] RequestRecord.kt class:2 func:2
- [ ] ExecutionModels.kt class:4
- [ ] RemoteLinearSolver.kt func:3
- [ ] RemoteQuadraticSolver.kt func:3
- [ ] Pipeline.kt func:2
- [ ] PredicateAnnotations.kt class:2
- [ ] Errors.kt | tags: @return:2
- [ ] StorageModels.kt class:2
- [ ] LogRecord.kt class:1
- [ ] ScalarFunctionDsl.kt func:1
- [ ] NormalizedModels.kt class:1

### ospf-kotlin-core (18 files, 81 KDoc + 21 tags)

- [ ] Model.kt func:19 | tags: @param:4, @return:3
- [ ] VariableRange.kt func:23
- [ ] TokenTable.kt func:9
- [ ] CallBackModelInterface.kt func:1 | tags: @return:6
- [ ] LinearConstraintInput.kt func:1 | tags: @param:3, @return:3
- [ ] CallBackModel.kt func:6
- [ ] MetaModel.kt func:6
- [ ] LinearTriadModel.kt func:1 | tags: @return:2
- [ ] LinearTriadDumpBuilders.kt func:2
- [ ] Constraint.kt func:2
- [ ] Linear.kt func:2
- [ ] Quadratic.kt func:2
- [ ] TokenTableRegistrationSupport.kt func:2
- [ ] MathInequalityFlatten.kt func:1
- [ ] Iteration.kt class:1
- [ ] SolverBoundaryCasts.kt class:1
- [ ] FunctionSymbol.kt func:1
- [ ] TokenList.kt class:1

### bpp3d-domain-item-context (21 files, 66 KDoc + 34 tags)

- [ ] Package.kt func:2 | tags: @param:7, @return:8
- [ ] ItemMerger.kt class:3 func:8 | tags: @param:1, @return:1
- [ ] ContinuousRadiusModelComponent.kt func:3 | tags: @param:2, @return:4
- [ ] Aggregation.kt class:1 func:7
- [ ] Bin.kt class:2 func:6
- [ ] Item.kt func:4 | tags: @return:1
- [ ] Id.kt | tags: @param:2, @return:2
- [ ] Pattern.kt | tags: @param:2, @return:2
- [ ] PlacementPlaneMapping.kt func:4
- [ ] QuantityDemandStatistics.kt class:4
- [ ] ItemContainer.kt class:1 func:2
- [ ] Layer.kt class:3
- [ ] Material.kt class:3
- [ ] PlacementTyping.kt func:3
- [ ] ShadowPriceMap.kt func:3
- [ ] DemandReducedCost.kt func:2
- [ ] DemandStatistics.kt | tags: @property:2
- [ ] PlacementFactory.kt func:2
- [ ] ItemContext.kt class:1
- [ ] Schema.kt class:1
- [ ] LoadingOrderCalculator.kt class:1

### gantt-scheduling-infrastructure (4 files, 50 KDoc + 47 tags)

- [ ] WorkingCalendar.kt func:43 | tags: @param:3, @return:3
- [ ] TimeWindow.kt func:3 | tags: @param:26, @return:15
- [ ] RenderTaskDTO.kt class:3
- [ ] TimeRange.kt class:1

### csp1d-application (9 files, 35 KDoc + 54 tags)

- [ ] Csp1dProblemBuilder.kt class:3 | tags: @return:32
- [ ] Csp1dProduceContext.kt func:5 | tags: @param:7, @return:7
- [ ] Csp1dColumnGeneration.kt class:3 func:8 | tags: @param:3, @return:3
- [ ] Csp1dRecovery.kt class:2 func:5
- [ ] Csp1dMilp.kt class:1 func:4
- [ ] Csp1dSolution.kt func:2
- [ ] Csp1dShadowPriceLifecycle.kt | tags: @param:1, @return:1
- [ ] Csp1dMilpSolver.kt class:1
- [ ] TopKCuttingPlans.kt func:1

### bpp3d-infrastructure (13 files, 53 KDoc + 33 tags)

- [ ] PWLRadiusSquaredApproximation.kt func:1 | tags: @param:6, @return:7
- [ ] PackingShape.kt class:10 func:3
- [ ] Projection.kt class:6 func:6
- [ ] ProjectivePlaneGeometryMapping.kt | tags: @param:5, @return:5
- [ ] QuantityGeometryCore.kt | tags: @param:4, @return:6
- [ ] QuantityContainerCore.kt func:6
- [ ] SemanticParameter.kt class:4 func:2
- [ ] RendererDTO.kt class:6
- [ ] Cylinder.kt class:2 func:1
- [ ] PackageType.kt class:3
- [ ] Container.kt func:1
- [ ] Orientation.kt class:1
- [ ] OrientationAxisPermutationMapping.kt func:1

### csp1d-cutting-plan-generation (13 files, 56 KDoc + 6 tags)

- [ ] GenerationCollector.kt class:7 func:18 | tags: @param:1, @return:1
- [ ] GenerationWidthIndex.kt class:4 func:5
- [ ] FullSumGenerator.kt func:5
- [ ] ConcurrentGenerationMaterialSliceTemplateCache.kt func:2 | tags: @return:2
- [ ] CuttingPlanCanonicalKey.kt class:2 func:1
- [ ] GenerationMaterialSliceTemplateCache.kt class:1 func:2
- [ ] NSameGenerator.kt func:3
- [ ] GenerationSliceTemplateCache.kt func:2
- [ ] GenerationTemplateReuse.kt class:1 func:1
- [ ] CuttingPlanConstraint.kt | tags: @param:1
- [ ] GenerationConstraints.kt | tags: @return:1
- [ ] GenerationKnifePruning.kt func:1
- [ ] GenerationMaterialWidthRangeKey.kt class:1

### quantities (7 files, 38 KDoc + 23 tags)

- [ ] Quantity.kt func:24
- [ ] SymbolQuantityOps.kt func:13
- [ ] Box2.kt | tags: @param:3, @return:7
- [ ] PlaneFrame3.kt func:1 | tags: @param:2, @return:3
- [ ] Box3.kt | tags: @return:3
- [ ] Placement3.kt | tags: @return:3
- [ ] Placement2.kt | tags: @return:2

### bpp3d-domain-layer-assignment (4 files, 23 KDoc + 12 tags)

- [ ] Load.kt class:1 func:15 | tags: @param:1, @return:1
- [ ] DemandConstraint.kt func:3 | tags: @param:5, @return:5
- [ ] LayerAssignmentAliases.kt func:3
- [ ] ScaledBpp3dSolverValueAdapter.kt func:1

### bpp3d-domain-packing (8 files, 32 KDoc + 3 tags)

- [ ] PackingGeometryGuard.kt class:1 func:14
- [ ] Aggregation.kt class:5
- [ ] PackageSolutionLikeAdapter.kt class:5
- [ ] MaterialAttribute.kt class:2 func:1
- [ ] PackingRendererAdapter.kt | tags: @return:3
- [ ] MaterialPacker.kt func:2
- [ ] PackingContext.kt class:1
- [ ] MaterialPackingPlan.kt class:1

### gantt-scheduling-task-context (6 files, 28 KDoc + 7 tags)

- [ ] Task.kt func:13
- [ ] SchedulingSolverValueAdapter.kt | tags: @param:3, @return:4
- [ ] TaskPlan.kt func:6
- [ ] TaskBunch.kt func:4
- [ ] TaskStepGraph.kt func:4
- [ ] Cost.kt func:1

### 后续步骤

1. `python3 scan_kdoc.py` 重新扫描
2. 继续用 Workflow 分批处理（脚本在 `.claude/workflows/scripts/`）
3. `./gradlew compileKotlin` 验证编译
4. `./gradlew test` 确认无回归

---

## bb3a534 回滚修复后待补全双语 KDoc 的文件

bb3a534 提交中 KDoc 注入工具导致代码腐坏，修复过程中以下文件被回滚或清理，丢失了双语 KDoc，需重新补全：

### 因 KDoc 腐坏被整体回滚（双语 KDoc 完全丢失）

- [ ] SortBy.kt -- KDoc 中注入了 `fun asc(...)` 代码片段，已回滚至 bb3a534^，双语 KDoc 全部丢失
- [ ] UpdateAssignment.kt -- KDoc 中注入了 `fun set(...)` 代码片段，已回滚至 bb3a534^，双语 KDoc 全部丢失

### 因被重写而回滚至 bb3a534^（双语 KDoc 全部丢失）

- [ ] Position.kt -- 整个文件被替换为无关的 GPS 定位类，已回滚
- [ ] StowageContext.kt -- 整个文件被替换为数据容器类，已回滚
- [ ] SolutionAnalyzer.kt -- 整个文件被替换为统计解分析器，已回滚

### 因 KDoc 嵌套/代码注入被清理（双语 KDoc 部分丢失）

- [ ] CoptSolver.kt -- 2 个 KDoc 块被注入到 init 方法 KDoc 内部，已清理注入内容，部分双语 KDoc 保留
- [ ] CapacitySchedulingSolution.kt -- `val order: Int? = null` 被注入到 8 处 KDoc 中，已清理，但 3 个 data class 的中文 KDoc 标题丢失（仅剩英文）

### 因新建但引用不存在的类型而删除（bb3a534 新增的 KDoc 随文件删除）

- [ ] DataSerializer.kt -- 已删除
- [ ] ResultHelper.kt -- 已删除
- [ ] Demo4Application.kt -- 已删除
- [ ] Demo6Application.kt -- 已删除
- [ ] ItemCargoLimit.kt -- 已删除
- [ ] ItemCountLimit.kt -- 已删除
- [ ] PositionMaxLoadLimit.kt -- 已删除
