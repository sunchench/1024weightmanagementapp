# 个人体重管理 App 开发计划

> 版本：V1.0  
> 目标：基于 Solo Forge、NutriTracker、OpenNutriTracker 三个开源项目的优秀架构与业务能力，重新规划一个 Android 原生个人体重管理 App。  
> 原则：**去除蓝牙体重秤/智能秤能力，保留并融合体重、饮食、运动、营养、AI、Health Connect、数据备份等能力。**

---

## 1. 项目目标

开发一款以“**长期体重管理闭环**”为核心的 Android App：

```text
用户建档
  ↓
体重目标
  ↓
BMR / TDEE / 每日热量目标
  ↓
每日饮食记录 ──→ AI 拍照识别
  ↓
运动记录
  ↓
每日能量收支
  ↓
体重趋势
  ↓
动态分析与建议
  ↓
持续调整体重管理目标
```

产品定位不是单纯的“体重记录器”或“卡路里计算器”，而是：

> **记录 → 分析 → 建议 → 调整 → 再记录**

形成完整的个人体重管理闭环。

---

# 2. 开源项目能力借鉴

## 2.1 Solo Forge：作为 Android 工程架构主要参考

GitHub：
https://github.com/kirilan/SoloForge

重点借鉴：

- Kotlin
- Jetpack Compose
- Material 3
- Single Activity
- Hilt
- Room
- DataStore
- WorkManager
- CameraX
- Ktor
- kotlinx.serialization
- 本地优先（Local-first）
- JSON 数据备份/恢复
- Room Migration
- Health Connect
- AI 食物分析
- Weight / Nutrition / Workout Domain 拆分
- 单元测试与 Migration Test

Solo Forge 当前采用本地优先设计，核心健康数据保存在设备本地；AI 请求为用户主动触发，并支持关闭 AI。该思路作为本项目的数据隐私和离线能力设计基础。

---

## 2.2 NutriTracker：作为 AI + 中国饮食场景主要参考

GitHub：
https://github.com/cdz-hy/NutriTracker

重点借鉴：

- AI 拍照识别食物
- 中餐识别
- 复合菜识别
- 外卖场景
- 食物重量估算
- 营养成分估算
- TDEE
- BMI
- WHO / 中国 WGOC BMI 标准
- 50+ 种运动
- MET 运动消耗计算
- 饮水记录
- 体重历史趋势
- Kotlin + Compose + MVVM + Hilt + Room
- CameraX
- DataStore
- AI API 可配置

NutriTracker 的数据模型和功能拆分可作为本项目营养/饮食领域设计的重要参考。

---

## 2.3 OpenNutriTracker：作为成熟营养业务模型参考

GitHub：
https://github.com/simonoppowa/OpenNutriTracker

重点借鉴：

- 食物日记
- 早餐 / 午餐 / 晚餐 / 零食
- 每餐热量目标
- 食物数据库
- 条形码/食品搜索思路
- TDEE
- BMI
- Macro
- 微量营养素
- 饮水
- 运动
- 断食
- 目标管理
- 数据导入/导出
- Clean Architecture / Feature-based Architecture
- 数据库与营养计算模型

本项目第一阶段**不实现蓝牙体重秤能力**。

OpenNutriTracker 当前为 Flutter 项目，因此主要作为产品能力、数据模型和营养计算逻辑参考，而不是 Android 代码基座。

---

# 3. 三个项目融合后的总体架构

采用：

> **Android 原生 + Kotlin + Jetpack Compose + Clean Architecture + Feature-based Architecture + Local-first**

总体结构：

```text
app
│
├── core
│   ├── common
│   ├── database
│   ├── datastore
│   ├── network
│   ├── ai
│   ├── health
│   ├── ui
│   └── util
│
├── domain
│   ├── user
│   ├── weight
│   ├── nutrition
│   ├── meal
│   ├── activity
│   ├── water
│   ├── goal
│   ├── health
│   └── recommendation
│
└── feature
    ├── onboarding
    ├── dashboard
    ├── weight
    ├── nutrition
    ├── meal
    ├── camera
    ├── activity
    ├── water
    ├── health
    ├── recommendation
    ├── profile
    └── settings
```

---

# 4. 技术栈

## 4.1 Android

| 层 | 技术 |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design | Material 3 |
| Architecture | Clean Architecture + MVVM |
| State | StateFlow |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Navigation | Navigation Compose |
| Camera | CameraX |
| Network | Ktor / OkHttp |
| Serialization | kotlinx.serialization |
| Image | Coil |
| Background | WorkManager |
| Health | Health Connect |
| Chart | Compose 自绘/轻量 Chart |
| Testing | JUnit + Compose UI Test + Android Instrumentation Test |

---

# 5. 核心设计原则

## 5.1 Local-first

用户的核心健康数据默认存储在本地：

- 用户资料
- 体重
- 饮食
- 运动
- 饮水
- 目标
- 每日统计
- AI 分析结果

网络不是 App 正常记录功能的前置条件。

---

## 5.2 AI 可插拔

AI 不直接写死到业务代码。

定义：

```kotlin
interface FoodRecognitionService {
    suspend fun recognize(
        image: Image
    ): FoodRecognitionResult
}
```

AI Provider 可以实现：

```text
OpenAI
Gemini
Qwen
其他 OpenAI Compatible API
本地模型
```

第一版建议采用：

```text
OpenAI Compatible API
```

允许用户自行配置：

- Base URL
- API Key
- Model

---

## 5.3 数据与 AI 解耦

AI 只负责：

```text
图片/文本
 ↓
食物识别
 ↓
估算重量
 ↓
营养估算
```

最终写入数据库前：

```text
AI结果
 ↓
用户确认
 ↓
Meal / Intake
 ↓
Room
```

**禁止 AI 直接修改最终健康数据。**

---

## 5.4 Ground Truth 原则

体重：

> 用户输入优先。

食物重量：

> 用户输入/包装标签/可靠数据库优先。

AI：

> 只能作为估算工具。

AI 输出必须允许用户：

- 修改
- 删除
- 重新识别
- 调整重量
- 调整营养值

---

# 6. V1 功能范围

## 6.1 用户建档

首次打开 App：

```text
姓名/昵称
性别
年龄
身高
当前体重
目标体重
活动水平
体重目标
```

体重目标：

```text
减重
维持
增重
```

---

# 7. 体重管理

## 7.1 体重记录

支持：

- 手动输入体重
- 日期
- 时间
- 备注
- 修改
- 删除

数据：

```text
weight_log
├── id
├── weight
├── timestamp
└── note
```

---

## 7.2 体重趋势

首页显示：

```text
当前体重
目标体重
距离目标
近 7 天变化
近 30 天变化
```

图表：

```text
体重
 │
80│       ●
79│    ●     ●
78│ ●
77│          ●
76│
  └──────────────
    7D 14D 30D 90D
```

---

# 8. BMI / BMR / TDEE

## 8.1 BMI

同时支持：

```text
WHO
中国 WGOC
```

---

## 8.2 BMR

采用可配置计算策略。

第一阶段：

> Mifflin-St Jeor

后续可扩展其他公式。

---

## 8.3 TDEE

```text
BMR × Activity Factor
```

活动水平：

```text
久坐
轻度活动
中度活动
高强度活动
非常高
```

---

# 9. 热量目标

根据：

```text
TDEE
+
目标
+
目标速度
```

计算：

```text
每日热量目标
```

例如：

```text
TDEE = 2300 kcal

减重：
-300 kcal
↓
目标：2000 kcal
```

必须提供安全范围校验，避免生成明显不合理的目标。

---

# 10. 饮食模块

## 10.1 每日饮食日记

```text
早餐
午餐
晚餐
零食
```

每餐记录：

```text
食物
重量
热量
蛋白质
脂肪
碳水
```

---

## 10.2 今日营养 Dashboard

```text
热量
████████░░ 1650 / 2100

蛋白质
██████░░░░ 85 / 130g

碳水
███████░░░ 180 / 230g

脂肪
██████░░░░ 50 / 70g
```

---

# 11. AI 拍照识别

## 11.1 用户流程

```text
点击「拍照记录」
        ↓
CameraX
        ↓
拍摄食物
        ↓
图片预处理
        ↓
AI Vision
        ↓
识别：
  - 食物
  - 菜品
  - 估计重量
  - 热量
  - 蛋白质
  - 脂肪
  - 碳水
        ↓
用户确认
        ↓
写入饮食记录
```

---

## 11.2 AI 结果必须带置信信息

例如：

```text
红烧肉       85g
置信度：中

米饭         150g
置信度：高

青菜         100g
置信度：中
```

如果：

```text
portion_unknown = true
```

则必须要求用户确认重量。

---

# 12. 运动模块

第一阶段不接复杂运动设备。

支持：

```text
步行
跑步
骑行
游泳
力量训练
瑜伽
篮球
足球
其他
```

记录：

```text
运动类型
开始时间
持续时间
强度
消耗热量
```

热量：

```text
MET × Weight × Duration
```

---

# 13. 饮水模块

支持：

```text
目标饮水量
快速 +250ml
快速 +500ml
自定义
```

首页：

```text
今日饮水

1250 / 2000 ml

██████░░░░
```

---

# 14. Health Connect

采用可选集成：

```text
Health Connect
     ↓
体重
运动
步数
活动
     ↓
App
```

默认：

> 用户授权后启用。

支持：

- 读取体重
- 写入体重
- 读取运动
- 写入运动
- 后续增加步数
- 后续增加睡眠

---

# 15. Dashboard

首页作为整个 App 的核心。

建议结构：

```text
┌───────────────────────────┐
│  Good morning             │
│                           │
│  当前体重                 │
│  78.2 kg                  │
│  ↓ 0.4kg / 7 days         │
│                           │
│  目标 70kg                │
│  ━━━━━━━━━━━○             │
│                           │
│  今日热量                 │
│  1650 / 2100 kcal         │
│                           │
│  P 85g   C 180g   F 50g  │
│                           │
│  今日运动 320 kcal        │
│  饮水 1250 / 2000 ml      │
│                           │
│  [+ 记录饮食]             │
│  [+ 记录体重]             │
└───────────────────────────┘
```

---

# 16. 数据模型

第一版建议至少包含：

```text
User
 ├── id
 ├── gender
 ├── age
 ├── height
 ├── currentWeight
 ├── targetWeight
 ├── activityLevel
 └── goalType

WeightLog
 ├── id
 ├── timestamp
 ├── weight
 └── note

Meal
 ├── id
 ├── date
 ├── mealType
 └── name

Food
 ├── id
 ├── name
 ├── servingSize
 ├── calories
 ├── protein
 ├── fat
 └── carbs

Intake
 ├── id
 ├── mealId
 ├── foodId
 ├── grams
 ├── calories
 ├── protein
 ├── fat
 └── carbs

Activity
 ├── id
 ├── type
 ├── duration
 ├── met
 └── calories

WaterIntake
 ├── id
 ├── timestamp
 └── ml

DailySummary
 ├── date
 ├── calories
 ├── protein
 ├── fat
 ├── carbs
 ├── exerciseCalories
 └── water

Goal
 ├── targetWeight
 ├── calorieTarget
 ├── proteinTarget
 ├── carbTarget
 └── fatTarget

AIAnalysis
 ├── id
 ├── imageUri
 ├── rawResponse
 ├── status
 └── createdAt
```

---

# 17. 数据库设计原则

采用 Room。

禁止：

```text
fallbackToDestructiveMigration()
```

数据库升级必须：

```text
修改 Entity
 ↓
version + 1
 ↓
Migration
 ↓
Schema Export
 ↓
Migration Test
```

每一次数据库结构变化必须增加 Migration Test。

---

# 18. AI 接口设计

建议：

```text
AIService
    │
    ├── FoodRecognitionService
    ├── NutritionEstimationService
    └── RecommendationService
```

第一阶段：

```text
FoodRecognitionService
```

第二阶段：

```text
RecommendationService
```

例如：

```text
过去 7 天：
体重下降 0.2kg
平均热量 2050
目标热量 2100

AI：
“过去一周体重下降较慢，可以继续观察 7 天，
无需立即降低热量目标。”
```

---

# 19. AI 体重管理 Agent：V2

V2 再增加：

```text
体重趋势
+
饮食趋势
+
运动
+
饮水
+
Health Connect
+
历史目标
        ↓
AI
        ↓
个性化建议
```

能力：

### 每日建议

```text
今天还剩 650 kcal

推荐：
高蛋白
低油
约 600 kcal
```

### 周总结

```text
过去 7 天：

体重：-0.5kg
平均热量：1980 kcal
目标：2100 kcal
运动：4 次

结论：
当前减重速度正常。

建议：
继续保持当前目标。
```

---

# 20. 隐私设计

健康数据属于高敏感数据。

原则：

```text
默认本地存储
不要求登录
不上传健康数据
AI 请求只发送用户主动提交的数据
API Key 加密保存
不集成广告 SDK
不集成无必要 Analytics
```

AI 开关：

```text
Settings
 └── AI 功能
      ├── Enable / Disable
      ├── Base URL
      ├── API Key
      └── Model
```

---

# 21. 数据备份

V1 必须实现：

```text
Export JSON
Import JSON
```

例如：

```text
backup/
 ├── user.json
 ├── weight.json
 ├── meals.json
 ├── activities.json
 └── water.json
```

建议最终采用：

```text
单一版本化 JSON Backup
```

例如：

```json
{
  "version": 1,
  "exportedAt": "...",
  "user": {},
  "weightLogs": [],
  "meals": [],
  "activities": [],
  "water": []
}
```

---

# 22. 测试计划

## Unit Test

重点测试：

```text
BMI
BMR
TDEE
热量目标
Macro
MET
体重趋势
目标进度
AI JSON Parser
Backup Parser
```

---

## Database Test

重点：

```text
CRUD
Migration
Backup Restore
数据完整性
```

---

## AI Test

必须测试：

```text
正常图片
复杂中餐
多菜品
外卖
模糊图片
无食物图片
低置信度
portion_unknown
AI 超时
AI 返回非法 JSON
网络失败
API Key 错误
```

---

## UI Test

重点：

```text
首次安装
用户建档
记录体重
记录饮食
拍照识别
确认 AI 结果
查看 Dashboard
查看趋势
修改目标
Health Connect 授权
Backup / Restore
```

---

# 23. 开发阶段计划

## Phase 0：项目初始化

目标：

> 能编译、能运行、架构稳定。

任务：

- Android Studio Project
- Kotlin
- Compose
- Material 3
- Hilt
- Room
- DataStore
- Navigation
- 基础 Clean Architecture
- Git
- CI
- Test Framework

交付：

```text
可运行 Android APK
```

---

## Phase 1：用户与体重

开发：

- Onboarding
- User Profile
- Weight Log
- BMI
- BMR
- TDEE
- Goal
- Weight Chart

交付：

> 可以完整记录个人体重并看到趋势。

---

## Phase 2：饮食

开发：

- Meal
- Food
- Intake
- Daily Calories
- Macro
- Water
- Dashboard

交付：

> 可以完成一天完整饮食记录。

---

## Phase 3：运动

开发：

- Activity
- MET
- Calories Burned
- Daily Energy Balance

交付：

> 完成“摄入 + 消耗”闭环。

---

## Phase 4：AI 食物识别

开发：

- CameraX
- Image preprocessing
- AI Provider
- Prompt
- JSON Schema
- Result confirmation
- Retry
- Error handling

交付：

> 拍照 → AI 分析 → 用户确认 → 自动写入饮食。

---

## Phase 5：Health Connect

开发：

- Permission
- Read Weight
- Write Weight
- Read Exercise
- Write Exercise

交付：

> 与 Android 健康数据生态打通。

---

## Phase 6：数据安全与备份

开发：

- JSON Export
- JSON Import
- Version migration
- API Key encryption
- Privacy settings

交付：

> 用户可以安全备份和恢复全部数据。

---

## Phase 7：AI 体重管理建议

开发：

- Daily analysis
- Weekly analysis
- Weight trend analysis
- Nutrition analysis
- Recommendation engine

交付：

> 从“记录工具”升级为“体重管理助手”。

---

# 24. V1 MVP 范围控制

V1 **必须有**：

```text
✓ 用户建档
✓ 当前/目标体重
✓ BMI
✓ BMR
✓ TDEE
✓ 体重记录
✓ 体重趋势
✓ 饮食记录
✓ 热量
✓ 蛋白质/脂肪/碳水
✓ 饮水
✓ 基础运动
✓ Dashboard
✓ AI 拍照识别
✓ 本地数据库
✓ JSON Backup
```

V1 **暂不做**：

```text
✗ 蓝牙体重秤
✗ 智能体脂秤
✗ 社交
✗ 排行榜
✗ 云端账号体系
✗ 订阅体系
✗ 广告
✗ 社区
✗ 复杂食谱推荐
✗ 医疗诊断
✗ 医疗处方
```

---

# 25. V2 功能

```text
AI 每日建议
AI 周报
AI 饮食计划
AI 晚餐推荐
AI 趋势异常提醒
睡眠
步数
更多 Health Connect 数据
更多营养素
食物数据库
条形码
食谱
断食
```

---

# 26. V3 产品方向

最终形成：

```text
                    Personal Health AI
                           │
          ┌────────────────┼────────────────┐
          │                │                │
        Weight           Nutrition        Activity
          │                │                │
          └────────────────┼────────────────┘
                           │
                       Health Data
                           │
                    AI Recommendation
                           │
                 Personal Weight Agent
```

最终产品目标：

> **不是帮用户“记录体重”，而是帮助用户持续完成体重管理。**

---

# 27. 开源项目借鉴边界

本项目需要明确：

### 可以借鉴

- 架构思想
- 数据模型设计思路
- UI/UX 思路
- 业务流程
- 计算公式
- 测试思路
- 工程实践

### 二次开发前必须确认

- LICENSE
- 代码是否可以直接复制
- 依赖库 License
- 数据库 License
- 食物数据库 License
- AI Prompt / Dataset 的授权
- 图片/图标/字体授权

目前三个主要参考项目均涉及 GPLv3，因此如果未来计划商业化发布，必须在编码前完成 License 合规审查；本开发计划默认采用“**架构借鉴优先，代码复制需逐项确认授权**”的策略。

---

# 28. 推荐最终技术路线

```text
Android Native
      │
      ▼
Kotlin + Compose
      │
      ▼
Clean Architecture
      │
      ├───────────────┐
      ▼               ▼
Local-first       AI Service
      │               │
      ▼               ▼
    Room        Food Recognition
      │               │
      ├───────────────┤
      ▼
Health Connect
      │
      ▼
Dashboard
      │
      ▼
Weight Management
```

核心思想：

> **Solo Forge 负责工程基座，NutriTracker 负责 AI/中国饮食能力，OpenNutriTracker 负责成熟营养业务模型。**

---

# 29. 第一阶段开发完成标准

当以下流程可以完整跑通时，认为 MVP 基础版本完成：

```text
安装 App
  ↓
首次启动
  ↓
填写身高/年龄/体重/目标体重
  ↓
计算 BMI/BMR/TDEE
  ↓
进入 Dashboard
  ↓
记录今日体重
  ↓
拍照记录午餐
  ↓
AI 识别
  ↓
用户确认食物/重量
  ↓
写入饮食
  ↓
记录运动
  ↓
记录饮水
  ↓
Dashboard 更新
  ↓
查看 7 天体重趋势
  ↓
导出 Backup
  ↓
清空数据
  ↓
Restore Backup
  ↓
数据恢复成功
```

这条链路跑通以后，再进入 AI 体重管理 Agent。

---

# 30. 最终推荐

**Android 工程基座：Solo Forge**

**AI/中国饮食参考：NutriTracker**

**营养/业务模型参考：OpenNutriTracker**

**明确去除：openScale / 蓝牙体重秤 / 智能秤能力**

最终形成：

> **一个 Local-first、Android 原生、AI 驱动、以体重目标为核心的个人体重管理 App。**

