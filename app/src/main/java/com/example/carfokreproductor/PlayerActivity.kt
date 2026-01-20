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

class PlayerActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var seekBar: SeekBar
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    // Nuevas variables de control
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

        // Vincular Vistas
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnRepeat = findViewById<ImageButton>(R.id.btnRepeat)
        val btnShuffle = findViewById<ImageButton>(R.id.btnShuffle)
        val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
        seekBar = findViewById(R.id.playerSeekBar)

        // 2. LÓGICA ÚNICA DEL BOTÓN PLAY/PAUSE
        btnPlayPause.setOnClickListener {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    btnPlayPause.setImageResource(R.drawable.play_circle_24px)
                    // Animación opcional
                    btnPlayPause.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                        btnPlayPause.animate().scaleX(1f).scaleY(1f).setDuration(100)
                    }
                } else {
                    mp.start()
                    btnPlayPause.setImageResource(R.drawable.pause_circle_24px)
                    updateSeekBar()
                    // Animación opcional
                    btnPlayPause.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction {
                        btnPlayPause.animate().scaleX(1f).scaleY(1f).setDuration(100)
                    }
                }
            }
        }

        // 3. LÓGICA DE REPETIR Y SHUFFLE (Sin duplicados)
        btnRepeat.setOnClickListener {
            isRepeat = !isRepeat
            val color = if (isRepeat) "#2196F3".toColorInt() else Color.WHITE
            btnRepeat.setColorFilter(color)
        }

        btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            val color = if (isShuffle) "#2196F3".toColorInt() else Color.WHITE
            btnShuffle.setColorFilter(color)
        }

        // 4. OTROS CONTROLES
        btnNext.setOnClickListener { nextSong() }
        btnPrev.setOnClickListener { prevSong() }

        // SeekBar manual
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(p)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        // 5. Iniciar canción
        playSong()
    }
    private fun playSong() {
        val songName = songList[currentPosition]
        findViewById<TextView>(R.id.tvPlayerTitle).text = songName
        val fullPath = File(folderPath, songName).absolutePath

        val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
        btnPlayPause.setImageResource(R.drawable.pause_circle_24px)

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(fullPath)
                prepare()
                start()
                seekBar.max = duration
                updateSeekBar()

                // LÓGICA AUTOMÁTICA AL ACABAR
                setOnCompletionListener {
                    if (isRepeat) {
                        playSong() // Repetir la misma
                    } else {
                        nextSong() // Pasar a la siguiente
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun nextSong() {
        if (songList.isEmpty()) return // Seguridad: si no hay canciones, no hacer nada

        if (isShuffle) {
            // Si hay más de una canción, buscamos una que no sea la actual
            if (songList.size > 1) {
                var newPosition: Int
                do {
                    newPosition = (0 until songList.size).random()
                } while (newPosition == currentPosition) // Se repite si sale la misma
                currentPosition = newPosition
            }
            // Si solo hay una canción, se quedará en esa (no hay otra opción)
        } else {
            // Modo normal: siguiente en la lista (y vuelve al principio al terminar)
            currentPosition = (currentPosition + 1) % songList.size
        }

        playSong()
    }

    private fun prevSong() {
        if (songList.isEmpty()) return

        if (isShuffle && songList.size > 1) {
            var newPosition: Int
            do {
                newPosition = (0 until songList.size).random()
            } while (newPosition == currentPosition)
            currentPosition = newPosition
        } else {
            // Modo normal: anterior o última si está en la primera
            currentPosition = if (currentPosition > 0) currentPosition - 1 else songList.size - 1
        }

        playSong()
    }

    private fun updateSeekBar() {
        runnable = Runnable {
            mediaPlayer?.let {
                seekBar.progress = it.currentPosition
                handler.postDelayed(runnable, 500)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        mediaPlayer?.release()
    }
}