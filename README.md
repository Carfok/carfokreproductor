# Carfok Reproductor

**Carfok Reproductor** es un reproductor de música moderno para Android, diseñado con **Material Design 3** y enfocado en ofrecer una experiencia visual atractiva y funcional.

## 🚀 Características

- **Reproducción Local**: Escanea y reproduce archivos de audio (MP3, WAV, AAC, etc.) desde la carpeta local `Music/CarfokMusic`.
- **Diseño Moderno**: Interfaz de usuario actualizada con Material 3, gradientes personalizados y componentes visuales elegantes.
- **Gestión de Listas de Reproducción**: Crea, edita y gestiona tus propias playlists de forma sencilla.
- **Metadatos y Carátulas**: Extracción automática de carátulas y títulos directamente desde los metadatos de los archivos de audio.
- **Reproducción en Segundo Plano**: Utiliza un *Foreground Service* para mantener la música sonando incluso con la pantalla apagada o la app en segundo plano.
- **Control por Notificaciones**: Notificación interactiva con controles de reproducción sincronizados mediante *MediaSession*.
- **Inteligencia en Llamadas**: Pausa automáticamente la música al recibir una llamada y la reanuda al finalizar (si estaba sonando).
- **Modos de Reproducción**: Soporte para modo aleatorio (*Shuffle*) y repetición (*Repeat*).

## 🛠️ Detalles Técnicos

### Requisitos
- Android SDK 24+ (Nougat o superior).
- Permisos de lectura de almacenamiento externo o archivos multimedia.
- Los archivos deben estar ubicados en: `Almacenamiento Interno > Music > CarfokMusic`.

### Tecnologías Utilizadas
- **Lenguaje**: Kotlin.
- **UI**: XML con componentes de Material 3 y ConstraintLayout.
- **Arquitectura**: Activities y Foreground Service para la lógica de audio.
- **Multimedia**: `MediaPlayer`, `MediaSessionCompat` y `MediaMetadataRetriever`.

### Permisos Clave
- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO`: Para acceder a la música.
- `WAKE_LOCK`: Para evitar que el procesador se duerma durante la reproducción.
- `FOREGROUND_SERVICE`: Para la reproducción en segundo plano.
- `READ_PHONE_STATE`: Para gestionar el estado de la música durante las llamadas.
- `POST_NOTIFICATIONS`: Para mostrar los controles en la barra de estado (Android 13+).

## 📦 Estructura del Proyecto

- **ListActivity**: Pantalla principal con el listado de todas las canciones.
- **PlayerActivity**: Interfaz detallada del reproductor con carátula grande y controles avanzados.
- **PlaylistActivity**: Gestión de las listas de reproducción creadas.
- **PlaylistSongsActivity**: Visualización de las canciones dentro de una lista específica.
- **MusicService**: El "cerebro" que gestiona la reproducción de audio en segundo plano.
- **PlaylistManager**: Encargado de la persistencia de las listas de reproducción (JSON/SharedPreferences).

---
Desarrollado como un reproductor ligero, vistoso y eficiente.
