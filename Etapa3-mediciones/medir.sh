#!/usr/bin/env bash
# =====================================================================
# Etapa 3 (BD I) - Mediciones de tiempo con y sin índices
# Consultas: igualdad, rango y JOIN. Cada variante se ejecuta 3 veces
# con EXPLAIN ANALYZE (tiempo real) y se reporta la mediana.
# También guarda el EXPLAIN tradicional (type / key / filas estimadas).
#
# No modifica datos. Único cambio posible: si no existe un índice sobre
# producto.precio, crea idx_producto_precio (definido en la Etapa 3).
#
# Uso:  bash medir.sh
# Genera: resultados-etapa3.md  (tabla de tiempos + EXPLAIN)
#         salida-completa.txt   (salidas crudas de EXPLAIN ANALYZE)
# =====================================================================
set -euo pipefail

DB="FoodStore"
HOST="127.0.0.1"
USUARIO="root"
CATEGORIA_ID=2              # misma categoría que en Consultas-avanzadas-y-reportes.sql
PRECIO_MIN=1000             # mismo rango que en Consultas-avanzadas-y-reportes.sql
PRECIO_MAX=5000

DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/resultados-etapa3.md"
RAW="$DIR/salida-completa.txt"

read -r -s -p "Contraseña de MySQL ($USUARIO): " PASS
echo

# Archivo temporal de credenciales (solo legible por vos, se borra al terminar)
CNF="$(mktemp)"
chmod 600 "$CNF"
trap 'rm -f "$CNF"' EXIT
printf '[client]\nuser=%s\npassword="%s"\nhost=%s\n' "$USUARIO" "$PASS" "$HOST" > "$CNF"

q()  { mysql --defaults-extra-file="$CNF" -N -B -r "$DB" -e "$1"; }   # sin encabezado
qh() { mysql --defaults-extra-file="$CNF"    -B -r "$DB" -e "$1"; }   # con encabezado

q "SELECT 1" > /dev/null || { echo "No se pudo conectar a MySQL."; exit 1; }

# --- Índices existentes sobre producto.categoria_id y producto.precio ---
# (incluye el índice que InnoDB crea automáticamente para la FK)
indices_de() {
  q "SELECT IFNULL(GROUP_CONCAT(DISTINCT INDEX_NAME ORDER BY INDEX_NAME), '')
     FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = '$DB' AND TABLE_NAME = 'producto'
       AND COLUMN_NAME = '$1' AND SEQ_IN_INDEX = 1"
}
IDX_CAT="$(indices_de categoria_id)"
IDX_PRE="$(indices_de precio)"

# Si no hay índice sobre precio, se crea el definido en
# Consultas-avanzadas-y-reportes.sql (único cambio que hace este script).
NOTA_INDICE=""
if [[ -z "$IDX_PRE" ]]; then
  echo "No existe índice sobre producto.precio: se crea idx_producto_precio..."
  q "CREATE INDEX idx_producto_precio ON producto(precio)"
  IDX_PRE="$(indices_de precio)"
  NOTA_INDICE="Se creó \`idx_producto_precio\` antes de medir, porque no existía."
fi
# Sobre categoria_id ya existe el índice que InnoDB crea para la FK
# (cumple la misma función que idx_producto_categoria), así que no se crea otro.

elegir_force() {   # $1 = lista separada por comas, $2 = nombre preferido
  if [[ ",$1," == *",$2,"* ]]; then echo "$2"; else echo "${1%%,*}"; fi
}
F_CAT="$(elegir_force "$IDX_CAT" idx_producto_categoria)"
F_PRE="$(elegir_force "$IDX_PRE" idx_producto_precio)"

ign() { [[ -n "$1" ]] && echo "IGNORE INDEX ($1)" || echo ""; }
frc() { [[ -n "$1" ]] && echo "FORCE INDEX ($1)"  || echo ""; }

# --- Consultas (variante sin índice / con índice) ---
EQ_SIN="SELECT * FROM producto p $(ign "$IDX_CAT") WHERE p.categoria_id = $CATEGORIA_ID AND p.eliminado = false"
EQ_CON="SELECT * FROM producto p $(frc "$F_CAT") WHERE p.categoria_id = $CATEGORIA_ID AND p.eliminado = false"

RG_SIN="SELECT p.id, p.nombre, p.precio FROM producto p $(ign "$IDX_PRE") WHERE p.precio BETWEEN $PRECIO_MIN AND $PRECIO_MAX AND p.eliminado = false"
RG_CON="SELECT p.id, p.nombre, p.precio FROM producto p $(frc "$F_PRE") WHERE p.precio BETWEEN $PRECIO_MIN AND $PRECIO_MAX AND p.eliminado = false"

JN_BASE_1="SELECT c.nombre AS categoria, COUNT(p.id) AS total_productos, ROUND(AVG(p.precio), 2) AS precio_promedio FROM categoria c LEFT JOIN producto p"
JN_BASE_2="ON c.id = p.categoria_id AND p.eliminado = false WHERE c.eliminado = false GROUP BY c.id, c.nombre"
JN_SIN="$JN_BASE_1 $(ign "$IDX_CAT") $JN_BASE_2"
JN_CON="$JN_BASE_1 $(frc "$F_CAT") $JN_BASE_2"

# --- Medición: 3 corridas con EXPLAIN ANALYZE, devuelve "t1 | t2 | t3 | mediana" ---
medir() {   # $1 = etiqueta, $2 = SQL
  local tiempos=() i salida t
  for i in 1 2 3; do
    salida="$(q "EXPLAIN ANALYZE $2")"
    printf '===== %s - corrida %s =====\n%s\n\n' "$1" "$i" "$salida" >> "$RAW"
    # tiempo total = fin del intervalo "actual time=inicio..fin" del nodo raíz (primera línea)
    t="$(grep -o 'actual time=[0-9.]*\.\.[0-9.]*' <<< "$salida" | head -1 | sed 's/.*\.\.//')"
    tiempos+=("${t:-?}")
  done
  local mediana
  mediana="$(printf '%s\n' "${tiempos[@]}" | sort -g | sed -n 2p)"
  echo "${tiempos[0]} | ${tiempos[1]} | ${tiempos[2]} | **${mediana}**"
}

# --- Plan: type / key / filas estimadas de la tabla producto (alias p) ---
plan() {   # $1 = SQL
  qh "EXPLAIN $1" | awk -F'\t' '
    NR == 1 { for (i = 1; i <= NF; i++) h[$i] = i; next }
    $h["table"] == "p" { printf "`%s` / `%s` / %s", $h["type"], $h["key"], $h["rows"]; exit }'
}

fila() {   # $1 = consulta, $2 = variante, $3 = SQL
  echo "| $1 | $2 | $(plan "$3") | $(medir "$1 - $2" "$3") |"
}

: > "$RAW"
echo "Midiendo (18 ejecuciones)..."

VERSION="$(q "SELECT VERSION()")"
TOTAL_PROD="$(q "SELECT COUNT(*) FROM producto")"
TOTAL_CAT="$(q "SELECT COUNT(*) FROM categoria")"
FILAS_EQ="$(q "SELECT COUNT(*) FROM producto WHERE categoria_id = $CATEGORIA_ID AND eliminado = false")"
FILAS_RG="$(q "SELECT COUNT(*) FROM producto WHERE precio BETWEEN $PRECIO_MIN AND $PRECIO_MAX AND eliminado = false")"

{
  echo "# Etapa 3 – Mediciones con y sin índices"
  echo
  echo "- **Fecha:** $(date '+%d/%m/%Y %H:%M')"
  echo "- **Motor:** MySQL $VERSION (InnoDB)"
  echo "- **Volumen:** $TOTAL_PROD productos, $TOTAL_CAT categorías"
  echo "- **Índices sobre \`producto.categoria_id\`:** ${IDX_CAT:-ninguno}"
  echo "- **Índices sobre \`producto.precio\`:** ${IDX_PRE:-ninguno} ${NOTA_INDICE}"
  echo "- **Nota:** \`categoria_id\` es el índice que InnoDB crea automáticamente para la clave foránea; cumple la función de \`idx_producto_categoria\`."
  echo "- **Método:** cada variante se ejecutó 3 veces con \`EXPLAIN ANALYZE\`; se toma el tiempo total del nodo raíz (\`actual time\`, en ms) y se reporta la **mediana**. *Sin índice*: \`IGNORE INDEX\` sobre todos los índices de la columna (incluido el que InnoDB crea para la FK). *Con índice*: \`FORCE INDEX\`."
  echo "- **Filas que cumplen el filtro:** igualdad (\`categoria_id = $CATEGORIA_ID\`) = $FILAS_EQ; rango (\`precio BETWEEN $PRECIO_MIN AND $PRECIO_MAX\`) = $FILAS_RG"
  echo
  echo "| Consulta | Variante | EXPLAIN: type / key / filas estimadas | Corrida 1 (ms) | Corrida 2 (ms) | Corrida 3 (ms) | Mediana (ms) |"
  echo "|---|---|---|---|---|---|---|"
  fila "Igualdad" "Sin índice" "$EQ_SIN"
  fila "Igualdad" "Con índice" "$EQ_CON"
  fila "Rango"    "Sin índice" "$RG_SIN"
  fila "Rango"    "Con índice" "$RG_CON"
  fila "JOIN"     "Sin índice" "$JN_SIN"
  fila "JOIN"     "Con índice" "$JN_CON"
  echo
  echo "## Consultas ejecutadas"
  echo
  echo '```sql'
  printf -- '-- Igualdad sin índice\n%s;\n-- Igualdad con índice\n%s;\n\n' "$EQ_SIN" "$EQ_CON"
  printf -- '-- Rango sin índice\n%s;\n-- Rango con índice\n%s;\n\n' "$RG_SIN" "$RG_CON"
  printf -- '-- JOIN sin índice\n%s;\n-- JOIN con índice\n%s;\n' "$JN_SIN" "$JN_CON"
  echo '```'
  echo
  echo "Salidas completas de EXPLAIN ANALYZE: \`salida-completa.txt\`."
} > "$OUT"

echo "Listo. Resultados en: $OUT"
