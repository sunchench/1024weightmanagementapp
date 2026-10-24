package com.weightmanagement.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.weightmanagement.app.data.local.dao.NutritionSum
import com.weightmanagement.app.data.local.entity.WeightLogEntity
import com.weightmanagement.app.domain.calculator.BmiCalculator
import com.weightmanagement.app.domain.util.DateUtils
import com.weightmanagement.app.navigation.Routes
import com.weightmanagement.app.ui.common.AppTopBar
import com.weightmanagement.app.ui.common.LabeledStat
import com.weightmanagement.app.ui.common.MacroProgressBar
import com.weightmanagement.app.ui.common.SectionCard
import com.weightmanagement.app.ui.common.WeightPoint
import com.weightmanagement.app.ui.common.WeightTrendChart
import com.weightmanagement.app.ui.common.format1
import java.util.Calendar

fun changeInWindow(weights: List<WeightLogEntity>, days: Int): Float? {
    if (weights.isEmpty()) return null
    val from = DateUtils.daysAgoStart(days)
    val inWindow = weights.filter { it.timestamp >= from }
    if (inWindow.size < 2) return null
    return inWindow.first().weightKg - inWindow.last().weightKg
}

@Composable
fun DashboardScreen(navController: NavHostController, viewModel: DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val nutrition by viewModel.nutrition.collectAsStateWithLifecycle()
    val exercise by viewModel.exerciseBurned.collectAsStateWithLifecycle()
    val water by viewModel.waterTotal.collectAsStateWithLifecycle()
    val weights by viewModel.weights.collectAsStateWithLifecycle()

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 6 -> "凌晨好"
        hour < 12 -> "早上好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
    val name = user?.nickname?.takeIf { it.isNotBlank() } ?: "你好"

    val latest = weights.firstOrNull()
    val change7 = changeInWindow(weights, 7)
    val change30 = changeInWindow(weights, 30)

    val chartPoints = weights.asReversed().map { WeightPoint(it.timestamp, it.weightKg) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "$greeting, $name",
            actions = {
                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "设置")
                }
            }
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 当前体重
            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("当前体重", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            format1(latest?.weightKg ?: user?.currentWeightKg ?: 0f) + " kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("目标 ${format1(user?.targetWeightKg ?: 0f)} kg", style = MaterialTheme.typography.bodyMedium)
                        val diff = (latest?.weightKg ?: user?.currentWeightKg ?: 0f) - (user?.targetWeightKg ?: 0f)
                        Text("距目标 ${format1(kotlin.math.abs(diff))} kg", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (user != null) {
                    val bmi = BmiCalculator.bmi(latest?.weightKg ?: user!!.currentWeightKg, user!!.heightCm)
                    Text(
                        "BMI ${format1(bmi)} · ${BmiCalculator.wgocCategory(bmi)}（中国标准）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("近7天", style = MaterialTheme.typography.labelSmall)
                        Text(
                            change7?.let { (if (it >= 0) "↓ " else "↑ ") + format1(kotlin.math.abs(it)) + "kg" } ?: "—",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("近30天", style = MaterialTheme.typography.labelSmall)
                        Text(
                            change30?.let { (if (it >= 0) "↓ " else "↑ ") + format1(kotlin.math.abs(it)) + "kg" } ?: "—",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(title = "体重趋势（近30次 · 点击数据点查看记录时间）") {
                if (chartPoints.size >= 2) {
                    WeightTrendChart(points = chartPoints, lineColor = MaterialTheme.colorScheme.primary)
                } else {
                    Text("记录体重后将显示趋势图", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(title = "今日热量") {
                val target = goal?.calorieTarget?.toFloat() ?: 0f
                MacroProgressBar("热量", nutrition.calories, target, "kcal", MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LabeledStat("蛋白质", format1(nutrition.protein) + "g", "目标 ${format1(goal?.proteinTarget ?: 0f)}")
                    LabeledStat("碳水", format1(nutrition.carbs) + "g", "目标 ${format1(goal?.carbTarget ?: 0f)}")
                    LabeledStat("脂肪", format1(nutrition.fat) + "g", "目标 ${format1(goal?.fatTarget ?: 0f)}")
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(title = "今日概览") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LabeledStat("运动消耗", format1(exercise) + "kcal")
                    LabeledStat("饮水", "${water}ml", "目标 ${goal?.waterTargetMl ?: 2000}")
                    LabeledStat("净摄入", format1(nutrition.calories - exercise) + "kcal")
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { navController.navigate(Routes.WEIGHT) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("记录体重")
                }
                Button(onClick = { navController.navigate(Routes.WATER) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.LocalDrink, contentDescription = null)
                    Text("饮水记录")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { navController.navigate(Routes.NUTRITION) }, modifier = Modifier.weight(1f)) {
                    Text("饮食日记")
                }
                Button(onClick = { navController.navigate(Routes.ACTIVITY) }, modifier = Modifier.weight(1f)) {
                    Text("运动记录")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
