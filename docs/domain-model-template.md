# [Bounded Context Name] Domain Model

[toc]

## 1. Overview

[A one-sentence description of the bounded context's responsibility and the business problem it solves.]

### 1. Dependent Contexts

1. [Upstream context name]

---

## 2. Concepts / Entities

### 1. [Entity Name]

[A brief business description of the entity.]

**$X_{i}$** : [Property description, using mathematical notation to represent property $X$ of entity $i$.]
**$Y_{i}$** : [Property description.]
**$Z_{i}(a)$** : [Property description], its definition is further specified in *[Algorithm Name]*.

> Each property begins with a bold mathematical symbol followed by a natural language description. Subscripts denote the owning entity.
> If a property's definition involves a standalone algorithm, reference it using `its definition is further specified in *[Algorithm Name]*`.

### 2. [Next Entity]

**$A_{j}$** : [Property description.]
**$B_{j}$** : [Property description.]

---

## 3. Variables

### 1. Decision Variables

**$x_{ij}$** : [Variable semantics], [physical quantity / dimensionless], domain is $[range]$, [natural language meaning], $\forall i \in I$, $\forall j \in J$.

**$y_{j}$** : [Variable semantics], [physical quantity], domain is $[range]$, [natural language meaning], $\forall j \in J$.

> Decision variables are unknowns determined by the solver in an optimization problem. Each variable must specify:
> - Mathematical symbol (with subscripts)
> - Physical quantity type or dimensionless
> - Domain (discrete / continuous)
> - Natural language meaning
> - Universal quantifier ($\forall$ over the relevant sets)

### 2. Auxiliary Variables

**$u_{ij}$** : [Auxiliary variable semantics], domain is $[range]$, [meaning], $\forall i \in I$, $\forall j \in J$.

> Auxiliary variables represent non-decision quantities such as slack variables or penalty terms.

---

## 4. Predicates

### 1. [Predicate Group Name]

> Predicates classify entity sets; each predicate defines a subset.

**[Predicate Name]** : [Predicate semantics].
**[Predicate Name]** : [Predicate semantics].
**[Predicate Name]** : [Predicate semantics].

### 2. [Next Predicate Group]

**[Predicate Name]** : [Predicate semantics].
**[Predicate Name]** : [Predicate semantics].

---

## 5. Sets

### 1. [Entity Category]

**$S$** : [Universal set description.]

**$S^{P}$** : Subset satisfying predicate $P$, [business meaning].
**$S^{Q}$** : Subset satisfying predicate $Q$, [business meaning].
**$S^{R}_{i}$** : Subset of entity $i$ satisfying predicate $R$, [business meaning].
**$S^{P, Q}$** : Intersection of predicates $P$ and $Q$, [business meaning].

> Set definition rules:
> - Uppercase letters denote universal sets
> - Superscripts denote predicate filter conditions
> - Subscripts denote scoping constraints (optional)
> - Each set must include a business meaning
> - If a set's definition involves a standalone algorithm, reference it using `its definition is further specified in *[Algorithm Name]*`

### 2. [Next Entity Category]

**$T$** : [Universal set description.]

**$T^{R}$** : Subset satisfying predicate $R$, [business meaning].

### 3. [Entity Pairs / Relations]

**$RN$** : [Relation description], its definition is further specified in *[Algorithm Name]*.

> For sets or relations that require standalone algorithm definitions (e.g., adjacency, ordering), reference the algorithm document here.

---

## 6. Intermediate Values

### 1. [Intermediate Value Name]

**Description**: [Natural language description of the meaning and computation logic.]

$$
M = f(S, T)
$$

> Or piecewise definition:

$$
M = \begin{cases}
f_1(S),& \text{condition}_1 \\ \\
f_2(T),& \text{condition}_2
\end{cases}
$$

### 2. [Next Intermediate Value]

**Description**: [Natural language description.]

$$
N = \sum_{i \in I} g(i)
$$

> Intermediate value definition rules:
> - First describe in natural language
> - Then provide a precise mathematical definition using LaTeX
> - Use `cases` environment for piecewise functions
> - Use standard mathematical notation for summation, products, etc.
> - Specify quantifiers and constraints
> - If the computation involves a standalone algorithm, reference it using `its definition is further specified in *[Algorithm Name]*`

---

## 7. Assertions

> Assertions are properties that always hold in the domain, used to verify data consistency and business rules.

### 1. [Assertion Name]

**Description**: [Natural language description of the assertion.]

$$
\forall i \in I \; (P(i) \rightarrow Q(i))
$$

### 2. [Next Assertion]

**Description**: [Natural language description.]

$$
|S^{P}| \leq |T^{Q}|
$$

> Assertion definition rules:
> - Use first-order logic symbols
> - $\forall$ universal quantifier, $\exists$ existential quantifier
> - $\rightarrow$ implication, $\wedge$ conjunction, $\vee$ disjunction
> - $\cup$ union, $\cap$ intersection, $\emptyset$ empty set
> - $|S|$ cardinality of set
> - Annotate applicable conditions when necessary (aircraft model constraints, algorithm constraints, etc.)

---

## 8. Constraints

> Constraints are conditions that must be satisfied in the optimization model, divided into hard and soft constraints.

### 1. [Constraint Name]

**[CN]**: [Chinese Constraint Name]
**Description**: [Natural language description of the business meaning.]

$$
s.t. \quad \text{expression} \leq \text{rhs}, \; \forall i \in I
$$

### 2. [Next Constraint]

**[CN]**: [Chinese Constraint Name]
**Description**: [Natural language description.]

$$
s.t. \quad \sum_{j \in J} a_{ij} x_{ij} = b_i, \; \forall i \in I
$$

> Constraint definition rules:
> - English name + Chinese name (in brackets)
> - Natural language description
> - Begin with `s.t.` (subject to)
> - Use standard mathematical programming notation
> - Specify universal quantifiers
> - Mark corollaries with `**Corollary**` and include derivation

**Corollary**: [Corollary description.]

$$
\text{mathematical expression}
$$

---

## 9. Objective Function (if applicable)

**Description**: [Natural language description of the optimization objective.]

$$
\min \sum_{i \in I} \sum_{j \in J} c_{ij} x_{ij}
$$

> Or:

$$
\max Z = f(x, y, z)
$$

---

## 10. Algorithm References

> List all algorithm documents referenced by this domain model. Each algorithm document resides in the context's `doc/` subdirectory.

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| [Algorithm 1] | `doc/[algo1-dir]/[algo1-doc].md` | Section 2.x | |
| [Algorithm 2] | `doc/[algo2-dir]/[algo2-doc].md` | Section 5.x | |

---

## 11. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| | | |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| | | | |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| | | |
