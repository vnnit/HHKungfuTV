package com.hhkungfu.tv.data.history

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
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
     * Lưu tập phim vừa xem vào lịch sử (Đa tầng 4 lớp bảo vệ).
     * 1. SharedPreferences (Truy xuất tức thì)
     * 2. context.filesDir (File nội bộ app)
     * 3. context.getExternalFilesDir (Bộ nhớ ngoài của app)
     * 4. /sdcard/Download/ (Bộ nhớ chung hệ thống - Vĩnh viễn không mất kể cả khi gỡ app)
     * Tự động lọc bỏ các mục cũ quá 30 ngày.
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

        // 2. Xóa mục cùng movieTitle + episodeSlug + sv nếu đã có trước đó để đưa lên đầu
        filteredList.removeAll { 
            (it.movieUrl == movieUrl || it.movieTitle.equals(movieTitle, ignoreCase = true)) && 
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

        // Giới hạn lưu tối đa 300 mục gần nhất (< 50KB)
        val trimmedList = filteredList.take(300)
        val json = gson.toJson(trimmedList)

        // 1. Lưu vào SharedPreferences
        getPrefs(context).edit().putString(KEY_HISTORY, json).apply()

        // 2. Lưu vào File nội bộ app
        try {
            val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
            backupFile.writeText(json)
        } catch (_: Exception) {}

        // 3. Lưu vào External Files Dir
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                File(extDir, BACKUP_FILE_NAME).writeText(json)
            }
        } catch (_: Exception) {}

        // 4. Lưu vào Thư mục Download công cộng (Bảo tồn vĩnh viễn kể cả khi gỡ app cài lại)
        try {
            val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloadDir != null && (publicDownloadDir.exists() || publicDownloadDir.mkdirs())) {
                File(publicDownloadDir, BACKUP_FILE_NAME).writeText(json)
            }
        } catch (_: Exception) {}
    }

    /**
     * Lấy toàn bộ danh sách lịch sử xem phim.
     * Cơ chế tự động phục hồi thông minh theo thứ tự 4 tầng nếu phát hiện dữ liệu bị rỗng.
     */
    fun getWatchHistory(context: Context): List<WatchHistoryItem> {
        var json = getPrefs(context).getString(KEY_HISTORY, null)

        // Phục hồi từ các tầng dự phòng nếu SharedPreferences bị rỗng
        if (json.isNullOrEmpty()) {
            // Tầng 2: filesDir
            try {
                val f1 = File(context.filesDir, BACKUP_FILE_NAME)
                if (f1.exists() && f1.length() > 0) {
                    json = f1.readText()
                }
            } catch (_: Exception) {}

            // Tầng 3: getExternalFilesDir
            if (json.isNullOrEmpty()) {
                try {
                    val extDir = context.getExternalFilesDir(null)
                    val f2 = if (extDir != null) File(extDir, BACKUP_FILE_NAME) else null
                    if (f2 != null && f2.exists() && f2.length() > 0) {
                        json = f2.readText()
                    }
                } catch (_: Exception) {}
            }

            // Tầng 4: Public Downloads Directory (Dành riêng cho trường hợp vừa gỡ app cài mới lại)
            if (json.isNullOrEmpty()) {
                try {
                    val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val f3 = if (publicDownloadDir != null) File(publicDownloadDir, BACKUP_FILE_NAME) else null
                    if (f3 != null && f3.exists() && f3.length() > 0) {
                        json = f3.readText()
                    }
                } catch (_: Exception) {}
            }

            // Nếu tìm thấy ở bất kỳ tầng nào, tự động khôi phục lại vào SharedPreferences
            if (!json.isNullOrEmpty()) {
                getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
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

    fun isEpisodeWatched(context: Context, movieTitle: String, episodeSlug: String, sv: String): Boolean {
        val history = getWatchHistory(context)
        return history.any { 
            it.movieTitle.equals(movieTitle, ignoreCase = true) && 
            it.episodeSlug == episodeSlug && 
            it.sv == sv 
        }
    }

    fun getLastWatchedEpisode(context: Context, movieTitle: String): WatchHistoryItem? {
        val history = getWatchHistory(context)
        return history.firstOrNull { 
            it.movieTitle.equals(movieTitle, ignoreCase = true) 
        }
    }

    fun clearAllHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
        try {
            File(context.filesDir, BACKUP_FILE_NAME).delete()
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                File(extDir, BACKUP_FILE_NAME).delete()
            }
            val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloadDir != null) {
                File(publicDownloadDir, BACKUP_FILE_NAME).delete()
            }
        } catch (_: Exception) {}
    }
}
