Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# analyze-paired-benchmark.ps1 的自测：合成配对 JSON，覆盖判定、区间、方法冲突、样本不足，
# 以及“一次调用展开多个参数组合时每个组合独立成结果单元、绝不合并不同规模分数”。
# 只写系统临时目录，不在仓库产生任何文件。
# Self-test for analyze-paired-benchmark.ps1 using synthetic paired JSON: verdicts, intervals, method disagreement,
# insufficient pairs, and the rule that each parameter combination expanded by one invocation stays a separate result
# unit (scores of different sizes are never merged). Writes only to the system temporary directory.

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$analyzePath = Join-Path $scriptRoot "analyze-paired-benchmark.ps1"
if (-not (Test-Path -LiteralPath $analyzePath)) {
    throw "Analysis script not found: $analyzePath"
}

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [bool] $Condition,

        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function New-JmhEntry {
    param(
        [Parameter(Mandatory = $true)]
        [double] $Score,

        [Parameter(Mandatory = $true)]
        [hashtable] $Params,

        [string] $Unit = "us/op",

        [string] $Method = "run",

        [double] $Allocation = 0.0
    )

    return [ordered]@{
        benchmark             = "fuookami.ospf.kotlin.benchmark.SyntheticCase.$Method"
        mode                  = "avgt"
        threads               = 1
        forks                 = 1
        params                = $Params
        primaryMetric         = [ordered]@{
            score      = $Score
            scoreError = ($Score * 0.01)
            scoreUnit  = $Unit
            count      = 10
        }
        secondaryMetrics      = [ordered]@{
            "gc.alloc.rate.norm" = [ordered]@{
                score     = $Allocation
                scoreUnit = "B/op"
            }
        }
    }
}

function New-JmhFile {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [object[]] $Entries
    )

    ConvertTo-Json -InputObject @($Entries) -Depth 6 | Set-Content -LiteralPath $Path -Encoding ascii
}

function Read-TableRows {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    $rows = @{}
    foreach ($line in (Get-Content -LiteralPath $Path)) {
        if ($line -notmatch '^\|\s*(\S+)\s*\|\s*(\S*)\s*\|\s*(\S*)\s*\|\s*(\d+)\s*\|\s*(\S*)\s*\|.*\|\s*([A-Z-]+)\s*\|$') {
            continue
        }
        $rows["$($Matches[1])|$($Matches[2])|$($Matches[3])"] = [pscustomobject]@{
            Cell    = $Matches[1]
            Method  = $Matches[2]
            Params  = $Matches[3]
            Pairs   = [int] $Matches[4]
            Ratio   = $Matches[5]
            Verdict = $Matches[6]
        }
    }
    return $rows
}

$tmp = Join-Path ([IO.Path]::GetTempPath()) ("ospf-paired-selftest-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tmp | Out-Null

try {
    # Case 1: candidate 60% faster on every pair, low variance -> BENEFIT.
    for ($pair = 1; $pair -le 5; $pair++) {
        New-JmhFile -Path (Join-Path $tmp "fast-B0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score (100.0 + $pair) -Params @{ size = "1024" }
        )
        New-JmhFile -Path (Join-Path $tmp "fast-C0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score (40.0 + $pair * 0.02) -Params @{ size = "1024" }
        )
    }

    # Case 2: identical scores -> EQUIVALENT.
    for ($pair = 1; $pair -le 5; $pair++) {
        New-JmhFile -Path (Join-Path $tmp "same-B0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score (50.0 + $pair * 0.01) -Params @{ size = "256" }
        )
        New-JmhFile -Path (Join-Path $tmp "same-C0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score (50.0 + $pair * 0.01) -Params @{ size = "256" }
        )
    }

    # Case 3: candidate 80% slower -> REGRESSION.
    for ($pair = 1; $pair -le 5; $pair++) {
        New-JmhFile -Path (Join-Path $tmp "slow-B0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score (20.0 + $pair * 0.01) -Params @{ size = "256" }
        )
        New-JmhFile -Path (Join-Path $tmp "slow-C0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score (36.0 + $pair * 0.01) -Params @{ size = "256" }
        )
    }

    # Case 4: only one complete pair -> INSUFFICIENT-PAIRS.
    New-JmhFile -Path (Join-Path $tmp "few-B0-pair1-time.json") -Entries @(New-JmhEntry -Score 10.0 -Params @{ size = "256" })
    New-JmhFile -Path (Join-Path $tmp "few-C0-pair1-time.json") -Entries @(New-JmhEntry -Score 10.0 -Params @{ size = "256" })

    # Case 5: noisy near-threshold cell -> INCONCLUSIVE (CI straddles 0.95).
    $noisyCandidate = @(96.0, 96.0, 96.0, 120.0, 120.0)
    for ($pair = 1; $pair -le 5; $pair++) {
        New-JmhFile -Path (Join-Path $tmp "noisy-B0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score 100.0 -Params @{ size = "256" }
        )
        New-JmhFile -Path (Join-Path $tmp "noisy-C0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score $noisyCandidate[$pair - 1] -Params @{ size = "256" }
        )
    }

    # Case 6: one invocation expanding three sizes. The small size improves, the large size regresses, so they must
    # appear as separate rows with opposite verdicts instead of one merged average.
    for ($pair = 1; $pair -le 5; $pair++) {
        New-JmhFile -Path (Join-Path $tmp "mixed-B0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score 10.0 -Params @{ count = "64" }
            New-JmhEntry -Score 100.0 -Params @{ count = "256" }
        )
        New-JmhFile -Path (Join-Path $tmp "mixed-C0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score 4.0 -Params @{ count = "64" }
            New-JmhEntry -Score 160.0 -Params @{ count = "256" }
        )
    }

    # Case 6b: two @Benchmark methods of one class in a single result file with identical params. They must stay
    # separate units; keying only on (cell, params) would let the second entry overwrite the first.
    for ($pair = 1; $pair -le 5; $pair++) {
        New-JmhFile -Path (Join-Path $tmp "methods-B0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score 100.0 -Params @{ size = "1024" } -Method "invalidateThenRebuild"
            New-JmhEntry -Score 50.0 -Params @{ size = "1024" } -Method "warmReadsOnly"
        )
        New-JmhFile -Path (Join-Path $tmp "methods-C0-pair$pair-time.json") -Entries @(
            New-JmhEntry -Score 40.0 -Params @{ size = "1024" } -Method "invalidateThenRebuild"
            New-JmhEntry -Score 90.0 -Params @{ size = "1024" } -Method "warmReadsOnly"
        )
    }

    $md = Join-Path $tmp "table.md"
    $output = & $analyzePath -ResultsDir $tmp -Suffix time -Output $md 2>&1
    $text = ($output | Out-String)
    Assert-True ($text -notmatch 'Exception|Cannot process') "Analyzer reported an error:`n$text"

    $rows = Read-TableRows -Path $md
    Assert-True ($rows.ContainsKey("fast|run|size=1024")) "fast cell missing:`n$text"
    Assert-True ($rows["fast|run|size=1024"].Verdict -eq "BENEFIT") "Expected fast=BENEFIT, got $($rows['fast|run|size=1024'].Verdict)`n$text"
    Assert-True ($rows["same|run|size=256"].Verdict -eq "EQUIVALENT") "Expected same=EQUIVALENT, got $($rows['same|run|size=256'].Verdict)`n$text"
    Assert-True ($rows["slow|run|size=256"].Verdict -eq "REGRESSION") "Expected slow=REGRESSION, got $($rows['slow|run|size=256'].Verdict)`n$text"
    Assert-True ($rows["few|run|size=256"].Verdict -eq "INSUFFICIENT-PAIRS") "Expected few=INSUFFICIENT-PAIRS, got $($rows['few|run|size=256'].Verdict)`n$text"
    Assert-True ($rows["noisy|run|size=256"].Verdict -match '^INCONCLUSIVE') "Expected noisy=INCONCLUSIVE*, got $($rows['noisy|run|size=256'].Verdict)`n$text"

    # Ratio direction: R = candidate/baseline, so a 60% saving is about 0.4.
    $fastRatio = [double] $rows["fast|run|size=1024"].Ratio
    Assert-True ([Math]::Abs($fastRatio - 0.4) -lt 0.05) "Expected fast ratio near 0.4, got $fastRatio`n$text"

    # Parameter combinations stay separate units with opposite verdicts and correct pair counts.
    Assert-True ($rows.ContainsKey("mixed|run|count=64")) "small size missing as its own unit:`n$text"
    Assert-True ($rows.ContainsKey("mixed|run|count=256")) "large size missing as its own unit:`n$text"
    Assert-True ($rows["mixed|run|count=64"].Verdict -eq "BENEFIT") "Expected mixed|count=64=BENEFIT, got $($rows['mixed|run|count=64'].Verdict)"
    Assert-True ($rows["mixed|run|count=256"].Verdict -eq "REGRESSION") "Expected mixed|count=256=REGRESSION, got $($rows['mixed|run|count=256'].Verdict)"
    Assert-True ($rows["mixed|run|count=64"].Pairs -eq 5) "Expected 5 pairs per combination, got $($rows['mixed|run|count=64'].Pairs)"

    # Case 6b assertions: same cell and same params, different @Benchmark methods -> two independent units,
    # one improving and one regressing; a params-only key would collapse them into a single misleading row.
    Assert-True ($rows.ContainsKey("methods|invalidateThenRebuild|size=1024")) "first method missing as its own unit:`n$text"
    Assert-True ($rows.ContainsKey("methods|warmReadsOnly|size=1024")) "second method missing as its own unit:`n$text"
    Assert-True ($rows["methods|invalidateThenRebuild|size=1024"].Verdict -eq "BENEFIT") "Expected invalidateThenRebuild=BENEFIT, got $($rows['methods|invalidateThenRebuild|size=1024'].Verdict)"
    Assert-True ($rows["methods|warmReadsOnly|size=1024"].Verdict -eq "REGRESSION") "Expected warmReadsOnly=REGRESSION, got $($rows['methods|warmReadsOnly|size=1024'].Verdict)"

    # gc suffix reads the allocation secondary metric instead of the primary time score.
    for ($pair = 1; $pair -le 5; $pair++) {
        New-JmhFile -Path (Join-Path $tmp "alloc-B0-pair$pair-gc.json") -Entries @(
            New-JmhEntry -Score 10.0 -Allocation 2048.0 -Params @{ size = "256" }
        )
        New-JmhFile -Path (Join-Path $tmp "alloc-C0-pair$pair-gc.json") -Entries @(
            New-JmhEntry -Score 10.0 -Allocation 1024.0 -Params @{ size = "256" }
        )
    }
    $gcMd = Join-Path $tmp "table-gc.md"
    & $analyzePath -ResultsDir $tmp -Suffix gc -Output $gcMd | Out-Null
    $gcRows = Read-TableRows -Path $gcMd
    Assert-True ($gcRows.ContainsKey("alloc|run|size=256")) "gc round did not pick up allocation cells:`n$(Get-Content $gcMd -Raw)"
    Assert-True ($gcRows["alloc|run|size=256"].Verdict -eq "BENEFIT") "Expected allocation halving to report BENEFIT"
    $gcText = Get-Content -LiteralPath $gcMd -Raw
    Assert-True ($gcText -match 'B/op') "Expected B/op unit from the allocation metric:`n$gcText"

    # Case 7: both sides below 1 B/op is zero at counter resolution; the ratio must be labelled degenerate,
    # never reported as a gain.
    for ($pair = 1; $pair -le 5; $pair++) {
        New-JmhFile -Path (Join-Path $tmp "tiny-B0-pair$pair-gc.json") -Entries @(
            New-JmhEntry -Score 10.0 -Allocation 0.0000042 -Params @{ size = "256" }
        )
        New-JmhFile -Path (Join-Path $tmp "tiny-C0-pair$pair-gc.json") -Entries @(
            New-JmhEntry -Score 10.0 -Allocation 0.0000006 -Params @{ size = "256" }
        )
    }
    $gcMd2 = Join-Path $tmp "table-gc2.md"
    & $analyzePath -ResultsDir $tmp -Suffix gc -Output $gcMd2 | Out-Null
    $gcRows2 = Read-TableRows -Path $gcMd2
    Assert-True ($gcRows2.ContainsKey("tiny|run|size=256")) "sub-1 B/op cell missing from the gc table"
    Assert-True ($gcRows2["tiny|run|size=256"].Verdict -eq "DEGENERATE-METRIC") "Expected DEGENERATE-METRIC for sub-1 B/op on both sides, got $($gcRows2['tiny|run|size=256'].Verdict)"
    # One side large and the other sub-1 B/op is a real drop and must still be reported as a benefit.
    Assert-True ($gcRows2["alloc|run|size=256"].Verdict -eq "BENEFIT") "Expected the real allocation drop to stay BENEFIT"


    # Time and allocation rounds must not mix files with a different suffix.
    $timeText = Get-Content -LiteralPath $md -Raw
    Assert-True ($timeText -notmatch 'alloc') "Time round leaked gc-suffixed cells:`n$timeText"

    # A directory holding only one side of the pair must report INSUFFICIENT-PAIRS, not fabricate a verdict.
    $lonely = Join-Path $tmp "lonely"
    New-Item -ItemType Directory -Path $lonely | Out-Null
    New-JmhFile -Path (Join-Path $lonely "only-B0-pair1-time.json") -Entries @(New-JmhEntry -Score 5.0 -Params @{ size = "256" })
    $lonelyMd = Join-Path $lonely "table.md"
    & $analyzePath -ResultsDir $lonely -Suffix time -Output $lonelyMd | Out-Null
    $lonelyText = Get-Content -LiteralPath $lonelyMd -Raw
    Assert-True ($lonelyText -match 'INSUFFICIENT-PAIRS') "Expected one-sided directory to report INSUFFICIENT-PAIRS:`n$lonelyText"
    Assert-True ($lonelyText -notmatch 'BENEFIT|REGRESSION|EQUIVALENT\b') "One-sided data must not produce a substantive verdict:`n$lonelyText"

    # A directory without any paired file naming must fail clearly instead of returning an empty table.
    $empty = Join-Path $tmp "empty"
    New-Item -ItemType Directory -Path $empty | Out-Null
    Set-Content -LiteralPath (Join-Path $empty "unrelated.json") -Value "[]" -Encoding ascii
    $failed = $false
    try { & $analyzePath -ResultsDir $empty -Suffix time | Out-Null } catch { $failed = $true }
    Assert-True $failed "Expected a failure when no paired results exist."

    Write-Host "PASS: analyze-paired-benchmark self-test (20 checks)"
}
finally {
    Remove-Item -LiteralPath $tmp -Recurse -Force -ErrorAction SilentlyContinue
}
