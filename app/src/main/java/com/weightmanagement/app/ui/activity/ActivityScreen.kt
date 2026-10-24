package com.weightmanagement.app.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.weightmanagement.app.domain.calculator.MetLibrary
import com.weightmanagement.app.domain.model.Labels
import com.weightmanagement.app.ui.common.AppTopBar
import com.weightmanagement.app.ui.common.ChipGroup
import com.weightmanagement.app.ui.common.SectionCard
import com.weightmanagement.app.ui.common.format0
import com.weightmanagement.app.ui.common.format1
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

@Composable
fun ActivityScreen(navController: NavHostController, viewModel: ActivityViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val availableDays by viewModel.availableDays.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    val today = com.weightmanagement.app.domain.util.DateUtils.dayStart()

    if (showAdd) {
        AddActivityDialog(
            initialWeight = user?.currentWeightKg ?: 70f,
            onDismiss = { showAdd = false },
            onSave = { type, dur, intensity, w ->
                viewModel.add(type, dur, intensity, w)
                showAdd = false
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "运动记录")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // 历史日期选择（与饮水页一致，首项固定「今天」）
            Text("选择日期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(today) + availableDays.filter { it != today }.distinct()) { day ->
                    val selected = day == selectedDay
                    androidx.compose.material3.AssistChip(
                        onClick = { viewModel.selectDay(day) },
                        label = { Text(if (day == today) "今天" else dayLabel(day)) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(title = if (selectedDay == today) "今日运动消耗" else "${dayLabel(selectedDay)} 运动消耗") {
                val dayBurned = activities.sumOf { it.caloriesBurned.toDouble() }.toFloat()
                Text("${format0(dayBurned)} kcal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("添加运动")
            }
            Spacer(Modifier.height(12.dp))
            if (activities.isEmpty()) {
                Text(
                    "暂无记录，点上方「添加运动」开始",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                activities.forEach { a ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(MetLibrary.LABELS[a.type] ?: a.type, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("${a.durationMin}分钟 · ${Labels.INTENSITY[a.intensity] ?: a.intensity} · MET ${format1(a.met)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(sdf.format(Date(a.startTime)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${format0(a.caloriesBurned)} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.delete(a) }) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun dayLabel(ts: Long): String {
    val today = com.weightmanagement.app.domain.util.DateUtils.dayStart()
    return when (ts) {
        today -> "今天"
        today - 24L * 3600 * 1000 -> "昨天"
        else -> com.weightmanagement.app.domain.util.DateUtils.formatDateTime(ts).substring(0, 10)
    }
}

@Composable
private fun AddActivityDialog(
    initialWeight: Float,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, Float) -> Unit
) {
    var type by remember { mutableStateOf("walking") }
    var duration by remember { mutableStateOf("30") }
    var intensity by remember { mutableStateOf("moderate") }
    var weight by remember { mutableStateOf(format1(initialWeight)) }

    val met = MetLibrary.metFor(type)
    val burned = MetLibrary.caloriesBurned(met, weight.toFloatOrNull() ?: initialWeight, duration.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onSave(type, duration.toIntOrNull() ?: 0, intensity, weight.toFloatOrNull() ?: initialWeight)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("添加运动") },
        text = {
            Column {
                Text("运动类型")
                ChipGroup(MetLibrary.LABELS.toList(), type) { type = it }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(duration, { duration = it }, label = { Text("时长 (分钟)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Spacer(Modifier.height(8.dp))
                Text("强度")
                ChipGroup(Labels.INTENSITY.toList(), intensity) { intensity = it }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(weight, { weight = it }, label = { Text("体重 (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Spacer(Modifier.height(8.dp))
                Text("预计消耗：约 ${format0(burned)} kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
