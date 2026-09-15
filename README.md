# FunTV Player

Cliente de IPTV nativo para Android TV / Fire TV, escrito en Kotlin, que se conecta
a un servicio propio vía **Xtream Codes API**. Login con usuario/contraseña/URL,
navegación por filas con Leanback (TV en Vivo, Películas, Series) y reproducción
HLS/MP4 con ExoPlayer (Media3). Pensado para distribuirse como APK directo (sideload),
no por Google Play ni Amazon Appstore.

## ⚠️ Estado de este repositorio

El código está completo y listo para compilar, pero **no se generó el .apk en esta
sesión**: el entorno donde corrió Claude Code tiene bloqueado por política de red
`dl.google.com` (403, "organization policy"), y tanto el SDK de Android
(`android.jar`, build-tools) como las librerías de AndroidX/Leanback/Media3 solo se
distribuyen desde ahí (`maven.google.com` redirige a `dl.google.com`). Verificación
hecha en esta sesión:

```
$ curl -I https://maven.google.com/com/android/tools/build/gradle/maven-metadata.xml
HTTP/2 301
location: https://dl.google.com/dl/android/maven2/...   ← bloqueado
```

Para obtener el `.apk` tienes dos caminos:

1. **Compilar en tu propia máquina** (recomendado, ver más abajo) — Android Studio
   descarga el SDK y las dependencias sin problema porque tu red no tiene ese bloqueo.
2. Pedir a quien administre esta cuenta que permita `dl.google.com` /
   `maven.google.com` en la política de red de las sesiones de Claude Code, y volver
   a pedirme que compile aquí.

## Requisitos para compilar

- **Android Studio** (Ladybug o más reciente) — la forma más simple, ya trae JDK y
  SDK Manager integrados. O bien, línea de comandos:
  - JDK 17+
  - Android SDK con `platform-35` y `build-tools;35.0.0` instalados (`sdkmanager`)
  - Variable de entorno `ANDROID_HOME` apuntando al SDK, o un archivo
    `local.properties` en la raíz con `sdk.dir=/ruta/a/tu/Android/Sdk`

## Compilar

```bash
# APK de debug (sin firmar, instalable directo para probar)
./gradlew assembleDebug
# queda en: app/build/outputs/apk/debug/app-debug.apk

# APK de release (firmado; ver "Firma" abajo)
./gradlew assembleRelease
# queda en: app/build/outputs/apk/release/app-release.apk
```

En Android Studio: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## Firma para release (keystore propio)

`app/build.gradle.kts` ya está preparado para firmar automáticamente si existe un
`keystore.properties` en la raíz. Genera tu keystore una sola vez:

```bash
./scripts/generate_keystore.sh
```

El script crea `funtv-release.keystore` y `keystore.properties` (ambos ya están en
`.gitignore`, nunca se suben a git). **Respalda el `.keystore` y sus contraseñas en
un lugar seguro fuera del proyecto**: si lo pierdes, no podrás firmar actualizaciones
con la misma identidad y los usuarios tendrían que desinstalar la app antes de
instalar una nueva versión. Sin `keystore.properties`, `assembleRelease` firma con la
clave de debug (útil para probar, no para distribuir).

## Instalar en el TV (sideload)

No se distribuye por tiendas oficiales. Opciones típicas:

- **ADB por red**: en el TV, activa depuración por red (Ajustes > Preferencias del
  dispositivo > Opciones de desarrollador), luego desde tu PC:
  ```bash
  adb connect <IP_DEL_TV>:5555
  adb install app/build/outputs/apk/release/app-release.apk
  ```
- **Gestor de archivos / USB**: copia el `.apk` a una memoria USB o súbelo a una nube,
  ábrelo con un gestor de archivos en el TV (ej. "Downloader" o "X-plore") con
  "Orígenes desconocidos" habilitado para esa app.

## Qué incluye la app

- **Login** (`ui/login`): usuario, contraseña y URL del servidor (con puerto);
  valida contra `player_api.php`; guarda las credenciales cifradas
  (`EncryptedSharedPreferences`) para no pedirlas de nuevo; muestra la fecha de
  expiración de la cuenta si el panel la envía; reintenta el login automáticamente
  al abrir la app si ya hay una sesión guardada.
- **Home** (`ui/main`): `BrowseSupportFragment` de Leanback con filas por categoría
  para TV en Vivo, Películas y Series (`get_live_categories/streams`,
  `get_vod_categories/streams`, `get_series_categories`, `get_series`), navegables
  por control remoto. Incluye una fila "Cuenta" con cierre de sesión.
- **Reproductor** (`ui/player`): ExoPlayer (Media3) con soporte HLS y MP4, controles
  por defecto (play/pausa, retroceder/avanzar, barra de progreso — ya navegables por
  D-pad), indicador de buffering y reintento automático con espera creciente ante
  errores de conexión (hasta 5 intentos, luego botón de reintento manual).
- **Detalle de serie** (`ui/details`): al seleccionar una serie, muestra su
  información (`get_series_info`) con una fila por temporada y los episodios como
  tarjetas.
- **Cliente Xtream Codes** (`data/api/XtreamClient.kt`): sobre OkHttp + Gson, con
  parseo defensivo de las inconsistencias típicas de estos paneles (una categoría
  vacía puede llegar como `{}` en vez de `[]`, ids numéricos a veces como texto).

## Estructura

```
app/src/main/java/com/funtv/player/
  data/api/        XtreamClient (llamadas HTTP), StreamUrlBuilder
  data/model/       Modelos de datos del API
  data/prefs/       SessionManager (credenciales cifradas)
  ui/login/         Pantalla de login
  ui/main/          Home (filas Leanback) y sus presenters
  ui/details/       Detalle de serie (temporadas/episodios)
  ui/player/        Reproductor ExoPlayer
```

## Branding

Colores tomados del logo: rojo `#E8433A`, azul `#2CA9E1`, verde `#66BB4A`, dorado
`#F5A623`, sobre fondo oscuro `#0D0D12` (paleta típica de apps de streaming). El
ícono, el banner de Android TV y el logo de la pantalla de login
(`app/src/main/res/drawable/ic_launcher_foreground.xml`, `tv_banner.xml`,
`funtv_logo.xml`) son una **interpretación vectorial** del logo que compartiste —no
se pudo extraer el PNG original en este entorno—, fiel a la paleta y composición
(molinillo de cuatro figuras de color alrededor de un núcleo de puntos). Si quieres
el logo exacto, reemplaza esos tres archivos por tus assets reales (o pídeme que lo
haga si me compartes el archivo de imagen en una carpeta del proyecto).

## Próximos pasos sugeridos (fuera del alcance de esta versión inicial)

- Búsqueda global (Leanback `SearchSupportFragment`).
- EPG (guía de programación) si el panel expone `get_short_epg`/`get_simple_data_table`.
- Favoritos / "continuar viendo".
- Paginación perezosa de categorías con muchísimo contenido (hoy se cargan todas al
  entrar al Home).
