package com.example.englishit

object AnswerChecker {
    fun norm(s: String): String =
        s.lowercase()
            .replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .trim()
            .replace(Regex("\\s+"), " ")
            .trimEnd('.', ',', '!', '?', ';', ':')
            .trim()

    fun check(input: String, accepted: List<String>): Boolean {
        val n = norm(input)
        if (n.isEmpty()) return false
        return accepted.any { norm(it) == n }
    }
}

object Srs {
    private val intervals = intArrayOf(0, 1, 2, 4, 8, 16)
    const val MAX_BOX = 5

    fun nextBox(box: Int, correct: Boolean): Int =
        if (correct) minOf(box + 1, MAX_BOX) else 0

    fun dueDay(today: Long, box: Int): Long = today + intervals[box.coerceIn(0, MAX_BOX)]
}
