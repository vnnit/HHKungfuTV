package com.hhkungfu.tv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hhkungfu.tv.ui.screens.category.CategoryScreen
import com.hhkungfu.tv.ui.screens.detail.DetailScreen
import com.hhkungfu.tv.ui.screens.history.HistoryScreen
import com.hhkungfu.tv.ui.screens.home.HomeScreen
import com.hhkungfu.tv.ui.screens.player.TvPlayerScreen
import com.hhkungfu.tv.ui.screens.search.SearchScreen
import com.hhkungfu.tv.utils.NavUtils

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                onMovieClick = { movie ->
                    navController.navigate(Screen.Detail.createRoute(movie.url))
                },
                onCategoryClick = { slug, title ->
                    navController.navigate(Screen.Category.createRoute(slug, title))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        // History Screen
        composable(Screen.History.route) {
            HistoryScreen(
                onMovieClick = { movieUrl ->
                    navController.navigate(Screen.Detail.createRoute(movieUrl))
                },
                onPlayHistoryItem = { item ->
                    navController.navigate(
                        Screen.Player.createRoute(
                            postId = item.movieUrl,
                            chapterSt = item.episodeSlug,
                            movieTitle = item.movieTitle,
                            episodeName = item.episodeName,
                            serverType = "pro",
                            sv = item.sv
                        )
                    )
                },
                onCategoryClick = { slug, title ->
                    navController.navigate(Screen.Category.createRoute(slug, title))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Search Screen
        composable(Screen.Search.route) {
            SearchScreen(
                onMovieClick = { movie ->
                    navController.navigate(Screen.Detail.createRoute(movie.url))
                },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onCategoryClick = { slug, title ->
                    navController.navigate(Screen.Category.createRoute(slug, title))
                }
            )
        }

        // Category Screen
        composable(
            route = Screen.Category.route,
            arguments = listOf(
                navArgument("slug") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedSlug = backStackEntry.arguments?.getString("slug") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
            
            val slug = NavUtils.decode(encodedSlug).ifEmpty { "tu-tien" }
            val title = NavUtils.decode(encodedTitle).ifEmpty { "Thể Loại" }

            CategoryScreen(
                slug = slug,
                title = title,
                onMovieClick = { movie ->
                    navController.navigate(Screen.Detail.createRoute(movie.url))
                },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.route)
                },
                onSelectOtherCategory = { otherSlug, otherTitle ->
                    navController.navigate(Screen.Category.createRoute(otherSlug, otherTitle)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Detail Screen
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("movieUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("movieUrl") ?: ""
            val movieUrl = NavUtils.decode(encodedUrl)

            DetailScreen(
                movieUrl = movieUrl,
                onBackClick = { navController.popBackStack() },
                onPlayEpisode = { episode, movieTitle, serverType, sv ->
                    navController.navigate(
                        Screen.Player.createRoute(
                            postId = episode.postId,
                            chapterSt = episode.slug,
                            movieTitle = movieTitle,
                            episodeName = episode.name,
                            serverType = serverType,
                            sv = sv
                        )
                    )
                }
            )
        }

        // Player Screen
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType },
                navArgument("chapterSt") { type = NavType.StringType },
                navArgument("movieTitle") { type = NavType.StringType },
                navArgument("episodeName") { type = NavType.StringType },
                navArgument("serverType") { type = NavType.StringType },
                navArgument("sv") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encPostId = backStackEntry.arguments?.getString("postId") ?: ""
            val encChapterSt = backStackEntry.arguments?.getString("chapterSt") ?: ""
            val encTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
            val encEp = backStackEntry.arguments?.getString("episodeName") ?: ""
            val encServer = backStackEntry.arguments?.getString("serverType") ?: ""
            val encSv = backStackEntry.arguments?.getString("sv") ?: ""

            val postId = NavUtils.decode(encPostId)
            val chapterSt = NavUtils.decode(encChapterSt)
            val movieTitle = NavUtils.decode(encTitle)
            val episodeName = NavUtils.decode(encEp)
            val serverType = NavUtils.decode(encServer).ifEmpty { "pro" }
            val sv = NavUtils.decode(encSv).ifEmpty { "1" }

            TvPlayerScreen(
                postId = postId,
                chapterSt = chapterSt,
                movieTitle = movieTitle,
                episodeName = episodeName,
                serverType = serverType,
                sv = sv,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
