package com.hhkungfu.tv.data.history

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.TimeUnit

data class WatchHistoryItem(
    val movieUrl: String = "",
    val movieTitle: String = "",
    val posterUrl: String = "",
    val episodeSlug: String = "",
    val episodeName: String = "",
    val sv: String = "1", // "1" for Vietsub, "2" for Thuyết Minh
    val timestamp: Long = System.currentTimeMillis()
)

object HistoryManager {
    private const val PREFS_NAME = "hhkungfu_watch_history"
    private const val KEY_HISTORY = "history_list"
    private const val BACKUP_FILE_NAME = "hhkungfu_history_backup.json"
    private const val EXPIRATION_DAYS = 30L

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Lưu tập phim vừa xem vào lịch sử.
     * Lưu đồng thời vào SharedPreferences + File Backup bền vững trong hệ thống.
     * Tự động xóa các mục cũ quá 30 ngày.
     */
    fun saveWatchHistory(
        context: Context,
        movieUrl: String,
        movieTitle: String,
        posterUrl: String = "",
        episodeSlug: String,
        episodeName: String,
        sv: String = "1"
    ) {
        val currentList = getWatchHistory(context).toMutableList()
        val now = System.currentTimeMillis()
        val expirationTime = now - TimeUnit.DAYS.toMillis(EXPIRATION_DAYS)

        // 1. Lọc bỏ các mục quá 30 ngày
        val filteredList = currentList.filter { it.timestamp >= expirationTime }.toMutableList()

        // 2. Xóa mục cùng movieUrl và episodeSlug nếu đã có trước đó để cập nhật lên đầu
        filteredList.removeAll { 
            (it.movieUrl == movieUrl || it.movieTitle == movieTitle) && 
            it.episodeSlug == episodeSlug && 
            it.sv == sv 
        }

        // 3. Thêm mục mới nhất lên đầu danh sách
        val newItem = WatchHistoryItem(
            movieUrl = movieUrl,
            movieTitle = movieTitle,
            posterUrl = posterUrl,
            episodeSlug = episodeSlug,
            episodeName = episodeName,
            sv = sv,
            timestamp = now
        )
        filteredList.add(0, newItem)

        // Giới hạn lưu tối đa 300 mục gần nhất (dung lượng < 50KB)
        val trimmedList = filteredList.take(300)
        val json = gson.toJson(trimmedList)

        // 1. Lưu vào SharedPreferences bền vững
        getPrefs(context).edit().putString(KEY_HISTORY, json).apply()

        // 2. Lưu dự phòng vào File nội bộ vĩnh viễn (không bao giờ mất khi update)
        try {
            val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
            backupFile.writeText(json)

            // Lưu thêm vào External Files nếu có
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val extBackupFile = File(extDir, BACKUP_FILE_NAME)
                extBackupFile.writeText(json)
            }
        } catch (e: Exception) {
            Log.e("HistoryManager", "Error saving backup file", e)
        }
    }

    /**
     * Lấy toàn bộ danh sách lịch sử xem phim (đã lọc các mục quá 30 ngày)
     * Có cơ chế tự động phục hồi từ Backup File nếu SharedPreferences bị rỗng.
     */
    fun getWatchHistory(context: Context): List<WatchHistoryItem> {
        var json = getPrefs(context).getString(KEY_HISTORY, null)

        // Nếu SharedPreferences trống, thử khôi phục từ File Backup bền vững
        if (json.isNullOrEmpty()) {
            try {
                val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
                if (backupFile.exists()) {
                    json = backupFile.readText()
                    // Khôi phục lại vào SharedPreferences
                    getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
                } else {
                    val extDir = context.getExternalFilesDir(null)
                    val extBackupFile = if (extDir != null) File(extDir, BACKUP_FILE_NAME) else null
                    if (extBackupFile != null && extBackupFile.exists()) {
                        json = extBackupFile.readText()
                        getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryManager", "Error reading backup file", e)
            }
        }

        if (json.isNullOrEmpty()) return emptyList()

        return try {
            val type = object : TypeToken<List<WatchHistoryItem>>() {}.type
            val list: List<WatchHistoryItem> = gson.fromJson(json, type) ?: emptyList()
            val expirationTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(EXPIRATION_DAYS)
            list.filter { it.timestamp >= expirationTime }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Kiểm tra xem một tập phim cụ thể đã được xem chưa
     */
    fun isEpisodeWatched(context: Context, movieTitle: String, episodeSlug: String, sv: String): Boolean {
        val history = getWatchHistory(context)
        return history.any { 
            it.movieTitle.equals(movieTitle, ignoreCase = true) && 
            it.episodeSlug == episodeSlug && 
            it.sv == sv 
        }
    }

    /**
     * Lấy tập phim xem gần nhất của một bộ phim
     */
    fun getLastWatchedEpisode(context: Context, movieTitle: String): WatchHistoryItem? {
        val history = getWatchHistory(context)
        return history.firstOrNull { 
            it.movieTitle.equals(movieTitle, ignoreCase = true) 
        }
    }

    /**
     * Xóa toàn bộ lịch sử
     */
    fun clearAllHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
        try {
            File(context.filesDir, BACKUP_FILE_NAME).delete()
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                File(extDir, BACKUP_FILE_NAME).delete()
            }
        } catch (_: Exception) {}
    }
}
