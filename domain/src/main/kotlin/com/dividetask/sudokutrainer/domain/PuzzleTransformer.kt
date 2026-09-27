package com.dividetask.sudokutrainer.domain

import kotlin.random.Random

/**
 * Produces a visually distinct but logically equivalent copy of a puzzle.
 *
 * Every transform here is a Sudoku symmetry: relabelling digits, shuffling
 * bands (and stacks), shuffling rows within a band (and columns within a
 * stack), and transposing. These preserve validity, uniqueness of the
 * solution, and the set of techniques needed to solve it — so the graded
 * difficulty is unchanged — while yielding roughly 1.2 trillion variants of
 * each bundled puzzle.
 */
object PuzzleTransformer {

    fun transform(puzzle: Puzzle, random: Random = Random.Default): Puzzle {
        // digitMap[d] is the new label for digit d (index 0 keeps 0 = empty).
        val digitMap = listOf(0) + (1..9).shuffled(random)
        val rowMap = lineOrder(random)
        val colMap = lineOrder(random)
        val transpose = random.nextBoolean()

        fun remap(grid: List<Int>): List<Int> = List(81) { i ->
            val r = i / 9
            val c = i % 9
            val (sr, sc) = if (transpose) colMap[c] to rowMap[r] else rowMap[r] to colMap[c]
            digitMap[grid[sr * 9 + sc]]
        }

        return puzzle.copy(
            givens = remap(puzzle.givens),
            solution = remap(puzzle.solution),
        )
    }

    /** A random line order that keeps each band's rows together. */
    private fun lineOrder(random: Random): List<Int> =
        (0..2).shuffled(random).flatMap { band ->
            (0..2).shuffled(random).map { band * 3 + it }
        }
}
