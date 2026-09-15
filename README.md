# FunTV Player

Cliente de IPTV nativo para Android TV / Fire TV, escrito en Kotlin, que se conecta
a un servicio propio vía **Xtream Codes API**. Login con usuario/contraseña/URL,
navegación por filas con Leanback (TV en Vivo, Películas, Series) y reproducción
HLS/MP4 con ExoPlayer (Media3). Pensado para distribuirse como APK directo (sideload),
no por Google Play ni Amazon Appstore.

## Estado de este repositorio

El entorno donde corre Claude Code tiene bloqueado `dl.google.com` por política de
red, y de ahí se distribuyen el SDK de Android y las librerías de
AndroidX/Leanback/Media3 — así que el `.apk` no se puede generar dentro de esa
sesión. Sí se puede (y se ha probado con éxito) compilar en tu propia máquina con
Android Studio, como se explica más abajo: tu red no tiene ese bloqueo.

Los cambios de código se siguen haciendo en las sesiones de Claude Code y
empujando a este repositorio; cada vez que haya cambios, trae la rama
(`git pull`) y vuelve a compilar (`Build > Build APK(s)`) para probarlos.

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
  valida contra `player_api.php`; guarda las credenciales localmente para no
  pedirlas de nuevo; muestra la fecha de expiración de la cuenta si el panel la
  envía; reintenta el login automáticamente al abrir la app si ya hay una sesión
  guardada.
- **Inicio** (`ui/main`): pantalla de aterrizaje con 4 tarjetas grandes — **TV en
  Vivo**, **Películas**, **Series** y **Cuenta** (cierre de sesión) — cada una abre
  su propia pantalla. Confirmación "presiona Atrás de nuevo para salir" para evitar
  cierres accidentales.
- **Secciones** (`ui/browse/SectionBrowseFragment`): una pantalla por tipo de
  contenido, con `BrowseSupportFragment` de Leanback y una fila por categoría
  (`get_live_categories/streams`, `get_vod_categories/streams`,
  `get_series_categories`, `get_series`) — así no se mezclan canales, películas y
  series en una sola lista.
- **Reproductor** (`ui/player`): ExoPlayer (Media3) con soporte HLS y MP4, controles
  por defecto (play/pausa, retroceder/avanzar, barra de progreso, calidad/audio y
  **subtítulos** — todo navegable por D-pad), indicador de buffering, reintento
  automático con espera creciente ante errores de conexión (retomando desde donde se
  cortó, no desde el inicio) y **"continuar viendo"** para películas/episodios
  (recuerda la posición y la retoma la próxima vez; no aplica a TV en vivo).
- **Detalle de serie** (`ui/details`): al seleccionar una serie, muestra su
  información (`get_series_info`) con una fila por temporada, episodios con su
  póster (`info.movie_image`) y título.
- **Cliente Xtream Codes** (`data/api/XtreamClient.kt`): sobre OkHttp + Gson, con
  parseo defensivo de las inconsistencias típicas de estos paneles (una categoría
  vacía puede llegar como `{}` en vez de `[]`, ids numéricos a veces como texto).

## Estructura

```
app/src/main/java/com/funtv/player/
  data/api/        XtreamClient (llamadas HTTP), StreamUrlBuilder
  data/model/      Modelos de datos del API
  data/prefs/      SessionManager (credenciales), PlaybackPositionManager ("continuar viendo")
  ui/login/        Pantalla de login
  ui/main/         Inicio (4 tarjetas de sección) y sus presenters
  ui/browse/       Pantalla de categorías por sección (Live/VOD/Series)
  ui/details/      Detalle de serie (temporadas/episodios)
  ui/player/       Reproductor ExoPlayer
```

## Branding

Colores tomados del logo: rojo `#E8433A`, azul `#2CA9E1`, verde `#66BB4A`, dorado
`#F5A623`, sobre fondo oscuro `#0D0D12` (paleta típica de apps de streaming).

El ícono, el banner de Android TV y el logo de la pantalla de login
(`app/src/main/res/drawable/ic_launcher_foreground.xml`, `tv_banner.xml`,
`funtv_logo.xml`) son una **interpretación vectorial**, no el logo real: no fue
posible extraer el PNG original en este entorno (llegó como imagen incrustada en el
chat, sin un archivo en disco accesible). Para el logo exacto, la forma más
confiable es usar el asistente de Android Studio con tu archivo real:

1. En el panel de proyecto, clic derecho sobre `app` > `New` > `Image Asset`.
2. Pestaña **Icon Type**: `Launcher Icons (Adaptive and Legacy)` → carga tu PNG en
   "Foreground Layer" → esto reemplaza `ic_launcher` en todas las densidades.
3. Para el banner de TV (320×180dp), exporta tu logo con el texto "FunTV" incluido
   como PNG a esas dimensiones (en Canva, Figma, etc.) y reemplaza
   `app/src/main/res/drawable/tv_banner.xml` por un `tv_banner.png` con ese
   contenido (actualiza la referencia en el manifest si cambias el nombre).
4. Para el logo de la pantalla de login, reemplaza `funtv_logo.xml` por tu PNG de
   la misma forma (o pídeme que lo haga si me compartes el archivo directamente
   como un archivo en este proyecto, no como imagen pegada en el chat).

## Roadmap priorizado

**Ya implementado:**
- Favoritos: mantén presionado OK/Aceptar sobre una tarjeta de canal/película/serie
  para marcarla o desmarcarla; aparecen en una fila "Favoritos" en el inicio.
- Búsqueda global (`ui/search`): filtra por nombre sobre el catálogo ya cacheado
  localmente (visita cada sección al menos una vez para que haya algo que buscar).
- Splash screen animado de arranque.
- Ficha "Cuenta" con servidor/usuario/vencimiento, versión de la app y cierre de
  sesión explícito (antes la tarjeta "Cuenta" cerraba sesión directo).
- Ficha de detalle de película, barra de progreso, fila "Continuar viendo",
  autoplay del siguiente episodio, zapping de canal con DPAD arriba/abajo dentro
  del reproductor, y detección de sesión vencida (redirige a Login con aviso en
  vez de mostrar un error de conexión genérico).

**Evaluadas y dejadas fuera por ahora** (con la razón):
- **EPG "en vivo ahora" en las tarjetas de canales**: pedir la programación de
  cada canal visible multiplicaría las peticiones al panel — contradice el
  trabajo de estabilidad de la Fase 0. Se puede hacer bien (cargar solo para las
  tarjetas realmente visibles), pero es un diseño aparte, no un añadido rápido.
- Perfiles + PIN de control parental, multi-cuenta/multi-servidor, certificados
  HTTPS autofirmados, selector de tema de color: quedan pendientes, no por
  riesgo sino por alcance — cada uno es una función completa en sí misma.

**Factibles pero de mayor alcance** (varias pantallas/lógica nueva):
- Descargas para ver sin internet (requiere gestión de almacenamiento, cola de
  descargas y, si el contenido está protegido, DRM — Xtream Codes normalmente no
  ofrece streams protegidos, así que sería "guardar el archivo", no DRM real).
- Multi-View (varios canales a la vez): técnicamente posible con varias instancias
  de ExoPlayer, pero exige mucho cuidado con memoria/CPU en hardware de TV modesto.
- Picture-in-Picture: soportado por Android desde API 26; el resto del proyecto usa
  minSdk 21, así que habría que decidir si vale la pena subir el mínimo o hacerlo
  condicional.

**Bloqueadas o dependientes de infraestructura que no tienes hoy**:
- Chromecast: el SDK de Cast se distribuye por `dl.google.com`, el mismo dominio
  bloqueado en esta sesión — se puede agregar, pero habría que compilarlo en tu
  máquina (ya lo estás haciendo) y no aquí.
- Notificaciones push / campanita / recordatorios de vencimiento / soporte por
  WhatsApp: necesitan un backend propio (servidor que dispare las notificaciones),
  no son solo cambios en la app cliente.
- "Catálogo inteligente" con sincronización en segundo plano: requiere diseñar una
  estrategia de caché/actualización; factible, pero es un proyecto en sí mismo.

Dime cuáles priorizar y seguimos con esas.
