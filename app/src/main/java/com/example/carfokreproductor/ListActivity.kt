package com.example.carfokreproductor

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*

class ListActivity : AppCompatActivity() {

    // Listas para manejar el filtrado
    private var songNamesFull: List<String> = listOf()
    private lateinit var adapter: SongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSongs)
        val searchView = findViewById<SearchView>(R.id.searchView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 1. RUTA Y LECTURA
        val folderPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CarfokMusic")
        if (!folderPath.exists()) folderPath.mkdirs()

        val extensionesAceptadas = listOf("mp3", "wav", "aac", "ogg", "m4a", "flac")
        val songFiles = folderPath.listFiles { file ->
            file.isFile && extensionesAceptadas.contains(file.extension.lowercase())
        }?.sortedBy { it.name } ?: emptyList()

        songNamesFull = songFiles.map { it.name }

        // 2. CONFIGURAR ADAPTADOR (Usamos una copia para la visualización inicial)
        adapter = SongAdapter(ArrayList(songNamesFull)) { songName ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putStringArrayListExtra("SONG_LIST", ArrayList(songNamesFull))
            intent.putExtra("FOLDER_PATH", folderPath.absolutePath)
            intent.putExtra("POSITION", songNamesFull.indexOf(songName))
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // 3. LÓGICA DEL BUSCADOR
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return true
            }
        })
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

    // --- ADAPTADOR ACTUALIZADO ---
    class SongAdapter(
        private var songs: MutableList<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvSongTitle)
        }

        fun updateList(newList: List<String>) {
            songs = newList.toMutableList()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
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