package com.example.carfokreproductor

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSongs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 1. DEFINIR LA RUTA ÚNICA (Pública)
        val folderPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "CarfokMusic"
        )

        // 2. CREAR LA CARPETA SI NO EXISTE
        if (!folderPath.exists()) {
            folderPath.mkdirs()
        }

        // 3. LEER ARCHIVOS
        val extensionesAceptadas = listOf("mp3", "wav", "aac", "ogg", "m4a", "flac")
        val songFiles = folderPath.listFiles { file ->
            file.isFile && extensionesAceptadas.contains(file.extension.lowercase())
        }?.sortedBy { it.name } ?: emptyList()

        val songNames = songFiles.map { it.name }

        // 4. CONFIGURAR ADAPTADOR
        recyclerView.adapter = SongAdapter(songNames) { songName ->
            val intent = Intent(this, PlayerActivity::class.java)

            // IMPORTANTE: Usamos la misma 'folderPath' que definimos arriba
            intent.putStringArrayListExtra("SONG_LIST", ArrayList(songNames))
            intent.putExtra("FOLDER_PATH", folderPath.absolutePath) // RUTA CORRECTA
            intent.putExtra("POSITION", songNames.indexOf(songName))

            startActivity(intent)
        }
    }

    // --- CLASE INTERNA DEL ADAPTADOR ---
    class SongAdapter(
        private val songs: List<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvSongTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val songName = songs[position]
            holder.tvTitle.text = songName
            holder.itemView.setOnClickListener { onItemClick(songName) }
        }

        override fun getItemCount() = songs.size
    }
}