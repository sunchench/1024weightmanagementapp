package com.weightmanagement.app.ui.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.weightmanagement.app.domain.util.DateUtils
import com.weightmanagement.app.ui.common.AppTopBar
import com.weightmanagement.app.ui.common.SectionCard

private fun dayLabel(ts: Long): String {
    val today = DateUtils.dayStart()
    return when (ts) {
        today -> "今天"
        today - 24L * 3600 * 1000 -> "昨天"
        else -> DateUtils.formatDateTime(ts).substring(0, 10)
    }
}

@Composable
fun WaterScreen(navController: NavHostController, viewModel: WaterViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val total by viewModel.todayTotal.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val availableDays by viewModel.availableDays.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()

    val today = DateUtils.dayStart()
    val target = goal?.waterTargetMl ?: 2000
    val ratio = if (target > 0) (total.toFloat() / target).coerceIn(0f, 1.5f) else 0f

    var custom by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "饮水记录")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // 历史日期选择
            Text("选择日期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableDays) { day ->
                    val selected = day == selectedDay
                    AssistChip(
                        onClick = { viewModel.selectDay(day) },
                        label = { Text(if (day == today) "今天" else dayLabel(day)) },
                        shape = RoundedCornerShape(50),
                        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(title = if (selectedDay == today) "今日饮水" else "${dayLabel(selectedDay)} 饮水") {
                Text("${total} / ${target} ml", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.add(250) }, modifier = Modifier.weight(1f)) { Text("+250ml") }
                Button(onClick = { viewModel.add(500) }, modifier = Modifier.weight(1f)) { Text("+500ml") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(custom, { custom = it }, label = { Text("自定义 (ml)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Button(onClick = {
                    custom.toIntOrNull()?.let { viewModel.add(it) }
                    custom = ""
                }) { Icon(Icons.Filled.Add, contentDescription = null); Text("添加") }
            }
            Spacer(Modifier.height(12.dp))
            Text("记录明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                items(records) { w ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${w.ml} ml", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(DateUtils.formatDateTime(w.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(onClick = { viewModel.delete(w) }) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
