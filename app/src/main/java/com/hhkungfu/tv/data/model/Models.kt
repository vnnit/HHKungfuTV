package com.hhkungfu.tv.data.model

data class MovieItem(
    val id: String = "",
    val title: String = "",
    val originalTitle: String = "",
    val url: String = "",
    val posterUrl: String = "",
    val quality: String = "HD",
    val latestEpisode: String = "",
    val description: String = ""
)

data class ScheduleDay(
    val id: String, // "chu-nhat", "thu-2", "thu-3", "thu-4", "thu-5", "thu-6", "thu-7"
    val code: String, // "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    val name: String, // "Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
    val dayOfWeek: Int // Calendar.SUNDAY .. Calendar.SATURDAY
)

data class EpisodeGroup(
    val title: String, // "🎙️ Thuyết Minh" or "📝 Việt Sub"
    val isThuyetMinh: Boolean,
    val sv: String = "1",
    val episodes: List<Episode> = emptyList()
)

data class MovieDetail(
    val id: String = "",
    val title: String = "",
    val originalTitle: String = "",
    val url: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val quality: String = "HD",
    val episodeCount: String = "",
    val status: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val year: String = "",
    val episodeGroups: List<EpisodeGroup> = emptyList(),
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val name: String = "",
    val slug: String = "", // e.g. "tap-155"
    val postId: String = "",
    val sv: String = "1",
    val watchUrl: String = ""
)

data class ServerOption(
    val type: String, // "pro", "tiktik", "vip4k", "vip4kv2"
    val displayName: String
)

data class StreamSource(
    val embedUrl: String = "",
    val directUrl: String = "",
    val isIframe: Boolean = true,
    val referer: String = "https://hhkungfu.ee/"
)

data class Category(
    val id: String,
    val name: String,
    val slug: String,
    val url: String
)

data class HomeSection(
    val title: String,
    val slug: String,
    val movies: List<MovieItem>
)
