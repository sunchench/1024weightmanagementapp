package com.weightmanagement.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 自测：验证「切换日期 → dayIntakes 立即反映新日期数据」的核心链路。
 * 模拟 Room observeByDay(day) 的语义：每个 day 查询返回该日数据流；
 * flatMapLatest 在 selectedDay 变化时取消旧查询、订阅新查询。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DayIntakesSwitchTest {

    // 模拟数据库：day -> 记录列表
    private val db = mutableMapOf<Long, List<String>>(
        100L to listOf("今天-早餐-包子", "今天-午餐-盒饭"),
        50L to listOf("过去-晚餐-面条")
    )

    // 模拟 IntakeDao.observeByDay
    private fun observeByDay(day: Long) = flowOf(db[day] ?: emptyList())

    @Test
    fun `切换日期后列表立即反映新日期且无残留`() = runTest(StandardTestDispatcher()) {
        val selectedDay = MutableStateFlow(100L)

        // 与 NutritionViewModel.dayIntakes 相同的管线结构
        val dayIntakes: StateFlow<List<String>> = selectedDay
            .flatMapLatest { day -> observeByDay(day) }
            .stateIn(
                scope = kotlinx.coroutines.CoroutineScope(coroutineContext + Dispatchers.Unconfined),
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        val collector = launch { dayIntakes.collect {} }
        advanceUntilIdle()

        // 初始：今天的数据
        assertEquals(listOf("今天-早餐-包子", "今天-午餐-盒饭"), dayIntakes.value)

        // 切到过去日期
        selectedDay.value = 50L
        advanceUntilIdle()
        assertEquals(listOf("过去-晚餐-面条"), dayIntakes.value)

        // 切回今天 —— 用户报的 bug 场景
        selectedDay.value = 100L
        advanceUntilIdle()
        assertEquals(listOf("今天-早餐-包子", "今天-午餐-盒饭"), dayIntakes.value)

        // 切到没有任何记录的日期 → 空列表而非残留旧数据
        selectedDay.value = 999L
        advanceUntilIdle()
        assertEquals(emptyList<String>(), dayIntakes.value)

        // 再切回今天
        selectedDay.value = 100L
        advanceUntilIdle()
        assertEquals(listOf("今天-早餐-包子", "今天-午餐-盒饭"), dayIntakes.value)

        coroutineContext.cancelChildren()
    }
}
