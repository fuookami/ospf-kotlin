# Demo2 Kotlin vs Rust 对比分析与改进计划

## 一、现状对比（修正版）

### 1. 应用类型（Application Types）

| 维度 | Rust | Kotlin |
|------|------|--------|
| FullLoadApplication | 有（MILP + Benders 双路径） | 有（仅 MILP 路径，Benders 为 TODO） |
| PredistributionApplication | 有（MILP + Benders 双路径） | 有（仅 MILP 路径，Benders 为 TODO） |
| WeightRecommendationApplication | 有（MILP + Benders 双路径） | 有（仅 MILP 路径，Benders 为 TODO） |
| LoadingOrderApplication | 有 | 有 |

**差距**: Kotlin 4 个应用类型齐全，但 Benders 路径全部是 `TODO("not implemented yet")`。

### 2. Benders 分解算法

| 维度 | Rust | Kotlin |
|------|------|--------|
| LinearBendersDecompositionSolver 接口 | 有（框架层） | 有（`QuadraticBendersDecompositionSolver.kt`，含 Linear 接口） |
| Benders 求解器插件（Gurobi/SCIP/CPLEX/COPT/MindOpt） | 有（Gurobi） | 有（5 个插件实现） |
| SolvePolicy（prefer_benders / fallback_to_milp） | 有 | 无 |
| BendersAdaptiveConfig（自适应迭代/容差） | 有 | 无 |
| 按问题规模自动调参（tune_benders_adaptive_config） | 有 | 无 |
| Quality Guard 系统（gap/time/progress/cut_efficiency/trajectory） | 有 | 无 |
| Quality Score 综合评分 | 有 | 无 |
| Benders 失败回退 MILP | 有 | 无 |
| Benders 模型构建（master/sub 分离） | 有（build_benders_models） | 无（但各 Context 已有 register 方法） |

**差距**: Kotlin 框架层已有 Benders 求解器基础设施（接口 + 5 个插件），但 demo2 应用层未接入。Rust 在应用层有约 600 行 Benders 编排逻辑（adaptive tuning、quality guard、score、fallback）需要迁移。

### 3. Pipeline 系统

| 维度 | Rust | Kotlin |
|------|------|--------|
| Demo2PipelineMode | 有（FullLoad/Predistribution/WeightRecommendation） | 有等价物（`StowageMode` 枚举） |
| PipelineSpec（priority + ModeSelector） | 有 | 无（各 Context 直接用 StowageMode 分支） |
| ModeSelector 条件过滤 | 有 | 无 |
| Domain Pipeline 统一编排 | 有（`apply_domain_pipeline`） | 无（各 Application 手动串联 Context） |

**差距**: Kotlin 通过 `StowageMode` 已有模式切换，但缺少 Rust 的 PipelineSpec 抽象和统一编排器。各 Application 中的 `init()` + `register()` 手动串联是等价实现，但代码重复度高。

### 4. 领域覆盖

| 领域 | Rust | Kotlin |
|------|------|--------|
| stowage | 有（assignment_limit） | 有（AggregationInitializer，更丰富） |
| airworthiness | 有（5 个 limits） | 有（12 个 limits，更细粒度） |
| mac_optimization | 有（balance_limit + moment_limit） | 有（3 个 limits） |
| loading_effectiveness | 有（2 个 limits） | 有（10 个 limits，更丰富） |
| express_effectiveness | 有（1 个 limit） | 有（2 个 limits） |
| soft_security | 有（2 个 limits） | 有（AggregationInitializer） |
| redundancy | 有（1 个 limit） | 有（AggregationInitializer） |
| mac（MAC/Torque 计算） | 无独立域 | 有（MAC/Torque 模型） |
| payload_maximization | 无 | 有（Aggregation） |
| recommended_weight_equalization | 无 | 有（含 PriorityAppointment） |
| aircraft（飞机模型） | 简化（仅 name/weight/priority） | 完整（Deck/ULD/FlightPhase/HatchDoor/Fuselage/Formula 等） |

**结论**: Kotlin 领域模型更丰富，这是优势，无需反向迁移。

### 5. 诊断系统（Diagnostics）

| 维度 | Rust | Kotlin |
|------|------|--------|
| 结构化 DiagnosticNote（level/group/code/message） | 有 | 无 |
| 可行性预检（envelope/payload/capacity） | 有 | 无 |
| 约束临界预警（capacity utilization 98%） | 有 | 无 |
| Benders 运行时诊断（iterations/gap/time/score） | 有 | 无 |

**差距**: Kotlin 没有诊断系统。这是 Rust 独有的能力。

### 6. 请求/响应 DTO

| 维度 | Rust | Kotlin |
|------|------|--------|
| Demo2Request | 完整（cargos/positions/aircraft_type/solve_policy/benders_config/...） | 桩（仅 `id: String`，parameter TODO） |
| Demo2Response | 含 status/objective/assignments/notes/diagnostics | 仅 `succeed: Boolean` |
| LoadingOrderResponse | 含 status/orders/notes/diagnostics | 仅 `succeed: Boolean` |

**差距**: Kotlin 的 DTO 是未完成的桩代码，需要对齐 Rust 的字段。

### 7. 测试

| 维度 | Rust | Kotlin |
|------|------|--------|
| 集成测试（4 个应用 + 分支场景） | 有 | 无 |
| 单元测试（diagnostics / pipeline_policy） | 有 | 无 |
| FrameworkDemoTest | - | 仅 `assertTrue(true)` 烟雾测试 |
| BendersCutApiTest（核心层） | - | 有（在 ospf-kotlin-core 中） |

**差距**: Kotlin demo2 层没有有效测试。

### 8. 心跳/进度回调

| 维度 | Rust | Kotlin |
|------|------|--------|
| 进度回调 | 无 | 有（`RunningHeartBeatDTO` + `FinnishHeartBeatDTO`） |
| 求解状态回调 | 无 | 有（`registrationStatusCallBack` + `solvingStatusCallBack`） |

**优势**: Kotlin 有 Rust 缺少的进度回调机制。

---

## 二、改进计划

### Phase 1: 基础设施补齐

#### P1-1: 完善 Request/Response DTO
- 将 `RequestDTO` 扩展为包含 cargos、positions、aircraft_type、solve_policy、benders_adaptive 等字段
- 将 `ResponseDTO` / `LoadingOrderResponseDTO` 扩展为包含 status、objective、assignments、notes、diagnostics
- 添加 `SolvePolicy`、`BendersAdaptiveConfig`、`DiagnosticNote`、`BendersQualityOverrideConfig` 等支撑类型
- 参考 Rust 的 `Demo2Request.sample()` 提供 Kotlin 的 sample 数据工厂
- **注意**: Kotlin 的 `Parameter` + `SemanticParameter` 已有部分字段，需确认是否可复用

#### P1-2: 实现诊断系统
- 创建 `diagnostics` 包，实现 `DiagnosticNote` 数据类
- 实现 `pushGroupedNote` 和 `buildStructuredDiagnostics` 解析器
- 定义所有 NOTE_LEVEL / NOTE_GROUP / NOTE_CODE 常量
- 实现 `appendCoreFeasibilityDiagnostics`（envelope/payload/capacity 预检）
- 实现 `appendCriticalConstraintNotes`（求解后临界预警）

### Phase 2: Benders 分解接入

#### P2-1: Benders 模型构建
- 在 `FullLoadAlgorithmImpl` 中实现 `solveWithBendersAlgorithm`
- 利用已有的 `LinearBendersDecompositionSolver` 接口和 Gurobi 插件
- 构建 master 模型（stowage + mac + airworthiness + soft_security + mac_optimization + express + loading + redundancy 的部分约束）
- 构建 sub 模型（airworthiness 约束作为子问题）
- 为 PredistributionApplication 和 WeightRecommendationApplication 同样实现 Benders 路径

#### P2-2: 自适应 Benders 求解策略
- 实现 `SolveMode` 决策（Milp / Benders）
- 实现 `tuneBendersAdaptiveConfig`（按二元变量数量自动调参：迭代次数 boost、tolerance 放松、stall window）
- 实现 `resolveSolveMode`（策略选择 + 诊断输出）
- 实现 Benders 失败回退 MILP 逻辑

#### P2-3: Quality Guard 系统
- 实现 `BendersQualityGuardConfig` 及其默认值
- 实现 5 维质量评估：
  - gap_guard：gap 超过 tolerance * 100 的阈值
  - time_guard：按迭代数和容差估算时间上限
  - progress_guard：迭代压力（90% max_iterations 时 gap 仍大）
  - cut_efficiency：cut 密度低于阈值
  - trajectory：目标函数步进改善低于阈值
- 实现 `resolveBendersQualityScore` 综合评分（加权 5 维风险）
- 实现 `resolveBendersQualityReason` 决策
- 实现 fallback_to_milp / accept_benders 动作

### Phase 3: Pipeline 优化（可选）

#### P3-1: 抽取公共编排逻辑
- 从 FullLoad/Predistribution/WeightRecommendation 的 `init()` + `register()` 中抽取公共 pipeline 编排
- 减少代码重复（当前 3 个 Application 各有 ~200 行几乎相同的 init/register 代码）

### Phase 4: 测试

#### P4-1: 单元测试
- `DiagnosticsTest`：结构化诊断解析、分组、级别
- `PipelinePolicyTest`：若实现了 PipelineSpec，测试排序与模式过滤
- `BendersAdaptiveConfigTest`：自适应参数调优逻辑

#### P4-2: 集成测试
- `FullLoadTest`：标准样本求解 + 结果验证
- `PredistributionTest`：预分配模式求解
- `WeightRecommendationTest`：推荐重量模式求解
- `LoadingOrderTest`：装载顺序生成
- `UnsupportedAircraftTest`：不支持机型的分支处理
- `BendersNoFallbackTest`：Benders 失败且不回退的场景

---

## 三、优先级排序

| 优先级 | 任务 | 理由 |
|--------|------|------|
| **高** | P1-1 DTO 完善 | 所有后续工作的基础，当前 DTO 是桩代码 |
| **高** | P1-2 诊断系统 | Rust 核心能力，影响所有应用的可观测性 |
| **高** | P2-1 Benders 模型构建 | Kotlin 框架层已有求解器，只差应用层接入 |
| **高** | P2-2 自适应策略 | Benders 的核心价值在于自适应调参 |
| **中** | P2-3 Quality Guard | Benders 质量保障，防止劣质解 |
| **中** | P3 Pipeline 优化 | 代码质量改善，非功能阻塞 |
| **中** | P4-1 单元测试 | 质量保障 |
| **中** | P4-2 集成测试 | 端到端验证 |

---

## 四、关键发现

1. **Kotlin 领域模型更丰富**: airworthiness 12 limits vs Rust 5，loading_effectiveness 10 vs 2，还有独立的 mac/payload_maximization/recommended_weight_equalization 域
2. **Kotlin 框架层 Benders 基础设施完备**: 5 个求解器插件 + LinearBendersDecompositionSolver 接口，只差 demo2 应用层接入
3. **Kotlin 有进度回调机制**: RunningHeartBeatDTO + solvingStatusCallBack，Rust 没有
4. **最大差距是 Benders 应用层编排**: ~600 行 Rust 逻辑（adaptive tuning + quality guard + score + fallback）需要迁移
5. **DTO 和测试是短板**: DTO 是桩代码，测试仅有烟雾测试

---

## 五、实施进度

### 已完成

#### P1-1: DTO 完善
- `RequestDTO` 扩展为包含 cargos/positions/aircraftType/solvePolicy/bendersAdaptive 等字段，与 Rust 对齐
- 新增支撑类型：`CargoInput`、`PositionInput`、`AircraftTypeInput`、`SolvePolicy`、`BendersAdaptiveConfig`、`BendersQualityOverrideConfig`、`WeightRecommendationObjectiveConfig`
- `ResponseDTO` 扩展为包含 status/objective/assignments/notes/diagnostics
- `LoadingOrderResponseDTO` 扩展为包含 status/orders/notes/diagnostics
- `RequestDTO.sample()` 提供与 Rust `Demo2Request::sample()` 等价的测试数据
- `ResponseDTO.noSolution()` / `ResponseDTO.optimal()` 工厂方法

#### P1-2: 诊断系统
- `Diagnostics` 对象：常量定义 + `pushGroupedNote` + `buildStructured` 解析器
- `DiagnosticNote` 数据类
- `FeasibilityDiagnostics` 对象：`appendCoreFeasibilityDiagnostics` + `appendCriticalConstraintNotes`
- 与 Rust 的 `diagnostics.rs` 完全对齐

#### P2-1: Benders 策略层
- `BendersStrategy` 对象：自适应配置调优、求解模式决策、质量评估
- `EffectiveBendersAdaptiveConfig`：按问题规模自动调参（迭代 boost/tolerance 放松/stall window）
- `BendersQualityGuardConfig`：5 维质量评估（gap/time/progress/cut_efficiency/trajectory）
- `resolveQualityScore`：综合评分（加权风险）
- `resolveQualityReason`：决策（gap_guard_exceeded/time_guard_exceeded 等）

#### P2-2: Benders 应用层集成
- `FullLoadApplication`：集成 Benders 策略 + MILP 回退
- `PredistributionApplication`：集成 Benders 策略 + MILP 回退
- `WeightRecommendationApplication`：集成 Benders 策略 + MILP 回退
- `BendersSolver`：Benders 迭代循环骨架（使用框架层 `LinearBendersDecompositionSolver`）

#### P4: 测试
- `DiagnosticsTest`：4 个测试（解析/回退/格式化/多笔记）
- `BendersStrategyTest`：10 个测试（机型支持/自适应调参/求解模式决策/质量评估）
- `FeasibilityDiagnosticsTest`：5 个测试（有效请求/envelope/payload/capacity）
- `RequestDTOTest`：8 个测试（sample 数据/DTO 结构/工厂方法）

### 待完成

- P3: Pipeline 优化（从 3 个 Application 的 init/register 中抽取公共编排逻辑）
- P4-2: 集成测试（需要可用的求解器环境）

#### 补充完成
- `BendersSolver`：完整迭代循环 + cut 添加（通过 `masterModel.addConstraint(cut, group)`）
- `DomainPipeline`：`FullLoadPipelineContext` / `PredistributionPipelineContext` 封装公共 init/register 链
- `TryHelpers`：`orReturn` 扩展函数减少 when/Ok/Failed/Fatal 样板代码

#### P5: 各域 registerForBendersMP/SP 实现

所有域 Context 和 Aggregation 的 `registerForBendersMP` / `registerForBendersSP` / `flushForBendersSP` 方法已全部实现，消除了所有 Benders 相关 TODO。

**实现策略**：
- **Master-problem 域**（stowage, mac, mac_optimization, soft_security, express_effectiveness, loading_effectiveness, redundancy, recommended_weight_equalization, payload_maximization）：`registerForBendersMP` 委托给 `register`，`registerForBendersSP` 返回 `ok`
- **Sub-problem 域**（airworthiness_security）：`registerForBendersMP` 返回 `ok`，`registerForBendersSP` 委托给 `register`（飞适航性约束构成子问题）
- **StowageAggregation**（特殊）：MP 注册 stowage + load，SP 注册 payload + totalWeight + maxLoadWeight（飞适航性子域）

**已实现的文件**：
- `MacContext` / `MacAggregation` - MAC 计算约束归入 master
- `MacOptimizationContext` / `MacOptimizationAggregation` - MAC 优化约束归入 master
- `AirworthinessSecurityContext` / `AirworthinessSecurityAggregation` - 飞适航性约束归入 sub
- `ExpressEffectivenessContext` / `ExpressEffectivenessAggregation` - 快递效率约束归入 master
- `LoadingEffectivenessContext` / `LoadingEffectivenessAggregation` - 装载效率约束归入 master
- `RedundancyContext` / `RedundancyAggregation` - 冗余约束归入 master
- `RecommendedWeightEqualizationContext` - 重量均衡约束归入 master
- `PayloadMaximizationContext` - 载荷最大化目标归入 master
