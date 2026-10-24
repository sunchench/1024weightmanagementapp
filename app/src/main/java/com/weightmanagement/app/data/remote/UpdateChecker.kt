package com.weightmanagement.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL

/** 远端版本信息（version.json） */
data class RemoteVersion(
    val versionCode: Long,
    val versionName: String,
    val releaseDate: String,
    val downloadUrl: String,
    val releaseNotes: List<String>
)

/**
 * 应用内检查更新：读取 GitHub 仓库的 version.json（raw 地址），
 * 与本地 versionCode 比对。返回 null=检查失败（原因）。
 */
class UpdateChecker {

    companion object {
        // GitHub raw 固定地址（主分支根目录 version.json）
        private const val VERSION_URL =
            "https://raw.githubusercontent.com/sunchench/1024weightmanagementapp/main/version.json"
    }

    sealed class Result {
        data class UpToDate(val currentName: String) : Result()
        data class UpdateAvailable(val remote: RemoteVersion) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun check(currentVersionCode: Long, currentVersionName: String): Result = withContext(Dispatchers.IO) {
        try {
            val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            // raw.githubusercontent 可能重定向，手动跟随一次
            conn.instanceFollowRedirects = true
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return@withContext Result.Error("获取版本信息失败 (HTTP $code)，请检查网络")
            }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val obj = try {
                Json.parseToJsonElement(body).jsonObject
            } catch (e: Exception) {
                return@withContext Result.Error("版本信息格式异常")
            }
            fun str(k: String): String = (obj[k] as? JsonPrimitive)?.content ?: ""
            val remoteCode = str("versionCode").toLongOrNull() ?: 0L
            val remoteName = str("versionName")
            val notes = (obj["releaseNotes"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.content }
                ?: emptyList()

            if (remoteCode <= currentVersionCode) {
                Result.UpToDate(currentVersionName)
            } else {
                Result.UpdateAvailable(
                    RemoteVersion(
                        versionCode = remoteCode,
                        versionName = remoteName,
                        releaseDate = str("releaseDate"),
                        downloadUrl = str("downloadUrl"),
                        releaseNotes = notes
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error("网络异常：${e.message ?: e.javaClass.simpleName}")
        }
    }
}
