# Carfok Reproductor

**Carfok Reproductor** es un reproductor de música moderno para Android, diseñado con **Material Design 3** y enfocado en ofrecer una experiencia visual atractiva y funcional.

## 🚀 Características Principales

### 🎵 Reproducción de Audio
- **Escaneo Local**: Detecta automáticamente archivos de audio (MP3, WAV, AAC, OGG, M4A, FLAC) en la carpeta `Music/CarfokMusic`.
- **Reproducción en Segundo Plano**: Utiliza un *Foreground Service* para mantener la música activa con la pantalla apagada o usando otras apps.
- **Modos de Reproducción**: Soporte completo para modo aleatorio (*Shuffle*) y modo repetición (*Repeat*).
- **Control por Notificaciones**: Notificación interactiva con controles multimedia sincronizados mediante *MediaSession*.
- **Gestión de Llamadas**: Pausa automática al recibir llamadas y reanudación al finalizar (opcional).

### 📂 Gestión de Biblioteca y Listas
- **Playlists Personalizadas**: Crea, renombra y gestiona múltiples listas de reproducción.
- **Organización Drag & Drop**: Reordena las canciones dentro de tus playlists simplemente arrastrándolas.
- **Borrado Inteligente**:
    - **Quitar de la lista**: Elimina la canción de una playlist específica sin borrar el archivo.
    - **Borrado Físico**: Elimina el archivo de audio directamente desde la aplicación, limpiando automáticamente sus referencias en todas las playlists.
- **Búsqueda en Tiempo Real**: Filtra rápidamente canciones por nombre en la lista principal.

### 🎨 Interfaz y Experiencia Visual
- **Diseño Material 3**: UI moderna con gradientes, tarjetas elevadas y componentes visuales elegantes.
- **Mini Reproductor (MiniPlayer)**: Control persistente en la parte inferior de la lista para gestionar la música sin cambiar de pantalla.
- **Extracción de Metadatos**: Carga dinámica de carátulas (Album Art) y títulos directamente desde los archivos de audio.
- **Sincronización de UI**: La barra de progreso, el tiempo y los iconos se actualizan en tiempo real en todas las pantallas.

## 🛠️ Detalles Técnicos

### Requisitos
- **SO**: Android SDK 24+ (Nougat o superior).
- **Ubicación de archivos**: `Almacenamiento Interno > Music > CarfokMusic`.

### Tecnologías y APIs
- **Lenguaje**: Kotlin.
- **Audio**: `MediaPlayer`, `MediaSessionCompat` para controles externos.
- **Multimedia**: `MediaMetadataRetriever` para extracción de imágenes y datos.
- **Persistencia**: `SharedPreferences` con `Gson` para almacenar playlists.
- **UI**: `RecyclerView` con `DiffUtil` para actualizaciones fluidas y `ItemTouchHelper` para gestos.

### Permisos Requeridos
- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO`: Acceso a la música.
- `POST_NOTIFICATIONS`: Controles en la barra de estado (Android 13+).
- `FOREGROUND_SERVICE`: Reproducción continua.
- `READ_PHONE_STATE`: Control de audio durante llamadas.

---
**Carfok Reproductor** - Potencia y diseño en la palma de tu mano.
