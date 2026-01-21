# 🎵 Carfok Music Player

**Carfok Music Player** es un reproductor de audio avanzado y ligero para Android, diseñado bajo una arquitectura robusta de servicios para garantizar que la música nunca se detenga. Con una interfaz oscura minimalista y un sistema de gestión de listas inteligente, es la herramienta definitiva para tu biblioteca local.

## ✨ Características Principales

* **📀 Extracción de Metadatos:** Visualización automática de la carátula del álbum (Album Art) extraída directamente de los archivos multimedia.
* **📂 Gestión de Almacenamiento:** Escanea automáticamente la carpeta `/Music/CarfokMusic`, organizando tu biblioteca al instante.
* **📝 Sistema de Playlists Personalizadas:** Crea, gestiona y reproduce listas de reproducción personalizadas que se guardan de forma persistente mediante GSON.
* **🎼 Mini Reproductor Persistente:** Controla la música desde la pantalla principal sin interrumpir tu navegación por la biblioteca.
* **🎧 Servicio en Primer Plano (Background Play):** Reproducción ininterrumpida gracias a un servicio vinculado que evita que Android cierre la app.
* **🔍 Buscador con DiffUtil:** Filtrado de canciones ultra rápido y con animaciones fluidas en la lista principal.
* **📱 Notificación Multimedia Avanzada:** Controles integrados con `MediaStyle`, vinculados a una `MediaSession` para compatibilidad con smartwatches y dispositivos Bluetooth.
* **🔀 Modos Inteligentes:** Funciones de **Bucle (Repeat One)** y **Aleatorio (Shuffle)**.

## 🛠️ Tecnologías Utilizadas

* **Kotlin**: Código limpio y tipado de última generación.
* **Android MediaSession**: Gestión profesional de controles de transporte y hardware externo.
* **GSON**: Persistencia de datos ligera para el gestor de Playlists.
* **CardView & ConstraintLayout**: Interfaz de usuario moderna, adaptada a pantallas con notch y gestos (fitsSystemWindows).
* **Version Catalogs (libs.toml)**: Gestión centralizada de dependencias.
* **MediaPlayer API**: Motor de audio nativo de alto rendimiento.



## 🚀 Instalación y Configuración

1. **Requisitos**: Android Studio Ladybug (o superior) y un dispositivo con Android 10 (API 29) o superior.
2. **Cargar música**:
   - Crea la carpeta `Music/CarfokMusic` en tu memoria interna.
   - Añade tus canciones en formatos soportados (`MP3`, `WAV`, `AAC`, `FLAC`, etc.).
3. **Compilación**:
   - Clona el repositorio.
   - Sincroniza el proyecto con Gradle para descargar las dependencias (especialmente **GSON** y **AndroidX Media**).
4. **Permisos**: Acepta los permisos de almacenamiento y notificaciones al iniciar para habilitar todas las funciones.

## 📁 Estructura del Proyecto

* `MusicService.kt`: El corazón del reproductor. Gestiona el audio y la notificación.
* `PlaylistManager.kt`: Gestor de persistencia de listas de reproducción.
* `PlayerActivity.kt`: Interfaz principal de reproducción con controles visuales.
* `ListActivity.kt`: Biblioteca principal con buscador y mini-player.
* `PlaylistActivity.kt`: Gestión de tus carpetas de listas personalizadas.

---
Desarrollado por **Carfok**
