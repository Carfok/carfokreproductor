package com.example.carfokreproductor

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlaylistManager(context: Context) {
    // Usamos SharedPreferences para guardar datos ligeros
    private val prefs = context.getSharedPreferences("carfok_playlists_db", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Estructura en memoria: "Nombre Playlist" -> [RutaCancion1, RutaCancion2...]
    private var playlists: MutableMap<String, MutableList<String>> = loadPlaylists()

    // 1. Crear una nueva lista vacía
    fun createPlaylist(name: String): Boolean {
        if (playlists.containsKey(name)) return false // Ya existe, no la sobreescribimos
        playlists[name] = mutableListOf()
        save()
        return true
    }

    // 2. Añadir canción a una lista
    fun addSongToPlaylist(playlistName: String, songPath: String) {
        val currentList = playlists[playlistName] ?: mutableListOf()
        // Evitamos duplicados
        if (!currentList.contains(songPath)) {
            currentList.add(songPath)
            playlists[playlistName] = currentList
            save()
        }
    }

    // 3. NUEVO: Eliminar una canción específica de una lista
    fun removeSongFromPlaylist(playlistName: String, songPath: String) {
        val currentList = playlists[playlistName]
        if (currentList != null) {
            currentList.remove(songPath)
            save() // Guardamos cambios inmediatamente
        }
    }

    // 4. Obtener nombres de todas las listas (ordenadas alfabéticamente)
    fun getPlaylistNames(): List<String> {
        return playlists.keys.sorted()
    }

    // 5. Obtener canciones de una lista específica
    fun getSongsInPlaylist(playlistName: String): List<String> {
        return playlists[playlistName] ?: emptyList()
    }

    // 6. Eliminar una lista completa
    fun deletePlaylist(name: String) {
        playlists.remove(name)
        save()
    }

    // --- Funciones internas de guardado ---
    private fun save() {
        val json = gson.toJson(playlists)
        prefs.edit().putString("playlists_json", json).apply()
    }

    private fun loadPlaylists(): MutableMap<String, MutableList<String>> {
        val json = prefs.getString("playlists_json", null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, MutableList<String>>>() {}.type
        return gson.fromJson(json, type)
    }
}