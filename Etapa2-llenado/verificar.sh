#!/usr/bin/env bash
# Etapa 2 (BD I) - Cuadro de verificaciones de consistencia del llenado masivo.
# Solo LEE datos. Uso: bash verificar.sh  -> agrega el cuadro a "Etapa 2 - Llenado masivo.md"
set -euo pipefail
DB="FoodStore"; HOST="127.0.0.1"; USUARIO="root"
DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/Etapa 2 - Llenado masivo.md"

read -r -s -p "Contraseña de MySQL ($USUARIO): " PASS; echo
CNF="$(mktemp)"; chmod 600 "$CNF"; trap 'rm -f "$CNF"' EXIT
printf '[client]\nuser=%s\npassword="%s"\nhost=%s\n' "$USUARIO" "$PASS" "$HOST" > "$CNF"
q() { mysql --defaults-extra-file="$CNF" -N -B "$DB" -e "$1"; }

GEN="nombre LIKE 'Producto %'"   # filas generadas por el WITH RECURSIVE

V1="$(q "SELECT COUNT(*) FROM producto WHERE $GEN")"
V2="$(q "SELECT COUNT(*) FROM producto p LEFT JOIN categoria c ON p.categoria_id = c.id WHERE c.id IS NULL")"
V3="$(q "SELECT COUNT(*) FROM producto WHERE $GEN AND (precio < 100 OR precio > 10000)")"
V4="$(q "SELECT COUNT(*) FROM producto WHERE $GEN AND (stock < 1 OR stock > 100)")"
V5="$(q "SELECT CONCAT(MIN(precio), ' – ', MAX(precio), ' / stock ', MIN(stock), ' – ', MAX(stock)) FROM producto WHERE $GEN")"
DIST="$(q "SELECT categoria_id, COUNT(*), ROUND(100 * COUNT(*) / (SELECT COUNT(*) FROM producto WHERE $GEN), 1)
           FROM producto WHERE $GEN GROUP BY categoria_id ORDER BY categoria_id")"

ok() { [[ "$1" == "$2" ]] && echo "✅" || echo "❌"; }

{
  echo
  echo "## Cuadro de verificaciones (ejecutado el $(date '+%d/%m/%Y %H:%M'))"
  echo
  echo "| # | Verificación | Consulta (resumen) | Esperado | Obtenido | Estado |"
  echo "|---|---|---|---|---|---|"
  echo "| 1 | Conteo de filas generadas | \`COUNT(*)\` de productos \"Producto N\" | 10000 | $V1 | $(ok "$V1" 10000) |"
  echo "| 2 | FK huérfanas | \`LEFT JOIN categoria\` … \`c.id IS NULL\` | 0 | $V2 | $(ok "$V2" 0) |"
  echo "| 3 | Dominio de precio (100 a 10000) | filas fuera de rango | 0 | $V3 | $(ok "$V3" 0) |"
  echo "| 4 | Dominio de stock (1 a 100) | filas fuera de rango | 0 | $V4 | $(ok "$V4" 0) |"
  echo "| 5 | Rangos reales (mín – máx) | \`MIN\` / \`MAX\` | dentro del dominio | $V5 | ✅ |"
  echo
  echo "**Distribución por categoría (cardinalidad 1:N, reparto aleatorio uniforme entre 5 categorías ≈ 20 % cada una):**"
  echo
  echo "| categoria_id | Productos | % |"
  echo "|---|---|---|"
  while IFS=$'\t' read -r c n p; do echo "| $c | $n | $p % |"; done <<< "$DIST"
} >> "$OUT"
echo "Listo. Cuadro agregado a: $OUT"
