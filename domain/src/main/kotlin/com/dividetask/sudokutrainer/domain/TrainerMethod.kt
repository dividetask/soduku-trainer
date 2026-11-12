package com.dividetask.sudokutrainer.domain

/**
 * A solving method the trainer can drill. [techniqueName] matches
 * [Hint.techniqueName]. [usesNotes] is false for the two
 * placement methods, whose drills show notes only when the player's
 * settings ask for them.
 * [minLevel] is the easiest puzzle difficulty worth searching for it.
 */
enum class TrainerMethod(
    val techniqueName: String,
    val usesNotes: Boolean,
    val minLevel: Int,
    val explanation: String,
) {
    HiddenSingle(
        "Hidden Single", usesNotes = false, minLevel = 2,
        "A digit can go in only one cell of a row, column or box, because it " +
            "already appears in a line or box crossing every other cell. Place the " +
            "digit in the one cell left for it.",
    ),
    NakedSingle(
        "Naked Single", usesNotes = false, minLevel = 1,
        "Every digit but one already appears in the cell's row, column or box. " +
            "The one digit left must go in that cell.",
    ),
    NakedPair(
        "Naked Pair", usesNotes = true, minLevel = 3,
        "Two cells in a row, column or box have the same two candidates and no " +
            "others. Those two digits must fill those two cells, so remove them from " +
            "every other cell in that unit.",
    ),
    HiddenPair(
        "Hidden Pair", usesNotes = true, minLevel = 3,
        "Two digits can only go in the same two cells of a row, column or box. " +
            "Those cells must hold those two digits, so remove every other candidate " +
            "from them.",
    ),
    PointingPair(
        "Pointing Pair", usesNotes = true, minLevel = 3,
        "If all of the candidates for a number in a box fall in the same row or " +
            "column, that candidate can be removed from the cells in that row or column " +
            "that are outside the box.",
    ),
    BoxLineReduction(
        "Box-Line Reduction", usesNotes = true, minLevel = 3,
        "If all of the candidates for a number in a row or column fall inside the " +
            "same box, that candidate can be removed from the cells in that box that are " +
            "not in the row or column.",
    ),
    NakedTriple(
        "Naked Triple", usesNotes = true, minLevel = 5,
        "Three cells in a row, column or box have only three digits between them " +
            "as candidates. Those digits must fill those cells, so remove them from " +
            "every other cell in that unit.",
    ),
    HiddenTriple(
        "Hidden Triple", usesNotes = true, minLevel = 5,
        "Three digits can only go in the same three cells of a row, column or box. " +
            "Those cells must hold those digits, so remove every other candidate from " +
            "them.",
    ),
    XWing(
        "X-Wing", usesNotes = true, minLevel = 6,
        "In two rows, a digit's only candidates sit in the same two columns, " +
            "forming a rectangle. The digit must take opposite corners, so remove it " +
            "from the rest of both columns (or swap rows and columns).",
    ),
    EmptyRectangle(
        "Empty Rectangle", usesNotes = true, minLevel = 7,
        "Inside a box, a digit's candidates form an L along one row and one column. " +
            "Find a line outside the box where the digit has only two spots, one of them " +
            "in line with one arm of the L. Remove the digit where the other spot's line " +
            "crosses the L's other arm.",
    ),
    Swordfish(
        "Swordfish", usesNotes = true, minLevel = 7,
        "An X-Wing across three lines: in three rows, a digit's candidates all sit " +
            "within the same three columns. The digit must use those columns in those " +
            "rows, so remove it from the rest of the three columns (or swap rows and " +
            "columns).",
    ),
    XYWing(
        "XY-Wing", usesNotes = true, minLevel = 7,
        "A pivot cell with candidates XY sees one cell with XZ and another with YZ. " +
            "Whatever the pivot becomes, one of those two cells must be Z, so remove Z " +
            "from every cell that sees both of them.",
    ),
}

/** The method's name for a count of more than one, e.g. "Naked Pairs". */
val TrainerMethod.pluralName: String
    get() = if (techniqueName.endsWith("fish")) techniqueName else "${techniqueName}s"
