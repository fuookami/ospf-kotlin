package fuookami.ospf.kotlin.core.intermediate_model

// Legacy test-name compatibility aliases after package refactor.

typealias ConstraintRelation = fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
typealias ConstraintSource = fuookami.ospf.kotlin.core.model.basic.ConstraintSource
typealias ObjectCategory = fuookami.ospf.kotlin.core.model.basic.ObjectCategory
typealias Variable = fuookami.ospf.kotlin.core.model.basic.Variable
typealias ExpressionRange<V> = fuookami.ospf.kotlin.core.model.basic.ExpressionRange<V>

typealias LinearConstraintBatch = fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
typealias QuadraticConstraintBatch = fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch
typealias SparseVectorF64 = fuookami.ospf.kotlin.core.model.intermediate.SparseVectorF64
typealias SparseMatrixF64 = fuookami.ospf.kotlin.core.model.intermediate.SparseMatrixF64
typealias SparseQuadraticVector = fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticVector
typealias SparseQuadraticMatrix = fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticMatrix
typealias LinearObjective = fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
typealias QuadraticObjective = fuookami.ospf.kotlin.core.model.intermediate.QuadraticObjective
typealias LinearTriadModel = fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
typealias QuadraticTetradModel = fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModel
typealias BasicLinearTriadModel = fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
typealias BasicQuadraticTetradModel = fuookami.ospf.kotlin.core.model.intermediate.BasicQuadraticTetradModel
typealias LinearTriadModelView = fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
typealias QuadraticTetradModelView = fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
typealias QuadraticCellF64 = fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellF64
typealias LinearCellImpl<V> = fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl<V>
typealias LinearCellImplF64 = fuookami.ospf.kotlin.core.model.intermediate.LinearCellImplF64
typealias QuadraticCellImpl<V> = fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl<V>
typealias QuadraticCellImplF64 = fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImplF64

typealias LinearMetaModel<V> = fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel<V>
typealias QuadraticMetaModel<V> = fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel<V>
typealias LinearMechanismModel<V> = fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel<V>
typealias QuadraticMechanismModel<V> = fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel<V>
typealias LinearConstraint = fuookami.ospf.kotlin.core.model.mechanism.LinearConstraint
typealias QuadraticConstraint = fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraint
typealias LinearConstraintImpl = fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
typealias QuadraticConstraintImpl = fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraintImpl
typealias LinearDualSolution = fuookami.ospf.kotlin.core.model.mechanism.LinearDualSolution
typealias QuadraticDualSolution = fuookami.ospf.kotlin.core.model.mechanism.QuadraticDualSolution
typealias LinearSubObject<V> = fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject<V>
typealias QuadraticSubObject<V> = fuookami.ospf.kotlin.core.model.mechanism.QuadraticSubObject<V>
typealias SingleObject<Obj> = fuookami.ospf.kotlin.core.model.mechanism.SingleObject<Obj>

typealias AutoTokenTable<V> = fuookami.ospf.kotlin.core.token.AutoTokenTable<V>
typealias ManualTokenTable<V> = fuookami.ospf.kotlin.core.token.ManualTokenTable<V>
typealias ConcurrentAutoTokenTable<V> = fuookami.ospf.kotlin.core.token.ConcurrentAutoTokenTable<V>
typealias ConcurrentManualAddTokenTable<V> = fuookami.ospf.kotlin.core.token.ConcurrentManualAddTokenTable<V>
typealias MutableTokenTable<V> = fuookami.ospf.kotlin.core.token.MutableTokenTable<V>
typealias MutableTokenTableF64 = fuookami.ospf.kotlin.core.token.MutableTokenTableF64
typealias ConcurrentMutableTokenTable<V> = fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable<V>
typealias ConcurrentMutableTokenTableF64 = fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTableF64

typealias TokenCacheContexts<V> = fuookami.ospf.kotlin.core.token.TokenCacheContexts<V>
typealias TokenCacheContextsF64 = fuookami.ospf.kotlin.core.token.TokenCacheContextsF64
typealias LinearFlattenContext<V> = fuookami.ospf.kotlin.core.token.LinearFlattenContext<V>
typealias LinearFlattenContextF64 = fuookami.ospf.kotlin.core.token.LinearFlattenContextF64
typealias QuadraticFlattenContext<V> = fuookami.ospf.kotlin.core.token.QuadraticFlattenContext<V>
typealias QuadraticFlattenContextF64 = fuookami.ospf.kotlin.core.token.QuadraticFlattenContextF64
typealias ValueCacheContext<V> = fuookami.ospf.kotlin.core.token.ValueCacheContext<V>
typealias ValueCacheContextF64 = fuookami.ospf.kotlin.core.token.ValueCacheContextF64
typealias RangeCacheContext<V> = fuookami.ospf.kotlin.core.token.RangeCacheContext<V>
typealias RangeCacheContextF64 = fuookami.ospf.kotlin.core.token.RangeCacheContextF64
typealias LinearFlattenData<V> = fuookami.ospf.kotlin.core.token.LinearFlattenData<V>
typealias LinearFlattenDataF64 = fuookami.ospf.kotlin.core.token.LinearFlattenDataF64
typealias QuadraticFlattenData<V> = fuookami.ospf.kotlin.core.token.QuadraticFlattenData<V>
typealias QuadraticFlattenDataF64 = fuookami.ospf.kotlin.core.token.QuadraticFlattenDataF64

typealias InvalidConstraintSignFromComparison = fuookami.ospf.kotlin.core.model.basic.InvalidConstraintSignFromComparison
