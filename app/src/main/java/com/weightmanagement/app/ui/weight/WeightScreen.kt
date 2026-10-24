package com.weightmanagement.app.ui.weight

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.weightmanagement.app.data.local.entity.WeightLogEntity
import com.weightmanagement.app.ui.common.AppTopBar
import com.weightmanagement.app.ui.common.SectionCard
import com.weightmanagement.app.ui.common.WeightPoint
import com.weightmanagement.app.ui.common.WeightTrendChart
import com.weightmanagement.app.ui.common.format1
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

@Composable
fun WeightScreen(navController: NavHostController, viewModel: WeightViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val weights by viewModel.weights.collectAsStateWithLifecycle()

    var weightText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<WeightLogEntity?>(null) }

    val chartPoints = weights.asReversed().map { WeightPoint(it.timestamp, it.weightKg) }

    if (editing != null) {
        EditWeightDialog(
            log = editing!!,
            onDismiss = { editing = null },
            onSave = { w, n ->
                viewModel.update(editing!!.copy(weightKg = w, note = n))
                editing = null
            },
            onDelete = {
                viewModel.delete(editing!!)
                editing = null
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "体重记录")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionCard(title = "趋势（点击数据点查看记录时间）") {
                if (chartPoints.size >= 2) WeightTrendChart(chartPoints, MaterialTheme.colorScheme.primary)                else Text("暂无足够数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "添加记录") {
                OutlinedTextField(weightText, { weightText = it }, label = { Text("体重 (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val w = weightText.toFloatOrNull()
                        if (w != null && w > 0) {
                            viewModel.add(w, note)
                            weightText = ""
                            note = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存") }
            }
            Spacer(Modifier.height(12.dp))
            Text("历史记录", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                items(weights) { log ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editing = log }) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(format1(log.weightKg) + " kg", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                if (log.note.isNotBlank()) Text(log.note, style = MaterialTheme.typography.bodySmall)
                                Text(sdf.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.delete(log) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EditWeightDialog(
    log: WeightLogEntity,
    onDismiss: () -> Unit,
    onSave: (Float, String) -> Unit,
    onDelete: () -> Unit
) {
    var w by remember { mutableStateOf(log.weightKg.toString()) }
    var n by remember { mutableStateOf(log.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { w.toFloatOrNull()?.let { onSave(it, n) } }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
        title = { Text("编辑记录") },
        text = {
            Column {
                OutlinedTextField(w, { w = it }, label = { Text("体重 (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(n, { n = it }, label = { Text("备注") })
            }
        }
    )
}
