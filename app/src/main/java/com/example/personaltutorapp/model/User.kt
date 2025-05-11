package com.example.personaltutorapp.model

data class User(
    val uid: String = "",      // Firebase Auth UID, 也是 users 集合的文件 ID
    val id: String = "",       // 您的自定義 ID (例如 "StudentTest", "teacher123")
    val displayName: String = "",
    val email: String = "",
    val isTutor: Boolean = false,
    val createdAt: Long? = null // 根據您的 Firebase 數據，添加 createdAt
)