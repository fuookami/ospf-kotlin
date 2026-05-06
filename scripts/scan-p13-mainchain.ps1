# P13 Mainchain Flt64 Scanner
# Usage: pwsh -File scripts/scan-p13-mainchain.ps1
# Output: JSON + text summary

$results = @{
    timestamp = ""
    import_as = 0
    suppress_unchecked_cast = 0
    typealias_flt64 = 0
    typealias_flt64_details = @()
    math_symbol_public = 0
    math_symbol_adapter = 0
    math_symbol_non_adapter = 0
    math_symbol_non_adapter_details = @()
    core_public = 0
    core_function = 0
    core_function_details = @()
    core_solver = 0
    core_solver_heuristic = 0
    core_callback = 0
    core_callback_details = @()
    core_mechanism = 0
    core_mechanism_details = @()
    core_other = 0
}

$results.timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss"

$mathRoot = "ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol"
$coreRoot = "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core"

# ---- A: import as ----
$importAs = Get-ChildItem -Recurse ospf-kotlin-math/src,ospf-kotlin-core/src -Filter *.kt |
    Select-String -Pattern 'import\s+\S+\s+as\s+'
$results.import_as = $importAs.Count

# ---- B: Suppress(UNCHECKED_CAST) ----
$suppress = Get-ChildItem -Recurse ospf-kotlin-math/src,ospf-kotlin-core/src -Filter *.kt |
    Select-String -Pattern 'Suppress.*UNCHECKED_CAST'
$results.suppress_unchecked_cast = $suppress.Count

# ---- C: typealias *Flt64 ----
$typealias = Get-ChildItem -Recurse ospf-kotlin-math/src,ospf-kotlin-core/src -Filter *.kt |
    Select-String -Pattern 'typealias\s+\w+.*Flt64'
$results.typealias_flt64 = $typealias.Count
$results.typealias_flt64_details = $typealias | ForEach-Object { "$($_.RelativePath):$($_.LineNumber): $($_.Line.Trim())" }

# ---- D: math.symbol public Flt64 signatures ----
$mathFiles = Get-ChildItem -Recurse $mathRoot -Filter *.kt
$mathMatches = $mathFiles | Select-String -Pattern '^(\s*)(fun|class|interface|data class|typealias|operator fun).*Flt64'
$results.math_symbol_public = $mathMatches.Count

$adapterMatches = $mathMatches | Where-Object { $_.RelativePath -match 'adapter[/\\]flt64' }
$results.math_symbol_adapter = $adapterMatches.Count

$nonAdapterMatches = $mathMatches | Where-Object { $_.RelativePath -notmatch 'adapter[/\\]flt64' }
$results.math_symbol_non_adapter = $nonAdapterMatches.Count
$results.math_symbol_non_adapter_details = $nonAdapterMatches | ForEach-Object { "$($_.RelativePath):$($_.LineNumber): $($_.Line.Trim())" }

# ---- E: core public Flt64 signatures ----
$coreFiles = Get-ChildItem -Recurse $coreRoot -Filter *.kt
$coreMatches = $coreFiles | Select-String -Pattern '^(\s*)(fun|class|interface|data class|typealias|operator fun|override fun).*Flt64'
$results.core_public = $coreMatches.Count

# E.1: core/intermediate_symbol/function
$functionMatches = $coreMatches | Where-Object { $_.RelativePath -match 'intermediate_symbol[/\\]function' }
$results.core_function = $functionMatches.Count
$results.core_function_details = $functionMatches | ForEach-Object { "$($_.RelativePath):$($_.LineNumber): $($_.Line.Trim())" }

# E.2: core/solver
$solverMatches = $coreMatches | Where-Object { $_.RelativePath -match 'solver[/\\]' }
$results.core_solver = $solverMatches.Count

# E.3: core/solver/heuristic
$heuristicMatches = $coreMatches | Where-Object { $_.RelativePath -match 'solver[/\\]heuristic' }
$results.core_solver_heuristic = $heuristicMatches.Count

# E.4: core/model/callback
$callbackMatches = $coreMatches | Where-Object { $_.RelativePath -match 'model[/\\]callback' }
$results.core_callback = $callbackMatches.Count
$results.core_callback_details = $callbackMatches | ForEach-Object { "$($_.RelativePath):$($_.LineNumber): $($_.Line.Trim())" }

# E.5: core/model/mechanism
$mechanismMatches = $coreMatches | Where-Object { $_.RelativePath -match 'model[/\\]mechanism' }
$results.core_mechanism = $mechanismMatches.Count
$results.core_mechanism_details = $mechanismMatches | ForEach-Object { "$($_.RelativePath):$($_.LineNumber): $($_.Line.Trim())" }

# E.6: core other (everything not in the above categories)
$knownPaths = @('intermediate_symbol[/\\]function', 'solver[/\\]', 'model[/\\]callback', 'model[/\\]mechanism')
$otherMatches = $coreMatches | Where-Object {
    $path = $_.RelativePath
    -not ($knownPaths | Where-Object { $path -match $_ })
}
$results.core_other = $otherMatches.Count

# ---- Text output ----
Write-Host "=== P13 Mainchain Flt64 Scan ==="
Write-Host "Timestamp: $($results.timestamp)"
Write-Host ""
Write-Host "Must-be-zero checks:"
Write-Host "  import as:                    $($results.import_as)"
Write-Host "  Suppress(UNCHECKED_CAST):     $($results.suppress_unchecked_cast)"
Write-Host "  typealias *Flt64:             $($results.typealias_flt64)"
Write-Host "  math/symbol non-adapter Flt64: $($results.math_symbol_non_adapter)"
Write-Host "  core/function Flt64:          $($results.core_function)"
Write-Host "  core/callback Flt64:          $($results.core_callback)"
Write-Host "  core/mechanism Flt64:         $($results.core_mechanism)"
Write-Host ""
Write-Host "Allowed (whitelist):"
Write-Host "  math/symbol adapter/flt64:    $($results.math_symbol_adapter)"
Write-Host "  core/solver Flt64:            $($results.core_solver)"
Write-Host "    (heuristic):                $($results.core_solver_heuristic)"
Write-Host ""
Write-Host "Summary:"
Write-Host "  math/symbol total Flt64:      $($results.math_symbol_public)"
Write-Host "  core total Flt64:             $($results.core_public)"
Write-Host "    function:                   $($results.core_function)"
Write-Host "    solver:                     $($results.core_solver)"
Write-Host "    callback:                   $($results.core_callback)"
Write-Host "    mechanism:                  $($results.core_mechanism)"
Write-Host "    other:                      $($results.core_other)"

if ($results.typealias_flt64 -gt 0) {
    Write-Host ""
    Write-Host "--- typealias *Flt64 details ---"
    $results.typealias_flt64_details | ForEach-Object { Write-Host $_ }
}

if ($results.math_symbol_non_adapter -gt 0) {
    Write-Host ""
    Write-Host "--- math/symbol non-adapter Flt64 details ---"
    $results.math_symbol_non_adapter_details | ForEach-Object { Write-Host $_ }
}

if ($results.core_function -gt 0) {
    Write-Host ""
    Write-Host "--- core/function Flt64 details ---"
    $results.core_function_details | ForEach-Object { Write-Host $_ }
}

if ($results.core_callback -gt 0) {
    Write-Host ""
    Write-Host "--- core/callback Flt64 details ---"
    $results.core_callback_details | ForEach-Object { Write-Host $_ }
}

if ($results.core_mechanism -gt 0) {
    Write-Host ""
    Write-Host "--- core/mechanism Flt64 details ---"
    $results.core_mechanism_details | ForEach-Object { Write-Host $_ }
}

# ---- JSON output ----
$json = @{
    timestamp = $results.timestamp
    import_as = $results.import_as
    suppress_unchecked_cast = $results.suppress_unchecked_cast
    typealias_flt64 = $results.typealias_flt64
    typealias_flt64_details = $results.typealias_flt64_details
    math_symbol_public = $results.math_symbol_public
    math_symbol_adapter = $results.math_symbol_adapter
    math_symbol_non_adapter = $results.math_symbol_non_adapter
    math_symbol_non_adapter_details = $results.math_symbol_non_adapter_details
    core_public = $results.core_public
    core_function = $results.core_function
    core_solver = $results.core_solver
    core_solver_heuristic = $results.core_solver_heuristic
    core_callback = $results.core_callback
    core_callback_details = $results.core_callback_details
    core_mechanism = $results.core_mechanism
    core_mechanism_details = $results.core_mechanism_details
    core_other = $results.core_other
} | ConvertTo-Json -Depth 5

$json | Out-File -FilePath scripts/scan-p13-mainchain-result.json -Encoding utf8
Write-Host ""
Write-Host "JSON saved to scripts/scan-p13-mainchain-result.json"

# ---- Gate check ----
$mustBeZero = @(
    $results.import_as,
    $results.suppress_unchecked_cast,
    $results.typealias_flt64,
    $results.math_symbol_non_adapter,
    $results.core_function,
    $results.core_callback,
    $results.core_mechanism
)

if ($mustBeZero | Where-Object { $_ -gt 0 }) {
    Write-Host "GATE: FAIL (must-be-zero checks not met)"
    exit 1
} else {
    Write-Host "GATE: PASS"
    exit 0
}
