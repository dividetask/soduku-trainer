package com.dividetask.sudokutrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dividetask.sudokutrainer.data.GameConfig
import com.dividetask.sudokutrainer.data.PuzzleRepository
import com.dividetask.sudokutrainer.data.SavedGameStore
import com.dividetask.sudokutrainer.data.SettingsStore
import com.dividetask.sudokutrainer.domain.Difficulty
import com.dividetask.sudokutrainer.domain.TrainerMethod
import com.dividetask.sudokutrainer.ui.game.GameScreen
import com.dividetask.sudokutrainer.ui.game.GameViewModel
import com.dividetask.sudokutrainer.ui.menu.MainMenuScreen
import com.dividetask.sudokutrainer.ui.settings.SettingsScreen
import com.dividetask.sudokutrainer.ui.trainer.TrainerMenuScreen
import com.dividetask.sudokutrainer.ui.trainer.TrainerScreen
import com.dividetask.sudokutrainer.ui.trainer.TrainerViewModel

private object Routes {
    const val MENU = "menu"
    const val SETTINGS = "settings"
    const val GAME = "game/{difficulty}?resume={resume}"
    const val TRAINER = "trainer"
    const val TRAINER_METHOD = "trainer/{method}"
    fun game(difficulty: Difficulty) = "game/${difficulty.name}?resume=false"
    const val RESUME = "game/Easy?resume=true"
    fun trainer(method: TrainerMethod) = "trainer/${method.name}"
}

@Composable
fun SudokuTrainerApp(
    repository: PuzzleRepository,
    config: GameConfig,
    settingsStore: SettingsStore,
    savedGames: SavedGameStore,
) {
    val navController = rememberNavController()
    // The first launch opens on Settings; after that, on the main menu.
    val start = remember { if (settingsStore.isFirstRun) Routes.SETTINGS else Routes.MENU }
    NavHost(
        navController = navController,
        startDestination = start,
    ) {
        composable(Routes.MENU) {
            val canResume = remember { savedGames.hasSavedGame() }
            MainMenuScreen(
                canResume = canResume,
                onResume = { navController.navigate(Routes.RESUME) },
                onNewGame = {
                    // The difficulty is chosen in Settings; send the player
                    // there if it somehow isn't set yet.
                    val difficulty = settingsStore.settings.value.difficulty
                    navController.navigate(if (difficulty != null) Routes.game(difficulty) else Routes.SETTINGS)
                },
                onLesson = { navController.navigate(Routes.TRAINER) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                store = settingsStore,
                onDone = {
                    settingsStore.markFirstRunDone()
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        // First run: Settings was the start screen.
                        navController.navigate(Routes.MENU) {
                            popUpTo(Routes.SETTINGS) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("resume") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("difficulty")
                ?: Difficulty.Easy.name
            val difficulty = runCatching { Difficulty.valueOf(name) }
                .getOrDefault(Difficulty.Easy)
            val resume = backStackEntry.arguments?.getBoolean("resume") ?: false
            val viewModel: GameViewModel = viewModel(
                factory = GameViewModel.factory(repository, config, settingsStore, savedGames, difficulty, resume),
            )
            GameScreen(
                viewModel = viewModel,
                onExit = { navController.backToMenu() },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.TRAINER) {
            TrainerMenuScreen(
                onMethod = { navController.navigate(Routes.trainer(it)) },
                onMainMenu = { navController.backToMenu() },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.TRAINER_METHOD,
            arguments = listOf(navArgument("method") { type = NavType.StringType }),
        ) { backStackEntry ->
            val method = backStackEntry.arguments?.getString("method")
                ?.let { runCatching { TrainerMethod.valueOf(it) }.getOrNull() }
                ?: TrainerMethod.NakedSingle
            val viewModel: TrainerViewModel = viewModel(
                factory = TrainerViewModel.factory(repository, config, settingsStore, method),
            )
            TrainerScreen(
                viewModel = viewModel,
                onMainMenu = { navController.backToMenu() },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onMethod = { next ->
                    // Swap in place, so back still leads to the method list.
                    navController.navigate(Routes.trainer(next)) {
                        popUpTo(Routes.TRAINER_METHOD) { inclusive = true }
                    }
                },
            )
        }
    }
}

private fun NavHostController.backToMenu() {
    popBackStack(Routes.MENU, inclusive = false)
}
