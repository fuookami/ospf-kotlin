package fuookami.ospf.kotlin.core.intermediate_model

// Legacy test-name compatibility aliases after package refactor.

typealias ConstraintRelation = fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
typealias ConstraintSource = fuookami.ospf.kotlin.core.model.basic.ConstraintSource
typealias ObjectCategory = fuookami.ospf.kotlin.core.model.basic.ObjectCategory
typealias Variable = fuookami.ospf.kotlin.core.model.basic.Variable
typealias ExpressionRange<V> = fuookami.ospf.kotlin.core.model.basic.ExpressionRange<V>
typealias InvalidConstraintSignFromComparison = fuookami.ospf.kotlin.core.model.basic.InvalidConstraintSignFromComparison

typealias LinearConstraintBatch = fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
typealias QuadraticConstraintBatch = fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch
typealias SparseVector<V> = fuookami.ospf.kotlin.core.model.intermediate.SparseVector<V>
typealias SparseMatrix<V> = fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix<V>
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
typealias QuadraticCell<V> = fuookami.ospf.kotlin.core.model.intermediate.QuadraticCell<V>
typealias LinearCellImpl<V> = fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl<V>
typealias QuadraticCellImpl<V> = fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl<V>

typealias LinearMetaModel<V> = fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel<V>
typealias QuadraticMetaModel<V> = fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel<V>
typealias LinearMechanismModel<V> = fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel<V>
typealias QuadraticMechanismModel<V> = fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel<V>
typealias Constraint<V, P> = fuookami.ospf.kotlin.core.model.mechanism.Constraint<V, P>
typealias LinearConstraintImpl<V> = fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl<V>
typealias QuadraticConstraintImpl<V> = fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraintImpl<V>
typealias MechanismLinear = fuookami.ospf.kotlin.core.model.mechanism.Linear
typealias MechanismQuadratic = fuookami.ospf.kotlin.core.model.mechanism.Quadratic
typealias LinearSubObject<V> = fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject<V>
typealias QuadraticSubObject<V> = fuookami.ospf.kotlin.core.model.mechanism.QuadraticSubObject<V>
typealias SingleObject<Obj> = fuookami.ospf.kotlin.core.model.mechanism.SingleObject<Obj>

typealias AutoTokenTable<V> = fuookami.ospf.kotlin.core.token.AutoTokenTable<V>
typealias ManualTokenTable<V> = fuookami.ospf.kotlin.core.token.ManualTokenTable<V>
typealias ConcurrentAutoTokenTable<V> = fuookami.ospf.kotlin.core.token.ConcurrentAutoTokenTable<V>
typealias ConcurrentManualAddTokenTable<V> = fuookami.ospf.kotlin.core.token.ConcurrentManualAddTokenTable<V>
typealias MutableTokenTable<V> = fuookami.ospf.kotlin.core.token.MutableTokenTable<V>
typealias ConcurrentMutableTokenTable<V> = fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable<V>

typealias TokenCacheContexts<V> = fuookami.ospf.kotlin.core.token.TokenCacheContexts<V>
typealias LinearFlattenContext<V> = fuookami.ospf.kotlin.core.token.LinearFlattenContext<V>
typealias QuadraticFlattenContext<V> = fuookami.ospf.kotlin.core.token.QuadraticFlattenContext<V>
typealias ValueCacheContext<V> = fuookami.ospf.kotlin.core.token.ValueCacheContext<V>
typealias RangeCacheContext<V> = fuookami.ospf.kotlin.core.token.RangeCacheContext<V>
typealias LinearFlattenData<V> = fuookami.ospf.kotlin.core.token.LinearFlattenData<V>
typealias QuadraticFlattenData<V> = fuookami.ospf.kotlin.core.token.QuadraticFlattenData<V>