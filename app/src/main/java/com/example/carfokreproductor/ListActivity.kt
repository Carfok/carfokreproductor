package com.example.carfokreproductor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*

class ListActivity : AppCompatActivity() {

    private var songNamesFull: List<String> = listOf()
    private lateinit var adapter: SongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        // --- BLOQUE DE PERMISOS PARA NOTIFICACIONES (Android 13+) ---
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

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSongs)
        val searchView = findViewById<SearchView>(R.id.searchView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val folderPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "CarfokMusic")
        if (!folderPath.exists()) folderPath.mkdirs()

        val extensionesAceptadas = listOf("mp3", "wav", "aac", "ogg", "m4a", "flac")
        val songFiles = folderPath.listFiles { file ->
            file.isFile && extensionesAceptadas.contains(file.extension.lowercase())
        }?.sortedBy { it.name } ?: emptyList()

        songNamesFull = songFiles.map { it.name }

        adapter = SongAdapter(ArrayList(songNamesFull)) { songName ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putStringArrayListExtra("SONG_LIST", ArrayList(songNamesFull))
            intent.putExtra("FOLDER_PATH", folderPath.absolutePath)
            intent.putExtra("POSITION", songNamesFull.indexOf(songName))
            startActivity(intent)
        }
        recyclerView.adapter = adapter

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

    class SongAdapter(
        private var songs: ArrayList<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvSongTitle)
        }

        fun updateList(newList: List<String>) {
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = songs.size
                override fun getNewListSize(): Int = newList.size

                override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                    // Comparamos si es la misma canción por el nombre
                    return songs[oldPos] == newList[newPos]
                }

                override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                    // En este caso, el contenido es el mismo que el item
                    return songs[oldPos] == newList[newPos]
                }
            }

            val diffResult = DiffUtil.calculateDiff(diffCallback)

            songs.clear()
            songs.addAll(newList)

            // Esto sustituye al notifyDataSetChanged y quita el warning
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
        }

        override fun getItemCount() = songs.size
    }
}