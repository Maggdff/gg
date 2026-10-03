package com.example.englishit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExerciseView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val instruction = when (ex.t) {
            "mcq" -> "اختر الإجابة الصحيحة"
            "fill" -> "أكمل الفراغ"
            "tf" -> "صح أم خطأ؟"
            "order" -> "رتّب الكلمات لتكوين الجملة"
            "match" -> "اربط كل عنصر بما يناسبه"
            else -> "اكتب إجابتك ثم قارنها بالنموذج"
        }
        T(instruction, 14.sp, color = MaterialTheme.colorScheme.primary)
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
    T(ex.q, 20.sp, true)
    opts.forEach { o ->
        val color = when {
            locked && o == right -> GreenBg
            locked && o == sel -> RedBg
            o == sel -> BlueBg
            else -> MaterialTheme.colorScheme.surface
        }
        ChoiceBox(o, color) { if (!locked) sel = o }
    }
    Button(enabled = sel != null && !locked, onClick = { onSubmit(sel == right) }) { Text("تحقق") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }
    val chips = remember(ex.id) { ex.opts.shuffled() }
    fun check() {
        if (text.isNotBlank() && !locked) onSubmit(AnswerChecker.check(text, ex.a))
    }
    T(ex.q, 20.sp, true)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        enabled = !locked,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, textDirection = TextDirection.Ltr),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { check() }),
        modifier = Modifier.fillMaxWidth()
    )
    if (chips.isNotEmpty()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { c ->
                    OutlinedButton(enabled = !locked, onClick = { text = c }) { Text(c) }
                }
            }
        }
    }
    Button(enabled = text.isNotBlank() && !locked, onClick = { check() }) { Text("تحقق") }
}

@Composable
fun TfView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    var sel by remember { mutableStateOf<Boolean?>(null) }
    T(ex.q, 20.sp, true)
    listOf(true to "✓  True  (صح)", false to "✗  False  (خطأ)").forEach { p ->
        val color = when {
            locked && p.first == ex.tf -> GreenBg
            locked && sel == p.first -> RedBg
            else -> MaterialTheme.colorScheme.surface
        }
        ChoiceBox(p.second, color) {
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
    val words = remember(ex.id) {
        (ex.a[0].split(" ").filter { it.isNotBlank() } + ex.extra).shuffled()
    }
    val chosen = remember { mutableStateListOf<Int>() }
    T(ex.q, 20.sp, true)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
        ) {
            FlowRow(
                Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chosen.forEach { i ->
                    Button(enabled = !locked, onClick = { chosen.remove(i) }) { Text(words[i], fontSize = 17.sp) }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            words.forEachIndexed { i, w ->
                OutlinedButton(enabled = !locked && i !in chosen, onClick = { chosen.add(i) }) {
                    Text(w, fontSize = 17.sp)
                }
            }
        }
    }
    Button(enabled = chosen.isNotEmpty() && !locked, onClick = {
        val sentence = chosen.joinToString(" ") { words[it] }
        onSubmit(AnswerChecker.check(sentence, ex.a))
    }) { Text("تحقق") }
}

@Composable
fun MatchView(ex: Ex, locked: Boolean, onSubmit: (Boolean) -> Unit) {
    val left = remember(ex.id) { ex.pairs.map { it.first } }
    val right = remember(ex.id) { ex.pairs.map { it.second }.shuffled() }
    var selL by remember { mutableStateOf<String?>(null) }
    val done = remember { mutableStateListOf<String>() }
    var mistakes by remember { mutableIntStateOf(0) }
    val doneRights = done.map { l -> ex.pairs.first { it.first == l }.second }
    T(ex.q, 18.sp, true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            left.forEach { l ->
                val isDone = l in done
                val color = when {
                    isDone -> GreenBg
                    selL == l -> BlueBg
                    else -> MaterialTheme.colorScheme.surface
                }
                ChoiceBox(l, color) { if (!isDone && !locked) selL = l }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            right.forEach { r ->
                val isDone = r in doneRights
                val color = if (isDone) GreenBg else MaterialTheme.colorScheme.surface
                ChoiceBox(r, color) {
                    val sl = selL
                    if (!isDone && !locked && sl != null) {
                        if (ex.pairs.first { it.first == sl }.second == r) {
                            done.add(sl)
                            selL = null
                            if (done.size == left.size) onSubmit(mistakes == 0)
                        } else {
                            mistakes++
                            selL = null
                        }
                    }
                }
            }
        }
    }
    if (mistakes > 0) T("محاولات خاطئة: $mistakes", 13.sp)
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
        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth()
    )
    if (!shown) {
        Button(onClick = { shown = true }) { Text("أظهر النموذج") }
    } else {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                T("نموذج الإجابة:", 14.sp, true)
                T(ex.model, 16.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !locked, onClick = { onSubmit(true) }) { Text("✅ كتبتها صح") }
            OutlinedButton(enabled = !locked, onClick = { onSubmit(false) }) { Text("❌ أحتاج مراجعة") }
        }
    }
}
