package com.example.carfokreproductor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
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
    private lateinit var ivAlbumArt: ImageView

    // Variables de Lógica
    private var songList: ArrayList<String> = arrayListOf()
    private var folderPath: String? = null
    private var currentPosition: Int = 0
    private var isRepeat: Boolean = false
    private var isShuffle: Boolean = false

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
        ivAlbumArt = findViewById(R.id.ivAlbumArt)
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
            isRepeat = !isRepeat
            btnRepeat.setColorFilter(if (isRepeat) Color.CYAN else Color.WHITE)
        }

        btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            btnShuffle.setColorFilter(if (isShuffle) Color.CYAN else Color.WHITE)
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
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun playSongFromIntent() {
        if (folderPath != null && songList.isNotEmpty()) {
            val songName = songList[currentPosition]
            val fullPath = File(folderPath, songName).absolutePath

            tvTitle.text = songName
            musicService?.startMusic(fullPath)
            updateAlbumArt(fullPath)
            updatePlayPauseIcon()
        }
    }

    private fun updateAlbumArt(path: String?) {
        if (path == null) return
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val art = retriever.embeddedPicture
            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                ivAlbumArt.setImageBitmap(bitmap)
            } else {
                ivAlbumArt.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            }
        } catch (e: Exception) {
            ivAlbumArt.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
        } finally {
            retriever.release()
        }
    }

    // Nueva función para actualizar la pantalla cuando el servicio cambia de canción solo
    private fun updateUIFromService() {
        handler.postDelayed({
            musicService?.let {
                tvTitle.text = it.currentSongTitle
                updateAlbumArt(it.currentSongPath)
                updatePlayPauseIcon()
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
                    updateAlbumArt(service.currentSongPath)
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