package com.hhkungfu.tv.ui.navigation

import com.hhkungfu.tv.utils.NavUtils

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Search : Screen("search")
    
    data object Category : Screen("category/{slug}/{title}") {
        fun createRoute(slug: String, title: String): String {
            return "category/${NavUtils.encode(slug)}/${NavUtils.encode(title)}"
        }
    }
    
    data object Detail : Screen("detail/{movieUrl}") {
        fun createRoute(movieUrl: String): String {
            return "detail/${NavUtils.encode(movieUrl)}"
        }
    }
    
    data object Player : Screen("player/{postId}/{chapterSt}/{movieTitle}/{episodeName}/{serverType}/{sv}") {
        fun createRoute(
            postId: String,
            chapterSt: String,
            movieTitle: String,
            episodeName: String,
            serverType: String,
            sv: String = "1"
        ): String {
            return "player/${NavUtils.encode(postId)}/${NavUtils.encode(chapterSt)}/${NavUtils.encode(movieTitle)}/${NavUtils.encode(episodeName)}/${NavUtils.encode(serverType)}/${NavUtils.encode(sv)}"
        }
    }
}
