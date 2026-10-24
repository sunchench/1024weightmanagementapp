package com.weightmanagement.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.weightmanagement.app.navigation.Routes
import com.weightmanagement.app.ui.common.AppTopBar
import com.weightmanagement.app.ui.common.SectionCard

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var showClear by remember { mutableStateOf(false) }
    var showAi by remember { mutableStateOf(false) }
    val aiConfig by viewModel.aiConfig.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "设置", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            SectionCard(title = "数据与备份") {
                Button(onClick = { navController.navigate(Routes.BACKUP) }, modifier = Modifier.fillMaxWidth()) {
                    Text("备份 / 恢复")
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(title = "AI 食物识别") {
                Text(
                    if (aiConfig.configured)
                        "已配置：${aiConfig.model}（拍照将调用 AI 视觉识别）"
                    else
                        "未配置。拍照识别将回退到本地食物库匹配；配置 OpenAI 兼容视觉接口后可自动识别食物与估算热量。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showAi = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (aiConfig.configured) "修改 AI 接口配置" else "配置 AI 接口")
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(title = "危险操作") {
                Button(
                    onClick = { showClear = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清空所有数据")
                }
            }

            Spacer(Modifier.height(12.dp))
            AboutSection(viewModel)

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearAllData {
                        showClear = false
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                        }
                    }
                }) { Text("确认清空") }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
            title = { Text("清空所有数据？") },
            text = { Text("此操作将删除全部体重、饮食、运动、饮水与目标数据，且不可恢复。建议先备份。") }
        )
    }

    if (showAi) {
        var baseUrl by remember(aiConfig) { mutableStateOf(aiConfig.baseUrl) }
        var apiKey by remember(aiConfig) { mutableStateOf(aiConfig.apiKey) }
        var model by remember(aiConfig) { mutableStateOf(aiConfig.model) }
        val testing by viewModel.testing.collectAsStateWithLifecycle()
        val testResult by viewModel.testResult.collectAsStateWithLifecycle()
        // 打开对话框时清除上次的检测结果
        androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.clearTestResult() }
        AlertDialog(
            onDismissRequest = { showAi = false },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveAiConfig(baseUrl, apiKey, model)
                    showAi = false
                }) { Text("保存") }
            },
            dismissButton = {
                androidx.compose.foundation.layout.Row {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.testAiConfig(baseUrl, apiKey, model) },
                        enabled = !testing
                    ) {
                        if (testing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.padding(end = 6.dp).height(14.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(if (testing) "检测中…" else "检测")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { showAi = false }) { Text("取消") }
                }
            },
            title = { Text("AI 视觉接口（OpenAI 兼容）") },
            text = {
                Column {
                    OutlinedTextField(
                        baseUrl, { baseUrl = it },
                        label = { Text("接口地址（如 https://api.example.com/）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        apiKey, { apiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        model, { model = it },
                        label = { Text("视觉模型名（如 doubao-seed-2.1-pro）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (testResult != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            testResult!!.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testResult!!.first)
                                androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "配置保存在本机，仅用于你主动发起的拍照识别请求。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

/** 关于区块：展示当前版本信息 + 检查更新（运行时从 PackageManager 读取，无需手动维护） */
@Composable
private fun AboutSection(viewModel: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val versionInfo = remember {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            val info = pm.getPackageInfo(pkg, 0)
            val versionName = info.versionName ?: "未知"
            @Suppress("DEPRECATION")
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
            versionName to versionCode
        } catch (e: Exception) {
            "未知" to 0L
        }
    }
    val (versionName, versionCode) = versionInfo

    SectionCard(title = "关于") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text("当前版本", style = MaterialTheme.typography.bodyMedium)
            Text(
                "v$versionName（内部版本 $versionCode）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "个人体重管理 · 数据仅保存在本机。升级安装不会丢失历史数据。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        // 检查更新
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.checkUpdate() },
                enabled = updateState !is SettingsViewModel.UpdateUiState.Checking,
                modifier = Modifier.weight(1f)
            ) {
                if (updateState is SettingsViewModel.UpdateUiState.Checking) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.padding(end = 6.dp).height(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    when (updateState) {
                        is SettingsViewModel.UpdateUiState.Checking -> "检查中…"
                        else -> "检查更新"
                    }
                )
            }
        }
        when (val u = updateState) {
            is SettingsViewModel.UpdateUiState.UpToDate -> {
                Spacer(Modifier.height(6.dp))
                Text(
                    "已是最新版本 v${u.currentName} ✓",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                )
            }
            is SettingsViewModel.UpdateUiState.Available -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    "发现新版本 v${u.versionName}！",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                u.releaseNotes.take(5).forEach {
                    Text("· $it", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                val ctx = androidx.compose.ui.platform.LocalContext.current
                var installMsg by remember { mutableStateOf<String?>(null) }
                if (installMsg != null) {
                    Text(
                        installMsg!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // 需要安装权限时给一键跳转按钮
                    if (installMsg == "NEED_INSTALL_PERMISSION") {
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { viewModel.requestInstallPermission() }, modifier = Modifier.fillMaxWidth()) {
                            Text("去开启「允许安装未知应用」")
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Button(
                    onClick = {
                        viewModel.downloadUpdate(u.versionName, u.apkUrl) { result ->
                            when (result) {
                                "NO_APK_URL" -> {
                                    // 无 APK 直链 → 浏览器兜底
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(u.downloadUrl)
                                    )
                                    ctx.startActivity(android.content.Intent.createChooser(intent, "选择浏览器打开下载页"))
                                }
                                else -> installMsg = "下载已开始（通知栏查看进度）；下载完成后点「安装更新」"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("下载更新（v${u.versionName}）") }
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val r = viewModel.installUpdate(u.versionName)
                        installMsg = when (r) {
                            null -> "已唤起安装器，请在系统界面确认安装"
                            "NEED_INSTALL_PERMISSION" -> "NEED_INSTALL_PERMISSION"
                            else -> r
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("安装更新（下载完成后点此）") }
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(u.downloadUrl)
                        )
                        ctx.startActivity(android.content.Intent.createChooser(intent, "选择浏览器打开下载页"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("浏览器打开下载页（备用）", style = MaterialTheme.typography.labelMedium) }
            }
            is SettingsViewModel.UpdateUiState.Error -> {
                Spacer(Modifier.height(6.dp))
                Text(
                    u.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}
