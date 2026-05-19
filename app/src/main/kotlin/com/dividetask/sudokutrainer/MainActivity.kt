package com.dividetask.sudokutrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dividetask.sudokutrainer.data.GameConfig
import com.dividetask.sudokutrainer.data.PuzzleRepository
import com.dividetask.sudokutrainer.ui.SudokuTrainerApp
import com.dividetask.sudokutrainer.ui.theme.SudokuTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = PuzzleRepository(applicationContext)
        val config = GameConfig.loadFromAssets(applicationContext)
        setContent {
            SudokuTrainerTheme {
                SudokuTrainerApp(repository, config)
            }
        }
    }
}
