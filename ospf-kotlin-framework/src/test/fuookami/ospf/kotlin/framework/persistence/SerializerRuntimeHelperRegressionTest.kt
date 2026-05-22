package fuookami.ospf.kotlin.framework.persistence

import fuookami.ospf.kotlin.framework.log.LogRecordPO
import fuookami.ospf.kotlin.framework.log.LogRecordType
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@Serializable
private data class LogPayload(
    val message: String,
    val level: Int
)

@Serializable
private data class DemoRequestDTO(
    override val id: String,
    val payload: String
) : RequestDTO<DemoRequestDTO>

@Serializable
private data class DemoResponseDTO(
    override val id: String,
    override val code: UInt64,
    override val msg: String,
    val payload: String
) : ResponseDTO<DemoResponseDTO>

class SerializerRuntimeHelperRegressionTest {
    @Test
    fun logRecordRpoRoundTripKeepsPayload() {
        val record = LogRecordPO(
            app = "framework",
            version = "test",
            serviceId = "svc-log",
            step = "serialize",
            type = LogRecordType.Info,
            time = LocalDateTime(2026, 5, 22, 10, 0, 0),
            availableTime = LocalDateTime(2026, 5, 22, 11, 0, 0),
            value = LogPayload("hello", 3)
        )

        val rpo = record.rpo
        val decoded = LogRecordPO<LogPayload>(rpo)

        assertNotNull(decoded)
        assertEquals(record.value, decoded!!.value)
        assertEquals(record.type, decoded.type)
        assertEquals(record.app, decoded.app)
    }

    @Test
    fun requestRecordRpoRoundTripKeepsPayload() {
        val request = DemoRequestDTO(id = "req-1", payload = "request-body")
        val record = RequestRecordPO(
            app = "framework",
            requester = "tester",
            version = "v1",
            request = request
        )

        val rpo = record.rpo
        val decoded = RequestRecordPO<DemoRequestDTO>(rpo)

        assertNotNull(decoded)
        assertEquals(record.id, decoded!!.id)
        assertEquals(request, decoded.request)
        assertEquals(record.requester, decoded.requester)
    }

    @Test
    fun responseRecordRpoRoundTripKeepsPayload() {
        val response = DemoResponseDTO(
            id = "resp-1",
            code = UInt64.one,
            msg = "ok",
            payload = "response-body"
        )
        val record = ResponseRecordPO(
            app = "framework",
            requester = "tester",
            version = "v1",
            response = response
        )

        val rpo = record.rpo
        val decoded = ResponseRecordPO<DemoResponseDTO>(rpo)

        assertNotNull(decoded)
        assertEquals(record.id, decoded!!.id)
        assertEquals(record.code, decoded.code)
        assertEquals(response, decoded.response)
    }
}
