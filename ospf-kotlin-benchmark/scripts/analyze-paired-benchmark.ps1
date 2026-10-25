param(
    # 结果目录：包含 <cell>-<label>-pair<n>-<suffix>.json 的配对计时文件。 / Directory with paired timing files named <cell>-<label>-pair<n>-<suffix>.json.
    [Parameter(Mandatory = $true)]
    [string] $ResultsDir,

    # 文件名后缀，time 为主轮，gc 为分配轮。 / File-name suffix: time for the main round, gc for the allocation round.
    [string] $Suffix = "time",

    # 基线标签与候选标签。 / Baseline and candidate labels.
    [string] $BaselineLabel = "B0",

    [string] $CandidateLabel = "C0",

    # 实用等效区间，默认 [0.95,1.05]。 / Practical equivalence band, default [0.95,1.05].
    [double] $EquivalenceLow = 0.95,

    [double] $EquivalenceHigh = 1.05,

    # 可选 Markdown 输出路径；不指定则只打印表格。 / Optional Markdown output; prints the table only when omitted.
    [string] $Output = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# 配对 fork 级统计：主判据为配对 log 比的 Student t 区间，稳健性核对为精确枚举 bootstrap（有放回有序重采样，无随机数）。
# 一次 JMH 调用可展开多个参数组合，每个组合作为独立结果单元，绝不把不同规模的分数合并（计划 §5.1）。
# 两种方法判定不一致时输出 INCONCLUSIVE-METHODS，绝不择优报告；样本不足返回 INSUFFICIENT-PAIRS。
# 结果 JSON 不入库，仅本脚本与其自测入库。
# Paired fork-level statistics: the primary decision uses the Student t interval of paired log ratios; robustness uses an
# exact-enumeration bootstrap over ordered resamples with replacement (no RNG). One JMH invocation can expand several
# parameter combinations, and each combination stays a separate result unit: scores of different sizes are never merged
# (plan §5.1). Disagreement yields INCONCLUSIVE-METHODS, never the more favourable verdict. Fewer than two complete
# pairs yields INSUFFICIENT-PAIRS. Result JSON stays outside the repository; only this script and its self-test are
# committed.

function Get-StudentTCritical {
    param(
        [Parameter(Mandatory = $true)]
        [int] $DegreesOfFreedom
    )

    $table = @{
        1 = 12.706; 2 = 4.303; 3 = 3.182; 4 = 2.776; 5 = 2.571
        6 = 2.447; 7 = 2.365; 8 = 2.306; 9 = 2.262; 10 = 2.228
        11 = 2.201; 12 = 2.179; 13 = 2.160; 14 = 2.145; 15 = 2.131
        20 = 2.086; 25 = 2.060; 30 = 2.042
    }
    if ($table.ContainsKey($DegreesOfFreedom)) {
        return $table[$DegreesOfFreedom]
    }
    return 1.96
}

function Get-Median {
    param(
        [Parameter(Mandatory = $true)]
        [double[]] $Values
    )

    $sorted = @($Values | Sort-Object)
    if ($sorted.Count -eq 0) {
        return [double]::NaN
    }
    $middle = [int] [Math]::Floor(($sorted.Count - 1) / 2)
    if ($sorted.Count % 2 -eq 1) {
        return $sorted[$middle]
    }
    return ($sorted[$middle] + $sorted[$middle + 1]) / 2.0
}

function Get-Verdict {
    param(
        [Parameter(Mandatory = $true)]
        [double] $Low,

        [Parameter(Mandatory = $true)]
        [double] $High,

        [Parameter(Mandatory = $true)]
        [double] $EquivalenceLow,

        [Parameter(Mandatory = $true)]
        [double] $EquivalenceHigh
    )

    if ($High -lt $EquivalenceLow) {
        return "BENEFIT"
    }
    if ($Low -gt $EquivalenceHigh) {
        return "REGRESSION"
    }
    if (($Low -ge $EquivalenceLow) -and ($High -le $EquivalenceHigh)) {
        return "EQUIVALENT"
    }
    return "INCONCLUSIVE"
}

function Get-ParamKey {
    param(
        [Parameter(Mandatory = $true)]
        $Params
    )

    if ($null -eq $Params) {
        return ""
    }
    $properties = @($Params.PSObject.Properties | Sort-Object Name)
    if ($properties.Count -eq 0) {
        return ""
    }
    return (($properties | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ",")
}

function Read-ForkEntries {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [string] $Metric
    )

    $entries = @(Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json)
    $result = @()
    foreach ($entry in $entries) {
        # 合成或旧版结果文件可能不带 params，StrictMode 下需先判断属性存在。
        # Synthetic or older result files can omit params, so check for the property before reading it under StrictMode.
        $paramsProperty = $entry.PSObject.Properties["params"]
        $key = if ($null -ne $paramsProperty) { Get-ParamKey -Params $paramsProperty.Value } else { "" }
        # 一次调用可展开同类的多个 @Benchmark 方法，方法名必须参与结果单元标识，否则后读的条目会覆盖先读的。
        # One invocation can expand several @Benchmark methods of the same class, so the method name must be part of
        # the result-unit identity; otherwise later entries would silently overwrite earlier ones.
        $benchmarkProperty = $entry.PSObject.Properties["benchmark"]
        $method = if ($null -ne $benchmarkProperty) { ([string]$benchmarkProperty.Value).Split(".")[-1] } else { "" }
        if ($Metric -eq "gc") {
            $secondary = $entry.PSObject.Properties["secondaryMetrics"]
            $allocation = if ($null -ne $secondary) { $secondary.Value."gc.alloc.rate.norm" } else { $null }
            if ($null -eq $allocation) {
                continue
            }
            $result += [pscustomobject]@{ Key = $key; Method = $method; Score = [double] $allocation.score; Unit = [string] $allocation.scoreUnit }
            continue
        }
        $primaryProperty = $entry.PSObject.Properties["primaryMetric"]
        if ($null -eq $primaryProperty) {
            continue
        }
        $primary = $primaryProperty.Value
        $result += [pscustomobject]@{ Key = $key; Method = $method; Score = [double] $primary.score; Unit = [string] $primary.scoreUnit }
    }
    return $result
}

function Get-UnitRow {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Cell,

        # @Benchmark 方法名；一次调用可展开同类多个方法，必须参与单元标识。
        # The @Benchmark method name: one invocation can expand several methods of a class, so it joins the unit key.
        [string] $Method = "",

        # 参数组合可为空（夹具无 @Param 时），因此不声明 Mandatory，避免空串无法绑定。
        # The parameter combination can be empty when a fixture has no @Param, so it is not declared Mandatory.
        [string] $Params = "",

        [Parameter(Mandatory = $true)]
        [hashtable] $ByPair,

        [Parameter(Mandatory = $true)]
        [string] $BaselineLabel,

        [Parameter(Mandatory = $true)]
        [string] $CandidateLabel,

        [Parameter(Mandatory = $true)]
        [double] $EquivalenceLow,

        [Parameter(Mandatory = $true)]
        [double] $EquivalenceHigh,

        [Parameter(Mandatory = $true)]
        [string] $Unit
    )

    $completePairs = @($ByPair.Keys | Where-Object { $ByPair[$_].ContainsKey($BaselineLabel) -and $ByPair[$_].ContainsKey($CandidateLabel) } | Sort-Object)
    if ($completePairs.Count -lt 2) {
        return [pscustomobject]@{
            Cell = $Cell; Method = $Method; Params = $Params; Pairs = $completePairs.Count; Ratio = $null; Low = $null; High = $null
            BootstrapLow = $null; BootstrapHigh = $null; ChangePercent = $null
            MedianBaseline = $null; MedianCandidate = $null; Unit = $Unit; Verdict = "INSUFFICIENT-PAIRS"
        }
    }

    $differences = @($completePairs | ForEach-Object { [Math]::Log($ByPair[$_][$CandidateLabel] / $ByPair[$_][$BaselineLabel]) })
    $count = $differences.Count
    $mean = ($differences | Measure-Object -Average).Average
    $variance = (($differences | ForEach-Object { ($_ - $mean) * ($_ - $mean) }) | Measure-Object -Sum).Sum / ($count - 1)
    $deviation = [Math]::Sqrt($variance)
    $critical = Get-StudentTCritical -DegreesOfFreedom ($count - 1)
    $halfWidth = $critical * $deviation / [Math]::Sqrt($count)
    $low = [Math]::Exp($mean - $halfWidth)
    $high = [Math]::Exp($mean + $halfWidth)

    # 精确枚举 n^n 个有序有放回重采样；超过 6 对时改为固定种子的确定性重采样，避免指数爆炸。
    # Exact enumeration of n^n ordered resamples with replacement; above 6 pairs fall back to a fixed-seed
    # deterministic resample so the enumeration cannot blow up exponentially.
    if ($count -le 6) {
        $resamples = New-Object 'double[]' ([int] [Math]::Pow($count, $count))
        $index = 0
        $total = [Math]::Pow($count, $count)
        for ([long] $code = 0; $code -lt $total; $code++) {
            $sum = 0.0
            $cursor = $code
            for ($i = 0; $i -lt $count; $i++) {
                $sum += $differences[[int] ($cursor % $count)]
                $cursor = [Math]::Floor($cursor / $count)
            }
            $resamples[$index++] = $sum / $count
        }
    } else {
        $generator = New-Object System.Random(20261025)
        $resamples = New-Object 'double[]' 20000
        for ($r = 0; $r -lt 20000; $r++) {
            $sum = 0.0
            for ($i = 0; $i -lt $count; $i++) { $sum += $differences[$generator.Next($count)] }
            $resamples[$r] = $sum / $count
        }
    }
    $sortedResamples = @($resamples | Sort-Object)
    $bootstrapLow = [Math]::Exp($sortedResamples[[int] [Math]::Floor(0.025 * ($sortedResamples.Count - 1))])
    $bootstrapHigh = [Math]::Exp($sortedResamples[[int] [Math]::Ceiling(0.975 * ($sortedResamples.Count - 1))])

    $tVerdict = Get-Verdict -Low $low -High $high -EquivalenceLow $EquivalenceLow -EquivalenceHigh $EquivalenceHigh
    $bootstrapVerdict = Get-Verdict -Low $bootstrapLow -High $bootstrapHigh -EquivalenceLow $EquivalenceLow -EquivalenceHigh $EquivalenceHigh
    $verdict = if ($tVerdict -eq $bootstrapVerdict) { $tVerdict } else { "INCONCLUSIVE-METHODS" }

    $baselineValues = @($completePairs | ForEach-Object { $ByPair[$_][$BaselineLabel] })
    $candidateValues = @($completePairs | ForEach-Object { $ByPair[$_][$CandidateLabel] })
    $medianBaseline = Get-Median -Values $baselineValues
    $medianCandidate = Get-Median -Values $candidateValues

    # 分配指标低于 1 B/op 时两端都是计数器分辨率上的零，比值只是噪声相除，不得当作收益或回退。
    # An allocation metric below 1 B/op is zero at counter resolution on both sides: the ratio is noise divided by
    # noise and must not be reported as a gain or a regression.
    if ($Unit -match '^B/op$' -and ($medianBaseline -lt 1.0) -and ($medianCandidate -lt 1.0)) {
        return [pscustomobject]@{
            Cell = $Cell; Method = $Method; Params = $Params; Pairs = $count; Ratio = [Math]::Round([Math]::Exp($mean), 4)
            Low = [Math]::Round($low, 4); High = [Math]::Round($high, 4)
            BootstrapLow = [Math]::Round($bootstrapLow, 4); BootstrapHigh = [Math]::Round($bootstrapHigh, 4)
            ChangePercent = [Math]::Round((1 - [Math]::Exp($mean)) * 100, 2)
            MedianBaseline = [Math]::Round($medianBaseline, 4); MedianCandidate = [Math]::Round($medianCandidate, 4)
            Unit = $Unit; Verdict = "DEGENERATE-METRIC"
        }
    }

    return [pscustomobject]@{
        Cell = $Cell
        Method = $Method
        Params = $Params
        Pairs = $count
        Ratio = [Math]::Round([Math]::Exp($mean), 4)
        Low = [Math]::Round($low, 4)
        High = [Math]::Round($high, 4)
        BootstrapLow = [Math]::Round($bootstrapLow, 4)
        BootstrapHigh = [Math]::Round($bootstrapHigh, 4)
        ChangePercent = [Math]::Round((1 - [Math]::Exp($mean)) * 100, 2)
        MedianBaseline = [Math]::Round($medianBaseline, 4)
        MedianCandidate = [Math]::Round($medianCandidate, 4)
        Unit = $Unit
        Verdict = $verdict
    }
}

function Get-PairedRows {
    param(
        [Parameter(Mandatory = $true)]
        [object[]] $Files,

        [Parameter(Mandatory = $true)]
        [string] $BaselineLabel,

        [Parameter(Mandatory = $true)]
        [string] $CandidateLabel,

        [Parameter(Mandatory = $true)]
        [double] $EquivalenceLow,

        [Parameter(Mandatory = $true)]
        [double] $EquivalenceHigh,

        [Parameter(Mandatory = $true)]
        [string] $Suffix
    )

    $namePattern = "^(.+)-($BaselineLabel|$CandidateLabel)-pair(\d+)-$Suffix\.json$"
    # 每个 (cell, 参数组合) 是独立结果单元：同一次调用展开多个规模时不合并分数。
    # Each (cell, parameter combination) is its own result unit: sizes expanded by one invocation are never merged.
    $units = @{}
    foreach ($file in $Files) {
        if ($file.Name -notmatch $namePattern) {
            continue
        }
        $cell = $Matches[1]
        $label = $Matches[2]
        $pair = [int] $Matches[3]
        foreach ($entry in @(Read-ForkEntries -Path $file.FullName -Metric $Suffix)) {
            if ($entry.Score -le 0) {
                continue
            }
            $unitKey = "$cell|$($entry.Method)|$($entry.Key)"
            if (-not $units.ContainsKey($unitKey)) {
                $units[$unitKey] = @{ ByPair = @{}; Cell = $cell; Method = $entry.Method; Params = $entry.Key; Unit = $entry.Unit }
            }
            $byPair = $units[$unitKey].ByPair
            if (-not $byPair.ContainsKey($pair)) {
                $byPair[$pair] = @{}
            }
            $byPair[$pair][$label] = $entry.Score
        }
    }

    $rows = @()
    foreach ($unitKey in ($units.Keys | Sort-Object)) {
        $unit = $units[$unitKey]
        $rows += Get-UnitRow -Cell $unit.Cell -Method $unit.Method -Params $unit.Params -ByPair $unit.ByPair `
            -BaselineLabel $BaselineLabel -CandidateLabel $CandidateLabel `
            -EquivalenceLow $EquivalenceLow -EquivalenceHigh $EquivalenceHigh -Unit $unit.Unit
    }
    return $rows
}

if (-not (Test-Path -LiteralPath $ResultsDir)) {
    throw "Results directory not found: $ResultsDir"
}

$pattern = "^(.+)-($BaselineLabel|$CandidateLabel)-pair(\d+)-$Suffix\.json$"
$files = @(Get-ChildItem -LiteralPath $ResultsDir -File -Filter "*-$Suffix.json" | Where-Object { $_.Name -match $pattern } | Sort-Object Name)
if ($files.Count -eq 0) {
    throw "No paired $Suffix results under: $ResultsDir"
}

$rows = Get-PairedRows -Files $files -BaselineLabel $BaselineLabel -CandidateLabel $CandidateLabel `
    -EquivalenceLow $EquivalenceLow -EquivalenceHigh $EquivalenceHigh -Suffix $Suffix

$columns = "Cell", "Method", "Params", "Pairs", "Ratio", "Low", "High", "BootstrapLow", "BootstrapHigh", "ChangePercent", "MedianBaseline", "MedianCandidate", "Unit", "Verdict"
$table = @($rows | Select-Object -Property $columns)
$table | Format-Table -AutoSize | Out-String -Width 260 | Write-Host

if ($Output.Trim().Length -gt 0) {
    $lines = @(
        "| Cell | Method | Params | Pairs | Ratio | 95% CI (t) | 95% CI (bootstrap) | Δ% | Median $BaselineLabel | Median $CandidateLabel | Unit | Verdict |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"
    )
    foreach ($row in $table) {
        $lines += ("| {0} | {1} | {2} | {3} | {4} | [{5}, {6}] | [{7}, {8}] | {9} | {10} | {11} | {12} | {13} |" -f `
            $row.Cell, $row.Method, $row.Params, $row.Pairs, $row.Ratio, $row.Low, $row.High, $row.BootstrapLow, $row.BootstrapHigh, `
            $row.ChangePercent, $row.MedianBaseline, $row.MedianCandidate, $row.Unit, $row.Verdict)
    }
    $lines | Set-Content -LiteralPath $Output -Encoding utf8
    Write-Host "Wrote Markdown table: $Output"
}
