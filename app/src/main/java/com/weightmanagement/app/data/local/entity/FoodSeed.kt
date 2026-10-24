package com.weightmanagement.app.data.local.entity

/**
 * 内置常见中式食物营养库（每 100g 可食部）。
 * 数据用于「拍照/上传识别」无 AI 配置时的本地辅助匹配，以及手动添加时的快速检索。
 * 数值参考《中国食物成分表》近似估算，仅作日常估算用途。
 */
object FoodSeed {
    val items: List<FoodEntity> = listOf(
        FoodEntity(name = "白米饭", servingSizeG = 100f, calories = 116f, protein = 2.6f, fat = 0.3f, carbs = 25.9f),
        FoodEntity(name = "糙米饭", servingSizeG = 100f, calories = 112f, protein = 2.6f, fat = 0.9f, carbs = 23.0f),
        FoodEntity(name = "馒头", servingSizeG = 100f, calories = 223f, protein = 7.0f, fat = 1.1f, carbs = 47.0f),
        FoodEntity(name = "面条(煮)", servingSizeG = 100f, calories = 110f, protein = 3.6f, fat = 0.4f, carbs = 23.0f),
        FoodEntity(name = "全麦面包", servingSizeG = 100f, calories = 246f, protein = 9.0f, fat = 3.2f, carbs = 46.0f),
        FoodEntity(name = "燕麦片", servingSizeG = 100f, calories = 367f, protein = 13.0f, fat = 6.7f, carbs = 66.0f),
        FoodEntity(name = "红薯", servingSizeG = 100f, calories = 99f, protein = 1.1f, fat = 0.2f, carbs = 24.0f),
        FoodEntity(name = "玉米(鲜)", servingSizeG = 100f, calories = 106f, protein = 4.0f, fat = 1.2f, carbs = 22.8f),
        FoodEntity(name = "土豆", servingSizeG = 100f, calories = 77f, protein = 2.0f, fat = 0.2f, carbs = 17.2f),
        FoodEntity(name = "鸡胸肉", servingSizeG = 100f, calories = 133f, protein = 24.6f, fat = 5.0f, carbs = 0.0f),
        FoodEntity(name = "鸡腿肉", servingSizeG = 100f, calories = 181f, protein = 19.0f, fat = 11.0f, carbs = 0.0f),
        FoodEntity(name = "牛肉(瘦)", servingSizeG = 100f, calories = 125f, protein = 20.2f, fat = 4.2f, carbs = 0.0f),
        FoodEntity(name = "猪肉(瘦)", servingSizeG = 100f, calories = 143f, protein = 20.3f, fat = 6.2f, carbs = 1.5f),
        FoodEntity(name = "猪五花", servingSizeG = 100f, calories = 395f, protein = 13.2f, fat = 37.0f, carbs = 0.0f),
        FoodEntity(name = "羊肉(瘦)", servingSizeG = 100f, calories = 118f, protein = 20.5f, fat = 3.9f, carbs = 0.0f),
        FoodEntity(name = "鱼肉(鲫)", servingSizeG = 100f, calories = 108f, protein = 17.1f, fat = 4.1f, carbs = 0.0f),
        FoodEntity(name = "虾仁", servingSizeG = 100f, calories = 93f, protein = 18.6f, fat = 0.8f, carbs = 0.0f),
        FoodEntity(name = "鸡蛋", servingSizeG = 100f, calories = 144f, protein = 13.3f, fat = 8.8f, carbs = 2.8f),
        FoodEntity(name = "豆腐", servingSizeG = 100f, calories = 81f, protein = 8.1f, fat = 3.7f, carbs = 4.2f),
        FoodEntity(name = "豆浆", servingSizeG = 100f, calories = 31f, protein = 3.0f, fat = 1.6f, carbs = 1.2f),
        FoodEntity(name = "牛奶", servingSizeG = 100f, calories = 54f, protein = 3.0f, fat = 3.2f, carbs = 3.4f),
        FoodEntity(name = "酸奶(原味)", servingSizeG = 100f, calories = 72f, protein = 2.5f, fat = 2.7f, carbs = 9.3f),
        FoodEntity(name = "西兰花", servingSizeG = 100f, calories = 34f, protein = 4.1f, fat = 0.6f, carbs = 4.3f),
        FoodEntity(name = "番茄", servingSizeG = 100f, calories = 18f, protein = 0.9f, fat = 0.2f, carbs = 3.9f),
        FoodEntity(name = "黄瓜", servingSizeG = 100f, calories = 15f, protein = 0.8f, fat = 0.2f, carbs = 2.9f),
        FoodEntity(name = "白菜", servingSizeG = 100f, calories = 17f, protein = 1.5f, fat = 0.1f, carbs = 3.2f),
        FoodEntity(name = "菠菜", servingSizeG = 100f, calories = 24f, protein = 2.6f, fat = 0.3f, carbs = 3.6f),
        FoodEntity(name = "胡萝卜", servingSizeG = 100f, calories = 39f, protein = 1.0f, fat = 0.2f, carbs = 8.8f),
        FoodEntity(name = "苹果", servingSizeG = 100f, calories = 52f, protein = 0.3f, fat = 0.2f, carbs = 13.8f),
        FoodEntity(name = "香蕉", servingSizeG = 100f, calories = 89f, protein = 1.1f, fat = 0.3f, carbs = 22.8f),
        FoodEntity(name = "橙子", servingSizeG = 100f, calories = 47f, protein = 0.8f, fat = 0.2f, carbs = 11.1f),
        FoodEntity(name = "西瓜", servingSizeG = 100f, calories = 30f, protein = 0.6f, fat = 0.2f, carbs = 7.6f),
        FoodEntity(name = "葡萄", servingSizeG = 100f, calories = 43f, protein = 0.5f, fat = 0.2f, carbs = 10.3f),
        FoodEntity(name = "花生", servingSizeG = 100f, calories = 567f, protein = 25.0f, fat = 49.0f, carbs = 16.0f),
        FoodEntity(name = "核桃", servingSizeG = 100f, calories = 627f, protein = 14.9f, fat = 58.8f, carbs = 9.5f),
        FoodEntity(name = "油(植物油)", servingSizeG = 100f, calories = 899f, protein = 0.0f, fat = 99.9f, carbs = 0.0f),
        FoodEntity(name = "可乐", servingSizeG = 100f, calories = 43f, protein = 0.0f, fat = 0.0f, carbs = 10.6f),
        FoodEntity(name = "啤酒", servingSizeG = 100f, calories = 32f, protein = 0.4f, fat = 0.0f, carbs = 2.6f),
        FoodEntity(name = "红烧肉", servingSizeG = 100f, calories = 345f, protein = 9.0f, fat = 32.0f, carbs = 4.0f),
        FoodEntity(name = "宫保鸡丁", servingSizeG = 100f, calories = 180f, protein = 12.0f, fat = 11.0f, carbs = 9.0f),
        FoodEntity(name = "番茄炒蛋", servingSizeG = 100f, calories = 95f, protein = 6.0f, fat = 6.5f, carbs = 4.0f),
        FoodEntity(name = "麻婆豆腐", servingSizeG = 100f, calories = 120f, protein = 7.0f, fat = 8.0f, carbs = 5.0f)
    )
}
