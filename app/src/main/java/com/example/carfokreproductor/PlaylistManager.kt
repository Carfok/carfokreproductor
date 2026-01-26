package com.example.carfokreproductor

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlaylistManager(context: Context) {
    private val prefs = context.getSharedPreferences("carfok_playlists_db", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var playlists: MutableMap<String, MutableList<String>> = loadPlaylists()

    fun createPlaylist(name: String): Boolean {
        if (playlists.containsKey(name)) return false
        playlists[name] = mutableListOf()
        save()
        return true
    }

    fun addSongToPlaylist(playlistName: String, songPath: String) {
        val currentList = playlists[playlistName] ?: mutableListOf()
        if (!currentList.contains(songPath)) {
            currentList.add(songPath)
            playlists[playlistName] = currentList
            save()
        }
    }

    fun removeSongFromPlaylist(playlistName: String, songPath: String) {
        val currentList = playlists[playlistName]
        if (currentList != null) {
            currentList.remove(songPath)
            save()
        }
    }

    // Nuevo método para actualizar el orden de una playlist
    fun savePlaylistOrder(playlistName: String, newList: List<String>) {
        playlists[playlistName] = newList.toMutableList()
        save()
    }

    fun getPlaylistNames(): List<String> {
        return playlists.keys.sorted()
    }

    fun getSongsInPlaylist(playlistName: String): List<String> {
        return playlists[playlistName] ?: emptyList()
    }

    fun deletePlaylist(name: String) {
        playlists.remove(name)
        save()
    }

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