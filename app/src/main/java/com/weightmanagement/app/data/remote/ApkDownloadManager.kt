package com.weightmanagement.app.data.remote

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * APK 下载与安装：
 * 1) 系统 DownloadManager 下载到公共 Download 目录（带通知、断点续传）
 * 2) 下载完成后 FileProvider 唤起系统安装器
 * 注意：Android 8+ 需要「安装未知应用」权限，未授权时引导用户开启。
 */
object ApkDownloadManager {

    /**
     * 用系统 DownloadManager 下载 APK。
     * @return 已提交的下载任务描述（提示用户从通知栏查看进度）
     */
    fun downloadApk(context: Context, url: String, versionName: String): String {
        val fileName = "weight-management-v$versionName.apk"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("体重管理 v$versionName")
            .setDescription("正在下载更新包")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS, fileName
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        return fileName
    }

    /**
     * 打开已下载的 APK 触发安装。
     * @return null=已发起安装；非 null=需要用户处理的提示（找不到文件/无安装权限）
     */
    fun installApk(context: Context, versionName: String): String? {
        val fileName = "weight-management-v$versionName.apk"
        val file = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (!file.exists()) {
            return "安装包尚未下载完成，请稍后重试"
        }

        // Android 8+：检查本应用是否有「安装未知应用」权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return "NEED_INSTALL_PERMISSION"
        }

        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return null
    }

    /** 跳转「允许安装未知应用」设置页 */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        }
    }
}
