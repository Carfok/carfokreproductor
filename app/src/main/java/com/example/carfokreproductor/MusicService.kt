package com.example.carfokreproductor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import java.io.File

class MusicService : Service(), MediaPlayer.OnCompletionListener {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat

    // Datos de reproducción
    var currentSongTitle: String = ""
    var currentSongPath: String? = null

    // --- CEREBRO DE LA LISTA ---
    private var songList: ArrayList<String> = arrayListOf()
    private var currentFolder: String = ""
    private var currentIndex: Int = -1

    // --- ESTADOS ---
    var isShuffle = false
    var isRepeat = false

    companion object {
        const val CHANNEL_ID = "MusicChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause" // No usado explícitamente pero útil
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREVIOUS = "action_prev"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "MusicServiceTag")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { playPause() }
            override fun onPause() { playPause() }
            override fun onSkipToNext() { playNext() }
            override fun onSkipToPrevious() { playPrevious() }
        })
        mediaSession.isActive = true
    }

    // Recibimos la lista desde la Activity
    fun setList(songs: ArrayList<String>, folderPath: String, position: Int) {
        this.songList = songs
        this.currentFolder = folderPath
        this.currentIndex = position
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playPause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
        }
        return START_STICKY // Importante para que el sistema intente revivirlo si muere
    }

    fun startMusic(path: String) {
        currentSongPath = path
        // Extraer nombre del archivo para el título
        currentSongTitle = File(path).nameWithoutExtension

        // Liberar anterior si existe
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(path)
            prepare() // Síncrono está bien para archivos locales

            // IMPORTANTE: WakeLock para que no se apague en background
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

            // IMPORTANTE: Listener para cuando acaba la canción
            setOnCompletionListener(this@MusicService)

            start()
        }

        showNotification(true)
    }

    // --- LÓGICA AUTOMÁTICA (Cuando acaba una canción) ---
    override fun onCompletion(mp: MediaPlayer?) {
        if (isRepeat) {
            // Si es repetición, tocamos la misma
            currentSongPath?.let { startMusic(it) }
        } else {
            // Si no, pasamos a la siguiente (la lógica de shuffle está dentro de playNext)
            playNext()
        }
    }

    fun playPause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            showNotification(false)
        } else {
            mediaPlayer?.start()
            showNotification(true)
        }
    }

    fun playNext() {
        if (songList.isEmpty()) return

        if (isShuffle) {
            // Elegir aleatorio
            currentIndex = (songList.indices).random()
        } else {
            // Siguiente normal
            currentIndex = (currentIndex + 1) % songList.size
        }

        playSongAtIndex(currentIndex)
    }

    fun playPrevious() {
        if (songList.isEmpty()) return

        // El botón Previous normalmente no usa shuffle, va al anterior lógico o físico
        currentIndex = if (currentIndex > 0) currentIndex - 1 else songList.size - 1
        playSongAtIndex(currentIndex)
    }

    private fun playSongAtIndex(index: Int) {
        val songName = songList[index]
        val fullPath = File(currentFolder, songName).absolutePath
        startMusic(fullPath)
    }

    // Funciones para buscar (Seekbar)
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun seekTo(pos: Int) { mediaPlayer?.seekTo(pos) }
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    // --- NOTIFICACIÓN ---
    private fun showNotification(isPlaying: Boolean) {
        val playPauseIcon = if (isPlaying) R.drawable.pause_circle_24px else R.drawable.play_circle_24px

        // Intent para abrir la app al tocar la notificación
        val contentIntent = Intent(this, PlayerActivity::class.java).apply {
            // Flags para no recrear la actividad si ya existe
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Intents para botones
        val prevIntent = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS), PendingIntent.FLAG_IMMUTABLE)
        val playIntent = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).setAction(ACTION_PLAY), PendingIntent.FLAG_IMMUTABLE)
        val nextIntent = PendingIntent.getService(this, 3, Intent(this, MusicService::class.java).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE)

        // Carátula
        val largeIcon = currentSongPath?.let { getAlbumArt(it) }
            ?: BitmapFactory.decodeResource(resources, R.drawable.play_circle_24px)

        // Actualizar estado MediaSession
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            .setState(state, getCurrentPosition().toLong(), 1f)
            .build())

        // Actualizar Metadatos (Pantalla de bloqueo)
        mediaSession.setMetadata(MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSongTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Carfok Music")
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, largeIcon)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration().toLong())
            .build())

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSongTitle)
            .setContentText("Carfok Music") // Artista genérico
            .setSmallIcon(R.drawable.play_circle_24px)
            .setLargeIcon(largeIcon)
            .setContentIntent(contentPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .addAction(R.drawable.skip_previous_24px, "Prev", prevIntent)
            .addAction(playPauseIcon, "Play", playIntent)
            .addAction(R.drawable.skip_next_24px, "Next", nextIntent)
            .setOngoing(isPlaying) // Si suena, no se puede quitar deslizando
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    // Función auxiliar para sacar imagen
    private fun getAlbumArt(path: String): android.graphics.Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val art = retriever.embeddedPicture
            if (art != null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
        } catch (e: Exception) { null } finally { retriever.release() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Controls for music playback"
            channel.setShowBadge(false)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaSession.release()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}