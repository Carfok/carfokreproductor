package com.example.carfokreproductor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private var musicService: MusicService? = null
    private var isBound = false

    // UI
    private lateinit var seekBar: SeekBar
    private lateinit var tvTitle: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnShuffle: ImageButton
    private lateinit var btnRepeat: ImageButton
    private lateinit var ivAlbumArt: ImageView

    // Datos temporales (solo para el primer play)
    private var songList: ArrayList<String> = arrayListOf()
    private var folderPath: String? = null
    private var currentPosition: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            // 1. Configuramos la lista en el servicio SIEMPRE
            if (folderPath != null && songList.isNotEmpty()) {
                musicService?.setList(songList, folderPath!!, currentPosition)

                // Solo iniciamos música si la canción solicitada es diferente a la que suena
                // o si no está sonando nada.
                val songName = songList[currentPosition]
                val fullPath = File(folderPath, songName).absolutePath

                if (musicService?.currentSongPath != fullPath) {
                    musicService?.startMusic(fullPath)
                }
            }

            // 2. Actualizamos la UI inicial (Shuffle/Repeat/Iconos)
            updateUIState()
            startSeekBarUpdater()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ocultar ActionBar si no lo has hecho en themes
        supportActionBar?.hide()
        setContentView(R.layout.activity_player)

        // Recibir datos del Intent
        songList = intent.getStringArrayListExtra("SONG_LIST") ?: arrayListOf()
        folderPath = intent.getStringExtra("FOLDER_PATH")
        currentPosition = intent.getIntExtra("POSITION", 0)

        initViews()

        // Iniciar Servicio
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvPlayerTitle)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.playerSeekBar)
        btnShuffle = findViewById(R.id.btnShuffle)
        btnRepeat = findViewById(R.id.btnRepeat)
        ivAlbumArt = findViewById(R.id.ivAlbumArt) // Asegúrate de tener este ID en el XML nuevo
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)

        btnPlayPause.setOnClickListener {
            musicService?.playPause()
            updatePlayPauseIcon()
        }

        btnNext.setOnClickListener {
            musicService?.playNext()
            updateUIState() // Actualiza título e imagen inmediatamente
        }

        btnPrev.setOnClickListener {
            musicService?.playPrevious()
            updateUIState()
        }

        // --- LÓGICA CORREGIDA: Delegar al servicio ---
        btnShuffle.setOnClickListener {
            musicService?.let { service ->
                service.isShuffle = !service.isShuffle
                updateShuffleButton()
            }
        }

        btnRepeat.setOnClickListener {
            musicService?.let { service ->
                service.isRepeat = !service.isRepeat
                updateRepeatButton()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) musicService?.seekTo(p)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    // Actualiza TODO lo visual basándose en el estado real del Servicio
    private fun updateUIState() {
        musicService?.let { service ->
            tvTitle.text = service.currentSongTitle
            updateAlbumArt(service.currentSongPath)
            updatePlayPauseIcon()
            updateShuffleButton()
            updateRepeatButton()
        }
    }

    private fun updateShuffleButton() {
        val isShuffle = musicService?.isShuffle ?: false
        btnShuffle.setColorFilter(if (isShuffle) Color.CYAN else Color.WHITE)
        btnShuffle.alpha = if (isShuffle) 1.0f else 0.7f
    }

    private fun updateRepeatButton() {
        val isRepeat = musicService?.isRepeat ?: false
        btnRepeat.setColorFilter(if (isRepeat) Color.CYAN else Color.WHITE)
        btnRepeat.alpha = if (isRepeat) 1.0f else 0.7f
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = musicService?.isPlaying() ?: false
        btnPlayPause.setImageResource(if (isPlaying) R.drawable.pause_circle_24px else R.drawable.play_circle_24px)
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
                ivAlbumArt.setImageResource(android.R.drawable.ic_lock_silent_mode_off) // Tu icono por defecto
            }
        } catch (e: Exception) {
            ivAlbumArt.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
        } finally {
            retriever.release()
        }
    }

    private fun startSeekBarUpdater() {
        runnable = Runnable {
            musicService?.let { service ->
                seekBar.max = service.getDuration()
                seekBar.progress = service.getCurrentPosition()

                // Sincronización automática: Si la canción cambió sola (por onCompletion), actualizamos la UI
                if (tvTitle.text != service.currentSongTitle) {
                    updateUIState()
                }

                // Actualizar icono play/pause por si cambió desde la notificación
                updatePlayPauseIcon()
            }
            handler.postDelayed(runnable, 500)
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        if (isBound) unbindService(connection)
    }
}