/**
 * Remote checkpoint domain model aliases. / 远端 checkpoint 领域模型别名。
 *
 * 本文件曾经是 core envelope 与子模型的一份**平行定义**（字段列表逐字重复），语义靠人工保持一致，
 * 改一处漏一处。现在它只是 core 类型的别名：字段列表、摘要与父链校验都只有 core 一处实现。
 *
 * This file used to hold a **parallel definition** of the core envelope and its sub-models (their
 * field lists duplicated verbatim), kept in sync by hand and therefore prone to one-sided edits. It
 * now holds only aliases of the core types: the field list, the digest, and parent-link validation
 * each have exactly one implementation, in core.
 *
 * 别名右侧使用全限定名是刻意的：别名与 core 类型同名，若再 import 同名简单名会构成冲突声明。
 * The right-hand sides are deliberately fully qualified: each alias shares its simple name with the
 * core type, so importing that simple name as well would be a conflicting declaration.
 */
package fuookami.ospf.kotlin.framework.solver.remote.domain

import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointEnvelope

/**
 * Portable checkpoint envelope shared with the dispatcher. / 与 dispatcher 共享的可移植 checkpoint envelope。
 *
 * @see ConstraintProgrammingCheckpointEnvelope
 */
typealias PortableCheckpointEnvelope = ConstraintProgrammingCheckpointEnvelope

/**
 * Portable incumbent representation. / 可移植 incumbent 表示。
 *
 * @see fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingIncumbent
 */
typealias PortableConstraintProgrammingIncumbent =
    fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingIncumbent

/**
 * Portable interval value. / 可移植 interval 值。
 *
 * @see fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingIntervalValue
 */
typealias PortableConstraintProgrammingIntervalValue =
    fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingIntervalValue

/**
 * Portable conflict evidence. / 可移植冲突证据。
 *
 * @see fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingConflict
 */
typealias PortableConstraintProgrammingConflict =
    fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingConflict

/**
 * Portable Benders state. / 可移植 Benders 状态。
 *
 * @see fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingBendersState
 */
typealias PortableConstraintProgrammingBendersState =
    fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingBendersState

/**
 * Versioned cut payload. / 版本化 cut payload。
 *
 * @see fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingCut
 */
typealias PortableConstraintProgrammingCut =
    fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingCut

/**
 * Portable solver provenance. / 可移植求解器来源。
 *
 * @see fuookami.ospf.kotlin.core.solver.constraint_programming.PortableSolverProvenance
 */
typealias PortableSolverProvenance =
    fuookami.ospf.kotlin.core.solver.constraint_programming.PortableSolverProvenance

/**
 * Wire shape of one cancellation fact. / 一条取消事实的线格式。
 *
 * envelope 的 `cancellationChain` 承载该类型，因此框架侧同样需要一个可用名。
 * The envelope's `cancellationChain` carries this type, so the framework side needs a usable name
 * for it as well.
 *
 * @see fuookami.ospf.kotlin.core.solver.constraint_programming.PortableCancellationRecord
 */
typealias PortableCancellationRecord =
    fuookami.ospf.kotlin.core.solver.constraint_programming.PortableCancellationRecord
