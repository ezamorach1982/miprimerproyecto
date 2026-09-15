#!/usr/bin/env bash
# Genera el keystore de firma de release para FunTV y el archivo keystore.properties
# que build.gradle.kts lee automáticamente. Ejecutar UNA sola vez, desde la raíz
# del proyecto: ./scripts/generate_keystore.sh
set -euo pipefail

cd "$(dirname "$0")/.."

KEYSTORE_FILE="funtv-release.keystore"
ALIAS="funtv"

if [ -f "$KEYSTORE_FILE" ]; then
  echo "Ya existe $KEYSTORE_FILE en la raíz del proyecto. Bórralo manualmente antes de regenerarlo (perderás la clave anterior; solo hazlo si sabes que no la necesitas)."
  exit 1
fi

echo "== Generación del keystore de firma de FunTV =="
echo "Se te pedirán dos contraseñas (una para el keystore y otra para la clave) y algunos datos de identificación (nombre, organización, etc.). Puedes usar la misma contraseña para ambas si lo prefieres."
echo

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

echo
read -r -p "Contraseña del keystore que acabas de crear: " STORE_PASSWORD
read -r -p "Contraseña de la clave ('$ALIAS'): " KEY_PASSWORD

cat > keystore.properties <<EOF
storeFile=$KEYSTORE_FILE
storePassword=$STORE_PASSWORD
keyAlias=$ALIAS
keyPassword=$KEY_PASSWORD
EOF

echo
echo "Listo. Se crearon:"
echo "  - $KEYSTORE_FILE   (el keystore en sí)"
echo "  - keystore.properties (credenciales que usa Gradle; NO se sube a git)"
echo
echo "IMPORTANTE — respaldo:"
echo "  1. Copia '$KEYSTORE_FILE' a un lugar seguro FUERA de este proyecto (otro disco, gestor de contraseñas, nube privada)."
echo "  2. Guarda también las contraseñas que ingresaste (por ejemplo, en un gestor de contraseñas)."
echo "  3. Si pierdes el keystore, no podrás publicar actualizaciones firmadas con la misma identidad:"
echo "     tendrías que generar uno nuevo y los usuarios tendrían que desinstalar la app vieja antes de instalar la nueva."
echo "  4. Nunca subas '$KEYSTORE_FILE' ni 'keystore.properties' a git (ya están en .gitignore)."
