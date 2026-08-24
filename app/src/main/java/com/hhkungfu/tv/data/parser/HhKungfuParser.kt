package com.hhkungfu.tv.data.parser

import android.util.Log
import com.hhkungfu.tv.data.model.Episode
import com.hhkungfu.tv.data.model.EpisodeGroup
import com.hhkungfu.tv.data.model.HomeSection
import com.hhkungfu.tv.data.model.MovieDetail
import com.hhkungfu.tv.data.model.MovieItem
import com.hhkungfu.tv.data.model.StreamSource
import com.hhkungfu.tv.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ConnectionPool
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class HhKungfuParser {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // In-memory cache for instant UI rendering
    private val movieDetailCache = ConcurrentHashMap<String, MovieDetail>()
    private val categoryCache = ConcurrentHashMap<String, List<MovieItem>>()
    private val scheduleCache = ConcurrentHashMap<String, List<MovieItem>>()
    private var homeCache: Pair<MovieItem?, List<HomeSection>>? = null

    suspend fun getHomePage(): Pair<MovieItem?, List<HomeSection>> = withContext(Dispatchers.IO) {
        homeCache?.let { return@withContext it }

        try {
            val html = fetchHtml(Constants.BASE_URL)
            val doc = Jsoup.parse(html, Constants.BASE_URL)
            
            val sections = mutableListOf<HomeSection>()
            val allMovies = parseMovieElements(doc.select(".halim-item"))
            
            if (allMovies.isNotEmpty()) {
                sections.add(
                    HomeSection(
                        title = "🔥 Phim Mới Cập Nhật",
                        slug = "moi-cap-nhat",
                        movies = allMovies.take(15)
                    )
                )
            }

            // Concurrently fetch top categories with timeout for fast startup
            val categoriesToFetch = listOf(
                Pair("Tu Tiên", "tu-tien"),
                Pair("Luyện Cấp", "luyen-cap"),
                Pair("Kiếm Hiệp", "kiem-hiep"),
                Pair("Trùng Sinh", "trung-sinh"),
                Pair("Xem Nhiều", "top-xem-nhieu")
            )

            coroutineScope {
                val catDeferreds = categoriesToFetch.map { (catName, catSlug) ->
                    async {
                        withTimeoutOrNull(4000) {
                            try {
                                val catMovies = getCategoryMovies(catSlug, 1)
                                if (catMovies.isNotEmpty()) {
                                    HomeSection(
                                        title = if (catSlug == "top-xem-nhieu") "⭐ $catName" else "🎬 $catName",
                                        slug = catSlug,
                                        movies = catMovies.take(12)
                                    )
                                } else null
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }

                catDeferreds.forEach { deferred ->
                    deferred.await()?.let { sections.add(it) }
                }
            }

            val heroMovie = allMovies.firstOrNull()
            val result = Pair(heroMovie, sections)
            homeCache = result
            result
        } catch (e: Exception) {
            Log.e("HhKungfuParser", "Error getHomePage", e)
            throw e
        }
    }

    suspend fun getSchedule(dayId: String): List<MovieItem> = withContext(Dispatchers.IO) {
        scheduleCache[dayId]?.let { return@withContext it }

        try {
            val formBody = FormBody.Builder()
                .add("action", "dox_schedule")
                .add("date", dayId)
                .build()

            val request = Request.Builder()
                .url("${Constants.BASE_URL}/wp-admin/admin-ajax.php")
                .header("User-Agent", Constants.USER_AGENT)
                .header("Referer", "${Constants.BASE_URL}/")
                .header("X-Requested-With", "XMLHttpRequest")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val doc = Jsoup.parse(body)
            val movies = parseMovieElements(doc.select(".halim-item, article"))
            if (movies.isNotEmpty()) {
                scheduleCache[dayId] = movies
            }
            return@withContext movies
        } catch (e: Exception) {
            Log.e("HhKungfuParser", "Error fetching schedule for $dayId", e)
            return@withContext emptyList()
        }
    }

    suspend fun getCategoryMovies(slug: String, page: Int = 1): List<MovieItem> = withContext(Dispatchers.IO) {
        val cleanSlug = slug.removePrefix("/").removeSuffix("/").trim()
        val cacheKey = "$cleanSlug-$page"
        categoryCache[cacheKey]?.let { return@withContext it }

        val targetUrl = buildCategoryUrl(cleanSlug, page)
        try {
            val html = fetchHtml(targetUrl)
            val doc = Jsoup.parse(html, Constants.BASE_URL)
            var list = parseMovieElements(doc.select(".halim-item"))
            
            // Fallback: If empty, try alternative url structure
            if (list.isEmpty() && !cleanSlug.startsWith("http")) {
                val fallbackUrl = if (cleanSlug.startsWith("category/")) {
                    "${Constants.BASE_URL}/${cleanSlug.removePrefix("category/")}/"
                } else {
                    "${Constants.BASE_URL}/category/$cleanSlug/"
                }
                val fallbackHtml = fetchHtml(fallbackUrl)
                val fallbackDoc = Jsoup.parse(fallbackHtml, Constants.BASE_URL)
                list = parseMovieElements(fallbackDoc.select(".halim-item"))
            }

            if (list.isNotEmpty()) {
                categoryCache[cacheKey] = list
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e("HhKungfuParser", "Error getCategoryMovies for $cleanSlug", e)
            return@withContext emptyList()
        }
    }

    private fun buildCategoryUrl(cleanSlug: String, page: Int): String {
        val base = when (cleanSlug) {
            "moi-cap-nhat", "top-xem-nhieu", "hoan-thanh", "lich-chieu", "tu-tien", "xuyen-khong" -> "${Constants.BASE_URL}/$cleanSlug"
            else -> {
                if (cleanSlug.startsWith("http")) cleanSlug
                else if (cleanSlug.startsWith("category/")) "${Constants.BASE_URL}/$cleanSlug"
                else "${Constants.BASE_URL}/category/$cleanSlug"
            }
        }
        return if (page > 1) "$base/page/$page/" else "$base/"
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<MovieItem> = withContext(Dispatchers.IO) {
        val url = if (page > 1) {
            "${Constants.BASE_URL}/page/$page/?s=${java.net.URLEncoder.encode(query, "UTF-8")}"
        } else {
            "${Constants.BASE_URL}/?s=${java.net.URLEncoder.encode(query, "UTF-8")}"
        }
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html, Constants.BASE_URL)
        parseMovieElements(doc.select(".halim-item"))
    }

    suspend fun getMovieDetail(movieUrl: String): MovieDetail = withContext(Dispatchers.IO) {
        var finalUrl = movieUrl.trim()
        
        // If not a valid HTTP/HTTPS url, resolve it automatically
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            if (finalUrl.contains("/") || (finalUrl.contains("-") && !finalUrl.contains(" ") && !finalUrl.all { it.isDigit() })) {
                finalUrl = "${Constants.BASE_URL}/${finalUrl.removePrefix("/")}"
            } else {
                // It's a title (e.g. "Thôn Phệ Tinh Không") or numeric ID -> Search for it on HhKungfu!
                try {
                    val searchResults = searchMovies(finalUrl)
                    val matched = searchResults.firstOrNull { 
                        it.title.equals(finalUrl, ignoreCase = true) || it.title.contains(finalUrl, ignoreCase = true) || finalUrl.contains(it.title, ignoreCase = true)
                    } ?: searchResults.firstOrNull()

                    if (matched != null && matched.url.isNotEmpty()) {
                        finalUrl = matched.url
                    }
                } catch (e: Exception) {
                    Log.e("HhKungfuParser", "Error resolving movie detail url for: $finalUrl", e)
                }
            }
        }

        movieDetailCache[finalUrl]?.let { return@withContext it }

        val html = fetchHtml(finalUrl)
        val doc = Jsoup.parse(html, finalUrl)
        
        val title = doc.select(".entry-title, .title-1, h1").firstOrNull()?.text()?.trim() ?: "Phim Hoạt Hình"
        val originalTitle = doc.select(".org-title, .original_title, .title-2").firstOrNull()?.text()?.trim() ?: ""
        
        var posterUrl = doc.select(".film-poster-img, .movie-poster img, .halim-thumb img").attr("src")
        if (posterUrl.isEmpty()) {
            posterUrl = doc.select(".film-poster-img, .movie-poster img, .halim-thumb img").attr("data-src")
        }
        
        val quality = doc.select(".status, .badge-quality, .quality").firstOrNull()?.text()?.trim() ?: "4K FULL HD"
        val episodeCount = doc.select(".episode, .episode-latest").firstOrNull()?.text()?.trim() ?: ""
        val description = doc.select(".entry-content, .film-content, #film-content").firstOrNull()?.text()?.trim() ?: "Đang cập nhật tóm tắt nội dung..."
        
        val genres = doc.select(".category a, .genre a").map { it.text().trim() }.filter { it.isNotEmpty() }
        val year = doc.select(".release a, .year, .country a").firstOrNull()?.text()?.trim() ?: "2024"
        
        // Parse episode groups (Thuyết Minh & Vietsub)
        var episodeGroups = parseEpisodeGroups(doc)
        
        // If no episode groups on detail page, check if there is a watch button leading to watch page
        if (episodeGroups.isEmpty()) {
            val watchLink = doc.select("a[href*='watch-'], a[href*='tap-']").firstOrNull()?.attr("href")
            if (!watchLink.isNullOrEmpty()) {
                try {
                    val watchHtml = fetchHtml(watchLink)
                    val watchDoc = Jsoup.parse(watchHtml, watchLink)
                    episodeGroups = parseEpisodeGroups(watchDoc)
                } catch (e: Exception) {
                    Log.e("HhKungfuParser", "Error parsing watch page $watchLink", e)
                }
            }
        }

        val allEpisodes = episodeGroups.flatMap { it.episodes }.distinctBy { it.name }
        
        val detail = MovieDetail(
            id = movieUrl.substringAfterLast("/").removeSuffix(".html"),
            title = title,
            originalTitle = originalTitle,
            url = movieUrl,
            posterUrl = posterUrl,
            backdropUrl = posterUrl,
            quality = quality,
            episodeCount = episodeCount,
            description = description,
            genres = genres,
            year = year,
            episodeGroups = episodeGroups,
            episodes = allEpisodes
        )
        movieDetailCache[movieUrl] = detail
        detail
    }

    private fun parseEpisodeGroups(doc: Document): List<EpisodeGroup> {
        val groups = mutableListOf<EpisodeGroup>()
        
        // SELECT EXACTLY .halim-server blocks (do NOT select parent #halim-list-server to avoid duplicate groups)
        val serverBlocks = doc.select(".halim-server")
        
        for (serverEl in serverBlocks) {
            val serverNameRaw = serverEl.select(".halim-server-name, .server-name, .title-server").text().trim()
            val cleanName = serverNameRaw.replace("#", "").replace(":", "").trim()
            val isThuyetMinh = cleanName.contains("Thuyết Minh", ignoreCase = true) || cleanName.contains("Lồng Tiếng", ignoreCase = true)

            val episodes = mutableListOf<Episode>()
            val epElements = serverEl.select(".halim-episode a, .list-episode a, li a")
            
            var svFound = if (isThuyetMinh) "2" else "1"

            for (el in epElements) {
                val epName = el.select(".halim-btn, span").text().ifEmpty { el.text() }.trim()
                val epSlug = el.attr("data-ep")
                val postId = el.attr("data-post-id")
                val sv = el.attr("data-sv").ifEmpty { if (isThuyetMinh) "2" else "1" }
                val href = el.attr("href")
                
                svFound = sv

                if (epName.isNotEmpty() && !epName.startsWith("Server")) {
                    episodes.add(
                        Episode(
                            name = epName,
                            slug = epSlug.ifEmpty { href.substringAfterLast("/").removeSuffix(".html") },
                            postId = postId,
                            sv = sv,
                            watchUrl = href
                        )
                    )
                }
            }

            if (episodes.isNotEmpty()) {
                val displayTitle = if (isThuyetMinh) "🎙️ Thuyết Minh" else "📝 Việt Sub"
                groups.add(
                    EpisodeGroup(
                        title = displayTitle,
                        isThuyetMinh = isThuyetMinh,
                        sv = svFound,
                        episodes = episodes.distinctBy { it.name }
                    )
                )
            }
        }

        // Fallback: If no .halim-server blocks found, parse all episodes into 1 group
        if (groups.isEmpty()) {
            val eps = parseEpisodes(doc)
            if (eps.isNotEmpty()) {
                groups.add(
                    EpisodeGroup(
                        title = "📝 Danh Sách Tập",
                        isThuyetMinh = false,
                        sv = "1",
                        episodes = eps
                    )
                )
            }
        }

        // Deduplicate groups by isThuyetMinh (ensure at most 1 Thuyết Minh and 1 Việt Sub)
        val deduplicatedGroups = groups
            .groupBy { it.isThuyetMinh }
            .map { entry -> entry.value.maxByOrNull { it.episodes.size } ?: entry.value.first() }

        // SORT: Thuyết Minh ALWAYS FIRST (at the top), Việt Sub below
        return deduplicatedGroups.sortedByDescending { it.isThuyetMinh }
    }

    private fun parseEpisodes(doc: Document): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val epElements = doc.select(".halim-episode a, #halim-ajax-list-server a, .list-episode a")
        
        for (el in epElements) {
            val epName = el.select(".halim-btn, span").text().ifEmpty { el.text() }.trim()
            val epSlug = el.attr("data-ep")
            val postId = el.attr("data-post-id")
            val sv = el.attr("data-sv").ifEmpty { "1" }
            val href = el.attr("href")
            
            if (epName.isNotEmpty()) {
                episodes.add(
                    Episode(
                        name = epName,
                        slug = epSlug.ifEmpty { href.substringAfterLast("/").removeSuffix(".html") },
                        postId = postId,
                        sv = sv,
                        watchUrl = href
                    )
                )
            }
        }
        return episodes.distinctBy { it.name }
    }

    suspend fun getStreamSource(
        postId: String,
        chapterSt: String,
        serverType: String = Constants.SERVER_PRO,
        sv: String = "1"
    ): StreamSource = withContext(Dispatchers.IO) {
        val urlBuilder = "${Constants.BASE_URL}/player/player.php".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("action", "dox_ajax_player")
            ?.addQueryParameter("post_id", postId)
            ?.addQueryParameter("chapter_st", chapterSt)
            ?.addQueryParameter("type", serverType)
            ?.addQueryParameter("sv", sv)
            ?: return@withContext StreamSource()
            
        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", Constants.USER_AGENT)
            .header("Referer", "${Constants.BASE_URL}/")
            .header("X-Requested-With", "XMLHttpRequest")
            .build()
            
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        
        val doc = Jsoup.parse(body)
        val iframeSrc = doc.select("iframe").attr("src")
        
        StreamSource(
            embedUrl = iframeSrc,
            directUrl = iframeSrc,
            isIframe = true,
            referer = Constants.BASE_URL
        )
    }

    private fun parseMovieElements(elements: org.jsoup.select.Elements): List<MovieItem> {
        val list = mutableListOf<MovieItem>()
        for (el in elements) {
            val thumb = el.select(".halim-thumb").firstOrNull() ?: el.select("a").firstOrNull()
            val url = thumb?.attr("href") ?: ""
            val title = thumb?.attr("title")?.ifEmpty { el.select(".entry-title, .schedule-title, h3").text() } 
                ?: el.select(".entry-title, .schedule-title, h3").text()
            val originalTitle = el.select(".original_title").text()
            
            var poster = el.select("img").attr("src")
            if (poster.isEmpty()) {
                poster = el.select("img").attr("data-src")
            }
            
            val status = el.select(".status, .schedule-episode").text().ifEmpty { "FULL HD" }
            val ep = el.select(".episode, .schedule-episode").text()
            
            if (title.isNotEmpty() && url.isNotEmpty()) {
                list.add(
                    MovieItem(
                        id = url.substringAfterLast("/").removeSuffix(".html"),
                        title = title.trim(),
                        originalTitle = originalTitle.trim(),
                        url = url,
                        posterUrl = poster,
                        quality = status.trim(),
                        latestEpisode = ep.trim()
                    )
                )
            }
        }
        return list.distinctBy { it.url }
    }

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Constants.USER_AGENT)
            .header("Referer", Constants.BASE_URL)
            .build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }
}
