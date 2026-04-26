@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure

@JvmInline
value class IATA(val code: String) {
    init {
        assert(code.length == 3)
    }

    override fun toString() = code
}

@JvmInline
value class ICAO(val code: String) {
    init {
        assert(code.length == 4)
    }

    override fun toString() = code
}

@JvmInline
value class AircraftTypeName(val name: String) {
    override fun toString() = name
}

@JvmInline
value class AircraftTypeCode(val code: String) {
    override fun toString() = code
}

@JvmInline
value class AircraftMinorTypeName(val name: String) {
    override fun toString() = name
}

@JvmInline
value class AircraftMinorTypeCode(val code: String) {
    override fun toString() = code
}

@JvmInline
value class WingAircraftTypeCode(val code: String) {
    override fun toString() = code
}

@JvmInline
value class AircraftRegisterNumber(val no: String) {
    override fun toString() = no
}

@JvmInline
value class PassengerClass(val cls: String) {
    override fun toString() = cls
}

@JvmInline
value class PilotRankNo(val no: String) {
    override fun toString() = no
}

@JvmInline
value class PilotCode(val code: String) {
    override fun toString() = code
}

@JvmInline
value class CrewManRankNo(val no: String) {
    override fun toString() = no
}

@JvmInline
value class WorkerNo(val no: String) {
    override fun toString() = no
}
