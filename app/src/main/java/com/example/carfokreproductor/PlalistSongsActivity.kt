package com.example.carfokreproductor

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
    private lateinit var adapter: ListActivity.SongAdapter // Reutilizamos el adaptador de ListActivity
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

        // Reutilizamos el SongAdapter que ya creamos en ListActivity
        // Nota: Para los 3 puntos, aquí daremos opción de BORRAR de la lista
        adapter = ListActivity.SongAdapter(
            songNames,
            onItemClick = { songName ->
                playSong(songName)
            },
            onOptionClick = { songName ->
                showRemoveDialog(songName)
            }
        )
        rvSongs.adapter = adapter
    }

    private fun loadSongs() {
        // 1. Obtener las rutas completas guardadas en el JSON
        val savedPaths = playlistManager.getSongsInPlaylist(playlistName)

        // 2. Filtrar solo las que existen físicamente (por si borraste el archivo mp3)
        val validFiles = savedPaths.map { File(it) }.filter { it.exists() }

        songPaths = validFiles.map { it.absolutePath }
        songNames = ArrayList(validFiles.map { it.name })

        val tvEmpty = findViewById<TextView>(R.id.tvEmptySongs)
        tvEmpty.visibility = if (songNames.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun playSong(songName: String) {
        val position = songNames.indexOf(songName)
        if (position == -1) return

        // Obtenemos la carpeta contenedora de la canción (asumimos que todas están en CarfokMusic
        // o extraemos la ruta padre de la primera canción si queremos ser más exactos)
        val musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CarfokMusic")

        val intent = Intent(this, PlayerActivity::class.java)
        // TRUCO: Enviamos SOLO los nombres de esta playlist
        intent.putStringArrayListExtra("SONG_LIST", songNames)
        intent.putExtra("FOLDER_PATH", musicFolder.absolutePath)
        intent.putExtra("POSITION", position)
        startActivity(intent)
    }

    private fun showRemoveDialog(songName: String) {
        AlertDialog.Builder(this)
            .setTitle("Quitar canción")
            .setMessage("¿Quitar '$songName' de la lista $playlistName?")
            .setPositiveButton("Quitar") { _, _ ->
                // Necesitamos la ruta completa para borrarla del manager
                val index = songNames.indexOf(songName)
                if (index != -1) {
                    val fullPath = songPaths[index]
                    playlistManager.removeSongFromPlaylist(playlistName, fullPath)

                    // Recargar la lista visualmente
                    loadSongs()
                    adapter.updateList(songNames)
                    Toast.makeText(this, "Eliminada de la lista", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Necesitamos asegurarnos de que la lista se refresque si volvemos atrás
    override fun onResume() {
        super.onResume()
        loadSongs()
        if (::adapter.isInitialized) {
            adapter.updateList(songNames)
        }
    }
}