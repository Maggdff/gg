package com.example.englishit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GreenBg = Color(0x5558CC02)
val RedBg = Color(0x55FF4B4B)
val BlueBg = Color(0x551CB0F6)

sealed interface Screen {
    object Home : Screen
    class Play(
        val title: String,
        val exercises: List<Ex>,
        val text: String,
        val lessonId: String?,
        val exam: Boolean
    ) : Screen

    class Flash(val items: List<Vocab>) : Screen
}

// ---------- small helpers ----------
@Composable
fun T(
    text: String,
    size: TextUnit = 18.sp,
    bold: Boolean = false,
    color: Color = Color.Unspecified,
    center: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val ar = isAr(text)
    Text(
        text = text,
        modifier = modifier,
        fontSize = size,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
        textAlign = if (center) TextAlign.Center else if (ar) TextAlign.Right else TextAlign.Left,
        style = TextStyle(textDirection = if (ar) TextDirection.Rtl else TextDirection.Ltr)
    )
}

@Composable
fun ChoiceBox(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }
    ) {
        Box(Modifier.padding(12.dp)) { T(text, 16.sp) }
    }
}

fun speakable(s: String): String = s.replace(Regex("\\(.*?\\)"), "").trim()

// ---------- root ----------
@Composable
fun App(store: Store) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = screen !is Screen.Home) { screen = Screen.Home }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (val s = screen) {
            is Screen.Home -> HomeScreen(store, tab, { tab = it }, { screen = it })
            is Screen.Play -> PlayScreen(s, store) { screen = Screen.Home }
            is Screen.Flash -> FlashScreen(s.items) { screen = Screen.Home }
        }
    }
}

@Composable
fun HomeScreen(store: Store, tab: Int, onTab: (Int) -> Unit, go: (Screen) -> Unit) {
    Scaffold(
        topBar = { TopStats(store) },
        bottomBar = {
            NavigationBar {
                val items = listOf("🗺️" to "المسار", "🔁" to "مراجعة", "📚" to "الكلمات", "📘" to "القواعد")
                items.forEachIndexed { i, p ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { onTab(i) },
                        icon = { Text(p.first, fontSize = 22.sp) },
                        label = { Text(p.second) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> PathTab(store, go)
                1 -> ReviewTab(store, go)
                2 -> WordsTab(go)
                else -> GrammarTab()
            }
        }
    }
}

@Composable
fun TopStats(store: Store) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🔥 ${store.streak}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("⭐ ${store.xp} XP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("🎯 ${store.todayXp}/50", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- path ----------
@Composable
fun PathTab(store: Store, go: (Screen) -> Unit) {
    val refresh = store.rev
    val offsets = listOf(0, 48, 80, 48)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        Content.units.forEach { u ->
            item(key = "h-" + u.id) {
                Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        T(u.titleAr, 18.sp, true)
                        T(u.title, 14.sp)
                    }
                }
            }
            itemsIndexed(u.lessons, key = { _, l -> l.id }) { i, l ->
                val best = store.bestScore(l.id)
                val color = when {
                    best >= 100 -> Color(0xFFFFC800)
                    best >= 0 -> Color(0xFF58A700)
                    else -> Color(0xFF1CB0F6)
                }
                val num = l.id.dropWhile { it != 'l' }.drop(1)
                val label = when {
                    best >= 100 -> "⭐"
                    best >= 0 -> "✓"
                    else -> num
                }
                Box(Modifier.fillMaxWidth().padding(start = offsets[i % 4].dp, top = 14.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(76.dp).clip(CircleShape).clickable {
                                go(Screen.Play(l.title, Content.sessionFor(l), l.text, l.id, false))
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(l.titleAr, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(l.title, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------- review / exam / cram ----------
@Composable
fun ReviewTab(store: Store, go: (Screen) -> Unit) {
    val refresh = store.rev
    val all = remember { Content.allExercises() }
    val due = all.filter { store.isDue(it.id) }
    val weak = all.filter { store.wrongs(it.id) > 0 }.sortedWith(
        compareByDescending<Ex> { store.wrongs(it.id).toDouble() / store.attempts(it.id) }
            .thenByDescending { store.wrongs(it.id) }
    )
    var sel by remember { mutableStateOf(setOf("u1", "u2", "x")) }
    var cnt by remember { mutableIntStateOf(20) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                T("📅 مراجعة اليوم", 18.sp, true)
                T("عناصر مستحقة للمراجعة الآن: ${due.size}", 15.sp)
                Button(
                    enabled = due.isNotEmpty(),
                    onClick = { go(Screen.Play("Review", due.shuffled().take(20), "", null, false)) }
                ) { Text("ابدأ المراجعة") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                T("❌ الأخطاء والنقاط الضعيفة (${weak.size})", 18.sp, true)
                weak.take(5).forEach {
                    T("• " + it.q.take(70) + "  (أخطاء: ${store.wrongs(it.id)})", 13.sp)
                }
                Button(
                    enabled = weak.isNotEmpty(),
                    onClick = { go(Screen.Play("Mistakes", weak.take(20).shuffled(), "", null, false)) }
                ) { Text("تدرّب على أخطائي") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                T("⚡ خطة الحفظ لبكرة (حوالي 30 دقيقة)", 18.sp, true)
                T("40 سؤال: أضعف عناصرك أولاً ثم المستحق ثم الجديد من كل الوحدات.", 14.sp)
                Button(onClick = {
                    val unseen = all.filter { store.attempts(it.id) == 0 }.shuffled()
                    val plan = (weak.take(25) + due.shuffled() + unseen).distinctBy { it.id }.take(40)
                    go(Screen.Play("Cram", plan, "", null, false))
                }) { Text("ابدأ الخطة") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                T("📝 وضع الامتحان (بدون تلميحات)", 18.sp, true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("u1" to "وحدة 1", "u2" to "وحدة 2", "x" to "الملازم").forEach { p ->
                        FilterChip(
                            selected = p.first in sel,
                            onClick = { sel = if (p.first in sel) sel - p.first else sel + p.first },
                            label = { Text(p.second) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30, 40).forEach { n ->
                        FilterChip(selected = cnt == n, onClick = { cnt = n }, label = { Text("$n") })
                    }
                }
                Button(
                    enabled = sel.isNotEmpty(),
                    onClick = {
                        go(Screen.Play("Exam", Content.allExercises(sel).shuffled().take(cnt), "", null, true))
                    }
                ) { Text("ابدأ الامتحان") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                T("📊 الدقة حسب الوحدة", 18.sp, true)
                Content.units.forEach { u ->
                    var att = 0
                    var wr = 0
                    Content.unitExercises(u).forEach { att += store.attempts(it.id); wr += store.wrongs(it.id) }
                    val acc = if (att == 0) "—" else "${(att - wr) * 100 / att}%"
                    T("${u.titleAr}:  $acc", 15.sp)
                }
            }
        }
    }
}

// ---------- words / search ----------
@Composable
fun WordsTab(go: (Screen) -> Unit) {
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<Grammar?>(null) }
    val res = Content.vocab.filter {
        q.isBlank() || it.en.contains(q, true) || it.ar.contains(q) || it.def.contains(q, true)
    }
    val gr = if (q.isBlank()) emptyList() else Content.grammar.filter { g ->
        g.title.contains(q, true) || g.titleAr.contains(q) || g.rules.any { it.contains(q, true) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            label = { Text("ابحث عن كلمة أو اختصار أو قاعدة") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            enabled = res.isNotEmpty(),
            onClick = { go(Screen.Flash(res.shuffled())) },
            modifier = Modifier.padding(vertical = 8.dp)
        ) { Text("🃏 بطاقات تعليمية (${res.size})") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(gr) { g ->
                Card(Modifier.fillMaxWidth().clickable { open = g }) {
                    Column(Modifier.padding(12.dp)) {
                        T("📘 " + g.title, 16.sp, true)
                        T(g.titleAr, 14.sp)
                    }
                }
            }
            items(res) { v ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(v.en, 17.sp, true)
                            T(v.ar, 15.sp)
                            if (v.def.isNotBlank()) T(v.def, 13.sp)
                        }
                        Text(
                            "🔊", fontSize = 24.sp,
                            modifier = Modifier.clickable { Tts.speak(speakable(v.en)) }.padding(8.dp)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    open?.let { g -> GrammarDialog(g) { open = null } }
}

// ---------- grammar ----------
@Composable
fun GrammarTab() {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Content.grammar) { g ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) { GrammarBody(g) }
            }
        }
    }
}

@Composable
fun GrammarBody(g: Grammar) {
    T(g.title, 20.sp, true)
    T(g.titleAr, 16.sp, true)
    Spacer(Modifier.height(8.dp))
    T(g.notesAr, 15.sp)
    Spacer(Modifier.height(8.dp))
    T("القواعد / Rules", 15.sp, true)
    (g.rules + g.examples).forEach { r ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("• $r", 15.sp, modifier = Modifier.weight(1f))
            Text("🔊", fontSize = 20.sp, modifier = Modifier.clickable { Tts.speak(r) }.padding(8.dp))
        }
    }
}

@Composable
fun GrammarDialog(g: Grammar, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { GrammarBody(g) } }
    )
}

// ---------- flashcards ----------
@Composable
fun FlashScreen(items: List<Vocab>, onExit: () -> Unit) {
    val queue = remember { mutableStateListOf<Vocab>().apply { addAll(items) } }
    var flip by remember { mutableStateOf(false) }
    var known by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✕", fontSize = 22.sp, modifier = Modifier.clickable { onExit() }.padding(8.dp))
            T("متبقي: ${queue.size}   |   عرفتها: $known", 15.sp)
        }
        if (queue.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 64.sp)
                T("خلصت كل البطاقات!", 20.sp, true, center = true)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onExit) { Text("رجوع") }
            }
        } else {
            val v = queue[0]
            Card(
                onClick = { flip = !flip },
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp)
            ) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!flip) {
                            T(v.en, 28.sp, true, center = true)
                            Text("🔊", fontSize = 30.sp, modifier = Modifier.clickable { Tts.speak(speakable(v.en)) }.padding(8.dp))
                            T("اضغط لقلب البطاقة", 13.sp, center = true)
                        } else {
                            T(v.ar, 24.sp, true, center = true)
                            if (v.def.isNotBlank()) T(v.def, 16.sp, center = true)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { val x = queue.removeAt(0); queue.add(x); flip = false },
                    modifier = Modifier.weight(1f)
                ) { Text("لم أعرفها ❌") }
                Button(
                    onClick = { queue.removeAt(0); known++; flip = false },
                    modifier = Modifier.weight(1f)
                ) { Text("عرفتها ✅") }
            }
        }
    }
}

// ---------- session ----------
@Composable
fun PlayScreen(s: Screen.Play, store: Store, onExit: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    key(attempt) { Session(s, store, onExit) { attempt++ } }
}

@Composable
fun Session(s: Screen.Play, store: Store, onExit: () -> Unit, onRetry: () -> Unit) {
    val queue = remember { mutableStateListOf<Ex>().apply { addAll(s.exercises) } }
    var idx by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(5) }
    var correct by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<Boolean?>(null) }
    val retried = remember { HashSet<String>() }
    val wrongs = remember { mutableStateListOf<Ex>() }
    var showText by remember { mutableStateOf(false) }
    var why by remember { mutableStateOf<String?>(null) }

    val finished = idx >= queue.size
    val dead = !s.exam && hearts <= 0 && feedback == null

    LaunchedEffect(finished) {
        if (finished && (s.exam || hearts > 0) && total > 0) {
            store.addXp(20)
            store.touchStreak()
            s.lessonId?.let { store.setBest(it, correct * 100 / total) }
        }
    }

    fun submit(ok: Boolean) {
        val ex = queue[idx]
        total++
        if (ok) {
            correct++
            store.addXp(10)
        } else {
            wrongs.add(ex)
            if (!s.exam) {
                hearts--
                if (ex.id !in retried && ex.t != "write") {
                    retried.add(ex.id)
                    queue.add(ex)
                }
            }
        }
        store.record(ex.id, ok)
        store.touchStreak()
        if (s.exam) idx++ else feedback = ok
    }

    when {
        dead -> EndView(
            emoji = "💔", title = "خلصت القلوب!", subtitle = "جرّب الدرس مرة تانية — القلوب بترجع 5.",
            wrongs = emptyList(), onRetry = onRetry, onExit = onExit
        )
        finished -> {
            val pct = if (total == 0) 0 else correct * 100 / total
            EndView(
                emoji = if (pct >= 80) "🎉" else "💪",
                title = "النتيجة: $correct / $total  ($pct%)",
                subtitle = "+${correct * 10 + 20} XP",
                wrongs = wrongs.distinctBy { it.id }, onRetry = onRetry, onExit = onExit
            )
        }
        else -> {
            val ex = queue[idx]
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✕", fontSize = 22.sp, modifier = Modifier.clickable { onExit() }.padding(8.dp))
                    LinearProgressIndicator(
                        progress = { idx.toFloat() / queue.size },
                        modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp))
                    )
                    if (!s.exam) Text("❤️ $hearts", fontSize = 18.sp, modifier = Modifier.padding(start = 10.dp))
                    if (s.text.isNotBlank()) {
                        Text("📖", fontSize = 22.sp, modifier = Modifier.clickable { showText = true }.padding(8.dp))
                    }
                }
                Box(Modifier.weight(1f)) {
                    key(idx) { ExerciseView(ex, feedback != null) { submit(it) } }
                }
                feedback?.let { ok ->
                    FeedbackBar(ok, ex, onWhy = { why = ex.g }) { feedback = null; idx++ }
                }
            }
        }
    }

    if (showText) {
        AlertDialog(
            onDismissRequest = { showText = false },
            confirmButton = { TextButton(onClick = { showText = false }) { Text("إغلاق") } },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { T(s.text, 17.sp) } }
        )
    }
    why?.let { id ->
        Content.grammar.firstOrNull { it.id == id }?.let { GrammarDialog(it) { why = null } }
    }
}

@Composable
fun FeedbackBar(ok: Boolean, ex: Ex, onWhy: () -> Unit, onContinue: () -> Unit) {
    Surface(color = if (ok) GreenBg else RedBg) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T(if (ok) "✅ صحيح!" else "❌ إجابة غير صحيحة", 20.sp, true)
            if (!ok && ex.t != "write") {
                T("الإجابة الصحيحة:", 14.sp)
                T(answerText(ex), 17.sp, true)
            }
            if (ex.exp.isNotBlank()) T(ex.exp, 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ex.g.isNotBlank()) OutlinedButton(onClick = onWhy) { Text("لماذا؟") }
                val spoken = spokenText(ex)
                if (spoken.isNotBlank()) {
                    Text("🔊", fontSize = 24.sp, modifier = Modifier.clickable { Tts.speak(spoken) }.padding(8.dp))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onContinue) { Text("متابعة") }
            }
        }
    }
}

@Composable
fun EndView(
    emoji: String, title: String, subtitle: String,
    wrongs: List<Ex>, onRetry: () -> Unit, onExit: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 64.sp)
        T(title, 22.sp, true, center = true)
        T(subtitle, 16.sp, center = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onExit) { Text("تم") }
            OutlinedButton(onClick = onRetry) { Text("إعادة") }
        }
        if (wrongs.isNotEmpty()) {
            T("الأسئلة الخاطئة والإجابات الصحيحة:", 17.sp, true)
            wrongs.forEach { w ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T(w.q, 15.sp)
                        if (w.t != "write") T("✔ " + answerText(w), 15.sp, true)
                        if (w.exp.isNotBlank()) T(w.exp, 13.sp)
                    }
                }
            }
        }
    }
}
