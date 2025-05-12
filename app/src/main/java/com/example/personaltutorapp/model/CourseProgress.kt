package com.example.personaltutorapp.model

data class CourseProgress(
    val courseId: String? = null,
    val studentId: String = "",
    val userAuthId: String = "",
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val studentName: String = ""
)