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
    @{ File = 'MetaModel.kt'; Pattern = 'fun.*add.*Quantity.*IntermediateSymbol.*Flt64'; Reason = 'Flt64 convenience add overload (solver boundary)'; Debt = 'none (solver-inherent)' }
    @{ File = 'MetaModel.kt'; Pattern = 'fun.*setSolverSolution';       Reason = 'solver solution ingestion'; Debt = 'none (solver-inherent)' }
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
    'SolverBoundaryCasts.kt'
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
        typealias_flt64 = $typealiasBoundary.Count
        core_function_override = $functionResult.BoundaryCount
        core_callback = $callbackBoundary.Count
        core_mechanism = $mechanismBoundary.Count
    }
    migration_debt = @($debtItems.Values)
} | ConvertTo-Json -Depth 5

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
if ($fail) { exit 1 } else { Write-Host ""; Write-Host "GATE: PASS"; exit 0 }
