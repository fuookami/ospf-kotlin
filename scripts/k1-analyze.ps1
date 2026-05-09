# K1 analysis: classify I5 non-adapter signature hits by module and category
$projRoot = (Get-Location).Path

function Get-RelPath($p) {
    if ($p.StartsWith($projRoot)) {
        $p = $p.Substring($projRoot.Length)
        if ($p.StartsWith('/') -or $p.StartsWith('\')) { $p = $p.Substring(1) }
    }
    return $p
}

function Is-WlPath($rp) {
    if ($rp -match 'adapter[/\\]flt64') { return $true }
    if ($rp -match 'solver[/\\]') { return $true }
    if ($rp -match 'model[/\\]mechanism') { return $true }
    if ($rp -match 'model[/\\]callback') { return $true }
    if ($rp -match 'model[/\\]intermediate') { return $true }
    if ($rp -match 'model[/\\]basic') { return $true }
    if ($rp -match 'intermediate_symbol[/\\]function') { return $true }
    if ($rp -match 'intermediate_symbol[/\\]flatten') { return $true }
    if ($rp -match 'intermediate_symbol[/\\]SymbolCombination\.kt') { return $true }
    if ($rp -match 'intermediate_symbol[/\\]IntermediateSymbol\.kt') { return $true }
    if ($rp -match 'intermediate_symbol[/\\]SolverBoundaryCasts\.kt') { return $true }
    if ($rp -match 'token[/\\]TokenList\.kt') { return $true }
    if ($rp -match 'token[/\\]Token\.kt') { return $true }
    if ($rp -match 'variable[/\\]') { return $true }
    return $false
}

$allKtFiles = Get-ChildItem -Recurse ospf-kotlin-math/src/main,ospf-kotlin-core/src/main -Filter *.kt
$coreHits = @{}
$mathHits = @{}
$coreTotal = 0
$mathTotal = 0

# Category counters
$catCompanionObject = 0  # companion object members (Flt64.zero, Flt64.one, etc.)
$catTypeIntrinsic = 0    # Flt64 class/number type itself
$catGenericSpecialization = 0  # V-typed class with Flt64 specialization
$catSolverBridge = 0     # solver bridge methods
$catOther = 0

foreach ($file in $allKtFiles) {
    $rp = Get-RelPath $file.FullName
    if (Is-WlPath $rp) { continue }

    $isCore = $rp -match 'ospf-kotlin-core'
    $isMath = $rp -match 'ospf-kotlin-math'

    $lines = Get-Content $file.FullName -ErrorAction Stop
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = [string]$lines[$i]
        $trimmed = $line.TrimStart()
        if ($trimmed.StartsWith('//') -or $trimmed.StartsWith('/*') -or $trimmed.StartsWith('*') -or
            $trimmed.StartsWith('import ') -or $trimmed.StartsWith('package ') -or
            $trimmed.StartsWith('@') -or $trimmed -eq '') { continue }
        if ($trimmed -match '^(internal|private|protected)\s+') { continue }
        if ($trimmed -match '^(val|var|fun|class|interface|data class|enum class|object|typealias|operator fun|infix fun|suspend fun|override fun)\s' -and
            $line -match 'Flt64') {

            $fileName = [System.IO.Path]::GetFileName($rp)

            # Categorize
            $cat = 'other'
            if ($fileName -eq 'Flt64.kt') { $cat = 'type_intrinsic' }
            elseif ($trimmed -match 'companion object|object\s') { $cat = 'companion_object' }
            elseif ($trimmed -match 'operator fun|fun Flt64|fun NumberField|val zero|val one|val infinity') { $cat = 'type_intrinsic' }
            elseif ($line -match '<Flt64>') { $cat = 'generic_specialization' }
            elseif ($line -match 'solver|Solver') { $cat = 'solver_bridge' }

            if ($isCore) {
                if (-not $coreHits.ContainsKey($fileName)) { $coreHits[$fileName] = 0 }
                $coreHits[$fileName]++
                $coreTotal++
            } elseif ($isMath) {
                if (-not $mathHits.ContainsKey($fileName)) { $mathHits[$fileName] = 0 }
                $mathHits[$fileName]++
                $mathTotal++
            }

            switch ($cat) {
                'type_intrinsic' { $catTypeIntrinsic++ }
                'companion_object' { $catCompanionObject++ }
                'generic_specialization' { $catGenericSpecialization++ }
                'solver_bridge' { $catSolverBridge++ }
                default { $catOther++ }
            }
        }
    }
}

Write-Host "=== K1: I5 Non-adapter Signature Flt64 Hits ==="
Write-Host ""
Write-Host "--- By Module ---"
Write-Host "  core: $coreTotal hits in $($coreHits.Count) files"
Write-Host "  math: $mathTotal hits in $($mathHits.Count) files"
Write-Host "  total: $($coreTotal + $mathTotal)"
Write-Host ""
Write-Host "--- Core files (top 20) ---"
foreach ($kv in $coreHits.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 20) {
    Write-Host "$($kv.Value.ToString().PadLeft(4)) $($kv.Key)"
}
Write-Host ""
Write-Host "--- Math files (top 20) ---"
foreach ($kv in $mathHits.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 20) {
    Write-Host "$($kv.Value.ToString().PadLeft(4)) $($kv.Key)"
}
Write-Host ""
Write-Host "--- By Category ---"
Write-Host "  type_intrinsic (Flt64 class itself): $catTypeIntrinsic"
Write-Host "  companion_object: $catCompanionObject"
Write-Host "  generic_specialization (<Flt64>): $catGenericSpecialization"
Write-Host "  solver_bridge: $catSolverBridge"
Write-Host "  other: $catOther"
