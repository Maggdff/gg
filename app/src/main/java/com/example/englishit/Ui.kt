package com.example.englishit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

sealed interface Screen {
    object Home : Screen
    class Play(
        val title: String,
        val exercises: List<Ex>,
        val text: String,
        val lessonId: String?,
        val exam: Boolean,
        val resume: Snap? = null
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

fun speakable(s: String): String = s.replace(Regex("\\(.*?\\)"), "").trim()

@Composable
fun CloseBtn(onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(brand.cardBorder).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = brand.muted) }
}

@Composable
fun SpeakBtn(text: String, onGradient: Boolean = false) {
    val b = brand
    Box(
        Modifier.size(46.dp).clip(CircleShape)
            .background(if (onGradient) Color.White.copy(alpha = 0.25f) else b.primary.copy(alpha = 0.14f))
            .clickable { Tts.speak(text) },
        contentAlignment = Alignment.Center
    ) { Text("🔊", fontSize = 22.sp) }
}

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
        if (store.showWelcome) WelcomeDialog { store.dismissWelcome() }
    }
}

@Composable
fun WelcomeDialog(onStart: () -> Unit) {
    val b = brand
    AppDialog(onDismiss = {}, dismissible = false) {
        LogoMark(88.dp)
        Text("English", fontSize = 32.sp, fontWeight = FontWeight.Black, color = b.primary)
        T("أهلاً بك! 👋", 20.sp, true, center = true)
        T(
            "مراجعة اللغة الإنجليزية لتقنية المعلومات — بدون إنترنت وبدون إعلانات.",
            14.sp, color = b.muted, center = true
        )
        Box(
            Modifier.clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(b.gradA, b.gradB)))
                .padding(horizontal = 26.dp, vertical = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("من تطوير", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Text("maggd alosimi", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(4.dp))
        ChunkyButton("ابدأ التعلّم 🚀", onStart, Modifier.fillMaxWidth())
    }
}

@Composable
fun HomeScreen(store: Store, tab: Int, onTab: (Int) -> Unit, go: (Screen) -> Unit) {
    val b = brand
    Scaffold(
        topBar = { TopStats(store) },
        bottomBar = {
            NavigationBar(containerColor = b.card, tonalElevation = 0.dp) {
                val items = listOf("🗺️" to "المسار", "🔁" to "مراجعة", "📚" to "الكلمات", "📘" to "القواعد")
                items.forEachIndexed { i, p ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { onTab(i) },
                        icon = { Text(p.first, fontSize = 22.sp, modifier = Modifier.alpha(if (tab == i) 1f else 0.55f)) },
                        label = {
                            Text(
                                p.second, fontSize = 12.sp,
                                fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = b.primary,
                            unselectedTextColor = b.muted,
                            indicatorColor = b.primary.copy(alpha = 0.16f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            Crossfade(targetState = tab, label = "tab") { t ->
                when (t) {
                    0 -> PathTab(store, go)
                    1 -> ReviewTab(store, go)
                    2 -> WordsTab(go)
                    else -> GrammarTab()
                }
            }
        }
    }
}

@Composable
fun TopStats(store: Store) {
    val b = brand
    val goal = 50
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(listOf(b.gradA, b.gradB)))
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogoMark(38.dp, inverted = true)
                Text("English", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Pill("🔥 ${store.streak}", Color.White.copy(alpha = 0.22f), Color.White, size = 15.sp)
                Pill("⭐ ${store.xp}", Color.White.copy(alpha = 0.22f), Color.White, size = 15.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (store.todayXp >= goal) "🎉 هدف اليوم" else "🎯 هدف اليوم",
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                AppProgress(
                    store.todayXp.toFloat() / goal, Modifier.weight(1f), 10.dp,
                    track = Color.White.copy(alpha = 0.28f), brush = SolidColor(b.gold)
                )
                Text("${store.todayXp}/$goal", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------- path ----------
private fun sway(i: Int): Float {
    val p = floatArrayOf(0f, 40f, 72f, 40f, 0f, -40f, -72f, -40f)
    return p[((i % 8) + 8) % 8]
}

@Composable
fun PathTab(store: Store, go: (Screen) -> Unit) {
    val b = brand
    @Suppress("UNUSED_VARIABLE") val rev = store.rev
    val flat = Content.units.flatMap { it.lessons }
    // the lesson to continue: an unfinished round first, otherwise the first lesson never completed
    val current = flat.firstOrNull { store.savedSession(it.id) != null }
        ?: flat.firstOrNull { store.bestScore(it.id) < 0 }
    var startIndex = 0
    var c = 0
    for (u in Content.units) {
        c += 1
        for (l in u.lessons) {
            if (l.id == current?.id) startIndex = c
            c += 1
        }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, startIndex - 1))
    var pending by remember { mutableStateOf<Lesson?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
        ) {
            Content.units.forEachIndexed { ui, u ->
                item(key = "h-" + u.id) {
                    val done = u.lessons.count { store.bestScore(it.id) >= 0 }
                    UnitHeader(u, ui, done, u.lessons.size)
                }
                itemsIndexed(u.lessons, key = { _, l -> l.id }) { i, l ->
                    LessonNode(l, i, u.lessons.size, store, l.id == current?.id) {
                        if (store.savedSession(l.id) != null) pending = l
                        else go(Screen.Play(l.title, Content.sessionFor(l), l.text, l.id, false))
                    }
                }
            }
            item(key = "footer") {
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("English • تطوير maggd alosimi", color = b.muted, fontSize = 12.sp)
                }
            }
        }
    }

    pending?.let { l ->
        val sn = store.savedSession(l.id)
        if (sn == null) {
            pending = null
        } else {
            AppDialog(onDismiss = { pending = null }) {
                Text("▶️", fontSize = 44.sp)
                T(l.titleAr, 20.sp, true, center = true)
                T(l.title, 14.sp, color = b.muted, center = true)
                Pill(
                    "وصلت إلى السؤال ${minOf(sn.idx + 1, sn.ids.size)} من ${sn.ids.size}",
                    b.orange.copy(alpha = 0.16f), b.orange, size = 14.sp
                )
                Spacer(Modifier.height(2.dp))
                ChunkyButton("متابعة من حيث توقفت", {
                    pending = null
                    go(Screen.Play(l.title, Content.sessionFor(l), l.text, l.id, false, sn))
                }, Modifier.fillMaxWidth())
                SoftButton("ابدأ من جديد", {
                    store.clearSession(l.id)
                    pending = null
                    go(Screen.Play(l.title, Content.sessionFor(l), l.text, l.id, false))
                }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun UnitHeader(u: UnitData, index: Int, done: Int, total: Int) {
    val b = brand
    val grads = listOf(
        listOf(b.gradA, b.gradB),
        listOf(Color(0xFFFF7A29), Color(0xFFFFB020)),
        listOf(Color(0xFF16B364), Color(0xFF18B4D8)),
        listOf(Color(0xFFE64980), Color(0xFF9775FA))
    )
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(grads[index % grads.size]))
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-34).dp).size(120.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-24).dp, y = 30.dp).size(80.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
        )
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T(u.titleAr, 18.sp, true, Color.White)
            T(u.title, 13.sp, color = Color.White.copy(alpha = 0.88f))
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppProgress(
                    done.toFloat() / total.coerceAtLeast(1), Modifier.weight(1f), 10.dp,
                    track = Color.White.copy(alpha = 0.30f), brush = SolidColor(Color.White)
                )
                Text("$done/$total", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LessonNode(
    l: Lesson, index: Int, count: Int, store: Store, isCurrent: Boolean, onTap: () -> Unit
) {
    val b = brand
    @Suppress("UNUSED_VARIABLE") val rev = store.rev
    val best = store.bestScore(l.id)
    val snap = store.savedSession(l.id)
    val inProg = snap != null
    val done = best >= 0
    val perfect = best >= 100
    val frac = if (snap != null) snap.idx.toFloat() / snap.ids.size.coerceAtLeast(1) else 0f
    val face = when {
        inProg -> b.orange
        perfect -> b.gold
        done -> b.success
        else -> b.primary
    }
    val edge = when {
        inProg -> b.orange.darken()
        perfect -> b.goldDark
        done -> b.successDark
        else -> b.primaryDark
    }
    val num = l.id.dropWhile { it != 'l' }.drop(1)
    val label = when {
        inProg -> "▶"
        perfect -> "⭐"
        done -> "✓"
        else -> num
    }
    val stars = when {
        best >= 100 -> 3
        best >= 80 -> 2
        best >= 0 -> 1
        else -> 0
    }
    val rounds = store.roundsOf(l.id)
    val lineColor = if (done) b.success.copy(alpha = 0.55f) else b.cardBorder
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed) 6.dp else 0.dp, label = "nodeDrop")
    val x = sway(index).dp

    Box(Modifier.fillMaxWidth().height(192.dp)) {
        // dashed connector between nodes
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val nodeY = 70.dp.toPx()
            val px = cx + sway(index).dp.toPx()
            val path = Path()
            if (index > 0) {
                val mx = (px + cx + sway(index - 1).dp.toPx()) / 2f
                path.moveTo(mx, 0f)
                path.cubicTo(mx, nodeY * 0.5f, px, nodeY * 0.5f, px, nodeY)
            } else {
                path.moveTo(px, nodeY)
            }
            if (index < count - 1) {
                val mx2 = (px + cx + sway(index + 1).dp.toPx()) / 2f
                val my = (nodeY + size.height) / 2f
                path.cubicTo(px, my, mx2, my, mx2, size.height)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 18f))
                )
            )
        }

        // the node
        Box(
            Modifier.align(Alignment.TopCenter).offset(x = x, y = 22.dp).size(96.dp)
                .clickable(interactionSource = src, indication = null, onClick = onTap),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                val inf = rememberInfiniteTransition(label = "pulse")
                val s by inf.animateFloat(
                    initialValue = 1f, targetValue = 1.16f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                    label = "pulseScale"
                )
                Box(Modifier.size(88.dp).scale(s).background(face.copy(alpha = 0.18f), CircleShape))
            }
            Canvas(Modifier.fillMaxSize()) {
                val st = 6.dp.toPx()
                val tl = Offset(st / 2f, st / 2f)
                val sz = Size(size.width - st, size.height - st)
                drawArc(
                    color = if (done && !inProg) face.copy(alpha = 0.5f) else b.cardBorder,
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = tl, size = sz,
                    style = Stroke(st, cap = StrokeCap.Round)
                )
                if (inProg && frac > 0f) {
                    drawArc(
                        color = b.orange,
                        startAngle = -90f, sweepAngle = 360f * frac, useCenter = false,
                        topLeft = tl, size = sz,
                        style = Stroke(st, cap = StrokeCap.Round)
                    )
                }
            }
            Box(Modifier.size(72.dp).offset(y = (-3).dp)) {
                Box(Modifier.fillMaxSize().offset(y = 6.dp).background(edge, CircleShape))
                Box(
                    Modifier.fillMaxSize().offset(y = drop)
                        .background(Brush.verticalGradient(listOf(face.lighten(0.22f), face)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }

        // "start / continue" tag above the current node
        if (isCurrent) {
            Box(
                Modifier.align(Alignment.TopCenter).offset(x = x, y = 0.dp)
                    .clip(RoundedCornerShape(12.dp)).background(face)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(if (inProg) "أكمل" else "ابدأ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // labels + progress info
        Column(
            Modifier.align(Alignment.TopCenter).offset(x = x, y = 124.dp).width(144.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            T(l.titleAr, 14.sp, true, center = true)
            T(l.title, 11.sp, color = b.muted, center = true)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (snap != null) {
                    Pill(
                        "${minOf(snap.idx, snap.ids.size)}/${snap.ids.size}",
                        b.orange.copy(alpha = 0.18f), b.orange, size = 11.sp
                    )
                } else if (stars > 0) {
                    Text("★".repeat(stars) + "☆".repeat(3 - stars), color = b.gold, fontSize = 15.sp)
                }
                if (rounds > 0) Pill("🔁 $rounds", b.infoBg, b.primary, size = 11.sp)
            }
        }
    }
}

// ---------- review / exam / cram ----------
@Composable
fun FeatureCard(
    emoji: String, tint: Color, title: String, subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 22.sp) }
            Column(Modifier.weight(1f)) {
                T(title, 17.sp, true)
                if (subtitle != null) T(subtitle, 13.sp, color = brand.muted)
            }
        }
        content()
    }
}

@Composable
fun ReviewTab(store: Store, go: (Screen) -> Unit) {
    val b = brand
    @Suppress("UNUSED_VARIABLE") val refresh = store.rev
    val all = remember { Content.allExercises() }
    val due = all.filter { store.isDue(it.id) }
    val weak = all.filter { store.wrongs(it.id) > 0 }.sortedWith(
        compareByDescending<Ex> { store.wrongs(it.id).toDouble() / store.attempts(it.id) }
            .thenByDescending { store.wrongs(it.id) }
    )
    var sel by remember { mutableStateOf(setOf("u1", "u2", "x")) }
    var cnt by remember { mutableIntStateOf(20) }
    val lessons = Content.units.flatMap { it.lessons }
    val doneLessons = lessons.count { store.bestScore(it.id) >= 0 }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FeatureCard("📈", b.gradB, "تقدّمك في المسار", "كل جولة تنهيها تُحفظ تلقائياً") {
            AppProgress(doneLessons.toFloat() / lessons.size.coerceAtLeast(1), Modifier.fillMaxWidth(), 12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("✅ $doneLessons / ${lessons.size} درس", b.successBg, b.successText)
                Pill("🔁 ${store.totalRounds()} جولة", b.infoBg, b.primary)
            }
        }
        FeatureCard("📅", b.primary, "مراجعة اليوم", "عناصر مستحقة للمراجعة الآن: ${due.size}") {
            ChunkyButton(
                "ابدأ المراجعة",
                { go(Screen.Play("Review", due.shuffled().take(20), "", null, false)) },
                Modifier.fillMaxWidth(), enabled = due.isNotEmpty()
            )
        }
        FeatureCard("❌", b.danger, "الأخطاء والنقاط الضعيفة", "${weak.size} عنصر يحتاج تركيز") {
            weak.take(5).forEach {
                T("• " + it.q.take(70) + "  (أخطاء: ${store.wrongs(it.id)})", 13.sp, color = b.muted)
            }
            ChunkyButton(
                "تدرّب على أخطائي",
                { go(Screen.Play("Mistakes", weak.take(20).shuffled(), "", null, false)) },
                Modifier.fillMaxWidth(), enabled = weak.isNotEmpty(),
                face = b.danger, edge = b.dangerDark
            )
        }
        FeatureCard("⚡", b.orange, "خطة الحفظ المكثفة", "40 سؤالاً (حوالي 30 دقيقة): الأضعف، ثم المستحق، ثم الجديد") {
            ChunkyButton(
                "ابدأ الخطة",
                {
                    val unseen = all.filter { store.attempts(it.id) == 0 }.shuffled()
                    val plan = (weak.take(25) + due.shuffled() + unseen).distinctBy { it.id }.take(40)
                    go(Screen.Play("Cram", plan, "", null, false))
                },
                Modifier.fillMaxWidth(), face = b.orange, edge = b.orange.darken()
            )
        }
        FeatureCard("📝", b.gradA, "وضع الامتحان", "بدون تلميحات وبدون قلوب") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("u1" to "وحدة 1", "u2" to "وحدة 2", "x" to "الملازم").forEach { p ->
                    FilterChip(
                        selected = p.first in sel,
                        onClick = { sel = if (p.first in sel) sel - p.first else sel + p.first },
                        label = { Text(p.second) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = b.primary.copy(alpha = 0.18f),
                            selectedLabelColor = b.primary
                        )
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 30, 40).forEach { n ->
                    FilterChip(
                        selected = cnt == n, onClick = { cnt = n }, label = { Text("$n") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = b.primary.copy(alpha = 0.18f),
                            selectedLabelColor = b.primary
                        )
                    )
                }
            }
            ChunkyButton(
                "ابدأ الامتحان",
                { go(Screen.Play("Exam", Content.allExercises(sel).shuffled().take(cnt), "", null, true)) },
                Modifier.fillMaxWidth(), enabled = sel.isNotEmpty()
            )
        }
        FeatureCard("📊", b.success, "الدقة حسب الوحدة") {
            Content.units.forEach { u ->
                var att = 0
                var wr = 0
                Content.unitExercises(u).forEach { att += store.attempts(it.id); wr += store.wrongs(it.id) }
                val pct = if (att == 0) 0 else (att - wr) * 100 / att
                T(u.titleAr, 14.sp, true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppProgress(pct / 100f, Modifier.weight(1f), 10.dp)
                    Text(if (att == 0) "—" else "$pct%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------- words / search ----------
private fun catLabel(c: String) = when (c) {
    "vocab" -> "كلمة"
    "term" -> "مصطلح"
    "acronym" -> "اختصار"
    "hw" -> "أجهزة"
    "adj" -> "صفة"
    "def" -> "تعريف"
    else -> c
}

@Composable
fun WordsTab(go: (Screen) -> Unit) {
    val b = brand
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<Grammar?>(null) }
    val res = Content.vocab.filter {
        q.isBlank() || it.en.contains(q, true) || it.ar.contains(q) || it.def.contains(q, true)
    }
    val gr = if (q.isBlank()) emptyList() else Content.grammar.filter { g ->
        g.title.contains(q, true) || g.titleAr.contains(q) || g.rules.any { it.contains(q, true) }
    }
    val tints = listOf(b.primary, b.orange, b.success, b.gradB, b.danger)
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            leadingIcon = { Text("🔍", fontSize = 18.sp) },
            label = { Text("ابحث عن كلمة أو اختصار أو قاعدة") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        ChunkyButton(
            "🃏 بطاقات تعليمية (${res.size})",
            { go(Screen.Flash(res.shuffled())) },
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            enabled = res.isNotEmpty(), height = 46.dp
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(gr) { g ->
                Panel(Modifier.fillMaxWidth().clickable { open = g }) {
                    T("📘 " + g.title, 16.sp, true)
                    T(g.titleAr, 14.sp, color = b.muted)
                }
            }
            items(res) { v ->
                val tint = tints[abs(v.cat.hashCode()) % tints.size]
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row { Pill(catLabel(v.cat), tint.copy(alpha = 0.16f), tint, size = 11.sp) }
                            T(v.en, 18.sp, true)
                            T(v.ar, 15.sp, color = b.primary)
                            if (v.def.isNotBlank()) T(v.def, 13.sp, color = b.muted)
                        }
                        SpeakBtn(speakable(v.en))
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
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(Content.grammar) { g ->
            Panel(Modifier.fillMaxWidth()) { GrammarBody(g) }
        }
    }
}

@Composable
fun GrammarBody(g: Grammar) {
    val b = brand
    T(g.title, 20.sp, true, b.primary)
    T(g.titleAr, 16.sp, true)
    T(g.notesAr, 15.sp, color = b.muted)
    T("القواعد / Rules", 15.sp, true)
    (g.rules + g.examples).forEach { r ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(b.primary.copy(alpha = 0.07f)).padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            T("• $r", 15.sp, modifier = Modifier.weight(1f).padding(8.dp))
            Box(
                Modifier.size(40.dp).clickable { Tts.speak(r) },
                contentAlignment = Alignment.Center
            ) { Text("🔊", fontSize = 18.sp) }
        }
    }
}

@Composable
fun GrammarDialog(g: Grammar, onDismiss: () -> Unit) {
    AppDialog(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { GrammarBody(g) }
        ChunkyButton("إغلاق", onDismiss, Modifier.fillMaxWidth())
    }
}

// ---------- flashcards ----------
@Composable
fun FlipCard(v: Vocab, modifier: Modifier) {
    val b = brand
    var flip by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (flip) 180f else 0f, tween(380), label = "flip")
    val front = rot <= 90f
    val grad = if (front) listOf(b.gradA, b.gradB) else listOf(b.orange, b.gold)
    Box(
        modifier
            .graphicsLayer {
                rotationY = rot
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(grad))
            .clickable { flip = !flip },
        contentAlignment = Alignment.Center
    ) {
        if (front) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                T(v.en, 30.sp, true, Color.White, center = true)
                SpeakBtn(speakable(v.en), onGradient = true)
                T("اضغط لقلب البطاقة", 13.sp, color = Color.White.copy(alpha = 0.8f), center = true)
            }
        } else {
            Column(
                Modifier.graphicsLayer { rotationY = 180f }.verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                T(v.ar, 26.sp, true, Color.White, center = true)
                if (v.def.isNotBlank()) T(v.def, 16.sp, color = Color.White.copy(alpha = 0.92f), center = true)
            }
        }
    }
}

@Composable
fun FlashScreen(items: List<Vocab>, onExit: () -> Unit) {
    val b = brand
    val queue = remember { mutableStateListOf<Vocab>().apply { addAll(items) } }
    var known by remember { mutableIntStateOf(0) }
    var step by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CloseBtn(onExit)
            AppProgress(known.toFloat() / items.size.coerceAtLeast(1), Modifier.weight(1f), 14.dp)
            Pill("🃏 ${queue.size}", b.infoBg, b.primary)
        }
        if (queue.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 72.sp)
                T("خلصت كل البطاقات!", 22.sp, true, center = true)
                T("عرفت $known بطاقة", 15.sp, color = b.muted, center = true)
                Spacer(Modifier.height(16.dp))
                ChunkyButton("رجوع", onExit, Modifier.fillMaxWidth())
            }
        } else {
            val v = queue[0]
            key(step) {
                FlipCard(v, Modifier.fillMaxWidth().weight(1f).padding(vertical = 18.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SoftButton("لم أعرفها ❌", {
                    val x = queue.removeAt(0)
                    queue.add(x)
                    step++
                }, Modifier.weight(1f))
                ChunkyButton("عرفتها ✅", {
                    queue.removeAt(0)
                    known++
                    step++
                }, Modifier.weight(1f), face = b.success, edge = b.successDark)
            }
        }
    }
}

// ---------- session ----------
@Composable
fun PlayScreen(s: Screen.Play, store: Store, onExit: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    key(attempt) { Session(s, store, if (attempt == 0) s.resume else null, onExit) { attempt++ } }
}

@Composable
fun Session(s: Screen.Play, store: Store, resumeIn: Snap?, onExit: () -> Unit, onRetry: () -> Unit) {
    val b = brand
    val resume = remember {
        resumeIn?.takeIf { sn -> sn.ids.isNotEmpty() && sn.ids.all { Content.exById(it) != null } }
    }
    val queue = remember {
        mutableStateListOf<Ex>().apply {
            if (resume != null) addAll(resume.ids.mapNotNull { Content.exById(it) })
            else addAll(s.exercises)
        }
    }
    var idx by remember { mutableIntStateOf(resume?.idx?.coerceIn(0, queue.size) ?: 0) }
    var hearts by remember { mutableIntStateOf(resume?.hearts ?: 5) }
    var correct by remember { mutableIntStateOf(resume?.correct ?: 0) }
    var total by remember { mutableIntStateOf(resume?.total ?: 0) }
    var feedback by remember { mutableStateOf<Boolean?>(null) }
    val retried = remember { HashSet<String>().apply { if (resume != null) addAll(resume.retried) } }
    val wrongs = remember {
        mutableStateListOf<Ex>().apply {
            if (resume != null) addAll(resume.wrongs.mapNotNull { Content.exById(it) })
        }
    }
    var showText by remember { mutableStateOf(false) }
    var why by remember { mutableStateOf<String?>(null) }
    var confirmExit by remember { mutableStateOf(false) }

    val finished = idx >= queue.size
    val dead = !s.exam && hearts <= 0 && feedback == null

    LaunchedEffect(finished) {
        if (finished && (s.exam || hearts > 0) && total > 0) {
            store.addXp(20)
            store.touchStreak()
            s.lessonId?.let {
                store.setBest(it, correct * 100 / total)
                store.completeRound(it)
                store.clearSession(it)
            }
        }
    }
    LaunchedEffect(dead) {
        if (dead) s.lessonId?.let { store.clearSession(it) }
    }

    // saves the round after every answer so it can be resumed from the path
    fun persist(next: Int) {
        val lid = s.lessonId ?: return
        if (s.exam) return
        if (hearts <= 0) {
            store.clearSession(lid)
        } else {
            store.saveSession(
                lid,
                Snap(queue.map { it.id }, next, hearts, correct, total, retried.toList(), wrongs.map { it.id })
            )
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
        if (s.exam) {
            idx++
        } else {
            feedback = ok
            persist(idx + 1)
        }
    }

    val canLeaveFreely = finished || dead || (total == 0 && feedback == null)
    fun requestExit() {
        if (canLeaveFreely) onExit() else confirmExit = true
    }
    BackHandler(enabled = !finished && !dead) { requestExit() }

    when {
        dead -> EndView(
            emoji = "💔", title = "خلصت القلوب!", subtitle = "جرّب الجولة مرة ثانية — القلوب ترجع 5.",
            stars = -1, wrongs = emptyList(), onRetry = onRetry, onExit = onExit
        )
        finished -> {
            val pct = if (total == 0) 0 else correct * 100 / total
            EndView(
                emoji = if (pct >= 80) "🎉" else "💪",
                title = "النتيجة: $correct / $total  ($pct%)",
                subtitle = "+${correct * 10 + 20} XP",
                stars = when {
                    pct >= 100 -> 3
                    pct >= 80 -> 2
                    pct >= 50 -> 1
                    else -> 0
                },
                wrongs = wrongs.distinctBy { it.id }, onRetry = onRetry, onExit = onExit
            )
        }
        else -> {
            val ex = queue[idx]
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CloseBtn { requestExit() }
                    AppProgress(idx.toFloat() / queue.size, Modifier.weight(1f), 14.dp)
                    if (!s.exam) Pill("❤️ $hearts", b.dangerBg, b.dangerText, size = 15.sp)
                    if (s.text.isNotBlank()) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(b.infoBg)
                                .clickable { showText = true },
                            contentAlignment = Alignment.Center
                        ) { Text("📖", fontSize = 18.sp) }
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
        AppDialog(onDismiss = { showText = false }) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState())
            ) { T(s.text, 17.sp) }
            ChunkyButton("إغلاق", { showText = false }, Modifier.fillMaxWidth())
        }
    }
    why?.let { id ->
        Content.grammar.firstOrNull { it.id == id }?.let { GrammarDialog(it) { why = null } }
    }
    if (confirmExit) {
        val saved = s.lessonId != null && !s.exam
        AppDialog(onDismiss = { confirmExit = false }) {
            Text(if (saved) "💾" else "🚪", fontSize = 44.sp)
            T(if (saved) "تقدّمك محفوظ" else "هل تريد الخروج؟", 20.sp, true, center = true)
            T(
                if (saved) "يمكنك متابعة هذه الجولة لاحقاً من المسار ومن نفس السؤال."
                else "إذا خرجت الآن سيضيع تقدمك في هذه الجولة.",
                14.sp, color = b.muted, center = true
            )
            ChunkyButton("متابعة الحل", { confirmExit = false }, Modifier.fillMaxWidth())
            SoftButton("خروج", { confirmExit = false; onExit() }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun FeedbackBar(ok: Boolean, ex: Ex, onWhy: () -> Unit, onContinue: () -> Unit) {
    val b = brand
    Surface(
        color = if (ok) b.successBg else b.dangerBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (ok) "🎉" else "😕", fontSize = 28.sp)
                T(
                    if (ok) "ممتاز! إجابة صحيحة" else "إجابة غير صحيحة", 20.sp, true,
                    if (ok) b.successText else b.dangerText
                )
            }
            if (!ok && ex.t != "write") {
                T("الإجابة الصحيحة:", 14.sp, color = b.dangerText)
                T(answerText(ex), 17.sp, true, b.dangerText)
            }
            if (ex.exp.isNotBlank()) T(ex.exp, 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ex.g.isNotBlank()) SoftButton("لماذا؟", onWhy, Modifier.width(96.dp))
                val spoken = spokenText(ex)
                if (spoken.isNotBlank()) SpeakBtn(spoken)
                Spacer(Modifier.weight(1f))
                ChunkyButton(
                    "متابعة", onContinue, Modifier.width(140.dp),
                    face = if (ok) b.success else b.danger,
                    edge = if (ok) b.successDark else b.dangerDark
                )
            }
        }
    }
}

@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val b = brand
    val inf = rememberInfiniteTransition(label = "confetti")
    val t by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "confettiT"
    )
    val colors = listOf(b.gold, b.success, b.primary, b.danger, b.gradB, b.orange)
    val parts = remember { List(40) { Pair(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier) {
        parts.forEachIndexed { i, p ->
            val prog = (t + p.second) % 1f
            val x = p.first * size.width + sin(prog * 6.283f * 2f + i) * 14.dp.toPx()
            val y = prog * size.height
            drawRect(
                color = colors[i % colors.size].copy(alpha = 1f - prog * 0.5f),
                topLeft = Offset(x, y),
                size = Size(7.dp.toPx(), 11.dp.toPx())
            )
        }
    }
}

@Composable
fun EndView(
    emoji: String, title: String, subtitle: String, stars: Int,
    wrongs: List<Ex>, onRetry: () -> Unit, onExit: () -> Unit
) {
    val b = brand
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val sc by animateFloatAsState(
        if (shown) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "emojiScale"
    )
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.size(120.dp).scale(sc)
                    .background(Brush.linearGradient(listOf(b.gradA, b.gradB)), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 60.sp) }
            T(title, 23.sp, true, center = true)
            if (stars >= 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        Text(
                            if (i < stars) "⭐" else "☆", fontSize = 36.sp,
                            color = b.muted, modifier = Modifier.alpha(if (i < stars) 1f else 0.6f)
                        )
                    }
                }
            }
            Pill(subtitle, b.gold.copy(alpha = 0.2f), b.orange, size = 15.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton("تم", onExit, Modifier.weight(1f), face = b.success, edge = b.successDark)
                SoftButton("إعادة", onRetry, Modifier.weight(1f))
            }
            if (wrongs.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                T("الأسئلة الخاطئة والإجابات الصحيحة:", 17.sp, true)
                wrongs.forEach { w ->
                    Panel(Modifier.fillMaxWidth()) {
                        T(w.q, 15.sp)
                        if (w.t != "write") T("✔ " + answerText(w), 15.sp, true, b.successText)
                        if (w.exp.isNotBlank()) T(w.exp, 13.sp, color = b.muted)
                    }
                }
            }
        }
        if (stars >= 2) Confetti(Modifier.fillMaxSize())
    }
}
