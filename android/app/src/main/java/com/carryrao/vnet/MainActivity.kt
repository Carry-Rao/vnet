package com.carryrao.vnet

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vnet_settings")

object VnetPrefs {
    val SERVER = stringPreferencesKey("server")
    val IP = stringPreferencesKey("ip")
    val PREFIX = intPreferencesKey("prefix")
    val IPV6 = stringPreferencesKey("ipv6")
    val PREFIX6 = intPreferencesKey("prefix6")
    val KEY = stringPreferencesKey("key")
}

class MainActivity : ComponentActivity() {

    private var pendingIntent: Intent? = null
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingIntent?.let { startForegroundService(it) }
        }
        pendingIntent = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VnetScreen(onStartVpn = { server, ip, prefix, ipv6, prefix6, key ->
                val intent = Intent(this, VnetVpnService::class.java).apply {
                    action = VnetVpnService.ACTION_START
                    putExtra(VnetVpnService.EXTRA_SERVER, server)
                    putExtra(VnetVpnService.EXTRA_IP, ip)
                    putExtra(VnetVpnService.EXTRA_PREFIX, prefix)
                    putExtra(VnetVpnService.EXTRA_IPV6, ipv6)
                    putExtra(VnetVpnService.EXTRA_PREFIX6, prefix6)
                    putExtra(VnetVpnService.EXTRA_KEY, key)
                }
                val prepareIntent = VpnService.prepare(this)
                if (prepareIntent != null) {
                    pendingIntent = intent
                    vpnPermissionLauncher.launch(prepareIntent)
                } else {
                    startForegroundService(intent)
                }
            }, onStopVpn = {
                val intent = Intent(this, VnetVpnService::class.java).apply {
                    action = VnetVpnService.ACTION_STOP
                }
                startForegroundService(intent)
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VnetScreen(onStartVpn: (String, String, Int, String, Int, String) -> Unit, onStopVpn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var server by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("24") }
    var ipv6 by remember { mutableStateOf("") }
    var prefix6 by remember { mutableStateOf("64") }
    var key by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    val stats by VnetStats.snapshot.collectAsState()

    LaunchedEffect(Unit) {
        context.dataStore.data.map { prefs ->
            Sext(
                prefs[VnetPrefs.SERVER] ?: "",
                prefs[VnetPrefs.IP] ?: "",
                prefs[VnetPrefs.PREFIX] ?: 24,
                prefs[VnetPrefs.IPV6] ?: "",
                prefs[VnetPrefs.PREFIX6] ?: 64,
                prefs[VnetPrefs.KEY] ?: ""
            )
        }.collect { s ->
            server = s.first
            ip = s.second
            prefix = s.third.toString()
            ipv6 = s.fourth
            prefix6 = s.fifth.toString()
            key = s.sixth
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text(stringResource(R.string.label_server)) },
                placeholder = { Text(stringResource(R.string.hint_server)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text(stringResource(R.string.label_ip)) },
                placeholder = { Text(stringResource(R.string.hint_ip)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.label_prefix)) },
                placeholder = { Text(stringResource(R.string.hint_prefix)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = ipv6,
                onValueChange = { ipv6 = it },
                label = { Text(stringResource(R.string.label_ipv6)) },
                placeholder = { Text(stringResource(R.string.hint_ipv6)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = prefix6,
                onValueChange = { prefix6 = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.label_prefix6)) },
                placeholder = { Text(stringResource(R.string.hint_prefix6)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(stringResource(R.string.label_key)) },
                placeholder = { Text(stringResource(R.string.hint_key)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
                onClick = {
                    if (running) {
                        onStopVpn()
                        running = false
                    } else {
                        val prefixInt = prefix.toIntOrNull() ?: 24
                        val prefix6Int = prefix6.toIntOrNull() ?: 64
                        scope.launch { savePrefs(context, server, ip, prefixInt, ipv6, prefix6Int, key) }
                        onStartVpn(server, ip, prefixInt, ipv6, prefix6Int, key)
                        running = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = if (running) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ) else ButtonDefaults.buttonColors()
            ) {
                Text(if (running) stringResource(R.string.btn_stop) else stringResource(R.string.btn_start))
            }

            if (running) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularStatsCard(
                        label = "TX",
                        value = stats.txBytes,
                        color = Color(0xFF4FC3F7),
                        modifier = Modifier.weight(1f)
                    )
                    CircularStatsCard(
                        label = "RX",
                        value = stats.rxBytes,
                        color = Color(0xFF81C784),
                        modifier = Modifier.weight(1f)
                    )
                }

                SpeedLineChart(
                    txSpeedHistory = stats.txSpeedHistory,
                    rxSpeedHistory = stats.rxSpeedHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                StatCard(
                    label = stringResource(R.string.stat_latency),
                    value = if (stats.latencyMs > 0) "${stats.latencyMs} ms" else "—",
                    subValue = "",
                    color = when {
                        stats.latencyMs in 1..200 -> Color(0xFF81C784)
                        stats.latencyMs in 201..500 -> Color(0xFFFFD54F)
                        else -> Color(0xFFE57373)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CircularStatsCard(
    label: String,
    value: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E2E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFF8888AA),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier.size(60.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxBytes = 1024 * 1024 * 1024L
                val percentage = (value.toFloat() / maxBytes).coerceAtMost(1f)
                val angle = percentage * 360f

                drawCircle(
                    color = Color(0xFF333333),
                    radius = 25f,
                    style = Stroke(width = 4f)
                )
                
                if (angle > 0) {
                    val arcSize = Size(50f, 50f)
                    drawArc(
                        color = color.copy(alpha = 0.8f),
                        startAngle = -90f,
                        sweepAngle = -angle,
                        useCenter = false,
                        topLeft = Offset(size.width/2 - arcSize.width/2, size.height/2 - arcSize.height/2),
                        size = arcSize,
                        style = Stroke(width = 4f)
                    )
                }
                
              	val text = formatBytes(value)
                val textBound = Rect()
                val textPaint = Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 24f
                    isAntiAlias = true
                }
                textPaint.getTextBounds(text, 0, text.length, textBound)
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    (size.width - textBound.width()) / 2f,
                    (size.height + textBound.height()) / 2f,
                    textPaint
                )
            }
        }
        
        Text(
            text = formatBytes(value),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SpeedLineChart(
    txSpeedHistory: List<Long>,
    rxSpeedHistory: List<Long>,
    modifier: Modifier = Modifier
) {
    val maxSpeed = 1000L
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        drawRect(color = Color(0xFF1E1E2E))
        
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = Color(0xFF333333),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        if (txSpeedHistory.isNotEmpty()) {
            val path = Path()
            txSpeedHistory.forEachIndexed { index, speed ->
                val x = (index.toFloat() / (txSpeedHistory.size - 1).coerceAtLeast(1)) * width
                val y = height - (speed.coerceAtMost(maxSpeed) / maxSpeed) * height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = Color(0xFF4FC3F7), style = Stroke(width = 2f))
        }
        
        if (rxSpeedHistory.isNotEmpty()) {
            val path = Path()
            rxSpeedHistory.forEachIndexed { index, speed ->
                val x = (index.toFloat() / (rxSpeedHistory.size - 1).coerceAtLeast(1)) * width
                val y = height - (speed.coerceAtMost(maxSpeed) / maxSpeed) * height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = Color(0xFF81C784), style = Stroke(width = 2f))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1073741824 -> String.format(Locale.US, "%.2f GB", bytes / 1073741824.0)
        bytes >= 1048576 -> String.format(Locale.US, "%.2f MB", bytes / 1048576.0)
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    label: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E2E))
            .padding(16.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF8888AA),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subValue,
            color = Color(0xFF666688),
            fontSize = 11.sp
        )
    }
}

private data class Sext<A, B, C, D, E, F>(
    val first: A, val second: B, val third: C,
    val fourth: D, val fifth: E, val sixth: F
)

private suspend fun savePrefs(
    context: Context, server: String, ip: String, prefix: Int,
    ipv6: String, prefix6: Int, key: String
) {
    context.dataStore.edit { prefs ->
        prefs[VnetPrefs.SERVER] = server
        prefs[VnetPrefs.IP] = ip
        prefs[VnetPrefs.PREFIX] = prefix
        prefs[VnetPrefs.IPV6] = ipv6
        prefs[VnetPrefs.PREFIX6] = prefix6
        prefs[VnetPrefs.KEY] = key
    }
}
