/**
 * 连续半径模型组件测试：组件注册、配置注入、结果提取和四路径分类。
 * Continuous radius model component tests: component registration, config injection,
 * result extraction, and four-path classification.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ConservativeRadiusEnvelope
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLBreakpointStrategy
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusApproximationConfig
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusSquaredApproximation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking

class ContinuousRadiusModelComponentTest {

    // ===== 辅助方法 / Helper methods =====

    /**
     * 创建 native-path 原型（固定半径，无 gap）。
     * Create a native-path prototype (fixed radius, no gap).
     */
    private fun nativePrototype(
        variableName: String = "native_r",
        radius: Double = 2.0,
        axis: Axis3 = Axis3.Y
    ): ContinuousCylinderRadiusSolverPrototype {
        return ContinuousCylinderRadiusSolverPrototype(
            source = "test",
            radiusWeightFunctionKey = "key_$variableName",
            axis = axis,
            variableName = variableName,
            radiusLowerBound = Quantity(infraScalar(radius), Meter),
            radiusUpperBound = Quantity(infraScalar(radius), Meter),
            initialRadius = Quantity(infraScalar(radius), Meter),
            gaps = emptyList()
        )
    }

    /**
     * 创建 PWL-path 原型（interval-only，无 initialRadius，有 SolverNative gap）。
     * Create a PWL-path prototype (interval-only, no initialRadius, with SolverNative gap).
     */
    private fun pwlPrototype(
        variableName: String = "pwl_r",
        rMin: Double = 2.0,
        rMax: Double = 5.0,
        axis: Axis3 = Axis3.Y
    ): ContinuousCylinderRadiusSolverPrototype {
        return ContinuousCylinderRadiusSolverPrototype(
            source = "test",
            radiusWeightFunctionKey = "key_$variableName",
            axis = axis,
            variableName = variableName,
            radiusLowerBound = Quantity(infraScalar(rMin), Meter),
            radiusUpperBound = Quantity(infraScalar(rMax), Meter),
            initialRadius = null,
            gaps = listOf(ContinuousCylinderRadiusOptimizationGap.SolverNativeRadiusIntervalUnsupported)
        )
    }

    /**
     * 创建 blocked 原型（无 bounds，无法注册）。
     * Create a blocked prototype (no bounds, cannot register).
     */
    private fun blockedPrototype(
        variableName: String = "blocked_r"
    ): ContinuousCylinderRadiusSolverPrototype {
        return ContinuousCylinderRadiusSolverPrototype(
            source = "test",
            radiusWeightFunctionKey = "key_$variableName",
            axis = Axis3.Y,
            variableName = variableName,
            radiusLowerBound = null,
            radiusUpperBound = null,
            initialRadius = null,
            gaps = listOf(ContinuousCylinderRadiusOptimizationGap.SolverNativeRadiusIntervalUnsupported)
        )
    }

    // ===== 1. 组件注册测试 / Component registration tests =====

    @Test
    fun testComponentCreatesNativeVariablesForNativePrototypes() {
        val prototypes = listOf(nativePrototype())
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig()
        )
        assertEquals(1, component.nativeVariables.size, "Should create 1 native variable")
        assertEquals(0, component.pwlVariables.size, "Should create 0 PWL variables")
        assertEquals("native_r", component.nativeVariables[0].prototype.variableName)
    }

    @Test
    fun testComponentCreatesPWLVariablesForPWLPrototypes() {
        val prototypes = listOf(pwlPrototype())
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        assertEquals(0, component.nativeVariables.size, "Should create 0 native variables")
        assertEquals(1, component.pwlVariables.size, "Should create 1 PWL variable")
        assertEquals("pwl_r", component.pwlVariables[0].prototype.variableName)
    }

    @Test
    fun testComponentCreatesMixedVariablesForMixedPrototypes() {
        val prototypes = listOf(
            nativePrototype("native1"),
            pwlPrototype("pwl1"),
            blockedPrototype("blocked1")
        )
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig()
        )
        assertEquals(1, component.nativeVariables.size, "Should create 1 native variable")
        assertEquals(1, component.pwlVariables.size, "Should create 1 PWL variable")
        assertEquals("native1", component.nativeVariables[0].prototype.variableName)
        assertEquals("pwl1", component.pwlVariables[0].prototype.variableName)
    }

    @Test
    fun testComponentRegistersPWLHelperVariablesAndTokens() {
        val prototypes = listOf(pwlPrototype())
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val pwlVar = component.pwlVariables[0]
        // PWL function should have selector variables for each segment
        assertTrue(
            pwlVar.pwlFunction.selectorVars.size > 0,
            "PWL function should have selector variables"
        )
        // Helper variables should be populated
        assertTrue(
            pwlVar.pwlFunction.helperVariables.isNotEmpty(),
            "PWL function should have helper variables"
        )
        // Result variable should exist
        assertNotNull(pwlVar.pwlFunction.resultVar, "PWL function should have a result variable")
    }

    // ===== 2. 配置注入测试 / Config injection tests =====

    @Test
    fun testDifferentConfigAffectsSegmentCount() {
        val prototypes = listOf(pwlPrototype(rMin = 1.0, rMax = 10.0))
        val component4 = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val component8 = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 8, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val segments4 = component4.pwlVariables[0].pwlApproximation.numSegments
        val segments8 = component8.pwlVariables[0].pwlApproximation.numSegments
        assertEquals(4, segments4, "4-segment config should produce 4 segments")
        assertEquals(8, segments8, "8-segment config should produce 8 segments")
    }

    @Test
    fun testDifferentConfigAffectsBreakpoints() {
        val prototypes = listOf(pwlPrototype(rMin = 2.0, rMax = 5.0))
        val componentUniform = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val componentAdaptive = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Adaptive)
        )
        val uniformBp = componentUniform.pwlVariables[0].pwlApproximation.breakpoints
        val adaptiveBp = componentAdaptive.pwlVariables[0].pwlApproximation.breakpoints
        // Both should have 5 breakpoints (4 segments + 1)
        assertEquals(5, uniformBp.size)
        assertEquals(5, adaptiveBp.size)
        // Uniform breakpoints should be equally spaced
        val uniformSpacing = (uniformBp[1] - uniformBp[0]).toDouble()
        for (i in 1 until uniformBp.size - 1) {
            assertEquals(
                uniformSpacing,
                (uniformBp[i + 1] - uniformBp[i]).toDouble(),
                1e-10,
                "Uniform breakpoints should be equally spaced"
            )
        }
    }

    @Test
    fun testErrorDrivenStrategyProducesValidApproximation() {
        val prototypes = listOf(pwlPrototype(rMin = 1.0, rMax = 5.0))
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(
                maxSegments = 8,
                breakpointStrategy = PWLBreakpointStrategy.ErrorDriven,
                relativeErrorTolerance = infraScalar(0.005)
            )
        )
        val pwlVar = component.pwlVariables[0]
        assertTrue(
            pwlVar.pwlApproximation.maxRelativeError.toDouble() < 0.01,
            "Error-driven with tolerance 0.005 should produce maxRelError < 0.01"
        )
    }

    @Test
    fun testDebugInfoConfigAffectsConstraintDescriptions() {
        val prototypes = listOf(pwlPrototype())
        val componentNoDebug = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4, enableDebugInfo = false)
        )
        val componentDebug = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4, enableDebugInfo = true)
        )
        val noDebugDescs = componentNoDebug.pwlVariables[0].constraintDescriptions()
        val debugDescs = componentDebug.pwlVariables[0].constraintDescriptions()
        // Debug descriptions should be longer when enableDebugInfo = true
        assertTrue(
            debugDescs.size > noDebugDescs.size,
            "Debug descriptions should include more details when enableDebugInfo = true"
        )
    }

    // ===== 3. 注册计划测试 / Registration plan tests =====

    @Test
    fun testRegistrationPlanClassifiesFourPaths() {
        val prototypes = listOf(
            nativePrototype("native1"),
            pwlPrototype("pwl1"),
            blockedPrototype("blocked1")
        )
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig()
        )
        val plan = component.registrationPlan
        assertEquals(3, plan.variableNames.size, "Should have 3 variables in plan")
        assertEquals(1, plan.registeredVariables.size, "Should have 1 native-registered variable")
        assertEquals(1, plan.pwlRegisteredVariables.size, "Should have 1 PWL-registered variable")
        assertEquals(1, plan.modelRegistrationBlockedVariables.size, "Should have 1 blocked variable")
        assertEquals(1, plan.productionReadyVariables.size, "Should have 1 production-ready variable (native with initialRadius)")
    }

    @Test
    fun testRegistrationPlanInfoContainsMutualExclusionSummary() {
        val prototypes = listOf(
            nativePrototype("native1"),
            pwlPrototype("pwl1"),
            blockedPrototype("blocked1")
        )
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig()
        )
        val info = component.info()
        assertTrue(info.containsKey("continuous_radius_solver_mutual_exclusion_summary"))
        val summary = info["continuous_radius_solver_mutual_exclusion_summary"]!!
        assertTrue(summary.contains("native=1"), "Summary should report native=1")
        assertTrue(summary.contains("pwl=1"), "Summary should report pwl=1")
        assertTrue(summary.contains("blocked=1"), "Summary should report blocked=1")
        assertTrue(summary.contains("productionReady=1"), "Summary should report productionReady=1")
    }

    @Test
    fun testRegistrationPlanInfoContainsPWLVariables() {
        val prototypes = listOf(pwlPrototype())
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val info = component.info()
        assertTrue(info.containsKey("continuous_radius_solver_pwl_registered_variables"))
        val pwlInfo = info["continuous_radius_solver_pwl_registered_variables"]!!
        assertTrue(pwlInfo.contains("pwl_r"), "PWL info should contain variable name")
        assertTrue(pwlInfo.contains("segments=4"), "PWL info should contain segment count")
    }

    // ===== 4. PWLExtractedRadius 值对象测试 / PWLExtractedRadius value object tests =====

    @Test
    fun testPWLExtractedRadiusComputesVolumes() {
        val rMin = infraScalar(2.0)
        val rMax = infraScalar(5.0)
        val pwlApproximation = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val envelope = ConservativeRadiusEnvelope(
            rMin = rMin,
            rMax = rMax
        )
        val extracted = PWLExtractedRadius(
            variableName = "test_r",
            solverRadius = infraScalar(3.0),
            solverRadiusSquared = infraScalar(9.5),
            actualRadiusSquared = infraScalar(9.0),
            pwlAbsoluteError = infraScalar(0.5),
            pwlRelativeError = infraScalar(0.5 / 9.0),
            isWithinEnvelope = true,
            envelope = envelope,
            pwlApproximation = pwlApproximation
        )
        val height = infraScalar(10.0)
        val pi = infraScalar(Math.PI)
        // actualVolume = π * r² * h = π * 9 * 10 = 90π
        assertEquals(Math.PI * 9.0 * 10.0, extracted.actualVolume(height, pi).toDouble(), 1e-6)
        // pwlVolume = π * q * h = π * 9.5 * 10 = 95π
        assertEquals(Math.PI * 9.5 * 10.0, extracted.pwlVolume(height, pi).toDouble(), 1e-6)
    }

    @Test
    fun testPWLExtractedRadiusInfoContainsDiagnostics() {
        val rMin = infraScalar(2.0)
        val rMax = infraScalar(5.0)
        val pwlApproximation = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val envelope = ConservativeRadiusEnvelope(
            rMin = rMin,
            rMax = rMax
        )
        val extracted = PWLExtractedRadius(
            variableName = "test_r",
            solverRadius = infraScalar(3.0),
            solverRadiusSquared = infraScalar(9.5),
            actualRadiusSquared = infraScalar(9.0),
            pwlAbsoluteError = infraScalar(0.5),
            pwlRelativeError = infraScalar(0.5 / 9.0),
            isWithinEnvelope = true,
            envelope = envelope,
            pwlApproximation = pwlApproximation
        )
        val info = extracted.info()
        assertTrue(info.containsKey("pwl_radius_test_r_r"), "Should contain r value")
        assertTrue(info.containsKey("pwl_radius_test_r_q"), "Should contain q value")
        assertTrue(info.containsKey("pwl_radius_test_r_r_squared"), "Should contain r² value")
        assertTrue(info.containsKey("pwl_radius_test_r_abs_error"), "Should contain absolute error")
        assertTrue(info.containsKey("pwl_radius_test_r_rel_error"), "Should contain relative error")
        assertTrue(info.containsKey("pwl_radius_test_r_within_envelope"), "Should contain envelope status")
        assertTrue(info.containsKey("pwl_radius_test_r_pwl_max_rel_error"), "Should contain max PWL relative error")
    }

    // ===== 5. 四路径互斥测试 / Four-path mutual exclusion tests =====

    @Test
    fun testNativePrototypeIsSolverRegisterableNotPWL() {
        val proto = nativePrototype()
        assertTrue(proto.isSolverRegisterable, "Native prototype should be solver-registerable")
        assertFalse(proto.isPWLRegisterable, "Native prototype should not be PWL-registerable")
    }

    @Test
    fun testPWLPrototypeIsPWLRegisterableNotNative() {
        val proto = pwlPrototype()
        assertFalse(proto.isSolverRegisterable, "PWL prototype should not be solver-registerable")
        assertTrue(proto.isPWLRegisterable, "PWL prototype should be PWL-registerable")
    }

    @Test
    fun testBlockedPrototypeIsNeitherRegisterable() {
        val proto = blockedPrototype()
        assertFalse(proto.isSolverRegisterable, "Blocked prototype should not be solver-registerable")
        assertFalse(proto.isPWLRegisterable, "Blocked prototype should not be PWL-registerable")
    }

    @Test
    fun testProductionReadyRequiresInitialRadiusAndNoGaps() {
        val native = nativePrototype()
        assertTrue(native.isProductionReady, "Native with initialRadius and no gaps should be production-ready")
        val pwl = pwlPrototype()
        assertFalse(pwl.isProductionReady, "PWL with no initialRadius should not be production-ready")
    }

    // ===== 6. 空原型列表测试 / Empty prototype list tests =====

    @Test
    fun testComponentWithEmptyPrototypes() {
        val component = ContinuousRadiusModelComponent(
            prototypes = emptyList(),
            config = PWLRadiusApproximationConfig()
        )
        assertTrue(component.nativeVariables.isEmpty())
        assertTrue(component.pwlVariables.isEmpty())
        val info = component.info()
        assertEquals("0", info["continuous_radius_solver_registration_plan_count"])
    }

    // ===== 7. 多 PWL 原型测试 / Multiple PWL prototype tests =====

    @Test
    fun testMultiplePWLPrototypesWithDifferentRanges() {
        val prototypes = listOf(
            pwlPrototype("pwl1", rMin = 1.0, rMax = 3.0),
            pwlPrototype("pwl2", rMin = 2.0, rMax = 8.0),
            pwlPrototype("pwl3", rMin = 0.5, rMax = 2.0)
        )
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        assertEquals(3, component.pwlVariables.size, "Should create 3 PWL variables")
        // Each should have independent breakpoints
        val breakpoints1 = component.pwlVariables[0].pwlApproximation.breakpoints
        val breakpoints2 = component.pwlVariables[1].pwlApproximation.breakpoints
        val breakpoints3 = component.pwlVariables[2].pwlApproximation.breakpoints
        // Different ranges should produce different breakpoint values
        assertFalse(
            breakpoints1.map { it.toDouble() } == breakpoints2.map { it.toDouble() },
            "Different ranges should produce different breakpoints"
        )
    }

    // ===== 8. 模型规模 KPI 测试 / Model scale KPI tests =====

    @Test
    fun testModelScaleInfoWithPWLVariables() {
        val prototypes = listOf(
            pwlPrototype("pwl1", rMin = 2.0, rMax = 5.0),
            pwlPrototype("pwl2", rMin = 1.0, rMax = 8.0)
        )
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val kpi = component.modelScaleInfo()
        assertEquals("2", kpi["pwl_total_prototypes"], "Should have 2 PWL prototypes")
        // Each with 4 segments
        assertEquals("8", kpi["pwl_total_segments"], "Should have 8 total segments (2*4)")
        // Per variable: 4 selector vars + 1 select-one constraint + 4*4=16 segment constraints = 17 constraints
        assertEquals("34", kpi["pwl_total_constraints"], "Should have 34 total constraints (2 * (1 + 4*4))")
        assertEquals("4", kpi["pwl_max_segments"], "Max segments should be 4")
        assertTrue(kpi.containsKey("pwl_total_selector_vars"), "Should contain selector var count")
        assertTrue(kpi.containsKey("pwl_total_helper_vars"), "Should contain helper var count")
        assertTrue(kpi.containsKey("pwl_max_relative_error"), "Should contain max relative error")
        assertTrue(kpi.containsKey("pwl_avg_relative_error"), "Should contain avg relative error")
    }

    @Test
    fun testModelScaleInfoWithEmptyPrototypes() {
        val component = ContinuousRadiusModelComponent(
            prototypes = emptyList(),
            config = PWLRadiusApproximationConfig()
        )
        val kpi = component.modelScaleInfo()
        assertEquals("0", kpi["pwl_total_prototypes"])
        assertEquals("0", kpi["pwl_total_segments"])
        assertEquals("0", kpi["pwl_total_selector_vars"])
        assertEquals("0", kpi["pwl_total_helper_vars"])
        assertEquals("0", kpi["pwl_total_constraints"])
        assertEquals("0", kpi["pwl_max_segments"])
        assertEquals("0.0", kpi["pwl_avg_segments"])
    }

    @Test
    fun testModelScaleInfoWithMultiplePWLVariables() {
        val prototypes = listOf(
            pwlPrototype("pwl1", rMin = 1.0, rMax = 5.0),
            pwlPrototype("pwl2", rMin = 2.0, rMax = 8.0),
            pwlPrototype("pwl3", rMin = 0.5, rMax = 2.0)
        )
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 8)
        )
        val kpi = component.modelScaleInfo()
        assertEquals("3", kpi["pwl_total_prototypes"])
        assertEquals("24", kpi["pwl_total_segments"], "Should have 24 total segments (3*8)")
        // Each variable: 8 selector vars, 1 select-one + 4*8=32 segment constraints = 33 constraints
        assertEquals("99", kpi["pwl_total_constraints"], "Should have 99 total constraints (3 * 33)")
        assertEquals("8", kpi["pwl_max_segments"])
    }

    // ===== 9. Core symbol lifecycle 测试 / Core symbol lifecycle tests =====

    /**
     * 辅助方法：创建 LinearMetaModel 并注册组件。
     * Helper: create LinearMetaModel and register the component.
     */
    private fun createModelAndRegister(
        component: ContinuousRadiusModelComponent
    ): LinearMetaModel<InfraNumber> {
        val model = LinearMetaModel(
            name = "test_pwl_lifecycle",
            converter = IntoValue.fromConverter(FltX)
        )
        val errors = mutableListOf<String>()
        val ensureTry: (fuookami.ospf.kotlin.utils.functional.Try, String) -> Unit = { result, msg ->
            if (result !is fuookami.ospf.kotlin.utils.functional.Ok) {
                errors.add(msg)
            }
        }
        component.register(model, ensureTry)
        assertTrue(errors.isEmpty(), "Registration should succeed without errors: $errors")
        return model
    }

    @Test
    fun shouldRegisterPWLFunctionAsIntermediateSymbol() {
        val prototypes = listOf(pwlPrototype())
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val model = createModelAndRegister(component)

        // The PWL function symbol should be in the model's token table symbols
        val pwlVar = component.pwlVariables[0]
        val pwlFunctionName = pwlVar.pwlFunction.name
        val symbolNames = model.tokens.symbols.map { it.name }
        assertTrue(
            symbolNames.contains(pwlFunctionName),
            "PWL function symbol '$pwlFunctionName' should be registered in tokens.symbols. Actual symbols: $symbolNames"
        )

        // The PWL function should be an IntermediateSymbol
        val pwlSymbol = model.tokens.symbols.find { it.name == pwlFunctionName }
        assertNotNull(pwlSymbol, "PWL symbol should be found in tokens.symbols")
        assertTrue(
            pwlSymbol is IntermediateSymbol<*>,
            "PWL symbol should be an IntermediateSymbol"
        )
    }

    @Test
    fun shouldRegisterRadiusVariableAndBounds() {
        val prototypes = listOf(pwlPrototype())
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val model = createModelAndRegister(component)

        // Radius variable should be in the model
        val pwlVar = component.pwlVariables[0]
        val rToken = model.tokens.find(pwlVar.radiusVariable)
        assertNotNull(rToken, "Radius variable should be registered in tokens")

        // Radius bound constraints should exist
        val constraintNames = model.constraints.mapNotNull {
            (it as? fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint<*>)?.name
        }
        assertTrue(
            constraintNames.contains("${pwlVar.variableName}_pwl_r_lb"),
            "Should have radius lower bound constraint"
        )
        assertTrue(
            constraintNames.contains("${pwlVar.variableName}_pwl_r_ub"),
            "Should have radius upper bound constraint"
        )
    }

    @Test
    fun shouldLetLinearMechanismModelExpandPWLFunctionConstraints() {
        val prototypes = listOf(pwlPrototype())
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val model = createModelAndRegister(component)
        val pwlVar = component.pwlVariables[0]
        val pwlFunctionName = pwlVar.pwlFunction.name

        // Build LinearMechanismModel which triggers core constraint expansion
        val mechanismModel = runBlocking {
            when (val result = LinearMechanismModel(model)) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> result.value
                else -> throw AssertionError("LinearMechanismModel construction should succeed")
            }
        }

        // Core should have expanded PWL function constraints
        val mechanismConstraintNames = mechanismModel.constraints.map { it.name }

        // Should contain select_one constraint
        assertTrue(
            mechanismConstraintNames.contains("${pwlFunctionName}_select_one"),
            "Mechanism model should contain PWL select_one constraint. Actual: $mechanismConstraintNames"
        )

        // Should contain segment constraints for each segment
        for (i in 0 until pwlVar.pwlApproximation.numSegments) {
            assertTrue(
                mechanismConstraintNames.contains("${pwlFunctionName}_seg_${i}_lb"),
                "Mechanism model should contain segment $i lower bound constraint"
            )
            assertTrue(
                mechanismConstraintNames.contains("${pwlFunctionName}_seg_${i}_ub"),
                "Mechanism model should contain segment $i upper bound constraint"
            )
            assertTrue(
                mechanismConstraintNames.contains("${pwlFunctionName}_seg_${i}_eq_ub"),
                "Mechanism model should contain segment $i equality upper constraint"
            )
            assertTrue(
                mechanismConstraintNames.contains("${pwlFunctionName}_seg_${i}_eq_lb"),
                "Mechanism model should contain segment $i equality lower constraint"
            )
        }

        // Helper variables should be in mechanism tokens
        val mechanismTokenVars = mechanismModel.tokens.tokens.map { it.variable }
        for (helperVar in pwlVar.pwlFunction.helperVariables) {
            assertTrue(
                mechanismTokenVars.contains(helperVar),
                "Helper variable ${helperVar.name} should be in mechanism tokens"
            )
        }
    }

    @Test
    fun shouldHandleMultiplePWLVariablesWithCoreLifecycle() {
        val prototypes = listOf(
            pwlPrototype("pwl1", rMin = 1.0, rMax = 3.0),
            pwlPrototype("pwl2", rMin = 2.0, rMax = 8.0)
        )
        val component = ContinuousRadiusModelComponent(
            prototypes = prototypes,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val model = createModelAndRegister(component)

        // Both PWL function symbols should be registered
        for (pwlVar in component.pwlVariables) {
            val symbolNames = model.tokens.symbols.map { it.name }
            assertTrue(
                symbolNames.contains(pwlVar.pwlFunction.name),
                "PWL function symbol '${pwlVar.pwlFunction.name}' should be registered"
            )
        }

        // Build mechanism model and verify constraints for both
        val mechanismModel = runBlocking {
            when (val result = LinearMechanismModel(model)) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> result.value
                else -> throw AssertionError("LinearMechanismModel construction should succeed")
            }
        }
        val mechanismConstraintNames = mechanismModel.constraints.map { it.name }

        for (pwlVar in component.pwlVariables) {
            assertTrue(
                mechanismConstraintNames.contains("${pwlVar.pwlFunction.name}_select_one"),
                "Mechanism model should contain select_one for ${pwlVar.variableName}"
            )
        }
    }
}
