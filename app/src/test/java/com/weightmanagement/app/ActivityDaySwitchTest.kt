package com.weightmanagement.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 自测：运动页日期切换（与饮水/饮食同模式的 selectedDay + flatMapLatest 管线）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDaySwitchTest {

    private val db = mutableMapOf<Long, List<String>>(
        100L to listOf("今天-跑步-300kcal", "今天-力量-200kcal"),
        50L to listOf("过去-游泳-400kcal")
    )

    private fun observeByDay(day: Long) = flowOf(db[day] ?: emptyList())

    @Test
    fun `运动页切换日期正确显示对应记录`() = runTest(StandardTestDispatcher()) {
        val selectedDay = MutableStateFlow(100L)
        val activities: StateFlow<List<String>> = selectedDay
            .flatMapLatest { day -> observeByDay(day) }
            .stateIn(
                scope = CoroutineScope(coroutineContext + Dispatchers.Unconfined),
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        val collector = launch { activities.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("今天-跑步-300kcal", "今天-力量-200kcal"), activities.value)

        selectedDay.value = 50L
        advanceUntilIdle()
        assertEquals(listOf("过去-游泳-400kcal"), activities.value)

        // 无记录日 → 空
        selectedDay.value = 999L
        advanceUntilIdle()
        assertEquals(emptyList<String>(), activities.value)

        // 切回今天
        selectedDay.value = 100L
        advanceUntilIdle()
        assertEquals(listOf("今天-跑步-300kcal", "今天-力量-200kcal"), activities.value)

        coroutineContext.cancelChildren()
    }
}
