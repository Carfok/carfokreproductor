package com.example.carfokreproductor

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.*

class ListActivity : AppCompatActivity() {

    private var songNamesFull: List<String> = listOf()
    private lateinit var adapter: SongAdapter
    private lateinit var playlistManager: PlaylistManager
    private lateinit var musicFolder: File

    // --- VARIABLES PARA EL MINI PLAYER ---
    private var musicService: MusicService? = null
    private var isBound = false
    private lateinit var miniPlayerLayout: CardView
    private lateinit var tvMiniTitle: TextView
    private lateinit var btnMiniPlayPause: ImageButton

    // Handler para actualizar la UI del mini player
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateTask: Runnable

    // Conexión con el servicio
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            updateMiniPlayerUI()
            startMiniPlayerUpdater()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        playlistManager = PlaylistManager(this)
        checkNotificationPermission()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSongs)
        val searchView = findViewById<SearchView>(R.id.searchView)
        val fabPlaylists = findViewById<FloatingActionButton>(R.id.fabPlaylists)
        val btnViewPlaylists = findViewById<ImageButton>(R.id.btnViewPlaylists)

        // Referencias Mini Player
        miniPlayerLayout = findViewById(R.id.layoutMiniPlayer)
        tvMiniTitle = findViewById(R.id.tvMiniTitle)
        tvMiniTitle.isSelected = true
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause)

        recyclerView.layoutManager = LinearLayoutManager(this)

        musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CarfokMusic")
        if (!musicFolder.exists()) musicFolder.mkdirs()

        loadSongs()

        adapter = SongAdapter(
            ArrayList(songNamesFull),
            onItemClick = { songName -> playSong(songName) },
            onOptionClick = { songName -> showAddToPlaylistDialog(songName) }
        )
        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return true
            }
        })

        btnViewPlaylists.setOnClickListener {
            startActivity(Intent(this, PlaylistActivity::class.java))
        }

        fabPlaylists.setOnClickListener {
            showCreatePlaylistDialog()
        }

        // --- LÓGICA MINI PLAYER ---

        // Clic en el Mini Player -> Abre PlayerActivity (para ver la carátula grande, barra, etc)
        miniPlayerLayout.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            // No pasamos lista nueva, PlayerActivity detectará que el servicio ya tiene una
            startActivity(intent)
        }

        // Clic en Play/Pause del Mini Player
        btnMiniPlayPause.setOnClickListener {
            musicService?.playPause()
            updateMiniPlayerUI()
        }
    }

    override fun onStart() {
        super.onStart()
        // Conectar al servicio para ver si hay música sonando
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        // Dejar de actualizar UI cuando la app no se ve
        handler.removeCallbacks(updateTask)
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    // Tarea repetitiva para detectar cambios de canción (ej. si se acaba y pasa a la siguiente)
    private fun startMiniPlayerUpdater() {
        updateTask = Runnable {
            updateMiniPlayerUI()
            handler.postDelayed(updateTask, 1000) // Revisar cada segundo
        }
        handler.post(updateTask)
    }

    private fun updateMiniPlayerUI() {
        musicService?.let { service ->
            // Solo mostramos el mini player si hay una canción cargada (ruta no nula)
            if (service.currentSongPath != null) {
                miniPlayerLayout.visibility = View.VISIBLE
                tvMiniTitle.text = service.currentSongTitle

                val isPlaying = service.isPlaying()
                btnMiniPlayPause.setImageResource(
                    if (isPlaying) R.drawable.pause_circle_24px else R.drawable.play_circle_24px
                )
            } else {
                miniPlayerLayout.visibility = View.GONE
            }
        }
    }

    // ... (El resto de funciones: loadSongs, playSong, Dialogs, filter, Permissions, Adapter...)
    // ... (Copia aquí el resto de tus funciones existentes sin cambios) ...

    private fun loadSongs() {
        val extensionesAceptadas = listOf("mp3", "wav", "aac", "ogg", "m4a", "flac")
        val songFiles = musicFolder.listFiles { file ->
            file.isFile && extensionesAceptadas.contains(file.extension.lowercase())
        }?.sortedBy { it.name } ?: emptyList()
        songNamesFull = songFiles.map { it.name }
    }

    private fun playSong(songName: String) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putStringArrayListExtra("SONG_LIST", ArrayList(songNamesFull))
        intent.putExtra("FOLDER_PATH", musicFolder.absolutePath)
        intent.putExtra("POSITION", songNamesFull.indexOf(songName))
        startActivity(intent)
    }

    private fun showCreatePlaylistDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nueva Lista de Reproducción")
        val input = EditText(this)
        input.hint = "Nombre de la lista"
        builder.setView(input)
        builder.setPositiveButton("Crear") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                if (playlistManager.createPlaylist(name)) {
                    Toast.makeText(this, "Lista '$name' creada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "La lista ya existe", Toast.LENGTH_LONG).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showAddToPlaylistDialog(songName: String) {
        val playlists = playlistManager.getPlaylistNames()
        if (playlists.isEmpty()) {
            Toast.makeText(this, "No tienes listas creadas.", Toast.LENGTH_SHORT).show()
            return
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Añadir a lista...")
        val playlistArray = playlists.toTypedArray()
        builder.setItems(playlistArray) { _, which ->
            val selectedPlaylist = playlistArray[which]
            val fullPath = File(musicFolder, songName).absolutePath
            playlistManager.addSongToPlaylist(selectedPlaylist, fullPath)
            Toast.makeText(this, "Añadida a $selectedPlaylist", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun filter(text: String?) {
        val filteredList = ArrayList<String>()
        for (item in songNamesFull) {
            if (item.lowercase(Locale.ROOT).contains(text?.lowercase(Locale.ROOT) ?: "")) {
                filteredList.add(item)
            }
        }
        adapter.updateList(filteredList)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    class SongAdapter(
        private var songs: ArrayList<String>,
        private val onItemClick: (String) -> Unit,
        private val onOptionClick: (String) -> Unit
    ) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvSongTitle)
            val btnOptions: ImageButton = view.findViewById(R.id.btnSongOptions)
        }

        fun updateList(newList: List<String>) {
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = songs.size
                override fun getNewListSize(): Int = newList.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean = songs[oldPos] == newList[newPos]
                override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean = songs[oldPos] == newList[newPos]
            }
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            songs.clear()
            songs.addAll(newList)
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val songName = songs[position]
            holder.tvTitle.text = songName
            holder.itemView.setOnClickListener { onItemClick(songName) }
            holder.btnOptions.setOnClickListener { onOptionClick(songName) }
        }

        override fun getItemCount() = songs.size
    }
}