package com.example.personaltutorapp.model

import com.google.firebase.firestore.Exclude

data class Lesson(
    val lessonId: String? = null,
    val courseId: String = "",
    val title: String = "",
    val content: String = "",
    val order: Int = 0,
    @get:Exclude val isCompleted: Boolean = false, // Dynamically set in ViewModel
    val mediaUrl: String? = null, // 存储多媒体文件的下载 URL
    val mediaType: String? = null // 存储文件类型（pdf, mp3, mp4）
)