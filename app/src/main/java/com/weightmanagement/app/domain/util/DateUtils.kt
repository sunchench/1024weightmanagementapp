package com.weightmanagement.app.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val dtf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    /** 格式化为「年-月-日 时-分」 */
    fun formatDateTime(ts: Long): String = dtf.format(ts)
    fun dayStart(ts: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun dayEnd(ts: Long = System.currentTimeMillis()): Long = dayStart(ts) + 24L * 3600 * 1000

    /** 过去 N 天的起始时间戳 */
    fun daysAgoStart(days: Int): Long = dayStart(System.currentTimeMillis()) - days * 24L * 3600 * 1000
}
