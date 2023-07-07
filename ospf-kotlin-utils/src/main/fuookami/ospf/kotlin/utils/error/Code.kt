package fuookami.ospf.kotlin.utils.error

import fuookami.ospf.kotlin.utils.math.*

enum class ErrorCode(private val code: UInt8) {
    None(UInt8(0x00U)),

    NotAFile(UInt8(0x10U)),
    NotADirectory(UInt8(0x11U)),
    FileNotFound(UInt8(0x12U)),
    DirectoryUnusable(UInt8(0x13U)),
    FileExtensionNotMatched(UInt8(0x14U)),
    DataNotFound(UInt8(0x15U)),
    DataEmpty(UInt8(0x16U)),
    EnumVisitorEmpty(UInt8(0x17U)),
    UniqueBoxLocked(UInt8(0x18U)),
    UniqueRefLocked(UInt8(0x19U)),
    SerializationFailed(UInt8(0x1aU)),
    DeserializationFailed(UInt8(0x1bU)),

    LackOfPipelines(UInt8(0x20U)),
    SolverNotFound(UInt8(0x21U)),
    OREngineEnvironmentLost(UInt8(0x22U)),
    OREngineConnectionOvertime(UInt8(0x23U)),
    OREngineModelingException(UInt8(0x24U)),
    OREngineSolvingException(UInt8(0x25U)),
    OREngineTerminated(UInt8(0x26U)),
    ORModelNoSolution(UInt8(0x27U)),
    ORModelUnbounded(UInt8(0x28U)),
    ORSolutionInvalid(UInt8(0x29U)),

    ApplicationFailed(UInt8(0x30U)),
    ApplicationError(UInt8(0x31U)),
    ApplicationException(UInt8(0x32U)),
    ApplicationStopped(UInt8(0x33U)),

    Other(UInt8.maximum - UInt8.one),
    Unknown(UInt8.maximum);

    companion object {
        fun from(code: UInt8) = ErrorCode.values().first { it.code == code }
    }

    fun toUInt8() = this.code

    override fun toString(): String {
        return this.name
    }
}

// todo: find localize way to set default message
