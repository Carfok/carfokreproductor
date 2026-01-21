package com.example.carfokreproductor

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlaylistActivity : AppCompatActivity() {

    private lateinit var playlistManager: PlaylistManager
    private lateinit var adapter: PlaylistAdapter
    private var playlistNames: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        playlistManager = PlaylistManager(this)

        val rvPlaylists = findViewById<RecyclerView>(R.id.rvPlaylists)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        rvPlaylists.layoutManager = LinearLayoutManager(this)

        // Cargar datos
        playlistNames = playlistManager.getPlaylistNames().toMutableList()

        // Mostrar u ocultar mensaje de "Vacío"
        if (playlistNames.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvPlaylists.visibility = View.GONE
        }

        adapter = PlaylistAdapter(
            playlistNames,
            onItemClick = { playlistName: String ->
                val intent = Intent(this, PlaylistSongsActivity::class.java)
                intent.putExtra("PLAYLIST_NAME", playlistName)
                startActivity(intent)
            },
            onDeleteClick = { playlistName: String ->
                showDeleteDialog(playlistName)
            }
        )
        rvPlaylists.adapter = adapter
    }

    private fun showDeleteDialog(name: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar lista")
            .setMessage("¿Seguro que quieres borrar la lista '$name'?")
            .setPositiveButton("Eliminar") { _: DialogInterface, _: Int ->
                playlistManager.deletePlaylist(name)
                refreshList()
                Toast.makeText(this, "Lista eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun refreshList() {
        playlistNames.clear()
        playlistNames.addAll(playlistManager.getPlaylistNames())
        adapter.notifyDataSetChanged()

        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val rvPlaylists = findViewById<RecyclerView>(R.id.rvPlaylists)

        if (playlistNames.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvPlaylists.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvPlaylists.visibility = View.VISIBLE
        }
    }

    class PlaylistAdapter(
        private val playlists: List<String>,
        private val onItemClick: (String) -> Unit,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPlaylistName)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDeletePlaylist)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_playlist, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val name = playlists[position]
            holder.tvName.text = name

            holder.itemView.setOnClickListener { onItemClick(name) }
            holder.btnDelete.setOnClickListener { onDeleteClick(name) }
        }

        override fun getItemCount() = playlists.size
    }
}