package com.weightmanagement.app.ui.nutrition

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.weightmanagement.app.data.local.entity.IntakeEntity
import com.weightmanagement.app.domain.model.Labels
import com.weightmanagement.app.domain.util.DateUtils
import com.weightmanagement.app.ui.common.AppTopBar
import com.weightmanagement.app.ui.common.LabeledStat
import com.weightmanagement.app.ui.common.SectionCard
import com.weightmanagement.app.ui.common.format1
import java.io.File

private val MEAL_ORDER = listOf("breakfast", "lunch", "dinner", "snack")

private fun dayLabel(ts: Long): String {
    val today = DateUtils.dayStart()
    return when (ts) {
        today -> "今天"
        today - 24L * 3600 * 1000 -> "昨天"
        else -> DateUtils.formatDateTime(ts).substring(0, 10)
    }
}

@Composable
fun NutritionScreen(navController: NavHostController, viewModel: NutritionViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val availableDays by viewModel.availableDays.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val aiConfigured by viewModel.aiConfigured.collectAsStateWithLifecycle()
    var addTarget by remember { mutableStateOf<String?>(null) }
    var showAddMealPicker by remember { mutableStateOf(false) }
    var photoMealTarget by remember { mutableStateOf<String?>(null) }

    // 相册选图
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val mealType = photoMealTarget
        if (uri != null && mealType != null) {
            viewModel.onImagePicked(mealType, uri)
        }
        photoMealTarget = null
    }

    // 相机拍照：拍前建临时文件（cache/camera/），拍完走识别流程
    var cameraTmpFile by remember { mutableStateOf<java.io.File?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val mealType = photoMealTarget
        val f = cameraTmpFile
        if (ok && mealType != null && f != null && f.exists()) {
            viewModel.onCameraShot(mealType, f)
        }
        cameraTmpFile = null
        photoMealTarget = null
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    val mealByType = meals.associateBy { it.mealType }
    val today = DateUtils.dayStart()

    // 「添加记录」先选餐别（早餐/午餐/晚餐/零食），拍照/相册按当前时间自动判断餐别
    fun autoMealTypeNow(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..9 -> "breakfast"
            in 10..13 -> "lunch"
            in 17..20 -> "dinner"
            else -> "snack"
        }
    }

    if (showAddMealPicker) {
        MealPickerDialog(
            onDismiss = { showAddMealPicker = false },
            onPick = { type ->
                showAddMealPicker = false
                addTarget = type
            }
        )
    }

    if (addTarget != null) {
        AddIntakeDialog(
            mealType = addTarget!!,
            viewModel = viewModel,
            onDismiss = { addTarget = null },
            onSave = { name, g, c, p, f, cb ->
                viewModel.add(addTarget!!, name, g, c, p, f, cb)
                addTarget = null
            }
        )
    }

    // 拍照/上传识别流程弹层
    when (val s = scanState) {
        is PhotoScanState.Analyzing -> ScanDialog(
            title = "AI 识别中…",
            body = { Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("正在分析食物图片")
            } },
            onDismiss = { viewModel.dismissScan() }
        )
        is PhotoScanState.ConfirmAi -> ConfirmAiDialog(
            state = s,
            aiConfigured = aiConfigured,
            viewModel = viewModel,
            onConfirm = { viewModel.confirmAi(it) },
            onDismiss = { viewModel.dismissScan() }
        )
        is PhotoScanState.NeedConfig -> ScanChoiceDialog(
            title = "未配置 AI 识别",
            message = "拍照识别需要先配置 AI 视觉接口（OpenAI 兼容）。请到「设置 → AI 食物识别」填写接口地址、API Key 和视觉模型名；也可改用本地食物库手动选择。",
            onRetry = { navController.navigate(com.weightmanagement.app.navigation.Routes.SETTINGS) ; viewModel.dismissScan() },
            retryLabel = "去设置",
            onLocal = { viewModel.goLocalPicker() },
            onDismiss = { viewModel.dismissScan() }
        )
        is PhotoScanState.AiFailed -> ScanChoiceDialog(
            title = "AI 识别失败",
            message = s.message,
            onRetry = { viewModel.retryAi() },
            retryLabel = "重试",
            onLocal = { viewModel.goLocalPicker() },
            onDismiss = { viewModel.dismissScan() }
        )
        is PhotoScanState.PickLocal -> PickLocalDialog(
            state = s,
            onSearch = { viewModel.searchLocal(it) },
            onPick = { food, grams -> viewModel.pickLocal(food, grams) },
            onDismiss = { viewModel.dismissScan() }
        )
        is PhotoScanState.Error -> ScanDialog(
            title = "识别失败",
            body = { Text(s.message) },
            onDismiss = { viewModel.dismissScan() }
        )
        PhotoScanState.Idle -> {}
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "饮食日记")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 历史日期选择（首项固定「今天」，方便随时切回）
            Text("选择日期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(today) + availableDays.filter { it != today }.distinct()) { day ->
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

            SectionCard(title = if (selectedDay == today) "今日营养" else "${dayLabel(selectedDay)} 营养") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LabeledStat("热量", format1(daily.calories) + "kcal")
                    LabeledStat("蛋白质", format1(daily.protein) + "g")
                    LabeledStat("脂肪", format1(daily.fat) + "g")
                    LabeledStat("碳水", format1(daily.carbs) + "g")
                }
                if (!aiConfigured) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "提示：未配置 AI 识别接口，拍照识别需先到 设置 → AI 食物识别 配置视觉模型。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 今日所吃：标题 → 操作按钮行（固定在标题下方）→ 记录列表
            SectionCard(title = if (selectedDay == today) "今日所吃" else "${dayLabel(selectedDay)} 所吃") {
                // 三个操作按钮：等宽、同一 Button 样式、图标统一 18dp
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { addTarget = null; showAddMealPicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.height(18.dp))
                        Text("添加记录")
                    }
                    Button(
                        onClick = {
                            photoMealTarget = autoMealTypeNow()
                            val dir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
                            val f = java.io.File(dir, "shot_${System.currentTimeMillis()}.jpg")
                            cameraTmpFile = f
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, context.packageName + ".fileprovider", f
                            )
                            takePicture.launch(uri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.height(18.dp))
                        Text("拍照")
                    }
                    Button(
                        onClick = {
                            photoMealTarget = autoMealTypeNow()
                            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.height(18.dp))
                        Text("相册")
                    }
                }
                Spacer(Modifier.height(10.dp))
                // 全部记录（按时间倒序，跨餐别），在按钮行下方
                DayIntakeList(viewModel = viewModel)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 识别流程弹层通用标题文案 */
private const val TAG = "NutritionScreen"

@Composable
private fun ScanDialog(
    title: String,
    body: @Composable () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(title) },
        text = { body() }
    )
}

/** 未配置 / 失败场景：展示原因 + 重试（或去设置）+ 转本地库 */
@Composable
private fun ScanChoiceDialog(
    title: String,
    message: String,
    onRetry: () -> Unit,
    retryLabel: String,
    onLocal: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onRetry) { Text(retryLabel) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onLocal) { Text("手动选择") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
        title = { Text(title) },
        text = { Text(message) }
    )
}

@Composable
private fun ConfirmAiDialog(
    state: PhotoScanState.ConfirmAi,
    aiConfigured: Boolean,
    viewModel: NutritionViewModel,
    onConfirm: (com.weightmanagement.app.domain.model.RecognizedFood) -> Unit,
    onDismiss: () -> Unit
) {
    val r = state.result
    var name by remember { mutableStateOf(r.displayName.ifBlank { "AI 识别食物" }) }
    var grams by remember { mutableStateOf(if (r.grams > 0f) format1(r.grams) else "") }
    var cal by remember { mutableStateOf(if (r.calories > 0f) format1(r.calories) else "") }
    var pro by remember { mutableStateOf(if (r.protein > 0f) format1(r.protein) else "") }
    var fat by remember { mutableStateOf(if (r.fat > 0f) format1(r.fat) else "") }
    var carb by remember { mutableStateOf(if (r.carbs > 0f) format1(r.carbs) else "") }
    val estimateState by viewModel.estimateState.collectAsStateWithLifecycle()

    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.resetEstimate()
        onDispose { viewModel.resetEstimate() }
    }

    fun tryEstimate() {
        val g = grams.toFloatOrNull()
        if (name.isNotBlank() && g != null && g > 0f) {
            viewModel.estimateNutrition(name.trim(), g)
        }
    }

    // 估算成功 → 自动填充（克数与请求时一致才覆盖，避免竞态）
    when (val es = estimateState) {
        is NutritionViewModel.EstimateUiState.Done -> {
            val g = grams.toFloatOrNull()
            if (g != null && kotlin.math.abs(g - es.grams) < 0.01f) {
                cal = format1(es.calories)
                pro = format1(es.protein)
                fat = format1(es.fat)
                carb = format1(es.carbs)
            }
        }
        else -> {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    com.weightmanagement.app.domain.model.RecognizedFood(
                        mealName = r.mealName,
                        name = name.ifBlank { "AI 识别食物" },
                        grams = grams.toFloatOrNull() ?: 0f,
                        calories = cal.toFloatOrNull() ?: 0f,
                        protein = pro.toFloatOrNull() ?: 0f,
                        fat = fat.toFloatOrNull() ?: 0f,
                        carbs = carb.toFloatOrNull() ?: 0f,
                        foodItems = r.foodItems
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text(if (aiConfigured) "AI 识别结果" else "识别结果") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AsyncImage(
                    model = File(state.imageUri),
                    contentDescription = "食物图片",
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
                // AI 拆解的食物明细
                if (r.foodItems.isNotEmpty()) {
                    Text("AI 识别的食物组成：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    r.foodItems.forEach { item ->
                        Text(
                            "· ${item.name} ${format1(item.weightG)}g（${format1(item.calories)}kcal）",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(name, { name = it }, label = { Text("食物名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                NumberField("重量 (g)", grams) { grams = it }
                // 更改食物/重量后重新估算营养
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { tryEstimate() },
                        enabled = name.isNotBlank() && !grams.isBlank() && estimateState !is NutritionViewModel.EstimateUiState.Loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (estimateState is NutritionViewModel.EstimateUiState.Loading) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (estimateState is NutritionViewModel.EstimateUiState.Loading) "估算中…" else "按新食物/重量重新估算")
                    }
                }
                when (val es = estimateState) {
                    is NutritionViewModel.EstimateUiState.Error -> {
                        Spacer(Modifier.height(4.dp))
                        Text(es.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    is NutritionViewModel.EstimateUiState.Done -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "已${if (es.source == "local") "按本地营养库" else "按 AI 估算"}刷新营养值",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {}
                }
                Spacer(Modifier.height(4.dp))
                NumberField("热量 (kcal)", cal) { cal = it }
                NumberField("蛋白质 (g)", pro) { pro = it }
                NumberField("脂肪 (g)", fat) { fat = it }
                NumberField("碳水 (g)", carb) { carb = it }
            }
        }
    )
}

@Composable
private fun PickLocalDialog(
    state: PhotoScanState.PickLocal,
    onSearch: (String) -> Unit,
    onPick: (com.weightmanagement.app.data.local.entity.FoodEntity, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember(state.keyword) { mutableStateOf(state.keyword) }
    var selected by remember { mutableStateOf<com.weightmanagement.app.data.local.entity.FoodEntity?>(null) }
    var grams by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val f = selected
            Button(onClick = { if (f != null) onPick(f, grams.toFloatOrNull() ?: 0f) }, enabled = selected != null) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("选择食物（本地营养库）") },
        text = {
            Column {
                AsyncImage(
                    model = File(state.imageUri),
                    contentDescription = "食物图片",
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    keyword, { keyword = it; onSearch(it) },
                    label = { Text("搜索食物") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    items(state.candidates) { food ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { selected = food },
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = if (selected?.name == food.name)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(food.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    "每100g：${format1(food.calories)}kcal · 蛋${format1(food.protein)} 脂${format1(food.fat)} 碳${format1(food.carbs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (selected != null) {
                    NumberField("重量 (g)（默认 ${format1(selected!!.servingSizeG)}g）", grams) { grams = it }
                }
            }
        }
    )
}

/** 选餐别弹层：添加记录时先选早餐/午餐/晚餐/零食 */
@Composable
private fun MealPickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("这一餐属于？") },
        text = {
            Column {
                MEAL_ORDER.forEach { type ->
                    Button(
                        onClick = { onPick(type) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) { Text(Labels.MEAL_TYPE[type] ?: type) }
                }
            }
        }
    )
}

/** 选中日全部饮食记录（单一列表，跨餐别、按时间倒序，标注餐别） */
@Composable
private fun DayIntakeList(viewModel: NutritionViewModel) {
    val dayIntakes by viewModel.dayIntakes.collectAsStateWithLifecycle()
    if (dayIntakes.isEmpty()) {
        Text("暂无记录，点下方「添加记录」或「拍照」开始", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    dayIntakes.forEach { (mealType, item) ->
        Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.imageUri.isNotBlank()) {
                    AsyncImage(
                        model = File(item.imageUri),
                        contentDescription = "食物图片",
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        item.foodName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${Labels.MEAL_TYPE[mealType] ?: mealType} · ${format1(item.grams)}g · ${format1(item.calories)}kcal · 蛋${format1(item.protein)} 脂${format1(item.fat)} 碳${format1(item.carbs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        DateUtils.formatDateTime(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.source == "ai") Text("AI 识别", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    else if (item.source == "photo") Text("拍照记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.delete(item) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
private fun AddIntakeDialog(
    mealType: String,
    viewModel: NutritionViewModel,
    onDismiss: () -> Unit,
    onSave: (String, Float, Float, Float, Float, Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    var cal by remember { mutableStateOf("") }
    var pro by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    val estimateState by viewModel.estimateState.collectAsStateWithLifecycle()

    // 打开/关闭对话框时重置估算状态
    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.resetEstimate()
        onDispose { viewModel.resetEstimate() }
    }

    fun tryEstimate() {
        val g = grams.toFloatOrNull()
        if (name.isNotBlank() && g != null && g > 0f) {
            viewModel.estimateNutrition(name.trim(), g)
        }
    }

    // 估算成功 → 自动填充（仅覆盖，不重复触发）
    when (val es = estimateState) {
        is NutritionViewModel.EstimateUiState.Done -> {
            val g = grams.toFloatOrNull()
            if (g != null && kotlin.math.abs(g - es.grams) < 0.01f) {
                cal = format1(es.calories)
                pro = format1(es.protein)
                fat = format1(es.fat)
                carb = format1(es.carbs)
            }
        }
        else -> {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onSave(
                    name.ifBlank { "食物" },
                    grams.toFloatOrNull() ?: 0f,
                    cal.toFloatOrNull() ?: 0f,
                    pro.toFloatOrNull() ?: 0f,
                    fat.toFloatOrNull() ?: 0f,
                    carb.toFloatOrNull() ?: 0f
                )
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("添加${Labels.MEAL_TYPE[mealType] ?: ""}食物") },
        text = {
            Column {
                OutlinedTextField(
                    name, { name = it },
                    label = { Text("食物名称") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                NumberField("重量 (g)", grams) { grams = it }
                // 自动估算：食物名+重量就绪后一键带出营养值
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { tryEstimate() },
                        enabled = name.isNotBlank() && !grams.isBlank() && estimateState !is NutritionViewModel.EstimateUiState.Loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (estimateState is NutritionViewModel.EstimateUiState.Loading) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (estimateState is NutritionViewModel.EstimateUiState.Loading) "估算中…" else "自动估算营养")
                    }
                }
                when (val es = estimateState) {
                    is NutritionViewModel.EstimateUiState.Error -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            es.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is NutritionViewModel.EstimateUiState.Done -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "已${if (es.source == "local") "按本地营养库" else "按 AI 估算"}填充，可手动修改",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {}
                }
                Spacer(Modifier.height(4.dp))
                NumberField("热量 (kcal)", cal) { cal = it }
                NumberField("蛋白质 (g)", pro) { pro = it }
                NumberField("脂肪 (g)", fat) { fat = it }
                NumberField("碳水 (g)", carb) { carb = it }
            }
        }
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
