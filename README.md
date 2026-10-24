# 个人体重管理 App（Weight Management）

Android 原生个人体重管理应用：**记录 → 分析 → 建议 → 调整 → 再记录** 的长期闭环。

- 版本：v3.3（versionCode 6）
- 架构：Kotlin + Jetpack Compose (Material 3) + Clean/MVVM + Hilt + Room + DataStore
- 最低系统：Android 8.0（API 26）；目标系统：Android 14（API 34）

## 功能总览

| 模块 | 说明 |
|---|---|
| 首页 | 问候语、当前体重/BMI、近 7/30 天变化、体重趋势图、今日热量与营养、运动/饮水/净摄入概览 |
| 体重 | 记录/编辑/删除体重（含备注）、趋势图（Y 轴 kg 刻度、X 轴日期、逐点体重标注、点击数据点显示完整时间与体重） |
| 饮食 | 按日记录三餐+零食；**拍照/上传食物图片识别热量**（AI 视觉接口，可选）；本地 40+ 常见中式食物营养库；饮食日记显示食物缩略图 |
| 运动 | 50+ 种运动、MET 消耗计算、运动历史 |
| 饮水 | 每日饮水记录、目标进度、历史日期查询 |
| 目标 | BMI/BMR/TDEE、每日热量与三大营养素目标；**最近一周建议**（基于近 7 天饮食、运动、饮水、体重数据给出针对性建议） |
| 我的 | 个人资料（头像/姓名/电话） |
| 设置 | 备份/恢复（JSON 导出导入）、AI 识别接口配置、清空数据 |

## 数据安全（升级不丢数据）

- 数据存储在本地 Room 数据库（`weight_management.db`），**升级安装（覆盖安装）不丢失任何历史数据**。
- 数据库从 v1→v2→v3 均使用增量迁移（只新增列，不重建表），旧版本 App 的数据在升级后完整保留。
- ⚠️ 卸载重装会清空数据；重要数据请先用「设置 → 备份」导出 JSON。

## 拍照识别食物（两种方式）

1. **AI 视觉识别（推荐）**：「设置 → AI 食物识别」配置任意 OpenAI 兼容视觉接口：
   - 接口地址：如 `https://api.volces.com/api/v3/`（以 `/` 结尾或不含 `/v1` 均可，App 自动拼接 `/v1/chat/completions`）
   - API Key：你的密钥
   - 视觉模型名：如 `doubao-seed-2.1-pro`
   - 配置后，在「饮食」页点「拍照识别」选图，AI 自动返回食物名称、分量与热量/蛋白/脂肪/碳水，可修改后保存。
2. **本地营养库匹配（无需配置）**：未配置 AI 时，拍照后从内置食物库搜索选择，按克数自动折算营养。
- 图片保存在应用私有目录，仅用于你主动发起的识别请求。

## 最近一周建议

「目标」页的「最近一周建议」基于近 7 天数据自动生成：

- 饮食：日均摄入 vs 目标的热量差、记录完整度、周体重变化速度
- 运动：运动天数是否达标（建议每周 3~5 次）
- 饮水：日均饮水量 vs 目标缺口

点「刷新建议」可基于最新数据重新计算。

## 安装说明

### 直接安装（推荐）

安装 `weight management-release-signed.apk`（release 签名包）：

1. 手机上打开该 APK；
2. 若提示「未知来源应用」，在系统设置中允许当前来源安装；
3. **从旧版本升级**：直接用新 APK 覆盖安装即可，数据保留；不要先卸载旧版。

### 自己构建

本项目工具链（JDK 17 / Android SDK 34 / Gradle 8.9）全部在 `toolchain/` 目录，不依赖 C 盘环境：

```bash
# Debug 包
./build_apk.sh

# Release 签名包（签名配置见 keystore.properties + signing/）
./build_release.sh
```

产物：
- Debug：`weight management.apk`
- Release：`weight management-release-signed.apk`

## 签名信息

- 密钥库：`signing/weightmanagement-release.jks`（RSA 2048，有效期 30 年）
- 别名：`weightmanagement`
- 口令：见 `keystore.properties`
- ⚠️ 发布更新必须使用同一签名，否则无法覆盖安装；请妥善备份 `signing/` 目录与 `keystore.properties`。

## 项目结构

```text
app/src/main/java/com/weightmanagement/app/
├── data/            # Room 实体/DAO、仓库、AI 配置、视觉客户端
│   ├── analyzer/    # AI 视觉识别器（未配置时回退本地库）
│   ├── remote/      # OpenAI 兼容视觉接口客户端
│   └── repository/  # 各业务仓库（含 AiConfigRepository）
├── domain/          # 计算器（BMI/BMR/TDEE/MET）、建议引擎、工具类
│   └── recommendation/  # 最近一周建议引擎
├── di/              # Hilt 模块（含数据库迁移 1→2→3）
├── navigation/      # Navigation Compose 路由
└── ui/              # Compose 界面（dashboard/weight/nutrition/...）
```

## 版本历史

| 版本 | 主要变更 |
|---|---|
| 1.0 | 首版：体重/饮食/运动/饮水/目标、BMR/TDEE、备份恢复 |
| 2.0 | 个人资料页、饮食&饮水按日期历史、静默升级迁移、UI 优化 |
| 3.0 | 拍照识别食物热量（AI+本地库）、体重趋势图坐标轴与逐点信息、最近一周建议、Release 签名包、设置页版本信息 |
| 3.1 | 趋势图 X 轴全部时间点倾斜 45° 展示、AI 接口配置「检测」按钮、支持直接拉起相机拍照识别（相册上传保留并修复读取失败 bug） |
| 3.2 | AI 食物识别移植开源项目 NutriTracker（中文营养师 prompt、复合菜拆解、失败原因透出）；饮食页改为单一「今日所吃」列表 + 添加时选餐别；日期选择固定首项「今天」可随时切回 |
| 3.3 | 修复切换日期后「今日所吃」列表残留旧数据的 bug（改为按日单 SQL 观测，含自测单测） |
