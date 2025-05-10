package com.example.personaltutorapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class Lesson(
    @PrimaryKey(autoGenerate = true) val lessonId: Int = 0,
    val courseId: Int,
    val title: String,
    val content: String = "",
    val order: Int = 0,
    val isCompleted: Boolean = false
)