package com.example.carfokreproductor


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.io.File
import kotlin.random.Random

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = MusicBinder()

    // Datos de reproducción
    var currentSongPath: String? = null
    var currentSongTitle: String = "Desconocido"
    private var songList: List<String> = emptyList()
    private var currentPosition: Int = 0
    private var folderPath: String? = null

    // Estados de reproducción
    var isShuffle = false
    var isRepeat = false

    private lateinit var mediaSession: MediaSessionCompat
    private val CHANNEL_ID = "carfok_music_channel"
    private val NOTIFICATION_ID = 1

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.isActive = true

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPlay() { playPause() }
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPause() { playPause() }
            override fun onSeekTo(pos: Long) { seekTo(pos.toInt()) }
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onSkipToNext() { playNext() }
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onSkipToPrevious() { playPrevious() }
        })

        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder = binder

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            handleNotificationActions(action)
        }
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleNotificationActions(action: String) {
        when (action) {
            "ACTION_PLAY_PAUSE" -> playPause()
            "ACTION_NEXT" -> playNext()
            "ACTION_PREV" -> playPrevious()
        }
    }

    fun setList(list: List<String>, path: String, position: Int) {
        songList = list
        folderPath = path
        currentPosition = position
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startMusic(path: String) {
        currentSongTitle = File(path).nameWithoutExtension

        if (currentSongPath == path && mediaPlayer?.isPlaying == true) return

        releasePlayer()

        currentSongPath = path
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
            setOnCompletionListener { 
                if (isRepeat) {
                    startMusic(currentSongPath!!) // Repetir misma canción
                } else {
                    playNext() // Siguiente (normal o shuffle)
                }
            }
        }

        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        updateMetaData()
        showNotification(true)
    }

    private fun releasePlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun stopMusic() {
        releasePlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showNotification(isPlaying: Boolean) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val iconPlayPause = if (isPlaying) R.drawable.pause_circle_24px else R.drawable.play_circle_24px

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSongTitle)
            .setContentText("Carfok Music Player")
            .setSmallIcon(R.drawable.play_circle_24px)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.play_circle_24px))
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(R.drawable.skip_previous_24px, "Anterior", getPendingIntent("ACTION_PREV"))
            .addAction(iconPlayPause, if (isPlaying) "Pausa" else "Play", getPendingIntent("ACTION_PLAY_PAUSE"))
            .addAction(R.drawable.skip_next_24px, "Siguiente", getPendingIntent("ACTION_NEXT"))
            .build()

        if (isPlaying) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun playNext() {
        if (songList.isEmpty() || folderPath == null) return
        
        if (isShuffle) {
            currentPosition = Random.nextInt(songList.size)
        } else {
            currentPosition = (currentPosition + 1) % songList.size
        }

        val nextPath = File(folderPath, songList[currentPosition]).absolutePath
        startMusic(nextPath)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun playPrevious() {
        if (songList.isEmpty() || folderPath == null) return
        currentPosition = if (currentPosition > 0) currentPosition - 1 else songList.size - 1
        val prevPath = File(folderPath, songList[currentPosition]).absolutePath
        startMusic(prevPath)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun playPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                showNotification(false)
            } else {
                it.start()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                showNotification(true)
            }
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        val state = if (isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        updatePlaybackState(state)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Reproductor Carfok", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updatePlaybackState(state: Int) {
        if (mediaPlayer == null) return
        val position = mediaPlayer!!.currentPosition.toLong()
        val speed = if (state == PlaybackStateCompat.STATE_PLAYING) 1.0f else 0.0f

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, position, speed)

        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun updateMetaData() {
        val duration = mediaPlayer?.duration?.toLong() ?: 0L
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSongTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Carfok Music")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .build()

        mediaSession.setMetadata(builder)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        stopMusic()
    }
}