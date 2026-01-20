# 🎵 Carfok Music Player

**Carfok Music Player** es un reproductor de audio ligero y potente para Android, diseñado para ofrecer una experiencia fluida con una interfaz oscura y moderna. Permite gestionar bibliotecas de música locales de forma eficiente y con control total desde dispositivos externos.

## ✨ Características Principales

* **📂 Gestión de Almacenamiento Público:** Escanea automáticamente la carpeta `/Music/CarfokMusic` en la memoria interna, facilitando al usuario la adición de archivos.
* **🎶 Compatibilidad Multiformato:** Soporta `MP3`, `WAV`, `AAC`, `OGG`, `M4A` y `FLAC`.
* **📱 Interfaz Moderna (Dark Mode):** Diseño optimizado para alto contraste con texto blanco y fondos profundos para una mejor visualización.
* **🔍 Buscador Inteligente:** Filtrado de canciones en tiempo real mediante un `SearchView` optimizado con `DiffUtil`.
* **🎧 Control Remoto (Bluetooth):** Integración completa con `MediaSession` para controlar la música desde cascos Bluetooth, relojes inteligentes o mandos externos.
* **🔔 Notificación Multimedia:** Controles de reproducción integrados en la barra de notificaciones con estilo `MediaStyle`.
* **🔀 Modos de Reproducción:** Funciones de **Bucle (Repeat)** y **Aleatorio (Shuffle)** inteligente (evita repetir la misma canción).

## 🛠️ Tecnologías Utilizadas

* **Kotlin**: Lenguaje principal de desarrollo.
* **Android Jetpack**: Componentes de arquitectura y UI.
* **MediaPlayer API**: Motor de reproducción de audio nativo.
* **MediaSessionCompat**: Control de eventos multimedia y hardware externo.
* **RecyclerView & DiffUtil**: Para una gestión de listas fluida y eficiente.
* **Version Catalogs (libs.toml)**: Gestión de dependencias moderna.

## 🚀 Instalación y Uso

1. **Clonar el repositorio** o descargar el código.
2. **Abrir con Android Studio** (Ladybug o superior recomendado).
3. **Cargar música**:
   - Crea una carpeta llamada `CarfokMusic` dentro de la carpeta `Music` de tu dispositivo.
   - Copia tus archivos de audio allí.
4. **Permisos**: Al iniciar, la app solicitará permiso para leer archivos y enviar notificaciones (en Android 13+).

## 📸 Capturas de Pantalla

| Lista de Canciones | Reproductor | Notificación |
| :---: | :---: | :---: |
| ![Lista](https://via.placeholder.com/200x400?text=Lista+Dark) | ![Player](https://via.placeholder.com/200x400?text=Player+Controls) | ![Notificación](https://via.placeholder.com/200x100?text=Media+Style) |

---
Desarrollado por **Carfok**
