#!/usr/bin/env python3
"""Replace internal typealias references in downstream modules with expanded generic types."""

import re
import os

# Type reference replacements (applied to file body, NOT to imports)
# Order: longer names first to avoid substring matching
TYPE_REPLACEMENTS = [
    # Math layer
    ("Flt64LinearInequality", "LinearInequality<Flt64>"),
    # Quantities layer
    ("QuantityCanonicalFlt64", "Quantity<CanonicalPolynomial<Flt64>>"),
    ("QuantityQuadraticFlt64", "Quantity<QuadraticPolynomial<Flt64>>"),
    ("QuantityLinearFlt64", "Quantity<LinearPolynomial<Flt64>>"),
    ("QuantityFlt64", "Quantity<Flt64>"),
    # Symbol combinations (Flt64 suffix) - individual symbols
    ("QuantityQuadraticExpressionSymbolFlt64", "Quantity<QuadraticExpressionSymbol<Flt64>>"),
    ("QuantityLinearExpressionSymbolFlt64", "Quantity<LinearExpressionSymbol<Flt64>>"),
    ("QuantityQuadraticIntermediateSymbolFlt64", "Quantity<QuadraticIntermediateSymbol<Flt64>>"),
    ("QuantityLinearIntermediateSymbolFlt64", "Quantity<LinearIntermediateSymbol<Flt64>>"),
    ("QuantityIntermediateSymbolFlt64", "Quantity<IntermediateSymbol<Flt64>>"),
    # Symbol combinations - array types (Dyn)
    ("DynQuantityQuadraticExpressionSymbolsFlt64", "DynQuantityQuadraticExpressionSymbols<Flt64>"),
    ("DynQuantityLinearExpressionSymbolsFlt64", "DynQuantityLinearExpressionSymbols<Flt64>"),
    ("DynQuantityLinearIntermediateSymbolsFlt64", "DynQuantityLinearIntermediateSymbols<Flt64>"),
    ("DynQuantityQuadraticIntermediateSymbolsFlt64", "DynQuantityQuadraticIntermediateSymbols<Flt64>"),
    ("DynQuadraticExpressionSymbolsFlt64", "QuadraticExpressionSymbols<Flt64>"),
    ("DynLinearExpressionSymbolsFlt64", "LinearExpressionSymbols<Flt64>"),
    ("DynLinearIntermediateSymbolsFlt64", "LinearIntermediateSymbols<Flt64>"),
    ("DynQuadraticIntermediateSymbolsFlt64", "QuadraticIntermediateSymbols<Flt64>"),
    # Symbol combinations - array types (1-4)
    ("QuantityQuadraticExpressionSymbols4Flt64", "QuantityQuadraticExpressionSymbols4<Flt64>"),
    ("QuantityQuadraticExpressionSymbols3Flt64", "QuantityQuadraticExpressionSymbols3<Flt64>"),
    ("QuantityQuadraticExpressionSymbols2Flt64", "QuantityQuadraticExpressionSymbols2<Flt64>"),
    ("QuantityQuadraticExpressionSymbols1Flt64", "QuantityQuadraticExpressionSymbols1<Flt64>"),
    ("QuantityLinearExpressionSymbols4Flt64", "QuantityLinearExpressionSymbols4<Flt64>"),
    ("QuantityLinearExpressionSymbols3Flt64", "QuantityLinearExpressionSymbols3<Flt64>"),
    ("QuantityLinearExpressionSymbols2Flt64", "QuantityLinearExpressionSymbols2<Flt64>"),
    ("QuantityLinearExpressionSymbols1Flt64", "QuantityLinearExpressionSymbols1<Flt64>"),
    ("QuantityQuadraticIntermediateSymbols4Flt64", "QuantityQuadraticIntermediateSymbols4<Flt64>"),
    ("QuantityQuadraticIntermediateSymbols3Flt64", "QuantityQuadraticIntermediateSymbols3<Flt64>"),
    ("QuantityQuadraticIntermediateSymbols2Flt64", "QuantityQuadraticIntermediateSymbols2<Flt64>"),
    ("QuantityQuadraticIntermediateSymbols1Flt64", "QuantityQuadraticIntermediateSymbols1<Flt64>"),
    ("QuantityLinearIntermediateSymbols4Flt64", "QuantityLinearIntermediateSymbols4<Flt64>"),
    ("QuantityLinearIntermediateSymbols3Flt64", "QuantityLinearIntermediateSymbols3<Flt64>"),
    ("QuantityLinearIntermediateSymbols2Flt64", "QuantityLinearIntermediateSymbols2<Flt64>"),
    ("QuantityLinearIntermediateSymbols1Flt64", "QuantityLinearIntermediateSymbols1<Flt64>"),
    ("QuadraticExpressionSymbols4Flt64", "QuadraticExpressionSymbols4<Flt64>"),
    ("QuadraticExpressionSymbols3Flt64", "QuadraticExpressionSymbols3<Flt64>"),
    ("QuadraticExpressionSymbols2Flt64", "QuadraticExpressionSymbols2<Flt64>"),
    ("QuadraticExpressionSymbols1Flt64", "QuadraticExpressionSymbols1<Flt64>"),
    ("LinearExpressionSymbols4Flt64", "LinearExpressionSymbols4<Flt64>"),
    ("LinearExpressionSymbols3Flt64", "LinearExpressionSymbols3<Flt64>"),
    ("LinearExpressionSymbols2Flt64", "LinearExpressionSymbols2<Flt64>"),
    ("LinearExpressionSymbols1Flt64", "LinearExpressionSymbols1<Flt64>"),
    ("QuadraticIntermediateSymbols4Flt64", "QuadraticIntermediateSymbols4<Flt64>"),
    ("QuadraticIntermediateSymbols3Flt64", "QuadraticIntermediateSymbols3<Flt64>"),
    ("QuadraticIntermediateSymbols2Flt64", "QuadraticIntermediateSymbols2<Flt64>"),
    ("QuadraticIntermediateSymbols1Flt64", "QuadraticIntermediateSymbols1<Flt64>"),
    ("LinearIntermediateSymbols4Flt64", "LinearIntermediateSymbols4<Flt64>"),
    ("LinearIntermediateSymbols3Flt64", "LinearIntermediateSymbols3<Flt64>"),
    ("LinearIntermediateSymbols2Flt64", "LinearIntermediateSymbols2<Flt64>"),
    ("LinearIntermediateSymbols1Flt64", "LinearIntermediateSymbols1<Flt64>"),
    # Models
    ("AbstractQuadraticMechanismModelFlt64", "AbstractQuadraticMechanismModel<Flt64>"),
    ("AbstractLinearMechanismModelFlt64", "AbstractLinearMechanismModel<Flt64>"),
    ("SingleObjectMechanismModelFlt64", "SingleObjectMechanismModel<Flt64>"),
    ("QuadraticMechanismModelFlt64", "QuadraticMechanismModel<Flt64>"),
    ("LinearMechanismModelFlt64", "LinearMechanismModel<Flt64>"),
    ("BasicMechanismModelFlt64", "BasicMechanismModel<Flt64>"),
    ("AbstractQuadraticMetaModelFlt64", "AbstractQuadraticMetaModel<Flt64>"),
    ("AbstractLinearMetaModelFlt64", "AbstractLinearMetaModel<Flt64>"),
    ("QuadraticMetaModelFlt64", "QuadraticMetaModel<Flt64>"),
    ("LinearMetaModelFlt64", "LinearMetaModel<Flt64>"),
    ("AbstractMetaModelFlt64", "AbstractMetaModel<Flt64>"),
    ("MetaModelFlt64", "MetaModel<Flt64>"),
    ("MechanismModelFlt64", "MechanismModel<Flt64>"),
    # Solver output
    ("FeasibleSolverOutputFlt64", "FeasibleSolverOutput<Flt64>"),
    # Constraints and dual solutions
    ("QuadraticConstraintImplFlt64", "QuadraticConstraintImpl<Flt64>"),
    ("LinearConstraintImplFlt64", "LinearConstraintImpl<Flt64>"),
    ("SymbolicQuadraticInequalityFlt64", "SymbolicQuadraticInequality<Flt64>"),
    ("SymbolicLinearInequalityFlt64", "SymbolicLinearInequality<Flt64>"),
    ("QuadraticSubObjectFlt64", "QuadraticSubObject<Flt64>"),
    ("LinearSubObjectFlt64", "LinearSubObject<Flt64>"),
    # Dual solutions - expand fully since both DualSolution and LinearConstraint are internal
    # Use kotlin.collections.Map to avoid shadowing by function type params
    ("DualSolution<Linear>", "kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>"),
    ("DualSolution<Quadratic>", "kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>"),
    ("LinearDualSolution", "kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>"),
    ("QuadraticDualSolution", "kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>"),
    # ConstraintFlt64<P> -> Constraint<Flt64, P>
    ("ConstraintFlt64<Linear>", "Constraint<Flt64, Linear>"),
    ("ConstraintFlt64<Quadratic>", "Constraint<Flt64, Quadratic>"),
    # LinearConstraint/QuadraticConstraint -> Constraint<Flt64, Linear/Quadratic>
    ("LinearConstraint", "Constraint<Flt64, Linear>"),
    ("QuadraticConstraint", "Constraint<Flt64, Quadratic>"),
    # Other Flt64 types
    ("MultiObjectCallBackModelFlt64", "MultiObjectCallBackModel<Flt64>"),
    ("CallBackModelFlt64", "CallBackModel<Flt64>"),
    ("ProductFunctionFlt64", "ProductFunction<Flt64>"),
    ("SolutionFlt64", "Solution<Flt64>"),
    # Intermediate symbols
    ("QuadraticExpressionSymbolFlt64", "QuadraticExpressionSymbol<Flt64>"),
    ("LinearExpressionSymbolFlt64", "LinearExpressionSymbol<Flt64>"),
    ("QuadraticIntermediateSymbolFlt64", "QuadraticIntermediateSymbol<Flt64>"),
    ("LinearIntermediateSymbolFlt64", "LinearIntermediateSymbol<Flt64>"),
    ("IntermediateSymbolFlt64", "IntermediateSymbol<Flt64>"),
    # Cells
    ("QuadraticCellImplFlt64", "QuadraticCellImpl<Flt64>"),
    ("LinearCellImplFlt64", "LinearCellImpl<Flt64>"),
    ("QuadraticCellFlt64", "QuadraticCell<Flt64>"),
    ("LinearCellFlt64", "LinearCell<Flt64>"),
    ("CellFlt64", "Cell<Flt64>"),
    # Sparse
    ("SparseMatrixFlt64", "SparseMatrix<Flt64>"),
    ("SparseVectorFlt64", "SparseVector<Flt64>"),
    # Flatten
    ("QuadraticFlattenContextFlt64", "QuadraticFlattenContext<Flt64>"),
    ("LinearFlattenContextFlt64", "LinearFlattenContext<Flt64>"),
    ("QuadraticFlattenDataFlt64", "QuadraticFlattenData<Flt64>"),
    ("LinearFlattenDataFlt64", "LinearFlattenData<Flt64>"),
    # Token
    ("ConcurrentMutableTokenTableFlt64", "ConcurrentMutableTokenTable<Flt64>"),
    ("AbstractMutableTokenTableFlt64", "AbstractMutableTokenTable<Flt64>"),
    ("ConcurrentTokenTableFlt64", "ConcurrentTokenTable<Flt64>"),
    ("MutableTokenTableFlt64", "MutableTokenTable<Flt64>"),
    ("AbstractTokenTableFlt64", "AbstractTokenTable<Flt64>"),
    ("TokenTableFlt64", "TokenTable<Flt64>"),
    ("AbstractMutableTokenListFlt64", "AbstractMutableTokenList<Flt64>"),
    ("AddableTokenCollectionFlt64", "AddableTokenCollection<Flt64>"),
    ("AutoTokenListFlt64", "AutoTokenList<Flt64>"),
    ("ManualTokenListFlt64", "ManualTokenList<Flt64>"),
    ("MutableTokenListFlt64", "MutableTokenList<Flt64>"),
    ("AbstractTokenListFlt64", "AbstractTokenList<Flt64>"),
    ("TokenListFlt64", "TokenList<Flt64>"),
    ("TokenCacheContextsFlt64", "TokenCacheContexts<Flt64>"),
    ("RangeCacheContextFlt64", "RangeCacheContext<Flt64>"),
    ("ValueCacheContextFlt64", "ValueCacheContext<Flt64>"),
    ("AnyVariableFlt64", "AnyVariable<Flt64>"),
]

# Import replacements: old import path -> list of new import paths
IMPORT_REPLACEMENTS = {
    # Math layer
    "fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality": ["fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality"],
    # MetaModel
    "fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.MetaModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.MetaModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.AbstractMetaModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.AbstractMetaModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMetaModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMetaModel"],
    # MechanismModel
    "fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.MechanismModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.MechanismModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.BasicMechanismModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.BasicMechanismModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.SingleObjectMechanismModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.SingleObjectMechanismModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel"],
    "fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModelFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel"],
    # Solver output
    "fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutputFlt64": ["fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput"],
    # Constraint types
    "fuookami.ospf.kotlin.core.model.mechanism.SymbolicLinearInequalityFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.SymbolicLinearInequality"],
    "fuookami.ospf.kotlin.core.model.mechanism.SymbolicQuadraticInequalityFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.SymbolicQuadraticInequality"],
    "fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImplFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl"],
    "fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraintImplFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraintImpl"],
    "fuookami.ospf.kotlin.core.model.mechanism.LinearSubObjectFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject"],
    "fuookami.ospf.kotlin.core.model.mechanism.QuadraticSubObjectFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.QuadraticSubObject"],
    # LinearConstraint/QuadraticConstraint are internal - replace with Constraint
    "fuookami.ospf.kotlin.core.model.mechanism.LinearConstraint": ["fuookami.ospf.kotlin.core.model.mechanism.Constraint"],
    "fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraint": ["fuookami.ospf.kotlin.core.model.mechanism.Constraint"],
    # DualSolution types
    "fuookami.ospf.kotlin.core.model.mechanism.LinearDualSolution": ["fuookami.ospf.kotlin.core.model.mechanism.Constraint"],
    "fuookami.ospf.kotlin.core.model.mechanism.QuadraticDualSolution": ["fuookami.ospf.kotlin.core.model.mechanism.Constraint"],
    "fuookami.ospf.kotlin.core.model.mechanism.DualSolution": ["fuookami.ospf.kotlin.core.model.mechanism.Constraint"],
    # Callback
    "fuookami.ospf.kotlin.core.model.callback.CallBackModelFlt64": ["fuookami.ospf.kotlin.core.model.callback.CallBackModel"],
    "fuookami.ospf.kotlin.core.model.callback.MultiObjectCallBackModelFlt64": ["fuookami.ospf.kotlin.core.model.callback.MultiObjectCallBackModel"],
    # Solution
    "fuookami.ospf.kotlin.core.model.basic.SolutionFlt64": ["fuookami.ospf.kotlin.core.model.basic.Solution"],
    # Product
    "fuookami.ospf.kotlin.core.model.mechanism.ProductFunctionFlt64": ["fuookami.ospf.kotlin.core.model.mechanism.ProductFunction"],
    # Cells
    "fuookami.ospf.kotlin.core.model.intermediate.CellFlt64": ["fuookami.ospf.kotlin.core.model.intermediate.Cell"],
    "fuookami.ospf.kotlin.core.model.intermediate.LinearCellFlt64": ["fuookami.ospf.kotlin.core.model.intermediate.LinearCell"],
    "fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellFlt64": ["fuookami.ospf.kotlin.core.model.intermediate.QuadraticCell"],
    "fuookami.ospf.kotlin.core.model.intermediate.LinearCellImplFlt64": ["fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl"],
    "fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImplFlt64": ["fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl"],
    "fuookami.ospf.kotlin.core.model.intermediate.SparseMatrixFlt64": ["fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix"],
    "fuookami.ospf.kotlin.core.model.intermediate.SparseVectorFlt64": ["fuookami.ospf.kotlin.core.model.intermediate.SparseVector"],
    # Intermediate symbols
    "fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbolFlt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolFlt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbolFlt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbolFlt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbolFlt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbol"],
    # Note: QuantityIntermediateSymbol, QuantityLinearIntermediateSymbol, etc. are
    # NOT Flt64-suffixed type aliases - they still exist and should NOT be replaced.
    # Only the *Flt64 suffixed versions were deleted.
    # Token
    "fuookami.ospf.kotlin.core.token.TokenTableFlt64": ["fuookami.ospf.kotlin.core.token.TokenTable"],
    "fuookami.ospf.kotlin.core.token.MutableTokenTableFlt64": ["fuookami.ospf.kotlin.core.token.MutableTokenTable"],
    "fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTableFlt64": ["fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable"],
    "fuookami.ospf.kotlin.core.token.ConcurrentTokenTableFlt64": ["fuookami.ospf.kotlin.core.token.ConcurrentTokenTable"],
    "fuookami.ospf.kotlin.core.token.AbstractTokenTableFlt64": ["fuookami.ospf.kotlin.core.token.AbstractTokenTable"],
    "fuookami.ospf.kotlin.core.token.AbstractMutableTokenTableFlt64": ["fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable"],
    "fuookami.ospf.kotlin.core.token.TokenListFlt64": ["fuookami.ospf.kotlin.core.token.TokenList"],
    "fuookami.ospf.kotlin.core.token.MutableTokenListFlt64": ["fuookami.ospf.kotlin.core.token.MutableTokenList"],
    "fuookami.ospf.kotlin.core.token.AutoTokenListFlt64": ["fuookami.ospf.kotlin.core.token.AutoTokenList"],
    "fuookami.ospf.kotlin.core.token.ManualTokenListFlt64": ["fuookami.ospf.kotlin.core.token.ManualTokenList"],
    "fuookami.ospf.kotlin.core.token.AbstractTokenListFlt64": ["fuookami.ospf.kotlin.core.token.AbstractTokenList"],
    "fuookami.ospf.kotlin.core.token.AbstractMutableTokenListFlt64": ["fuookami.ospf.kotlin.core.token.AbstractMutableTokenList"],
    "fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64": ["fuookami.ospf.kotlin.core.token.AddableTokenCollection"],
    "fuookami.ospf.kotlin.core.token.AnyVariableFlt64": ["fuookami.ospf.kotlin.core.token.AnyVariable"],
    "fuookami.ospf.kotlin.core.variable.AnyVariableFlt64": ["fuookami.ospf.kotlin.core.variable.AnyVariable"],
    "fuookami.ospf.kotlin.core.token.TokenCacheContextsFlt64": ["fuookami.ospf.kotlin.core.token.TokenCacheContexts"],
    "fuookami.ospf.kotlin.core.token.ValueCacheContextFlt64": ["fuookami.ospf.kotlin.core.token.ValueCacheContext"],
    "fuookami.ospf.kotlin.core.token.RangeCacheContextFlt64": ["fuookami.ospf.kotlin.core.token.RangeCacheContext"],
    "fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64": ["fuookami.ospf.kotlin.core.token.LinearFlattenData"],
    "fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64": ["fuookami.ospf.kotlin.core.token.QuadraticFlattenData"],
    "fuookami.ospf.kotlin.core.token.LinearFlattenContextFlt64": ["fuookami.ospf.kotlin.core.token.LinearFlattenContext"],
    "fuookami.ospf.kotlin.core.token.QuadraticFlattenContextFlt64": ["fuookami.ospf.kotlin.core.token.QuadraticFlattenContext"],
    # SymbolCombination type aliases (now deleted)
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols1Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols1"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols2Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols2"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols3Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols3"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols4Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols4"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols1Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols1"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols2Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols2"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols3Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols3"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols4Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols4"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols2Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols2"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols3Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols3"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols4Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols4"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearExpressionSymbols1Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearExpressionSymbols1"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearExpressionSymbols2Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearExpressionSymbols2"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearExpressionSymbols3Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearExpressionSymbols3"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearExpressionSymbols4Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearExpressionSymbols4"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticExpressionSymbols1Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticExpressionSymbols1"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticExpressionSymbols2Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticExpressionSymbols2"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticExpressionSymbols3Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticExpressionSymbols3"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticExpressionSymbols4Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticExpressionSymbols4"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearIntermediateSymbols1Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearIntermediateSymbols1"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearIntermediateSymbols2Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearIntermediateSymbols2"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearIntermediateSymbols3Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearIntermediateSymbols3"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearIntermediateSymbols4Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityLinearIntermediateSymbols4"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticIntermediateSymbols1Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticIntermediateSymbols1"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticIntermediateSymbols2Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticIntermediateSymbols2"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticIntermediateSymbols3Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticIntermediateSymbols3"],
    "fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticIntermediateSymbols4Flt64": ["fuookami.ospf.kotlin.core.intermediate_symbol.QuantityQuadraticIntermediateSymbols4"],
    # Quantities layer
    "fuookami.ospf.kotlin.quantities.quantity.QuantityFlt64": ["fuookami.ospf.kotlin.quantities.quantity.Quantity"],
    "fuookami.ospf.kotlin.quantities.quantity.QuantityLinearFlt64": ["fuookami.ospf.kotlin.quantities.quantity.Quantity"],
    "fuookami.ospf.kotlin.quantities.quantity.QuantityQuadraticFlt64": ["fuookami.ospf.kotlin.quantities.quantity.Quantity"],
    "fuookami.ospf.kotlin.quantities.quantity.QuantityCanonicalFlt64": ["fuookami.ospf.kotlin.quantities.quantity.Quantity"],
}

# When these type patterns appear in the body, we need to ensure these imports exist
AUTO_IMPORTS = {
    r"Constraint<Flt64,\s*Linear>": [
        "fuookami.ospf.kotlin.core.model.mechanism.Constraint",
        "fuookami.ospf.kotlin.core.model.mechanism.Linear",
    ],
    r"Constraint<Flt64,\s*Quadratic>": [
        "fuookami.ospf.kotlin.core.model.mechanism.Constraint",
        "fuookami.ospf.kotlin.core.model.mechanism.Quadratic",
    ],
    r"<Flt64>": [
        "fuookami.ospf.kotlin.math.algebra.number.Flt64",
    ],
}

TARGET_DIRS = [
    "ospf-kotlin-core/src/main",
    "ospf-kotlin-core/src/test",
    "ospf-kotlin-quantities/src/main",
    "ospf-kotlin-core-plugin",
    "ospf-kotlin-framework",
    "ospf-kotlin-framework-gantt-scheduling",
    "ospf-kotlin-framework-bpp3d",
    "ospf-kotlin-example",
]


def process_file(filepath):
    """Process a single Kotlin file."""
    with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
        content = f.read()

    original = content
    new_imports_needed = set()

    # Step 1: Fix import lines - remove old, collect new
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("import "):
            import_path = stripped[7:].split("//")[0].strip()
            if import_path in IMPORT_REPLACEMENTS:
                for new_import in IMPORT_REPLACEMENTS[import_path]:
                    new_imports_needed.add(new_import)
                continue  # remove this import line
        new_lines.append(line)

    # Step 2: Apply type reference replacements (skip import lines)
    result_lines = []
    for line in new_lines:
        if line.strip().startswith("import "):
            result_lines.append(line)
        else:
            modified = line
            for old, new in TYPE_REPLACEMENTS:
                modified = re.sub(r'\b' + re.escape(old) + r'\b', new, modified)
            result_lines.append(modified)

    # Step 3: Check if we need auto-imports for Constraint<Flt64, Linear/Quadratic>
    body_content = '\n'.join(l for l in result_lines if not l.strip().startswith("import "))
    for pattern, import_paths in AUTO_IMPORTS.items():
        if re.search(pattern, body_content):
            for import_path in import_paths:
                new_imports_needed.add(import_path)

    # Step 4: Insert new imports after the last existing import
    if new_imports_needed:
        last_import_idx = -1
        for i, line in enumerate(result_lines):
            if line.strip().startswith("import "):
                last_import_idx = i

        existing_imports = {l.strip() for l in result_lines if l.strip().startswith("import ")}
        imports_to_add = []
        for imp in sorted(new_imports_needed):
            if f"import {imp}" not in existing_imports:
                imports_to_add.append(f"import {imp}")

        if imports_to_add:
            for j, imp in enumerate(imports_to_add):
                result_lines.insert(last_import_idx + 1 + j, imp)

    content = '\n'.join(result_lines)

    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False


def main():
    base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    total_files = 0
    changed_files = 0

    for target_dir in TARGET_DIRS:
        dir_path = os.path.join(base, target_dir)
        if not os.path.exists(dir_path):
            continue
        for root, dirs, files in os.walk(dir_path):
            for f in files:
                if f.endswith('.kt'):
                    filepath = os.path.join(root, f)
                    total_files += 1
                    try:
                        if process_file(filepath):
                            changed_files += 1
                            print(f"  Modified: {os.path.relpath(filepath, base)}")
                    except Exception as e:
                        print(f"  ERROR: {os.path.relpath(filepath, base)}: {e}")

    print(f"\nProcessed {total_files} files, modified {changed_files}")


if __name__ == "__main__":
    main()