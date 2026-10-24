package com.weightmanagement.app.data.repository

import com.weightmanagement.app.data.local.dao.GoalDao
import com.weightmanagement.app.data.local.dao.UserDao
import com.weightmanagement.app.data.local.entity.GoalEntity
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.domain.calculator.BmrCalculator
import com.weightmanagement.app.domain.calculator.GoalCalculator
import com.weightmanagement.app.domain.calculator.TdeeCalculator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val userDao: UserDao,
    private val goalDao: GoalDao
) {
    fun observeUser(): Flow<UserEntity?> = userDao.observe()
    suspend fun getUser(): UserEntity? = userDao.get()

    suspend fun saveUser(user: UserEntity) {
        userDao.upsert(user)
        recomputeGoal(user)
    }

    fun observeGoal(): Flow<GoalEntity?> = goalDao.observe()

    /** 退出登录：清除个人档案（返回建档页），其余记录保留 */
    suspend fun clearUser() = userDao.clear()

    /** 依据用户资料计算并保存目标 */
    suspend fun recomputeGoal(user: UserEntity) {
        val bmr = BmrCalculator.mifflinStJeor(user.currentWeightKg, user.heightCm, user.age, user.gender)
        val tdee = TdeeCalculator.tdee(bmr, user.activityLevel)
        val cal = GoalCalculator.calorieTarget(tdee, user.goalType, 0.5f)
        val macros = GoalCalculator.macroTargets(cal, weightKg = user.currentWeightKg)
        goalDao.upsert(
            GoalEntity(
                targetWeightKg = user.targetWeightKg,
                calorieTarget = cal,
                proteinTarget = macros.proteinG,
                carbTarget = macros.carbG,
                fatTarget = macros.fatG,
                waterTargetMl = 2000
            )
        )
    }
}
