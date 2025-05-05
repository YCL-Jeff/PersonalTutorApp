package com.example.personaltutorapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val lessonId: Int = 0,
    val courseId: Int = 0,
    val title: String = "",
    val content: String = "",
    val order: Int = 0,
    val isCompleted: Boolean = false
)

// 新增學生-課程關係實體
@Entity(tableName = "student_lesson_progress", primaryKeys = ["studentId", "lessonId"])
data class StudentLessonProgress(
    val studentId: String,
    val lessonId: Int,
    val lessonCompleted: Boolean = false,
    val completedTimestamp: Long? = null
)
