package com.example.personaltutorapp.model

data class User(
    val uid: String = "",      // Firebase Auth UID - this is the primary identifier and documents in the users collection use this as their ID
    val id: String = "",       // Custom ID (studentId for students, tutorId for tutors) - this is a secondary identifier
    val displayName: String = "",
    val email: String = "",
    val isTutor: Boolean = false,
    val createdAt: Long? = null // 根據您的 Firebase 數據，添加 createdAt
)