package com.weightmanagement.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeightPoint(
    val timestamp: Long,
    val weightKg: Float
)

/**
 * 体重趋势折线图（带坐标轴与逐点信息）：
 * - Y 轴：kg 单位刻度网格线
 * - X 轴：记录日期
 * - 每个点上方标注体重值；点击数据点显示完整「时间 + 体重」
 */
@Composable
fun WeightTrendChart(
    points: List<WeightPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    var selected by remember { mutableStateOf<Int?>(null) }
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = axisColor.copy(alpha = 0.25f)
    val labelColor = axisColor
    val pointLabelColor = lineColor

    val density = LocalDensity.current
    val labelTextPx = with(density) { 10.sp.toPx() }
    val valueTextPx = with(density) { 11.sp.toPx() }
    val axisTextPx = with(density) { 10.sp.toPx() }

    // Y 轴范围向外留 15% 余量并取整刻度
    val minYraw = points.minOf { it.weightKg }
    val maxYraw = points.maxOf { it.weightKg }
    val span = (maxYraw - minYraw).coerceAtLeast(0.5f)
    val minY = (minYraw - span * 0.15f * if (minYraw - span * 0.15f > 0) 1f else 0f)
    val maxY = maxYraw + span * 0.15f
    val range = (maxY - minY).coerceAtLeast(0.5f)

    // Y 轴刻度：约 4 条
    val stepCount = 4
    val rawStep = range / stepCount
    val niceStep = generateSequence(rawStep) { it / 2f }
        .map { s -> kotlin.math.ceil(s).coerceAtLeast(1f) }
        .first { s -> range / s <= stepCount + 1 }
    val yTicks = generateSequence(kotlin.math.ceil(minY / niceStep) * niceStep) { it + niceStep }
        .takeWhile { it <= maxY }
        .toList()

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .pointerInput(points) {
                    detectTapGestures { tap ->
                        // 命中检测：找最近的点（半径 24dp）
                        val hitRadius = 24.dp.toPx()
                        val w = size.width
                        val padL = 44.dp.toPx()
                        val padR = 12.dp.toPx()
                        val padT = 18.dp.toPx()
                        val padB = 44.dp.toPx()
                        val stepX = (w - padL - padR) / (points.size - 1).coerceAtLeast(1)
                        var best: Int? = null
                        var bestDist = Float.MAX_VALUE
                        points.forEachIndexed { i, p ->
                            val x = padL + stepX * i
                            val y = padT + (1f - (p.weightKg - minY) / range) * (size.height - padT - padB)
                            val d = (tap - Offset(x, y)).getDistance()
                            if (d < bestDist) { bestDist = d; best = i }
                        }
                        selected = if (bestDist <= hitRadius) best else null
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val padL = 44.dp.toPx()
            val padR = 12.dp.toPx()
            val padT = 18.dp.toPx()
            val padB = 44.dp.toPx()
            val plotW = w - padL - padR
            val plotH = h - padT - padB
            val stepX = plotW / (points.size - 1)

            fun toOffset(i: Int): Offset {
                val x = padL + stepX * i
                val y = padT + (1f - (points[i].weightKg - minY) / range) * plotH
                return Offset(x, y)
            }

            // 网格线 + Y 轴刻度（kg）
            yTicks.forEach { tick ->
                val y = padT + (1f - (tick - minY) / range) * plotH
                drawLine(gridColor, Offset(padL, y), Offset(w - padR, y), strokeWidth = 1.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(
                    formatTick(tick),
                    padL - 6.dp.toPx(),
                    y + labelTextPx / 3f,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = labelTextPx
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                )
            }
            // Y 轴单位
            drawContext.canvas.nativeCanvas.drawText(
                "kg", 4.dp.toPx(), 12.dp.toPx(),
                android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = axisTextPx
                    isAntiAlias = true
                }
            )

            // X 轴日期刻度：全部记录点都标注，倾斜 45° 展示，避免横向重叠
            val dateFmt = SimpleDateFormat("MM-dd", Locale.CHINA)
            val xLabelPaint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = labelTextPx
                textAlign = android.graphics.Paint.Align.LEFT  // 旋转后以左端为锚点
                isAntiAlias = true
            }
            points.forEachIndexed { i, _ ->
                val o = toOffset(i)
                val label = dateFmt.format(Date(points[i].timestamp))
                val cx = drawContext.canvas.nativeCanvas.save()
                // 绕数据点下方锚点逆时针旋转 45°
                drawContext.canvas.nativeCanvas.rotate(-45f, o.x, h - 6.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(
                    label, o.x, h - 6.dp.toPx(), xLabelPaint
                )
                drawContext.canvas.nativeCanvas.restoreToCount(cx)
            }

            // 折线
            val path = Path().apply {
                points.forEachIndexed { i, _ ->
                    val o = toOffset(i)
                    if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                }
            }
            drawPath(path, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))

            // 数据点 + 每点体重标注
            val valuePaint = android.graphics.Paint().apply {
                color = pointLabelColor.toArgb()
                textSize = valueTextPx
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            points.forEachIndexed { i, _ ->
                val o = toOffset(i)
                // 选中点放大高亮
                if (selected == i) {
                    drawCircle(lineColor.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = o)
                    drawCircle(lineColor, radius = 5.dp.toPx(), center = o)
                } else {
                    drawCircle(lineColor, radius = 4.dp.toPx(), center = o)
                }
                // 体重值标注（隔点绘制避免重叠：点少全标，点多每 2 个标 1 个）
                val shouldLabel = points.size <= 8 || i % 2 == 0 || i == points.size - 1 || selected == i
                if (shouldLabel) {
                    drawContext.canvas.nativeCanvas.drawText(
                        format1(points[i].weightKg),
                        o.x, o.y - 8.dp.toPx(), valuePaint
                    )
                }
            }
        }

        // 选中点的完整信息（时间 + 体重）
        selected?.let { i ->
            val p = points[i]
            val full = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            Text(
                text = "${full.format(Date(p.timestamp))} · ${format1(p.weightKg)} kg",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 0.dp)
                    .align(androidx.compose.ui.Alignment.TopCenter)
            )
        }
    }
}

/** Y 轴刻度值：整数直接显示，小数保留 1 位 */
private fun formatTick(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)
