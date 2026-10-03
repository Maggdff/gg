package com.example.englishit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Content {
    var units: List<UnitData> = emptyList()
    var vocab: List<Vocab> = emptyList()
    var grammar: List<Grammar> = emptyList()
    val byId = HashMap<String, Ex>()
    private val autoPool = HashMap<String, List<Ex>>()

    private fun read(ctx: Context, name: String): String =
        ctx.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun JSONArray.strList(): List<String> = (0 until length()).map { getString(it) }

    fun load(ctx: Context) {
        if (units.isNotEmpty()) return
        vocab = parseVocab(JSONObject(read(ctx, "vocab.json")).getJSONArray("items"))
        grammar = parseGrammar(JSONObject(read(ctx, "grammar.json")).getJSONArray("items"))
        val all = ArrayList<UnitData>()
        for (f in listOf("u1.json", "u2.json", "extra.json")) {
            val arr = JSONObject(read(ctx, f)).getJSONArray("units")
            for (i in 0 until arr.length()) all.add(parseUnit(arr.getJSONObject(i)))
        }
        units = all
        for (u in units) for (l in u.lessons) {
            for (e in l.exercises) byId[e.id] = e
            val au = l.auto
            if (au != null) {
                val pool = vocab.filter { it.cat in au.cats }
                val gen = pool.flatMap { genVocab(it, pool) }
                autoPool[l.id] = gen
                for (e in gen) byId[e.id] = e
            }
        }
    }

    private fun parseVocab(a: JSONArray) = (0 until a.length()).map {
        val o = a.getJSONObject(it)
        Vocab(o.getString("en"), o.getString("ar"), o.optString("def", ""), o.getString("cat"))
    }

    private fun parseGrammar(a: JSONArray) = (0 until a.length()).map {
        val o = a.getJSONObject(it)
        Grammar(
            o.getString("id"), o.getString("title"), o.getString("titleAr"), o.getString("notesAr"),
            o.getJSONArray("rules").strList(), o.getJSONArray("examples").strList()
        )
    }

    private fun parseUnit(o: JSONObject): UnitData {
        val ls = o.getJSONArray("lessons")
        val lessons = (0 until ls.length()).map { parseLesson(ls.getJSONObject(it)) }
        return UnitData(o.getString("id"), o.getString("title"), o.getString("titleAr"), lessons)
    }

    private fun parseLesson(o: JSONObject): Lesson {
        val id = o.getString("id")
        val boxes = HashMap<String, List<String>>()
        o.optJSONObject("boxes")?.let { b -> for (k in b.keys()) boxes[k] = b.getJSONArray(k).strList() }
        val exArr = o.getJSONArray("ex")
        val exs = (0 until exArr.length()).map { parseEx("$id-${it + 1}", exArr.getJSONObject(it), boxes) }
        val au = o.optJSONObject("auto")?.let {
            Auto(it.getJSONArray("cats").strList(), it.getInt("n"), it.optInt("match", 0))
        }
        return Lesson(id, o.getString("title"), o.getString("titleAr"), o.optString("text", ""), exs, au)
    }

    private fun parseEx(id: String, o: JSONObject, boxes: Map<String, List<String>>): Ex {
        val t = o.getString("t")
        var opts = o.optJSONArray("opts")?.strList() ?: emptyList()
        var a: List<String> = emptyList()
        var tf = true
        when (t) {
            "mcq" -> a = listOf(opts[o.getInt("a")])
            "fill" -> {
                a = o.getJSONArray("a").strList()
                val b = o.optString("b", "")
                if (b.isNotEmpty()) opts = boxes[b] ?: emptyList()
            }
            "order" -> a = listOf(o.getString("a"))
            "tf" -> tf = o.getBoolean("a")
        }
        val pairs = o.optJSONArray("pairs")?.let { arr ->
            (0 until arr.length()).map { val p = arr.getJSONArray(it); p.getString(0) to p.getString(1) }
        } ?: emptyList()
        return Ex(
            id = id, t = t, q = o.optString("q", ""), opts = opts, a = a, tf = tf, pairs = pairs,
            model = o.optString("model", ""), exp = o.optString("exp", ""), g = o.optString("g", ""),
            extra = o.optJSONArray("extra")?.strList() ?: emptyList()
        )
    }

    // ---------- auto-generated vocabulary questions ----------
    private fun distinctOpts(correct: String, others: List<String>): List<String> =
        (others.filter { it != correct && it.isNotBlank() }.distinct().shuffled().take(3) + correct).distinct()

    private fun genVocab(v: Vocab, pool: List<Vocab>): List<Ex> {
        val out = ArrayList<Ex>()
        val others = pool.filter { it.en != v.en }
        val arOpts = distinctOpts(v.ar, others.map { it.ar })
        if (arOpts.size >= 2) {
            out.add(Ex("auto:m:${v.cat}:${v.en}", "mcq", "ما معنى: ${v.en}", opts = arOpts, a = listOf(v.ar),
                exp = "${v.en} = ${v.ar}"))
        }
        if (v.def.isNotBlank() && v.cat != "adj") {
            val defOpts = distinctOpts(v.def, others.map { it.def })
            if (defOpts.size >= 2) {
                val q = if (v.cat == "acronym") "What does ${v.en} stand for?" else "What is the definition of: ${v.en}?"
                out.add(Ex("auto:d:${v.cat}:${v.en}", "mcq", q, opts = defOpts, a = listOf(v.def),
                    exp = "${v.en} = ${v.def}  (${v.ar})"))
            }
            if (v.cat != "acronym") {
                val enOpts = distinctOpts(v.en, others.map { it.en })
                if (enOpts.size >= 2) {
                    out.add(Ex("auto:w:${v.cat}:${v.en}", "mcq", "Which word matches this definition?\n\"${v.def}\"",
                        opts = enOpts, a = listOf(v.en), exp = "${v.en} = ${v.ar}"))
                }
            }
        }
        return out
    }

    private fun matchGroup(items: List<Vocab>): Ex? {
        val pick = items.shuffled().take(5)
        if (pick.size < 3) return null
        val pairs = pick.map { it.en to (if (it.def.isNotBlank()) it.def else it.ar) }
        return Ex("auto:match:" + pick.joinToString("|") { it.en }, "match",
            "Match each word with its meaning", pairs = pairs, exp = "راجع التعريفات في تبويب الكلمات")
    }

    // ---------- sessions ----------
    fun sessionFor(l: Lesson): List<Ex> {
        val list = ArrayList<Ex>(l.exercises)
        val au = l.auto
        if (au != null) {
            val pool = autoPool[l.id].orEmpty()
            list.addAll(pool.shuffled().take(au.n))
            val items = vocab.filter { it.cat in au.cats && (it.def.isNotBlank() || it.ar.isNotBlank()) }
            repeat(au.match) { matchGroup(items)?.let { list.add(it) } }
        }
        return list
    }

    fun allExercises(unitIds: Set<String>? = null): List<Ex> {
        val out = ArrayList<Ex>()
        for (u in units) {
            if (unitIds != null && u.id !in unitIds) continue
            for (l in u.lessons) {
                out.addAll(l.exercises.filter { it.t != "write" })
                out.addAll(autoPool[l.id].orEmpty())
            }
        }
        return out
    }

    /** Rebuilds an exercise from its id (used to resume an unfinished round). */
    fun exById(id: String): Ex? {
        byId[id]?.let { return it }
        if (id.startsWith("auto:match:")) {
            val items = id.removePrefix("auto:match:").split("|")
                .mapNotNull { n -> vocab.firstOrNull { it.en == n } }
            if (items.size < 3) return null
            val pairs = items.map { it.en to (if (it.def.isNotBlank()) it.def else it.ar) }
            return Ex(id, "match", "Match each word with its meaning", pairs = pairs,
                exp = "راجع التعريفات في تبويب الكلمات")
        }
        return null
    }

    fun unitExercises(u: UnitData): List<Ex> =
        u.lessons.flatMap { it.exercises + autoPool[it.id].orEmpty() }
}
