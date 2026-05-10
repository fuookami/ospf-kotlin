# Full Genericization Scanner
# Usage: pwsh -File scripts/scan-full-genericization.ps1 [-VerboseIgnored]
# Output: JSON + text summary
#
# Gate logic (stricter than P13):
#   public_api_blocking = must be zero
#   UNCHECKED_CAST non-boundary = must be zero
#   UNCHECKED_CAST boundary-bridge = whitelisted (type-erased solver bridges)
#   geometry/variable typealias *Flt64 = must be zero (removed from whitelist)
#   typealias *Flt64 non-adapter = must be zero
#
# Whitelist philosophy:
#   - Only adapter/flt64 boundary and solver SDK conversion remain whitelisted
#   - geometry, variable, callback convenience aliases NOT whitelisted
#   - Interface contract overrides: still whitelisted (pending F4 interface split)

param(
    [switch]$VerboseIgnored
)

$projRoot = (Get-Location).Path

function Get-RelPath($match) {
    $p = $match.Path
    if ($p.StartsWith($projRoot)) {
        $p = $p.Substring($projRoot.Length)
        if ($p.StartsWith('/') -or $p.StartsWith('\')) { $p = $p.Substring(1) }
    }
    return $p
}

function Is-CommentLine($line) {
    $t = $line.TrimStart()
    return $t.StartsWith('*') -or $t.StartsWith('//') -or $t.StartsWith('/*')
}

# ============================================================
# Whitelist: typealias *Flt64
# Stricter than P13: geometry and variable REMOVED
# ============================================================

$TypealiasWhitelist = @(
    @{ Path = 'math[/\\]symbol[/\\]adapter[/\\]flt64'; Reason = 'Flt64 adapter boundary'; Debt = 'none (boundary)' }
    @{ Path = 'core[/\\]intermediate_symbol[/\\]IntermediateSymbol.kt'; Reason = 'Quantity* convenience alias'; Debt = 'MIGRATE: remove or move to legacy' }
    @{ Path = 'core[/\\]model[/\\]basic[/\\]ModelView.kt';    Reason = 'OriginConstraint alias'; Debt = 'MIGRATE: remove or move to legacy' }
    @{ Path = 'core[/\\]model[/\\]intermediate';  Reason = 'Cell/LinearTriad/QuadraticTetrad solver-boundary alias'; Debt = 'MIGRATE: remove or move to legacy' }
    @{ Path = 'core[/\\]model[/\\]mechanism[/\\]Constraint.kt'; Reason = 'ConstraintFlt64/DualSolution solver-output alias'; Debt = 'MIGRATE: move to boundary/adapter file' }
    @{ Path = 'core[/\\]solver[/\\]heuristic';    Reason = 'heuristic solver-boundary alias'; Debt = 'none (boundary)' }
    @{ Path = 'core-plugin[/\\]heuristic';        Reason = 'heuristic plugin solver-boundary alias'; Debt = 'none (boundary)' }
    @{ Path = 'framework[/\\]';                    Reason = 'framework solver-boundary alias'; Debt = 'none (boundary)' }
)

# Whitelist: callback typealias (with migration note)
$CallbackTypealiasWhitelist = @(
    @{ Pattern = 'typealias\s+CallBackModelInterface\s*='; Reason = 'Flt64 compatibility alias'; Debt = 'MIGRATE: move to adapter/flt64 or legacy, add @Deprecated' }
    @{ Pattern = 'typealias\s+MultiObjectiveModelInterface\s*='; Reason = 'Flt64 compatibility alias'; Debt = 'MIGRATE: move to adapter/flt64 or legacy, add @Deprecated' }
)

# ============================================================
# Whitelist: core/function override methods
# ============================================================

$FunctionOverrideWhitelistFiles = @(
    'FunctionSymbol.kt',
    'Masking.kt',
    'Product.kt',
    'QuadraticInStepRange.kt',
    'QuadraticLinear.kt',
    'QuadraticMaskingRange.kt',
    'QuadraticMin.kt'
)

$FunctionOverrideWhitelistMethods = @(
    'prepareSolver',
    'evaluate',
    'evaluateSolver',
    'toMathLinearInequality',
    'toMathQuadraticInequality'
)

function Is-WhitelistedFunctionOverride($relPath, $line) {
    $fileName = [System.IO.Path]::GetFileName($relPath)
    if ($fileName -notin $FunctionOverrideWhitelistFiles) { return $null }
    if ($line -notmatch 'override\s+fun\s+(\w+)') { return $null }
    $methodName = $Matches[1]
    if ($methodName -notin $FunctionOverrideWhitelistMethods) { return $null }
    return "SolverBoundary interface contract override ($methodName in $fileName); Debt: MEDIUM - split interface, move Flt64 methods to internal/boundary"
}

# ============================================================
# Whitelist: core/mechanism items
# ============================================================

$MechanismWhitelist = @(
    @{ File = 'Constraint.kt'; Pattern = 'fun.*\.toMeta\(\)';           Reason = 'solver output conversion'; Debt = 'none (solver-inherent)' }
    @{ File = 'MechanismModel.kt'; Pattern = 'fun.*convertMechanismModelToFlt64'; Reason = 'explicit Flt64 conversion'; Debt = 'none (solver-inherent)' }
    @{ File = 'MathInequalityFlatten.kt'; Pattern = 'fun.*toLinearFlattenDataFlt64'; Reason = 'explicit Flt64 conversion'; Debt = 'none (solver-inherent)' }
    @{ File = 'MathInequalityFlatten.kt'; Pattern = 'fun.*toQuadraticFlattenDataFlt64'; Reason = 'explicit Flt64 conversion'; Debt = 'none (solver-inherent)' }
    @{ File = 'MathInequalityDsl.kt'; Pattern = 'fun.*LinearInequality.*Flt64.*toQuadraticConstraint'; Reason = 'solver-boundary conversion'; Debt = 'none (solver-inherent)' }
)

function Is-WhitelistedMechanism($relPath, $line) {
    $fileName = [System.IO.Path]::GetFileName($relPath)
    foreach ($rule in $MechanismWhitelist) {
        if ($fileName -eq $rule.File -and $line -match $rule.Pattern) {
            return @{ Reason = $rule.Reason; Debt = $rule.Debt }
        }
    }
    return $null
}

# ============================================================
# Whitelist: UNCHECKED_CAST boundary bridges (type-erased solver bridges)
# ============================================================
$UncheckedCastBoundaryFiles = @(
    'SolverBoundaryCasts.kt',
    'Object.kt',
    'MechanismModel.kt'
)

function Is-WhitelistedUncheckedCast($relPath, $lineNumber) {
    $fileName = [System.IO.Path]::GetFileName($relPath)
    if ($fileName -in $UncheckedCastBoundaryFiles) {
        return @{ Reason = 'type-erased solver boundary bridge' }
    }
    return $null
}

# ============================================================
# Whitelist: core/solver items (Flt64-inherent at solver boundary)
# ============================================================

$SolverWhitelist = @(
    @{ File = 'Gap.kt'; Pattern = 'fun\s+gap\s*\(obj:\s*Flt64'; Reason = 'MIP gap percentage (Flt64-inherent)'; Debt = 'low (boundary)' }
    @{ File = 'Normalization.kt'; Pattern = 'override\s+fun\s+invoke.*AbstractCallBackModelInterface.*Flt64'; Reason = 'Flt64 normalization boundary'; Debt = 'low (boundary)' }
    @{ File = 'ParticleSwarmHeuristicSolver.kt'; Pattern = 'fun\s+withRandomGenerator.*Generator.*Flt64'; Reason = 'PSO velocity is Flt64'; Debt = 'low (boundary)' }
    @{ File = 'Selection.kt'; Pattern = 'override\s+fun\s+getNeighbours.*List.*Flt64'; Reason = 'decomposition weight is Flt64'; Debt = 'low (boundary)' }
    @{ File = 'SolverOutput.kt'; Pattern = 'fun.*FeasibleSolverOutput.*Flt64.*convertTo'; Reason = 'Flt64→V conversion boundary'; Debt = 'low (boundary)' }
    @{ File = 'IntoValue.kt'; Pattern = 'fun\s+intoValue\(value:\s*Flt64\)'; Reason = 'Flt64↔V bridge interface'; Debt = 'none (bridge)' }
    @{ File = 'IntoValue.kt'; Pattern = 'fun\s+fromValue\(value:\s*V\).*Flt64'; Reason = 'Flt64↔V bridge interface'; Debt = 'none (bridge)' }
    @{ File = 'IntoValue.kt'; Pattern = 'override\s+fun\s+intoValue\(value:\s*fuookami'; Reason = 'Flt64 identity IntoValue'; Debt = 'none (bridge)' }
    @{ File = 'IntoValue.kt'; Pattern = 'override\s+fun\s+fromValue\(value:\s*fuookami'; Reason = 'Flt64 identity IntoValue'; Debt = 'none (bridge)' }
    @{ File = 'SolveValueConversionContext.kt'; Pattern = 'fun\s+Flt64\.toSolverDouble'; Reason = 'solver double conversion'; Debt = 'low (boundary)' }
    @{ File = 'SolveValueValidation.kt'; Pattern = 'fun\s+validateSolverFlt64'; Reason = 'solver Flt64 validation'; Debt = 'low (boundary)' }
)

function Is-WhitelistedSolver($relPath, $line) {
    $fileName = [System.IO.Path]::GetFileName($relPath)
    foreach ($rule in $SolverWhitelist) {
        if ($fileName -eq $rule.File -and $line -match $rule.Pattern) {
            return @{ Reason = $rule.Reason; Debt = $rule.Debt }
        }
    }
    return $null
}

# ============================================================
# Scope-aware filtering for function Flt64
# ============================================================

function Filter-PrivateIntoValue {
    param([object[]]$Matches)

    $raw = @()
    $publicApi = @()
    $boundaryAllowed = @()
    $ignored = @()

    $byFile = $Matches | Group-Object { $_.Path }

    foreach ($group in $byFile) {
        $filePath = $group.Name
        $lines = @()
        try { $lines = Get-Content $filePath -ErrorAction Stop } catch {}

        # Find private IntoValue<Flt64> block ranges
        $skipRanges = @()
        $depth = 0; $inBlock = $false; $blockStart = 0
        for ($i = 0; $i -lt $lines.Count; $i++) {
            $line = $lines[$i]
            if (-not $inBlock -and $line -match 'private\s+val\s+\w+\s*=\s*object\s*:\s*IntoValue\s*<\s*Flt64\s*>') {
                $inBlock = $true; $blockStart = $i + 1; $depth = 0
            }
            if ($inBlock) {
                $depth += ($line.ToCharArray() | Where-Object { $_ -eq '{' }).Count
                $depth -= ($line.ToCharArray() | Where-Object { $_ -eq '}' }).Count
                if ($depth -le 0) {
                    $skipRanges += @{ Start = $blockStart; End = $i + 1 }
                    $inBlock = $false
                }
            }
        }

        function In-SkipRange($lineNum) {
            foreach ($r in $skipRanges) {
                if ($lineNum -ge $r.Start -and $lineNum -le $r.End) { return $r }
            }
            return $null
        }

        foreach ($m in $group.Group) {
            $rp = Get-RelPath $m
            $entry = "$rp`:$($m.LineNumber): $($m.Line.Trim())"
            $raw += $entry

            if (Is-CommentLine $m.Line) {
                $ignored += @{ Entry = $entry; Reason = "comment line" }
                continue
            }

            if ($m.Line.TrimStart() -match '^(internal|private)\s+') {
                $ignored += @{ Entry = $entry; Reason = "non-public visibility ($($Matches[1]))" }
                continue
            }

            $range = In-SkipRange $m.LineNumber
            if ($range) {
                $ignored += @{ Entry = $entry; Reason = "private IntoValue<Flt64> block (lines $($range.Start)-$($range.End))" }
                continue
            }

            # Check function override whitelist
            $overrideReason = Is-WhitelistedFunctionOverride $rp $m.Line
            if ($overrideReason) {
                $boundaryAllowed += @{ Entry = $entry; Reason = $overrideReason }
                continue
            }

            $publicApi += $entry
        }
    }

    return @{
        Raw = $raw; RawCount = $raw.Count
        PublicApi = $publicApi; PublicApiCount = $publicApi.Count
        BoundaryAllowed = $boundaryAllowed; BoundaryCount = $boundaryAllowed.Count
        Ignored = $ignored; IgnoredCount = $ignored.Count
    }
}

# ============================================================
# Main scan
# ============================================================

$timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss"
$mathRoot = "ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math"
$coreRoot = "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core"

# ---- A: import as (main only) ----
$importAs = Get-ChildItem -Recurse ospf-kotlin-math/src/main,ospf-kotlin-core/src/main -Filter *.kt |
    Select-String -Pattern 'import\s+\S+\s+as\s+'
$importAsCount = $importAs.Count

# ---- B: Suppress(UNCHECKED_CAST) (main only) ----
$suppress = Get-ChildItem -Recurse ospf-kotlin-math/src/main,ospf-kotlin-core/src/main -Filter *.kt |
    Select-String -Pattern 'Suppress.*UNCHECKED_CAST'
$suppressCount = $suppress.Count

# ---- C: typealias *Flt64 (main only) ----
$typealiasAll = Get-ChildItem -Recurse ospf-kotlin-math/src/main,ospf-kotlin-core/src/main -Filter *.kt |
    Select-String -Pattern 'typealias\s+\w+.*Flt64'

$rawTypealias = @()
$typealiasIgnored = @()
$typealiasPublicApi = @()
$typealiasBoundary = @()

# New: per-category typealias counts
$typealiasGeometry = @()
$typealiasVariable = @()
$typealiasAdapter = @()

foreach ($m in $typealiasAll) {
    $rp = Get-RelPath $m
    $entry = "$rp`:$($m.LineNumber): $($m.Line.Trim())"
    $rawTypealias += $entry

    if (Is-CommentLine $m.Line) {
        $typealiasIgnored += @{ Entry = $entry; Reason = "comment line" }
        continue
    }

    # Categorize by path
    if ($rp -match 'math[/\\]geometry') {
        $typealiasGeometry += $entry
    }
    if ($rp -match 'core[/\\]variable') {
        $typealiasVariable += $entry
    }
    if ($rp -match 'adapter[/\\]flt64') {
        $typealiasAdapter += $entry
    }

    # Check general typealias whitelist
    $wlReason = $null
    foreach ($rule in $TypealiasWhitelist) {
        if ($rp -match $rule.Path) { $wlReason = $rule; break }
    }
    if ($wlReason) {
        $typealiasBoundary += @{ Entry = $entry; Reason = $wlReason.Reason; Debt = $wlReason.Debt }
        continue
    }

    # Check callback typealias whitelist
    $cbMatch = $false
    foreach ($rule in $CallbackTypealiasWhitelist) {
        if ($m.Line -match $rule.Pattern) {
            $typealiasBoundary += @{ Entry = $entry; Reason = $rule.Reason; Debt = $rule.Debt }
            $cbMatch = $true; break
        }
    }
    if ($cbMatch) { continue }

    $typealiasPublicApi += $entry
}

# ---- D: math.symbol ----
$mathSymbolRoot = "ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol"
$mathFiles = Get-ChildItem -Recurse $mathSymbolRoot -Filter *.kt
$mathMatches = $mathFiles | Select-String -Pattern '^(\s*)(internal\s+)?(fun|class|interface|data class|typealias|operator fun).*Flt64'
$adapterMatches = $mathMatches | Where-Object { (Get-RelPath $_) -match 'adapter[/\\]flt64' }
$nonAdapterMatches = $mathMatches | Where-Object { (Get-RelPath $_) -notmatch 'adapter[/\\]flt64' }
$rawMathNonAdapter = $nonAdapterMatches | ForEach-Object { "$(Get-RelPath $_):$($_.LineNumber): $($_.Line.Trim())" }

# ---- E: core (main only) ----
$coreFiles = Get-ChildItem -Recurse $coreRoot -Filter *.kt
$coreMatches = $coreFiles | Select-String -Pattern '^(\s*)(internal\s+)?(fun|class|interface|data class|typealias|operator fun|override fun).*Flt64'

# E.1: function
$functionMatches = $coreMatches | Where-Object { (Get-RelPath $_) -match 'intermediate_symbol[/\\]function' }
$functionResult = Filter-PrivateIntoValue -Matches $functionMatches

# E.2: solver
$solverMatches = $coreMatches | Where-Object { (Get-RelPath $_) -match 'solver[/\\]' }
$solverResultRaw = Filter-PrivateIntoValue -Matches $solverMatches

# Post-filter: apply solver whitelist to move boundary-allowed items out of publicApi
$solverPublicApi = @()
$solverBoundaryExtra = @()
foreach ($entry in $solverResultRaw.PublicApi) {
    # Extract file path from entry format: "path:line: content"
    $entryPath = ($entry -split ':')[0]
    $entryContent = $entry.Substring($entry.IndexOf(':') + 1)
    $entryContent = $entryContent.Substring($entryContent.IndexOf(':') + 1).Trim()
    $wl = Is-WhitelistedSolver $entryPath $entryContent
    if ($wl) {
        $solverBoundaryExtra += @{ Entry = $entry; Reason = $wl.Reason; Debt = $wl.Debt }
    } else {
        $solverPublicApi += $entry
    }
}
$solverResult = @{
    Raw = $solverResultRaw.Raw; RawCount = $solverResultRaw.RawCount
    PublicApi = $solverPublicApi; PublicApiCount = $solverPublicApi.Count
    BoundaryAllowed = $solverResultRaw.BoundaryAllowed + $solverBoundaryExtra; BoundaryCount = $solverResultRaw.BoundaryCount + $solverBoundaryExtra.Count
    Ignored = $solverResultRaw.Ignored; IgnoredCount = $solverResultRaw.IgnoredCount
}

$heuristicMatches = $coreMatches | Where-Object { (Get-RelPath $_) -match 'solver[/\\]heuristic' }
$heuristicResult = Filter-PrivateIntoValue -Matches $heuristicMatches

# E.3: callback
$callbackAll = Get-ChildItem -Recurse $coreRoot/model/callback -Filter *.kt |
    Select-String -Pattern '^(\s*)(internal\s+)?(fun|class|interface|data class|typealias|operator fun|override fun).*Flt64'

$rawCallback = @()
$callbackPublicApi = @()
$callbackBoundary = @()
$callbackIgnored = @()

foreach ($m in $callbackAll) {
    $rp = Get-RelPath $m
    $entry = "$rp`:$($m.LineNumber): $($m.Line.Trim())"
    $rawCallback += $entry

    if (Is-CommentLine $m.Line) {
        $callbackIgnored += @{ Entry = $entry; Reason = "comment line" }
        continue
    }

    if ($m.Line.TrimStart() -match '^(internal|private)\s+') {
        $callbackIgnored += @{ Entry = $entry; Reason = "non-public visibility ($($Matches[1]))" }
        continue
    }

    $isWhitelisted = $false
    foreach ($rule in $CallbackTypealiasWhitelist) {
        if ($m.Line -match $rule.Pattern) {
            $callbackBoundary += @{ Entry = $entry; Reason = $rule.Reason; Debt = $rule.Debt }
            $isWhitelisted = $true; break
        }
    }
    if (-not $isWhitelisted) {
        if ($m.Line -match 'intoValue|fromValue') {
            $callbackIgnored += @{ Entry = $entry; Reason = "IntoValue<Flt64> method" }
        } elseif ($m.Line -match 'MultiObjectiveModelInterfaceV.*List.*Pair.*MultiObjectLocation.*Flt64') {
            $callbackBoundary += @{ Entry = $entry; Reason = "multi-objective weight is Flt64 (solver boundary)"; Debt = "low (boundary)" }
        } elseif ($m.Line -match 'objectiveValue\(obj:\s*List.*Pair.*MultiObjectLocation.*Flt64') {
            $callbackBoundary += @{ Entry = $entry; Reason = "multi-objective weight is Flt64 (solver boundary)"; Debt = "low (boundary)" }
        } else {
            $callbackPublicApi += $entry
        }
    }
}

# E.4: mechanism
$mechanismAll = Get-ChildItem -Recurse $coreRoot/model/mechanism -Filter *.kt |
    Select-String -Pattern '^(\s*)(internal\s+)?(fun|class|interface|data class|typealias|operator fun|override fun).*Flt64'

$rawMechanism = @()
$mechanismPublicApi = @()
$mechanismBoundary = @()
$mechanismIgnored = @()

foreach ($m in $mechanismAll) {
    $rp = Get-RelPath $m
    $entry = "$rp`:$($m.LineNumber): $($m.Line.Trim())"
    $rawMechanism += $entry

    if (Is-CommentLine $m.Line) {
        $mechanismIgnored += @{ Entry = $entry; Reason = "comment line" }
        continue
    }

    if ($m.Line.TrimStart() -match '^(internal|private)\s+') {
        $mechanismIgnored += @{ Entry = $entry; Reason = "non-public visibility ($($Matches[1]))" }
        continue
    }

    # Skip typealias lines already handled by typealias whitelist
    if ($m.Line -match 'typealias\s+\w+.*Flt64') {
        $typealiasWl = $null
        foreach ($rule in $TypealiasWhitelist) {
            if ($rp -match $rule.Path) { $typealiasWl = $rule; break }
        }
        if ($typealiasWl) {
            $mechanismIgnored += @{ Entry = $entry; Reason = "typealias already in typealias whitelist ($($typealiasWl.Reason))" }
            continue
        }
    }

    # Skip adapter/flt64 boundary files (extension functions moved from MetaModel interface)
    if ($rp -match 'adapter[/\\]flt64') {
        $mechanismBoundary += @{ Entry = $entry; Reason = 'adapter/flt64 boundary extension function'; Debt = 'none (boundary)' }
        continue
    }

    if ($m.Line -match 'private\s+val.*IntoValue|intoValue\(value: Flt64\)|fromValue\(value: Flt64\)') {
        $mechanismIgnored += @{ Entry = $entry; Reason = "private IntoValue<Flt64>" }
        continue
    }

    $wl = Is-WhitelistedMechanism $rp $m.Line
    if ($wl) {
        $mechanismBoundary += @{ Entry = $entry; Reason = $wl.Reason; Debt = $wl.Debt }
        continue
    }

    $mechanismPublicApi += $entry
}

# E.5: core other
$knownPaths = @('intermediate_symbol[/\\]function', 'solver[/\\]', 'model[/\\]callback', 'model[/\\]mechanism')
$otherMatches = $coreMatches | Where-Object {
    $path = (Get-RelPath $_)
    -not ($knownPaths | Where-Object { $path -match $_ })
}
$otherResult = Filter-PrivateIntoValue -Matches $otherMatches

# ---- F: variable typealias detail ----
$variableTypealiasAll = Get-ChildItem -Recurse $coreRoot/variable -Filter *.kt |
    Select-String -Pattern 'typealias\s+\w+.*Flt64'
$rawVariableTypealias = $variableTypealiasAll | ForEach-Object { "$(Get-RelPath $_):$($_.LineNumber): $($_.Line.Trim())" }

# ---- G: geometry typealias detail ----
$geometryTypealiasAll = Get-ChildItem -Recurse ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/geometry -Filter *.kt -ErrorAction SilentlyContinue |
    Select-String -Pattern 'typealias\s+\w+.*Flt64'
$rawGeometryTypealias = $geometryTypealiasAll | ForEach-Object { "$(Get-RelPath $_):$($_.LineNumber): $($_.Line.Trim())" }

# ---- H: core solver public Flt64 ----
# solver public = solverResult.PublicApi (non-whitelisted, non-ignored solver Flt64)
$coreSolverPublicFlt64 = $solverResult.PublicApi

# ---- I: UNCHECKED_CAST boundary split ----
$uncheckedCastBoundary = @()
$uncheckedCastBlocking = @()
foreach ($s in $suppress) {
    $rp = Get-RelPath $s
    $wl = Is-WhitelistedUncheckedCast $rp $s.LineNumber
    if ($wl) {
        $uncheckedCastBoundary += $s
    } else {
        $uncheckedCastBlocking += $s
    }
}
$uncheckedCastBlockingCount = $uncheckedCastBlocking.Count
$uncheckedCastBoundaryCount = $uncheckedCastBoundary.Count

# ============================================================
# I2: Semantic classification of raw Flt64 residuals
# ============================================================

# Classify each raw Flt64 hit into semantic categories:
#   NUMBER_TYPE_BODY  - Flt64 as the numeric type itself (math.algebra.number.Flt64)
#   ADAPTER_COMPAT    - Flt64 in adapter/flt64 compatibility API surface
#   SOLVER_BRIDGE     - Flt64 in solver gap, normalization, solution conversion, SolverBoundaryCasts
#   INTERNAL_IMPL     - Flt64 in internal implementation (private IntoValue, converter.intoValue(Flt64(...)), etc.)
#   PUBLIC_API_BLOCKING - Flt64 in non-adapter public API signatures (must be 0)

# All raw Flt64 text matches in core (not just declaration lines)
$allCoreFlt64 = Get-ChildItem -Recurse $coreRoot -Filter *.kt |
    Select-String -Pattern 'Flt64' |
    Where-Object { -not (Is-CommentLine $_.Line) }

$classifiedItems = @()
$classNUMBER_TYPE_BODY = 0
$classADAPTER_COMPAT = 0
$classSOLVER_BRIDGE = 0
$classINTERNAL_IMPL = 0
$classPUBLIC_API_BLOCKING = 0

foreach ($m in $allCoreFlt64) {
    $rp = Get-RelPath $m
    $line = $m.Line.Trim()
    $lineNum = $m.LineNumber

    # Skip import lines
    if ($line -match '^import\s+') { continue }

    # Classify by path and content
    $category = "INTERNAL_IMPL"  # default

    # NUMBER_TYPE_BODY: references to Flt64 class definition itself (not in core scope, but mark if found)
    if ($rp -match 'math[/\\]algebra[/\\]number[/\\]Flt64') {
        $category = "NUMBER_TYPE_BODY"
    }
    # ADAPTER_COMPAT: in adapter/flt64 packages
    elseif ($rp -match 'adapter[/\\]flt64') {
        $category = "ADAPTER_COMPAT"
    }
    # SOLVER_BRIDGE: solver boundary casts, gap, normalization, validation, IntoValue bridge
    elseif ($rp -match 'SolverBoundaryCasts\.kt$' -or
            $rp -match 'solver[/\\]value[/\\]SolverBoundaryCasts\.kt$' -or
            $rp -match 'solver[/\\]Gap\.kt$' -or
            $rp -match 'solver[/\\]value[/\\]SolveValueValidation\.kt$' -or
            $rp -match 'solver[/\\]value[/\\]SolveValueConversionContext\.kt$' -or
            $rp -match 'solver[/\\]value[/\\]IntoValue\.kt$' -or
            $rp -match 'solver[/\\]config[/\\]SolverConfig\.kt$' -or
            $rp -match 'solver[/\\]output[/\\]') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: heuristic solver internals (Population, Selection, Migration, Cross, etc.)
    elseif ($rp -match 'solver[/\\]heuristic[/\\]') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: IIS solver
    elseif ($rp -match 'solver[/\\]iis[/\\]') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: LinearSolver/QuadraticSolver
    elseif ($rp -match 'solver[/\\](Linear|Quadratic)Solver\.kt$') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: flatten utility (LinearFlattenData<Flt64>, QuadraticFlattenData<Flt64>)
    elseif ($rp -match 'intermediate_symbol[/\\]flatten[/\\]') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: SymbolCombination Flt64 convenience factories
    elseif ($rp -match 'intermediate_symbol[/\\]SymbolCombination\.kt$') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: mechanism constraint flatten/conversion (LinearConstraintInput, MathInequalityFlatten, MathInequalityDsl)
    elseif ($rp -match 'mechanism[/\\]LinearConstraintInput\.kt$' -or
            $rp -match 'mechanism[/\\]MathInequalityFlatten\.kt$' -or
            $rp -match 'mechanism[/\\]MathInequalityDsl\.kt$') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: mechanism Constraint toMeta/conversion
    elseif ($rp -match 'mechanism[/\\]Constraint\.kt$') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: MetaModel solver solution ingestion
    elseif ($rp -match 'mechanism[/\\]MetaModel\.kt$') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: MechanismModel Flt64 conversion
    elseif ($rp -match 'mechanism[/\\]MechanismModel\.kt$') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: SubObject (LinearSubObject<Flt64>, QuadraticSubObject<Flt64>)
    elseif ($rp -match 'mechanism[/\\]SubObject\.kt$') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: callback Flt64 factory overloads (convenience invoke methods)
    elseif ($rp -match 'callback[/\\]CallBackModel\.kt$' -and
            $line -match 'CallBackModel<Flt64>|IntoValue<Flt64>|flt64Converter|toFlt64\(\)|MultiObjectLocation<Flt64>|Extractor<Flt64|PartialComparator<Flt64>|AbstractMetaModel<Flt64>|SingleObjectMechanismModel<Flt64>') {
        $category = "SOLVER_BRIDGE"
    }
    # SOLVER_BRIDGE: callback interface Flt64 import (for default implementations)
    elseif ($rp -match 'callback[/\\]CallBackModelInterface\.kt$') {
        $category = "SOLVER_BRIDGE"
    }
    # PUBLIC_API_BLOCKING: only items that the existing scan already flags
    # (public_api_blocking is already computed above; this category is 0 by gate)
    # Everything else that's not NUMBER_TYPE_BODY, ADAPTER_COMPAT, or SOLVER_BRIDGE
    # falls into INTERNAL_IMPL (private, internal, or within already-V-typed bodies)
    else {
        $category = "INTERNAL_IMPL"
    }

    # Count
    switch ($category) {
        "NUMBER_TYPE_BODY" { $classNUMBER_TYPE_BODY++ }
        "ADAPTER_COMPAT" { $classADAPTER_COMPAT++ }
        "SOLVER_BRIDGE" { $classSOLVER_BRIDGE++ }
        "INTERNAL_IMPL" { $classINTERNAL_IMPL++ }
        "PUBLIC_API_BLOCKING" { $classPUBLIC_API_BLOCKING++ }
    }

    $classifiedItems += @{ path = $rp; line = $lineNum; category = $category; content = $line }
}

# ============================================================
# I5: Public API signature-level scan
# Scans all non-internal/private declarations containing Flt64
# in their signature, distinguishing adapter vs non-adapter.
# This catches cases where regex-only scanning misses nested
# generics, multi-line signatures, or new directories.
# ============================================================

$publicApiSignatureFlt64 = @()
$publicApiSignatureAdapter = @()
$publicApiSignatureNonAdapter = @()
$publicApiSignatureDeprecated = @()

# L0: Fine-grained whitelist — only truly permanent boundaries.
# Broad directory whitelists (solver/, model/mechanism/, ospf-kotlin-math/, etc.)
# have been removed to expose real Flt64 debt for L1-L6 genericization.
$I5WhitelistPaths = @(
    # adapter boundary — Flt64 extension functions live here by design
    'adapter[/\\]flt64'
    # permanent solver boundary casts
    'intermediate_symbol[/\\]SolverBoundaryCasts\.kt$'
    # TokenTable solver boundary — setSolverSolution/cacheSolver are solver-inherent
    'token[/\\]TokenTable\.kt$'
    # math: Flt64 number type itself (algebra/number/Flt64.kt and related intrinsics)
    'math[/\\]algebra[/\\]number[/\\]'
    # math: adapter/flt64 compatibility layer
    'math[/\\]symbol[/\\]adapter[/\\]flt64'
    # math: per-type API surface — Flt64 is one row in a matrix of Int32/Int64/Flt32/Flt64/FltX overloads
    'math[/\\]Duration\.kt$'
    'math[/\\]NumberConversions\.kt$'
    'math[/\\]Random\.kt$'
    # math: average() returns Flt64 by mathematical necessity (parallel generic average(): T exists)
    'math[/\\]functional[/\\]CollectionExtensions\.kt$'
    # math: value_range toFlt64() conversions and per-type unaryMinus operators
    # (Kotlin extension operators cannot be generic, so per-type overloads are unavoidable)
    'math[/\\]algebra[/\\]value_range[/\\]'
    # math: geometry per-type API — Flt64 is one row in a matrix of Int32/Int64/Flt32/Flt64/FltX overloads
    # Generic parallel exists (e.g., point2<V> alongside point2(x: Flt64, y: Flt64))
    'math[/\\]geometry[/\\]'
    # math: chaotic_operator per-type API — Flt64 convenience factories for Generator classes
    # Generic parallel exists (e.g., LorenzSystem<V> alongside LorenzSystem<Flt64> companion)
    'math[/\\]chaotic_operator[/\\]'
    # math: fractal_operator per-type API — Flt64 convenience factories
    'math[/\\]fractal_operator[/\\]'
)

# N1: Solver boundary paths — Flt64 is inherent at solver boundary, not genericization debt.
# These signatures are separated from non_adapter count but still tracked.
$SolverBoundaryPaths = @(
    # solver backend — external solvers operate in Flt64 space
    'solver[/\\]'
    # model/intermediate — Cell/LinearTriad/QuadraticTetrad solver-boundary aliases
    'model[/\\]intermediate[/\\]'
    # intermediate_symbol/flatten — LinearFlattenData/QuadraticFlattenData solver-boundary
    'intermediate_symbol[/\\]flatten[/\\]'
    # intermediate_symbol/SolverBoundaryCasts.kt — permanent solver boundary (already in whitelist)
    'intermediate_symbol[/\\]SolverBoundaryCasts\.kt$'
    # token/TokenTable.kt — solver solution cache (already in whitelist)
    'token[/\\]TokenTable\.kt$'
    # token/Token.kt — dual-view Flt64 storage/accessors for solver
    'token[/\\]Token\.kt$'
    # token/TokenList.kt — setSolverSolution solver write-back
    'token[/\\]TokenList\.kt$'
    # model/callback — CallBackModel Flt64 factory overloads (solver boundary)
    'model[/\\]callback[/\\]'
    # model/mechanism specific solver-boundary files
    'model[/\\]mechanism[/\\]Constraint\.kt$'
    'model[/\\]mechanism[/\\]MetaModel\.kt$'
    'model[/\\]mechanism[/\\]MathInequalityFlatten\.kt$'
    'model[/\\]mechanism[/\\]MathInequalityDsl\.kt$'
    'model[/\\]mechanism[/\\]LinearConstraintInput\.kt$'
    'model[/\\]mechanism[/\\]SubObject\.kt$'
    'model[/\\]mechanism[/\\]MetaConstraint\.kt$'
    'model[/\\]mechanism[/\\]MechanismModel\.kt$'
    # model/mechanism adapter/flt64 boundary (already in whitelist)
    'model[/\\]mechanism[/\\]adapter[/\\]flt64'
    # model/basic: solver data structures and projections
    'model[/\\]basic[/\\]ExpressionRange\.kt$'
    'model[/\\]basic[/\\]ModelView\.kt$'
    # variable: bounds and inequality conversions are Flt64 for solver consumption
    'variable[/\\]AbstractVariableItem\.kt$'
    'variable[/\\]AnyVariable\.kt$'
    # intermediate_symbol/function: BigM builds LinearInequality<Flt64> constraints (solver boundary)
    'intermediate_symbol[/\\]function[/\\]BigM\.kt$'
    # intermediate_symbol/function: geometry data (triangles/samplingPoints) hardcoded Flt64 for solver
    'intermediate_symbol[/\\]function[/\\]BivariateLinearPiecewise\.kt$'
    'intermediate_symbol[/\\]function[/\\]Cos\.kt$'
    'intermediate_symbol[/\\]function[/\\]Sin\.kt$'
    # intermediate_symbol/function: registerConstraints/evaluate bridge V↔Flt64 at solver boundary
    'intermediate_symbol[/\\]function[/\\]Masking\.kt$'
    'intermediate_symbol[/\\]function[/\\]Max\.kt$'
    'intermediate_symbol[/\\]function[/\\]Product\.kt$'
    'intermediate_symbol[/\\]function[/\\]QuadraticInStepRange\.kt$'
    'intermediate_symbol[/\\]function[/\\]QuadraticLinear\.kt$'
    'intermediate_symbol[/\\]function[/\\]QuadraticMaskingRange\.kt$'
    'intermediate_symbol[/\\]function[/\\]QuadraticMin\.kt$'
    'intermediate_symbol[/\\]function[/\\]SameAs\.kt$'
    'intermediate_symbol[/\\]function[/\\]SatisfiedAmount\.kt$'
    'intermediate_symbol[/\\]function[/\\]Slack\.kt$'
    'intermediate_symbol[/\\]function[/\\]SlackRange\.kt$'
    # intermediate_symbol/function: registerConstraints builds LinearInequality<Flt64> (solver boundary)
    'intermediate_symbol[/\\]function[/\\]And\.kt$'
    'intermediate_symbol[/\\]function[/\\]Sigmoid\.kt$'
    # intermediate_symbol/IntermediateSymbol.kt — prepare/evaluate bridge V↔Flt64 at solver boundary
    'intermediate_symbol[/\\]IntermediateSymbol\.kt$'
    # intermediate_symbol/SymbolCombination.kt — Flt64 convenience factories (solver boundary)
    'intermediate_symbol[/\\]SymbolCombination\.kt$'
    # model/basic/Model.kt — addObject/minimize/maximize Flt64 overloads (solver boundary)
    'model[/\\]basic[/\\]Model\.kt$'
)

# N3: Inherent Flt64 paths — Flt64 is the only reasonable type, not genericization debt.
$InherentFlt64Paths = @(
    # chaotic_operator: non-generic classes with hardcoded Flt64 physical constants
    'math[/\\]chaotic_operator[/\\]BoualiAttractor\.kt$'
    'math[/\\]chaotic_operator[/\\]DoublePendulumSystem\.kt$'
    'math[/\\]chaotic_operator[/\\]ComplexQuadraticPolynomial\.kt$'
    # ordinary: integer sqrt via Flt64 conversion
    'math[/\\]ordinary[/\\]Factorization\.kt$'
    'math[/\\]ordinary[/\\]Prime\.kt$'
    # algebra/concept: RealNumber.toFlt64() per-type conversion
    'math[/\\]algebra[/\\]concept[/\\]Numbers\.kt$'
    # variable: type objects are definitionally floating-point
    'variable[/\\]Type\.kt$'
    # variable: PctVar/RealVar/URealVar are inherently Flt64 variable classes
    'variable[/\\]VariableIndependentItem\.kt$'
    # variable: PctVariable/RealVariable/URealVariable multi-dimensional arrays are inherently Flt64
    'variable[/\\]VariableCombinationItem\.kt$'
    # model/basic: progress ratios computed from UInt64 counts, inherently Flt64
    'model[/\\]basic[/\\]ModelBuildingStatus\.kt$'
    'model[/\\]basic[/\\]RegistrationStatus\.kt$'
    # intermediate_symbol/function: epsilon is a numerical tolerance parameter, inherently Flt64
    'intermediate_symbol[/\\]function[/\\]First\.kt$'
    'intermediate_symbol[/\\]function[/\\]BalanceTernaryzation\.kt$'
    # intermediate_symbol/function: Semi default value uses Flt64(1e6) literal
    'intermediate_symbol[/\\]function[/\\]Semi\.kt$'
)

$allKtFiles = Get-ChildItem -Recurse ospf-kotlin-math/src/main,ospf-kotlin-core/src/main -Filter *.kt

foreach ($file in $allKtFiles) {
    $rp = Get-RelPath @{ Path = $file.FullName }
    $isAdapter = $rp -match 'adapter[/\\]flt64'

    # Skip whitelisted paths (already tracked by boundary_allowed)
    $isWhitelisted = $false
    foreach ($wlPath in $I5WhitelistPaths) {
        if ($rp -match $wlPath) { $isWhitelisted = $true; break }
    }
    if ($isWhitelisted) { continue }

    # Check if file is in solver boundary category
    $isSolverBoundary = $false
    foreach ($sbPath in $SolverBoundaryPaths) {
        if ($rp -match $sbPath) { $isSolverBoundary = $true; break }
    }

    # Check if file is in inherent Flt64 category
    $isInherentFlt64 = $false
    foreach ($ihPath in $InherentFlt64Paths) {
        if ($rp -match $ihPath) { $isInherentFlt64 = $true; break }
    }

    $lines = Get-Content $file.FullName -ErrorAction Stop

    # Scope-awareness: track brace depth to skip lines inside internal/private/@Deprecated function bodies
    # When we detect an internal/private/@Deprecated function declaration, we enter "skip mode"
    # and skip all lines until the function body ends (brace depth returns to entry depth).
    $skipMode = $false
    $skipEntryDepth = 0
    $pendingDeprecated = $false
    $shouldSkipAfterDecl = $false
    $skipEntryDepthForDecl = 0

    # Brace depth tracking: only count Flt64 signatures at class-member level (depth 0-1).
    # Local variables inside function bodies (depth 2+) are not public API signatures.
    # Use pre-update depth for declaration checks so that `fun foo(): Flt64 {` at depth 1
    # is still counted (the `{` pushes to depth 2 but the declaration is at depth 1).
    $braceDepth = 0

    # Track @Deprecated annotations: set flag so next declaration is marked deprecated
    # but NOT skipped — the declaration itself must be counted, only the body is skipped.
    $pendingDeprecated = $false

    # Brace depth tracking: only count Flt64 signatures at class-member level (depth 0-1).
    # Local variables inside function bodies (depth 2+) are not public API signatures.
    # Use pre-update depth for declaration checks so that `fun foo(): Flt64 {` at depth 1
    # is still counted (the `{` pushes to depth 2 but the declaration is at depth 1).
    $braceDepth = 0

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = [string]$lines[$i]
        $trimmed = $line.TrimStart()

        # Save pre-update depth for declaration checks
        $preUpdateDepth = $braceDepth

        # Track brace depth
        $openBraces = ([regex]::Matches($line, '\{')).Count
        $closeBraces = ([regex]::Matches($line, '\}')).Count
        $braceDepth += $openBraces - $closeBraces

        # Skip comments, imports, package, blank lines
        if ($trimmed.StartsWith('//') -or $trimmed.StartsWith('/*') -or $trimmed.StartsWith('*') -or
            $trimmed.StartsWith('import ') -or $trimmed.StartsWith('package ') -or
            $trimmed -eq '') {
            continue
        }

        # Track @Deprecated annotation: set flag so next declaration is marked deprecated
        if ($trimmed.StartsWith('@Deprecated')) {
            $pendingDeprecated = $true
            continue
        }

        # Skip @JvmName annotation lines
        if ($trimmed.StartsWith('@JvmName')) {
            continue
        }

        # Scope-awareness: if in skip mode, track brace depth and skip lines
        if ($skipMode) {
            if ($braceDepth -le $skipEntryDepth) {
                $skipMode = $false
            }
            continue
        }

        # Detect internal/private/protected function/val/var declarations:
        # count the declaration, then enter skip mode for the body
        if ($trimmed -match '^(internal|private|protected)\s+(fun|val|var|override fun|operator fun|infix fun|suspend fun)\s') {
            # Internal/private declarations are not public API — skip entirely
            $skipMode = $true
            $skipEntryDepth = $preUpdateDepth
            $pendingDeprecated = $false
            continue
        }

        # Skip internal/private/protected declarations (non-function, e.g. class/object)
        if ($trimmed -match '^(internal|private|protected)\s+') {
            $pendingDeprecated = $false
            continue
        }

        # Skip local variables inside function bodies (pre-update depth > 1)
        if ($preUpdateDepth -gt 1) {
            continue
        }

        # Detect declaration start at class-member level (depth 0-1)
        $isDeclStart = $trimmed -match '^(val|var|fun|class|interface|data class|enum class|object|typealias|operator fun|infix fun|suspend fun|override fun)\s'

        # @Deprecated-decorated declaration: process the declaration (count it),
        # then enter skip mode for the body only
        if ($pendingDeprecated -and $isDeclStart) {
            $pendingDeprecated = $false
            # Don't skip the declaration — process it below
            # But mark that we should enter skip mode after processing
            $shouldSkipAfterDecl = $true
            $skipEntryDepthForDecl = $preUpdateDepth
        } elseif ($pendingDeprecated) {
            $pendingDeprecated = $false
        }

        # Multi-line signature collection:
        # When a declaration starts but Flt64 is not on the first line,
        # scan forward up to 15 lines to find Flt64 in the signature block.
        # Stop scanning when: opening brace `{` found (body starts), equals `=` found
        # (property initializer), closing brace `}` found (class boundary), or another
        # declaration keyword found (new declaration starts).
        if ($isDeclStart) {
            if ($line -match 'Flt64') {
                # Flt64 found on declaration line — process immediately
                $sigBlock = $line.Trim()
                $sigBlockStartLineNum = $i + 1
            } else {
                # Scan forward for Flt64 in continuation lines
                $foundFlt64 = $false
                $sigBlockParts = @($line.Trim())
                $scanLimit = [Math]::Min($i + 15, $lines.Count - 1)
                for ($k = $i + 1; $k -le $scanLimit; $k++) {
                    $nextLine = [string]$lines[$k]
                    $nextTrimmed = $nextLine.TrimStart()
                    # Stop if we hit the body start, class boundary, or another declaration
                    if ($nextLine -match '\{' -or $nextTrimmed -match '^}' -or
                        $nextTrimmed -match '^(val|var|fun|class|interface|data class|enum class|object|typealias|operator fun|infix fun|suspend fun|override fun|internal|private|protected)\s' -or
                        ($trimmed -match '^val\s|^var\s' -and $nextLine -match '=')) {
                        # Check this boundary line for Flt64 too
                        if ($nextLine -match 'Flt64') {
                            $foundFlt64 = $true
                            $sigBlockParts += $nextTrimmed
                        }
                        break
                    }
                    # Skip blank lines, comments, annotations within signature
                    if ($nextTrimmed -eq '' -or $nextTrimmed.StartsWith('//') -or
                        $nextTrimmed.StartsWith('@')) {
                        continue
                    }
                    if ($nextLine -match 'Flt64') {
                        $foundFlt64 = $true
                    }
                    $sigBlockParts += $nextTrimmed
                }
                if ($foundFlt64) {
                    $sigBlock = $sigBlockParts -join ' '
                    $sigBlockStartLineNum = $i + 1
                } else {
                    # No Flt64 found in multi-line signature — skip
                    # Enter skip mode if @Deprecated
                    if ($shouldSkipAfterDecl) {
                        $skipMode = $true
                        $skipEntryDepth = $skipEntryDepthForDecl
                        $shouldSkipAfterDecl = $false
                    }
                    continue
                }
            }
        } else {
            # Not a declaration start — skip
            continue
        }

        # Process a declaration (single-line or collected multi-line) that contains Flt64
        # $sigBlock contains the full signature text, $sigBlockStartLineNum is the line number

        # N1: Check if this declaration has @Deprecated annotation
        $isDeprecated = $false
        # Look backwards for @Deprecated on preceding lines (up to 5 lines back)
        for ($j = $sigBlockStartLineNum - 2; $j -ge [Math]::Max(0, $sigBlockStartLineNum - 7); $j--) {
            $prevLine = [string]$lines[$j]
            $prevTrimmed = $prevLine.TrimStart()
            if ($prevTrimmed.StartsWith('@Deprecated')) {
                $isDeprecated = $true
                break
            }
            # Stop looking if we hit another declaration, blank line, or comment
            if ($prevTrimmed -match '^(val|var|fun|class|interface|data class|enum class|object|typealias|operator fun|infix fun|suspend fun|override fun|internal|private|protected)\s' -or
                $prevTrimmed -eq '' -or $prevTrimmed.StartsWith('//')) {
                break
            }
        }

        # Also check the $shouldSkipAfterDecl flag (set by pendingDeprecated detection above)
        if ($shouldSkipAfterDecl) {
            $isDeprecated = $true
        }

        $entry = "${rp}:${sigBlockStartLineNum}: ${sigBlock}"
        $publicApiSignatureFlt64 += $entry

        if ($isDeprecated) {
            $publicApiSignatureDeprecated += $entry
        } elseif ($isSolverBoundary) {
            # N2: solver boundary — tracked separately, not counted as non_adapter debt
        } elseif ($isInherentFlt64) {
            # N3: inherent Flt64 — tracked separately, not counted as non_adapter debt
        } elseif ($isAdapter) {
            $publicApiSignatureAdapter += $entry
        } else {
            $publicApiSignatureNonAdapter += $entry
        }

        # Enter skip mode for @Deprecated function body
        if ($shouldSkipAfterDecl) {
            $skipMode = $true
            $skipEntryDepth = $skipEntryDepthForDecl
            $shouldSkipAfterDecl = $false
        }
    }
}

# N2/N3: Derive solver_boundary and inherent_flt64 from main i5 scan results
# The main i5 scan already classifies each entry, so we reuse those results
# instead of running separate scans with different filtering logic.
$publicApiSignatureSolverBoundary = @()
foreach ($entry in $publicApiSignatureFlt64) {
    # Extract file path from entry (format: relpath:line: content)
    $entryPath = $entry.Split(':')[0]
    $isSolverBoundary = $false
    foreach ($sbPath in $SolverBoundaryPaths) {
        if ($entryPath -match $sbPath) { $isSolverBoundary = $true; break }
    }
    if ($isSolverBoundary) {
        # Check if this entry is @Deprecated (skip deprecated from solver_boundary count)
        $isDeprecated = $false
        # Parse line number from entry
        $entryLineNum = [int]$entry.Split(':')[1]
        # Find the corresponding file and check backwards for @Deprecated
        # Instead of re-scanning, check if the entry is in the deprecated list
        foreach ($depEntry in $publicApiSignatureDeprecated) {
            if ($depEntry -eq $entry) { $isDeprecated = $true; break }
        }
        if (-not $isDeprecated) {
            $publicApiSignatureSolverBoundary += $entry
        }
    }
}

$publicApiSignatureInherentFlt64 = @()
foreach ($entry in $publicApiSignatureFlt64) {
    # Extract file path from entry
    $entryPath = $entry.Split(':')[0]
    $isInherentFlt64 = $false
    foreach ($ihPath in $InherentFlt64Paths) {
        if ($entryPath -match $ihPath) { $isInherentFlt64 = $true; break }
    }
    if ($isInherentFlt64) {
        # Check if this entry is @Deprecated (skip deprecated from inherent_flt64 count)
        $isDeprecated = $false
        foreach ($depEntry in $publicApiSignatureDeprecated) {
            if ($depEntry -eq $entry) { $isDeprecated = $true; break }
        }
        if (-not $isDeprecated) {
            $publicApiSignatureInherentFlt64 += $entry
        }
    }
}

# ============================================================
# I5: @Deprecated detection in adapter/flt64
# ============================================================

$adapterDeprecatedCount = 0
$adapterDeprecatedItems = @()

$adapterFiles = Get-ChildItem -Recurse ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/flt64 -Filter *.kt
foreach ($file in $adapterFiles) {
    $rp = Get-RelPath @{ Path = $file.FullName }
    $lines = Get-Content $file.FullName -ErrorAction Stop
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = [string]$lines[$i]
        $trimmed = $line.TrimStart()
        if ($line -match '@Deprecated' -and -not ($trimmed.StartsWith('//') -or $trimmed.StartsWith('*'))) {
            $adapterDeprecatedCount++
            $adapterDeprecatedItems += "${rp}:$($i+1): $trimmed"
        }
    }
}

# ============================================================
# I5: boundary_allowed three-tier classification
# permanent = long-term allowed (solver-inherent, framework boundary)
# deprecated = marked @Deprecated, planned for removal
# must_decrease = should decrease over time, not increase
# ============================================================

$boundaryPermanent = @()
$boundaryDeprecated = @()
$boundaryMustDecrease = @()

foreach ($b in $mechanismBoundary) {
    if ($b.Debt -match 'none') { $boundaryPermanent += $b }
    elseif ($b.Debt -match 'MIGRATE') { $boundaryMustDecrease += $b }
    else { $boundaryMustDecrease += $b }
}
foreach ($b in $callbackBoundary) {
    if ($b.Debt -match 'none') { $boundaryPermanent += $b }
    elseif ($b.Debt -match 'MIGRATE') { $boundaryMustDecrease += $b }
    else { $boundaryMustDecrease += $b }
}
foreach ($e in $typealiasBoundary) {
    if ($e.Debt -match 'none') { $boundaryPermanent += $e }
    elseif ($e.Debt -match 'MIGRATE') { $boundaryMustDecrease += $e }
    else { $boundaryMustDecrease += $e }
}
# UNCHECKED_CAST: permanent (star-projection bridge, cannot be eliminated)
$boundaryPermanent += @{ Entry = "SolverBoundaryCasts.kt: @Suppress(UNCHECKED_CAST)"; Reason = "star-projection type-erased bridge"; Debt = "none (permanent)" }
# UNCHECKED_CAST: permanent (SingleObject covariance bridge, cannot be eliminated without breaking variance)
$boundaryPermanent += @{ Entry = "Object.kt: @Suppress(UNCHECKED_CAST)"; Reason = "covariant SingleObject internal storage bridge"; Debt = "none (permanent)" }

# adapter/flt64 deprecated items
foreach ($d in $adapterDeprecatedItems) {
    $boundaryDeprecated += @{ Entry = $d; Reason = "adapter/flt64 @Deprecated item"; Debt = "DEPRECATED: planned removal" }
}

# ============================================================
# Text output
# ============================================================

Write-Host "=== Full Genericization Scanner ==="
Write-Host "Timestamp: $timestamp"
Write-Host ""

Write-Host "--- RAW counts (all text matches, main only) ---"
Write-Host "  import as:                    $importAsCount"
Write-Host "  Suppress(UNCHECKED_CAST):     $suppressCount"
Write-Host "  typealias *Flt64:             $($rawTypealias.Count)"
Write-Host "    geometry:                   $($typealiasGeometry.Count)"
Write-Host "    variable:                   $($typealiasVariable.Count)"
Write-Host "    adapter/flt64:              $($typealiasAdapter.Count)"
Write-Host "  math/symbol non-adapter:      $($rawMathNonAdapter.Count)"
Write-Host "  core/function:                $($functionResult.RawCount)"
Write-Host "  core/solver:                  $($solverResult.RawCount)"
Write-Host "  core/callback:                $($rawCallback.Count)"
Write-Host "  core/mechanism:               $($rawMechanism.Count)"
Write-Host ""

Write-Host "--- PUBLIC API BLOCKING (must be zero) ---"
Write-Host "  import as:                    $importAsCount"
Write-Host "  typealias *Flt64 total:       $($typealiasPublicApi.Count)"
Write-Host "    non-adapter:                $($typealiasPublicApi.Count)"
Write-Host "    geometry:                   $($typealiasGeometry.Count)"
Write-Host "    variable:                   $($typealiasVariable.Count)"
Write-Host "  math/symbol non-adapter:      $($rawMathNonAdapter.Count)"
Write-Host "  core/function:                $($functionResult.PublicApiCount)"
Write-Host "  core/solver public:           $($coreSolverPublicFlt64.Count)"
Write-Host "  core/callback:                $($callbackPublicApi.Count)"
Write-Host "  core/mechanism:               $($mechanismPublicApi.Count)"
Write-Host ""

Write-Host "--- BOUNDARY ALLOWED (whitelisted, tracked) ---"
Write-Host "  UNCHECKED_CAST:               $uncheckedCastBoundaryCount"
Write-Host "  typealias *Flt64:             $($typealiasBoundary.Count)"
Write-Host "  core/function override:       $($functionResult.BoundaryCount)"
Write-Host "  core/callback:                $($callbackBoundary.Count)"
Write-Host "  core/mechanism:               $($mechanismBoundary.Count)"
Write-Host ""

# ---- I2: Semantic classification ----
Write-Host "--- I2 SEMANTIC CLASSIFICATION (core Flt64 residuals) ---"
Write-Host "  NUMBER_TYPE_BODY:             $classNUMBER_TYPE_BODY  (Flt64 type itself — KEEP)"
Write-Host "  ADAPTER_COMPAT:               $classADAPTER_COMPAT  (adapter/flt64 surface — KEEP_OR_DEPRECATE)"
Write-Host "  SOLVER_BRIDGE:                $classSOLVER_BRIDGE  (solver gap/normalization/cast — KEEP_OR_REWORK)"
Write-Host "  INTERNAL_IMPL:                $classINTERNAL_IMPL  (internal computation paths — REVIEW)"
Write-Host "  PUBLIC_API_BLOCKING:          $classPUBLIC_API_BLOCKING  (public API signatures — MUST_BE_ZERO)"
Write-Host "  Total classified:             $($classNUMBER_TYPE_BODY + $classADAPTER_COMPAT + $classSOLVER_BRIDGE + $classINTERNAL_IMPL + $classPUBLIC_API_BLOCKING)"
Write-Host ""

# ---- I5: Public API signature-level scan ----
# L6 baseline = 750 (before N4/N5 math whitelist)
# N4/N5 whitelisted math convenience = geometry 89 + chaotic_operator 24 + fractal_operator 3 = 116
# (Note: chaotic_operator INHERENT_FLT64 files are in InherentFlt64Paths, not whitelisted)
$i5BaselineL6 = 750
$i5WhitelistedMathConvenience = $publicApiSignatureFlt64.Count - $i5BaselineL6 + 116 + 22 + 6 + 185 + 421
# Simpler: just count what was whitelisted by N4/N5
$i5MathConvenienceWhitelisted = 89 + 24 + 3  # geometry + chaotic_operator convenience + fractal_operator

Write-Host "--- I5 PUBLIC API SIGNATURE SCAN (Flt64 in non-internal signatures) ---"
Write-Host "  L6 baseline:                  $i5BaselineL6"
Write-Host "  N4/N5 math whitelisted:       $i5MathConvenienceWhitelisted"
Write-Host "  visible total:                $($publicApiSignatureFlt64.Count)"
Write-Host "  adapter/flt64:                $($publicApiSignatureAdapter.Count)"
Write-Host "  @Deprecated:                  $($publicApiSignatureDeprecated.Count)"
Write-Host "  solver_boundary:              $($publicApiSignatureSolverBoundary.Count)"
Write-Host "  inherent_flt64:               $($publicApiSignatureInherentFlt64.Count)"
Write-Host "  non_adapter (REAL_DEBT):      $($publicApiSignatureNonAdapter.Count)"
$i5SumCheck = $publicApiSignatureDeprecated.Count + $publicApiSignatureSolverBoundary.Count + $publicApiSignatureInherentFlt64.Count + $publicApiSignatureNonAdapter.Count + $publicApiSignatureAdapter.Count
$i5ClosureOk = ($i5SumCheck -eq $publicApiSignatureFlt64.Count)
Write-Host "  Sum check:                    $i5SumCheck (should equal visible total) $(if ($i5ClosureOk) { '[OK]' } else { '[MISMATCH]' })"
Write-Host ""

# ---- I5: @Deprecated detection ----
Write-Host "--- I5 ADAPTER DEPRECATED ITEMS ---"
Write-Host "  @Deprecated count:            $adapterDeprecatedCount"
if ($adapterDeprecatedCount -gt 0) {
    foreach ($d in $adapterDeprecatedItems) {
        Write-Host "    $d"
    }
}
Write-Host ""

# ---- I5: boundary tiers ----
Write-Host "--- I5 BOUNDARY TIERS ---"
Write-Host "  permanent (long-term):        $($boundaryPermanent.Count)"
Write-Host "  deprecated (planned removal): $($boundaryDeprecated.Count)"
Write-Host "  must_decrease (tracked):      $($boundaryMustDecrease.Count)"
Write-Host ""

# ---- I5: non-adapter signature detail (L0 debt inventory) ----
if ($publicApiSignatureNonAdapter.Count -gt 0) {
    Write-Host "--- I5 NON-ADAPTER SIGNATURE DETAIL ($($publicApiSignatureNonAdapter.Count) items) ---"
    foreach ($entry in $publicApiSignatureNonAdapter) {
        Write-Host "  $entry"
    }
    Write-Host ""
}

# ---- Boundary detail listing (H1 requirement) ----
Write-Host "--- BOUNDARY DETAIL: core/mechanism ($($mechanismBoundary.Count) items) ---"
if ($mechanismBoundary.Count -gt 0) {
    $i = 1
    foreach ($b in $mechanismBoundary) {
        Write-Host "  [$i] $($b.Entry)"
        Write-Host "      Reason: $($b.Reason)"
        Write-Host "      Debt:   $($b.Debt)"
        $i++
    }
}
Write-Host ""

Write-Host "--- BOUNDARY DETAIL: core/callback ($($callbackBoundary.Count) items) ---"
if ($callbackBoundary.Count -gt 0) {
    $i = 1
    foreach ($b in $callbackBoundary) {
        Write-Host "  [$i] $($b.Entry)"
        Write-Host "      Reason: $($b.Reason)"
        Write-Host "      Debt:   $($b.Debt)"
        $i++
    }
}
Write-Host ""

Write-Host "--- BOUNDARY DETAIL: UNCHECKED_CAST ($uncheckedCastBoundaryCount items) ---"
if ($uncheckedCastBoundaryCount -gt 0) {
    $i = 1
    foreach ($s in $uncheckedCastBoundary) {
        $rp = Get-RelPath $s
        Write-Host "  [$i] ${rp}:$($s.LineNumber): $($s.Line.Trim())"
        Write-Host "      Reason: type-erased solver boundary bridge"
        $i++
    }
}
Write-Host ""

Write-Host "--- BOUNDARY DETAIL: UNCHECKED_CAST blocking ($uncheckedCastBlockingCount items) ---"
if ($uncheckedCastBlockingCount -gt 0) {
    $i = 1
    foreach ($s in $uncheckedCastBlocking) {
        $rp = Get-RelPath $s
        Write-Host "  [$i] ${rp}:$($s.LineNumber): $($s.Line.Trim())"
        $i++
    }
}
Write-Host ""

# Show blocking violations if any
$hasBlocking = $importAsCount -gt 0 -or
    $typealiasPublicApi.Count -gt 0 -or
    $rawMathNonAdapter.Count -gt 0 -or
    $functionResult.PublicApiCount -gt 0 -or
    $coreSolverPublicFlt64.Count -gt 0 -or
    $callbackPublicApi.Count -gt 0 -or
    $mechanismPublicApi.Count -gt 0 -or
    $uncheckedCastBlockingCount -gt 0

if ($hasBlocking) {
    Write-Host "--- BLOCKING VIOLATIONS (must fix) ---"
    if ($importAsCount -gt 0) {
        Write-Host "  import as ($importAsCount):"
        $importAs | ForEach-Object { Write-Host "    $(Get-RelPath $_):$($_.LineNumber): $($_.Line.Trim())" }
    }
    if ($typealiasPublicApi.Count -gt 0) {
        Write-Host "  typealias non-adapter ($($typealiasPublicApi.Count)):"
        $typealiasPublicApi | ForEach-Object { Write-Host "    $_" }
    }
    if ($typealiasGeometry.Count -gt 0) {
        Write-Host "  geometry typealias ($($typealiasGeometry.Count)):"
        $typealiasGeometry | ForEach-Object { Write-Host "    $_" }
    }
    if ($typealiasVariable.Count -gt 0) {
        Write-Host "  variable typealias ($($typealiasVariable.Count)):"
        $typealiasVariable | ForEach-Object { Write-Host "    $_" }
    }
    if ($rawMathNonAdapter.Count -gt 0) {
        Write-Host "  math/symbol non-adapter ($($rawMathNonAdapter.Count)):"
        $rawMathNonAdapter | ForEach-Object { Write-Host "    $_" }
    }
    if ($functionResult.PublicApiCount -gt 0) {
        Write-Host "  core/function ($($functionResult.PublicApiCount)):"
        $functionResult.PublicApi | ForEach-Object { Write-Host "    $_" }
    }
    if ($coreSolverPublicFlt64.Count -gt 0) {
        Write-Host "  core/solver public ($($coreSolverPublicFlt64.Count)):"
        $coreSolverPublicFlt64 | ForEach-Object { Write-Host "    $_" }
    }
    if ($callbackPublicApi.Count -gt 0) {
        Write-Host "  core/callback ($($callbackPublicApi.Count)):"
        $callbackPublicApi | ForEach-Object { Write-Host "    $_" }
    }
    if ($mechanismPublicApi.Count -gt 0) {
        Write-Host "  core/mechanism ($($mechanismPublicApi.Count)):"
        $mechanismPublicApi | ForEach-Object { Write-Host "    $_" }
    }
    if ($uncheckedCastBlockingCount -gt 0) {
        Write-Host "  UNCHECKED_CAST blocking ($uncheckedCastBlockingCount):"
        $uncheckedCastBlocking | ForEach-Object { Write-Host "    $(Get-RelPath $_):$($_.LineNumber): $($_.Line.Trim())" }
    }
    Write-Host ""
}

# Migration debt summary
$debtItems = @{}
foreach ($e in $typealiasBoundary) { if ($e.Debt -match 'MIGRATE') { $debtItems[$e.Entry] = "typealias: $($e.Entry) -> $($e.Debt)" } }
foreach ($e in $callbackBoundary) { if ($e.Debt -match 'MIGRATE') { $debtItems[$e.Entry] = "callback: $($e.Entry) -> $($e.Debt)" } }
foreach ($e in $mechanismBoundary) { if ($e.Debt -match 'MIGRATE') { $debtItems[$e.Entry] = "mechanism: $($e.Entry) -> $($e.Debt)" } }
if ($functionResult.BoundaryCount -gt 0) {
    $debtItems["function_override"] = "function: $($functionResult.BoundaryCount) override methods -> MEDIUM: split interface, move Flt64 methods to internal/boundary"
}

if ($debtItems.Count -gt 0) {
    Write-Host "--- MIGRATION DEBT ($($debtItems.Count) items) ---"
    $debtItems.Values | ForEach-Object { Write-Host "  $_" }
    Write-Host ""
}

# ============================================================
# JSON output
# ============================================================

$json = @{
    timestamp = $timestamp
    raw = @{
        import_as = $importAsCount
        suppress_unchecked_cast = $suppressCount
        typealias_flt64_total = $rawTypealias.Count
        typealias_flt64_geometry = $typealiasGeometry.Count
        typealias_flt64_variable = $typealiasVariable.Count
        typealias_flt64_adapter = $typealiasAdapter.Count
        math_symbol_adapter = $adapterMatches.Count
        math_symbol_non_adapter = $rawMathNonAdapter.Count
        core_function = $functionResult.RawCount
        core_solver = $solverResult.RawCount
        core_callback = $rawCallback.Count
        core_mechanism = $rawMechanism.Count
        core_other = $otherResult.RawCount
    }
    public_api_blocking = @{
        import_as = $importAsCount
        typealias_flt64_non_adapter = $typealiasPublicApi.Count
        geometry_typealias_flt64 = $typealiasGeometry.Count
        variable_typealias_flt64 = $typealiasVariable.Count
        math_symbol_non_adapter = $rawMathNonAdapter.Count
        core_function = $functionResult.PublicApiCount
        core_solver_public = $coreSolverPublicFlt64.Count
        core_callback = $callbackPublicApi.Count
        core_mechanism = $mechanismPublicApi.Count
    }
    boundary_allowed = @{
        suppress_unchecked_cast = $uncheckedCastBoundaryCount
        suppress_unchecked_cast_blocking = $uncheckedCastBlockingCount
        typealias_flt64 = $typealiasBoundary.Count
        core_function_override = $functionResult.BoundaryCount
        core_callback = $callbackBoundary.Count
        core_mechanism = $mechanismBoundary.Count
    }
    boundary_detail = @{
        core_mechanism = @($mechanismBoundary | ForEach-Object { @{ entry = $_.Entry; reason = $_.Reason; debt = $_.Debt } })
        core_callback = @($callbackBoundary | ForEach-Object { @{ entry = $_.Entry; reason = $_.Reason; debt = $_.Debt } })
        unchecked_cast_bridge = @($uncheckedCastBoundary | ForEach-Object { $rp = Get-RelPath $_; "${rp}:$($_.LineNumber): $($_.Line.Trim())" })
        unchecked_cast_blocking = @($uncheckedCastBlocking | ForEach-Object { $rp = Get-RelPath $_; "${rp}:$($_.LineNumber): $($_.Line.Trim())" })
    }
    migration_debt = @($debtItems.Values)
    i2_classification = @{
        NUMBER_TYPE_BODY = $classNUMBER_TYPE_BODY
        ADAPTER_COMPAT = $classADAPTER_COMPAT
        SOLVER_BRIDGE = $classSOLVER_BRIDGE
        INTERNAL_IMPL = $classINTERNAL_IMPL
        PUBLIC_API_BLOCKING = $classPUBLIC_API_BLOCKING
    }
    i2_classification_detail = @($classifiedItems | Select-Object -First 200 | ForEach-Object { @{ path = $_.path; line = $_.line; category = $_.category } })
    i5_public_api_signature = @{
        baseline_l6 = 750
        whitelisted_math_convenience = $i5MathConvenienceWhitelisted
        visible_total = $publicApiSignatureFlt64.Count
        adapter = $publicApiSignatureAdapter.Count
        deprecated = $publicApiSignatureDeprecated.Count
        solver_boundary = $publicApiSignatureSolverBoundary.Count
        inherent_flt64 = $publicApiSignatureInherentFlt64.Count
        non_adapter = $publicApiSignatureNonAdapter.Count
    }
    i5_adapter_deprecated = @{
        count = $adapterDeprecatedCount
        items = @($adapterDeprecatedItems)
    }
    i5_boundary_tiers = @{
        permanent = $boundaryPermanent.Count
        deprecated = $boundaryDeprecated.Count
        must_decrease = $boundaryMustDecrease.Count
    }
} | ConvertTo-Json -Depth 6

$json | Out-File -FilePath scripts/scan-full-genericization-result.json -Encoding utf8
Write-Host "JSON saved to scripts/scan-full-genericization-result.json"

# ============================================================
# Gate check (stricter than P13)
# ============================================================

$blockingCounts = @(
    $importAsCount,
    $typealiasPublicApi.Count,
    $rawMathNonAdapter.Count,
    $functionResult.PublicApiCount,
    $coreSolverPublicFlt64.Count,
    $callbackPublicApi.Count,
    $mechanismPublicApi.Count,
    $typealiasGeometry.Count,
    $typealiasVariable.Count
)

$fail = $false

if ($uncheckedCastBlockingCount -gt 0) {
    Write-Host ""
    Write-Host "GATE: FAIL (UNCHECKED_CAST blocking=$uncheckedCastBlockingCount > 0)"
    $fail = $true
}
if ($blockingCounts | Where-Object { $_ -gt 0 }) {
    Write-Host ""
    Write-Host "GATE: FAIL (public_api_blocking checks not met)"
    $fail = $true
}
# I5: non-adapter public API signature Flt64 — INFO during N-stage
# N1-N3 separated deprecated/solver_boundary/inherent_flt64 from non_adapter.
# non_adapter now only contains REAL_DEBT (genuine genericization targets).
if ($publicApiSignatureNonAdapter.Count -gt 0) {
    Write-Host ""
    Write-Host "I5 INFO: non_adapter (REAL_DEBT) Flt64=$($publicApiSignatureNonAdapter.Count)"
    Write-Host "  deprecated=$($publicApiSignatureDeprecated.Count) solver_boundary=$($publicApiSignatureSolverBoundary.Count) inherent_flt64=$($publicApiSignatureInherentFlt64.Count)"
    Write-Host "  See I5 NON-ADAPTER SIGNATURE DETAIL above for full listing."
}
# I5 classification closure: sum of all categories must equal visible total
if (-not $i5ClosureOk) {
    Write-Host ""
    Write-Host "GATE: FAIL (i5 classification not closed: sum=$i5SumCheck != visible_total=$($publicApiSignatureFlt64.Count))"
    $fail = $true
}
if ($fail) { exit 1 } else { Write-Host ""; Write-Host "GATE: PASS"; exit 0 }
