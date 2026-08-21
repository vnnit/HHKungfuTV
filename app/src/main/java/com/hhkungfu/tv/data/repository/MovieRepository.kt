package com.hhkungfu.tv.data.repository

import com.hhkungfu.tv.data.model.Category
import com.hhkungfu.tv.data.model.HomeSection
import com.hhkungfu.tv.data.model.MovieDetail
import com.hhkungfu.tv.data.model.MovieItem
import com.hhkungfu.tv.data.model.ScheduleDay
import com.hhkungfu.tv.data.model.ServerOption
import com.hhkungfu.tv.data.model.StreamSource
import com.hhkungfu.tv.data.parser.HhKungfuParser
import com.hhkungfu.tv.utils.Constants
import java.util.Calendar

class MovieRepository(
    private val parser: HhKungfuParser = HhKungfuParser()
) {
    val categories = listOf(
        Category("home", "Trang Chủ", "home", "${Constants.BASE_URL}/"),
        Category("lich-chieu", "Lịch Chiếu", "lich-chieu", "${Constants.BASE_URL}/lich-chieu"),
        Category("tu-tien", "Tu Tiên", "tu-tien", "${Constants.BASE_URL}/tu-tien"),
        Category("luyen-cap", "Luyện Cấp", "luyen-cap", "${Constants.BASE_URL}/category/luyen-cap"),
        Category("trung-sinh", "Trùng Sinh", "trung-sinh", "${Constants.BASE_URL}/category/trung-sinh"),
        Category("kiem-hiep", "Kiếm Hiệp", "kiem-hiep", "${Constants.BASE_URL}/category/kiem-hiep"),
        Category("xuyen-khong", "Xuyên Không", "xuyen-khong", "${Constants.BASE_URL}/xuyen-khong"),
        Category("hai-huoc", "Hài Hước", "hai-huoc", "${Constants.BASE_URL}/category/hai-huoc"),
        Category("moi-cap-nhat", "Mới Cập Nhật", "moi-cap-nhat", "${Constants.BASE_URL}/moi-cap-nhat"),
        Category("top-xem-nhieu", "Xem Nhiều", "top-xem-nhieu", "${Constants.BASE_URL}/top-xem-nhieu"),
        Category("hoan-thanh", "Hoàn Thành", "hoan-thanh", "${Constants.BASE_URL}/hoan-thanh")
    )

    val scheduleDays = listOf(
        ScheduleDay("chu-nhat", "Sun", "Chủ nhật", Calendar.SUNDAY),
        ScheduleDay("thu-2", "Mon", "Thứ Hai", Calendar.MONDAY),
        ScheduleDay("thu-3", "Tue", "Thứ Ba", Calendar.TUESDAY),
        ScheduleDay("thu-4", "Wed", "Thứ Tư", Calendar.WEDNESDAY),
        ScheduleDay("thu-5", "Thu", "Thứ Năm", Calendar.THURSDAY),
        ScheduleDay("thu-6", "Fri", "Thứ Sáu", Calendar.FRIDAY),
        ScheduleDay("thu-7", "Sat", "Thứ Bảy", Calendar.SATURDAY)
    )

    val serverOptions = listOf(
        ServerOption(Constants.SERVER_PRO, "1080P V2 (Nhanh)"),
        ServerOption(Constants.SERVER_TIKTIK, "1080P V1 (Chuẩn)"),
        ServerOption(Constants.SERVER_VIP4K, "4K V1 (Siêu Nét)"),
        ServerOption(Constants.SERVER_VIP4KV2, "4K V2 (Siêu Nét)")
    )

    suspend fun getHomePage(): Pair<MovieItem?, List<HomeSection>> {
        return parser.getHomePage()
    }

    suspend fun getSchedule(dayId: String): List<MovieItem> {
        return parser.getSchedule(dayId)
    }

    suspend fun getCategoryMovies(slug: String, page: Int = 1): List<MovieItem> {
        return parser.getCategoryMovies(slug, page)
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<MovieItem> {
        return parser.searchMovies(query, page)
    }

    suspend fun getMovieDetail(movieUrl: String): MovieDetail {
        return parser.getMovieDetail(movieUrl)
    }

    suspend fun getStreamSource(postId: String, chapterSt: String, serverType: String, sv: String = "1"): StreamSource {
        return parser.getStreamSource(postId, chapterSt, serverType, sv)
    }
}
