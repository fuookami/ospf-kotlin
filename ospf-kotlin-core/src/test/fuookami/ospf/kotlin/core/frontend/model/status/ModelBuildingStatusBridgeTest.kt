package fuookami.ospf.kotlin.core.frontend.model.status

import fuookami.ospf.kotlin.core.backend.intermediate_model.IntermediateModelDumpingStatus
import fuookami.ospf.kotlin.core.backend.intermediate_model.toModelBuildingStatus
import fuookami.ospf.kotlin.core.frontend.model.mechanism.MechanismModelDumpingStatus
import fuookami.ospf.kotlin.core.frontend.model.mechanism.RegistrationStatus
import fuookami.ospf.kotlin.core.frontend.model.mechanism.toModelBuildingStatus
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ModelBuildingStatusBridgeTest {
    @Test
    fun registrationStatusShouldMapToRegisterTokens() {
        val status = RegistrationStatus(
            emptySymbolAmount = UInt64.one,
            readySymbolAmount = UInt64(5),
            totalSymbolAmount = UInt64(10)
        )

        val unified = status.toModelBuildingStatus("demo-linear")

        assertEquals(ModelBuildingStage.RegisterTokens, unified.stage)
        assertEquals(UInt64(5), unified.ready)
        assertEquals(UInt64(10), unified.total)
    }

    @Test
    fun mechanismDumpingStatusShouldMapToConstraintStageBeforeConstraintCompleted() {
        val status = MechanismModelDumpingStatus(
            readyConstraintAmount = UInt64(3),
            totalConstraintAmount = UInt64(10),
            readySymbolAmount = UInt64.zero,
            totalSymbolAmount = UInt64(8)
        )

        val linearUnified = status.toModelBuildingStatus("demo-linear", quadratic = false)
        val quadraticUnified = status.toModelBuildingStatus("demo-quadratic", quadratic = true)

        assertEquals(ModelBuildingStage.RegisterLinearConstraints, linearUnified.stage)
        assertEquals(ModelBuildingStage.RegisterQuadraticConstraints, quadraticUnified.stage)
    }

    @Test
    fun mechanismDumpingStatusShouldMapToRegisterSymbolsAfterConstraintCompleted() {
        val status = MechanismModelDumpingStatus(
            readyConstraintAmount = UInt64(10),
            totalConstraintAmount = UInt64(10),
            readySymbolAmount = UInt64(4),
            totalSymbolAmount = UInt64(8)
        )

        val unified = status.toModelBuildingStatus("demo-linear", quadratic = false)

        assertEquals(ModelBuildingStage.RegisterSymbols, unified.stage)
        assertEquals(UInt64(4), unified.ready)
        assertEquals(UInt64(8), unified.total)
    }

    @Test
    fun intermediateDumpingStatusShouldMapToFlattenStage() {
        val status = IntermediateModelDumpingStatus(
            readyConstraintAmount = UInt64(6),
            totalConstraintAmount = UInt64(12)
        )

        val linearUnified = status.toModelBuildingStatus("demo-linear", quadratic = false)
        val quadraticUnified = status.toModelBuildingStatus("demo-quadratic", quadratic = true)

        assertEquals(ModelBuildingStage.FlattenLinearModel, linearUnified.stage)
        assertEquals(ModelBuildingStage.FlattenQuadraticModel, quadraticUnified.stage)
        assertEquals(UInt64(6), linearUnified.ready)
        assertEquals(UInt64(12), linearUnified.total)
    }
}

