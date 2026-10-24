package com.weightmanagement.app.ui.onboarding

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.domain.model.Labels
import com.weightmanagement.app.navigation.Routes
import com.weightmanagement.app.ui.common.ChipGroup
import com.weightmanagement.app.ui.common.SectionCard

@Composable
fun OnboardingScreen(navController: NavHostController, viewModel: OnboardingViewModel = hiltViewModel()) {
    var nickname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var age by remember { mutableStateOf("25") }
    var height by remember { mutableStateOf("170") }
    var currentWeight by remember { mutableStateOf("70") }
    var targetWeight by remember { mutableStateOf("65") }
    var activityLevel by remember { mutableStateOf("moderate") }
    var goalType by remember { mutableStateOf("lose") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("欢迎使用体重管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("先花一分钟建立你的健康档案", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        SectionCard(title = "基本信息") {
            OutlinedTextField(nickname, { nickname = it }, label = { Text("昵称（可选）") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("性别")
            ChipGroup(listOf("male" to "男", "female" to "女"), gender) { gender = it }
            Spacer(Modifier.height(8.dp))
            LabeledNumber("年龄", age) { age = it }
            LabeledNumber("身高 (cm)", height) { height = it }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard(title = "体重目标") {
            LabeledNumber("当前体重 (kg)", currentWeight) { currentWeight = it }
            Spacer(Modifier.height(8.dp))
            LabeledNumber("目标体重 (kg)", targetWeight) { targetWeight = it }
            Spacer(Modifier.height(8.dp))
            Text("目标类型")
            ChipGroup(Labels.GOAL_TYPE.toList(), goalType) { goalType = it }
            Spacer(Modifier.height(8.dp))
            Text("活动水平")
            ChipGroup(Labels.ACTIVITY_LEVEL.toList(), activityLevel) { activityLevel = it }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val user = UserEntity(
                    nickname = nickname,
                    name = nickname,
                    gender = gender,
                    age = age.toIntOrNull() ?: 25,
                    heightCm = height.toFloatOrNull() ?: 170f,
                    currentWeightKg = currentWeight.toFloatOrNull() ?: 70f,
                    targetWeightKg = targetWeight.toFloatOrNull() ?: 65f,
                    activityLevel = activityLevel,
                    goalType = goalType
                )
                viewModel.save(user) {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始管理")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LabeledNumber(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.height(8.dp))
    }
}
