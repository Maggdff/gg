package com.example.englishit

data class Ex(
    val id: String,
    val t: String,                       // mcq | fill | tf | order | match | write
    val q: String,
    val opts: List<String> = emptyList(),
    val a: List<String> = emptyList(),   // accepted answers (mcq: correct option text)
    val tf: Boolean = true,
    val pairs: List<Pair<String, String>> = emptyList(),
    val model: String = "",
    val exp: String = "",
    val g: String = "",                  // grammar id for the "Why?" button
    val extra: List<String> = emptyList()
)

data class Auto(val cats: List<String>, val n: Int, val match: Int)

data class Lesson(
    val id: String,
    val title: String,
    val titleAr: String,
    val text: String,
    val exercises: List<Ex>,
    val auto: Auto?
)

data class UnitData(val id: String, val title: String, val titleAr: String, val lessons: List<Lesson>)

data class Vocab(val en: String, val ar: String, val def: String, val cat: String)

data class Grammar(
    val id: String,
    val title: String,
    val titleAr: String,
    val notesAr: String,
    val rules: List<String>,
    val examples: List<String>
)

fun isAr(s: String): Boolean = s.any { it in '\u0600'..'\u06FF' }

fun answerText(ex: Ex): String = when (ex.t) {
    "fill" -> ex.a.joinToString(" / ")
    "tf" -> if (ex.tf) "True (صح)" else "False (خطأ)"
    "match" -> ex.pairs.joinToString("\n") { it.first + " = " + it.second }
    "write" -> ex.model
    else -> ex.a.firstOrNull() ?: ""
}

fun spokenText(ex: Ex): String = when (ex.t) {
    "fill" -> ex.q.substringAfter(": ", ex.q).replace("___", ex.a.firstOrNull() ?: "")
    "mcq", "order" -> ex.a.firstOrNull() ?: ""
    else -> ""
}
