package com.example.personaltutorapp.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lesson_progress",
    indices = [
        Index(value = ["studentEmail", "courseType"], unique = true)
    ]
)
data class LessonProgress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,                // Student name
    val studentEmail: String,        // Student email as unique identifier
    val courseType: String,          // Course type/ID
    val progress: Int                // Progress percentage (0-100)
) 