#!/bin/bash
# P11 全仓清零扫描脚本
set -e

echo "=== P11 Scan Report ==="
echo "Date: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

count1=$(grep -r '@Suppress("UNCHECKED_CAST")' --include='*.kt' 2>/dev/null | wc -l)
count2=$(grep -rE 'typealias\s+\w+Flt64\s*=' --include='*.kt' 2>/dev/null | wc -l)
count3=$(grep -r 'AbstractTokenTableFlt64' --include='*.kt' 2>/dev/null | wc -l)
count4=$(grep -r 'Flt64LinearInequality' --include='*.kt' 2>/dev/null | wc -l)
count5=$(grep -r 'Solution<Flt64>' --include='*.kt' 2>/dev/null | wc -l)
count6=$(grep -r 'IntoValue\.Flt64' --include='*.kt' 2>/dev/null | wc -l)
count7=$(grep -r 'MechanismModelFlt64' --include='*.kt' 2>/dev/null | wc -l)
count8=$(grep -r 'LinearIntermediateSymbolFlt64' --include='*.kt' 2>/dev/null | wc -l)
count9=$(grep -r 'QuadraticIntermediateSymbolFlt64' --include='*.kt' 2>/dev/null | wc -l)

echo "UNCHECKED_CAST: $count1"
echo "typealias *Flt64: $count2"
echo "AbstractTokenTableFlt64: $count3"
echo "Flt64LinearInequality: $count4"
echo "Solution<Flt64>: $count5"
echo "IntoValue.Flt64: $count6"
echo "MechanismModelFlt64: $count7"
echo "LinearIntermediateSymbolFlt64: $count8"
echo "QuadraticIntermediateSymbolFlt64: $count9"

total=$((count1 + count2 + count3 + count4 + count5 + count6 + count7 + count8 + count9))
echo ""
echo "Total: $total"

if [ "$total" -eq 0 ]; then
  echo "STATUS: PASS"
  exit 0
else
  echo "STATUS: FAIL"
  exit 1
fi
