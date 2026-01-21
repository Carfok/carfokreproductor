package com.example.carfokreproductor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PlayerActivity : AppCompatActivity() {

    // Variables del Servicio
    private var musicService: MusicService? = null
    private var isBound = false

    // UI
    private lateinit var seekBar: SeekBar
    private lateinit var tvTitle: TextView
    private lateinit var btnPlayPause: ImageButton

    // Variables de Lógica
    private var songList: ArrayList<String> = arrayListOf()
    private var folderPath: String? = null
    private var currentPosition: Int = 0

    // Hilo para actualizar la barra de progreso
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    // --- CONEXIÓN CON EL SERVICIO ---
    private val connection = object : ServiceConnection {
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            // 1. IMPORTANTE: Pasamos la lista y la posición al servicio al conectar
            folderPath?.let {
                musicService?.setList(songList, it, currentPosition)
            }

            // 2. Iniciamos la música
            playSongFromIntent()
            startSeekBarUpdater()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // 1. Recibir datos de la lista
        songList = intent.getStringArrayListExtra("SONG_LIST") ?: arrayListOf()
        folderPath = intent.getStringExtra("FOLDER_PATH")
        currentPosition = intent.getIntExtra("POSITION", 0)

        // 2. Inicializar Vistas
        tvTitle = findViewById(R.id.tvPlayerTitle)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.playerSeekBar)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnRepeat = findViewById<ImageButton>(R.id.btnRepeat)
        val btnShuffle = findViewById<ImageButton>(R.id.btnShuffle)

        // 3. Configurar Botones (Ahora usan la lógica del servicio)
        btnPlayPause.setOnClickListener {
            musicService?.playPause()
            updatePlayPauseIcon()
        }

        btnNext.setOnClickListener {
            musicService?.playNext()
            updateUIFromService()
        }

        btnPrev.setOnClickListener {
            musicService?.playPrevious()
            updateUIFromService()
        }

        btnRepeat.setOnClickListener {
            musicService?.let {
                it.isRepeat = !it.isRepeat
                btnRepeat.setColorFilter(if (it.isRepeat) Color.CYAN else Color.WHITE)
            }
        }

        btnShuffle.setOnClickListener {
            musicService?.let {
                it.isShuffle = !it.isShuffle
                btnShuffle.setColorFilter(if (it.isShuffle) Color.CYAN else Color.WHITE)
            }
        }

        // 4. Configurar SeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) musicService?.seekTo(p)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        // 5. Iniciar y conectar el Servicio
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun playSongFromIntent() {
        if (folderPath != null && songList.isNotEmpty()) {
            val songName = songList[currentPosition]
            val fullPath = File(folderPath, songName).absolutePath

            tvTitle.text = songName
            musicService?.startMusic(fullPath)
            updatePlayPauseIcon()
        }
    }

    // Nueva función para actualizar la pantalla cuando el servicio cambia de canción solo
    private fun updateUIFromService() {
        handler.postDelayed({
            musicService?.let {
                tvTitle.text = it.currentSongTitle
                updatePlayPauseIcon()
                
                // Actualizar colores de botones según el estado del servicio
                findViewById<ImageButton>(R.id.btnRepeat).setColorFilter(if (it.isRepeat) Color.CYAN else Color.WHITE)
                findViewById<ImageButton>(R.id.btnShuffle).setColorFilter(if (it.isShuffle) Color.CYAN else Color.WHITE)
            }
        }, 200)
    }

    private fun updatePlayPauseIcon() {
        handler.postDelayed({
            val isPlaying = musicService?.isPlaying() ?: false
            btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.pause_circle_24px else R.drawable.play_circle_24px
            )
        }, 100)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startSeekBarUpdater() {
        runnable = Runnable {
            musicService?.let { service ->
                seekBar.max = service.getDuration()
                seekBar.progress = service.getCurrentPosition()

                // Si el título en pantalla es distinto al que suena (porque cambió en la notificación)
                if (tvTitle.text != service.currentSongTitle) {
                    tvTitle.text = service.currentSongTitle
                    // También asegurar que los botones reflejen el estado (por si cambiaron fuera de la Activity)
                    findViewById<ImageButton>(R.id.btnRepeat).setColorFilter(if (service.isRepeat) Color.CYAN else Color.WHITE)
                    findViewById<ImageButton>(R.id.btnShuffle).setColorFilter(if (service.isShuffle) Color.CYAN else Color.WHITE)
                }
            }
            handler.postDelayed(runnable, 500)
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}