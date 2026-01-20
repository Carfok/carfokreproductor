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

class PlayerActivity : AppCompatActivity() {

    private lateinit var mediaSession: MediaSessionCompat
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var seekBar: SeekBar
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    // Vistas (Declararlas aquí evita errores de hilos)
    private lateinit var tvTitle: TextView
    private lateinit var btnPlayPause: ImageButton

    private var songList: ArrayList<String> = arrayListOf()
    private var folderPath: String? = null
    private var currentPosition: Int = 0
    private var isRepeat: Boolean = false
    private var isShuffle: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // 1. Recuperar datos
        songList = intent.getStringArrayListExtra("SONG_LIST") ?: arrayListOf()
        folderPath = intent.getStringExtra("FOLDER_PATH")
        currentPosition = intent.getIntExtra("POSITION", 0)

        // 2. Vincular Vistas
        tvTitle = findViewById(R.id.tvPlayerTitle)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnRepeat = findViewById<ImageButton>(R.id.btnRepeat)
        val btnShuffle = findViewById<ImageButton>(R.id.btnShuffle)
        seekBar = findViewById(R.id.playerSeekBar)

        // 3. Inicializar Sesión
        setupMediaSession()

        // 4. Lógica de Botones
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

        // 5. Iniciar
        playSong()
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
        // Ejecutamos siempre en el hilo principal para la UI
        runOnUiThread {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    btnPlayPause.setImageResource(R.drawable.play_circle_24px)
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                } else {
                    mp.start()
                    btnPlayPause.setImageResource(R.drawable.pause_circle_24px)
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    updateSeekBar()
                }
            }
        }
    }

    private fun playSong() {
        if (songList.isEmpty() || folderPath == null) return

        val songName = songList[currentPosition]
        val fullPath = File(folderPath, songName).absolutePath

        // Actualizar UI inmediatamente
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
        handler.removeCallbacksAndMessages(null) // Limpiar handlers previos
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
        mediaPlayer?.release()
        mediaPlayer = null
    }
}