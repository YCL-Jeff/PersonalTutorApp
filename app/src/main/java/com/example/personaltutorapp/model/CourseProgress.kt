package com.example.personaltutorapp.model

data class CourseProgress(
    val courseId: String? = null,
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val progressPercentage: Int = 0,
    val studentId: String = "",
    val studentName: String = ""
)