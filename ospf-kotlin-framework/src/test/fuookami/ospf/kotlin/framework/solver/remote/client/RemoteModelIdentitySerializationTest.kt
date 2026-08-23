package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.framework.solver.remote.domain.SerializedLinearModel
import fuookami.ospf.kotlin.framework.solver.remote.domain.SerializedModelElementOrigin

/**
 * 线性/二次模型身份 DTO 回归测试。 / Linear/quadratic model identity DTO regression tests.
 */
class RemoteModelIdentitySerializationTest {
    private val json = Json {
        ignoreUnknownKeys = false
    }

    /**
     * 验证跨仓库模型 fixture 保留稳定身份元数据。 /
     * Verify that the cross-repository model fixture preserves stable identity metadata.
     */
    @Test
    fun canonicalLinearModelFixturePreservesIdentityMetadata() {
        val fixture = checkNotNull(javaClass.getResource("/fixtures/remote-linear-model-v2.json"))
            .readText()
        val model = json.decodeFromString(SerializedLinearModel.serializer(), fixture)

        assertEquals("fixture-model", model.identityNamespace)
        assertEquals("1.0", model.identitySchemaVersion)
        assertEquals("variable:x", model.variables.single().identityId)
        assertEquals("STABLE", model.variables.single().identityScope)
        assertEquals("demand", model.variables.single().identityOriginKind)
        assertEquals(
            listOf(
                SerializedModelElementOrigin("demand", "x"),
                SerializedModelElementOrigin("source", "x")
            ),
            model.variables.single().identityProvenance
        )
        assertEquals("constraint:capacity", model.constraints.single().identityId)
        assertEquals("capacity", model.constraints.single().identityOriginKind)
        assertEquals(
            listOf(
                SerializedModelElementOrigin("capacity", "machine-1"),
                SerializedModelElementOrigin("source", "machine-1")
            ),
            model.constraints.single().identityProvenance
        )
        assertEquals("objective:cost", model.objective.identityId)
        assertEquals("total", model.objective.identityOriginKey)
        assertEquals(
            listOf(
                SerializedModelElementOrigin("cost", "total"),
                SerializedModelElementOrigin("source", "total")
            ),
            model.objective.identityProvenance
        )

        val roundTripped = json.decodeFromString(
            SerializedLinearModel.serializer(),
            json.encodeToString(SerializedLinearModel.serializer(), model)
        )
        assertEquals(model, roundTripped)
    }

    @Test
    fun provenanceListSurvivesRemoteJsonRoundTrip() {
        val fixture = checkNotNull(javaClass.getResource("/fixtures/remote-linear-model-v2.json"))
            .readText()
        val model = json.decodeFromString(SerializedLinearModel.serializer(), fixture)
        val withProvenance = model.copy(
            variables = model.variables.map { variable ->
                variable.copy(
                    identityProvenance = listOf(
                        SerializedModelElementOrigin("source", "x"),
                        SerializedModelElementOrigin("variable", "x")
                    )
                )
            },
            constraints = model.constraints.map { constraint ->
                constraint.copy(
                    identityProvenance = listOf(
                        SerializedModelElementOrigin("capacity", "machine-1"),
                        SerializedModelElementOrigin("source", "machine-1")
                    )
                )
            },
            objective = model.objective.copy(
                identityProvenance = listOf(
                    SerializedModelElementOrigin("objective", "total"),
                    SerializedModelElementOrigin("source", "total")
                )
            )
        )

        val roundTripped = json.decodeFromString(
            SerializedLinearModel.serializer(),
            json.encodeToString(SerializedLinearModel.serializer(), withProvenance)
        )

        assertEquals(withProvenance, roundTripped)
    }
}
