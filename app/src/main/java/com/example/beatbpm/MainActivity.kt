package com.example.beatbpm


import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BeatBpmScreen() }
    }
}


@Composable
fun BeatBpmScreen() {
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 85) }
    var bpm by remember { mutableIntStateOf(120) }
    var bpmText by remember { mutableStateOf("120") }
    var tolerance by remember { mutableIntStateOf(50) }
    var mode by remember { mutableStateOf("有声测试") }
    var isRunning by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var beatLit by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var taps by remember { mutableStateOf(listOf<Long>()) }
    var deviations by remember { mutableStateOf(listOf<Int>()) }
    var expectedStartNs by remember { mutableStateOf(0L) }
    var intervalNs by remember { mutableStateOf(0L) }
    var lastResult by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { tone.release() } }

    fun stopAll() { isRunning = false; isTesting = false; countdown = 0; beatLit = false }
    fun startMetronome() {
        stopAll(); isRunning = true; showResult = false
    }
    fun startTest() {
        stopAll(); taps = emptyList(); deviations = emptyList(); showResult = false
        isTesting = true; countdown = 4
        intervalNs = (60_000_000_000L / bpm)
    }

    LaunchedEffect(isRunning, isTesting, bpm, mode) {
        if (isRunning) {
            val stepMs = 60_000L / bpm
            while (isActive && isRunning) {
                beatLit = true
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                delay(90)
                beatLit = false
                delay((stepMs - 90).coerceAtLeast(5))
            }
        }
        if (isTesting) {
            val stepNs = 60_000_000_000L / bpm
            val anchor = System.nanoTime()
            expectedStartNs = anchor + stepNs * 4
            // Four audible count-in beats; after them, the test section starts.
            for (i in 0 until 4) {
                if (!isTesting) break
                countdown = 4 - i
                beatLit = true
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 75)
                delay(80)
                beatLit = false
                val waitUntil = anchor + stepNs * (i + 1)
                val remaining = (waitUntil - System.nanoTime()) / 1_000_000L
                if (remaining > 0) delay(remaining)
            }
            countdown = 0
            if (isTesting) {
                val testAnchor = anchor + stepNs * 4
                for (beat in 0 until 16) {
                    if (!isTesting) break
                    val beatTime = testAnchor + stepNs * beat
                    val now = System.nanoTime()
                    val waitMs = (beatTime - now) / 1_000_000L
                    if (waitMs > 0) delay(waitMs)
                    beatLit = true
                    if (mode == "有声测试") tone.startTone(ToneGenerator.TONE_PROP_BEEP, 65)
                    delay(65)
                    beatLit = false
                }
            }
            if (isTesting) {
                isTesting = false
                isRunning = false
                showResult = true
            }
        }
    }


    val bg = Color(0xFF101820)
    val panel = Color(0xFF1D2A35)
    val mint = Color(0xFF63E6B0)
    val tapModifier = Modifier.fillMaxWidth().weight(1f).pointerInput(isTesting, expectedStartNs, intervalNs, taps, tolerance) {
        detectTapGestures {
            if (isTesting && countdown == 0 && taps.size < 16 && expectedStartNs > 0L) {
                val now = System.nanoTime()
                val index = taps.size
                val target = expectedStartNs + intervalNs * index
                val error = ((now - target) / 1_000_000L).toInt()
                taps = taps + now
                deviations = deviations + error
                beatLit = true
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = mint, background = bg, surface = panel)) {
        Column(Modifier.fillMaxSize().background(bg).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BEAT BPM", color = mint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("节拍精准度训练", color = Color.LightGray, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip("有声测试", mode == "有声测试", mint) { mode = "有声测试"; stopAll() }
                ModeChip("无声测试", mode == "无声测试", mint) { mode = "无声测试"; stopAll() }
            }
            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("节拍速度", color = Color.LightGray)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { bpm = (bpm - 1).coerceAtLeast(40); bpmText = bpm.toString() }, enabled = !isTesting) { Text("−1", color = mint, fontSize = 20.sp) }
                        Text("$bpm", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { bpm = (bpm + 1).coerceAtMost(240); bpmText = bpm.toString() }, enabled = !isTesting) { Text("+1", color = mint, fontSize = 20.sp) }
                    }
                    Text("BPM", color = mint, fontSize = 14.sp)
                    Slider(value = bpm.toFloat(), onValueChange = { bpm = it.roundToInt(); bpmText = bpm.toString() }, valueRange = 40f..240f, enabled = !isTesting)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf(-5, 5).forEach { change ->
                            OutlinedButton(onClick = { bpm = (bpm + change).coerceIn(40,240); bpmText = bpm.toString() }, enabled = !isTesting, modifier = Modifier.padding(horizontal = 5.dp)) { Text(if (change < 0) "−5" else "+5") }
                        }
                        OutlinedTextField(value = bpmText, onValueChange = { bpmText = it; it.toIntOrNull()?.let { n -> if (n in 40..240) bpm = n } }, enabled = !isTesting, label = { Text("输入 BPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(105.dp), singleLine = true)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("允许误差", color = Color.LightGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(10,20,30,50,100,200).forEach { ms ->
                            FilterChip(selected = tolerance == ms, onClick = { tolerance = ms }, label = { Text("±$ms") }, enabled = !isTesting)
                        }
                    }
                    Text("点击误差在 ±${tolerance} ms 内算合格", color = mint, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().weight(1f).background(if (beatLit) Color(0xFF214D3D) else panel, RoundedCornerShape(24.dp)).then(tapModifier), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isTesting) (if (countdown > 0) "预备 $countdown" else "点击这里打拍") else "♩", color = mint, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(if (isTesting) "${taps.size}/16 拍" else if (isRunning) "节拍播放中" else "点击开始", color = Color.White, fontSize = 18.sp)
                    if (isTesting && mode == "无声测试" && countdown == 0) Text("静音阶段 · 凭感觉保持节奏", color = Color.LightGray, fontSize = 13.sp)
                    if (!isTesting) Text("大区域点击即可打拍", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { if (isRunning) stopAll() else startMetronome() }, enabled = !isTesting, modifier = Modifier.weight(1f)) { Text(if (isRunning) "暂停节拍" else "开始节拍") }
                Button(onClick = { if (isTesting) stopAll() else startTest() }, modifier = Modifier.weight(1f)) { Text(if (isTesting) "停止测试" else "开始测试") }
            }
            if (showResult) {
                Spacer(Modifier.height(10.dp))
                val absAvg = if (deviations.isNotEmpty()) deviations.map { abs(it) }.average().roundToInt() else 0
                val pass = if (deviations.isNotEmpty()) deviations.count { abs(it) <= tolerance } else 0
                Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("测试结果", color = mint, fontWeight = FontWeight.Bold)
                        Text("合格拍数：$pass / ${deviations.size}　平均绝对误差：${absAvg} ms", color = Color.White)
                        Text("误差（ms）：${deviations.joinToString(", ")}", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ModeChip(label: String, selected: Boolean, mint: Color, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = mint, selectedLabelColor = Color(0xFF10231D)))
}
