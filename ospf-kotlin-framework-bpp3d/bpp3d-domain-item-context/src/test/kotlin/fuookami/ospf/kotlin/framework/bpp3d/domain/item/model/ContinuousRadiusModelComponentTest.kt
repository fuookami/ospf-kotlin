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
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ConservativeRadiusEnvelope
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLBreakpointStrategy
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusApproximationConfig
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusSquaredApproximation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter

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
}
