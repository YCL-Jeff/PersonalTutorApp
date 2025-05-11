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