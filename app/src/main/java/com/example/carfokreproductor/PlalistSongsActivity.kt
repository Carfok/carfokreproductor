package com.example.carfokreproductor

import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.Collections

class PlaylistSongsActivity : AppCompatActivity() {

    private lateinit var playlistManager: PlaylistManager
    private lateinit var adapter: ListActivity.SongAdapter
    private var playlistName: String = ""
    private var songPaths: ArrayList<String> = arrayListOf()
    private var songNames: ArrayList<String> = arrayListOf()

    // --- CONEXIÓN AL SERVICIO ---
    private var musicService: MusicService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_songs)

        playlistManager = PlaylistManager(this)
        playlistName = intent.getStringExtra("PLAYLIST_NAME") ?: "Lista"

        val tvTitle = findViewById<TextView>(R.id.tvPlaylistTitle)
        tvTitle.text = playlistName

        val rvSongs = findViewById<RecyclerView>(R.id.rvPlaylistSongs)
        rvSongs.layoutManager = LinearLayoutManager(this)

        loadSongs()

        // Carpeta base para el adaptador (usada para encontrar carátulas)
        val musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CarfokMusic")

        adapter = ListActivity.SongAdapter(
            songNames,
            musicFolder.absolutePath,
            onItemClick = { songName: String -> playSong(songName) },
            onOptionClick = { songName: String -> showRemoveDialog(songName) }
        )
        rvSongs.adapter = adapter

        // IMPLEMENTACIÓN DEL DRAG AND DROP (Reordenar)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition

                Collections.swap(songNames, fromPos, toPos)
                Collections.swap(songPaths, fromPos, toPos)

                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // Al soltar, guardamos el nuevo orden
                playlistManager.savePlaylistOrder(playlistName, songPaths)
                // Si la música está sonando y es esta lista, actualizamos el servicio para que el orden sea el nuevo
                if (isBound && musicService != null) {
                    musicService?.setList(songNames, musicFolder.absolutePath, -1) // -1 indica que no cambie la canción actual pero sí el "cerebro" de la lista
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(rvSongs)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun loadSongs() {
        val savedPaths = playlistManager.getSongsInPlaylist(playlistName)
        val validFiles = savedPaths.map { File(it) }.filter { it.exists() }

        songPaths = ArrayList(validFiles.map { it.absolutePath })
        songNames = ArrayList(validFiles.map { it.name })

        val tvEmpty = findViewById<TextView>(R.id.tvEmptySongs)
        tvEmpty.visibility = if (songNames.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun playSong(songName: String) {
        val position = songNames.indexOf(songName)
        if (position == -1) return

        val musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CarfokMusic")
        val fullPath = File(musicFolder, songName).absolutePath

        // 1. Actualizar el servicio de inmediato
        if (isBound && musicService != null) {
            musicService?.setList(songNames, musicFolder.absolutePath, position)
            musicService?.startMusic(fullPath)
        }

        // 2. Ir al reproductor (o traerlo al frente)
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putStringArrayListExtra("SONG_LIST", songNames)
            putExtra("FOLDER_PATH", musicFolder.absolutePath)
            putExtra("POSITION", position)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun showRemoveDialog(songName: String) {
        AlertDialog.Builder(this)
            .setTitle("Quitar canción")
            .setMessage("¿Quitar '$songName' de la lista $playlistName?")
            .setPositiveButton("Quitar") { _: DialogInterface, _: Int ->
                val index = songNames.indexOf(songName)
                if (index != -1) {
                    val fullPath = songPaths[index]
                    playlistManager.removeSongFromPlaylist(playlistName, fullPath)
                    loadSongs()
                    adapter.updateList(songNames)
                    Toast.makeText(this, "Eliminada de la lista", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // No recargamos todo si no es necesario para evitar que el scroll o el orden visual parpadeen
        val currentNames = ArrayList(songNames)
        loadSongs()
        if (currentNames != songNames) {
            adapter.updateList(songNames)
        }
    }
}
