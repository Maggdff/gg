package com.example.englishit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CS { Idle, Selected, Correct, Wrong }

@Composable
fun ChoiceBox(
    text: String,
    state: CS,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    val b = brand
    val shape = RoundedCornerShape(16.dp)
    val bg by animateColorAsState(
        when (state) {
            CS.Correct -> b.successBg
            CS.Wrong -> b.dangerBg
            CS.Selected -> b.primary.copy(alpha = 0.12f)
            CS.Idle -> b.card
        }, label = "choiceBg"
    )
    val bd by animateColorAsState(
        when (state) {
            CS.Correct -> b.success
            CS.Wrong -> b.danger
            CS.Selected -> b.primary
            CS.Idle -> b.cardBorder
        }, label = "choiceBorder"
    )
    val fg = when (state) {
        CS.Correct -> b.successText
        CS.Wrong -> b.dangerText
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier.clip(shape).background(bg).border(2.dp, bd, shape)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 14.dp)
    ) { T(text, 16.sp, state != CS.Idle, fg) }
}

@Composable
fun WordChip(text: String, enabled: Boolean, filled: Boolean, onClick: () -> Unit) {
    val b = brand
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.alpha(if (enabled) 1f else 0.4f).clip(shape)
            .background(if (filled) b.primary else b.card)
            .border(2.dp, if (filled) b.primaryDark else b.cardBorder, shape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ExerciseView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    val b = brand
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val instruction = when (ex.t) {
            "mcq" -> "اختر الإجابة الصحيحة"
            "fill" -> "أكمل الفراغ"
            "tf" -> "صح أم خطأ؟"
            "order" -> "رتّب الكلمات لتكوين الجملة"
            "match" -> "اربط كل عنصر بما يناسبه"
            else -> "اكتب إجابتك ثم قارنها بالنموذج"
        }
        Row {
            Pill(instruction, b.primary.copy(alpha = 0.14f), b.primary)
        }
        when (ex.t) {
            "mcq" -> McqView(ex, locked, onSubmit)
            "fill" -> FillView(ex, locked, onSubmit)
            "tf" -> TfView(ex, locked, onSubmit)
            "order" -> OrderView(ex, locked, onSubmit)
            "match" -> MatchView(ex, locked, onSubmit)
            else -> WriteView(ex, locked, onSubmit)
        }
    }
}

@Composable
fun McqView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    val opts = remember(ex.id) { ex.opts.shuffled() }
    var sel by remember { mutableStateOf<String?>(null) }
    val right = ex.a.firstOrNull()
    T(ex.q, 21.sp, true)
    opts.forEach { o ->
        val state = when {
            locked && o == right -> CS.Correct
            locked && o == sel -> CS.Wrong
            o == sel -> CS.Selected
            else -> CS.Idle
        }
        ChoiceBox(o, state) { if (!locked) sel = o }
    }
    ChunkyButton("تحقق", { onSubmit(sel == right) }, Modifier.fillMaxWidth(), enabled = sel != null && !locked)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }
    val chips = remember(ex.id) { ex.opts.shuffled() }
    fun check() {
        if (text.isNotBlank() && !locked) onSubmit(AnswerChecker.check(text, ex.a))
    }
    T(ex.q, 21.sp, true)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        enabled = !locked,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, textDirection = TextDirection.Ltr),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { check() }),
        modifier = Modifier.fillMaxWidth()
    )
    if (chips.isNotEmpty()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEach { c -> WordChip(c, !locked, text == c) { text = c } }
            }
        }
    }
    ChunkyButton("تحقق", { check() }, Modifier.fillMaxWidth(), enabled = text.isNotBlank() && !locked)
}

@Composable
fun TfView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    var sel by remember { mutableStateOf<Boolean?>(null) }
    T(ex.q, 21.sp, true)
    listOf(true to "✓   True  (صح)", false to "✗   False  (خطأ)").forEach { p ->
        val state = when {
            locked && p.first == ex.tf -> CS.Correct
            locked && sel == p.first -> CS.Wrong
            sel == p.first -> CS.Selected
            else -> CS.Idle
        }
        ChoiceBox(p.second, state) {
            if (!locked) {
                sel = p.first
                onSubmit(p.first == ex.tf)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    val b = brand
    val words = remember(ex.id) {
        (ex.a[0].split(" ").filter { it.isNotBlank() } + ex.extra).shuffled()
    }
    val chosen = remember { mutableStateListOf<Int>() }
    val tray = RoundedCornerShape(18.dp)
    T(ex.q, 21.sp, true)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 80.dp).clip(tray)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, b.cardBorder, tray)
                .padding(10.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chosen.forEach { i -> WordChip(words[i], !locked, true) { chosen.remove(i) } }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.forEachIndexed { i, w ->
                WordChip(w, !locked && i !in chosen, false) { chosen.add(i) }
            }
        }
    }
    ChunkyButton("تحقق", {
        val sentence = chosen.joinToString(" ") { words[it] }
        onSubmit(AnswerChecker.check(sentence, ex.a))
    }, Modifier.fillMaxWidth(), enabled = chosen.isNotEmpty() && !locked)
}

@Composable
fun MatchView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    val left = remember(ex.id) { ex.pairs.map { it.first } }
    val right = remember(ex.id) { ex.pairs.map { it.second }.shuffled() }
    var selL by remember { mutableStateOf<String?>(null) }
    var bad by remember { mutableStateOf<String?>(null) }
    val done = remember { mutableStateListOf<String>() }
    var mistakes by remember { mutableIntStateOf(0) }
    val doneRights = done.map { l -> ex.pairs.first { it.first == l }.second }
    T(ex.q, 18.sp, true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            left.forEach { l ->
                val isDone = l in done
                val state = when {
                    isDone -> CS.Correct
                    selL == l -> CS.Selected
                    else -> CS.Idle
                }
                ChoiceBox(l, state) { if (!isDone && !locked) { selL = l; bad = null } }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            right.forEach { r ->
                val isDone = r in doneRights
                val state = when {
                    isDone -> CS.Correct
                    bad == r -> CS.Wrong
                    else -> CS.Idle
                }
                ChoiceBox(r, state) {
                    val sl = selL
                    if (!isDone && !locked && sl != null) {
                        if (ex.pairs.first { it.first == sl }.second == r) {
                            done.add(sl)
                            selL = null
                            bad = null
                            if (done.size == left.size) onSubmit(mistakes == 0)
                        } else {
                            mistakes++
                            bad = r
                            selL = null
                        }
                    }
                }
            }
        }
    }
    if (mistakes > 0) T("محاولات خاطئة: $mistakes", 13.sp, color = brand.dangerText)
}

@Composable
fun WriteView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(false) }
    T(ex.q, 18.sp, true)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        minLines = 3,
        enabled = !locked,
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth()
    )
    if (!shown) {
        ChunkyButton("أظهر النموذج", { shown = true }, Modifier.fillMaxWidth())
    } else {
        Panel(Modifier.fillMaxWidth()) {
            T("نموذج الإجابة:", 14.sp, true, brand.primary)
            T(ex.model, 16.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChunkyButton(
                "✅ كتبتها صح", { onSubmit(true) }, Modifier.weight(1f), enabled = !locked,
                face = brand.success, edge = brand.successDark
            )
            SoftButton("❌ أحتاج مراجعة", { onSubmit(false) }, Modifier.weight(1f), enabled = !locked)
        }
    }
}
