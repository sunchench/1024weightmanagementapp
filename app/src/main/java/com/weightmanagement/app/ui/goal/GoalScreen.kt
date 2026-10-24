package com.weightmanagement.app.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.domain.calculator.BmiCalculator
import com.weightmanagement.app.domain.calculator.BmrCalculator
import com.weightmanagement.app.domain.calculator.TdeeCalculator
import com.weightmanagement.app.domain.model.Labels
import com.weightmanagement.app.ui.common.AppTopBar
import com.weightmanagement.app.ui.common.ChipGroup
import com.weightmanagement.app.ui.common.LabeledStat
import com.weightmanagement.app.ui.common.SectionCard
import com.weightmanagement.app.ui.common.format0
import com.weightmanagement.app.ui.common.format1

@Composable
fun GoalScreen(navController: NavHostController, viewModel: GoalViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()

    var targetWeight by remember { mutableStateOf(user?.targetWeightKg?.toString() ?: "65") }
    var goalType by remember { mutableStateOf(user?.goalType ?: "lose") }
    var activityLevel by remember { mutableStateOf(user?.activityLevel ?: "moderate") }

    if (user == null) {
        Text("未找到用户档案", modifier = Modifier.padding(16.dp))
        return
    }

    val bmr = BmrCalculator.mifflinStJeor(user!!.currentWeightKg, user!!.heightCm, user!!.age, user!!.gender)
    val tdee = TdeeCalculator.tdee(bmr, user!!.activityLevel)
    val bmi = BmiCalculator.bmi(user!!.currentWeightKg, user!!.heightCm)

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "目标与计算")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            SectionCard(title = "身体指标") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LabeledStat("BMI", format1(bmi), BmiCalculator.wgocCategory(bmi))
                    LabeledStat("基础代谢率", format0(bmr) + "kcal")
                    LabeledStat("每日总消耗", format0(tdee) + "kcal")
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "目标热量与营养") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LabeledStat("每日热量", (goal?.calorieTarget ?: 0).toString() + "kcal")
                    LabeledStat("蛋白", format1(goal?.proteinTarget ?: 0f) + "g")
                    LabeledStat("碳水", format1(goal?.carbTarget ?: 0f) + "g")
                    LabeledStat("脂肪", format1(goal?.fatTarget ?: 0f) + "g")
                }
                Spacer(Modifier.height(8.dp))
                Text("目标饮水：${goal?.waterTargetMl ?: 2000} ml", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "最近一周建议") {
                val rec = recommendations
                if (rec == null) {
                    Text("计算中…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val s = rec.stats
                    Text(
                        "本周概览：饮食记录 ${s.recordedDays}/7 天 · 运动 ${s.exerciseDays} 天 · 饮水记录 ${s.waterDays} 天" +
                            (s.weightChangeKg?.let { " · 体重${if (it >= 0) "+" else ""}${format1(it)}kg" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    RecommendationBlock("饮食", rec.diet)
                    Spacer(Modifier.height(8.dp))
                    RecommendationBlock("运动", rec.exercise)
                    Spacer(Modifier.height(8.dp))
                    RecommendationBlock("饮水", rec.water)
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.material3.TextButton(onClick = { viewModel.refreshRecommendations() }) {
                        Text("刷新建议")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "调整目标") {
                OutlinedTextField(targetWeight, { targetWeight = it }, label = { Text("目标体重 (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Spacer(Modifier.height(8.dp))
                Text("目标类型")
                ChipGroup(Labels.GOAL_TYPE.toList(), goalType) { goalType = it }
                Spacer(Modifier.height(8.dp))
                Text("活动水平")
                ChipGroup(Labels.ACTIVITY_LEVEL.toList(), activityLevel) { activityLevel = it }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val updated = user!!.copy(
                            targetWeightKg = targetWeight.toFloatOrNull() ?: user!!.targetWeightKg,
                            goalType = goalType,
                            activityLevel = activityLevel
                        )
                        viewModel.update(updated)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存并重新计算目标") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecommendationBlock(title: String, items: List<String>) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        items.forEach { tip ->
            Text(
                "· $tip",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
