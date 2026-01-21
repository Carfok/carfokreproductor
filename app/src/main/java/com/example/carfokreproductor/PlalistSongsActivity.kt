package com.example.carfokreproductor

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PlaylistSongsActivity : AppCompatActivity() {

    private lateinit var playlistManager: PlaylistManager
    private lateinit var adapter: ListActivity.SongAdapter
    private var playlistName: String = ""
    private var songPaths: List<String> = listOf()
    private var songNames: ArrayList<String> = arrayListOf()

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

        // Obtenemos la carpeta de música por defecto para el adaptador
        val musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CarfokMusic")

        adapter = ListActivity.SongAdapter(
            songNames,
            musicFolder.absolutePath,
            onItemClick = { songName: String ->
                playSong(songName)
            },
            onOptionClick = { songName: String ->
                showRemoveDialog(songName)
            }
        )
        rvSongs.adapter = adapter
    }

    private fun loadSongs() {
        val savedPaths = playlistManager.getSongsInPlaylist(playlistName)
        val validFiles = savedPaths.map { File(it) }.filter { it.exists() }

        songPaths = validFiles.map { it.absolutePath }
        songNames = ArrayList(validFiles.map { it.name })

        val tvEmpty = findViewById<TextView>(R.id.tvEmptySongs)
        tvEmpty.visibility = if (songNames.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun playSong(songName: String) {
        val position = songNames.indexOf(songName)
        if (position == -1) return

        val musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CarfokMusic")

        val intent = Intent(this, PlayerActivity::class.java)
        intent.putStringArrayListExtra("SONG_LIST", songNames)
        intent.putExtra("FOLDER_PATH", musicFolder.absolutePath)
        intent.putExtra("POSITION", position)
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
        loadSongs()
        if (::adapter.isInitialized) {
            adapter.updateList(songNames)
        }
    }
}