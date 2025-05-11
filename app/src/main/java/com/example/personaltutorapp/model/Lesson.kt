package com.example.personaltutorapp.model

data class Lesson(
    val lessonId: String? = null,
    val courseId: String = "",
    val title: String = "",
    val content: String = "",
    val order: Int = 0,
    val isCompleted: Boolean = false
)