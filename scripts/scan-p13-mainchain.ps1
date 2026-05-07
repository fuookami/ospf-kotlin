# P13 Mainchain Flt64 Scanner (v3)
# Usage: pwsh -File scripts/scan-p13-mainchain.ps1 [-VerboseIgnored]
# Output: JSON + text summary
#
# Gate logic:
#   public_api_blocking = must be zero (genuine violations)
#   boundary_allowed    = whitelisted solver-boundary items (tracked, not blocking)
#
# Whitelist philosophy:
#   - Solver-boundary conversion functions: whitelist if name signals Flt64
#   - Interface contract overrides: whitelist with fixed file+method list
#   - Convenience typealias: whitelist with migration note
#   - User-facing DSL: NOT whitelisted, must migrate or V-typed

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
# Narrow rules: fixed path regex + explicit reason
# ============================================================

$TypealiasWhitelist = @(
    @{ Path = 'math[/\\]geometry';                Reason = 'geometry convenience alias'; Debt = 'none (stable)' }
    @{ Path = 'math[/\\]symbol[/\\]adapter[/\\]flt64'; Reason = 'Flt64 adapter boundary'; Debt = 'none (boundary)' }
    @{ Path = 'core[/\\]variable';                Reason = 'variable convenience alias'; Debt = 'low (stable)' }
    @{ Path = 'core[/\\]intermediate_symbol[/\\]IntermediateSymbol.kt'; Reason = 'Quantity* convenience alias'; Debt = 'low (stable)' }
    @{ Path = 'core[/\\]model[/\\]basic[/\\]MultiObject.kt';  Reason = 'MulObj solver boundary'; Debt = 'low' }
    @{ Path = 'core[/\\]model[/\\]basic[/\\]ModelView.kt';    Reason = 'OriginConstraint alias'; Debt = 'low' }
    @{ Path = 'core[/\\]model[/\\]intermediate';  Reason = 'Cell/LinearTriad/QuadraticTetrad solver-boundary alias'; Debt = 'low' }
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
# Fixed 7 files + fixed method names = interface contract override
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
# Very narrow: fixed file + fixed function/pattern
# ============================================================

$MechanismWhitelist = @(
    # Constraint.kt: toMeta() conversion (ConstraintFlt64/DualSolution handled by typealias whitelist)
    @{ File = 'Constraint.kt'; Pattern = 'fun.*\.toMeta\(\)';           Reason = 'solver output conversion'; Debt = 'low' }
    # Solver-boundary conversion (name signals Flt64)
    @{ File = 'MechanismModel.kt'; Pattern = 'fun.*convertMechanismModelToFlt64'; Reason = 'explicit Flt64 conversion'; Debt = 'low (boundary)' }
    @{ File = 'MetaModel.kt'; Pattern = 'fun.*setSolverSolution';       Reason = 'solver solution ingestion'; Debt = 'low (boundary)' }
    # Flatten data conversion (name signals Flt64)
    @{ File = 'MathInequalityFlatten.kt'; Pattern = 'fun.*toLinearFlattenDataFlt64'; Reason = 'explicit Flt64 conversion'; Debt = 'low (boundary)' }
    @{ File = 'MathInequalityFlatten.kt'; Pattern = 'fun.*toQuadraticFlattenDataFlt64'; Reason = 'explicit Flt64 conversion'; Debt = 'low (boundary)' }
    @{ File = 'MathInequalityFlatten.kt'; Pattern = 'fun.*toFrontendPolynomial'; Reason = 'Flt64 polynomial adapter'; Debt = 'low (boundary)' }
    @{ File = 'MathInequalityFlatten.kt'; Pattern = 'fun.*toFlattenData\(\)'; Reason = 'Flt64 flatten adapter'; Debt = 'low (boundary)' }
    @{ File = 'MathInequalityFlatten.kt'; Pattern = 'fun.*from\(mono:.*Flt64'; Reason = 'Flt64 factory adapter'; Debt = 'low (boundary)' }
    # LinearInequality<Flt64> conversion to quadratic constraint
    @{ File = 'MathInequalityDsl.kt'; Pattern = 'fun.*LinearInequality.*Flt64.*toQuadraticConstraint'; Reason = 'solver-boundary conversion'; Debt = 'low (boundary)' }
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
$mathRoot = "ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol"
$coreRoot = "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core"

# ---- A: import as (main only) ----
$importAs = Get-ChildItem -Recurse ospf-kotlin-math/src/main,ospf-kotlin-core/src/main -Filter *.kt |
    Select-String -Pattern 'import\s+\S+\s+as\s+'
$importAsCount = $importAs.Count

# ---- B: Suppress(UNCHECKED_CAST) (main only) ----
$suppress = Get-ChildItem -Recurse ospf-kotlin-math/src/main,ospf-kotlin-core/src/main -Filter *.kt |
    Select-String -Pattern 'Suppress.*UNCHECKED_CAST'
$suppressCount = $suppress.Count

# ---- C: typealias *Flt64 (main only, exclude src/test) ----
$typealiasAll = Get-ChildItem -Recurse ospf-kotlin-math/src/main,ospf-kotlin-core/src/main -Filter *.kt |
    Select-String -Pattern 'typealias\s+\w+.*Flt64'

$rawTypealias = @()
$typealiasIgnored = @()
$typealiasPublicApi = @()
$typealiasBoundary = @()

foreach ($m in $typealiasAll) {
    $rp = Get-RelPath $m
    $entry = "$rp`:$($m.LineNumber): $($m.Line.Trim())"
    $rawTypealias += $entry

    if (Is-CommentLine $m.Line) {
        $typealiasIgnored += @{ Entry = $entry; Reason = "comment line" }
        continue
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
$mathFiles = Get-ChildItem -Recurse $mathRoot -Filter *.kt
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

# E.2-3: solver
$solverMatches = $coreMatches | Where-Object { (Get-RelPath $_) -match 'solver[/\\]' }
$solverResult = Filter-PrivateIntoValue -Matches $solverMatches

$heuristicMatches = $coreMatches | Where-Object { (Get-RelPath $_) -match 'solver[/\\]heuristic' }
$heuristicResult = Filter-PrivateIntoValue -Matches $heuristicMatches

# E.4: callback (main only)
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
        # Check private IntoValue
        if ($m.Line -match 'intoValue|fromValue') {
            $callbackIgnored += @{ Entry = $entry; Reason = "IntoValue<Flt64> method" }
        } else {
            $callbackPublicApi += $entry
        }
    }
}

# E.5: mechanism (main only)
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

    # Check private IntoValue
    if ($m.Line -match 'private\s+val.*IntoValue|intoValue\(value: Flt64\)|fromValue\(value: Flt64\)') {
        $mechanismIgnored += @{ Entry = $entry; Reason = "private IntoValue<Flt64>" }
        continue
    }

    # Check mechanism whitelist
    $wl = Is-WhitelistedMechanism $rp $m.Line
    if ($wl) {
        $mechanismBoundary += @{ Entry = $entry; Reason = $wl.Reason; Debt = $wl.Debt }
        continue
    }

    $mechanismPublicApi += $entry
}

# E.6: core other
$knownPaths = @('intermediate_symbol[/\\]function', 'solver[/\\]', 'model[/\\]callback', 'model[/\\]mechanism')
$otherMatches = $coreMatches | Where-Object {
    $path = (Get-RelPath $_)
    -not ($knownPaths | Where-Object { $path -match $_ })
}
$otherResult = Filter-PrivateIntoValue -Matches $otherMatches

# ============================================================
# Text output
# ============================================================

Write-Host "=== P13 Mainchain Flt64 Scan (v3) ==="
Write-Host "Timestamp: $timestamp"
Write-Host ""

Write-Host "--- RAW counts (all text matches, main only) ---"
Write-Host "  import as:                    $importAsCount"
Write-Host "  Suppress(UNCHECKED_CAST):     $suppressCount"
Write-Host "  typealias *Flt64:             $($rawTypealias.Count)"
Write-Host "  math/symbol non-adapter:      $($rawMathNonAdapter.Count)"
Write-Host "  core/function:                $($functionResult.RawCount)"
Write-Host "  core/callback:                $($rawCallback.Count)"
Write-Host "  core/mechanism:               $($rawMechanism.Count)"
Write-Host ""

Write-Host "--- PUBLIC API BLOCKING (must be zero) ---"
Write-Host "  import as:                    $importAsCount"
Write-Host "  typealias *Flt64:             $($typealiasPublicApi.Count)"
Write-Host "  math/symbol non-adapter:      $($rawMathNonAdapter.Count)"
Write-Host "  core/function:                $($functionResult.PublicApiCount)"
Write-Host "  core/callback:                $($callbackPublicApi.Count)"
Write-Host "  core/mechanism:               $($mechanismPublicApi.Count)"
Write-Host ""

Write-Host "--- BOUNDARY ALLOWED (whitelisted, tracked) ---"
Write-Host "  UNCHECKED_CAST:               $suppressCount (max 4)"
Write-Host "  typealias *Flt64:             $($typealiasBoundary.Count)"
Write-Host "  core/function override:       $($functionResult.BoundaryCount)"
Write-Host "  core/callback:                $($callbackBoundary.Count)"
Write-Host "  core/mechanism:               $($mechanismBoundary.Count)"
Write-Host ""

# Show boundary details
Write-Host "--- Boundary whitelist details ---"
if ($typealiasBoundary.Count -gt 0) {
    Write-Host "  typealias:"
    foreach ($e in $typealiasBoundary) { Write-Host "    $($e.Entry)  [$($e.Reason)] $($e.Debt)" }
}
if ($functionResult.BoundaryCount -gt 0) {
    Write-Host "  function override ($($functionResult.BoundaryCount) entries, showing first 10):"
    $show = $functionResult.BoundaryAllowed | Select-Object -First 10
    foreach ($e in $show) { Write-Host "    $($e.Entry)  [$($e.Reason)]" }
    if ($functionResult.BoundaryCount -gt 10) { Write-Host "    ... and $($functionResult.BoundaryCount - 10) more" }
}
if ($callbackBoundary.Count -gt 0) {
    Write-Host "  callback:"
    foreach ($e in $callbackBoundary) { Write-Host "    $($e.Entry)  [$($e.Reason)] $($e.Debt)" }
}
if ($mechanismBoundary.Count -gt 0) {
    Write-Host "  mechanism:"
    foreach ($e in $mechanismBoundary) { Write-Host "    $($e.Entry)  [$($e.Reason)] $($e.Debt)" }
}
Write-Host ""

# Show blocking violations if any
$hasBlocking = ($typealiasPublicApi.Count + $rawMathNonAdapter.Count + $functionResult.PublicApiCount + $callbackPublicApi.Count + $mechanismPublicApi.Count) -gt 0
if ($hasBlocking) {
    Write-Host "--- BLOCKING VIOLATIONS (must fix) ---"
    if ($typealiasPublicApi.Count -gt 0) {
        Write-Host "  typealias ($($typealiasPublicApi.Count)):"
        $typealiasPublicApi | ForEach-Object { Write-Host "    $_" }
    }
    if ($rawMathNonAdapter.Count -gt 0) {
        Write-Host "  math/symbol non-adapter ($($rawMathNonAdapter.Count)):"
        $rawMathNonAdapter | ForEach-Object { Write-Host "    $_" }
    }
    if ($functionResult.PublicApiCount -gt 0) {
        Write-Host "  core/function ($($functionResult.PublicApiCount)):"
        $functionResult.PublicApi | ForEach-Object { Write-Host "    $_" }
    }
    if ($callbackPublicApi.Count -gt 0) {
        Write-Host "  core/callback ($($callbackPublicApi.Count)):"
        $callbackPublicApi | ForEach-Object { Write-Host "    $_" }
    }
    if ($mechanismPublicApi.Count -gt 0) {
        Write-Host "  core/mechanism ($($mechanismPublicApi.Count)):"
        $mechanismPublicApi | ForEach-Object { Write-Host "    $_" }
    }
    Write-Host ""
}

# Migration debt summary (deduplicate by entry)
$debtItems = @{}
foreach ($e in $typealiasBoundary) { if ($e.Debt -match 'MIGRATE') { $debtItems[$e.Entry] = "typealias: $($e.Entry) -> $($e.Debt)" } }
foreach ($e in $callbackBoundary) { if ($e.Debt -match 'MIGRATE') { $debtItems[$e.Entry] = "callback: $($e.Entry) -> $($e.Debt)" } }
foreach ($e in $mechanismBoundary) { if ($e.Debt -match 'MIGRATE') { $debtItems[$e.Entry] = "mechanism: $($e.Entry) -> $($e.Debt)" } }
# function override always has MEDIUM debt
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
        typealias_flt64 = $rawTypealias.Count
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
        typealias_flt64 = $typealiasPublicApi.Count
        math_symbol_non_adapter = $rawMathNonAdapter.Count
        core_function = $functionResult.PublicApiCount
        core_callback = $callbackPublicApi.Count
        core_mechanism = $mechanismPublicApi.Count
    }
    boundary_allowed = @{
        suppress_unchecked_cast = $suppressCount
        typealias_flt64 = $typealiasBoundary.Count
        core_function_override = $functionResult.BoundaryCount
        core_callback = $callbackBoundary.Count
        core_mechanism = $mechanismBoundary.Count
    }
    migration_debt = @($debtItems.Values)
} | ConvertTo-Json -Depth 5

$json | Out-File -FilePath scripts/scan-p13-mainchain-result.json -Encoding utf8
Write-Host "JSON saved to scripts/scan-p13-mainchain-result.json"

# ============================================================
# Gate check
# ============================================================

$maxUncheckedCast = 4
$blockingCounts = @(
    $importAsCount,
    $typealiasPublicApi.Count,
    $rawMathNonAdapter.Count,
    $functionResult.PublicApiCount,
    $callbackPublicApi.Count,
    $mechanismPublicApi.Count
)

$fail = $false
if ($suppressCount -gt $maxUncheckedCast) {
    Write-Host ""
    Write-Host "GATE: FAIL (UNCHECKED_CAST=$suppressCount > allowed $maxUncheckedCast)"
    $fail = $true
}
if ($blockingCounts | Where-Object { $_ -gt 0 }) {
    Write-Host ""
    Write-Host "GATE: FAIL (public_api_blocking checks not met)"
    $fail = $true
}
if ($fail) { exit 1 } else { Write-Host ""; Write-Host "GATE: PASS"; exit 0 }
