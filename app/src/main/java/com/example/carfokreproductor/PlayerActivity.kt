package com.example.carfokreproductor

import java.io.File
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.core.graphics.toColorInt
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build

class PlayerActivity : AppCompatActivity() {

    private lateinit var mediaSession: MediaSessionCompat
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var seekBar: SeekBar
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private lateinit var tvTitle: TextView
    private lateinit var btnPlayPause: ImageButton

    private var songList: ArrayList<String> = arrayListOf()
    private var folderPath: String? = null
    private var currentPosition: Int = 0
    private var isRepeat: Boolean = false
    private var isShuffle: Boolean = false

    // ID del canal de notificación
    private val CHANNEL_ID = "carfok_music_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        songList = intent.getStringArrayListExtra("SONG_LIST") ?: arrayListOf()
        folderPath = intent.getStringExtra("FOLDER_PATH")
        currentPosition = intent.getIntExtra("POSITION", 0)

        tvTitle = findViewById(R.id.tvPlayerTitle)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnRepeat = findViewById<ImageButton>(R.id.btnRepeat)
        val btnShuffle = findViewById<ImageButton>(R.id.btnShuffle)
        seekBar = findViewById(R.id.playerSeekBar)

        // Crear el canal de notificación
        createNotificationChannel()
        setupMediaSession()

        btnPlayPause.setOnClickListener { togglePlayPause() }

        btnRepeat.setOnClickListener {
            isRepeat = !isRepeat
            btnRepeat.setColorFilter(if (isRepeat) "#2196F3".toColorInt() else Color.WHITE)
        }

        btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            btnShuffle.setColorFilter(if (isShuffle) "#2196F3".toColorInt() else Color.WHITE)
        }

        btnNext.setOnClickListener { nextSong() }
        btnPrev.setOnClickListener { prevSong() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(p)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        playSong()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción de Música",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles del reproductor Carfok"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(isPlaying: Boolean) {
        val songName = songList[currentPosition]

        // Intent para que al tocar la notificación vuelva a la app
        val intent = Intent(this, PlayerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.play_circle_24px) // Asegúrate que este icono existe
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.play_circle_24px))
            .setContentTitle(songName)
            .setContentText("Carfok Music Player")
            .setContentIntent(pendingIntent)
            // Estilo Multimedia
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            // Botón Anterior (Acción 0)
            .addAction(R.drawable.skip_previous_24px, "Anterior", null)
            // Botón Play/Pause (Acción 1)
            .addAction(
                if (isPlaying) R.drawable.pause_circle_24px else R.drawable.play_circle_24px,
                if (isPlaying) "Pausa" else "Play",
                null
            )
            // Botón Siguiente (Acción 2)
            .addAction(R.drawable.skip_next_24px, "Siguiente", null)
            .setOngoing(isPlaying) // La notificación no se puede borrar si está sonando
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "CarfokMusicSession")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { togglePlayPause() }
            override fun onPause() { togglePlayPause() }
            override fun onSkipToNext() { nextSong() }
            override fun onSkipToPrevious() { prevSong() }
            override fun onSeekTo(pos: Long) { mediaPlayer?.seekTo(pos.toInt()) }
        })
        mediaSession.isActive = true
    }

    private fun togglePlayPause() {
        runOnUiThread {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    btnPlayPause.setImageResource(R.drawable.play_circle_24px)
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    showNotification(false) // Actualizar notificación a PAUSA
                } else {
                    mp.start()
                    btnPlayPause.setImageResource(R.drawable.pause_circle_24px)
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    showNotification(true) // Actualizar notificación a PLAY
                    updateSeekBar()
                }
            }
        }
    }

    private fun playSong() {
        if (songList.isEmpty() || folderPath == null) return
        val songName = songList[currentPosition]
        val fullPath = File(folderPath, songName).absolutePath

        runOnUiThread {
            tvTitle.text = songName
            btnPlayPause.setImageResource(R.drawable.pause_circle_24px)
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(fullPath)
                prepare()
                start()
                seekBar.max = duration
                updateSeekBar()

                setOnCompletionListener {
                    if (isRepeat) playSong() else nextSong()
                }
            }
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            showNotification(true) // Mostrar notificación al iniciar nueva canción
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun nextSong() {
        if (songList.isEmpty()) return
        if (isShuffle && songList.size > 1) {
            var newPosition: Int
            do {
                newPosition = (0 until songList.size).random()
            } while (newPosition == currentPosition)
            currentPosition = newPosition
        } else {
            currentPosition = (currentPosition + 1) % songList.size
        }
        playSong()
    }

    private fun prevSong() {
        if (songList.isEmpty()) return
        currentPosition = if (currentPosition > 0) currentPosition - 1 else songList.size - 1
        playSong()
    }

    private fun updatePlaybackState(state: Int) {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, mediaPlayer?.currentPosition?.toLong() ?: 0L, 1.0f)
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun updateSeekBar() {
        handler.removeCallbacksAndMessages(null)
        runnable = Runnable {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    seekBar.progress = it.currentPosition
                    handler.postDelayed(runnable, 500)
                }
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        handler.removeCallbacksAndMessages(null)
        // Quitar la notificación al cerrar la app
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(1)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}