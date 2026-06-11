# BPP3D 每日工作记录

## 2026-06-11

### PWL 连续半径 v1 端到端验证闭环

1. **Renderer actualVolume 回写**（refactor.md 事项 2 遗留）：
   - `PackingRendererAdapter.toSchema`：当 `solverResult.pwlMetadata != null` 时使用 `pwlMetadata.actualVolume()` 计算真实 π\*r²\*h（而非用 solver 半径的近似 π\*q\*h）
   - PWL 诊断信息写入 renderer item info（pwl_volume、pwl_error、pwl_max_error、pwl_segments、pwl_within_envelope）
   - Renderer DTO `actualVolume` 字段正确反映真实圆柱体积

2. **PWL 负例测试**（refactor.md 事项 3）：
   - `PWLContinuousRadiusNegativeTest`（19 个测试）：
     - Big-M 边界验证（断点处精确、范围覆盖）
     - Envelope 溢出（r 超出 [rMin, rMax] 被标记为无效、溢出几何超出保守边界）
     - PWL 误差超限（极少段数误差大、足够段数误差小、PWL 过近似、无负体积）
     - Silent downgrade 防护（配置不含离散化字段、策略枚举不含 Discrete/Fallback、不允许退化为单点）
     - 互斥协议验证（保守 envelope 覆盖所有有效半径、支撑覆盖保守）
   - `PWLContinuousRadiusIntegrationNegativeTest`（7 个测试）：
     - PWLRadiusSelectionMetadata 验证（拒绝 numSegments=0、拒绝 blank source）
     - actualVolume 使用真实 r²（而非近似 q）、actualVolume < pwlVolume
     - Envelope 违规标记、诊断信息一致性
     - selectionSource 默认为 "pwl"

3. **Gurobi 端到端验证**（refactor.md 事项 1）：
   - GurobiColumnGenerationTest 通过（50 测试，0 失败）
   - Gurobi dataset suite 通过（50 测试，0 失败）

### 验收结果

1. BPP3D 全模块编译和测试通过
2. generic-boundary-check 通过
3. shape-boundary-check 通过
4. geometry-boundary-check 通过
5. geometry-module-dry-run 通过（8 个 internal baseline warning）
6. git diff --check 通过（仅 CRLF 警告）
7. Gurobi 列生成测试通过（50 测试）
8. Gurobi dataset suite 通过（50 测试）
9. PWL 负例测试全部通过（19 + 7 = 26 测试）

### 新增文件

- `bpp3d-infrastructure/src/test/**/PWLContinuousRadiusNegativeTest.kt`
- `bpp3d-domain-packing-context/src/test/**/PWLContinuousRadiusIntegrationNegativeTest.kt`

### 修改文件

- `bpp3d-domain-packing-context/src/main/**/PackingRendererAdapter.kt`（PWL actualVolume 回写 + PWL 诊断 info）
- `refactor.md`（更新完成状态和验收记录）

---

## 2026-06-10

## 本轮完成事项

### PWL 连续半径近似 v1 基础闭环

1. **PWL contract 定义**（refactor.md 事项 1）：
   - `PWLRadiusApproximationConfig`：PWL 配置（maxSegments、relativeErrorTolerance、breakpointStrategy、customBreakpoints、enableDebugInfo）
   - `PWLBreakpointStrategy`：三种策略（Uniform、Adaptive、ErrorDriven）
   - `PWLRadiusSquaredApproximation`：分段线性近似函数 q ≈ r²，包含断点生成、弦线斜率/截距计算、误差估计和求值

2. **Solver 注册路径**（refactor.md 事项 3）：
   - 基于 core `UnivariateLinearPiecewiseFunction`：使用 Big-M + 二值选择变量方法
   - `PWLContinuousRadiusSolverVariable`：持有 radius 变量、core PWL 函数、PWL 近似函数和保守 envelope
   - `registerPWLContinuousRadiusVariables`：将 r 上下界和 PWL 函数的辅助变量/约束注册到 LinearMetaModel
   - Big-M 自动推导：从 rMin/rMax 和 PWL 输出范围计算

3. **保守几何 envelope**（refactor.md 事项 4）：
   - `ConservativeRadiusEnvelope`：使用 rMax 保守建模 footprint、bounding dimensions、支撑覆盖和碰撞边界
   - 包含真实几何计算方法（realFootprintWidth/Depth/Height 等）
   - 包含 envelope 验证方法（isRadiusValid）

4. **结果回写**（refactor.md 事项 5）：
   - `PWLExtractedRadius`：从 solver 结果提取 r、q、误差和 envelope 信息
   - `PWLRadiusSelectionMetadata`：记录 PWL 近似元数据（solverRadiusSquared、actualRadiusSquared、误差、段数等）
   - `CylinderRadiusSelectionResult.pwlMetadata`：扩展选择结果包含 PWL 元数据
   - `withPWLSolverSelectedRadius`：PWL 专用回写函数
   - `buildPWLContinuousRadiusSelectionResults`：PWL 结果回写到 ColumnGenerationPackingAnalyzer

5. **互斥与诊断**（refactor.md 事项 6）：
   - `isPWLRegisterable`：PWL 路径互斥条件（gaps 仅含 interval-unsupported、有 bounds、无 initialRadius）
   - `continuousRadiusSolverVariableRegistrationPlan`：四路径分类（native、PWL、productionReady、blocked）
   - 诊断信息包含 mutual_exclusion_summary 和 PWL 详细信息

6. **测试**（refactor.md 事项 7）：
   - `PWLRadiusSquaredApproximationTest`：22 个测试覆盖 uniform/adaptive/error-driven 断点、求值、误差、自定义断点和边界
   - `ConservativeRadiusEnvelopeTest`：14 个测试覆盖 footprint、bounding、real geometry、validation 和边界

7. **CylinderShapeContract 扩展**：
   - `isPWLRegisterable` 属性：标识可通过 PWL 路径注册的原型
   - `PWLRadiusSelectionMetadata` 数据类：PWL 元数据
   - `withPWLSolverSelectedRadius` 扩展函数：PWL 结果回写

### 重构：用 core UnivariateLinearPiecewiseFunction 替代手写约束

初始实现使用手写的 lambda/SOS2 约束注册，存在 SOS2 solver-registration gap。重构后：
- 使用 core 提供的 `UnivariateLinearPiecewiseFunction`（Big-M + 二值选择变量）
- 消除了 SOS2 solver-registration gap——core 的 Big-M 方法不需要 SOS2 约束
- 保留了 `UnivariateLinearPiecewiseFunction` 的 evaluate、resultPolynomial 和 helperVariables 能力
- 在 LinearMetaModel 上手动注册约束（因为 core 的 registerConstraints 需要 AbstractLinearMechanismModel）

## 代码质量修复

- 移除 `PWLRadiusSquaredApproximation` 和 `PWLContinuousRadiusRegistration` 中的 `.abs()` 扩展函数（FltX 已有成员）
- 移除 `ConservativeRadiusEnvelope` 中未使用的 import（`times`、`Meter`）
- 优化 `evaluate()` 方法：使用 FltX 比较替代 toDouble() 比较
- 优化 chord 斜率计算：直接使用 slope = r0 + r1 和 intercept = -r0 * r1
- 优化 ErrorDriven 策略：只对误差最大的段做二分细分（而非全部段加中点）

## 验收结果

1. BPP3D 全模块编译通过
2. BPP3D 全模块测试通过（36+ 基础设施测试 + 全模块测试）
3. generic-boundary-check 通过
4. shape-boundary-check 通过
5. geometry-boundary-check 通过
6. geometry-module-dry-run 通过（8 个 internal baseline warning）
7. git diff --check 通过（仅 CRLF 警告）

## 剩余工作

1. Gurobi 端到端验证（实际 interval-only 数据集）
2. PWL actualVolume/pwlVolume 写入 renderer DTO 和 packing snapshot
3. PWL 负例测试（Big-M 边界、envelope 溢出、silent downgrade 防护）

## 新增/修改文件

### 新增文件
- `bpp3d-infrastructure/src/main/**/PWLRadiusApproximationConfig.kt`
- `bpp3d-infrastructure/src/main/**/PWLRadiusSquaredApproximation.kt`
- `bpp3d-infrastructure/src/main/**/ConservativeRadiusEnvelope.kt`
- `bpp3d-infrastructure/src/test/**/PWLRadiusSquaredApproximationTest.kt`
- `bpp3d-infrastructure/src/test/**/ConservativeRadiusEnvelopeTest.kt`
- `bpp3d-application/src/main/**/PWLContinuousRadiusRegistration.kt`

### 修改文件
- `bpp3d-domain-item-context/src/main/**/CylinderShapeContract.kt`（isPWLRegisterable + PWLRadiusSelectionMetadata + withPWLSolverSelectedRadius）
- `bpp3d-application/src/main/**/ContinuousRadiusSolverRegistrationPlan.kt`（PWL 路径分类 + 互斥诊断）
- `bpp3d-application/src/main/**/ColumnGenerationStandardExecutors.kt`（PWL 变量注册集成）
- `bpp3d-application/src/main/**/ColumnGenerationPackingAnalyzer.kt`（PWL 结果回写）
