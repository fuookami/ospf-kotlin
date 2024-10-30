package fuookami.ospf.kotlin.utils.error

import fuookami.ospf.kotlin.utils.math.*

enum class ErrorCode(private val code: UInt8) {
    None(UInt8(0x00U)),
    AuthenticationError(UInt8(0x01U)),

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

    TokenExisted(UInt8(0x20U)),
    SymbolRepetitive(UInt8(0x21U)),
    LackOfPipelines(UInt8(0x22U)),
    SolverNotFound(UInt8(0x23U)),
    OREngineEnvironmentLost(UInt8(0x24U)),
    OREngineConnectionOvertime(UInt8(0x25U)),
    OREngineModelingException(UInt8(0x26U)),
    OREngineSolvingException(UInt8(0x27U)),
    OREngineTerminated(UInt8(0x28U)),
    ORModelNoSolution(UInt8(0x29U)),
    ORModelUnbounded(UInt8(0x2aU)),
    ORSolutionInvalid(UInt8(0x2bU)),

    ApplicationFailed(UInt8(0x30U)),
    ApplicationError(UInt8(0x31U)),
    ApplicationException(UInt8(0x32U)),
    ApplicationStopped(UInt8(0x33U)),
    IllegalArgument(UInt8(0x34U)),

    Other(UInt8.maximum - UInt8.one),
    Unknown(UInt8.maximum);

    companion object {
        fun from(code: UInt8) = ErrorCode.entries.first { it.code == code }
        fun from(code: UInt64) = ErrorCode.entries.first { it.code.toUInt64() == code }
    }

    fun toUInt8() = this.code
    fun toUInt64() = this.code.toUInt64()

    override fun toString(): String {
        return this.name
    }
}

// todo: find localize way to set default message
