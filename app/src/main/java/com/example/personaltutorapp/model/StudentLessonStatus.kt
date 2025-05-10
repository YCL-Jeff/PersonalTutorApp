package com.example.personaltutorapp.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "student_lesson_status",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["uid"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["lessonId"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId", "lessonId"], unique = true),
        Index(value = ["studentId"]),
        Index(value = ["lessonId"]),
        Index(value = ["courseId"])
    ]
)
data class StudentLessonStatus(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val lessonId: Int,
    val courseId: Int,
    var isCompleted: Boolean = false
) 