package com.dividetask.sudokutrainer.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dividetask.sudokutrainer.data.GameConfig
import com.dividetask.sudokutrainer.data.PuzzleRepository
import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.ui.game.GameScreen
import com.dividetask.sudokutrainer.ui.game.GameViewModel
import com.dividetask.sudokutrainer.ui.menu.MainMenuScreen

private object Routes {
    const val MENU = "menu"
    const val GAME = "game/{difficulty}"
    fun game(difficulty: Difficulty) = "game/${difficulty.name}"
}

@Composable
fun SudokuTrainerApp(repository: PuzzleRepository, config: GameConfig) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.MENU,
    ) {
        composable(Routes.MENU) {
            MainMenuScreen(
                onStartGame = { difficulty ->
                    navController.navigate(Routes.game(difficulty))
                },
            )
        }
        composable(
            route = Routes.GAME,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType }),
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("difficulty")
                ?: Difficulty.Easy.name
            val difficulty = runCatching { Difficulty.valueOf(name) }
                .getOrDefault(Difficulty.Easy)
            val viewModel: GameViewModel = viewModel(
                factory = GameViewModel.factory(repository, config, difficulty),
            )
            GameScreen(
                viewModel = viewModel,
                onExit = {
                    navController.popBackStack(Routes.MENU, inclusive = false)
                },
            )
        }
    }
}
