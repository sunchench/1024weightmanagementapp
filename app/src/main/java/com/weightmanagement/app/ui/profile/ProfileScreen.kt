package com.weightmanagement.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.navigation.Routes
import com.weightmanagement.app.ui.common.AppTopBar
import java.io.File

@Composable
fun ProfileScreen(navController: NavHostController, viewModel: ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val user by viewModel.user.collectAsStateWithLifecycle()

    if (user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val ctx = LocalContext.current
    var name by remember { mutableStateOf(user!!.name.ifBlank { user!!.nickname }) }
    var phone by remember { mutableStateOf(user!!.phone) }
    var avatarUri by remember { mutableStateOf(user!!.avatarUri) }
    var showLogout by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            runCatching {
                ctx.contentResolver.openInputStream(it)?.use { input ->
                    val dir = File(ctx.filesDir, "avatar")
                    dir.mkdirs()
                    val f = File(dir, "avatar.png")
                    f.outputStream().use { out -> input.copyTo(out) }
                    avatarUri = f.absolutePath
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "个人中心", onBack = { navController.popBackStack() })
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            // 头像
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri.isNotBlank()) {
                    AsyncImage(
                        model = File(avatarUri),
                        contentDescription = "头像",
                        modifier = Modifier.size(96.dp).clip(CircleShape)
                    )
                } else {
                    val initial = name.firstOrNull()?.uppercase() ?: "我"
                    Text(initial, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                IconButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "更换头像", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("姓名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("电话号码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    viewModel.save(
                        user!!.copy(name = name, nickname = name, phone = phone, avatarUri = avatarUri)
                    )
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showLogout = true },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Text("退出", color = MaterialTheme.colorScheme.onErrorContainer)
            }

            Spacer(Modifier.height(8.dp))
            val genderLabel = if (user!!.gender == "female") "女" else "男"
            Text(
                "档案：$genderLabel · ${user!!.age}岁 · ${user!!.heightCm.toInt()}cm · 目标 ${user!!.targetWeightKg.toInt()}kg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            confirmButton = {
                Button(onClick = {
                    showLogout = false
                    viewModel.logout {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                        }
                    }
                }) { Text("确认退出") }
            },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("取消") } },
            title = { Text("退出登录？") },
            text = { Text("将清除个人档案并返回建档页。体重、饮食、运动、饮水等历史记录会保留。") }
        )
    }
}
