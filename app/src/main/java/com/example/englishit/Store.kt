package com.example.englishit

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("englishit", Context.MODE_PRIVATE)

    var xp by mutableIntStateOf(sp.getInt("xp", 0)); private set
    var streak by mutableIntStateOf(sp.getInt("streak", 0)); private set
    var todayXp by mutableIntStateOf(0); private set
    var rev by mutableIntStateOf(0); private set   // bumps when stats change (to refresh UI)

    /** true until the user dismisses the first-launch welcome notice */
    var showWelcome by mutableStateOf(!sp.getBoolean("welcomed", false)); private set

    private val stats = HashMap<String, IntArray>()  // id -> [attempts, wrong, box, dueDay]
    private val best = HashMap<String, Int>()
    private val rounds = HashMap<String, Int>()      // lessonId -> number of completed rounds
    private val sessions = HashMap<String, Snap>()   // lessonId -> unfinished round

    private fun JSONArray.strs(): List<String> = (0 until length()).map { getString(it) }

    init {
        val today = today()
        if (sp.getLong("xpDay", -1L) == today) todayXp = sp.getInt("todayXp", 0)
        try {
            val o = JSONObject(sp.getString("stats", "{}") ?: "{}")
            for (k in o.keys()) {
                val a = o.getJSONArray(k)
                stats[k] = intArrayOf(a.getInt(0), a.getInt(1), a.getInt(2), a.getInt(3))
            }
            val b = JSONObject(sp.getString("best", "{}") ?: "{}")
            for (k in b.keys()) best[k] = b.getInt(k)
        } catch (e: Exception) {
            // ignore corrupted data
        }
        try {
            val r = JSONObject(sp.getString("rounds", "{}") ?: "{}")
            for (k in r.keys()) rounds[k] = r.getInt(k)
        } catch (e: Exception) {
            // ignore corrupted data
        }
        try {
            val s = JSONObject(sp.getString("sessions", "{}") ?: "{}")
            for (k in s.keys()) {
                val o = s.getJSONObject(k)
                sessions[k] = Snap(
                    ids = o.getJSONArray("ids").strs(),
                    idx = o.getInt("i"),
                    hearts = o.getInt("h"),
                    correct = o.getInt("c"),
                    total = o.getInt("t"),
                    retried = o.getJSONArray("r").strs(),
                    wrongs = o.getJSONArray("w").strs()
                )
            }
        } catch (e: Exception) {
            sessions.clear()
        }
    }

    fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86400000L
    }

    fun dismissWelcome() {
        showWelcome = false
        sp.edit().putBoolean("welcomed", true).apply()
    }

    fun addXp(n: Int) {
        xp += n
        val t = today()
        if (sp.getLong("xpDay", -1L) != t) todayXp = 0
        todayXp += n
        sp.edit().putInt("xp", xp).putLong("xpDay", t).putInt("todayXp", todayXp).apply()
    }

    fun touchStreak() {
        val t = today()
        val last = sp.getLong("lastDay", -1L)
        if (last == t) return
        streak = if (last == t - 1) streak + 1 else 1
        sp.edit().putInt("streak", streak).putLong("lastDay", t).apply()
    }

    fun record(id: String, correct: Boolean) {
        val s = stats.getOrPut(id) { intArrayOf(0, 0, 0, 0) }
        s[0] += 1
        if (!correct) s[1] += 1
        s[2] = Srs.nextBox(s[2], correct)
        s[3] = Srs.dueDay(today(), s[2]).toInt()
        val o = JSONObject()
        for ((k, v) in stats) o.put(k, JSONArray(listOf(v[0], v[1], v[2], v[3])))
        sp.edit().putString("stats", o.toString()).apply()
        rev += 1
    }

    fun attempts(id: String) = stats[id]?.get(0) ?: 0
    fun wrongs(id: String) = stats[id]?.get(1) ?: 0
    fun isDue(id: String): Boolean {
        val s = stats[id] ?: return false
        return s[0] > 0 && s[3] <= today()
    }

    fun bestScore(lessonId: String) = best[lessonId] ?: -1
    fun setBest(lessonId: String, pct: Int) {
        if (pct > (best[lessonId] ?: -1)) {
            best[lessonId] = pct
            val o = JSONObject()
            for ((k, v) in best) o.put(k, v)
            sp.edit().putString("best", o.toString()).apply()
            rev += 1
        }
    }

    // ---------- path progress: finished rounds ----------
    fun roundsOf(lessonId: String) = rounds[lessonId] ?: 0
    fun totalRounds() = rounds.values.sum()

    fun completeRound(lessonId: String) {
        rounds[lessonId] = roundsOf(lessonId) + 1
        val o = JSONObject()
        for ((k, v) in rounds) o.put(k, v)
        sp.edit().putString("rounds", o.toString()).apply()
        rev += 1
    }

    // ---------- path progress: unfinished round (resume) ----------
    fun savedSession(lessonId: String): Snap? = sessions[lessonId]

    fun saveSession(lessonId: String, s: Snap) {
        sessions[lessonId] = s
        writeSessions()
    }

    fun clearSession(lessonId: String) {
        if (sessions.remove(lessonId) != null) writeSessions()
    }

    private fun writeSessions() {
        val o = JSONObject()
        for ((k, v) in sessions) {
            val j = JSONObject()
            j.put("ids", JSONArray(v.ids))
            j.put("i", v.idx)
            j.put("h", v.hearts)
            j.put("c", v.correct)
            j.put("t", v.total)
            j.put("r", JSONArray(v.retried))
            j.put("w", JSONArray(v.wrongs))
            o.put(k, j)
        }
        sp.edit().putString("sessions", o.toString()).apply()
        rev += 1
    }
}
