package com.weightmanagement.app.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * 食物图片工具：从相册/相机读图 → 压缩 → 存内部存储 + 转 base64 供 AI 识别。
 */
object ImageUtils {

    /** 压缩后的最大边长（控制 AI 请求体大小与存储体积） */
    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 82

    /**
     * 把 Uri 指向的图片压缩为 JPEG，保存到内部存储 food_images/ 目录。
     * @return 保存后的 File；失败返回 null
     */
    fun saveToInternal(context: Context, uri: Uri): File? {
        return try {
            // 先把 Uri 内容完整读入内存（Uri 的流只能消费一次）
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val bmp = decodeScaled(bytes) ?: return null
            writeInternal(context, bmp)
        } catch (e: Exception) {
            null
        }
    }

    /** 相机拍照落盘的文件 → 压缩保存到内部存储 food_images/ */
    fun saveFileToInternal(context: Context, file: File): File? {
        return try {
            val bmp = decodeScaled(file.readBytes()) ?: return null
            writeInternal(context, bmp)
        } catch (e: Exception) {
            null
        }
    }

    /** 读取文件并转 base64（用于 AI 视觉接口） */
    fun toBase64(file: File): String {
        return Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
    }

    private fun writeInternal(context: Context, bmp: Bitmap): File {
        val dir = File(context.filesDir, "food_images").apply { mkdirs() }
        val file = File(dir, "food_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        bmp.recycle()
        return file
    }

    /**
     * 解码字节数组并缩放到 MAX_SIDE 以内。
     * 注意：inJustDecodeBounds 模式下 decodeStream 返回 null 是正常行为，
     * 尺寸信息在 opts.outWidth/outHeight（此前误用返回值判空导致相册选图必然失败）。
     */
    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        // 先读尺寸（仅 bounds）
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
        // 精确缩放
        val side = max(raw.width, raw.height)
        return if (side > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / side
            Bitmap.createScaledBitmap(
                raw, (raw.width * scale).toInt().coerceAtLeast(1),
                (raw.height * scale).toInt().coerceAtLeast(1), true
            )
        } else raw
    }

    /** 供 Coil 显示用的文件 Uri */
    fun fileUri(file: File): Uri = Uri.fromFile(file)
}
