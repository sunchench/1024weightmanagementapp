package com.weightmanagement.app.ui.backup

import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.weightmanagement.app.ui.common.AppTopBar
import com.weightmanagement.app.ui.common.SectionCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(navController: NavHostController, viewModel: BackupViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val context = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            }.onSuccess { text ->
                if (text.isBlank()) msg = "文件为空"
                else viewModel.importJson(text) { ok, m -> msg = m }
            }.onFailure { msg = "读取文件失败：${it.message}" }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "备份与恢复", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            SectionCard(title = "导出备份") {
                Text("将所有数据导出为单个版本化 JSON 文件，保存到你可访问的目录。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.exportAndSave("", onResult = { json ->
                        runCatching {
                            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                                ?: context.filesDir
                            dir.mkdirs()
                            val name = "weight_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())}.json"
                            val file = File(dir, name)
                            file.writeText(json)
                            msg = "已导出到：${file.absolutePath}"
                        }.onFailure {
                            msg = "导出失败：${it.message}"
                        }
                    })
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("导出为 JSON 文件")
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(title = "导入恢复") {
                Text("从之前导出的 JSON 文件恢复全部数据（会先清空当前数据）。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("选择备份文件并恢复")
                }
            }

            msg?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
